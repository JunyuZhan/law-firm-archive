package com.lawfirm.application.matter.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件参与人 DTO
 */
@Data
public class MatterParticipantDTO {

    private Long id;
    private Long matterId;
    private Long userId;
    private String userName;
    private String userPosition;
    private String role;
    private String roleName;
    private BigDecimal commissionRate;
    private Boolean isOriginator;
    private LocalDate joinDate;
    private LocalDate exitDate;
    private String status;
    private String remark;
}

