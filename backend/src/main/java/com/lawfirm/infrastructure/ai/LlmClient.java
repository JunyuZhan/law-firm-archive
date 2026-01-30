package com.lawfirm.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 大模型调用客户端.
 *
 * <p>支持多种 LLM API： - 云端服务：OpenAI、Claude、通义千问、文心一言、智谱、DeepSeek、Moonshot、Yi、MiniMax -
 * 本地部署：Dify、Ollama、LocalAI、vLLM、Xinference、OneAPI、OpenAI 兼容 API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /** AI使用量记录器 */
  private final AiUsageRecorder usageRecorder;

  /** 连接超时时间（毫秒） */
  private static final int CONNECT_TIMEOUT_MS = 30_000;

  /** 读取超时时间（毫秒） */
  private static final int READ_TIMEOUT_MS = 300_000;

  /** 请求超时时间（毫秒） */
  private static final int REQUEST_TIMEOUT_MS = 360_000;

  /** 最大重试次数 */
  private static final int MAX_RETRIES = 2;

  /** 重试延迟（毫秒） */
  private static final int RETRY_DELAY_MS = 1000;

  /** 连接池最大连接数 */
  private static final int MAX_TOTAL_CONNECTIONS = 50;

  /** 每个路由的最大连接数 */
  private static final int MAX_CONNECTIONS_PER_ROUTE = 10;

  /** 默认最大Token数 */
  private static final int DEFAULT_MAX_TOKENS = 4096;

  /** 默认温度参数 */
  private static final double DEFAULT_TEMPERATURE = 0.7;

  /** 保持连接活跃时间（分钟） */
  private static final int KEEP_ALIVE_MINUTES = 5;

  /** 空闲连接清理时间（分钟） */
  private static final int IDLE_CONNECTION_MINUTES = 2;

  /**
   * 创建配置了超时时间的 RestTemplate 使用 Apache HttpClient 5，更好地处理长连接和大响应
   *
   * @return 配置好的 RestTemplate 实例
   */
  private RestTemplate createRestTemplate() {
    // Socket 配置：启用 TCP Keep-Alive，增加 socket 超时
    SocketConfig socketConfig =
        SocketConfig.custom()
            .setSoTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
            .setSoKeepAlive(true) // 启用 TCP Keep-Alive
            .setTcpNoDelay(true) // 禁用 Nagle 算法，减少延迟
            .build();

    // 连接配置
    ConnectionConfig connectionConfig =
        ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
            .setSocketTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
            .build();

    // 配置 SSL 和普通连接
    Registry<ConnectionSocketFactory> socketFactoryRegistry =
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", SSLConnectionSocketFactory.getSocketFactory())
            .build();

    // 配置连接池
    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
    connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
    connectionManager.setDefaultSocketConfig(socketConfig);
    connectionManager.setDefaultConnectionConfig(connectionConfig);

    // 配置请求超时（连接超时已在ConnectionConfig中配置）
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(REQUEST_TIMEOUT_MS))
            .setConnectionKeepAlive(Timeout.ofMinutes(KEEP_ALIVE_MINUTES)) // 保持连接活跃
            .build();

    // 创建 HttpClient
    HttpClient httpClient =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictExpiredConnections()
            .evictIdleConnections(Timeout.ofMinutes(IDLE_CONNECTION_MINUTES))
            .disableContentCompression() // 禁用压缩，避免分块解码问题
            .build();

    // 创建 RestTemplate
    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    return new RestTemplate(factory);
  }

  /**
   * 带重试的 HTTP POST 请求 处理 "Premature EOF" 等临时网络错误
   *
   * @param restTemplate RestTemplate 实例
   * @param url 请求 URL
   * @param request 请求实体
   * @param apiName API 名称（用于日志）
   * @param <T> 请求实体类型
   * @return 响应实体
   */
  private <T> ResponseEntity<String> executeWithRetry(
      final RestTemplate restTemplate,
      final String url,
      final HttpEntity<T> request,
      final String apiName) {
    Exception lastException = null;

    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        return restTemplate.postForEntity(url, request, String.class);
      } catch (ResourceAccessException e) {
        lastException = e;
        String errorMsg = e.getMessage() != null ? e.getMessage() : "";

        // 检查是否是可重试的错误
        boolean isRetryable =
            errorMsg.contains("Premature EOF")
                || errorMsg.contains("Connection reset")
                || errorMsg.contains("Read timed out")
                || (e.getCause() instanceof IOException);

        if (isRetryable && attempt < MAX_RETRIES) {
          log.warn(
              "调用 {} API 出现临时错误 (尝试 {}/{}): {}，将在 {}ms 后重试",
              apiName,
              attempt,
              MAX_RETRIES,
              errorMsg,
              RETRY_DELAY_MS);
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

    throw new RuntimeException(
        "调用 "
            + apiName
            + " API 失败，已重试 "
            + MAX_RETRIES
            + " 次: "
            + (lastException != null ? lastException.getMessage() : "未知错误"));
  }

  /**
   * 调用大模型生成文本（简化版，向后兼容）
   *
   * @param integration AI 集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的文本内容
   */
  public String generate(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    return generate(integration, systemPrompt, userPrompt, null, null, null);
  }

  /**
   * 调用大模型生成文本（完整版，支持使用量记录）
   *
   * @param integration AI 集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @param requestType 请求类型（DOCUMENT_GENERATE/CHAT/SUMMARY等）
   * @param businessType 业务类型（MATTER/PERSONAL）
   * @param businessId 业务ID（如项目ID）
   * @return 生成的文本内容
   */
  public String generate(
      final ExternalIntegration integration,
      final String systemPrompt,
      final String userPrompt,
      final String requestType,
      final String businessType,
      final Long businessId) {
    String code = integration.getIntegrationCode();
    long startTime = System.currentTimeMillis();
    String responseBody = null;
    boolean success = true;
    String errorMessage = null;

    log.info(
        "调用大模型: code={}, name={}, requestType={}",
        code,
        integration.getIntegrationName(),
        requestType);

    try {
      // 调用并获取原始响应
      GenerateResult result =
          switch (code) {
              // 云端大模型
            case "AI_OPENAI" -> callOpenAIWithResponse(integration, systemPrompt, userPrompt);
            case "AI_CLAUDE" -> callClaudeWithResponse(integration, systemPrompt, userPrompt);
            case "AI_QWEN" -> callQwenWithResponse(integration, systemPrompt, userPrompt);
            case "AI_WENXIN" -> callWenxinWithResponse(integration, systemPrompt, userPrompt);
            case "AI_ZHIPU" -> callZhipuWithResponse(integration, systemPrompt, userPrompt);
            case "AI_DEEPSEEK", "AI_DEEPSEEK_R1" -> callDeepSeekWithResponse(
                integration, systemPrompt, userPrompt);
            case "AI_MOONSHOT" -> callMoonshotWithResponse(integration, systemPrompt, userPrompt);
            case "AI_YI" -> callYiWithResponse(integration, systemPrompt, userPrompt);
            case "AI_MINIMAX" -> callMinimaxWithResponse(integration, systemPrompt, userPrompt);

              // 本地部署/私有化大模型
            case "AI_DIFY" -> callDifyWithResponse(integration, systemPrompt, userPrompt);
            case "AI_OLLAMA" -> callOllamaWithResponse(integration, systemPrompt, userPrompt);
            case "AI_LOCALAI" -> callLocalAIWithResponse(integration, systemPrompt, userPrompt);
            case "AI_VLLM" -> callVllmWithResponse(integration, systemPrompt, userPrompt);
            case "AI_XINFERENCE" -> callXinferenceWithResponse(
                integration, systemPrompt, userPrompt);
            case "AI_ONEAPI" -> callOneAPIWithResponse(integration, systemPrompt, userPrompt);
            case "AI_OPENAI_COMPATIBLE" -> callOpenAICompatibleWithResponse(
                integration, systemPrompt, userPrompt);
            case "AI_CUSTOM" -> callCustomAPIWithResponse(integration, systemPrompt, userPrompt);

            default -> throw new RuntimeException("不支持的 AI 模型: " + code);
          };

      responseBody = result.getResponseBody();
      return result.getContent();

    } catch (Exception e) {
      success = false;
      errorMessage = e.getMessage();
      throw e;

    } finally {
      // 异步记录使用量（不阻塞主流程）
      long duration = System.currentTimeMillis() - startTime;
      if (usageRecorder != null) {
        usageRecorder.recordUsage(
            integration,
            requestType,
            businessType,
            businessId,
            responseBody,
            duration,
            success,
            errorMessage);
      }
    }
  }

  /** 生成结果内部类（包含内容和原始响应）. */
  private static class GenerateResult {
    /** 生成的内容 */
    private String content;

    /** 原始响应体 */
    private String responseBody;

    /**
     * 构造函数.
     *
     * @param content 生成的内容
     * @param responseBody 原始响应体
     */
    GenerateResult(final String content, final String responseBody) {
      this.content = content;
      this.responseBody = responseBody;
    }

    /**
     * 获取生成的内容.
     *
     * @return 生成的内容
     */
    public String getContent() {
      return content;
    }

    /**
     * 获取原始响应体.
     *
     * @return 原始响应体
     */
    public String getResponseBody() {
      return responseBody;
    }
  }

  // ==================== 带响应体的调用方法（用于使用量记录） ====================

  /**
   * 调用OpenAI并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callOpenAIWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用Claude并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callClaudeWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String content = callClaude(integration, systemPrompt, userPrompt);
    return new GenerateResult(content, null); // Claude方法需要重构才能获取响应体
  }

  /**
   * 调用通义千问并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callQwenWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用文心一言并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callWenxinWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String content = callWenxin(integration, systemPrompt, userPrompt);
    return new GenerateResult(content, null);
  }

  /**
   * 调用智谱AI并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callZhipuWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用DeepSeek并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callDeepSeekWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用Moonshot并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callMoonshotWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用Yi并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callYiWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用Minimax并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callMinimaxWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String content = callMinimax(integration, systemPrompt, userPrompt);
    return new GenerateResult(content, null);
  }

  /**
   * 调用Dify并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callDifyWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String content = callDify(integration, systemPrompt, userPrompt);
    return new GenerateResult(content, null);
  }

  /**
   * 调用Ollama并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callOllamaWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String content = callOllama(integration, systemPrompt, userPrompt);
    return new GenerateResult(content, null);
  }

  /**
   * 调用LocalAI并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callLocalAIWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用vLLM并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callVllmWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用Xinference并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callXinferenceWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用OneAPI并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callOneAPIWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用OpenAI兼容API并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callOpenAICompatibleWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String responseBody =
        callOpenAIStyleRaw(integration, systemPrompt, userPrompt, "chat/completions");
    String content = extractOpenAIContent(responseBody);
    return new GenerateResult(content, responseBody);
  }

  /**
   * 调用自定义API并返回响应.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成结果
   */
  private GenerateResult callCustomAPIWithResponse(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    String content = callCustomAPI(integration, systemPrompt, userPrompt);
    return new GenerateResult(content, null);
  }

  /**
   * OpenAI风格API调用（返回原始响应）.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @param endpoint API端点
   * @return 原始响应体
   */
  private String callOpenAIStyleRaw(
      final ExternalIntegration integration,
      final String systemPrompt,
      final String userPrompt,
      final String endpoint) {
    RestTemplate restTemplate = createRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(integration.getApiKey());
    headers.setConnection("keep-alive");

    Map<String, Object> extraConfig = integration.getExtraConfig();
    String model = getConfigValue(extraConfig, "model", "gpt-3.5-turbo");
    int maxTokens = getConfigIntValue(extraConfig, "maxTokens", DEFAULT_MAX_TOKENS);
    double temperature = getConfigDoubleValue(extraConfig, "temperature", DEFAULT_TEMPERATURE);

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("max_tokens", maxTokens);
    body.put("temperature", temperature);
    body.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)));

    String apiUrl = integration.getApiUrl().trim();
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }
    apiUrl += endpoint;

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<String> response =
          executeWithRetry(restTemplate, apiUrl, request, "OpenAI Style");
      return response.getBody();
    } catch (Exception e) {
      log.error("调用 OpenAI Style API 失败", e);
      throw new RuntimeException("调用 OpenAI Style API 失败: " + e.getMessage());
    }
  }

  /**
   * 从OpenAI风格响应中提取内容.
   *
   * @param responseBody 响应体
   * @return 提取的内容
   */
  private String extractOpenAIContent(final String responseBody) {
    try {
      JsonNode json = objectMapper.readTree(responseBody);
      return json.path("choices").get(0).path("message").path("content").asText();
    } catch (Exception e) {
      log.error("解析 OpenAI 响应失败", e);
      throw new RuntimeException("解析 OpenAI 响应失败: " + e.getMessage());
    }
  }

  // ==================== 云端大模型 API ====================

  /**
   * 调用OpenAI API (GPT-3.5/GPT-4).
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callOpenAI(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用Claude API (Anthropic).
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  private String callClaude(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    RestTemplate restTemplate = createRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", integration.getApiKey());
    headers.set("anthropic-version", "2023-06-01");
    headers.setConnection("keep-alive");

    Map<String, Object> extraConfig = integration.getExtraConfig();
    String model = getConfigValue(extraConfig, "model", "claude-3-opus-20240229");
    int maxTokens = getConfigIntValue(extraConfig, "maxTokens", DEFAULT_MAX_TOKENS);

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("max_tokens", maxTokens);
    body.put("system", systemPrompt);
    body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

    String apiUrl = integration.getApiUrl().trim();
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }
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
   * 调用通义千问API.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callQwen(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    RestTemplate restTemplate = createRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + integration.getApiKey());
    headers.setConnection("keep-alive");

    Map<String, Object> extraConfig = integration.getExtraConfig();
    String model = getConfigValue(extraConfig, "model", "qwen-max");

    Map<String, Object> input = new HashMap<>();
    input.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)));

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("input", input);

    String apiUrl = integration.getApiUrl().trim();
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }
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
   * 调用文心一言API.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  private String callWenxin(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    RestTemplate restTemplate = createRestTemplate();

    String accessToken = getWenxinAccessToken(integration);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setConnection("keep-alive");

    Map<String, Object> body = new HashMap<>();
    body.put(
        "messages", List.of(Map.of("role", "user", "content", systemPrompt + "\n\n" + userPrompt)));

    Map<String, Object> extraConfig = integration.getExtraConfig();
    String model = getConfigValue(extraConfig, "model", "ernie-bot-4");

    String apiUrl =
        String.format(
            "%s/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/%s?access_token=%s",
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

  /**
   * 获取文心一言的访问令牌.
   *
   * @param integration AI集成配置
   * @return 访问令牌
   */
  private String getWenxinAccessToken(final ExternalIntegration integration) {
    RestTemplate restTemplate = createRestTemplate();
    String tokenUrl =
        String.format(
            "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
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
   * 调用智谱清言API (GLM).
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callZhipu(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用DeepSeek API.
   *
   * <p>支持两种模型：
   *
   * <ul>
   *   <li>deepseek-chat：对话模型，响应快速
   *   <li>deepseek-reasoner：R1推理模型，推理能力强，适合复杂法律文书
   * </ul>
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callDeepSeek(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    // 根据配置中的 model 决定使用哪个模型，默认 deepseek-chat
    return callOpenAIStyleWithDefaultModel(
        integration, systemPrompt, userPrompt, "chat/completions", "deepseek-chat");
  }

  /**
   * 调用Moonshot (Kimi) API - 月之暗面.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callMoonshot(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用Yi (零一万物) API.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callYi(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用MiniMax API.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  private String callMinimax(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
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
    body.put(
        "messages",
        List.of(
            Map.of("sender_type", "BOT", "sender_name", "AI助手", "text", systemPrompt),
            Map.of("sender_type", "USER", "sender_name", "用户", "text", userPrompt)));
    body.put("tokens_to_generate", DEFAULT_MAX_TOKENS);
    body.put("temperature", DEFAULT_TEMPERATURE);

    String apiUrl = integration.getApiUrl().trim();
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }
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
   * 调用Dify API.
   *
   * <p>Dify是一个开源的LLM应用开发平台
   *
   * <p>API文档: https://docs.dify.ai/
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  private String callDify(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    RestTemplate restTemplate = createRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + integration.getApiKey());
    headers.setConnection("keep-alive");

    Map<String, Object> extraConfig = integration.getExtraConfig();
    String user = getConfigValue(extraConfig, "user", "law-firm-user");
    String conversationId = getConfigValue(extraConfig, "conversationId", "");
    String responseMode =
        getConfigValue(extraConfig, "responseMode", "blocking"); // blocking 或 streaming

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
      if (inputsObj instanceof Map<?, ?>) {
        @SuppressWarnings("unchecked")
        Map<String, Object> inputsMap = (Map<String, Object>) inputsObj;
        inputs.putAll(inputsMap);
      }
    }
    body.put("inputs", inputs);

    String apiUrl = integration.getApiUrl().trim();
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }

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
   * 调用Ollama API.
   *
   * <p>Ollama是本地运行大模型的工具，支持Llama、Mistral、Gemma等
   *
   * <p>API文档: https://github.com/ollama/ollama/blob/main/docs/api.md
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  private String callOllama(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
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
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }

    if ("generate".equals(apiType)) {
      // generate API
      body.put("prompt", systemPrompt + "\n\n" + userPrompt);
      body.put("system", systemPrompt);
      apiUrl += "api/generate";
    } else {
      // chat API (默认)
      body.put(
          "messages",
          List.of(
              Map.of("role", "system", "content", systemPrompt),
              Map.of("role", "user", "content", userPrompt)));
      apiUrl += "api/chat";
    }

    // 可选参数
    Map<String, Object> options = new HashMap<>();
    if (extraConfig != null) {
      if (extraConfig.containsKey("temperature")) {
        options.put(
            "temperature", getConfigDoubleValue(extraConfig, "temperature", DEFAULT_TEMPERATURE));
      }
      if (extraConfig.containsKey("num_predict")) {
        options.put(
            "num_predict", getConfigIntValue(extraConfig, "num_predict", DEFAULT_MAX_TOKENS));
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
   * 调用LocalAI API.
   *
   * <p>LocalAI是OpenAI兼容的本地AI后端
   *
   * <p>API文档: https://localai.io/api/
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callLocalAI(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    // LocalAI 完全兼容 OpenAI API
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用vLLM API.
   *
   * <p>vLLM是高性能的LLM推理和服务框架
   *
   * <p>API文档: https://docs.vllm.ai/
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callVllm(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    // vLLM 支持 OpenAI 兼容 API
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用Xinference API.
   *
   * <p>Xinference是一个开源的分布式推理框架
   *
   * <p>API文档: https://inference.readthedocs.io/
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callXinference(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    // Xinference 支持 OpenAI 兼容 API
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用OneAPI.
   *
   * <p>OneAPI是一个多模型代理服务，统一管理多个AI模型
   *
   * <p>支持OpenAI兼容API
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callOneAPI(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用OpenAI兼容API.
   *
   * <p>适用于任何提供OpenAI兼容接口的服务
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  @SuppressWarnings("unused")
  private String callOpenAICompatible(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
    return callOpenAIStyle(integration, systemPrompt, userPrompt, "chat/completions");
  }

  /**
   * 调用自定义API.
   *
   * <p>支持自定义请求格式和响应解析
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @return 生成的内容
   */
  private String callCustomAPI(
      final ExternalIntegration integration, final String systemPrompt, final String userPrompt) {
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
      if (headersObj instanceof Map<?, ?>) {
        @SuppressWarnings("unchecked")
        Map<String, String> headersMap = (Map<String, String>) headersObj;
        headersMap.forEach(headers::set);
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
        body.put(
            "messages",
            List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
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
        if (extraConfig != null && extraConfig.containsKey("extraFields")) {
          Object extraFieldsObj = extraConfig.get("extraFields");
          if (extraFieldsObj instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> extraFieldsMap = (Map<String, Object>) extraFieldsObj;
            body.putAll(extraFieldsMap);
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
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/";
      }
      apiUrl += endpoint;
    }

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<String> response = executeWithRetry(restTemplate, apiUrl, request, "自定义 API");
      JsonNode json = objectMapper.readTree(response.getBody());

      // 自定义响应解析
      String responsePath =
          getConfigValue(extraConfig, "responsePath", "choices.0.message.content");
      return extractJsonPath(json, responsePath);
    } catch (Exception e) {
      log.error("调用自定义 API 失败", e);
      throw new RuntimeException("调用自定义 API 失败: " + e.getMessage());
    }
  }

  /**
   * 调用OpenAI风格的API（大多数模型都兼容）.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @param endpoint API端点
   * @return 生成的内容
   */
  private String callOpenAIStyle(
      final ExternalIntegration integration,
      final String systemPrompt,
      final String userPrompt,
      final String endpoint) {
    return callOpenAIStyleWithDefaultModel(
        integration, systemPrompt, userPrompt, endpoint, "gpt-4");
  }

  /**
   * 调用OpenAI风格的API，支持指定默认模型.
   *
   * @param integration AI集成配置
   * @param systemPrompt 系统提示词
   * @param userPrompt 用户提示词
   * @param endpoint API端点
   * @param defaultModel 默认模型名称
   * @return 生成的内容
   */
  private String callOpenAIStyleWithDefaultModel(
      final ExternalIntegration integration,
      final String systemPrompt,
      final String userPrompt,
      final String endpoint,
      final String defaultModel) {
    RestTemplate restTemplate = createRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(integration.getApiKey());
    headers.setConnection("keep-alive"); // 保持连接活跃

    Map<String, Object> extraConfig = integration.getExtraConfig();
    String model = getConfigValue(extraConfig, "model", defaultModel);
    int maxTokens = getConfigIntValue(extraConfig, "maxTokens", DEFAULT_MAX_TOKENS);
    double temperature = getConfigDoubleValue(extraConfig, "temperature", DEFAULT_TEMPERATURE);

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("max_tokens", maxTokens);
    body.put("temperature", temperature);
    body.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)));

    String apiUrl = integration.getApiUrl().trim();
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }
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
   * 从JSON中提取指定路径的值.
   *
   * <p>支持格式：choices.0.message.content
   *
   * @param json JSON节点
   * @param path 路径字符串
   * @return 提取的值
   */
  private String extractJsonPath(final JsonNode json, final String path) {
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

  /**
   * 从配置中获取字符串值.
   *
   * @param config 配置Map
   * @param key 键名
   * @param defaultValue 默认值
   * @return 配置值或默认值
   */
  private String getConfigValue(
      final Map<String, Object> config, final String key, final String defaultValue) {
    if (config == null || !config.containsKey(key)) {
      return defaultValue;
    }
    Object value = config.get(key);
    return value != null ? value.toString() : defaultValue;
  }

  /**
   * 从配置中获取整数值.
   *
   * @param config 配置Map
   * @param key 键名
   * @param defaultValue 默认值
   * @return 配置值或默认值
   */
  private int getConfigIntValue(
      final Map<String, Object> config, final String key, final int defaultValue) {
    if (config == null || !config.containsKey(key)) {
      return defaultValue;
    }
    Object value = config.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * 从配置中获取双精度浮点数值.
   *
   * @param config 配置Map
   * @param key 键名
   * @param defaultValue 默认值
   * @return 配置值或默认值
   */
  private double getConfigDoubleValue(
      final Map<String, Object> config, final String key, final double defaultValue) {
    if (config == null || !config.containsKey(key)) {
      return defaultValue;
    }
    Object value = config.get(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    try {
      return Double.parseDouble(value.toString());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
