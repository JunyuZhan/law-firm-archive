package com.lawfirm.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

/**
 * 大模型调用客户端
 * 支持多种 LLM API：
 * - 云端服务：OpenAI、Claude、通义千问、文心一言、智谱、DeepSeek、Moonshot、Yi、MiniMax
 * - 本地部署：Dify、Ollama、LocalAI、vLLM、Xinference、OneAPI、OpenAI 兼容 API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final ObjectMapper objectMapper;
    
    // AI 请求超时配置
    // 连接超时：30秒
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    // 读取超时：5分钟（DeepSeek R1 等推理模型可能需要更长时间）
    private static final int READ_TIMEOUT_MS = 300_000;
    // 请求超时（整个请求的最大时间）：6分钟
    private static final int REQUEST_TIMEOUT_MS = 360_000;
    // 最大重试次数
    private static final int MAX_RETRIES = 2;
    // 重试延迟（毫秒）
    private static final int RETRY_DELAY_MS = 1000;
    
    /**
     * 创建配置了超时时间的 RestTemplate
     * 使用 Apache HttpClient 5，更好地处理长连接和大响应
     */
    private RestTemplate createRestTemplate() {
        // Socket 配置：启用 TCP Keep-Alive，增加 socket 超时
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
                .setSoKeepAlive(true)  // 启用 TCP Keep-Alive
                .setTcpNoDelay(true)   // 禁用 Nagle 算法，减少延迟
                .build();
        
        // 连接配置
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
                .setSocketTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
                .build();
        
        // 配置 SSL 和普通连接
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        
        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(10);
        connectionManager.setDefaultSocketConfig(socketConfig);
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        
        // 配置请求超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
                .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(REQUEST_TIMEOUT_MS))
                .setConnectionKeepAlive(Timeout.ofMinutes(5))  // 保持连接活跃
                .build();
        
        // 创建 HttpClient
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.ofMinutes(2))
                .disableContentCompression()  // 禁用压缩，避免分块解码问题
                .build();
        
        // 创建 RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
    
    /**
     * 带重试的 HTTP POST 请求
     * 处理 "Premature EOF" 等临时网络错误
     */
    private <T> ResponseEntity<String> executeWithRetry(RestTemplate restTemplate, String url, 
            HttpEntity<T> request, String apiName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return restTemplate.postForEntity(url, request, String.class);
            } catch (ResourceAccessException e) {
                lastException = e;
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                
                // 检查是否是可重试的错误
                boolean isRetryable = errorMsg.contains("Premature EOF") 
                        || errorMsg.contains("Connection reset")
                        || errorMsg.contains("Read timed out")
                        || (e.getCause() instanceof IOException);
                
                if (isRetryable && attempt < MAX_RETRIES) {
                    log.warn("调用 {} API 出现临时错误 (尝试 {}/{}): {}，将在 {}ms 后重试", 
                            apiName, attempt, MAX_RETRIES, errorMsg, RETRY_DELAY_MS);
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("请求被中断", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        
        throw new RuntimeException("调用 " + apiName + " API 失败，已重试 " + MAX_RETRIES + " 次: " 
                + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    /**
     * 调用大模型生成文本
     * 
     * @param integration AI 集成配置
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return 生成的文本内容
     */
    public String generate(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        String code = integration.getIntegrationCode();
        
        log.info("调用大模型: code={}, name={}", code, integration.getIntegrationName());
        
        return switch (code) {
            // 云端大模型
            case "AI_OPENAI" -> callOpenAI(integration, systemPrompt, userPrompt);
            case "AI_CLAUDE" -> callClaude(integration, systemPrompt, userPrompt);
            case "AI_QWEN" -> callQwen(integration, systemPrompt, userPrompt);
            case "AI_WENXIN" -> callWenxin(integration, systemPrompt, userPrompt);
            case "AI_ZHIPU" -> callZhipu(integration, systemPrompt, userPrompt);
            case "AI_DEEPSEEK", "AI_DEEPSEEK_R1" -> callDeepSeek(integration, systemPrompt, userPrompt);
            case "AI_MOONSHOT" -> callMoonshot(integration, systemPrompt, userPrompt);
            case "AI_YI" -> callYi(integration, systemPrompt, userPrompt);
            case "AI_MINIMAX" -> callMinimax(integration, systemPrompt, userPrompt);
            
            // 本地部署/私有化大模型
            case "AI_DIFY" -> callDify(integration, systemPrompt, userPrompt);
            case "AI_OLLAMA" -> callOllama(integration, systemPrompt, userPrompt);
            case "AI_LOCALAI" -> callLocalAI(integration, systemPrompt, userPrompt);
            case "AI_VLLM" -> callVllm(integration, systemPrompt, userPrompt);
            case "AI_XINFERENCE" -> callXinference(integration, systemPrompt, userPrompt);
            case "AI_ONEAPI" -> callOneAPI(integration, systemPrompt, userPrompt);
            case "AI_OPENAI_COMPATIBLE" -> callOpenAICompatible(integration, systemPrompt, userPrompt);
            case "AI_CUSTOM" -> callCustomAPI(integration, systemPrompt, userPrompt);
            
            default -> throw new RuntimeException("不支持的 AI 模型: " + code);
        };
    }

    // ==================== 云端大模型 API ====================

    /**
     * 调用 OpenAI API (GPT-3.5/GPT-4)
     */
    private String callOpenAI(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 Claude API (Anthropic)
     */
    private String callClaude(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", integration.getApiKey());
        headers.set("anthropic-version", "2023-06-01");
        headers.setConnection("keep-alive");

        Map<String, Object> extraConfig = integration.getExtraConfig();
        String model = getConfigValue(extraConfig, "model", "claude-3-opus-20240229");
        int maxTokens = getConfigIntValue(extraConfig, "maxTokens", 4096);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", systemPrompt);
        body.put("messages", List.of(
                Map.of("role", "user", "content", userPrompt)
        ));

        String apiUrl = integration.getApiUrl().trim();
        if (!apiUrl.endsWith("/")) apiUrl += "/";
        apiUrl += "messages";

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "Claude");
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("调用 Claude API 失败", e);
            throw new RuntimeException("调用 Claude API 失败: " + e.getMessage());
        }
    }

    /**
     * 调用通义千问 API
     */
    private String callQwen(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + integration.getApiKey());
        headers.setConnection("keep-alive");

        Map<String, Object> extraConfig = integration.getExtraConfig();
        String model = getConfigValue(extraConfig, "model", "qwen-max");

        Map<String, Object> input = new HashMap<>();
        input.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", input);

        String apiUrl = integration.getApiUrl().trim();
        if (!apiUrl.endsWith("/")) apiUrl += "/";
        apiUrl += "services/aigc/text-generation/generation";

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "通义千问");
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("output").path("text").asText();
        } catch (Exception e) {
            log.error("调用通义千问 API 失败", e);
            throw new RuntimeException("调用通义千问 API 失败: " + e.getMessage());
        }
    }

    /**
     * 调用文心一言 API
     */
    private String callWenxin(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = createRestTemplate();
        
        String accessToken = getWenxinAccessToken(integration);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setConnection("keep-alive");

        Map<String, Object> body = new HashMap<>();
        body.put("messages", List.of(
                Map.of("role", "user", "content", systemPrompt + "\n\n" + userPrompt)
        ));

        Map<String, Object> extraConfig = integration.getExtraConfig();
        String model = getConfigValue(extraConfig, "model", "ernie-bot-4");
        
        String apiUrl = String.format("%s/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/%s?access_token=%s",
                integration.getApiUrl().trim(), model, accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "文心一言");
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("result").asText();
        } catch (Exception e) {
            log.error("调用文心一言 API 失败", e);
            throw new RuntimeException("调用文心一言 API 失败: " + e.getMessage());
        }
    }

    private String getWenxinAccessToken(ExternalIntegration integration) {
        RestTemplate restTemplate = createRestTemplate();
        String tokenUrl = String.format("https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                integration.getApiKey(), integration.getApiSecret());
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(tokenUrl, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("access_token").asText();
        } catch (Exception e) {
            log.error("获取文心一言 access_token 失败", e);
            throw new RuntimeException("获取文心一言 access_token 失败: " + e.getMessage());
        }
    }

    /**
     * 调用智谱清言 API (GLM)
     */
    private String callZhipu(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 DeepSeek API
     * 支持两种模型：
     * - deepseek-chat：对话模型，响应快速
     * - deepseek-reasoner：R1 推理模型，推理能力强，适合复杂法律文书
     */
    private String callDeepSeek(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        // 根据配置中的 model 决定使用哪个模型，默认 deepseek-chat
        return callOpenAIStyleWithDefaultModel(integration, systemPrompt, userPrompt, "chat/completions", "deepseek-chat");
    }

    /**
     * 调用 Moonshot (Kimi) API - 月之暗面
     */
    private String callMoonshot(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 Yi (零一万物) API
     */
    private String callYi(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 MiniMax API
     */
    private String callMinimax(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + integration.getApiKey());
        headers.setConnection("keep-alive");

        Map<String, Object> extraConfig = integration.getExtraConfig();
        String model = getConfigValue(extraConfig, "model", "abab5.5-chat");
        String groupId = getConfigValue(extraConfig, "groupId", "");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("sender_type", "BOT", "sender_name", "AI助手", "text", systemPrompt),
                Map.of("sender_type", "USER", "sender_name", "用户", "text", userPrompt)
        ));
        body.put("tokens_to_generate", 4096);
        body.put("temperature", 0.7);

        String apiUrl = integration.getApiUrl().trim();
        if (!apiUrl.endsWith("/")) apiUrl += "/";
        apiUrl += "text/chatcompletion_v2?GroupId=" + groupId;

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "MiniMax");
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("choices").get(0).path("messages").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("调用 MiniMax API 失败", e);
            throw new RuntimeException("调用 MiniMax API 失败: " + e.getMessage());
        }
    }

    // ==================== 本地部署/私有化大模型 API ====================

    /**
     * 调用 Dify API
     * Dify 是一个开源的 LLM 应用开发平台
     * API 文档: https://docs.dify.ai/
     */
    private String callDify(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + integration.getApiKey());
        headers.setConnection("keep-alive");

        Map<String, Object> extraConfig = integration.getExtraConfig();
        String user = getConfigValue(extraConfig, "user", "law-firm-user");
        String conversationId = getConfigValue(extraConfig, "conversationId", "");
        String responseMode = getConfigValue(extraConfig, "responseMode", "blocking"); // blocking 或 streaming

        Map<String, Object> body = new HashMap<>();
        // Dify 的查询格式
        body.put("query", systemPrompt + "\n\n" + userPrompt);
        body.put("user", user);
        body.put("response_mode", responseMode);
        
        if (!conversationId.isEmpty()) {
            body.put("conversation_id", conversationId);
        }

        // 支持自定义输入变量
        Map<String, Object> inputs = new HashMap<>();
        if (extraConfig != null && extraConfig.containsKey("inputs")) {
            Object inputsObj = extraConfig.get("inputs");
            if (inputsObj instanceof Map) {
                inputs.putAll((Map<String, Object>) inputsObj);
            }
        }
        body.put("inputs", inputs);

        String apiUrl = integration.getApiUrl().trim();
        if (!apiUrl.endsWith("/")) apiUrl += "/";
        
        // Dify 有两种 API：Chat 和 Completion
        String apiType = getConfigValue(extraConfig, "apiType", "chat");
        if ("completion".equals(apiType)) {
            apiUrl += "completion-messages";
        } else {
            apiUrl += "chat-messages";
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "Dify");
            JsonNode json = objectMapper.readTree(response.getBody());
            
            // Dify 返回格式
            if (json.has("answer")) {
                return json.path("answer").asText();
            } else if (json.has("text")) {
                return json.path("text").asText();
            } else {
                return json.toString();
            }
        } catch (Exception e) {
            log.error("调用 Dify API 失败", e);
            throw new RuntimeException("调用 Dify API 失败: " + e.getMessage());
        }
    }

    /**
     * 调用 Ollama API
     * Ollama 是本地运行大模型的工具，支持 Llama、Mistral、Gemma 等
     * API 文档: https://github.com/ollama/ollama/blob/main/docs/api.md
     */
    private String callOllama(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setConnection("keep-alive");

        Map<String, Object> extraConfig = integration.getExtraConfig();
        String model = getConfigValue(extraConfig, "model", "llama2");
        boolean stream = false; // 不使用流式

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        
        // Ollama 支持两种格式：chat 和 generate
        String apiType = getConfigValue(extraConfig, "apiType", "chat");
        
        String apiUrl = integration.getApiUrl().trim();
        if (!apiUrl.endsWith("/")) apiUrl += "/";
        
        if ("generate".equals(apiType)) {
            // generate API
            body.put("prompt", systemPrompt + "\n\n" + userPrompt);
            body.put("system", systemPrompt);
            apiUrl += "api/generate";
        } else {
            // chat API (默认)
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            apiUrl += "api/chat";
        }

        // 可选参数
        Map<String, Object> options = new HashMap<>();
        if (extraConfig != null) {
            if (extraConfig.containsKey("temperature")) {
                options.put("temperature", getConfigDoubleValue(extraConfig, "temperature", 0.7));
            }
            if (extraConfig.containsKey("num_predict")) {
                options.put("num_predict", getConfigIntValue(extraConfig, "num_predict", 4096));
            }
        }
        if (!options.isEmpty()) {
            body.put("options", options);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "Ollama");
            JsonNode json = objectMapper.readTree(response.getBody());
            
            // Ollama 返回格式
            if (json.has("message")) {
                return json.path("message").path("content").asText();
            } else if (json.has("response")) {
                return json.path("response").asText();
            } else {
                return json.toString();
            }
        } catch (Exception e) {
            log.error("调用 Ollama API 失败", e);
            throw new RuntimeException("调用 Ollama API 失败: " + e.getMessage());
        }
    }

    /**
     * 调用 LocalAI API
     * LocalAI 是 OpenAI 兼容的本地 AI 后端
     * API 文档: https://localai.io/api/
     */
    private String callLocalAI(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        // LocalAI 完全兼容 OpenAI API
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 vLLM API
     * vLLM 是高性能的 LLM 推理和服务框架
     * API 文档: https://docs.vllm.ai/
     */
    private String callVllm(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        // vLLM 支持 OpenAI 兼容 API
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 Xinference API
     * Xinference 是一个开源的分布式推理框架
     * API 文档: https://inference.readthedocs.io/
     */
    private String callXinference(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        // Xinference 支持 OpenAI 兼容 API
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 OneAPI
     * OneAPI 是一个多模型代理服务，统一管理多个 AI 模型
     * 支持 OpenAI 兼容 API
     */
    private String callOneAPI(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用 OpenAI 兼容 API
     * 适用于任何提供 OpenAI 兼容接口的服务
     */
    private String callOpenAICompatible(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
    }

    /**
     * 调用自定义 API
     * 支持自定义请求格式和响应解析
     */
    private String callCustomAPI(ExternalIntegration integration, String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setConnection("keep-alive");
        
        Map<String, Object> extraConfig = integration.getExtraConfig();
        
        // 添加自定义 Header
        String authHeader = getConfigValue(extraConfig, "authHeader", "Authorization");
        String authPrefix = getConfigValue(extraConfig, "authPrefix", "Bearer ");
        if (integration.getApiKey() != null && !integration.getApiKey().isEmpty()) {
            headers.set(authHeader, authPrefix + integration.getApiKey());
        }
        
        // 自定义额外 Headers
        if (extraConfig != null && extraConfig.containsKey("headers")) {
            Object headersObj = extraConfig.get("headers");
            if (headersObj instanceof Map) {
                ((Map<String, String>) headersObj).forEach(headers::set);
            }
        }

        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        
        // 支持自定义请求体模板
        String requestTemplate = getConfigValue(extraConfig, "requestTemplate", "openai");
        
        switch (requestTemplate) {
            case "openai" -> {
                String model = getConfigValue(extraConfig, "model", "default");
                body.put("model", model);
                body.put("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ));
            }
            case "simple" -> {
                body.put("prompt", systemPrompt + "\n\n" + userPrompt);
            }
            case "custom" -> {
                // 从 extraConfig 获取自定义字段
                String promptField = getConfigValue(extraConfig, "promptField", "prompt");
                String systemField = getConfigValue(extraConfig, "systemField", "system");
                body.put(promptField, userPrompt);
                body.put(systemField, systemPrompt);
                // 添加额外字段
                if (extraConfig.containsKey("extraFields")) {
                    Object extraFieldsObj = extraConfig.get("extraFields");
                    if (extraFieldsObj instanceof Map) {
                        body.putAll((Map<String, Object>) extraFieldsObj);
                    }
                }
            }
            default -> {
                body.put("prompt", systemPrompt + "\n\n" + userPrompt);
            }
        }

        String apiUrl = integration.getApiUrl().trim();
        String endpoint = getConfigValue(extraConfig, "endpoint", "");
        if (!endpoint.isEmpty()) {
            if (!apiUrl.endsWith("/")) apiUrl += "/";
            apiUrl += endpoint;
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "自定义 API");
            JsonNode json = objectMapper.readTree(response.getBody());
            
            // 自定义响应解析
            String responsePath = getConfigValue(extraConfig, "responsePath", "choices.0.message.content");
            return extractJsonPath(json, responsePath);
        } catch (Exception e) {
            log.error("调用自定义 API 失败", e);
            throw new RuntimeException("调用自定义 API 失败: " + e.getMessage());
        }
    }

    // ==================== 通用方法 ====================

    /**
     * 调用 OpenAI 风格的 API（大多数模型都兼容）
     */
    private String callOpenAIStyle(ExternalIntegration integration, String systemPrompt, String userPrompt, String endpoint) {
        return callOpenAIStyleWithDefaultModel(integration, systemPrompt, userPrompt, endpoint, "gpt-4");
    }

    /**
     * 调用 OpenAI 风格的 API，支持指定默认模型
     */
    private String callOpenAIStyleWithDefaultModel(ExternalIntegration integration, String systemPrompt, String userPrompt, String endpoint, String defaultModel) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(integration.getApiKey());
        headers.setConnection("keep-alive");  // 保持连接活跃

        Map<String, Object> extraConfig = integration.getExtraConfig();
        String model = getConfigValue(extraConfig, "model", defaultModel);
        int maxTokens = getConfigIntValue(extraConfig, "maxTokens", 4096);
        double temperature = getConfigDoubleValue(extraConfig, "temperature", 0.7);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        String apiUrl = integration.getApiUrl().trim();
        if (!apiUrl.endsWith("/")) apiUrl += "/";
        apiUrl += endpoint;

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String apiName = integration.getIntegrationName();
        
        try {
            ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, apiName);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("调用 AI API 失败: {}", apiName, e);
            throw new RuntimeException("调用 " + apiName + " API 失败: " + e.getMessage());
        }
    }

    /**
     * 从 JSON 中提取指定路径的值
     * 支持格式：choices.0.message.content
     */
    private String extractJsonPath(JsonNode json, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = json;
        
        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return "";
            }
            
            try {
                int index = Integer.parseInt(part);
                current = current.get(index);
            } catch (NumberFormatException e) {
                current = current.path(part);
            }
        }
        
        return current != null ? current.asText() : "";
    }

    // ===== 辅助方法 =====
    
    private String getConfigValue(Map<String, Object> config, String key, String defaultValue) {
        if (config == null || !config.containsKey(key)) return defaultValue;
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getConfigIntValue(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || !config.containsKey(key)) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double getConfigDoubleValue(Map<String, Object> config, String key, double defaultValue) {
        if (config == null || !config.containsKey(key)) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
