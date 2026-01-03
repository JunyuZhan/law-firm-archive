package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案件查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MatterQueryDTO extends PageQuery {

    /**
     * 案件名称（模糊）
     */
    private String name;

    /**
     * 案件编号（模糊）
     */
    private String matterNo;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 主办律师ID
     */
    private Long leadLawyerId;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 案件类型
     */
    private String matterType;

    /**
     * 状态
     */
    private String status;

    /**
     * 我参与的案件（当前用户）
     */
    private Boolean myMatters;
}

