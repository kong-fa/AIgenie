package com.aIgenie.view.dialogs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

/**
 * 设置对话框
 * 负责展示和处理应用设置，并直接更新application.yaml文件
 */
public class SettingsDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(SettingsDialog.class);

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

    /**
     * 配置文件读取优先级，按 Spring Boot 实际外部化配置查找顺序排列：
     * <ol>
     *   <li>{@code ./config/application.yaml} —— Spring Boot 推荐的外部配置位置</li>
     *   <li>{@code ./application.yaml}        —— 与 jar 同目录的覆盖配置</li>
     *   <li>{@code src/main/resources/application.yaml}
     *       —— 仅用于 IDE 直接运行的开发场景，对打包后的 jar 不生效</li>
     * </ol>
     * 如果以上都不存在，会从 classpath 内的 {@code application.yaml}（即 jar 内的副本）读取。
     */
    private static final String[] CONFIG_READ_PATHS = {
            "./config/application.yaml",
            "./application.yaml",
            "src/main/resources/application.yaml"
    };

    /**
     * 写入时优先选用已存在的外部配置文件；若都不存在，则在工作目录新建
     * {@code ./application.yaml}，这是 Spring Boot 启动时会自动加载的位置。
     */
    private static final String DEFAULT_WRITE_PATH = "./application.yaml";

    /** classpath 兜底路径，用于在用户从未创建过外部配置时读取默认值。 */
    private static final String CLASSPATH_FALLBACK_RESOURCE = "/application.yaml";

    /** 当前使用的配置文件路径；为 null 表示来自 classpath（jar 内置）。 */
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
            Map<String, Object> config = readYamlConfig();
            if (config == null || config.isEmpty()) {
                logger.warn("读取到的配置为空");
                return;
            }

            logger.debug("成功读取配置文件: {}", currentConfigPath);

            Map<String, Object> springConfig = getNestedMap(config, "spring");
            if (springConfig != null) {
                Map<String, Object> aiConfig = getNestedMap(springConfig, "ai");
                if (aiConfig != null) {
                    Map<String, Object> openAiConfig = getNestedMap(aiConfig, "openai");
                    if (openAiConfig != null) {
                        String apiUrl = (String) openAiConfig.get("base-url");
                        String apiKey = (String) openAiConfig.get("api-key");

                        if (apiUrl != null) apiUrlField.setText(apiUrl);
                        if (apiKey != null) apiKeyField.setText(apiKey);

                        Map<String, Object> chatConfig = getNestedMap(openAiConfig, "chat");
                        if (chatConfig != null) {
                            Map<String, Object> optionsConfig = getNestedMap(chatConfig, "options");
                            if (optionsConfig != null) {
                                String model = (String) optionsConfig.get("model");
                                if (model != null) modelField.setText(model);
                            }
                        }
                    }
                }
            }

            Map<String, Object> aigenieConfig = getNestedMap(config, "aigenie");
            if (aigenieConfig != null) {
                String theme = (String) aigenieConfig.get("theme");
                Object dockingEnabled = aigenieConfig.get("docking-enabled");

                if ("深色".equals(theme)) {
                    themeSelector.setSelectedIndex(1);
                } else {
                    themeSelector.setSelectedIndex(0);
                }

                if (dockingEnabled instanceof Boolean) {
                    enableDockingCheckbox.setSelected((Boolean) dockingEnabled);
                }
            }

        } catch (Exception e) {
            logger.error("加载配置时出错", e);
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

        for (String path : CONFIG_READ_PATHS) {
            File configFile = new File(path);
            if (configFile.exists() && configFile.isFile()) {
                logger.debug("找到外部配置文件: {}", configFile.getAbsolutePath());
                currentConfigPath = path;

                try (InputStream in = new FileInputStream(configFile)) {
                    Object obj = yaml.load(in);
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) obj;
                        return result;
                    } else {
                        logger.warn("配置文件内容不是 Map: {}", obj);
                    }
                } catch (Exception e) {
                    logger.warn("读取配置文件 {} 出错: {}", path, e.getMessage());
                }
            }
        }

        // classpath 兜底：从 jar 内置的 application.yaml 读默认值。
        // 此时 currentConfigPath 保持 null，saveSettings() 会写到外部 DEFAULT_WRITE_PATH。
        try (InputStream in = SettingsDialog.class.getResourceAsStream(CLASSPATH_FALLBACK_RESOURCE)) {
            if (in != null) {
                logger.debug("外部配置不存在，回退到 classpath 内的 application.yaml 作为默认值");
                Object obj = yaml.load(in);
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) obj;
                    return result;
                }
            }
        } catch (Exception e) {
            logger.warn("读取 classpath 配置失败: {}", e.getMessage());
        }

        logger.warn("未找到任何 application.yaml，使用空白配置");
        return new LinkedHashMap<>();
    }

    /**
     * 解析写入路径：优先复用刚才读到的外部文件，否则使用 {@link #DEFAULT_WRITE_PATH}。
     * 不会写入 {@code src/main/resources/application.yaml}：那只是源代码副本，
     * 修改它对运行中的 jar 没有任何效果，会让"重启后生效"的承诺落空。
     */
    private File resolveWriteTarget() {
        if (currentConfigPath != null
                && !"src/main/resources/application.yaml".equals(currentConfigPath)) {
            return new File(currentConfigPath);
        }
        return new File(DEFAULT_WRITE_PATH);
    }
    
    /**
     * 将配置写入YAML文件
     */
    private File writeYamlConfig(Map<String, Object> config) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setIndent(2);
        options.setIndentWithIndicator(true);
        options.setCanonical(false);

        Yaml yaml = new Yaml(options);

        File configFile = resolveWriteTarget();
        // 自动创建父目录（例如选用 ./config/application.yaml 时）
        File parent = configFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        logger.info("写入配置到: {}", configFile.getAbsolutePath());

        try (Writer writer = new FileWriter(configFile)) {
            yaml.dump(config, writer);
        }
        // 写入成功后同步 currentConfigPath，下次保存继续写到同一个外部文件
        currentConfigPath = configFile.getPath();
        return configFile;
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
            
            // 写入配置文件并拿到实际写入位置（用于在确认对话框中告知用户）
            File savedTo = writeYamlConfig(config);

            // 通知监听器
            if (changeListener != null) {
                changeListener.onSettingsChanged(theme, apiUrl, apiKey, dockingEnabled, model);
            }

            // 显示确认消息：明确写入路径，方便用户在重启后确认 Spring Boot 会加载该文件
            JOptionPane.showMessageDialog(this,
                    "设置已保存到:\n" + savedTo.getAbsolutePath()
                            + "\n\n下次启动 AIgenie 时该配置会自动生效。",
                    "保存成功",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
            
        } catch (Exception e) {
            logger.error("保存设置时出错", e);
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