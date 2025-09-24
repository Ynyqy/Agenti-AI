package com.lusheng.ragblock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class KeywordExtractionService {

    // FIX 1: 声明 logger 变量
    // 这个静态 final 变量用于在整个类中记录日志。
    private static final Logger logger = LoggerFactory.getLogger(KeywordExtractionService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // FIX 2: 声明 apiKey, baseUrl, 和 model 变量
    // @Value 注解会从 application.properties 文件中读取配置并注入到这些变量中。
    @Value("${dashscope.api.key}")
    private String apiKey;
    @Value("${dashscope.base.url}")
    private String baseUrl;
    @Value("${dashscope.model}")
    private String model;

    // 构造函数
    public KeywordExtractionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 异步从用户问题中提取最主要的一个关键词。
     */
    @Async
    public CompletableFuture<String> extractPrimaryKeywordAsync(String question) {
        // 现在 logger, baseUrl, model 都可以被正确解析了
        logger.info("Starting primary keyword extraction for question: '{}'", question);
        String url = baseUrl + "/chat/completions";

        String prompt = String.format(
                "你是一个关键词提取工具。请从以下问题中提取出最核心、最主要的一个关键词或短语。只返回这个词，不要任何引号、JSON格式、解释或其他任何多余的内容。问题是：“%s”",
                question
        );

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", Collections.singletonList(message));
        payload.put("temperature", 0.0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.get("choices") instanceof List) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> responseMessage = (Map<String, Object>) firstChoice.get("message");
                    String keyword = (String) responseMessage.get("content");

                    keyword = keyword.trim().replace("\"", "");

                    logger.info("Successfully extracted primary keyword: {}", keyword);
                    return CompletableFuture.completedFuture(keyword);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to extract primary keyword for question '{}': {}", question, e.getMessage());
        }

        return CompletableFuture.completedFuture("");
    }
}