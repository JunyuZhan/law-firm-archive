package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建培训通知命令
 */
@Data
public class CreateTrainingNoticeCommand {
    
    @NotBlank(message = "通知标题不能为空")
    private String title;
    
    private String content;
    
    private List<AttachmentCommand> attachments;
    
    @Data
    public static class AttachmentCommand {
        private String fileName;
        private String fileUrl;
    }
}

