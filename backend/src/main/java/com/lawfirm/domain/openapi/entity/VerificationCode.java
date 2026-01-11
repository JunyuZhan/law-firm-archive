package com.lawfirm.domain.openapi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 公开验证码实体
 * 用于函件、合同等的二维码验证
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("openapi_verification_code")
public class VerificationCode extends BaseEntity {

    /**
     * 验证码
     */
    private String verificationCode;

    /**
     * 验证类型：LETTER-函件, CONTRACT-合同, CERTIFICATE-证书
     */
    private String verificationType;

    /**
     * 业务对象ID
     */
    private Long businessId;

    /**
     * 业务编号
     */
    private String businessNo;

    /**
     * 过期时间（NULL表示永久有效）
     */
    private LocalDateTime expiresAt;

    /**
     * 最大验证次数
     */
    private Integer maxVerifyCount;

    /**
     * 已验证次数
     */
    @lombok.Builder.Default
    private Integer verifyCount = 0;

    /**
     * 状态：ACTIVE, REVOKED, EXPIRED
     */
    @lombok.Builder.Default
    private String status = STATUS_ACTIVE;

    // ========== 类型常量 ==========
    public static final String TYPE_LETTER = "LETTER";
    public static final String TYPE_CONTRACT = "CONTRACT";
    public static final String TYPE_CERTIFICATE = "CERTIFICATE";

    // ========== 状态常量 ==========
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    /**
     * 检查验证码是否有效
     */
    public boolean isValid() {
        if (!STATUS_ACTIVE.equals(this.status)) {
            return false;
        }
        if (this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt)) {
            return false;
        }
        if (this.maxVerifyCount != null && this.verifyCount >= this.maxVerifyCount) {
            return false;
        }
        return true;
    }
}

