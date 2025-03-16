package com.aIgenie.util;

import com.aIgenie.view.components.TitlePanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 窗口停靠行为类
 * 允许窗口停靠在屏幕边缘并自动收缩
 */
public class DockableWindowBehavior {
    private JFrame window;
    private TitlePanel titlePanel;
    
    // 窗口状态变量
    private boolean isDocked = false;
    private boolean userMovedAway = false;
    private int dockPosition = 0; // 0=未停靠, 1=左侧, 2=右侧, 3=顶部
    private Rectangle lastUndockedBounds;
    private int screenWidth;
    private int screenHeight;
    private int dockSensitivity = 20; // 停靠敏感区域大小
    
    // 配置选项
    private boolean dockingEnabled = true;
    
    // 防抖动控制
    private boolean isCollapsed = false;
    private boolean isProcessingAction = false;
    private long lastActionTime = 0;
    private static final long DEBOUNCE_TIME = 500; // 防抖动时间（毫秒）
    
    // 添加定时器用于检查鼠标位置
    private Timer mouseTrackTimer;
    
    public DockableWindowBehavior(JFrame window, TitlePanel titlePanel) {
        this.window = window;
        this.titlePanel = titlePanel;
        
        // 设置标题栏可拖动并传递JFrame引用
        titlePanel.setDraggableFrame(window);
        
        // 获取屏幕尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;
        
        // 设置事件监听器
        setupListeners();
        
        // 设置鼠标追踪定时器 - 降低频率到500ms
        setupMouseTrackingTimer();
    }
    
    /**
     * 设置是否启用停靠功能
     */
    public void setDockingEnabled(boolean enabled) {
        this.dockingEnabled = enabled;
        if (!enabled && isDocked) {
            // 如果禁用停靠并且窗口当前已停靠，恢复到未停靠状态
            isDocked = false;
            isCollapsed = false;
            if (lastUndockedBounds != null) {
                window.setBounds(lastUndockedBounds);
            }
        }
        
        // 启用或禁用定时器
        if (enabled) {
            if (!mouseTrackTimer.isRunning()) {
                mouseTrackTimer.start();
            }
        } else {
            if (mouseTrackTimer.isRunning()) {
                mouseTrackTimer.stop();
            }
        }
    }
    
    /**
     * 设置鼠标位置跟踪定时器
     */
    private void setupMouseTrackingTimer() {
        mouseTrackTimer = new Timer(300, e -> { // 增加检查间隔到500ms
            if (!dockingEnabled || isProcessingAction) return;
            
            // 检查上次操作后是否经过了足够的防抖动时间
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastActionTime < DEBOUNCE_TIME) {
                return;
            }
            
            try {
                // 获取当前鼠标位置
                PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                if (pointerInfo == null) return;
                
                Point mousePos = pointerInfo.getLocation();
                
                // 获取窗口位置和大小
                Rectangle windowBounds = window.getBounds();
                
                // 检查鼠标是否在窗口内
                boolean mouseInWindow = windowBounds.contains(mousePos);
                
                // 如果窗口已停靠且鼠标在窗口外，并且窗口未收缩，则收缩窗口
                if (isDocked && !mouseInWindow && !isCollapsed && !userMovedAway) {
                    isProcessingAction = true;
                    SwingUtilities.invokeLater(() -> {
                        collapseDock();
                        isCollapsed = true;
                        isProcessingAction = false;
                        lastActionTime = System.currentTimeMillis();
                    });
                }
                
                // 如果窗口已停靠且收缩状态，但鼠标重新进入窗口区域，则展开窗口
                else if (isDocked && mouseInWindow && isCollapsed) {
                    isProcessingAction = true;
                    SwingUtilities.invokeLater(() -> {
                        expandDock();
                        isCollapsed = false;
                        isProcessingAction = false;
                        lastActionTime = System.currentTimeMillis();
                    });
                }
            } catch (Exception ex) {
                // 处理可能的异常，如鼠标信息不可用
                System.err.println("检查鼠标位置时出错: " + ex.getMessage());
                isProcessingAction = false;
            }
        });
        
