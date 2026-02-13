package com.lawfirm.domain.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 档案操作日志实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("archive_operation_log")
public class ArchiveOperationLog extends BaseEntity {

  /** 档案ID */
  private Long archiveId;

  /** 操作类型：STORE(入库), BORROW(借出), RETURN(归还), TRANSFER(转移), DESTROY(销毁) */
  private String operationType;

  /** 操作描述 */
  private String operationDescription;

  /** 操作人ID */
  private Long operatorId;

  /** 操作时间 */
  private LocalDateTime operatedAt;
}
