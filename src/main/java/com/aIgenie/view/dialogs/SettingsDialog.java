package com.aIgenie.view.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

/**
 * 设置对话框
 * 负责展示和处理应用设置，并直接更新application.yaml文件
 */
public class SettingsDialog extends JDialog {
    private JComboBox<String> themeSelector;
    private JTextField apiUrlField;
    private JTextField apiKeyField;
    private JTextField modelField;
    
    // 设置变更监听器接口
    public interface SettingsChangeListener {
        void onSettingsChanged(String theme, String apiUrl, String apiKey, boolean dockingEnabled, String model);
    }
    
    private SettingsChangeListener changeListener;
    
    private JCheckBox enableDockingCheckbox;
    
    // 配置文件路径 - 使用多个可能的路径
    private static final String[] CONFIG_PATHS = {
        "src/main/resources/application.yaml",  // IDE开发环境
        "application.yaml",                     // 运行时目录
        "./application.yaml",                   // 相对路径
        "../application.yaml",                  // 上一级目录
        "config/application.yaml"               // 配置目录
    };
    
    // 当前使用的配置文件路径
    private String currentConfigPath;
    
    public SettingsDialog(Frame owner) {
        super(owner, "设置", true);
        
        setupUI();
        loadCurrentSettings();
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
        apiPanel.setLayout(new GridLayout(3, 2, 10, 10));
        apiPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        apiPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        apiPanel.add(new JLabel("API URL:"));
        apiUrlField = new JTextField(20);
        apiPanel.add(apiUrlField);
        
        apiPanel.add(new JLabel("API Key:"));
        apiKeyField = new JTextField(20);
        apiPanel.add(apiKeyField);
        
        apiPanel.add(new JLabel("AI模型:"));
        modelField = new JTextField(20);
        apiPanel.add(modelField);
        
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
     * 加载当前配置设置
     */
    private void loadCurrentSettings() {
        try {
            // 尝试读取配置
            Map<String, Object> config = readYamlConfig();
            if (config == null || config.isEmpty()) {
                System.err.println("警告: 读取到的配置为空！");
                return;
            }
            
            System.out.println("成功读取配置文件: " + currentConfigPath);
            System.out.println("配置内容: " + config);
            
            // 设置API URL和Key
            Map<String, Object> springConfig = getNestedMap(config, "spring");
            if (springConfig != null) {
                Map<String, Object> aiConfig = getNestedMap(springConfig, "ai");
                if (aiConfig != null) {
                    Map<String, Object> openAiConfig = getNestedMap(aiConfig, "openai");
                    if (openAiConfig != null) {
                        String apiUrl = (String) openAiConfig.get("base-url");
                        String apiKey = (String) openAiConfig.get("api-key");
                        
                        System.out.println("读取到的API URL: " + apiUrl);
                        System.out.println("读取到的API Key: " + apiKey);
                        
                        // 设置UI元素
                        if (apiUrl != null) apiUrlField.setText(apiUrl);
                        if (apiKey != null) apiKeyField.setText(apiKey);
                        
                        // 获取模型
                        Map<String, Object> chatConfig = getNestedMap(openAiConfig, "chat");
                        if (chatConfig != null) {
                            Map<String, Object> optionsConfig = getNestedMap(chatConfig, "options");
                            if (optionsConfig != null) {
                                String model = (String) optionsConfig.get("model");
                                System.out.println("读取到的模型: " + model);
                                if (model != null) modelField.setText(model);
                            }
                        }
                    }
                }
            }
            
            // 读取主题和停靠设置
            Map<String, Object> aigenieConfig = getNestedMap(config, "aigenie");
            if (aigenieConfig != null) {
                String theme = (String) aigenieConfig.get("theme");
                Object dockingEnabled = aigenieConfig.get("docking-enabled");
                
                System.out.println("读取到的主题: " + theme);
                System.out.println("读取到的停靠设置: " + dockingEnabled);
                
                // 设置主题
                if ("深色".equals(theme)) {
                    themeSelector.setSelectedIndex(1);
                } else {
                    themeSelector.setSelectedIndex(0);
                }
                
                // 设置停靠
                if (dockingEnabled instanceof Boolean) {
                    enableDockingCheckbox.setSelected((Boolean) dockingEnabled);
                }
            }
            
        } catch (Exception e) {
            System.err.println("加载配置时出错: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "无法加载配置文件: " + e.getMessage(), 
                "配置错误", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 安全获取嵌套Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) instanceof Map) {
            return (Map<String, Object>) map.get(key);
        }
        // 如果不存在返回null而不是创建新的
        return null;
    }
    
    /**
     * 获取写入用的嵌套Map（如果不存在则创建）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateNestedMap(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) instanceof Map) {
            return (Map<String, Object>) map.get(key);
        }
        // 如果不存在则创建
        Map<String, Object> newMap = new LinkedHashMap<>();
        map.put(key, newMap);
        return newMap;
    }
    
    /**
     * 从YAML文件读取配置
     */
    private Map<String, Object> readYamlConfig() throws IOException {
        Yaml yaml = new Yaml();
        
        // 尝试所有可能的路径
        for (String path : CONFIG_PATHS) {
            File configFile = new File(path);
            if (configFile.exists() && configFile.isFile()) {
                System.out.println("找到配置文件: " + configFile.getAbsolutePath());
                currentConfigPath = path;
                
                try (InputStream in = new FileInputStream(configFile)) {
                    Object obj = yaml.load(in);
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) obj;
                        return result;
                    } else {
                        System.err.println("配置文件内容不是Map: " + obj);
                    }
                } catch (Exception e) {
                    System.err.println("读取配置文件 " + path + " 出错: " + e.getMessage());
                }
            }
        }
        
