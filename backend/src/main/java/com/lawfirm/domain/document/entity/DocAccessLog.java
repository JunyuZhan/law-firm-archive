package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 文档访问日志实体. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("doc_access_log")
public class DocAccessLog {

  /** 主键ID. */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 文档ID. */
  private Long documentId;

  /** 用户ID. */
  private Long userId;

  /** 操作类型. */
  private String actionType;

  /** IP地址. */
  private String ipAddress;

  /** 用户代理. */
  private String userAgent;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 操作类型：查看. */
  public static final String ACTION_VIEW = "VIEW";

  /** 操作类型：下载. */
  public static final String ACTION_DOWNLOAD = "DOWNLOAD";

  /** 操作类型：打印. */
  public static final String ACTION_PRINT = "PRINT";

  /** 操作类型：编辑. */
  public static final String ACTION_EDIT = "EDIT";
}
