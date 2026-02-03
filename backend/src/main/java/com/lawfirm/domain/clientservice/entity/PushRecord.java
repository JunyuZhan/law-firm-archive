package com.lawfirm.domain.clientservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import com.lawfirm.infrastructure.persistence.typehandler.PostgresJsonTypeHandler;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 数据推送记录实体 记录向客户服务系统推送的项目数据 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "openapi_push_record", autoResultMap = true)
public class PushRecord extends BaseEntity {

  /** 项目ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 推送类型 MANUAL - 手动推送 AUTO - 自动推送 UPDATE - 数据更新推送 */
  private String pushType;

  /** 推送范围（逗号分隔） 如：MATTER_INFO,MATTER_PROGRESS,LAWYER_INFO */
  private String scopes;

  /** 推送的数据快照（JSON格式） 保存脱敏后的数据，用于审计 */
  @TableField(typeHandler = PostgresJsonTypeHandler.class)
  private String dataSnapshot;

  /** 客户服务系统返回的数据ID 用于后续更新或撤销 */
  private String externalId;

  /** 客户服务系统返回的客户访问链接 */
  private String externalUrl;

  /** 状态 PENDING - 待推送 SUCCESS - 成功 FAILED - 失败 */
  private String status;

  /** 错误信息 */
  private String errorMessage;

  /** 重试次数 */
  private Integer retryCount;

  /** 数据在客户服务系统中的有效期 */
  private LocalDateTime expiresAt;

  // ========== 状态常量 ==========
  /** 状态：待推送 */
  public static final String STATUS_PENDING = "PENDING";

  /** 状态：成功 */
  public static final String STATUS_SUCCESS = "SUCCESS";

  /** 状态：失败 */
  public static final String STATUS_FAILED = "FAILED";

  // ========== 推送类型常量 ==========
  /** 类型：手动推送 */
  public static final String TYPE_MANUAL = "MANUAL";

  /** 类型：自动推送 */
  public static final String TYPE_AUTO = "AUTO";

  /** 类型：数据更新推送 */
  public static final String TYPE_UPDATE = "UPDATE";
}
