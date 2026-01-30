package com.lawfirm.application.system.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 角色DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

  /** 角色ID */
  private Long id;

  /** 角色编码. */
  private String roleCode;

  /** 角色名称. */
  private String roleName;

  /** 描述. */
  private String description;

  /** 数据范围. */
  private String dataScope;

  /** 状态. */
  private String status;

  /** 排序. */
  private Integer sortOrder;

  /** 菜单ID列表. */
  private List<Long> menuIds;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
