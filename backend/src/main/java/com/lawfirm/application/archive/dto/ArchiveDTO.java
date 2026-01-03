package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveDTO extends BaseDTO {

    private String archiveNo;
    private Long matterId;
    private String matterNo;
    private String matterName;
    private String archiveName;
    private String archiveType;
    private String archiveTypeName;
    private String clientName;
    private String mainLawyerName;
    private LocalDate caseCloseDate;
    private Integer volumeCount;
    private Integer pageCount;
    private String catalog;
    private Long locationId;
    private String locationName;
    private String boxNo;
    private String retentionPeriod;
    private String retentionPeriodName;
    private LocalDate retentionExpireDate;
    private Boolean hasElectronic;
    private String electronicUrl;
    private String status;
    private String statusName;
    private Long storedBy;
    private String storedByName;
    private LocalDateTime storedAt;
    private LocalDate destroyDate;
    private String destroyReason;
    private Long destroyApproverId;
    private String remarks;
    
    /**
     * 借阅记录
     */
    private List<ArchiveBorrowDTO> borrows;
}

