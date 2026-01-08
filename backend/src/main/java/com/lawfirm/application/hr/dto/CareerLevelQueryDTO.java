package com.lawfirm.application.hr.dto;

import lombok.Data;

/**
 * 职级查询 DTO
 */
@Data
public class CareerLevelQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String category;
    private String status;
}

