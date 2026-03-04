package com.archivesystem.service.impl;

import com.archivesystem.entity.CallbackRecord;
import com.archivesystem.entity.ExternalSource;
import com.archivesystem.mq.CallbackMessage;
import com.archivesystem.repository.CallbackRecordMapper;
import com.archivesystem.repository.ExternalSourceMapper;
import com.archivesystem.service.CallbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 回调服务实现
 */
@Slf4j
@Service
public class CallbackServiceImpl implements CallbackService {

    private final ObjectMapper objectMapper;
    private final ExternalSourceMapper externalSourceMapper;
    private final CallbackRecordMapper callbackRecordMapper;
    
    // 配置了超时的 RestTemplate
    private final RestTemplate restTemplate;
    
    // 回调超时时间（毫秒）
    private static final int CALLBACK_CONNECT_TIMEOUT = 5000;  // 连接超时 5 秒
    private static final int CALLBACK_READ_TIMEOUT = 30000;    // 读取超时 30 秒
    
    // 默认回调密钥（当无法获取配置时使用，生产环境应配置具体密钥）
    private static final String DEFAULT_CALLBACK_SECRET = "archive-callback-secret";

    public CallbackServiceImpl(ObjectMapper objectMapper, 
                               ExternalSourceMapper externalSourceMapper,
                               CallbackRecordMapper callbackRecordMapper) {
        this.objectMapper = objectMapper;
        this.externalSourceMapper = externalSourceMapper;
        this.callbackRecordMapper = callbackRecordMapper;
        
        // 配置 RestTemplate 超时
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CALLBACK_CONNECT_TIMEOUT);
        factory.setReadTimeout(CALLBACK_READ_TIMEOUT);
        this.restTemplate = new RestTemplate(factory);
    }

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
     * 按 sourceType 查询外部来源配置，否则使用默认密钥
     */
    private String getCallbackSecret(String sourceType) {
        if (sourceType != null) {
            try {
                // 先按 sourceCode 查，再按 sourceType 查
                ExternalSource source = externalSourceMapper.selectBySourceCode(sourceType);
                if (source == null) {
                    source = externalSourceMapper.selectBySourceType(sourceType);
                }
                if (source != null && source.getApiKey() != null && !source.getApiKey().isEmpty()) {
                    log.debug("使用外部来源密钥: sourceType={}, sourceCode={}", sourceType, source.getSourceCode());
                    return source.getApiKey();
                }
            } catch (Exception e) {
                log.warn("获取外部来源配置失败，使用默认密钥: sourceType={}, error={}", sourceType, e.getMessage());
            }
        }
        log.warn("未找到外部来源配置，使用默认密钥: sourceType={}", sourceType);
        return DEFAULT_CALLBACK_SECRET;
    }
    
    /**
     * 使用 HMAC-SHA256 生成签名（与律所系统保持一致）
     * 签名格式：HMAC-SHA256(timestamp + "\n" + body, secret)
     * 输出格式：十六进制字符串（与律所系统 hexDecode 兼容）
     */
    private String generateHmacSignature(String timestamp, String body, String secret) {
        try {
            String data = timestamp + "\n" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // 使用十六进制编码（与律所系统 hexDecode 保持一致）
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            log.error("生成HMAC签名失败: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public void logFailedCallback(CallbackMessage message, String errorMessage) {
        log.error("回调最终失败: archiveId={}, archiveNo={}, callbackUrl={}, error={}",
                message.getArchiveId(), 
                message.getArchiveNo(),
                message.getCallbackUrl(),
                errorMessage);
        
        // 持久化失败的回调记录，便于后续人工处理或重试
        try {
            CallbackRecord record = CallbackRecord.builder()
                    .archiveId(message.getArchiveId())
                    .archiveNo(message.getArchiveNo())
                    .callbackUrl(message.getCallbackUrl())
                    .callbackType(CallbackRecord.TYPE_FILE_TRANSFERRED)
                    .callbackStatus(CallbackRecord.STATUS_FAILED)
                    .requestBody(buildCallbackPayload(message))
                    .retryCount(message.getRetryCount())
                    .maxRetries(message.getMaxRetries())
                    .errorMessage(errorMessage)
                    .callbackAt(message.getCompletedAt())
                    .completedAt(LocalDateTime.now())
                    .build();
            
            callbackRecordMapper.insert(record);
            log.info("失败回调记录已保存: recordId={}, archiveId={}", record.getId(), message.getArchiveId());
        } catch (Exception e) {
            log.error("保存失败回调记录异常: archiveId={}, error={}", message.getArchiveId(), e.getMessage());
        }
    }
    
    /**
     * 构建回调请求体
     * 与管理系统 ArchiveCallbackRequest 字段保持一致
     */
    private Map<String, Object> buildCallbackPayload(CallbackMessage message) {
        Map<String, Object> payload = new HashMap<>();
        // 基础字段
        payload.put("archiveId", message.getArchiveId());
        payload.put("archiveNo", message.getArchiveNo());
        payload.put("sourceType", message.getSourceType());
        payload.put("sourceId", message.getSourceId());
        payload.put("sourceNo", message.getSourceNo());
        
        // 状态字段
        payload.put("status", message.getStatus());
        payload.put("message", buildStatusMessage(message));
        
        // 文件处理统计
        payload.put("successCount", message.getSuccessCount());
        payload.put("failedCount", message.getFailedCount());
        payload.put("totalCount", message.getTotalCount());
        
        // 时间和签名相关
        payload.put("completedAt", message.getCompletedAt() != null ? 
                message.getCompletedAt().toString() : LocalDateTime.now().toString());
        payload.put("timestamp", System.currentTimeMillis());
        
        // 错误信息
        if (message.getErrorMessage() != null) {
            payload.put("errorMessage", message.getErrorMessage());
        }
        
        return payload;
    }
    
    /**
     * 构建状态消息
     */
    private String buildStatusMessage(CallbackMessage message) {
        return switch (message.getStatus()) {
            case CallbackMessage.STATUS_SUCCESS -> "档案处理完成";
            case CallbackMessage.STATUS_PARTIAL -> 
                    String.format("档案部分处理完成，成功 %d/%d 个文件", 
                            message.getSuccessCount(), message.getTotalCount());
            case CallbackMessage.STATUS_FAILED -> "档案处理失败: " + message.getErrorMessage();
            default -> "档案处理状态: " + message.getStatus();
        };
    }
    
}
