package com.lusheng.ragchat.dto;

public class CompletionRequest {
    private String question;
    private String session_id;
    private boolean stream = true; // 默认总是流式

    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getSession_id() { return session_id; }
    public void setSession_id(String session_id) { this.session_id = session_id; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
}
