package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议室DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingRoomDTO extends BaseDTO {
    private Long id;
    private String name;
    private String code;
    private String location;
    private Integer capacity;
    private String equipment;
    private String description;
    private String status;
    private String statusName;
    private Boolean enabled;
    private Integer sortOrder;
}
