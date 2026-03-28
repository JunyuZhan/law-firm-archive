package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电子文件实体类.
 * 对应数据库表: arc_digital_file
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_digital_file")
public class DigitalFile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 档案ID */
    private Long archiveId;

    // ===== 文件标识 =====
    
    /** 文件序号 */
    private String fileNo;

    // ===== 文件信息 =====
    
    /** 文件名 */
    private String fileName;
    
    /** 原始文件名 */
    private String originalName;
    
    /** 扩展名 */
    private String fileExtension;
    
    /** MIME类型 */
    private String mimeType;
    
    /** 文件大小（字节） */
    private Long fileSize;

    // ===== 存储信息 =====
    
    /** 存储路径（MinIO对象路径） */
    private String storagePath;
    
    /** 存储桶 */
    private String storageBucket;

    // ===== 格式信息 =====
    
    /** 格式名称 */
    private String formatName;
    
    /** 格式版本 */
    private String formatVersion;
    
    /** 是否长期保存格式 */
    @Builder.Default
    private Boolean isLongTermFormat = false;
    
    /** 转换后的长期保存格式路径 */
    private String convertedPath;

    // ===== 完整性校验 =====
    
    /** 哈希算法 */
    @Builder.Default
    private String hashAlgorithm = "SHA256";
    
    /** 哈希值 */
    private String hashValue;

    // ===== 内容信息 =====
    
    /** OCR状态 */
    private String ocrStatus;
    
    /** OCR识别内容 */
    private String ocrContent;

    // ===== 预览信息 =====
    
    /** 是否有预览 */
    @Builder.Default
    private Boolean hasPreview = false;
    
    /** 预览文件路径 */
    private String previewPath;
    
    /** 缩略图路径 */
    private String thumbnailPath;

    // ===== 分类信息 =====
    
    /** 文件分类 */
    @Builder.Default
    private String fileCategory = CATEGORY_MAIN;
    
    /** 排序 */
    @Builder.Default
    private Integer sortOrder = 0;
    
    /** 描述 */
    private String description;

    // ===== 来源信息 =====
    
    /** 来源URL */
    private String sourceUrl;

    // ===== 系统字段 =====
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadAt;
    
    private Long uploadBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    @Builder.Default
    private Boolean deleted = false;

    // ===== 常量 =====
    
    public static final String CATEGORY_MAIN = "MAIN";
    public static final String CATEGORY_ATTACHMENT = "ATTACHMENT";
    public static final String CATEGORY_COVER = "COVER";
    public static final String CATEGORY_CATALOG = "CATALOG";

    public static final String OCR_NONE = "NONE";
    public static final String OCR_PENDING = "PENDING";
    public static final String OCR_COMPLETED = "COMPLETED";
    public static final String OCR_FAILED = "FAILED";
}
