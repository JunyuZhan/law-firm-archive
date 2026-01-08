package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部系统集成查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExternalIntegrationQueryDTO extends PageQuery {
    
    /** 关键字（搜索名称、编码） */
    private String keyword;
    
    /** 集成类型 */
    private String integrationType;
    
    /** 是否启用 */
    private Boolean enabled;
}

