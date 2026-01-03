package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 档案查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveQueryDTO extends PageQuery {

    private String archiveNo;
    private String archiveName;
    private String matterNo;
    private String matterName;
    private String clientName;
    private String archiveType;
    private String status;
    private Long locationId;
    private LocalDate caseCloseDateFrom;
    private LocalDate caseCloseDateTo;
}

