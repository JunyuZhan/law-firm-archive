package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 案件参与人（团队成员）实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("matter_participant")
public class MatterParticipant extends BaseEntity {

  /** 案件ID */
  private Long matterId;

  /** 用户ID */
  private Long userId;

  /** 项目角色：LEAD-主办律师, CO_COUNSEL-协办律师, PARALEGAL-律师助理, TRAINEE-实习律师 */
  private String role;

  /** 提成比例（%） */
  private BigDecimal commissionRate;

  /** 是否案源人 */
  private Boolean isOriginator;

  /** 加入日期 */
  private LocalDate joinDate;

  /** 退出日期 */
  private LocalDate exitDate;

  /** 状态：ACTIVE-参与中, EXITED-已退出 */
  private String status;

  /** 备注 */
  private String remark;
}
