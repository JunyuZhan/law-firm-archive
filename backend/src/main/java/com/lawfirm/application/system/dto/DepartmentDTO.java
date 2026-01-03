package com.lawfirm.application.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {

    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 父部门名称
     */
    private String parentName;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 部门负责人ID
     */
    private Long leaderId;

    /**
     * 部门负责人名称
     */
    private String leaderName;

    /**
     * 状态
     */
    private String status;

    /**
     * 子部门列表（树形结构用）
     */
    private List<DepartmentDTO> children;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
