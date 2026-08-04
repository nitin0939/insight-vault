package com.ai.applications.rag.insightvault.controller;

import com.ai.applications.rag.insightvault.repository.KnowledgeDocumentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public HomeController(KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    @GetMapping({"/", "/home"})
    public String home(Authentication authentication, Model model) {
        long total = knowledgeDocumentRepository.count();
        long ready = knowledgeDocumentRepository.countByStatus(com.ai.applications.rag.insightvault.models.DocumentStatus.READY);
        model.addAttribute("documentCount", total);
        model.addAttribute("readyCount", ready);
        model.addAttribute("username", authentication != null ? authentication.getName() : "guest");
        return "home";
    }
}
