package com.archivesystem.dto.archive;

import lombok.Data;

import java.util.List;

/**
 * 档案补充上传请求DTO.
 * @author junyuzhan
 */
@Data
public class ArchiveSupplementRequest {

    /** 档案形式（ELECTRONIC-电子档案, PHYSICAL-纸质档案, HYBRID-混合档案） */
    private String archiveForm;

    /** 存放位置ID */
    private Long locationId;

    /** 盒号 */
    private String boxNo;

    /** 补充的文件ID列表 */
    private List<Long> fileIds;
}
