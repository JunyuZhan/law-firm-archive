package com.lawfirm.application.evidence.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 证据查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvidenceQueryDTO extends PageQuery {

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 证据名称（模糊搜索）
     */
    private String name;

    /**
     * 证据类型
     */
    private String evidenceType;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 质证状态
     */
    private String crossExamStatus;
}
