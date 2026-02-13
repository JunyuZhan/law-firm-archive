package com.lawfirm.common.base;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** DTO基类 - 所有DTO继承此类 */
@Data
public abstract class BaseDTO implements Serializable {

  /** 主键ID */
  private Long id;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
