package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发展规划里程碑实体. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("hr_development_milestone")
public class DevelopmentMilestone implements Serializable {

  /** 主键ID. */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 发展规划ID. */
  private Long planId;

  /** 里程碑名称. */
  private String milestoneName;

  /** 描述. */
  private String description;

  /** 目标日期. */
  private LocalDate targetDate;

  /** 状态：PENDING-待完成, IN_PROGRESS-进行中, COMPLETED-已完成, DELAYED-已延期. */
  private String status;

  /** 实际完成日期. */
  private LocalDate completedDate;

  /** 完成备注. */
  private String completionNote;

  /** 排序. */
  private Integer sortOrder;

  /** 创建时间. */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  /** 更新时间. */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;
}
