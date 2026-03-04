package com.archivesystem.dto.archive;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 档案创建请求DTO（手动创建）.
 */
@Data
public class ArchiveCreateRequest {

    /** 全宗ID */
    private Long fondsId;

    /** 分类ID */
    private Long categoryId;

    /** 档案形式（ELECTRONIC-电子档案, PHYSICAL-纸质档案, HYBRID-混合档案） */
    private String archiveForm = "ELECTRONIC";

    /** 档案门类 */
    @NotBlank(message = "档案门类不能为空")
    private String archiveType;

    /** 存放位置ID（纸质/混合档案必填） */
    private Long locationId;

    /** 盒号 */
    private String boxNo;

    /** 档案题名 */
    @NotBlank(message = "档案题名不能为空")
    @Size(max = 500, message = "档案题名不能超过500个字符")
    private String title;

    /** 文件编号 */
    private String fileNo;

    /** 责任者 */
    private String responsibility;

    /** 归档日期 */
    private LocalDate archiveDate;

    /** 文件日期 */
    private LocalDate documentDate;

    /** 页数 */
    private Integer pageCount;

    /** 件数 */
    private Integer piecesCount;

    /** 保管期限代码 */
    @NotBlank(message = "保管期限不能为空")
    private String retentionPeriod;

    /** 密级 */
    private String securityLevel;

    // 业务关联
    private String caseNo;
    private String caseName;
    private String clientName;
    private String lawyerName;
    private LocalDate caseCloseDate;

    /** 关键词 */
    private String keywords;

    /** 摘要 */
    private String archiveAbstract;

    /** 备注 */
    private String remarks;

    /** 扩展元数据 */
    private Map<String, Object> extraData;

    /** 已上传的文件ID列表 */
    private List<Long> fileIds;
}
