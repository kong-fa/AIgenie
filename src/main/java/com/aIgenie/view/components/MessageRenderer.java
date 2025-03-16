package com.aIgenie.view.components;

import com.aIgenie.model.ChatMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息渲染组件 - 替代JTextArea
 */
public class MessageRenderer extends JPanel {
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatMessage typingIndicator = null;
    private JTextArea debugTextArea; // 用于兼容旧代码
    private ChatMessage streamingMessage;
    private JPanel streamingMessagePanel;
    
    public MessageRenderer() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(240, 240, 240));
        
        // 创建一个隐藏的调试文本区域，用于兼容旧代码
        debugTextArea = new JTextArea();
        debugTextArea.setVisible(false);
        
        // 添加组件调整大小的监听器
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 当组件大小改变时，调整所有子组件的最大宽度
                int width = getWidth();
                if (width > 0) {
                    for (Component comp : getComponents()) {
                        if (comp instanceof JPanel) {
                            comp.setMaximumSize(new Dimension(width - 10, comp.getPreferredSize().height));
                        }
                    }
                }
                revalidate();
                repaint();
            }
        });
    }
    
    /**
     * 设置文本内容 (兼容方法)
     */
    public void setText(String text) {
        System.out.println("MessageRenderer.setText 被调用: " + text);
        // 解析文本并转换为消息
        messages.clear();
        
        if (text != null && !text.isEmpty()) {
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    // 简单的解析，假设每行格式为: "发送者: 内容"
                    int colonIndex = line.indexOf(": ");
                    if (colonIndex > 0) {
                        String sender = line.substring(0, colonIndex);
                        String content = line.substring(colonIndex + 2);
                        messages.add(new ChatMessage(sender, content));
                    }
                }
            }
        }
        
        refreshDisplay();
    }
    
    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        refreshDisplay();
    }
    
    /**
     * 设置输入提示
     */
    public void setTypingIndicator(ChatMessage indicator) {
        this.typingIndicator = indicator;
        refreshDisplay();
    }
    
    /**
     * 清除输入提示
     */
    public void clearTypingIndicator() {
        this.typingIndicator = null;
        refreshDisplay();
    }
    
    /**
     * 滚动到底部 (公开方法)
     */
    public void scrollToBottom() {
        Container parent = getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof JScrollPane) {
            final JScrollPane scrollPane = (JScrollPane) parent;
            SwingUtilities.invokeLater(() -> {
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                verticalBar.setValue(verticalBar.getMaximum());
                // System.out.println("滚动到底部完成");
            });
        } else {
            System.out.println("找不到父滚动面板");
        }
    }
    
    /**
     * 设置流式消息
     */
    public void setStreamingMessage(ChatMessage message, boolean autoScroll) {
        this.streamingMessage = message;
        refreshDisplay(autoScroll);
    }

    public void setStreamingMessage(ChatMessage message) {
        setStreamingMessage(message, false);
    }
    
    /**
     * 更新流式消息内容
     */
    public void updateStreamingMessage(ChatMessage message, boolean autoScroll) {
        // 只需更新面板内容，不需要完全重绘
        if (streamingMessagePanel != null) {
            // 保存当前滚动位置
            int currentScrollPosition = autoScroll ? -1 : getCurrentScrollPosition();
            
            // 找到内容文本区域并更新
            for (Component comp : streamingMessagePanel.getComponents()) {
                if (comp instanceof JTextArea) {
                    JTextArea textArea = (JTextArea)comp;
                    textArea.setText(message.getContent());
                    break;
                }
            }
            
            // 处理滚动
            if (autoScroll) {
                scrollToBottom();
            } else if (currentScrollPosition >= 0) {
                restoreScrollPosition(currentScrollPosition);
            }
        } else {
            // 如果面板不存在，刷新显示
            refreshDisplay(autoScroll);
        }
    }

    public void updateStreamingMessage(ChatMessage message) {
        updateStreamingMessage(message, false);
    }
    
    /**
     * 完成流式消息
     */
    public void finalizeStreamingMessage(ChatMessage message) {
        // 将流式消息添加到正常消息列表
        if (!messages.contains(message)) {
            messages.add(message);
        }
        streamingMessage = null;
        streamingMessagePanel = null;
        refreshDisplay();
    }
    
    /**
     * 刷新显示
     */
    private void refreshDisplay(boolean autoScroll) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 保存当前滚动位置
                int currentScrollPosition = getCurrentScrollPosition();
                
                removeAll();
                
                // 添加所有消息
                for (ChatMessage message : messages) {
                    add(createMessageComponent(message));
                }
                
                // 添加流式消息
                if (streamingMessage != null) {
                    streamingMessagePanel = createMessageComponent(streamingMessage);
                    add(streamingMessagePanel);
                }
                
                // 添加正在输入提示
                if (typingIndicator != null) {
                    add(createMessageComponent(typingIndicator));
                }
                
                // 添加底部空白，以便更好地滚动
                add(Box.createVerticalGlue());
                
                // 强制重绘
                revalidate();
                repaint();
                
                // 决定是否滚动
                if (autoScroll) {
                    // 明确要求滚动到底部
                    scrollToBottom();
                } else {
                    // 如果不要求滚动，恢复原来的滚动位置
                    restoreScrollPosition(currentScrollPosition);
                }
                
                System.out.println("消息显示刷新完成，共 " + messages.size() + " 条" +
                        (streamingMessage != null ? " + 1条流式消息" : "") +
                        (typingIndicator != null ? " + 1条输入提示" : ""));
            } catch (Exception e) {
                System.err.println("刷新消息显示时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    // 为了兼容，保留原有方法
    private void refreshDisplay() {
        refreshDisplay(false);
    }
    
    private JPanel createMessageComponent(ChatMessage message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        boolean isUserMessage = "我".equals(message.getSender());
        
        // 创建不同的背景颜色
        Color bgColor = isUserMessage ? new Color(220, 248, 198) : new Color(255, 255, 255);
        panel.setBackground(bgColor);
        
        // 创建边框
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, isUserMessage ? 50 : 10, 5, isUserMessage ? 10 : 50),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isUserMessage ? new Color(170, 218, 148) : new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            )
        ));
        
        // 创建标题（发送者）
        JLabel senderLabel = new JLabel(message.getSender());
        senderLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        senderLabel.setForeground(isUserMessage ? new Color(0, 100, 0) : new Color(0, 0, 180));
        
        // 创建内容
        JTextArea contentText = new JTextArea(message.getContent());
        contentText.setFont(new Font("Dialog", Font.PLAIN, 14));
        contentText.setEditable(false);
        contentText.setLineWrap(true);
        contentText.setWrapStyleWord(true);
        contentText.setBackground(bgColor);
        contentText.setBorder(null);
        
        // 添加组件
        panel.add(senderLabel, BorderLayout.NORTH);
        panel.add(contentText, BorderLayout.CENTER);
        
        // 设置最大宽度，确保在窗口调整大小时文本能正确换行
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        
        return panel;
    }

    /**
     * 获取当前滚动位置
     */
    private int getCurrentScrollPosition() {
        Container parent = getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) parent;
            return scrollPane.getVerticalScrollBar().getValue();
        }
        return 0;
    }

    /**
     * 恢复滚动位置
     */
    private void restoreScrollPosition(int position) {
        Container parent = getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof JScrollPane) {
            final JScrollPane scrollPane = (JScrollPane) parent;
            final int scrollPos = position;
            SwingUtilities.invokeLater(() -> {
                scrollPane.getVerticalScrollBar().setValue(scrollPos);
            });
        }
    }
} 