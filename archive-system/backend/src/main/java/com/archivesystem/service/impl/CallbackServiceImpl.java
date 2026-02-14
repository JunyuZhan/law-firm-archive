package com.archivesystem.service.impl;

import com.archivesystem.entity.ExternalSource;
import com.archivesystem.mq.CallbackMessage;
import com.archivesystem.repository.ExternalSourceMapper;
import com.archivesystem.service.CallbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 回调服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackServiceImpl implements CallbackService {

    private final ObjectMapper objectMapper;
    private final ExternalSourceMapper externalSourceMapper;
    
    // 使用共享的RestTemplate（生产环境建议配置连接池）
    private final RestTemplate restTemplate = new RestTemplate();
    
    // 回调超时时间（秒）
    private static final int CALLBACK_TIMEOUT = 30;
    
    // 默认回调密钥（当无法获取配置时使用）
    private static final String DEFAULT_CALLBACK_SECRET = "archive-callback-secret";

    @Override
    public boolean sendCallback(CallbackMessage message) {
        if (message.getCallbackUrl() == null || message.getCallbackUrl().isEmpty()) {
            log.debug("回调URL为空，跳过回调: archiveId={}", message.getArchiveId());
            return true;
        }
        
        try {
            // 构建回调请求体
            Map<String, Object> requestBody = buildCallbackPayload(message);
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            // 获取回调密钥
            String callbackSecret = getCallbackSecret(message.getSourceType());
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String timestamp = String.valueOf(System.currentTimeMillis());
            headers.set("X-Callback-Timestamp", timestamp);
            headers.set("X-Callback-Source", "archive-system");
            
            // 生成签名（使用 HMAC-SHA256，与律所系统保持一致）
            String signature = generateHmacSignature(timestamp, requestBodyJson, callbackSecret);
            headers.set("X-Callback-Signature", signature);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("发送回调请求: url={}, archiveId={}", message.getCallbackUrl(), message.getArchiveId());
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    message.getCallbackUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("回调成功: archiveId={}, status={}", message.getArchiveId(), response.getStatusCode());
                return true;
            } else {
                log.warn("回调返回非成功状态: archiveId={}, status={}", 
                        message.getArchiveId(), response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("回调请求异常: archiveId={}, url={}, error={}", 
                    message.getArchiveId(), message.getCallbackUrl(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取回调密钥
     * 优先从外部来源配置获取，否则使用默认密钥
     */
    private String getCallbackSecret(String sourceType) {
        if (sourceType != null) {
            try {
                ExternalSource source = externalSourceMapper.selectBySourceCode(sourceType);
                if (source != null && source.getApiKey() != null && !source.getApiKey().isEmpty()) {
                    return source.getApiKey();
                }
            } catch (Exception e) {
                log.warn("获取外部来源配置失败，使用默认密钥: sourceType={}, error={}", sourceType, e.getMessage());
            }
        }
        return DEFAULT_CALLBACK_SECRET;
    }
    
    /**
     * 使用 HMAC-SHA256 生成签名（与律所系统保持一致）
     * 签名格式：HMAC-SHA256(timestamp + "\n" + body, secret)
     */
    private String generateHmacSignature(String timestamp, String body, String secret) {
        try {
            String data = timestamp + "\n" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("生成HMAC签名失败: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public void logFailedCallback(CallbackMessage message, String errorMessage) {
        log.error("回调最终失败: archiveId={}, archiveNo={}, callbackUrl={}, error={}",
                message.getArchiveId(), 
                message.getArchiveNo(),
                message.getCallbackUrl(),
                errorMessage);
        
        // TODO: 可扩展为持久化到数据库，用于后续人工处理或重试
        // callbackFailureRepository.save(CallbackFailure.builder()
        //         .archiveId(message.getArchiveId())
        //         .callbackUrl(message.getCallbackUrl())
        //         .errorMessage(errorMessage)
        //         .failedAt(LocalDateTime.now())
        //         .build());
    }
    
    /**
     * 构建回调请求体
     */
    private Map<String, Object> buildCallbackPayload(CallbackMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("archiveId", message.getArchiveId());
        payload.put("archiveNo", message.getArchiveNo());
        payload.put("sourceType", message.getSourceType());
        payload.put("sourceId", message.getSourceId());
        payload.put("status", message.getStatus());
        payload.put("successCount", message.getSuccessCount());
        payload.put("failedCount", message.getFailedCount());
        payload.put("totalCount", message.getTotalCount());
        payload.put("completedAt", message.getCompletedAt() != null ? 
                message.getCompletedAt().toString() : LocalDateTime.now().toString());
        
        if (message.getErrorMessage() != null) {
            payload.put("errorMessage", message.getErrorMessage());
        }
        
        return payload;
    }
    
}
