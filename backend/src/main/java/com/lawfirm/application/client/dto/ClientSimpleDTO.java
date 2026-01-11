package com.lawfirm.application.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户简要信息 DTO
 * 用于下拉选择等场景，只包含必要字段，避免暴露敏感信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSimpleDTO {
    
    /**
     * 客户ID
     */
    private Long id;
    
    /**
     * 客户编号
     */
    private String clientNo;
    
    /**
     * 客户名称
     */
    private String name;
    
    /**
     * 客户类型：INDIVIDUAL-个人, ENTERPRISE-企业
     */
    private String clientType;
    
    /**
     * 客户状态
     */
    private String status;
}

