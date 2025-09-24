package com.lusheng.ragblock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    @JsonProperty("session_id")
    private String sessionId;

    // 使用 Object 类型来接收 RAGFlow 返回的整个 'data' 部分
    private Object answer;

    // 字段类型从 List<String> 改为 String
    private String keyword;

    // --- 新增字段 ---
    @JsonProperty("user_id")
    private String userId;
}