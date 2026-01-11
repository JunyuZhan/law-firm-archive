package com.lawfirm.domain.openapi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * OpenAPI 访问日志实体
 * 记录所有客户门户的访问记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("openapi_access_log")
public class OpenApiAccessLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 令牌ID
     */
    private Long tokenId;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求参数（脱敏）
     */
    private String requestParams;

    /**
     * 响应状态码
     */
    private Integer responseCode;

    /**
     * 响应时间(ms)
     */
    private Integer responseTimeMs;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 访问结果：SUCCESS, DENIED, ERROR
     */
    private String accessResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 访问时间
     */
    @lombok.Builder.Default
    private LocalDateTime accessAt = LocalDateTime.now();

    // ========== 结果常量 ==========
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_DENIED = "DENIED";
    public static final String RESULT_ERROR = "ERROR";
}