        // 启动定时器
        mouseTrackTimer.start();
    }
    
    private void setupListeners() {
        // 监控窗口移动 - 检查是否需要停靠
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (!dockingEnabled || isProcessingAction) return;
                
                // 防抖动检查
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastActionTime < DEBOUNCE_TIME) {
                    return;
                }
                
                checkAndApplyDocking();
            }
        });
        
        // 鼠标进入窗口时 - 自动展开已停靠的窗口
        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!dockingEnabled || isProcessingAction) return;
                
                // 防抖动检查
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastActionTime < DEBOUNCE_TIME) {
                    return;
                }
                
                // 如果窗口已停靠且处于收缩状态，展开窗口
                if (isDocked && isCollapsed) {
                    isProcessingAction = true;
                    SwingUtilities.invokeLater(() -> {
                        expandDock();
                        isCollapsed = false;
                        isProcessingAction = false;
                        lastActionTime = System.currentTimeMillis();
                    });
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // mouseExited事件不再处理收缩，完全依靠定时器
            }
        });
        
        // 在用户明确点击标题栏时，重置状态
        titlePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 用户开始拖动，重置状态
                userMovedAway = false;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dockingEnabled || isProcessingAction) return;
                
                // 防抖动检查
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastActionTime < DEBOUNCE_TIME) {
                    return;
                }
                
                // 当用户释放鼠标时，检查是否需要停靠
                checkAndApplyDocking();
            }
        });
    }
    
    /**
     * 检查窗口位置并应用停靠逻辑
     */
    private void checkAndApplyDocking() {
        try {
            isProcessingAction = true;
            
            Point location = window.getLocation();
            
            // 检查是否在停靠区域
            boolean nearLeftEdge = location.x <= dockSensitivity;
            boolean nearTopEdge = location.y <= dockSensitivity;
            boolean nearRightEdge = Math.abs(location.x + window.getWidth() - screenWidth) <= dockSensitivity;
            
            boolean nearDockingArea = nearLeftEdge || nearTopEdge || nearRightEdge;
            
            // 停靠检测
            if (nearDockingArea) {
                // 如果接近边缘，应用停靠
                if (nearLeftEdge) {
                    dockToLeft();
                } else if (nearTopEdge) {
                    dockToTop();
                } else if (nearRightEdge) {
                    dockToRight();
                }
                
                isDocked = true;
                isCollapsed = false;
                userMovedAway = false;
            } else {
                // 如果不在停靠区域，并且窗口已经停靠，标记用户主动移开
                if (isDocked) {
                    isDocked = false;
                    isCollapsed = false;
                    userMovedAway = true;
                    // 保存非停靠状态的位置，以便稍后恢复
                    lastUndockedBounds = window.getBounds();
                }
            }
            
            lastActionTime = System.currentTimeMillis();
        } finally {
            isProcessingAction = false;
        }
    }
    
    /**
     * 停靠到左侧
     */
    private void dockToLeft() {
        dockPosition = 1;
        // 保存最后非停靠状态的位置
        if (!isDocked) {
            lastUndockedBounds = window.getBounds();
        }
        window.setLocation(0, window.getLocation().y);
    }
    
    /**
     * 停靠到顶部
     */
    private void dockToTop() {
        dockPosition = 3;
        if (!isDocked) {
            lastUndockedBounds = window.getBounds();
        }
        window.setLocation(window.getLocation().x, 0);
    }
    
    /**
     * 停靠到右侧
     */
    private void dockToRight() {
        dockPosition = 2;
        if (!isDocked) {
            lastUndockedBounds = window.getBounds();
        }
        window.setLocation(screenWidth - window.getWidth(), window.getLocation().y);
    }
    
    /**
     * 收缩停靠的窗口
     */
    private void collapseDock() {
        switch (dockPosition) {
            case 1: // 左侧停靠
                window.setLocation(-window.getWidth() + 5, window.getLocation().y);
                break;
            case 2: // 右侧停靠
                window.setLocation(screenWidth - 5, window.getLocation().y);
                break;
            case 3: // 顶部停靠
                window.setLocation(window.getLocation().x, -window.getHeight() + 5);
                break;
        }
    }
    
    /**
     * 展开停靠的窗口
     */
    private void expandDock() {
        switch (dockPosition) {
            case 1: // 左侧停靠
                window.setLocation(0, window.getLocation().y);
                break;
            case 2: // 右侧停靠
                window.setLocation(screenWidth - window.getWidth(), window.getLocation().y);
                break;
            case 3: // 顶部停靠
                window.setLocation(window.getLocation().x, 0);
                break;
        }
    }
} 