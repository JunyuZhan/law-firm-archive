package com.lawfirm.application.evidence.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 证据DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvidenceDTO extends BaseDTO {

    private Long id;
    private String evidenceNo;
    private Long matterId;
    private String matterName;
    
    private String name;
    private String evidenceType;
    private String evidenceTypeName;
    private String source;
    
    private String groupName;
    private Integer sortOrder;
    
    private String provePurpose;
    private String description;
    
    private Boolean isOriginal;
    private Integer originalCount;
    private Integer copyCount;
    private Integer pageStart;
    private Integer pageEnd;
    private String pageRange;
    
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileSizeDisplay;
    
    private String crossExamStatus;
    private String crossExamStatusName;
    
    private String status;
    
    /**
     * 质证记录
     */
    private List<EvidenceCrossExamDTO> crossExams;
    
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
