package com.aIgenie;

import com.aIgenie.controller.ChatController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import javax.swing.SwingUtilities;

/**
 * Spring Boot主应用程序类
 */
@SpringBootApplication
public class AIApplication {

    private static final Logger logger = LoggerFactory.getLogger(AIApplication.class);

    public static void main(String[] args) {
        // 设置非无头模式，允许GUI
        System.setProperty("java.awt.headless", "false");

        ApplicationContext context = new SpringApplicationBuilder(AIApplication.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .headless(false)
                .run(args);

        logger.info("Spring容器启动成功，开始初始化AI聊天应用");

        try {
            ChatController controller = context.getBean(ChatController.class);
            logger.debug("成功获取ChatController: {}", controller);
            // Swing 组件应在 EDT 上初始化
            SwingUtilities.invokeLater(controller::initialize);
        } catch (Exception e) {
            logger.error("获取或初始化ChatController失败", e);
        }
    }
}
