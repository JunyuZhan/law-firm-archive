package com.archivesystem.mq;

import com.archivesystem.dto.archive.ArchiveReceiveRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案接收消息 DTO
 * 用于异步处理档案文件下载和存储
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveReceiveMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID（用于幂等）
     */
    private String messageId;
    
    /**
     * 档案ID
     */
    private Long archiveId;
    
    /**
     * 档案号
     */
    private String archiveNo;
    
    /**
     * 来源类型
     */
    private String sourceType;
    
    /**
     * 来源系统ID
     */
    private String sourceId;
    
    /**
     * 回调URL（处理完成后通知）
     */
    private String callbackUrl;
    
    /**
     * 待处理的文件列表
     */
    private List<FileInfo> files;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 最大重试次数
     */
    private int maxRetries;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 文件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String fileName;
        private String fileType;
        private String downloadUrl;
        private String fileCategory;
        private Integer sortOrder;
    }
    
    /**
     * 从接收请求构建消息
     */
    public static ArchiveReceiveMessage fromRequest(Long archiveId, String archiveNo, 
            ArchiveReceiveRequest request, String callbackUrl) {
        ArchiveReceiveMessageBuilder builder = ArchiveReceiveMessage.builder()
                .messageId(java.util.UUID.randomUUID().toString())
                .archiveId(archiveId)
                .archiveNo(archiveNo)
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .callbackUrl(callbackUrl)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now());
        
        if (request.getFiles() != null) {
            List<FileInfo> fileInfos = new java.util.ArrayList<>();
            for (int i = 0; i < request.getFiles().size(); i++) {
                ArchiveReceiveRequest.FileInfo reqFile = request.getFiles().get(i);
                fileInfos.add(FileInfo.builder()
                        .fileName(reqFile.getFileName())
                        .fileType(reqFile.getFileType())
                        .downloadUrl(reqFile.getDownloadUrl())
                        .fileCategory(reqFile.getFileCategory())
                        .sortOrder(i + 1)
                        .build());
            }
            builder.files(fileInfos);
        }
        
        return builder.build();
    }
}
