package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 外出登记DTO（M8-005）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GoOutRecordDTO extends BaseDTO {
    private Long id;
    private String recordNo;
    private Long userId;
    private String userName;
    private LocalDateTime outTime;
    private LocalDateTime expectedReturnTime;
    private LocalDateTime actualReturnTime;
    private String location;
    private String reason;
    private String companions;
    private String status;
    private String statusName;
}

