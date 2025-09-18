package com.lusheng.logreview.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lusheng.logreview.service.LogReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/logs") // 定义 API 的基础路径
@RequiredArgsConstructor
public class LogReviewController {

    private final LogReviewService logReviewService;

    /**
     * API 端点，用于分析纯文本日志
     * @param logContent 请求体中的日志文本
     * @return Dify 的分析结果
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeLog(@RequestBody String logContent) {
        try {
            log.info("Received text log analysis request.");
            JsonNode result = logReviewService.analyzeLogContent(logContent);
            // 如果成功，返回 200 OK 和分析结果
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // 如果参数错误（如内容为空），返回 400 Bad Request
            log.error("Bad request for log analysis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 如果发生其他内部错误，返回 500 Internal Server Error
            log.error("Internal server error during log analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器内部错误：" + e.getMessage());
        }
    }
}

