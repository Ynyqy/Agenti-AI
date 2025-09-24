package com.lusheng.test.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SSE测试控制器 - 用于验证SpringBoot SSE功能是否正常
 * 这个控制器不依赖外部服务，可以直接测试SSE流式响应
 */
@RestController
@RequestMapping("/api/test")
public class TestSSEController {

    /**
     * 简单的SSE测试接口，返回模拟的流式数据
     * 用于验证SSE功能是否正常工作
     */
    @PostMapping("/sse-demo")
    public SseEmitter testSSE(@RequestBody TestRequest request) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时
        
        // 异步发送数据
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 发送开始事件
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("开始回答: " + request.getQuestion()));
                
                // 模拟流式响应
                String[] words = {"SpringBoot", "开启", "SSE", "需要", "使用", "SseEmitter", "类", 
                                "通过", "@GetMapping", "或", "@PostMapping", "注解", "返回", 
                                "SseEmitter", "对象", "即可", "实现", "流式", "响应"};
                
                for (String word : words) {
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(word));
                    Thread.sleep(500); // 模拟延迟
                }
                
                // 发送完成事件
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("回答完成！"));
                
                emitter.complete();
                
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public String health() {
        return "Server is running! SSE功能正常";
    }
    
    // 内部请求DTO
    public static class TestRequest {
        private String question;
        private String conversationId;
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    }
}