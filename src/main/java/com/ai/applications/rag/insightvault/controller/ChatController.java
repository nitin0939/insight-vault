package com.ai.applications.rag.insightvault.controller;

import com.ai.applications.rag.insightvault.models.ChatAnswer;
import com.ai.applications.rag.insightvault.models.ChatQuestion;
import com.ai.applications.rag.insightvault.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public String chatPage() {
        return "chat";
    }

    @PostMapping("/ask")
    @ResponseBody
    public ChatAnswer ask(@RequestBody ChatQuestion request, Authentication authentication) {
        return chatService.answer(request.question(), authentication);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ChatAnswer handleInvalidQuestion(IllegalArgumentException ex) {
        return new ChatAnswer(ex.getMessage(), List.of());
    }
}
