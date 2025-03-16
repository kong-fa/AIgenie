package com.aIgenie.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import com.aIgenie.view.dialogs.SettingsDialog;
import com.aIgenie.util.DockableWindowBehavior;

/**
 * 自定义标题栏组件
 */
public class TitlePanel extends JPanel {
    private Point dragStart;
    private JLabel titleLabel;
    private JButton minimizeButton;
    private JButton alwaysOnTopButton;
    private JButton closeButton;
    private JButton settingsButton;
    private JFrame parentFrame;
    private boolean isAlwaysOnTop = false;
    private Color backgroundColor = new Color(25, 118, 210); // 蓝色背景
    private Color textColor = Color.WHITE;
    
    public TitlePanel(String title) {
        setPreferredSize(new Dimension(getWidth(), 30));
        setBackground(backgroundColor);
        
        // 设置布局和组件
        setupComponents(title);
    }
    
    private void setupComponents(String title) {
        setLayout(new BorderLayout());
        
        // 创建左侧面板用于标题，使用垂直居中对齐
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0)); // 左边距
        leftPanel.setOpaque(false);
        
        // 创建标题标签，添加垂直对齐
        titleLabel = new JLabel(title);
        titleLabel.setForeground(textColor);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        titleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        leftPanel.add(titleLabel);
        
        add(leftPanel, BorderLayout.WEST);  // 改回WEST，确保标题在左侧
        
        // 创建右侧面板用于按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 3));
        rightPanel.setOpaque(false);
        
        // 创建设置按钮
        settingsButton = new JButton("⚙");
        settingsButton.setPreferredSize(new Dimension(26, 24));
        settingsButton.setFocusPainted(false);
        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setBorder(null);
        settingsButton.setForeground(textColor);
        settingsButton.setFont(new Font("Dialog", Font.BOLD, 16));
        settingsButton.setToolTipText("设置");
        settingsButton.addActionListener(e -> showSettingsDialog());
        settingsButton.addMouseListener(createButtonHoverEffect(settingsButton, new Color(220, 220, 220)));
        rightPanel.add(settingsButton);
        
        // 创建置顶按钮
        alwaysOnTopButton = new JButton("📌");
        alwaysOnTopButton.setPreferredSize(new Dimension(26, 24));
        alwaysOnTopButton.setFocusPainted(false);
        alwaysOnTopButton.setBorderPainted(false);
        alwaysOnTopButton.setContentAreaFilled(false);
        alwaysOnTopButton.setBorder(null);
        alwaysOnTopButton.setForeground(textColor);
        alwaysOnTopButton.setFont(new Font("Dialog", Font.BOLD, 14));
        alwaysOnTopButton.setToolTipText("置于顶层");
        alwaysOnTopButton.addActionListener(e -> toggleAlwaysOnTop());
        alwaysOnTopButton.addMouseListener(createButtonHoverEffect(alwaysOnTopButton, new Color(220, 220, 220)));
        rightPanel.add(alwaysOnTopButton);
        
        // 创建最小化按钮
        minimizeButton = new JButton("—");
        minimizeButton.setPreferredSize(new Dimension(26, 24));
        minimizeButton.setFocusPainted(false);
        minimizeButton.setBorderPainted(false);
        minimizeButton.setContentAreaFilled(false);
        minimizeButton.setBorder(null);
        minimizeButton.setForeground(textColor);
        minimizeButton.setFont(new Font("Dialog", Font.BOLD, 14));
        minimizeButton.setToolTipText("最小化");
        minimizeButton.addActionListener(e -> {
            if (parentFrame != null) {
                parentFrame.setState(Frame.ICONIFIED);
            }
        });
        minimizeButton.addMouseListener(createButtonHoverEffect(minimizeButton, new Color(220, 220, 220)));
        rightPanel.add(minimizeButton);
        
        // 创建关闭按钮
        closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(26, 24));
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(null);
        closeButton.setForeground(textColor);
        closeButton.setFont(new Font("Dialog", Font.BOLD, 18));
        closeButton.setToolTipText("关闭");
        closeButton.addActionListener(e -> System.exit(0));
        closeButton.addMouseListener(createButtonHoverEffect(closeButton, new Color(255, 95, 87)));
        rightPanel.add(closeButton);
        
        add(rightPanel, BorderLayout.EAST);
    }
    
    /**
     * 切换窗口的置顶状态
     */
    private void toggleAlwaysOnTop() {
        if (parentFrame != null) {
            isAlwaysOnTop = !isAlwaysOnTop;
            parentFrame.setAlwaysOnTop(isAlwaysOnTop);
            
            // 更新按钮状态
            if (isAlwaysOnTop) {
                alwaysOnTopButton.setForeground(new Color(255, 255, 0)); // 黄色表示激活
                alwaysOnTopButton.setToolTipText("取消置顶");
            } else {
                alwaysOnTopButton.setForeground(Color.WHITE);
                alwaysOnTopButton.setToolTipText("置于顶层");
            }
        }
    }
    
    public void setDraggableFrame(JFrame frame) {
        this.parentFrame = frame;
        
        // 添加鼠标事件，使标题栏可拖动窗口
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    Point currentPoint = e.getLocationOnScreen();
                    frame.setLocation(
                        currentPoint.x - dragStart.x,
                        currentPoint.y - dragStart.y
                    );
                }
            }
        });
    }
    
    /**
     * 显示设置对话框
     */
    private void showSettingsDialog() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        SettingsDialog dialog = new SettingsDialog(owner);
        
        // 添加设置变更监听器
        dialog.setChangeListener((theme, apiUrl, apiKey, dockingEnabled, model) -> {
            // 处理设置变更
            System.out.println("设置已更改: 主题=" + theme + ", API URL=" + apiUrl + ", 模型=" + model);
            
            // 启用或禁用窗口停靠
            if (parentFrame != null) {
                try {
                    DockableWindowBehavior behavior = 
                        (DockableWindowBehavior) ((JComponent)parentFrame.getContentPane()).getClientProperty("dockBehavior");
                    if (behavior != null) {
                        behavior.setDockingEnabled(dockingEnabled);
                    }
                } catch (Exception ex) {
                    System.err.println("无法设置停靠行为: " + ex.getMessage());
                }
            }
            
            // 这里可以添加重启应用程序的提示
            int option = JOptionPane.showConfirmDialog(
                owner,
                "配置已保存到文件。某些设置需要重启应用程序才能生效，是否立即重启？",
                "重启提示",
                JOptionPane.YES_NO_OPTION
            );
            
            if (option == JOptionPane.YES_OPTION) {
                // 执行重启操作
                System.exit(0);  // 在生产环境中，应该实现更优雅的重启机制
            }
        });
        
        // 显示对话框
        dialog.setVisible(true);
    }
    
    // 添加鼠标悬停效果的辅助方法
    private MouseAdapter createButtonHoverEffect(JButton button, Color hoverColor) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(hoverColor);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(textColor);
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        };
    }
    
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 添加底部分隔线
        g.setColor(new Color(0, 0, 0, 30));
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, 30); // 固定高度为30像素
    }
} 