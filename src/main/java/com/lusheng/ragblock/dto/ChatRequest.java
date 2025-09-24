package com.lusheng.ragblock.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatRequest {
    private String question;

    @JsonProperty("session_id") // 允许客户端使用下划线形式
    private String sessionId;

    // --- 新增字段 ---
    @JsonProperty("user_id") // 允许客户端使用下划线
    private String userId;
}