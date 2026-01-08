package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

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
    
    /**
     * 用户选择要包含的数据源ID列表
     * 如果为空，则包含所有启用的数据源
     */
    private List<Long> selectedDataSourceIds;
    
    /**
     * 归档数据快照（JSON格式）
     * 由系统自动生成，包含项目所有相关数据
     */
    private String archiveSnapshot;
}

