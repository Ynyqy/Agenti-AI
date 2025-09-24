package com.lusheng.ragblock.service;

import com.lusheng.ragblock.dto.CallbackRequest;
import com.lusheng.ragblock.dto.ChatRequest;
import com.lusheng.ragblock.dto.ChatResponse;
import com.lusheng.ragblock.entity.AgtAffairsFile;
import com.lusheng.ragblock.repository.AgtAffairsFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class RAGFlowBlockService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordExtractionService.class);

    private final RestTemplate restTemplate;
    private final CallbackService callbackService; // <--- 注入 CallbackService

    private final AgtAffairsFileRepository fileRepository; // 注入 Repository

    private final KeywordExtractionService keywordService; // 注入关键词服务

    public RAGFlowBlockService(RestTemplate restTemplate, CallbackService callbackService, AgtAffairsFileRepository fileRepository, KeywordExtractionService keywordService) {
        this.restTemplate = restTemplate;
        this.callbackService = callbackService;
        this.fileRepository = fileRepository; // 初始化 Repository
        this.keywordService = keywordService; // 初始化关键词服务
    }

    @Value("${ragflow.api.key}")
    private String apiKey;
    @Value("${ragflow.api.chat_id}")
    private String chatId;
    @Value("${ragflow.api.baseurl}")
    private String ragflowAddress;



    /**
     * 主 chat 方法，协调并行任务
     */
    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        // --- 1. 从请求中获取 user_id ---
        String userId = request.getUserId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = createSession("New Session from SpringBoot");
        }

        // 1. 并行启动 RAGFlow 查询 (不变)
        CompletableFuture<Object> ragflowFuture = getRAGFlowResponseAsync(request.getQuestion(), sessionId);

        // 2. 并行启动关键词提取 (注意返回类型是 CompletableFuture<String>)
        CompletableFuture<String> keywordFuture = keywordService.extractPrimaryKeywordAsync(request.getQuestion());

        // 3. 等待所有并行任务完成 (不变)
        CompletableFuture.allOf(ragflowFuture, keywordFuture).join();

        try {
            // 4. 获取每个任务的结果
            Object ragflowData = ragflowFuture.get();
            String keyword = keywordFuture.get(); // <-- 类型从 List<String> 变为 String

            // 5. 触发异步回调（不变）
            handleCallback(ragflowData, keyword, userId);

            // 6. 合并结果并返回 (构造函数可能需要调整)
            return new ChatResponse(sessionId, ragflowData, keyword, userId);

        } catch (Exception e) {
            logger.error("Error while combining async results", e);
            throw new RuntimeException("Failed to process chat request due to an internal error.", e);
        }
    }

    /**
     * 将原始的 RAGFlow 查询逻辑封装成一个异步方法
     * @return 一个最终会包含 RAGFlow 'data' 对象的 CompletableFuture
     */
    @Async // 标记为异步
    public CompletableFuture<Object> getRAGFlowResponseAsync(String question, String sessionId) {
        Map<String, Object> completionPayload = new HashMap<>();
        completionPayload.put("question", question);
        completionPayload.put("stream", false);
        completionPayload.put("session_id", sessionId);

        String url = String.format("%s/api/v1/chats/%s/completions", ragflowAddress, chatId);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(completionPayload, createHeaders());

        try {
            ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && (Integer) responseBody.get("code") == 0) {
                return CompletableFuture.completedFuture(responseBody.get("data"));
            } else {
                String error = responseBody != null ? (String) responseBody.get("message") : "Unknown RAGFlow error";
                throw new RuntimeException("Failed RAGFlow completion: " + error);
            }
        } catch (Exception e) {
            logger.error("Exception during RAGFlow async request", e);
            // 异步方法中抛出异常会让Future以异常状态完成
            throw new RuntimeException("HTTP Error during RAGFlow completion", e);
        }
    }

    /**
     * 将原始的 RAGFlow 查

    /**
     * 解析 RAGFlow 的响应数据并触发回调
     * @param ragflowData RAGFlow 返回的 data 字段内容
     */
    /**
     * 解析 RAGFlow 响应，查询数据库，并触发回调
     */
    private void handleCallback(Object ragflowData, String keyword, String userId) {
        if (!(ragflowData instanceof Map)) return;

        try {
            Map<String, Object> dataMap = (Map<String, Object>) ragflowData;
            String answer = (String) dataMap.get("answer");

            Map<String, Object> reference = (Map<String, Object>) dataMap.get("reference");
            if (reference == null || !(reference.get("doc_aggs") instanceof List)) return;

            List<Map<String, Object>> docAggs = (List<Map<String, Object>>) reference.get("doc_aggs");
            List<String> docNames = docAggs.stream()
                    .map(agg -> (String) agg.get("doc_name"))
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());

            if (answer == null || docNames.isEmpty()) return;

            // --- 数据库查询逻辑 ---

            // 1. 准备用于查询的标题列表 (去除文件扩展名)
            List<String> titlesForQuery = docNames.stream()
                    .map(this::removeExtension)
                    .collect(Collectors.toList());

            // 2. 从数据库中一次性查询所有匹配的记录
            List<AgtAffairsFile> foundFiles = fileRepository.findByTitleIn(titlesForQuery);

            // 3. 为了快速查找，将查询结果转为 Map<标题, PDF_URL>
            Map<String, String> titleToUrlMap = foundFiles.stream()
                    .collect(Collectors.toMap(AgtAffairsFile::getTitle, AgtAffairsFile::getPdfUrl, (url1, url2) -> url1)); // 处理重复标题

            // 4. 构建回调负载
            List<CallbackRequest.DocumentInfo> documentInfos = docNames.stream().map(docName -> {
                String title = removeExtension(docName);
                String pdfUrl = titleToUrlMap.getOrDefault(title, "URL not found"); // 如果找不到URL，提供一个默认值
                return new CallbackRequest.DocumentInfo(docName, pdfUrl);
            }).collect(Collectors.toList());

            CallbackRequest callbackPayload = new CallbackRequest(answer, documentInfos, keyword, userId);
            callbackService.sendCallback(callbackPayload);

        } catch (Exception e) {
            System.err.println("Error during handleCallback with DB lookup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 辅助方法：从文件名中移除扩展名
     * "document.name.md" -> "document.name"
     */
    private String removeExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return filename; // 没有扩展名
        }
        return filename.substring(0, lastDotIndex);
    }


    /**
     * 创建一个新的 RAGFlow 会话
     */
    private String createSession(String sessionName) {
        String url = String.format("%s/api/v1/chats/%s/sessions", ragflowAddress, chatId);

        // 使用 Map 构造请求体
        Map<String, String> sessionPayload = Map.of("name", sessionName);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(sessionPayload, createHeaders());

        try {
            ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && (Integer) responseBody.get("code") == 0) {
                // 从 Map 中逐层解析出 session_id
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data != null && data.get("id") != null) {
                    String newSessionId = (String) data.get("id");
                    System.out.println("Session created successfully! Session ID: " + newSessionId);
                    return newSessionId;
                }
            }
            String errorMessage = (responseBody != null) ? (String) responseBody.get("message") : "Unknown error";
            throw new RuntimeException("Failed to create RAGFlow session: " + errorMessage);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("HTTP Error during session creation: " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * 创建通用的 HTTP Headers
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }
}
