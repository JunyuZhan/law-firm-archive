package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统公告DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnouncementDTO extends BaseDTO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private String typeName;
    private Integer priority;
    private String status;
    private String statusName;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Boolean isTop;
    private LocalDateTime createdAt;
}
