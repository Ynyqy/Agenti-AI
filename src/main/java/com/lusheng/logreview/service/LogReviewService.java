package com.lusheng.logreview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lusheng.logreview.client.DifyWorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor // 使用 Lombok 自动注入 final 字段
public class LogReviewService {

    private final DifyWorkflowClient difyWorkflowClient;

    /**
     * 分析纯文本日志内容
     * @param logContent 用户传入的纯文本日志
     * @return Dify 返回的分析结果
     */
    public JsonNode analyzeLogContent(String logContent) {
        if (logContent == null || logContent.isBlank()) {
            log.warn("Input log content is blank or null.");
            throw new IllegalArgumentException("日志内容不能为空");
        }

        try {
            log.info("Starting analysis for text content...");

            // 调用 Dify 客户端进行分析
            log.info("Sending log content to Dify workflow...");
            JsonNode result = difyWorkflowClient.runWorkflow(logContent);
            log.info("Successfully received analysis result from Dify.");

            return result;

        } catch (Exception e) {
            log.error("An error occurred during log content analysis", e);
            // 抛出运行时异常，让全局异常处理器来处理它
            throw new RuntimeException("日志内容分析失败，请稍后再试。", e);
        }
    }
}

