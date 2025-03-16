package com.aIgenie.view;

import javax.swing.*;
import java.awt.*;

/**
 * 用于显示请求和响应的调试窗口
 */
public class RequestResponseWindow extends JFrame {
    private JTextArea requestArea;
    private JTextArea responseArea;
    
    public RequestResponseWindow() {
        setTitle("AI请求/响应查看器");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);
        
        // 创建请求区域
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("请求内容"));
        requestArea = new JTextArea();
        requestArea.setEditable(false);
        requestArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane requestScrollPane = new JScrollPane(requestArea);
        requestPanel.add(requestScrollPane, BorderLayout.CENTER);
        
        // 创建响应区域
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("响应内容"));
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane responseScrollPane = new JScrollPane(responseArea);
        responsePanel.add(responseScrollPane, BorderLayout.CENTER);
        
        // 添加到分割面板
        splitPane.setTopComponent(requestPanel);
        splitPane.setBottomComponent(responsePanel);
        
        // 添加功能按钮
        JPanel buttonPanel = new JPanel();
        JButton clearButton = new JButton("清除");
        clearButton.addActionListener(e -> {
            requestArea.setText("");
            responseArea.setText("");
        });
        buttonPanel.add(clearButton);
        
        // 添加到主窗口
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }
    
    public void setRequestContent(String content) {
        requestArea.setText(content);
        requestArea.setCaretPosition(0);
    }
    
    public void setResponseContent(String content) {
        responseArea.setText(content);
        responseArea.setCaretPosition(0);
    }
    
    public void appendRequestContent(String content) {
        requestArea.append(content + "\n\n");
        requestArea.setCaretPosition(requestArea.getText().length());
    }
    
    public void appendResponseContent(String content) {
        responseArea.append(content + "\n\n");
        responseArea.setCaretPosition(responseArea.getText().length());
    }
} 