package com.lusheng.logreview.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyWorkflowClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 建议将这些配置放到 application.properties 中，而不是硬编码
    private static final String BASE_URL = "http://192.168.200.12/v1";
    private static final String API_KEY  = "app-dw370VJFlIL46FUKFr462Sg4";
    private static final String WORKFLOW_RUN_ENDPOINT = "/workflows/run";

    /**
     * 同步阻塞调用 Dify 工作流
     * @param content 从日志中提取的内容
     * @return 工作流返回的 JSON 响应
     */
    public JsonNode runWorkflow(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        Map<String, Object> body = Map.of(
                "inputs", Map.of("content", content),   // 把 content 作为 Dify 工作流的输入参数
                "response_mode", "blocking",
                "user", "spring-boot-log-review-app" // 可以自定义一个用户标识
        );

        HttpEntity<String> entity;
        try {
            // 将 Map 转换为 JSON 字符串
            String jsonBody = objectMapper.writeValueAsString(body);
            entity = new HttpEntity<>(jsonBody, headers);
        } catch (Exception e) {
            log.error("Failed to construct request body", e);
            throw new IllegalStateException("构造 body 失败", e);
        }

        try {
            ResponseEntity<JsonNode> resp =
                    restTemplate.exchange(BASE_URL + WORKFLOW_RUN_ENDPOINT,
                            HttpMethod.POST,
                            entity,
                            JsonNode.class);

            log.info("Dify API response successfully received.");
            log.debug("Dify response body: {}", resp.getBody()); // 使用 debug 级别打印详细日志
            return resp.getBody();
        } catch (Exception e) {
            log.error("Error while calling Dify API", e);
            // 这里可以根据业务需求抛出自定义异常或返回 null/错误信息
            throw new RuntimeException("调用 Dify 工作流失败", e);
        }
    }
}
