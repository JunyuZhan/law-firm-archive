package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 上传新版本命令
 */
@Data
public class UploadNewVersionCommand {

    /**
     * 原文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    /**
     * 新文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 新文件路径
     */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 变更说明
     */
    private String changeNote;
}
