package com.ai.applications.rag.insightvault.controller;

import com.ai.applications.rag.insightvault.models.entity.KnowledgeDocument;
import com.ai.applications.rag.insightvault.repository.KnowledgeDocumentRepository;
import com.ai.applications.rag.insightvault.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public DocumentController(DocumentService documentService, KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.documentService = documentService;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    @GetMapping
    public String listDocuments(Authentication authentication, Model model) {
        model.addAttribute("documents", documentService.findVisibleDocuments(authentication));
        return "documents/list";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "documents/upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("files") MultipartFile[] files,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        List<MultipartFile> selectedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                selectedFiles.add(file);
            }
        }

        if (selectedFiles.isEmpty()) {
            log.warn("Upload request from '{}' rejected: no files selected", authentication.getName());
            redirectAttributes.addFlashAttribute("message", "Please choose at least one document to upload.");
            return "redirect:/documents";
        }

        log.info("Upload request from '{}': {} file(s) selected", authentication.getName(), selectedFiles.size());
        int uploadedCount = 0;
        for (MultipartFile file : selectedFiles) {
            try {
                documentService.upload(file, authentication.getName());
                uploadedCount++;
            } catch (Exception ex) {
                log.warn("Upload of '{}' by '{}' failed: {}", file.getOriginalFilename(), authentication.getName(), ex.getMessage());
                redirectAttributes.addFlashAttribute("message",
                        "Uploaded " + uploadedCount + " document(s). One or more files were skipped: " + ex.getMessage());
                return "redirect:/documents";
            }
        }

        redirectAttributes.addFlashAttribute("message",
                uploadedCount == 1
                        ? "Document uploaded successfully. Ingestion has started."
                        : uploadedCount + " documents uploaded successfully. Ingestion has started.");
        return "redirect:/documents";
    }

    @GetMapping("/{documentId}")
    public String documentDetails(@PathVariable UUID documentId, Authentication authentication, Model model) {
        model.addAttribute("document", documentService.findAuthorizedDocument(documentId, authentication));
        return "documents/details";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "query", required = false) String query, Authentication authentication, Model model) {
        List<KnowledgeDocument> documents = documentService.search(query, authentication);
        model.addAttribute("documents", documents);
        return "documents/list";
    }
}
