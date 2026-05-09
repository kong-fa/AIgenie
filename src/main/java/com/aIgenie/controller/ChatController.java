package com.aIgenie.controller;

import com.aIgenie.model.ChatMessage;
import com.aIgenie.service.AIService;
import com.aIgenie.service.RequestResponseListener;
import com.aIgenie.view.ChatWindow;
import com.aIgenie.service.impl.CustomAIServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingUtilities;

/**
 * 聊天控制器
 */
@Component
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private ChatWindow chatWindow;
    private final List<ChatMessage> messageHistory;
    private AIService aiService;
    private ChatMessage currentStreamingMessage = null;

    @Autowired
    public ChatController(AIService aiService) {
        this.messageHistory = new ArrayList<>();
        this.aiService = aiService;
        logger.info("ChatController 初始化，AI服务: {}",
                aiService != null ? aiService.getClass().getSimpleName() : "null");
    }

    // 无参构造函数，用于不使用Spring时的手动初始化
    public ChatController() {
        this.messageHistory = new ArrayList<>();
        logger.info("ChatController 初始化(无AI服务)");
    }

    public void setAiService(AIService aiService) {
        logger.info("设置AI服务: {}", aiService.getClass().getSimpleName());
        this.aiService = aiService;
    }

    public void initialize() {
        logger.info("初始化聊天窗口...");
        chatWindow = new ChatWindow(this);

        // 如果是自定义服务，设置流式响应监听器
        if (aiService instanceof CustomAIServiceImpl) {
            CustomAIServiceImpl customService = (CustomAIServiceImpl) aiService;

            // 添加流式响应监听器
            customService.addStreamListener(chunk -> SwingUtilities.invokeLater(() -> {
                // 第一次接收到 chunk 时，创建消息并把当前 chunk 作为初始内容
                if (currentStreamingMessage == null) {
                    currentStreamingMessage = new ChatMessage("AIgenie", chunk == null ? "" : chunk);
                    // 第一次显示时不自动滚动，避免打断用户阅读
                    chatWindow.displayStreamingMessage(currentStreamingMessage, false);
                } else {
                    // 后续 chunk 追加到现有消息
                    currentStreamingMessage.appendContent(chunk);
                    chatWindow.updateStreamingMessage(currentStreamingMessage, false);
                }
            }));
        }

        chatWindow.display();
        logger.info("聊天窗口显示完成");
    }

    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            logger.debug("空消息，忽略");
            return;
        }

        logger.info("处理用户消息: {}", content);

        // 创建用户消息并显示
        final ChatMessage userMessage = new ChatMessage("我", content);
        messageHistory.add(userMessage);

        SwingUtilities.invokeLater(() -> chatWindow.displayMessage(userMessage));

        if (aiService == null) {
            logger.warn("AI服务不可用，使用模拟回复");
            ChatMessage replyMessage = new ChatMessage("AIgenie", "收到你的消息: " + content);
            messageHistory.add(replyMessage);
            SwingUtilities.invokeLater(() -> chatWindow.displayMessage(replyMessage));
            return;
        }

        // 重置当前流式消息
        currentStreamingMessage = null;

        logger.debug("调用AI服务...");
        CompletableFuture<String> future = aiService.sendMessageAsync(content);

        future.whenComplete((reply, error) -> SwingUtilities.invokeLater(() -> {
            if (error != null) {
                logger.error("AI回复失败", error);
                String errorText = "抱歉，我遇到了一个问题: " + error.getMessage();
                if (currentStreamingMessage != null) {
                    currentStreamingMessage.setContent(errorText);
                    chatWindow.finalizeStreamingMessage(currentStreamingMessage);
                    messageHistory.add(currentStreamingMessage);
                } else {
                    ChatMessage errorMessage = new ChatMessage("AIgenie", errorText);
                    messageHistory.add(errorMessage);
                    chatWindow.displayMessage(errorMessage);
                }
                currentStreamingMessage = null;
                return;
            }

            logger.info("AI回复完成，长度: {}", reply == null ? 0 : reply.length());

            if (currentStreamingMessage != null) {
                // 用最终完整内容替换流式累积的内容，避免不一致
                currentStreamingMessage.setContent(reply);
                chatWindow.finalizeStreamingMessage(currentStreamingMessage);
                messageHistory.add(currentStreamingMessage);
            } else {
                ChatMessage aiMessage = new ChatMessage("AIgenie", reply);
                messageHistory.add(aiMessage);
                chatWindow.displayMessage(aiMessage);
            }

            currentStreamingMessage = null;
        }));
    }
    
    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }
} 