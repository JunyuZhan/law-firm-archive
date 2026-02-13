package com.lawfirm.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 实体基类 - 所有领域实体继承此类 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity implements Serializable {

  /** 主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 乐观锁版本号 - 用于并发控制，防止数据覆盖 每次更新时自动加1，如果版本不匹配则更新失败 */
  @Version
  @TableField(fill = FieldFill.INSERT)
  @lombok.Builder.Default
  private Integer version = 1;

  /** 创建时间 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  /** 更新时间 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  /** 创建人ID */
  @TableField(fill = FieldFill.INSERT)
  private Long createdBy;

  /** 更新人ID */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Long updatedBy;

  /** 是否删除 */
  @TableLogic @lombok.Builder.Default private Boolean deleted = false;

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }
}
