package com.aIgenie.service.impl;

import com.aIgenie.service.AIService;
import com.aIgenie.service.RequestResponseListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResponseExtractor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 基于 RestTemplate 的自定义 OpenAI 兼容 AI 服务实现。
 * 该类不再通过 {@code @Service} 自动扫描创建，而是由 {@link com.aIgenie.config.AIConfig}
 * 根据 {@code aigenie.use-custom-client} 开关有条件地注册为 Spring Bean，
 * 避免与 {@link AIServiceImpl} 同时存在导致的 Bean 冲突。
 */
public class CustomAIServiceImpl implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(CustomAIServiceImpl.class);

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE = "[DONE]";

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 持有对话上下文，所有访问需要在 {@code historyLock} 同步块中进行。 */
    private final List<ObjectNode> messageHistory = new ArrayList<>();
    private final Object historyLock = new Object();

    private final String systemPrompt;
    private final int maxHistoryGroups;

    /** 监听器集合使用 CopyOnWriteArrayList 保证多线程下迭代时的安全性。 */
    private final List<RequestResponseListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> streamListeners = new CopyOnWriteArrayList<>();

    public CustomAIServiceImpl(String baseUrl,
                               String apiKey,
                               String model,
                               String systemPrompt,
                               int historyLimit,
                               double temperature,
                               int maxTokens,
                               Duration connectTimeout,
                               Duration readTimeout) {
        this.apiUrl = baseUrl + "/chat/completions";
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.maxHistoryGroups = historyLimit;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.restTemplate = buildRestTemplate(connectTimeout, readTimeout);
        this.objectMapper = new ObjectMapper();

        logger.info("初始化自定义AI客户端，模型={}, temperature={}, max_tokens={}, connectTimeout={}s, readTimeout={}s",
                model, temperature, maxTokens, connectTimeout.toSeconds(), readTimeout.toSeconds());
        logger.info("API URL: {}", this.apiUrl);
    }

    /**
     * 构造带超时的 RestTemplate。RestTemplate 默认无任何超时，网络抖动时会无限期阻塞，
     * 进而导致控制器一直处于 in-flight 状态、UI 输入永远禁用。
     */
    private static RestTemplate buildRestTemplate(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return new RestTemplateBuilder()
                .requestFactory(() -> factory)
                .build();
    }

    @Override
    public String sendMessage(String message) {
        ObjectNode userMessage = createMessageNode("user", message);
        appendToHistory(userMessage);

        try {
            String requestJson = buildRequestJson(false);
            HttpEntity<String> request = new HttpEntity<>(requestJson, buildHeaders());

            logger.debug("发送非流式请求到 {}", apiUrl);
            String responseBody = restTemplate.postForObject(apiUrl, request, String.class);
            logger.debug("收到响应，长度: {}", responseBody == null ? 0 : responseBody.length());

            JsonNode responseJson = objectMapper.readTree(responseBody);
            String content = responseJson.path("choices").path(0).path("message").path("content").asText();

            appendToHistory(createMessageNode("assistant", content));
            notifyListeners(requestJson, responseBody);
            return content;
        } catch (Exception e) {
            // 调用失败时回滚用户消息，保持对话上下文一致
            removeFromHistory(userMessage);
            logger.error("AI调用出错", e);
            return "抱歉，我遇到了一个问题: " + e.getMessage();
        }
    }

    @Override
    public CompletableFuture<String> sendMessageAsync(String message) {
        CompletableFuture<String> future = new CompletableFuture<>();
        // 仅在 trace 级别记录用户原文，避免敏感输入意外落盘
        logger.debug("异步请求开始，长度: {}", message == null ? 0 : message.length());
        logger.trace("异步请求原文: {}", message);

        CompletableFuture.runAsync(() -> sendMessageStreaming(message,
                this::notifyStreamListeners,
                future::complete,
                future::completeExceptionally));

        return future;
    }

    public void addRequestResponseListener(RequestResponseListener listener) {
        listeners.add(listener);
    }

    public void removeRequestResponseListener(RequestResponseListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String request, String response) {
        for (RequestResponseListener listener : listeners) {
            try {
                listener.onRequestResponse(request, response);
            } catch (Exception e) {
                logger.warn("通知请求/响应监听器时出错", e);
            }
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

    private void notifyStreamListeners(String chunk) {
        for (Consumer<String> listener : streamListeners) {
            try {
                listener.accept(chunk);
            } catch (Exception e) {
                logger.warn("通知流式监听器时出错", e);
            }
        }
    }

    /**
     * 发送支持流式响应的消息。
     * 错误处理保证 onComplete / onError 二者只会被调用一次，避免重复回调污染 future。
     */
    private void sendMessageStreaming(String message,
                                      Consumer<String> onChunk,
                                      Consumer<String> onComplete,
                                      Consumer<Throwable> onError) {
        ObjectNode userMessage = createMessageNode("user", message);
        appendToHistory(userMessage);

        try {
            String requestJson = buildRequestJson(true);
            HttpHeaders headers = buildHeaders();
            byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);

            logger.debug("启动流式请求到 {}", apiUrl);

            final StringBuilder fullResponse = new StringBuilder();

            restTemplate.execute(apiUrl, HttpMethod.POST,
                    req -> {
                        req.getHeaders().putAll(headers);
                        req.getBody().write(requestBytes);
                    },
                    (ResponseExtractor<Void>) response -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isEmpty()) continue;
                                if (!line.startsWith(SSE_DATA_PREFIX)) continue;

                                String data = line.substring(SSE_DATA_PREFIX.length());
                                if (SSE_DONE.equals(data)) {
                                    logger.debug("流式传输完成");
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
                                            fullResponse.append(content);
                                            onChunk.accept(content);
                                        }
                                    }
                                } catch (Exception parseEx) {
                                    // 单条 SSE 解析失败时，仅记录日志，不中断整个流式响应
                                    logger.warn("解析流式数据出错: {}", parseEx.getMessage());
                                }
                            }
                        }
                        return null;
                    });

            String finalResponse = fullResponse.toString();
            logger.debug("流式响应完成，总长度: {}", finalResponse.length());

            appendToHistory(createMessageNode("assistant", finalResponse));
            onComplete.accept(finalResponse);
        } catch (Throwable t) {
            // 失败时回滚用户消息，避免对话上下文污染
            removeFromHistory(userMessage);
            logger.error("流式AI调用出错", t);
            onError.accept(t);
        }
    }

    private ObjectNode createMessageNode(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content == null ? "" : content);
        return node;
    }

    private void appendToHistory(ObjectNode message) {
        synchronized (historyLock) {
            messageHistory.add(message);
            while (messageHistory.size() > maxHistoryGroups * 2) {
                messageHistory.remove(0);
            }
        }
    }

    private void removeFromHistory(ObjectNode message) {
        synchronized (historyLock) {
            messageHistory.remove(message);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    /**
     * 构造带历史上下文的请求 JSON 字符串。
     */
    private String buildRequestJson(boolean stream) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("stream", stream);

        ArrayNode messagesNode = requestBody.putArray("messages");
        messagesNode.add(createMessageNode("system", systemPrompt));

        synchronized (historyLock) {
            for (ObjectNode msg : messageHistory) {
                messagesNode.add(msg);
            }
        }

        return objectMapper.writeValueAsString(requestBody);
    }
}
