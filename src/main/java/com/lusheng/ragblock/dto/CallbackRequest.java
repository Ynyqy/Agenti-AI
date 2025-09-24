package com.lusheng.ragblock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackRequest {
    private String answer;

    // 字段名改为 "documents"，其类型变为一个包含详细信息的对象列表
    private List<DocumentInfo> documents;

    private String keyword; // <-- 1. 在这里添加 keyword 字段

    // --- 新增字段 ---
    @JsonProperty("user_id")
    private String userId;


    /**
     * 内部类，用于封装每个文档的详细信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo {
        // 字段名与 Python 接收方保持一致
        private String docName;
        private String pdfUrl;
    }
}