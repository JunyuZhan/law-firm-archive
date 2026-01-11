package com.lawfirm.domain.openapi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 客户访问令牌实体
 * 用于管理客户门户的访问授权
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("openapi_client_token")
public class ClientAccessToken extends BaseEntity {

    /**
     * 访问令牌（安全随机生成的128位字符串）
     */
    private String token;

    /**
     * 令牌类型
     */
    @lombok.Builder.Default
    private String tokenType = "BEARER";

    /**
     * 关联的客户ID
     */
    private Long clientId;

    /**
     * 关联的项目ID（可选，限定到具体项目）
     */
    private Long matterId;

    /**
     * 授权范围（逗号分隔）
     * MATTER_INFO - 项目基本信息
     * MATTER_PROGRESS - 项目进度
     * TASK_LIST - 任务列表
     * DEADLINE_INFO - 关键期限
     * DOCUMENT_LIST - 文档列表
     * LAWYER_INFO - 律师信息
     * FEE_INFO - 费用信息
     */
    private String scope;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 最大访问次数（NULL表示不限制）
     */
    private Integer maxAccessCount;

    /**
     * 已访问次数
     */
    @lombok.Builder.Default
    private Integer accessCount = 0;

    /**
     * IP白名单（逗号分隔，NULL表示不限制）
     */
    private String ipWhitelist;

    /**
     * 最后访问IP
     */
    private String lastAccessIp;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessAt;

    /**
     * 状态：ACTIVE-有效, REVOKED-已撤销, EXPIRED-已过期
     */
    @lombok.Builder.Default
    private String status = STATUS_ACTIVE;

    /**
     * 撤销时间
     */
    private LocalDateTime revokedAt;

    /**
     * 撤销人
     */
    private Long revokedBy;

    /**
     * 撤销原因
     */
    private String revokeReason;

    /**
     * 备注说明
     */
    private String remark;

    // ========== 状态常量 ==========
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    // ========== 授权范围常量 ==========
    public static final String SCOPE_MATTER_INFO = "MATTER_INFO";
    public static final String SCOPE_MATTER_PROGRESS = "MATTER_PROGRESS";
    public static final String SCOPE_TASK_LIST = "TASK_LIST";
    public static final String SCOPE_DEADLINE_INFO = "DEADLINE_INFO";
    public static final String SCOPE_DOCUMENT_LIST = "DOCUMENT_LIST";
    public static final String SCOPE_LAWYER_INFO = "LAWYER_INFO";
    public static final String SCOPE_FEE_INFO = "FEE_INFO";

    /**
     * 检查令牌是否有效
     */
    public boolean isValid() {
        if (!STATUS_ACTIVE.equals(this.status)) {
            return false;
        }
        if (this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt)) {
            return false;
        }
        if (this.maxAccessCount != null && this.accessCount >= this.maxAccessCount) {
            return false;
        }
        return true;
    }

    /**
     * 检查是否有指定权限范围
     */
    public boolean hasScope(String requiredScope) {
        if (this.scope == null || this.scope.isEmpty()) {
            return false;
        }
        String[] scopes = this.scope.split(",");
        for (String s : scopes) {
            if (s.trim().equals(requiredScope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查IP是否在白名单中
     */
    public boolean isIpAllowed(String ip) {
        if (this.ipWhitelist == null || this.ipWhitelist.isEmpty()) {
            return true; // 未设置白名单，允许所有IP
        }
        String[] whitelist = this.ipWhitelist.split(",");
        for (String allowedIp : whitelist) {
            if (allowedIp.trim().equals(ip)) {
                return true;
            }
        }
        return false;
    }
}

