package com.aIgenie.model;

import java.util.Date;

/**
 * 聊天消息模型
 */
public class ChatMessage {
    private String sender;
    private String content;
    private Date timestamp;
    
    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = new Date();
    }
    
    public String getSender() {
        return sender;
    }
    
    public String getContent() {
        return content;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * 追加内容到消息
     */
    public void appendContent(String chunk) {
        this.content += chunk;
    }
    
    @Override
    public String toString() {
        return sender + ": " + content;
    }
} 