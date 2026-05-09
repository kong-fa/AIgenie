package com.aIgenie.config;

import com.aIgenie.service.AIService;
import com.aIgenie.service.impl.CustomAIServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class AIConfig {

    private static final Logger logger = LoggerFactory.getLogger(AIConfig.class);

    /**
     * 自定义 AI 服务 Bean。仅在 aigenie.use-custom-client=true 时注册（默认开启），
     * 与 {@link CustomAIServiceImpl} 上的相同条件保持一致，确保开关行为一致。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "aigenie.use-custom-client", havingValue = "true", matchIfMissing = true)
    public AIService customAIService(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${spring.ai.openai.chat.options.temperature:0.7}") double temperature,
            @Value("${spring.ai.openai.chat.options.max-tokens:2000}") int maxTokens,
            @Value("${aigenie.system-prompt:你是一个有用的AI助手，名为'AIgenie'。请简洁明了地回答用户的问题。}") String systemPrompt,
            @Value("${aigenie.chat-history-limit:10}") int historyLimit,
            // 默认 connect 10s / read 60s，避免 RestTemplate 默认零超时导致的永久挂起。
            // 流式响应通常较慢，read 超时给到 60s 比较合理。
            @Value("${aigenie.http.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${aigenie.http.read-timeout-seconds:60}") long readTimeoutSeconds) {

        logger.info("创建自定义AI服务Bean (use-custom-client=true)");
        return new CustomAIServiceImpl(
                baseUrl, apiKey, model, systemPrompt, historyLimit,
                temperature, maxTokens,
                Duration.ofSeconds(connectTimeoutSeconds),
                Duration.ofSeconds(readTimeoutSeconds));
    }
}
