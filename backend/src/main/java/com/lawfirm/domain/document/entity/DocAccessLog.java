package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 文档访问日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("doc_access_log")
public class DocAccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档ID */
    private Long documentId;

    /** 用户ID */
    private Long userId;

    /** 操作类型 */
    private String actionType;

    /** IP地址 */
    private String ipAddress;

    /** 用户代理 */
    private String userAgent;

    /** 创建时间 */
    private LocalDateTime createdAt;

    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_DOWNLOAD = "DOWNLOAD";
    public static final String ACTION_PRINT = "PRINT";
    public static final String ACTION_EDIT = "EDIT";
}
