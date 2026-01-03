package com.lawfirm.application.evidence.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 证据清单DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvidenceListDTO extends BaseDTO {
    private Long id;
    private String listNo;
    private Long matterId;
    private String name;
    private String listType;
    private String listTypeName;
    private String evidenceIds;
    private List<Long> evidenceIdList;
    private String fileUrl;
    private String fileName;
    private String status;
    private String statusName;
    
    /** 包含的证据列表 */
    private List<EvidenceDTO> evidences;
}
