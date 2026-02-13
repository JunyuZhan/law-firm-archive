package com.lawfirm.domain.clientservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 客户下载日志实体 存储客户服务系统回调的客户下载行为 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("client_download_log")
public class ClientDownloadLog extends BaseEntity {

  /** 项目ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 客户服务系统的文件ID */
  private String fileId;

  /** 文件名 */
  private String fileName;

  /** 下载时间（客户服务系统的时间） */
  private LocalDateTime downloadTime;

  /** IP地址 */
  private String ipAddress;

  /** 用户代理 */
  private String userAgent;

  /** 事件类型（固定值：DOWNLOAD） */
  private String eventType;

  // ========== 事件类型常量 ==========
  /** 事件类型：下载 */
  public static final String EVENT_TYPE_DOWNLOAD = "DOWNLOAD";
}
