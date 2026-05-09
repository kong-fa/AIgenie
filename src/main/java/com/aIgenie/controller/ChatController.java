package com.aIgenie.controller;

import com.aIgenie.model.ChatMessage;
import com.aIgenie.service.AIService;
import com.aIgenie.view.ChatWindow;
import com.aIgenie.service.impl.CustomAIServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

/**
 * 聊天控制器
 *
 * 并发模型：
 * 通过 {@link #requestInFlight} 闸门保证同一时间只有一条 AI 请求在执行，
 * 进行中会通过 {@link ChatWindow#setInputEnabled(boolean)} 禁用输入栏，
 * 避免多条流式响应交错、对话上下文错乱。
 * 所有 UI 状态（包括 {@link #currentStreamingMessage}）只在 EDT 上读写。
 */
@Component
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private ChatWindow chatWindow;
    private final List<ChatMessage> messageHistory;
    private AIService aiService;

    /** 仅在 EDT 上访问。 */
    private ChatMessage currentStreamingMessage = null;

    /** 是否有 AI 请求在进行中，用于阻止并发发送。 */
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);

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

            customService.addStreamListener(chunk -> SwingUtilities.invokeLater(() -> {
                // 没有进行中的请求时，忽略迟到的 chunk（防御保护）
                if (!requestInFlight.get()) {
                    return;
                }
                // 第一次接收到 chunk 时，创建消息并把当前 chunk 作为初始内容
                if (currentStreamingMessage == null) {
                    currentStreamingMessage = new ChatMessage("AIgenie", chunk == null ? "" : chunk);
                    chatWindow.displayStreamingMessage(currentStreamingMessage, false);
                } else {
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

        // 闸门：进行中的请求不允许并发，避免流式回复混乱
        if (!requestInFlight.compareAndSet(false, true)) {
            logger.debug("已有 AI 请求在进行中，忽略新发送");
            return;
        }

        logger.info("处理用户消息: {}", content);

        final ChatMessage userMessage = new ChatMessage("我", content);
        messageHistory.add(userMessage);

        SwingUtilities.invokeLater(() -> {
            chatWindow.displayMessage(userMessage);
            chatWindow.setInputEnabled(false);
        });

        if (aiService == null) {
            logger.warn("AI服务不可用，使用模拟回复");
            ChatMessage replyMessage = new ChatMessage("AIgenie", "收到你的消息: " + content);
            messageHistory.add(replyMessage);
            SwingUtilities.invokeLater(() -> {
                chatWindow.displayMessage(replyMessage);
                finishRequest();
            });
            return;
        }

        // 重置当前流式消息（仍在 EDT 之外，但接下来所有写入都在 EDT 上做）
        SwingUtilities.invokeLater(() -> currentStreamingMessage = null);

        logger.debug("调用AI服务...");
        CompletableFuture<String> future = aiService.sendMessageAsync(content);

        future.whenComplete((reply, error) -> SwingUtilities.invokeLater(() -> {
            try {
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
                    return;
                }

                logger.info("AI回复完成，长度: {}", reply == null ? 0 : reply.length());

                if (currentStreamingMessage != null) {
                    currentStreamingMessage.setContent(reply);
                    chatWindow.finalizeStreamingMessage(currentStreamingMessage);
                    messageHistory.add(currentStreamingMessage);
                } else {
                    ChatMessage aiMessage = new ChatMessage("AIgenie", reply);
                    messageHistory.add(aiMessage);
                    chatWindow.displayMessage(aiMessage);
                }
            } finally {
                finishRequest();
            }
        }));
    }

    /**
     * 重置进行中的状态并恢复输入。必须在 EDT 上调用。
     */
    private void finishRequest() {
        currentStreamingMessage = null;
        requestInFlight.set(false);
        if (chatWindow != null) {
            chatWindow.setInputEnabled(true);
        }
    }

    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }
}
