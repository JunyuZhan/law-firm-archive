package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建档案命令
 */
@Data
public class CreateArchiveCommand {

    @NotNull(message = "案件ID不能为空")
    private Long matterId;

    private String archiveName;
    private String archiveType;
    private Integer volumeCount;
    private Integer pageCount;
    private String catalog;
    private String retentionPeriod;
    private Boolean hasElectronic;
    private String electronicUrl;
    private String remarks;
}

