package com.aIgenie.service.impl;

import com.aIgenie.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 Spring AI 1.0 {@link ChatClient} 的标准 AI 服务实现。
 * 仅在 {@code aigenie.use-custom-client=false} 时启用，
 * 与 {@link CustomAIServiceImpl} 互斥，避免重复注册。
 */
@Service
@ConditionalOnProperty(name = "aigenie.use-custom-client", havingValue = "false")
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个有用的AI助手，名为'AIgenie'。请简洁明了地回答用户的问题。";

    private final ChatClient chatClient;
    private final List<Message> messageHistory;

    @Autowired
    public AIServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.messageHistory = Collections.synchronizedList(new ArrayList<>());
        this.messageHistory.add(new SystemMessage(DEFAULT_SYSTEM_PROMPT));
        logger.info("标准AI服务初始化完成 (Spring AI 1.0 ChatClient)");
    }

    @Override
    public synchronized String sendMessage(String message) {
        logger.debug("开始标准AI调用，长度: {}", message == null ? 0 : message.length());
        logger.trace("标准AI调用原文: {}", message);

        UserMessage userMessage = new UserMessage(message);
        messageHistory.add(userMessage);

        Prompt prompt;
        synchronized (messageHistory) {
            prompt = new Prompt(new ArrayList<>(messageHistory));
        }

        try {
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            AssistantMessage assistantMessage = response.getResult().getOutput();
            String aiResponse = assistantMessage.getText();
            logger.debug("收到回复，长度: {}", aiResponse == null ? 0 : aiResponse.length());

            messageHistory.add(assistantMessage);
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
