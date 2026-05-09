package com.aIgenie.service.impl;

import com.aIgenie.service.AIService;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 Spring AI {@link ChatClient} 的标准 AI 服务实现。
 * 仅在 {@code aigenie.use-custom-client=false} 时启用，
 * 与 {@link CustomAIServiceImpl} 互斥，避免重复注册。
 */
@Service
@ConditionalOnProperty(name = "aigenie.use-custom-client", havingValue = "false")
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    private final ChatClient chatClient;
    private final List<Message> messageHistory;
    private final String systemPrompt = "你是一个有用的AI助手，名为'AIgenie'。请简洁明了地回答用户的问题。";

    @Autowired
    public AIServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.messageHistory = Collections.synchronizedList(new ArrayList<>());
        this.messageHistory.add(new SystemPromptTemplate(systemPrompt).createMessage());
        logger.info("标准AI服务初始化完成");
    }

    @Override
    public synchronized String sendMessage(String message) {
        logger.debug("开始标准AI调用，消息: {}", message);

        UserMessage userMessage = new UserMessage(message);
        messageHistory.add(userMessage);

        Prompt prompt;
        synchronized (messageHistory) {
            prompt = new Prompt(new ArrayList<>(messageHistory));
        }

        try {
            ChatResponse response = chatClient.call(prompt);
            String aiResponse = response.getResult().getOutput().getContent();
            logger.debug("收到回复，长度: {}", aiResponse == null ? 0 : aiResponse.length());

            messageHistory.add(response.getResult().getOutput());
            return aiResponse;
        } catch (Exception e) {
            // 调用失败时回滚刚刚添加的用户消息，避免历史污染
            messageHistory.remove(userMessage);
            logger.error("标准AI调用失败", e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<String> sendMessageAsync(String message) {
        return CompletableFuture.supplyAsync(() -> sendMessage(message));
    }
} 