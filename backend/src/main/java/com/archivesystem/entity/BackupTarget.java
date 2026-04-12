package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备份目标配置.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_backup_target")
public class BackupTarget implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String targetType;
    private Boolean enabled;
    private String localPath;
    private String smbHost;
    private Integer smbPort;
    private String smbShare;
    private String smbUsername;
    private String smbPasswordEncrypted;
    private String smbSubPath;
    private String remarks;
    private String verifyStatus;
    private String verifyMessage;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    public static final String TYPE_LOCAL = "LOCAL";
    public static final String TYPE_SMB = "SMB";

    public static final String VERIFY_PENDING = "PENDING";
    public static final String VERIFY_SUCCESS = "SUCCESS";
    public static final String VERIFY_FAILED = "FAILED";
}
