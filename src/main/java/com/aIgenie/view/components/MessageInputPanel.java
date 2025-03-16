package com.aIgenie.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * 消息输入面板
 */
public class MessageInputPanel extends JPanel {
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
        if (onSendListener != null) {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                System.out.println("用户输入了消息: " + message);
                onSendListener.accept(message);
                messageField.setText("");
            } else {
                System.out.println("用户尝试发送空消息，已忽略");
            }
        } else {
            System.out.println("警告: 没有设置消息发送监听器");
        }
    }
    
    public void setOnSendListener(Consumer<String> listener) {
        this.onSendListener = listener;
    }
} 