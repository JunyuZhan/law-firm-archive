package com.lawfirm.application.admin.command;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建资产命令 */
@Data
public class CreateAssetCommand {

  /** 资产名称 */
  @NotBlank(message = "资产名称不能为空")
  private String name;

  /** 资产分类 */
  @NotBlank(message = "资产分类不能为空")
  private String category;

  /** 品牌 */
  private String brand;

  /** 型号 */
  private String model;

  /** 规格 */
  private String specification;

  /** 序列号 */
  private String serialNumber;

  /** 采购日期 */
  private LocalDate purchaseDate;

  /** 采购价格 */
  private BigDecimal purchasePrice;

  /** 供应商 */
  private String supplier;

  /** 保修到期日期 */
  private LocalDate warrantyExpireDate;

  /** 使用年限（年） */
  private Integer usefulLife;

  /** 位置 */
  private String location;

  /** 部门ID */
  private Long departmentId;

  /** 图片URL */
  private String imageUrl;

  /** 备注 */
  private String remarks;
}
