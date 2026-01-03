package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户会话查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSessionQueryDTO extends PageQuery {

    private Long userId;
    private String username;
    private String status;
    private String ipAddress;
}

