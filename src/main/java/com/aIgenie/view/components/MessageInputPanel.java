package com.aIgenie.view.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * 消息输入面板
 */
public class MessageInputPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MessageInputPanel.class);

    private JTextField messageField;
    private JButton sendButton;
    private Consumer<String> onSendListener;
    
    public MessageInputPanel() {
        setLayout(new BorderLayout(5, 0));
        
        // 创建消息输入框
        messageField = new JTextField();
        messageField.setFont(new Font("Dialog", Font.PLAIN, 14));
        
        // 创建发送按钮
        sendButton = new JButton("发送");
        sendButton.setFocusPainted(false);
        
        // 添加组件
        add(messageField, BorderLayout.CENTER);
        add(sendButton, BorderLayout.EAST);
        
        // 添加事件监听器
        setupEventListeners();
    }
    
    private void setupEventListeners() {
        // 发送按钮点击事件
        sendButton.addActionListener(e -> sendMessage());
        
        // 输入框回车事件
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
    }
    
    private void sendMessage() {
        if (!sendButton.isEnabled()) {
            // 上一条 AI 响应仍在进行中，禁止再发新消息
            return;
        }
        if (onSendListener == null) {
            logger.warn("没有设置消息发送监听器");
            return;
        }
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            logger.debug("用户尝试发送空消息，已忽略");
            return;
        }
        logger.debug("用户输入了消息: {}", message);
        onSendListener.accept(message);
        messageField.setText("");
    }

    /**
     * 启用或禁用消息输入。
     * AI 响应进行中应禁用，避免出现并发请求导致流式回复交错、对话历史错乱。
     */
    public void setInputEnabled(boolean enabled) {
        messageField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        sendButton.setText(enabled ? "发送" : "等待中...");
    }
    
    public void setOnSendListener(Consumer<String> listener) {
        this.onSendListener = listener;
    }
} 