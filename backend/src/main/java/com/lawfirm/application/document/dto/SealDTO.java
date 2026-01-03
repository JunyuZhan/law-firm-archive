package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 印章DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SealDTO extends BaseDTO {

    private Long id;
    private String sealNo;
    private String name;
    
    private String sealType;
    private String sealTypeName;
    
    private Long keeperId;
    private String keeperName;
    
    private String imageUrl;
    
    private String status;
    private String statusName;
    
    private String description;
    
    /**
     * 使用次数（统计）
     */
    private Integer usageCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
