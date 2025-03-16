package com.aIgenie.controller;

import com.aIgenie.model.ChatMessage;
import com.aIgenie.service.AIService;
import com.aIgenie.service.RequestResponseListener;
import com.aIgenie.view.ChatWindow;
import com.aIgenie.service.impl.CustomAIServiceImpl;
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
    private ChatWindow chatWindow;
    private List<ChatMessage> messageHistory;
    private AIService aiService;
    private ChatMessage currentStreamingMessage = null;
    
    @Autowired
    public ChatController(AIService aiService) {
        this.messageHistory = new ArrayList<>();
        this.aiService = aiService;
        System.out.println("ChatController 初始化，AI服务: " + (aiService != null ? aiService.getClass().getSimpleName() : "null"));
    }
    
    // 无参构造函数，用于不使用Spring时的手动初始化
    public ChatController() {
        this.messageHistory = new ArrayList<>();
        System.out.println("ChatController 初始化(无AI服务)");
    }
    
    public void setAiService(AIService aiService) {
        System.out.println("设置AI服务: " + aiService.getClass().getSimpleName());
        this.aiService = aiService;
    }
    
    public void initialize() {
        System.out.println("初始化聊天窗口...");
        chatWindow = new ChatWindow(this);
        
        // 如果是自定义服务，设置流式响应监听器
        if (aiService instanceof CustomAIServiceImpl) {
            CustomAIServiceImpl customService = (CustomAIServiceImpl) aiService;
            
            // 添加流式响应监听器
            customService.addStreamListener(chunk -> {
                SwingUtilities.invokeLater(() -> {
                    // 如果当前没有流式消息，创建一个并只在第一次时滚动
                    if (currentStreamingMessage == null) {
                        currentStreamingMessage = new ChatMessage("AIgenie", "");
                        // 第一次显示时标记不要自动滚动
                        chatWindow.displayStreamingMessage(currentStreamingMessage, false);
                    } else {
                        // 追加内容到当前消息
                        currentStreamingMessage.appendContent(chunk);
                        
                        // 更新显示但不强制滚动
                        chatWindow.updateStreamingMessage(currentStreamingMessage, false);
                    }
                });
            });
        }
        
        chatWindow.display();
        System.out.println("聊天窗口显示完成");
    }
    
    public void sendMessage(String content) {
        if (content != null && !content.trim().isEmpty()) {
            System.out.println("\n====== 处理用户消息 ======");
            System.out.println("用户输入: " + content);
            
            // 创建用户消息并显示
            final ChatMessage userMessage = new ChatMessage("我", content);
            messageHistory.add(userMessage);
            
            SwingUtilities.invokeLater(() -> {
                chatWindow.displayMessage(userMessage);
            });
            
            if (aiService != null) {
                // 重置当前流式消息
                currentStreamingMessage = null;
                
                // 使用AI服务获取回复
                System.out.println("调用AI服务...");
                CompletableFuture<String> future = aiService.sendMessageAsync(content);
                
                future.thenAccept(reply -> {
                    System.out.println("AI回复完成: " + reply);
                    
                    SwingUtilities.invokeLater(() -> {
                        // 流式响应完成，将当前流式消息标记为完成
                        if (currentStreamingMessage != null) {
                            // 已经有流式消息在显示中，更新它
                            currentStreamingMessage.setContent(reply);
                            chatWindow.finalizeStreamingMessage(currentStreamingMessage);
                            messageHistory.add(currentStreamingMessage);
                        } else {
                            // 没有流式消息，创建一个新消息并显示
                            ChatMessage aiMessage = new ChatMessage("AIgenie", reply);
                            messageHistory.add(aiMessage);
                            chatWindow.displayMessage(aiMessage);
                        }
                        
                        // 重置当前流式消息
                        currentStreamingMessage = null;
                        
                        System.out.println("====== 消息处理完成 ======\n");
                    });
                });
            } else {
                // 模拟响应处理...
                System.out.println("AI服务不可用，使用模拟回复");
                ChatMessage replyMessage = new ChatMessage("AIgenie", "收到你的消息: " + content);
                messageHistory.add(replyMessage);
                chatWindow.displayMessage(replyMessage);
            }
        } else {
            System.out.println("空消息，忽略");
        }
    }
    
    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }
} 