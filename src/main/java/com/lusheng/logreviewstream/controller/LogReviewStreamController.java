package com.lusheng.logreviewstream.controller;


import com.lusheng.logreviewstream.service.LogReviewStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/logs") // 定义 API 的基础路径
@RequiredArgsConstructor
public class LogReviewStreamController {

    private final LogReviewStreamService logReviewStreamService;

    /**
     * API 端点，用于以流式方式分析纯文本日志
     * @param logContent 请求体中的日志文本
     * @return 一个 Server-Sent Events (SSE) 流
     */
    @PostMapping(value = "/analyze-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> analyzeLogStream(@RequestBody String logContent) {
        log.info("Received text log streaming analysis request.");
        // 直接返回 Service 层提供的 Flux<String> 流
        // Spring Boot 会自动将其处理为 SSE 返回给客户端
        return logReviewStreamService.analyzeLogContentStreaming(logContent)
                .doOnError(e -> log.error("Error in streaming response: {}", e.getMessage()));
    }
}

