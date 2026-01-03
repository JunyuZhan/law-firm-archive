package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新股东信息命令
 */
@Data
public class UpdateShareholderCommand {

    @NotNull(message = "股东ID不能为空")
    private Long id;

    private String shareholderName;
    private String shareholderType;
    private String idCard;
    private String creditCode;
    private BigDecimal shareholdingRatio;
    private BigDecimal investmentAmount;
    private LocalDate investmentDate;
    private String position;
    private String remark;
}

