package com.aIgenie.service;

import java.util.concurrent.CompletableFuture;

/**
 * AI服务接口
 */
public interface AIService {
    
    /**
     * 发送消息到AI并获取回复
     * @param message 用户消息
     * @return AI的回复
     */
    String sendMessage(String message);
    
    /**
     * 异步发送消息到AI并获取回复
     * @param message 用户消息
     * @return 包含AI回复的CompletableFuture
     */
    CompletableFuture<String> sendMessageAsync(String message);
} 