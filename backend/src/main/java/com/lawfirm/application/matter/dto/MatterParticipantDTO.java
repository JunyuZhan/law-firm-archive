package com.lawfirm.application.matter.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 案件参与人 DTO. */
@Data
public class MatterParticipantDTO {

  /** 主键ID. */
  private Long id;

  /** 案件ID. */
  private Long matterId;

  /** 用户ID. */
  private Long userId;

  /** 用户姓名. */
  private String userName;

  /** 用户职位. */
  private String userPosition;

  /** 角色. */
  private String role;

  /** 角色名称. */
  private String roleName;

  /** 提成比例. */
  private BigDecimal commissionRate;

  /** 是否为案源人. */
  private Boolean isOriginator;

  /** 加入日期. */
  private LocalDate joinDate;

  /** 退出日期. */
  private LocalDate exitDate;

  /** 状态. */
  private String status;

  /** 备注. */
  private String remark;
}
