package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 期限提醒查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeadlineQueryDTO extends PageQuery {

    private Long matterId;
    private String deadlineType;
    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadlineDateStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadlineDateEnd;
}

