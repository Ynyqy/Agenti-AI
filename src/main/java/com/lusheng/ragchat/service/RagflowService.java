package com.lusheng.ragchat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lusheng.ragchat.dto.CompletionRequest;
import com.lusheng.ragchat.uitils.RagflowInitialResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RagflowService {

    private static final Logger logger = LoggerFactory.getLogger(RagflowService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper; // Spring Boot 自动配置了 ObjectMapper

    @Value("${ragflow.api.key}")
    private String ragflowApiKey;

    @Value("${ragflow.api.baseurl}")
    private String ragflowApiBaseUrl;

    @Value("${ragflow.api.chat_id}")
    private String chatId;

    public RagflowService(WebClient.Builder webClientBuilder, @Value("${ragflow.api.baseurl}") String baseUrl, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public Mono<Tuple2<String, Flux<String>>> getCompletions(CompletionRequest request) {
        // 情况一：如果请求中已经有 session_id，直接调用并返回流
        if (request.getSession_id() != null && !request.getSession_id().isEmpty()) {
            logger.info("继续会话，Session ID: {}", request.getSession_id());
            Flux<String> responseStream = callRagflowStream(request);
            // 在这种情况下，我们不返回新的 session_id (返回 null)
            return Mono.just(Tuples.of("", responseStream));
        }

        // 情况二：没有 session_id，需要先创建会话，再提问
        logger.info("开始新会话，问题: {}", request.getQuestion());

        // 使用 flatMap 优雅地链接两个异步调用
        return createNewSessionAndAsk(request);
    }

    /**
     * 封装了创建新会话并立即提问的两步逻辑
     */
    private Mono<Tuple2<String, Flux<String>>> createNewSessionAndAsk(CompletionRequest originalRequest) {
        // 第一步：调用 completions 接口以创建一个新会话。
        // RAGflow 会忽略这个问题，但会返回一个包含新 session_id 的 JSON 对象。
        return webClient.post()
                .uri("/api/v1/chats/{chat_id}/completions", chatId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ragflowApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(originalRequest)
                .retrieve()
                .bodyToMono(String.class) // 获取第一个非流式响应体
                .flatMap(responseBody -> {
                    try {
                        // 解析这个响应以提取新的 session_id
                        // 注意：RAGflow的流式响应是 data: {...}，但第一次响应可能不是，这里假设它是纯JSON
                        String jsonPart = responseBody.startsWith("data:") ? responseBody.substring(5).trim() : responseBody;
                        RagflowInitialResponse initialResponse = objectMapper.readValue(jsonPart, RagflowInitialResponse.class);
                        String newSessionId = initialResponse.getData().getSessionId();

                        if (newSessionId == null || newSessionId.isEmpty()) {
                            return Mono.error(new RuntimeException("无法从RAGflow获取新的Session ID"));
                        }

                        logger.info("成功创建新会话，新的 Session ID: {}", newSessionId);

                        // 第二步：使用新的 session_id 和同样的问题，发起真正的流式请求
                        CompletionRequest subsequentRequest = new CompletionRequest();
                        subsequentRequest.setQuestion(originalRequest.getQuestion());
                        subsequentRequest.setSession_id(newSessionId);

                        Flux<String> actualAnswerStream = callRagflowStream(subsequentRequest);

                        // 将新的 session_id 和真正的答案流一起返回
                        return Mono.just(Tuples.of(newSessionId, actualAnswerStream));

                    } catch (Exception e) {
                        logger.error("解析RAGflow初始响应失败", e);
                        return Mono.error(e);
                    }
                });
    }

    /**
     * 实际调用 RAGflow 并获取流式响应的通用方法
     */
//    private Flux<String> callRagflowStream(CompletionRequest request) {
//        return webClient.post()
//                .uri("/api/v1/chats/{chat_id}/completions", chatId)
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ragflowApiKey)
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(request)
//                .retrieve()
//                .bodyToFlux(String.class);
//    }

    /**
     * 实际调用 RAGflow 并获取流式响应的通用方法。
     * 增加了将累积式响应转换为增量式响应的逻辑。
     */
    private Flux<String> callRagflowStream(CompletionRequest request) {
        Flux<String> rawStream = webClient.post()
                .uri("/api/v1/chats/{chat_id}/completions", chatId)
                .header("Authorization", "Bearer " + ragflowApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);

        // 这是一个有状态的转换，我们需要把累积的 answer 变成增量的
        return rawStream
                // 1. 过滤掉空行和最后的结束标志，只处理包含 "answer" 的数据块
                .filter(line -> line.contains("\"answer\""))
                // 2. 将每个 JSON 字符串解析为 JsonNode 对象
                .map(this::readTree)
                // 3. 使用 buffer(2, 1) 来获取当前块和前一个块
                .buffer(2, 1)
                // 4. 计算增量
                .map(this::calculateDiff)
                // 5. 将修改后的 JsonNode 对象转换回字符串
                .map(this::writeTree)
                // 6. RAGflow 流的最后会有一个结束标志，我们需要把它加回来，以便客户端知道流已结束
                .concatWith(Mono.just("data: {\"code\": 0, \"data\": true}"));
    }

    /**
     * 辅助方法：计算两个连续 JSON 块之间 "answer" 字段的差异
     */
    private JsonNode calculateDiff(List<JsonNode> nodes) {
        // 如果列表只有一个元素，说明这是流的第一个块，直接返回
        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        JsonNode previousNode = nodes.get(0);
        JsonNode currentNode = nodes.get(1);

        String previousAnswer = previousNode.path("data").path("answer").asText("");
        String currentAnswer = currentNode.path("data").path("answer").asText("");

        String diff;
        // 确保当前答案是以之前的答案开头的，以避免错误
        if (currentAnswer.startsWith(previousAnswer)) {
            diff = currentAnswer.substring(previousAnswer.length());
        } else {
            // 如果不是，说明流可能有异常，直接返回当前内容
            diff = currentAnswer;
        }

        // 创建当前节点的一个可变副本，并修改 answer 的值
        ObjectNode newNode = (ObjectNode) currentNode.deepCopy();
        ((ObjectNode) newNode.path("data")).put("answer", diff);

        return newNode;
    }

    /**
     * 辅助方法：安全地将字符串解析为 JsonNode
     */
    private JsonNode readTree(String jsonString) {
        try {
            // RAGflow 的流式响应格式为 "data: {...}"，需要去掉 "data: " 前缀
            String cleanJson = jsonString.startsWith("data:") ? jsonString.substring(5).trim() : jsonString;
            return objectMapper.readTree(cleanJson);
        } catch (IOException e) {
            logger.error("JSON parsing error for string: {}", jsonString, e);
            // 返回一个空的JsonNode以避免NPE
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 辅助方法：安全地将 JsonNode 转换为字符串，并加上 "data: " 前缀
     */
    private String writeTree(JsonNode node) {
        try {
            return "data: " + objectMapper.writeValueAsString(node);
        } catch (IOException e) {
            logger.error("JSON writing error for node: {}", node.toString(), e);
            return "";
        }
    }


}