package com.aIgenie.view.components;

import com.aIgenie.model.ChatMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天内容显示面板 - 重新实现
 */
public class ChatPanel extends JPanel {
    private MessageRenderer messageRenderer;
    private JScrollPane scrollPane;
    private ChatMessage typingIndicator;
    private List<ChatMessage> messages = new ArrayList<>();
    private boolean needsRedraw = false;
    private ChatMessage streamingMessage;
    
    public ChatPanel() {
        setLayout(new BorderLayout());
        
        // 创建消息渲染器
        messageRenderer = new MessageRenderer();
        
        // 添加滚动条 - 只保留垂直滚动条
        scrollPane = new JScrollPane(messageRenderer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 禁用水平滚动条
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 添加大小变化监听器，在面板大小变化时重新渲染
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshAllMessages();
            }
        });
        
        // 为了调试，添加边框
        setBorder(BorderFactory.createLineBorder(Color.RED, 1));
    }
    
    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        try {
            System.out.println("添加新消息: " + message);
            messageRenderer.addMessage(message);
            System.out.println("消息已添加");
        } catch (Exception e) {
            System.err.println("添加消息出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示正在输入提示
     */
    public void showTypingIndicator(ChatMessage indicator) {
        try {
            System.out.println("显示输入提示: " + indicator);
            this.typingIndicator = indicator;
            refreshAllMessages();
        } catch (Exception e) {
            System.err.println("显示输入提示出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 移除正在输入提示
     */
    public void removeTypingIndicator() {
        try {
            System.out.println("移除输入提示");
            if (this.typingIndicator != null) {
                this.typingIndicator = null;
                messageRenderer.clearTypingIndicator();
            }
        } catch (Exception e) {
            System.err.println("移除输入提示出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 刷新所有消息显示
     */
    private void refreshAllMessages() {
        try {
            System.out.println("重新加载所有消息");
            
            // 不再需要手动构建消息，直接将消息添加到渲染器即可
            for (ChatMessage msg : messages) {
                messageRenderer.addMessage(msg);
            }
            
            // 设置输入指示器
            if (typingIndicator != null) {
                messageRenderer.setTypingIndicator(typingIndicator);
            } else {
                messageRenderer.clearTypingIndicator();
            }
            
        } catch (Exception e) {
            System.err.println("刷新消息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示流式消息
     */
    public void showStreamingMessage(ChatMessage message, boolean autoScroll) {
        try {
            System.out.println("显示流式消息: " + message);
            this.streamingMessage = message;
            messageRenderer.setStreamingMessage(message, autoScroll);
        } catch (Exception e) {
            System.err.println("显示流式消息时出错: " + e.getMessage());
            e.printStackTrace();
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
            // 确保是同一消息
            if (this.streamingMessage == message) {
                messageRenderer.updateStreamingMessage(message, autoScroll);
            }
        } catch (Exception e) {
            System.err.println("更新流式消息时出错: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("完成流式消息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 如果需要重绘，进行刷新
        if (needsRedraw) {
            refreshAllMessages();
            needsRedraw = false;
        }
    }
} 