package com.aIgenie.service.impl;

import com.aIgenie.service.AIService;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI服务实现类
 */
@Service
public class AIServiceImpl implements AIService {
    
    private final ChatClient chatClient;
    private final List<Message> messageHistory;
    private final String systemPrompt = "你是一个有用的AI助手，名为'AIgenie'。请简洁明了地回答用户的问题。";
    
    @Autowired
    public AIServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.messageHistory = new ArrayList<>();
        // 添加系统提示到历史记录
        this.messageHistory.add(new SystemPromptTemplate(systemPrompt).createMessage());
        System.out.println("标准AI服务初始化完成");
    }
    
    @Override
    public String sendMessage(String message) {
        System.out.println("\n====== 开始标准AI调用 ======");
        System.out.println("消息: " + message);
        
        // 创建用户消息并添加到历史记录
        UserMessage userMessage = new UserMessage(message);
        messageHistory.add(userMessage);
        
        // 创建包含历史记录的提示
        Prompt prompt = new Prompt(new ArrayList<>(messageHistory));
        
        // 发送请求并获取回复
        System.out.println("发送请求到Spring AI...");
        ChatResponse response = chatClient.call(prompt);
        String aiResponse = response.getResult().getOutput().getContent();
        System.out.println("收到回复: " + aiResponse);
        
        // 将AI回复添加到历史记录
        messageHistory.add(response.getResult().getOutput());
        System.out.println("====== 标准AI调用结束 ======\n");
        
        return aiResponse;
    }
    
    @Override
    public CompletableFuture<String> sendMessageAsync(String message) {
        return CompletableFuture.supplyAsync(() -> sendMessage(message));
    }
} 