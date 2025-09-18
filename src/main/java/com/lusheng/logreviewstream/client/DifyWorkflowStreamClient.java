package com.lusheng.logreviewstream.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Component
public class DifyWorkflowStreamClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Dify 配置信息
    private static final String BASE_URL = "http://192.168.200.12/v1";
    private static final String API_KEY  = "app-dw370VJFlIL46FUKFr462Sg4";
    private static final String WORKFLOW_RUN_ENDPOINT = "/workflows/run";

    @Autowired
    public DifyWorkflowStreamClient(WebClient.Builder webClientBuilder) {
        // 使用 WebClient.Builder 创建并配置 WebClient 实例
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    /**
     * 以流式方式调用 Dify 工作流
     * @param content 日志内容
     * @return 返回一个字符串流 (Flux<String>)，每个字符串是 Dify 返回的一个事件
     */
    public Flux<String> runWorkflowStreaming(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        // 注意: response_mode 改为 "streaming"
        Map<String, Object> body = Map.of(
                "inputs", Map.of("content", content),
                "response_mode", "streaming", // 关键改动：请求流式响应
                "user", "spring-boot-streaming-app"
        );

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("Failed to construct request body for streaming", e);
            // 返回一个发出错误信号的流
            return Flux.error(new IllegalStateException("构造 body 失败", e));
        }

        log.info("Starting stream request to Dify workflow.");

        return webClient.post()
                .uri(WORKFLOW_RUN_ENDPOINT)
                .headers(h -> h.addAll(headers))
                .bodyValue(jsonBody)
                .retrieve() // 发送请求并获取响应
                .bodyToFlux(String.class) // 将响应体转换为字符串流 (Flux)
                .doOnError(error -> log.error("Error receiving data from Dify stream: {}", error.getMessage()))
                .doOnComplete(() -> log.info("Dify stream completed."));
    }
}
