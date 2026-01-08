package com.lawfirm.application.system.command;

import lombok.Data;

import java.util.List;

/**
 * 创建数据交接命令
 */
@Data
public class CreateHandoverCommand {

    /**
     * 移交人ID
     */
    private Long fromUserId;

    /**
     * 接收人ID
     */
    private Long toUserId;

    /**
     * 交接类型：RESIGNATION-离职交接, PROJECT-项目移交, CLIENT-客户移交, LEAD-案源移交
     */
    private String handoverType;

    /**
     * 交接原因
     */
    private String reason;

    /**
     * 指定移交的项目ID列表（项目移交时使用）
     */
    private List<Long> matterIds;

    /**
     * 指定移交的客户ID列表（客户移交时使用）
     */
    private List<Long> clientIds;

    /**
     * 指定移交的案源ID列表（案源移交时使用）
     */
    private List<Long> leadIds;

    /**
     * 指定移交的任务ID列表（任务移交时使用）
     */
    private List<Long> taskIds;

    /**
     * 是否包含案源人身份（离职交接时）
     * true: 案源人身份也一并移交
     * false: 保留原案源人（涉及提成分配）
     */
    private Boolean includeOriginator = false;

    /**
     * 备注
     */
    private String remark;
}

