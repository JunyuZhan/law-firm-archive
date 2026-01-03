package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建股东信息命令
 */
@Data
public class CreateShareholderCommand {

    @NotNull(message = "客户ID不能为空")
    private Long clientId;

    @NotBlank(message = "股东名称不能为空")
    private String shareholderName;

    @NotBlank(message = "股东类型不能为空")
    private String shareholderType; // INDIVIDUAL-个人, ENTERPRISE-企业

    private String idCard; // 个人股东身份证号
    private String creditCode; // 企业股东统一社会信用代码
    private BigDecimal shareholdingRatio; // 持股比例
    private BigDecimal investmentAmount; // 投资金额
    private LocalDate investmentDate; // 投资日期
    private String position; // 职务
    private String remark; // 备注
}

