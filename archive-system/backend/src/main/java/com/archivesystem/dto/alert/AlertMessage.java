package com.archivesystem.dto.alert;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警消息.
 */
@Data
@Builder
public class AlertMessage {

    /** 告警级别 */
    public enum Level {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /** 告警类型 */
    public enum Type {
        ARCHIVE_PROCESS_FAILED,
        ARCHIVE_EXPIRING,
        DEAD_LETTER,
        SYSTEM_ERROR,
        SECURITY_ALERT
    }

    /** 告警级别 */
    private Level level;

    /** 告警类型 */
    private Type type;

    /** 告警标题 */
    private String title;

    /** 告警内容 */
    private String content;

    /** 关联的档案ID */
    private Long archiveId;

    /** 关联的档案号 */
    private String archiveNo;

    /** 额外信息 */
    private Map<String, Object> extra;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 格式化为文本消息.
     */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(getLevelText()).append("】").append(title).append("\n");
        sb.append("时间: ").append(createdAt != null ? createdAt : LocalDateTime.now()).append("\n");
        if (archiveNo != null) {
            sb.append("档案号: ").append(archiveNo).append("\n");
        }
        sb.append("详情: ").append(content);
        return sb.toString();
    }

    private String getLevelText() {
        return switch (level) {
            case INFO -> "通知";
            case WARNING -> "警告";
            case ERROR -> "错误";
            case CRITICAL -> "严重";
        };
    }
}
