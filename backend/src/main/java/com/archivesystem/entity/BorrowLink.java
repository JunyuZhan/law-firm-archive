package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电子借阅链接实体类.
 * 对应数据库表: arc_borrow_link
 * 用于生成临时访问链接，支持外部系统申请借阅
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_borrow_link")
public class BorrowLink implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的借阅申请ID（可为空） */
    private Long borrowId;

    /** 档案ID */
    private Long archiveId;

    /** 档案号（冗余） */
    private String archiveNo;

    /** 访问令牌（UUID） */
    private String accessToken;

    // ===== 申请信息 =====

    /** 来源类型：INTERNAL-内部申请, LAW_FIRM-律所系统 */
    private String sourceType;

    /** 来源系统名称 */
    private String sourceSystem;

    /** 来源系统用户ID */
    private String sourceUserId;

    /** 来源系统用户姓名 */
    private String sourceUserName;

    /** 借阅目的 */
    private String borrowPurpose;

    // ===== 链接配置 =====

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 最大访问次数（NULL不限制） */
    private Integer maxAccessCount;

    /** 是否允许下载 */
    @Builder.Default
    private Boolean allowDownload = true;

    // ===== 访问统计 =====

    /** 访问次数 */
    @Builder.Default
    private Integer accessCount = 0;

    /** 下载次数 */
    @Builder.Default
    private Integer downloadCount = 0;

    /** 最后访问时间 */
    private LocalDateTime lastAccessAt;

    /** 最后访问IP */
    private String lastAccessIp;

    // ===== 状态 =====

    /** 状态：ACTIVE-有效, EXPIRED-已过期, REVOKED-已撤销 */
    @Builder.Default
    private String status = STATUS_ACTIVE;

    /** 撤销原因 */
    private String revokeReason;

    /** 撤销时间 */
    private LocalDateTime revokedAt;

    /** 撤销人ID */
    private Long revokedBy;

    // ===== 系统字段 =====

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    // ===== 状态常量 =====

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REVOKED = "REVOKED";

    // ===== 来源类型常量 =====

    public static final String SOURCE_TYPE_INTERNAL = "INTERNAL";
    public static final String SOURCE_TYPE_LAW_FIRM = "LAW_FIRM";

    /**
     * 检查链接是否有效
     */
    public boolean isValid() {
        if (!STATUS_ACTIVE.equals(this.status)) {
            return false;
        }
        if (this.expireAt != null && LocalDateTime.now().isAfter(this.expireAt)) {
            return false;
        }
        if (this.maxAccessCount != null && this.accessCount >= this.maxAccessCount) {
            return false;
        }
        return true;
    }

    /**
     * 增加访问次数
     */
    public void incrementAccessCount(String ip) {
        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
        this.lastAccessAt = LocalDateTime.now();
        this.lastAccessIp = ip;
    }

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }
}
