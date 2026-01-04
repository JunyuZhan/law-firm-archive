package com.lawfirm.application.document.command;

import lombok.Data;

/**
 * 更新印章命令
 */
@Data
public class UpdateSealCommand {

    /**
     * 印章名称
     */
    private String name;

    /**
     * 保管人ID
     */
    private Long keeperId;

    /**
     * 保管人姓名
     */
    private String keeperName;

    /**
     * 印章图片URL
     */
    private String imageUrl;

    /**
     * 描述
     */
    private String description;
}

