package com.lawfirm.domain.clientservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 客户访问日志实体
 * 记录客户服务系统回调的客户访问行为
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("client_access_log")
public class ClientAccessLog extends BaseEntity {

  /** 项目ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 访问时间（客户服务系统的时间） */
  private LocalDateTime accessTime;

  /** IP地址 */
  private String ipAddress;

  /** 用户代理 */
  private String userAgent;

  /** 事件类型（固定值：ACCESS） */
  private String eventType;

  // ========== 事件类型常量 ==========
  /** 事件类型：访问 */
  public static final String EVENT_TYPE_ACCESS = "ACCESS";
}
