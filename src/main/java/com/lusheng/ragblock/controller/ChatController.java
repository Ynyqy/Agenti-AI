package com.lusheng.ragblock.controller;
import com.lusheng.ragblock.dto.ChatRequest;
import com.lusheng.ragblock.dto.ChatResponse;
import com.lusheng.ragblock.service.RAGFlowBlockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat") // 建议在类级别上定义路径
public class ChatController {

    private final RAGFlowBlockService ragflowService;

    public ChatController(RAGFlowBlockService ragflowService) {
        this.ragflowService = ragflowService;
    }

    // Spring Boot 3.x 及以上版本，@PostMapping可以直接使用，无需写路径
    @PostMapping
    public ResponseEntity<ChatResponse> handleChat(@RequestBody ChatRequest chatRequest) {
        if (chatRequest.getQuestion() == null || chatRequest.getQuestion().isEmpty()) {
            // --- FIX 1: 使用三参数构造函数 ---
            // 第一个参数 (sessionId): null
            // 第二个参数 (ragflowData): 错误信息
            // 第三个参数 (keyword): null
            ChatResponse errorResponse = new ChatResponse(null, "Question cannot be empty.", null, chatRequest.getUserId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        try {
            ChatResponse response = ragflowService.chat(chatRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse(chatRequest.getSessionId(), e.getMessage(), null, chatRequest.getUserId());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}