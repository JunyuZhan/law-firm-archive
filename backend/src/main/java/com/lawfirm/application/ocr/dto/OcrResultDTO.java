package com.lawfirm.application.ocr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** OCR识别结果DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResultDTO {

  /** 是否成功 */
  private boolean success;

  /** 错误信息 */
  private String errorMessage;

  /** 识别类型 */
  private String type;

  /** 类型名称 */
  private String typeName;

  /** 原始文本 */
  private String rawText;

  /** 数据 */
  private Map<String, Object> data;

  /** 置信度 */
  private Double confidence;

  // 银行回单

  /** 银行名称 */
  private String bankName;

  /** 金额 */
  private BigDecimal amount;

  /** 交易日期 */
  private LocalDate transactionDate;

  /** 付款人名称 */
  private String payerName;

  /** 付款人账户 */
  private String payerAccount;

  /** 收款人名称 */
  private String payeeName;

  /** 收款人账户 */
  private String payeeAccount;

  /** 交易号 */
  private String transactionNo;

  /** 备注 */
  private String remark;

  // 身份证

  /** 姓名 */
  private String name;

  /** 身份证号 */
  private String idNumber;

  /** 性别 */
  private String gender;

  /** 民族 */
  private String ethnicity;

  /** 出生日期 */
  private LocalDate birthDate;

  /** 地址 */
  private String address;

  /** 签发机关 */
  private String issuingAuthority;

  /** 有效期起始 */
  private LocalDate validFrom;

  /** 有效期结束 */
  private LocalDate validTo;

  // 营业执照

  /** 公司名称 */
  private String companyName;

  /** 统一社会信用代码 */
  private String creditCode;

  /** 公司类型 */
  private String companyType;

  /** 法定代表人 */
  private String legalRepresentative;

  /** 注册资本 */
  private String registeredCapital;

  /** 成立日期 */
  private LocalDate establishDate;

  /** 营业期限 */
  private String businessTerm;

  /** 经营范围 */
  private String businessScope;

  /** 注册地址 */
  private String registeredAddress;

  // 名片

  /** 公司名称 */
  private String cardCompany;

  /** 职位 */
  private String title;

  /** 手机号 */
  private String mobile;

  /** 电话 */
  private String phone;

  /** 邮箱 */
  private String email;

  /** 网站 */
  private String website;

  // 发票

  /** 发票类型 */
  private String invoiceType;

  /** 发票代码 */
  private String invoiceCode;

  /** 发票号码 */
  private String invoiceNo;

  /** 开票日期 */
  private LocalDate invoiceDate;

  /** 销售方名称 */
  private String sellerName;

  /** 销售方税号 */
  private String sellerTaxNo;

  /** 购买方名称 */
  private String buyerName;

  /** 购买方税号 */
  private String buyerTaxNo;

  /** 发票金额 */
  private BigDecimal invoiceAmount;

  /** 税额 */
  private BigDecimal taxAmount;

  /** 价税合计 */
  private BigDecimal totalAmount;
}
