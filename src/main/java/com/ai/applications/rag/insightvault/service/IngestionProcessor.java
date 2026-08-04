package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.models.entity.KnowledgeDocument;
import com.ai.applications.rag.insightvault.repository.KnowledgeDocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.layout.ForkPDFLayoutTextStripper;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionProcessor {

    private static final Logger log = LoggerFactory.getLogger(IngestionProcessor.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final EmbeddingIndexer embeddingIndexer;

    public IngestionProcessor(KnowledgeDocumentRepository documentRepository, EmbeddingIndexer embeddingIndexer) {
        this.documentRepository = documentRepository;
        this.embeddingIndexer = embeddingIndexer;
    }

    @Transactional
    public void process(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        log.info("Ingestion started: documentId={} filename={}", documentId, document.getOriginalFilename());
        try {
            document.markProcessing();
            documentRepository.save(document);

            List<String> chunks = extractChunks(document);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("The uploaded file did not produce any usable text chunks.");
            }
            log.debug("Ingestion documentId={}: split into {} chunks", documentId, chunks.size());

            embeddingIndexer.index(document, chunks);
            document.markReady(chunks.size());
            documentRepository.save(document);
            log.info("Ingestion completed: documentId={} filename={} chunks={}",
                    documentId, document.getOriginalFilename(), chunks.size());
        } catch (Exception ex) {
            String failure = ex.getMessage() == null ? "Unknown ingestion error" : ex.getMessage();
            log.warn("Ingestion failed: documentId={} filename={} reason={}",
                    documentId, document.getOriginalFilename(), failure, ex);
            document.markFailed(failure);
            documentRepository.save(document);
        }
    }

    private List<String> extractChunks(KnowledgeDocument document) {
        Path filePath = Path.of(document.getStoragePath());
        List<Document> pageDocuments = isPdf(document) ? readPdfPages(filePath) : readViaTika(filePath);

        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(pageDocuments).stream().map(Document::getText).toList();
    }

    private boolean isPdf(KnowledgeDocument document) {
        if (document.getContentType() != null && document.getContentType().equalsIgnoreCase("application/pdf")) {
            return true;
        }
        String filename = document.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    private List<Document> readPdfPages(Path filePath) {
        File file = filePath.toFile();
        int pageCount;
        try (PDDocument probe = Loader.loadPDF(file)) {
            pageCount = probe.getNumberOfPages();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to open PDF: " + filePath.getFileName(), ex);
        }
        if (pageCount == 0) {
            return List.of();
        }

        int workers = Math.min(pageCount, Runtime.getRuntime().availableProcessors());
        int pagesPerWorker = (int) Math.ceil((double) pageCount / workers);

        List<int[]> ranges = new ArrayList<>();
        for (int start = 1; start <= pageCount; start += pagesPerWorker) {
            int end = Math.min(start + pagesPerWorker - 1, pageCount);
            ranges.add(new int[] {start, end});
        }

        return ranges.parallelStream()
                .map(range -> extractPageRange(file, range[0], range[1]))
                .flatMap(List::stream)
                .toList();
    }

    private List<Document> extractPageRange(File file, int startPage, int endPage) {
        try (PDDocument document = Loader.loadPDF(file)) {
            List<Document> pages = new ArrayList<>();
            for (int page = startPage; page <= endPage; page++) {
                PDFTextStripper stripper = new ForkPDFLayoutTextStripper();
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(new Document(stripper.getText(document)));
            }
            return pages;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read PDF pages " + startPage + "-" + endPage
                    + " of " + file.getName(), ex);
        }
    }

    private List<Document> readViaTika(Path filePath) {
        try {
            Tika tika = new Tika();
            tika.setMaxStringLength(-1);
            String extractedText = tika.parseToString(filePath.toFile());
            if (extractedText == null || extractedText.isBlank()) {
                throw new IllegalStateException("No readable text found in the uploaded document.");
            }
            return List.of(new Document(extractedText));
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read and parse the uploaded file: " + filePath.getFileName(), ex);
        }
    }
}
