package com.aIgenie.service.impl;

import com.aIgenie.service.AIService;
import com.aIgenie.service.RequestResponseListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@ConditionalOnProperty(name = "aigenie.use-custom-client", havingValue = "true")
public class CustomAIServiceImpl implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(CustomAIServiceImpl.class);
    
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final List<ObjectNode> messageHistory = new ArrayList<>();
    private final String systemPrompt;
    private final int maxHistoryGroups;
    private List<RequestResponseListener> listeners = new ArrayList<>();
    private List<Consumer<String>> streamListeners = new ArrayList<>();
    
    public CustomAIServiceImpl(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${aigenie.system-prompt:你是一个有用的AI助手，名为'AIgenie'。请简洁明了地回答用户的问题。}") String systemPrompt,
            @Value("${aigenie.chat-history-limit:10}") int historyLimit) {
        this.apiUrl = baseUrl + "/chat/completions";
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.maxHistoryGroups = historyLimit;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        System.out.println("初始化自定义AI客户端");
        System.out.println("模型: " + model);
        System.out.println("API URL: " + this.apiUrl);
        System.out.println("系统提示: " + this.systemPrompt);
        
        logger.info("初始化自定义AI客户端，使用模型: {}", model);
        logger.info("API URL: {}", this.apiUrl);
    }
    
    @Override
    public String sendMessage(String message) {
        try {
            System.out.println("\n====== 开始调用AI接口 ======");
            System.out.println("发送消息到AI: " + message);
            
            // 添加用户消息到历史
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messageHistory.add(userMessage);
            
            // 限制历史消息数量
            while (messageHistory.size() > maxHistoryGroups * 2) {
                messageHistory.remove(0);
            }
            
            // 准备请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", false); // 不使用流式响应
            
            // 添加消息数组，首先是系统消息，然后是历史消息
            ArrayNode messagesNode = requestBody.putArray("messages");
            
            // 添加系统提示消息
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messagesNode.add(systemMessage);
            
            // 添加历史消息
            for (ObjectNode msg : messageHistory) {
                messagesNode.add(msg);
            }
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 创建请求实体
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            
            // 在发送HTTP请求前
            String requestJson = objectMapper.writeValueAsString(requestBody);
            System.out.println("发送请求到: " + apiUrl);
            System.out.println("请求体: " + requestJson);
            
            // 发送请求
            System.out.println("正在等待AI响应...");
            String responseBody = restTemplate.postForObject(apiUrl, request, String.class);
            
            // 请求后
            System.out.println("收到响应");
            System.out.println("响应体: " + responseBody);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(responseBody);
            String content = responseJson.path("choices").path(0).path("message").path("content").asText();
            
            // 添加AI回复到历史
            ObjectNode assistantMessage = objectMapper.createObjectNode();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", content);
            messageHistory.add(assistantMessage);
            
            // 解析后
            System.out.println("解析后的AI回复: " + content);
            System.out.println("====== AI调用结束 ======\n");
            
            notifyListeners(requestJson, responseBody);
            
            return content;
        } catch (Exception e) {
            System.err.println("====== AI调用出错 ======");
            System.err.println("错误消息: " + e.getMessage());
            e.printStackTrace(System.err);
            System.err.println("=======================\n");
            return "抱歉，我遇到了一个问题: " + e.getMessage();
        }
    }
    
    @Override
    public CompletableFuture<String> sendMessageAsync(String message) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        System.out.println("异步请求开始: " + message);
        
        // 在新线程中执行
        CompletableFuture.runAsync(() -> {
            try {
                // 使用流式响应
                sendMessageStreaming(message, 
                    // 流式更新回调
                    chunk -> notifyStreamListeners(chunk),
                    // 完成回调
                    result -> future.complete(result),
                    // 错误回调
                    error -> future.completeExceptionally(error)
                );
            } catch (Exception e) {
                future.completeExceptionally(e);
                System.err.println("发送消息时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return future;
    }

    public void addRequestResponseListener(RequestResponseListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(String request, String response) {
        for (RequestResponseListener listener : listeners) {
            listener.onRequestResponse(request, response);
        }
    }

    /**
     * 添加流式响应监听器
     */
    public void addStreamListener(Consumer<String> listener) {
        streamListeners.add(listener);
    }
    
    /**
     * 移除流式响应监听器
     */
    public void removeStreamListener(Consumer<String> listener) {
        streamListeners.remove(listener);
    }
    
    /**
     * 通知流式响应监听器
     */
    private void notifyStreamListeners(String chunk) {
        for (Consumer<String> listener : streamListeners) {
            listener.accept(chunk);
        }
    }
    
    /**
     * 发送支持流式响应的消息
     */
    private void sendMessageStreaming(
            String message, 
            Consumer<String> onChunk, 
            Consumer<String> onComplete,
            Consumer<Throwable> onError) {
        
        try {
            System.out.println("\n====== 开始调用AI接口(流式) ======");
            System.out.println("发送消息到AI: " + message);
            
            // 添加用户消息到历史
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messageHistory.add(userMessage);
            
            // 限制历史消息数量
            while (messageHistory.size() > maxHistoryGroups * 2) {
                messageHistory.remove(0);
            }
            
            // 准备请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true); // 启用流式响应!
            
            // 添加消息数组，首先是系统消息，然后是历史消息
            ArrayNode messagesNode = requestBody.putArray("messages");
            
            // 添加系统提示消息
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messagesNode.add(systemMessage);
            
            // 添加历史消息
            for (ObjectNode msg : messageHistory) {
                messagesNode.add(msg);
            }
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 创建请求实体
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            
            // 在发送HTTP请求前
            String requestJson = objectMapper.writeValueAsString(requestBody);
            System.out.println("发送请求到: " + apiUrl);
            System.out.println("请求体: " + requestJson);
            
            // 发送流式请求并处理响应
            System.out.println("启动流式响应处理...");
            
            final StringBuilder fullResponse = new StringBuilder();
            final StringBuilder currentContent = new StringBuilder();
            
            // 使用ResponseExtractor处理流式响应
            restTemplate.execute(apiUrl, org.springframework.http.HttpMethod.POST, 
                req -> {
                    req.getHeaders().putAll(headers);
                    req.getBody().write(objectMapper.writeValueAsString(requestBody).getBytes());
                },
                (ResponseExtractor<Void>) response -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) continue;
                            
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                
                                if ("[DONE]".equals(data)) {
                                    System.out.println("流式传输完成");
                                    continue;
                                }
                                
                                try {
                                    JsonNode chunk = objectMapper.readTree(data);
                                    JsonNode choices = chunk.path("choices");
                                    
                                    if (choices.isArray() && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).path("delta");
                                        JsonNode contentNode = delta.path("content");
                                        
                                        if (!contentNode.isMissingNode()) {
                                            String content = contentNode.asText();
                                            currentContent.append(content);
                                            fullResponse.append(content);
                                            
                                            // 通知监听器有新的内容块
                                            onChunk.accept(content);
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("解析流式数据出错: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("处理流式响应时出错: " + e.getMessage());
                        onError.accept(e);
                    }
                    return null;
                }
            );
            
            // 流式处理完成，获取完整响应
            String finalResponse = fullResponse.toString();
            System.out.println("完整响应: " + finalResponse);
            
            // 添加AI回复到历史
            ObjectNode assistantMessage = objectMapper.createObjectNode();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", finalResponse);
            messageHistory.add(assistantMessage);
            
            System.out.println("====== AI调用结束(流式) ======\n");
            
            // 调用完成回调
            onComplete.accept(finalResponse);
            
        } catch (Exception e) {
            System.err.println("====== AI调用出错(流式) ======");
            System.err.println("错误消息: " + e.getMessage());
            e.printStackTrace(System.err);
            System.err.println("=======================\n");
            
            onError.accept(e);
        }
    }
} 