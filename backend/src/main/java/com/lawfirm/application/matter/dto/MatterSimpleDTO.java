package com.lawfirm.application.matter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目简要信息 DTO
 * 用于下拉选择等场景，只包含必要字段，避免暴露敏感信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatterSimpleDTO {
    
    /**
     * 项目ID
     */
    private Long id;
    
    /**
     * 项目编号
     */
    private String matterNo;
    
    /**
     * 项目名称
     */
    private String name;
    
    /**
     * 项目类型
     */
    private String matterType;
    
    /**
     * 项目类型名称
     */
    private String matterTypeName;
    
    /**
     * 项目状态
     */
    private String status;
    
    /**
     * 项目状态名称
     */
    private String statusName;
    
    /**
     * 主要客户名称（仅显示名称，不暴露ID）
     */
    private String clientName;
    
    /**
     * 合同编号
     */
    private String contractNo;
    
    /**
     * 承办律师名称
     */
    private String leadLawyerName;
}

