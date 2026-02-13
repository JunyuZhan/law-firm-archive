package com.archivesystem.service.impl;

import com.archivesystem.mq.CallbackMessage;
import com.archivesystem.service.CallbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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
    
    // 使用共享的RestTemplate（生产环境建议配置连接池）
    private final RestTemplate restTemplate = new RestTemplate();
    
    // 回调超时时间（秒）
    private static final int CALLBACK_TIMEOUT = 30;

    @Override
    public boolean sendCallback(CallbackMessage message) {
        if (message.getCallbackUrl() == null || message.getCallbackUrl().isEmpty()) {
            log.debug("回调URL为空，跳过回调: archiveId={}", message.getArchiveId());
            return true;
        }
        
        try {
            // 构建回调请求体
            Map<String, Object> requestBody = buildCallbackPayload(message);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Callback-Timestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("X-Callback-Source", "archive-system");
            
            // 生成签名（可选，用于安全验证）
            String signature = generateSignature(requestBody);
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
    
    /**
     * 生成签名（简单实现，生产环境应使用更安全的算法）
     */
    private String generateSignature(Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            // 简单的签名：MD5(json + secret)
            // 生产环境应使用 HMAC-SHA256 等更安全的算法
            return cn.hutool.crypto.digest.DigestUtil.md5Hex(json + "archive-callback-secret");
        } catch (Exception e) {
            return "";
        }
    }
}
