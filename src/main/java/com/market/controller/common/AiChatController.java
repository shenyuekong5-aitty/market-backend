package com.market.controller.common;

import com.market.common.Result;
import com.market.entity.User;
import com.market.service.AiChatService;
import com.market.service.AiKnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    public record ChatRequest(@NotBlank @Size(max = 500) String question, String sessionId) {}

    private final AiChatService chatService;
    private final AiKnowledgeService knowledgeService;

    public AiChatController(AiChatService chatService, AiKnowledgeService knowledgeService) {
        this.chatService = chatService;
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/chat")
    public Result<AiChatService.ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User user)) throw new IllegalStateException("用户未登录");
        return Result.success(chatService.chat(user.getId(), request.question(), request.sessionId()));
    }

    @GetMapping("/suggestions")
    public Result<List<String>> suggestions() {
        return Result.success(knowledgeService.suggestions());
    }
}
