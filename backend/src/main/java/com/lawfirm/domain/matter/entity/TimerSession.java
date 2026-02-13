package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 计时器会话实体（M3-044） */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("timer_session")
public class TimerSession extends BaseEntity {

  /** 用户ID */
  private Long userId;

  /** 案件ID */
  private Long matterId;

  /** 工作类型 */
  private String workType;

  /** 工作内容 */
  private String workContent;

  /** 是否计费 */
  @lombok.Builder.Default private Boolean billable = true;

  /** 开始时间 */
  private LocalDateTime startTime;

  /** 暂停时间 */
  private LocalDateTime pauseTime;

  /** 恢复时间 */
  private LocalDateTime resumeTime;

  /** 已累计的秒数 */
  @lombok.Builder.Default private Long elapsedSeconds = 0L;

  /** 状态：RUNNING-运行中, PAUSED-已暂停, STOPPED-已停止 */
  @lombok.Builder.Default private String status = "RUNNING";
}