        System.err.println("未找到任何配置文件，将使用默认路径: " + CONFIG_PATHS[0]);
        currentConfigPath = CONFIG_PATHS[0];
        return new LinkedHashMap<>();
    }
    
    /**
     * 将配置写入YAML文件
     */
    private void writeYamlConfig(Map<String, Object> config) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setIndent(2);
        options.setIndentWithIndicator(true);
        options.setCanonical(false);
        
        Yaml yaml = new Yaml(options);
        
        // 使用当前配置路径
        File configFile = new File(currentConfigPath);
        System.out.println("写入配置到: " + configFile.getAbsolutePath());
        
        try (Writer writer = new FileWriter(configFile)) {
            yaml.dump(config, writer);
        }
    }
    
    /**
     * 保存设置
     */
    private void saveSettings() {
        try {
            String theme = (String) themeSelector.getSelectedItem();
            String apiUrl = apiUrlField.getText();
            String apiKey = apiKeyField.getText();
            String model = modelField.getText();
            boolean dockingEnabled = enableDockingCheckbox.isSelected();
            
            // 读取当前配置
            Map<String, Object> config = readYamlConfig();
            if (config == null) {
                config = new LinkedHashMap<>();
            }
            
            // 更新配置
            Map<String, Object> springConfig = getOrCreateNestedMap(config, "spring");
            Map<String, Object> aiConfig = getOrCreateNestedMap(springConfig, "ai");
            Map<String, Object> openAiConfig = getOrCreateNestedMap(aiConfig, "openai");
            
            openAiConfig.put("base-url", apiUrl);
            openAiConfig.put("api-key", apiKey);
            
            Map<String, Object> chatConfig = getOrCreateNestedMap(openAiConfig, "chat");
            Map<String, Object> optionsConfig = getOrCreateNestedMap(chatConfig, "options");
            optionsConfig.put("model", model);
            
            // 保存自定义主题设置
            Map<String, Object> aigenieConfig = getOrCreateNestedMap(config, "aigenie");
            aigenieConfig.put("theme", theme);
            aigenieConfig.put("docking-enabled", dockingEnabled);
            
            // 写入配置文件
            writeYamlConfig(config);
            
            // 通知监听器
            if (changeListener != null) {
                changeListener.onSettingsChanged(theme, apiUrl, apiKey, dockingEnabled, model);
            }
            
            // 显示确认消息
            JOptionPane.showMessageDialog(this, "设置已保存并更新配置文件");
            dispose();
            
        } catch (Exception e) {
            System.err.println("保存设置时出错: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "保存配置时出错: " + e.getMessage(), 
                "保存错误", 
                JOptionPane.ERROR_MESSAGE);
        }
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