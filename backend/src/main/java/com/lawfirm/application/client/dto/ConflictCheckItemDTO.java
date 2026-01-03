package com.lawfirm.application.client.dto;

import lombok.Data;

/**
 * 利冲检查项 DTO
 */
@Data
public class ConflictCheckItemDTO {

    private Long id;
    private Long checkId;
    private String partyName;
    private String partyType;
    private String partyTypeName;
    private String idNumber;
    private Boolean hasConflict;
    private String conflictDetail;
    private Long relatedMatterId;
    private String relatedMatterName;
    private Long relatedClientId;
    private String relatedClientName;
}

