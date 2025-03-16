package com.aIgenie;

import com.aIgenie.controller.ChatController;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

/**
 * Spring Boot主应用程序类
 */
@SpringBootApplication
public class AIApplication {
    
    public static void main(String[] args) {
        // 设置非无头模式，允许GUI
        System.setProperty("java.awt.headless", "false");
        
        // 使用SpringApplicationBuilder并设置为非Web应用
        ApplicationContext context = new SpringApplicationBuilder(AIApplication.class)
            .web(WebApplicationType.NONE)  // 设置为非Web应用
            .bannerMode(Banner.Mode.OFF)   // 关闭启动横幅
            .headless(false)  // 显式设置非无头模式
            .run(args);
        
        System.out.println("Spring容器启动成功，开始初始化AI聊天应用");
        
        // 查看所有Bean
        System.out.println("已注册的AI和控制器Bean:");
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            if (beanName.contains("AIService") || beanName.contains("Controller")) {
                System.out.println("- " + beanName);
            }
        }
        
        // 从Spring容器获取ChatController并初始化UI
        try {
            ChatController controller = context.getBean(ChatController.class);
            System.out.println("成功获取ChatController: " + controller);
            controller.initialize();
        } catch (Exception e) {
            System.err.println("获取或初始化ChatController失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 