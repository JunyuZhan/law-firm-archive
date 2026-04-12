package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 档案文件实体类.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("archive_file")
public class ArchiveFile extends BaseEntity {

    /** 档案ID */
    private Long archiveId;

    /** 文件名称 */
    private String fileName;

    /** 原始文件名 */
    private String originalFileName;

    /** 文件类型（MIME类型） */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 存储路径（MinIO对象名） */
    private String storagePath;

    /** 文件分类：COVER-封面, CATALOG-目录, DOCUMENT-文档, ATTACHMENT-附件 */
    private String category;

    /** 排序号 */
    private Integer sortOrder;

    /** 文件描述 */
    private String description;

    /** 来源URL（外部文件的原始URL） */
    private String sourceUrl;

    /** 文件MD5（用于去重） */
    private String fileMd5;

    // ===== 文件分类常量 =====
    
    public static final String CATEGORY_COVER = "COVER";
    public static final String CATEGORY_CATALOG = "CATALOG";
    public static final String CATEGORY_DOCUMENT = "DOCUMENT";
    public static final String CATEGORY_ATTACHMENT = "ATTACHMENT";
}
