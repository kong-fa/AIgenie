package com.aIgenie.view.components;

import com.aIgenie.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * 聊天内容显示面板
 * 实际消息列表由 {@link MessageRenderer} 维护，本类只负责滚动容器与转发调用。
 */
public class ChatPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ChatPanel.class);

    private final MessageRenderer messageRenderer;
    private final JScrollPane scrollPane;
    private ChatMessage typingIndicator;
    private ChatMessage streamingMessage;

    public ChatPanel() {
        setLayout(new BorderLayout());

        messageRenderer = new MessageRenderer();

        scrollPane = new JScrollPane(messageRenderer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        // 提升滚动手感
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        try {
            logger.debug("添加新消息: {}", message);
            messageRenderer.addMessage(message);
        } catch (Exception e) {
            logger.error("添加消息出错", e);
        }
    }

    /**
     * 显示正在输入提示
     */
    public void showTypingIndicator(ChatMessage indicator) {
        try {
            this.typingIndicator = indicator;
            messageRenderer.setTypingIndicator(indicator);
        } catch (Exception e) {
            logger.error("显示输入提示出错", e);
        }
    }

    /**
     * 移除正在输入提示
     */
    public void removeTypingIndicator() {
        try {
            if (this.typingIndicator != null) {
                this.typingIndicator = null;
                messageRenderer.clearTypingIndicator();
            }
        } catch (Exception e) {
            logger.error("移除输入提示出错", e);
        }
    }

    /**
     * 显示流式消息
     */
    public void showStreamingMessage(ChatMessage message, boolean autoScroll) {
        try {
            this.streamingMessage = message;
            messageRenderer.setStreamingMessage(message, autoScroll);
        } catch (Exception e) {
            logger.error("显示流式消息时出错", e);
        }
    }

    public void showStreamingMessage(ChatMessage message) {
        showStreamingMessage(message, false);
    }

    /**
     * 更新流式消息
     */
    public void updateStreamingMessage(ChatMessage message, boolean autoScroll) {
        try {
            if (this.streamingMessage == message) {
                messageRenderer.updateStreamingMessage(message, autoScroll);
            }
        } catch (Exception e) {
            logger.error("更新流式消息时出错", e);
        }
    }

    public void updateStreamingMessage(ChatMessage message) {
        updateStreamingMessage(message, false);
    }

    /**
     * 完成流式消息
     */
    public void finalizeStreamingMessage(ChatMessage message) {
        try {
            if (this.streamingMessage == message) {
                messageRenderer.finalizeStreamingMessage(message);
                this.streamingMessage = null;
            }
        } catch (Exception e) {
            logger.error("完成流式消息时出错", e);
        }
    }
}
