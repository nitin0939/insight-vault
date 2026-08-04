package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.models.ChatAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int TOP_K = 10;

    private static final String SYSTEM_PROMPT = """
            You are the assistant for Insight Vault, a document knowledge base.
            Answer the user's question using the context chunks provided below.
            The user's wording may not exactly match the terminology used in the context
            (e.g. abbreviations, informal names, or minor typos) - use reasonable judgment
            to match their intent to the concepts actually described in the context.
            Only say you don't know if the context genuinely does not cover the topic asked about.
            Be concise.
            """;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public ChatService(@Qualifier("applicationVectorStore") VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    public ChatAnswer answer(String question, Authentication authentication) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A question is required.");
        }

        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(TOP_K).build());
        log.info("Chat question from '{}' retrieved {} context chunk(s)", authentication.getName(), matches.size());

        if (matches.isEmpty()) {
            return new ChatAnswer(
                    "I couldn't find anything relevant in the uploaded documents to answer that.",
                    List.of());
        }

        String context = matches.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        List<String> sources = matches.stream()
                .map(document -> String.valueOf(document.getMetadata().get("source_name")))
                .distinct()
                .toList();

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage("Context:\n" + context + "\n\nQuestion: " + question)
        ));

        log.debug("Sending chat prompt to model for question: '{}'", question);
        ChatResponse response = chatModel.call(prompt);
        String answerText = response.getResult().getOutput().getText();
        log.info("Chat model responded for question from '{}'", authentication.getName());

        return new ChatAnswer(answerText, sources);
    }
}
