package com.aIgenie.service.impl;

import com.aIgenie.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final SystemMessage systemMessage;
    private final int maxHistoryGroups;

    @Autowired
    public AIServiceImpl(
            ChatClient.Builder chatClientBuilder,
            @Value("${aigenie.system-prompt:" + DEFAULT_SYSTEM_PROMPT + "}") String systemPrompt,
            @Value("${aigenie.chat-history-limit:10}") int historyLimit) {
        this.chatClient = chatClientBuilder.build();
        this.messageHistory = Collections.synchronizedList(new ArrayList<>());
        this.systemMessage = new SystemMessage(systemPrompt);
        this.maxHistoryGroups = historyLimit;
        logger.info("标准AI服务初始化完成 (Spring AI 1.0 ChatClient), historyLimit={}", historyLimit);
    }

    @Override
    public synchronized String sendMessage(String message) {
        logger.debug("开始标准AI调用，长度: {}", message == null ? 0 : message.length());
        logger.trace("标准AI调用原文: {}", message);

        UserMessage userMessage = new UserMessage(message);
        messageHistory.add(userMessage);
        trimHistory();

        Prompt prompt = buildPrompt();

        try {
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            AssistantMessage assistantMessage = response.getResult().getOutput();
            String aiResponse = assistantMessage.getText();
            logger.debug("收到回复，长度: {}", aiResponse == null ? 0 : aiResponse.length());

            messageHistory.add(assistantMessage);
            trimHistory();
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

    /**
     * 构造发送给模型的 Prompt：始终把当前 systemMessage 放在最前，
     * 后面跟最近的 user / assistant 消息历史。
     */
    private Prompt buildPrompt() {
        List<Message> snapshot;
        synchronized (messageHistory) {
            snapshot = new ArrayList<>(messageHistory.size() + 1);
            snapshot.add(systemMessage);
            snapshot.addAll(messageHistory);
        }
        return new Prompt(snapshot);
    }

    /**
     * 限制对话历史长度，避免在长会话中无限增长导致 token 浪费 / 内存膨胀。
     * 按"轮"算：每轮包含一对 user + assistant 消息，所以保留消息数 = limit * 2。
     * SystemMessage 不进入历史列表，因此不受 trim 影响。
     */
    private void trimHistory() {
        if (maxHistoryGroups <= 0) {
            return;
        }
        synchronized (messageHistory) {
            int maxMessages = maxHistoryGroups * 2;
            while (messageHistory.size() > maxMessages) {
                messageHistory.remove(0);
            }
            // 修剪后第一条不应是 assistant，否则上下文以"AI 凭空回复"开头会破坏对话连贯
            while (!messageHistory.isEmpty()
                    && messageHistory.get(0).getMessageType() == MessageType.ASSISTANT) {
                messageHistory.remove(0);
            }
        }
    }
}
