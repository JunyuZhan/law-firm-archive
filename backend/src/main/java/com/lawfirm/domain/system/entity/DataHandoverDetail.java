package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 数据交接明细实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_handover_detail")
public class DataHandoverDetail extends BaseEntity {

  /** 交接单ID */
  private Long handoverId;

  /** 数据类型：MATTER, CLIENT, LEAD, TASK, MATTER_PARTICIPANT, CONTRACT_PARTICIPANT */
  private String dataType;

  /** 数据ID */
  private Long dataId;

  /** 数据编号 */
  private String dataNo;

  /** 数据名称 */
  private String dataName;

  /** 变更字段名：lead_lawyer_id, responsible_lawyer_id 等 */
  private String fieldName;

  /** 原值（用户ID） */
  private String oldValue;

  /** 新值（用户ID） */
  private String newValue;

  /** 状态：PENDING-待处理, DONE-已完成, FAILED-失败 */
  @lombok.Builder.Default private String status = "PENDING";

  /** 错误信息 */
  private String errorMessage;

  /** 执行时间 */
  private LocalDateTime executedAt;
}
