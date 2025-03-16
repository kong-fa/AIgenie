package com.aIgenie.config;

import com.aIgenie.service.AIService;
import com.aIgenie.service.impl.CustomAIServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIConfig {

    @Bean
    @Primary
    public AIService customAIService(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${aigenie.system-prompt:你是一个有用的AI助手，名为'AIgenie'。请简洁明了地回答用户的问题。}") String systemPrompt,
            @Value("${aigenie.chat-history-limit:10}") int historyLimit) {
            
        System.out.println("创建自定义AI服务Bean");
        return new CustomAIServiceImpl(baseUrl, apiKey, model, systemPrompt, historyLimit);
    }
} 