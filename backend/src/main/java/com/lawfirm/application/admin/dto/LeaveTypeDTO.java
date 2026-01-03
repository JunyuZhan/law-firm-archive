package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 请假类型DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveTypeDTO extends BaseDTO {
    private Long id;
    private String name;
    private String code;
    private Boolean paid;
    private BigDecimal annualLimit;
    private Boolean needApproval;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
}
