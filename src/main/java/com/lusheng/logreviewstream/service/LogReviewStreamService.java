package com.lusheng.logreviewstream.service;


import com.lusheng.logreviewstream.client.DifyWorkflowStreamClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor // 使用 Lombok 自动注入 final 字段
public class LogReviewStreamService {

    private final DifyWorkflowStreamClient difyWorkflowStreamClient;

    /**
     * 以流式方式分析纯文本日志内容
     * @param logContent 用户传入的纯文本日志
     * @return Dify 返回的分析结果流
     */
    public Flux<String> analyzeLogContentStreaming(String logContent) {
        if (logContent == null || logContent.isBlank()) {
            log.warn("Input log content is blank or null.");
            // 对于响应式流，我们返回一个包含错误的 Flux
            return Flux.error(new IllegalArgumentException("日志内容不能为空"));
        }

        try {
            log.info("Starting streaming analysis for text content...");
            // 直接返回从 Dify 客户端获取的流
            return difyWorkflowStreamClient.runWorkflowStreaming(logContent);
        } catch (Exception e) {
            log.error("An error occurred during log content analysis", e);
            return Flux.error(new RuntimeException("日志内容分析失败，请稍后再试。", e));
        }
    }
}
