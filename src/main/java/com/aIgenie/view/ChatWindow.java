package com.aIgenie.view;

import com.aIgenie.controller.ChatController;
import com.aIgenie.model.ChatMessage;
import com.aIgenie.util.DockableWindowBehavior;
import com.aIgenie.view.components.ChatPanel;
import com.aIgenie.view.components.MessageInputPanel;
import com.aIgenie.view.components.TitlePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * 聊天窗口主类
 */
public class ChatWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(ChatWindow.class);

    private final ChatController controller;
    private ChatPanel chatPanel;
    private MessageInputPanel inputPanel;
    private TitlePanel titlePanel;
    private DockableWindowBehavior dockBehavior;

    public ChatWindow(ChatController controller) {
        this.controller = controller;

        setupWindow();
        initComponents();
        layoutComponents();

        dockBehavior = new DockableWindowBehavior(this, titlePanel);
        ((JComponent) getContentPane()).putClientProperty("dockBehavior", dockBehavior);
    }

    private void setupWindow() {
        setTitle("AIgenie");
        setSize(350, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        titlePanel = new TitlePanel("AIgenie");
        chatPanel = new ChatPanel();
        inputPanel = new MessageInputPanel();

        inputPanel.setOnSendListener(controller::sendMessage);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        add(titlePanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    public void displayMessage(ChatMessage message) {
        try {
            chatPanel.addMessage(message);
        } catch (Exception e) {
            logger.error("显示消息时发生错误", e);
        }
    }

    public void displayTypingIndicator(ChatMessage typingMessage) {
        try {
            chatPanel.showTypingIndicator(typingMessage);
        } catch (Exception e) {
            logger.error("显示输入指示器时发生错误", e);
        }
    }

    public void removeTypingIndicator() {
        SwingUtilities.invokeLater(() -> {
            try {
                chatPanel.removeTypingIndicator();
            } catch (Exception e) {
                logger.error("移除输入指示器时发生错误", e);
            }
        });
    }

    public void display() {
        setVisible(true);
    }

    /**
     * 显示流式消息
     *
     * @param autoScroll 是否自动滚动到底部
     */
    public void displayStreamingMessage(ChatMessage message, boolean autoScroll) {
        try {
            chatPanel.showStreamingMessage(message, autoScroll);
        } catch (Exception e) {
            logger.error("显示流式消息时出错", e);
        }
    }

    /**
     * 更新流式消息内容
     *
     * @param autoScroll 是否自动滚动到底部
     */
    public void updateStreamingMessage(ChatMessage message, boolean autoScroll) {
        try {
            chatPanel.updateStreamingMessage(message, autoScroll);
        } catch (Exception e) {
            logger.error("更新流式消息时出错", e);
        }
    }

    public void displayStreamingMessage(ChatMessage message) {
        displayStreamingMessage(message, false);
    }

    public void updateStreamingMessage(ChatMessage message) {
        updateStreamingMessage(message, false);
    }

    /**
     * 完成流式消息
     */
    public void finalizeStreamingMessage(ChatMessage message) {
        try {
            chatPanel.finalizeStreamingMessage(message);
        } catch (Exception e) {
            logger.error("完成流式消息时出错", e);
        }
    }
}
