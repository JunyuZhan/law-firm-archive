package com.lawfirm.application.client.command;

import lombok.Data;

import java.util.List;

/**
 * 为客户分配标签命令
 */
@Data
public class AssignClientTagsCommand {

    private Long clientId;
    private List<Long> tagIds;
}

