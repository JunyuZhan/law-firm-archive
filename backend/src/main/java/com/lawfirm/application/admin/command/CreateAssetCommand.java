package com.lawfirm.application.admin.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建资产命令
 */
@Data
public class CreateAssetCommand {

    @NotBlank(message = "资产名称不能为空")
    private String name;

    @NotBlank(message = "资产分类不能为空")
    private String category;

    private String brand;
    private String model;
    private String specification;
    private String serialNumber;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private String supplier;
    private LocalDate warrantyExpireDate;
    private Integer usefulLife;
    private String location;
    private Long departmentId;
    private String imageUrl;
    private String remarks;
}
