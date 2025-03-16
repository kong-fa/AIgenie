package com.aIgenie.view;

import com.aIgenie.controller.ChatController;
import com.aIgenie.model.ChatMessage;
import com.aIgenie.util.DockableWindowBehavior;
import com.aIgenie.view.components.ChatPanel;
import com.aIgenie.view.components.MessageInputPanel;
import com.aIgenie.view.components.TitlePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 聊天窗口主类
 */
public class ChatWindow extends JFrame {
    private ChatController controller;
    private ChatPanel chatPanel;
    private MessageInputPanel inputPanel;
    private TitlePanel titlePanel;
    private DockableWindowBehavior dockBehavior;
    
    public ChatWindow(ChatController controller) {
        this.controller = controller;
        
        // 设置窗口基本属性
        setupWindow();
        
        // 创建和初始化组件
        initComponents();
        
        // 布局组件
        layoutComponents();
        
        // 添加窗口停靠行为
        dockBehavior = new DockableWindowBehavior(this, titlePanel);
        // 将实例保存为窗口属性，以便后续访问
        ((JComponent)getContentPane()).putClientProperty("dockBehavior", dockBehavior);
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
        
        // 添加发送消息的动作监听器
        inputPanel.setOnSendListener(message -> controller.sendMessage(message));
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // 添加调试边框
        titlePanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
        chatPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
        inputPanel.setBorder(BorderFactory.createLineBorder(Color.MAGENTA, 1));
        
        add(titlePanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        
        // 打印组件树
        System.out.println("组件树:");
        printComponentHierarchy(this, 0);
    }
    
    // 添加用于调试的组件树打印方法
    private void printComponentHierarchy(Container container, int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }
        
        System.out.println(indent + container.getClass().getSimpleName() + 
                          " [" + container.getWidth() + "x" + container.getHeight() + "]");
        
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                printComponentHierarchy((Container) child, level + 1);
            } else {
                System.out.println(indent + "  " + child.getClass().getSimpleName() + 
                                  " [" + child.getWidth() + "x" + child.getHeight() + "]");
            }
        }
    }
    
    public void displayMessage(ChatMessage message) {
        try {
            System.out.println("开始显示消息: " + message);
            chatPanel.addMessage(message);
            System.out.println("消息显示完成");
        } catch (Exception e) {
            System.err.println("显示消息时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void displayTypingIndicator(ChatMessage typingMessage) {
        try {
            System.out.println("开始显示输入指示器: " + typingMessage);
            chatPanel.showTypingIndicator(typingMessage);
            System.out.println("输入指示器显示完成");
        } catch (Exception e) {
            System.err.println("显示输入指示器时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void removeTypingIndicator() {
        try {
            System.out.println("开始移除输入指示器");
            SwingUtilities.invokeLater(() -> {
                try {
                    chatPanel.removeTypingIndicator();
                    System.out.println("输入指示器移除完成");
                } catch (Exception e) {
                    System.err.println("移除输入指示器时发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("调度移除输入指示器时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void display() {
        setVisible(true);
    }
    
    /**
     * 显示流式消息
     * @param autoScroll 是否自动滚动到底部
     */
    public void displayStreamingMessage(ChatMessage message, boolean autoScroll) {
        try {
            System.out.println("开始显示流式消息: " + message);
            chatPanel.showStreamingMessage(message, autoScroll);
        } catch (Exception e) {
            System.err.println("显示流式消息时出错: " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * 更新流式消息内容
     * @param autoScroll 是否自动滚动到底部
     */
    public void updateStreamingMessage(ChatMessage message, boolean autoScroll) {
        try {
            chatPanel.updateStreamingMessage(message, autoScroll);
        } catch (Exception e) {
            System.err.println("更新流式消息时出错: " + e);
            e.printStackTrace();
        }
    }
    
    // 为了兼容，保留原始方法，默认不滚动
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
            System.out.println("完成流式消息: " + message);
            chatPanel.finalizeStreamingMessage(message);
        } catch (Exception e) {
            System.err.println("完成流式消息时出错: " + e);
            e.printStackTrace();
        }
    }
} 