package com.lusheng.ragblock.service;

import com.lusheng.ragblock.dto.CallbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CallbackService {

    private static final Logger logger = LoggerFactory.getLogger(CallbackService.class);

    private final RestTemplate restTemplate;

    @Value("${callback.enabled}")
    private boolean isCallbackEnabled;

    @Value("${callback.url}")
    private String callbackUrl;

    public CallbackService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 异步发送回调请求。
     * @Async 注解确保此方法在一个单独的线程中执行，不会阻塞调用者。
     * @param payload 要发送的数据
     */
    @Async
    public void sendCallback(CallbackRequest payload) {
        if (!isCallbackEnabled) {
            logger.info("Callback is disabled. Skipping.");
            return;
        }

        logger.info("Sending callback to URL: {}", callbackUrl);
        logger.info("Callback payload: {}", payload);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CallbackRequest> entity = new HttpEntity<>(payload, headers);

            // 发送 POST 请求，我们只关心是否成功，对返回内容不做处理
            restTemplate.postForEntity(callbackUrl, entity, String.class);

            logger.info("Callback sent successfully.");
        } catch (RestClientException e) {
            // 捕获所有 RestTemplate 相关的异常
            logger.error("Failed to send callback to {}: {}", callbackUrl, e.getMessage());
        } catch (Exception e) {
            // 捕获其他可能的异常
            logger.error("An unexpected error occurred during callback", e);
        }
    }
}