package com.aIgenie.view.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 设置对话框
 * 负责展示和处理应用设置
 */
public class SettingsDialog extends JDialog {
    private JComboBox<String> themeSelector;
    private JTextField apiUrlField;
    private JPasswordField apiKeyField;
    
    // 设置变更监听器接口
    public interface SettingsChangeListener {
        void onSettingsChanged(String theme, String apiUrl, String apiKey, boolean dockingEnabled);
    }
    
    private SettingsChangeListener changeListener;
    
    private JCheckBox enableDockingCheckbox;
    
    public SettingsDialog(Frame owner) {
        super(owner, "设置", true);
        
        setupUI();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // 创建设置面板
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 添加设置选项
        // 1. 主题设置
        JLabel themeLabel = new JLabel("主题:");
        themeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(themeLabel);
        
        themeSelector = new JComboBox<>(new String[]{"浅色", "深色"});
        themeSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, themeSelector.getPreferredSize().height));
        settingsPanel.add(themeSelector);
        settingsPanel.add(Box.createVerticalStrut(15));
        
        // 2. API设置
        JLabel apiLabel = new JLabel("API设置:");
        apiLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(apiLabel);
        
        JPanel apiPanel = new JPanel();
        apiPanel.setLayout(new GridLayout(2, 2, 10, 10));
        apiPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        apiPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        apiPanel.add(new JLabel("API URL:"));
        apiUrlField = new JTextField(20);
        apiPanel.add(apiUrlField);
        
        apiPanel.add(new JLabel("API Key:"));
        apiKeyField = new JPasswordField(20);
        apiPanel.add(apiKeyField);
        
        settingsPanel.add(apiPanel);
        settingsPanel.add(Box.createVerticalStrut(5));
        
        // 在设置面板中添加新选项
        enableDockingCheckbox = new JCheckBox("启用窗口停靠功能", true);
        enableDockingCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(enableDockingCheckbox);
        settingsPanel.add(Box.createVerticalStrut(10));
        
        // 添加按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("取消");
        
        saveButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // 添加面板到对话框
        add(settingsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置对话框大小和位置
        pack();
        setMinimumSize(new Dimension(350, getHeight()));
        setLocationRelativeTo(getOwner());
    }
    
    /**
     * 保存设置
     */
    private void saveSettings() {
        String theme = (String) themeSelector.getSelectedItem();
        String apiUrl = apiUrlField.getText();
        String apiKey = new String(apiKeyField.getPassword());
        boolean dockingEnabled = enableDockingCheckbox.isSelected();
        
        // 通知监听器
        if (changeListener != null) {
            changeListener.onSettingsChanged(theme, apiUrl, apiKey, dockingEnabled);
        }
        
        // 显示确认消息
        JOptionPane.showMessageDialog(this, "设置已保存");
        dispose();
    }
    
    /**
     * 设置当前配置值
     */
    public void setCurrentSettings(String theme, String apiUrl, String apiKey) {
        if ("深色".equals(theme)) {
            themeSelector.setSelectedIndex(1);
        } else {
            themeSelector.setSelectedIndex(0);
        }
        
        apiUrlField.setText(apiUrl != null ? apiUrl : "");
        apiKeyField.setText(apiKey != null ? apiKey : "");
    }
    
    /**
     * 设置设置变更监听器
     */
    public void setChangeListener(SettingsChangeListener listener) {
        this.changeListener = listener;
    }
} 