package com.archivesystem.service.impl;

import com.archivesystem.entity.CallbackRecord;
import com.archivesystem.entity.ExternalSource;
import com.archivesystem.mq.CallbackMessage;
import com.archivesystem.repository.CallbackRecordMapper;
import com.archivesystem.repository.ExternalSourceMapper;
import com.archivesystem.security.OutboundUrlValidator;
import com.archivesystem.service.CallbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 回调服务实现
 * @author junyuzhan
 */
@Slf4j
@Service
public class CallbackServiceImpl implements CallbackService {
    private static final String CALLBACK_FAILURE_PUBLIC_MESSAGE = "回调失败，请联系系统管理员查看系统日志";
    private static final String ARCHIVE_PROCESS_FAILURE_PUBLIC_MESSAGE = "档案处理失败，请联系系统管理员查看系统日志";

    private final ObjectMapper objectMapper;
    private final ExternalSourceMapper externalSourceMapper;
    private final CallbackRecordMapper callbackRecordMapper;
    private final OutboundUrlValidator outboundUrlValidator;
    
    private final RestTemplate restTemplate;
    
    public CallbackServiceImpl(ObjectMapper objectMapper, 
                               ExternalSourceMapper externalSourceMapper,
                               CallbackRecordMapper callbackRecordMapper,
                               OutboundUrlValidator outboundUrlValidator,
                               @Qualifier("callbackRestTemplate") RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.externalSourceMapper = externalSourceMapper;
        this.callbackRecordMapper = callbackRecordMapper;
        this.outboundUrlValidator = outboundUrlValidator;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean sendCallback(CallbackMessage message) {
        if (message.getCallbackUrl() == null || message.getCallbackUrl().isEmpty()) {
            log.debug("回调URL为空，跳过回调: archiveId={}", message.getArchiveId());
            return true;
        }
        
        try {
            outboundUrlValidator.validate(message.getCallbackUrl(), "回调地址");
            // 构建回调请求体
            Map<String, Object> requestBody = buildCallbackPayload(message);
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            // 获取回调密钥
            String callbackSecret = getCallbackSecret(message.getSourceType());
            if (callbackSecret == null || callbackSecret.isBlank()) {
                log.error("未找到可用的回调签名密钥，拒绝发送未签名回调: archiveId={}, sourceType={}",
                        message.getArchiveId(), message.getSourceType());
                return false;
            }
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            headers.set("X-Callback-Timestamp", timestamp);
            headers.set("X-Callback-Nonce", nonce);
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
     * 获取回调密钥。
     * 仅允许使用已配置来源的 API Key 作为签名密钥，避免落回弱默认值。
     */
    private String getCallbackSecret(String sourceType) {
        if (externalSourceMapper == null) {
            log.warn("ExternalSourceMapper 未注入，无法解析回调签名密钥");
            return null;
        }
        if (sourceType != null && !sourceType.isBlank()) {
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
                log.warn("获取外部来源配置失败: sourceType={}, error={}", sourceType, e.getMessage());
            }
        }
        log.warn("未找到启用的外部来源密钥配置: sourceType={}", sourceType);
        return null;
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
        return HexFormat.of().formatHex(bytes);
    }

    @Override
    public void logFailedCallback(CallbackMessage message, String errorMessage) {
        String publicErrorMessage = sanitizeCallbackErrorMessage(errorMessage);
        log.error("回调最终失败: archiveId={}, archiveNo={}, callbackUrl={}, error={}",
                message.getArchiveId(), 
                message.getArchiveNo(),
                message.getCallbackUrl(),
                publicErrorMessage);
        
        // 持久化失败的回调记录，便于后续人工处理或重试
        try {
            if (callbackRecordMapper == null) {
                log.warn("CallbackRecordMapper 未注入，跳过失败回调持久化: archiveId={}", message.getArchiveId());
                return;
            }
            CallbackRecord record = CallbackRecord.builder()
                    .archiveId(message.getArchiveId())
                    .archiveNo(message.getArchiveNo())
                    .callbackUrl(message.getCallbackUrl())
                    .callbackType(CallbackRecord.TYPE_FILE_TRANSFERRED)
                    .callbackStatus(CallbackRecord.STATUS_FAILED)
                    .requestBody(buildCallbackPayload(message))
                    .retryCount(message.getRetryCount())
                    .maxRetries(message.getMaxRetries())
                    .errorMessage(publicErrorMessage)
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
        
        // 回调验签只以请求头中的 timestamp 为准，避免头/body 双时间戳口径混用
        payload.put("completedAt", message.getCompletedAt() != null ? 
                message.getCompletedAt().toString() : LocalDateTime.now().toString());
        
        // 错误信息
        if (message.getErrorMessage() != null) {
            payload.put("errorMessage", sanitizeArchiveProcessErrorMessage(message.getErrorMessage()));
        }
        
        return payload;
    }
    
    /**
     * 构建状态消息
     */
    private String buildStatusMessage(CallbackMessage message) {
        if (message.getStatus() == null) {
            return "档案处理状态未知";
        }
        return switch (message.getStatus()) {
            case CallbackMessage.STATUS_SUCCESS -> "档案处理完成";
            case CallbackMessage.STATUS_PARTIAL -> 
                    String.format("档案部分处理完成，成功 %d/%d 个文件", 
                            message.getSuccessCount(), message.getTotalCount());
            case CallbackMessage.STATUS_FAILED -> ARCHIVE_PROCESS_FAILURE_PUBLIC_MESSAGE;
            default -> "档案处理状态: " + message.getStatus();
        };
    }

    private String sanitizeCallbackErrorMessage(String errorMessage) {
        return CALLBACK_FAILURE_PUBLIC_MESSAGE;
    }

    private String sanitizeArchiveProcessErrorMessage(String errorMessage) {
        return ARCHIVE_PROCESS_FAILURE_PUBLIC_MESSAGE;
    }
    
}
