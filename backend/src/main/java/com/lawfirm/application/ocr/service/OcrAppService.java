package com.lawfirm.application.ocr.service;

import com.lawfirm.application.ocr.dto.OcrResultDTO;
import com.lawfirm.common.util.UrlSecurityValidator;
import com.lawfirm.infrastructure.ocr.OcrResult;
import com.lawfirm.infrastructure.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR识别应用服务
 *
 * <p>安全说明：所有URL接口都经过SSRF防护验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrAppService {

  /** OCR服务 */
  private final OcrService ocrService;

  /** URL安全验证器 */
  private final UrlSecurityValidator urlSecurityValidator;

  /**
   * 通用文字识别.
   *
   * @param file 文件
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeText(final MultipartFile file) {
    log.info("通用文字识别: {}", file.getOriginalFilename());
    OcrResult result = ocrService.recognizeText(file);
    return toDTO(result);
  }

  /**
   * 通用文字识别（URL）. 安全：已验证URL防止SSRF攻击
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeTextByUrl(final String imageUrl) {
    urlSecurityValidator.validateImageUrl(imageUrl); // SSRF防护
    log.info("通用文字识别(URL): {}", imageUrl);
    OcrResult result = ocrService.recognizeText(imageUrl);
    return toDTO(result);
  }

  /**
   * 银行回单识别.
   *
   * @param file 文件
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeBankReceipt(final MultipartFile file) {
    log.info("银行回单识别: {}", file.getOriginalFilename());
    OcrResult result = ocrService.recognizeBankReceipt(file);
    return toDTO(result);
  }

  /**
   * 银行回单识别（URL）. 安全：已验证URL防止SSRF攻击
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeBankReceiptByUrl(final String imageUrl) {
    urlSecurityValidator.validateImageUrl(imageUrl); // SSRF防护
    log.info("银行回单识别(URL): {}", imageUrl);
    OcrResult result = ocrService.recognizeBankReceipt(imageUrl);
    return toDTO(result);
  }

  /**
   * 身份证识别.
   *
   * @param file 文件
   * @param isFront 是否正面
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeIdCard(final MultipartFile file, final boolean isFront) {
    log.info("身份证识别({}): {}", isFront ? "正面" : "背面", file.getOriginalFilename());
    OcrResult result = ocrService.recognizeIdCard(file, isFront);
    return toDTO(result);
  }

  /**
   * 身份证识别（URL）. 安全：已验证URL防止SSRF攻击
   *
   * @param imageUrl 图片URL
   * @param isFront 是否正面
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeIdCardByUrl(final String imageUrl, final boolean isFront) {
    urlSecurityValidator.validateImageUrl(imageUrl); // SSRF防护
    log.info("身份证识别(URL, {}): {}", isFront ? "正面" : "背面", imageUrl);
    OcrResult result = ocrService.recognizeIdCard(imageUrl, isFront);
    return toDTO(result);
  }

  /**
   * 营业执照识别.
   *
   * @param file 文件
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeBusinessLicense(final MultipartFile file) {
    log.info("营业执照识别: {}", file.getOriginalFilename());
    OcrResult result = ocrService.recognizeBusinessLicense(file);
    return toDTO(result);
  }

  /**
   * 营业执照识别（URL）. 安全：已验证URL防止SSRF攻击
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeBusinessLicenseByUrl(final String imageUrl) {
    urlSecurityValidator.validateImageUrl(imageUrl); // SSRF防护
    log.info("营业执照识别(URL): {}", imageUrl);
    OcrResult result = ocrService.recognizeBusinessLicense(imageUrl);
    return toDTO(result);
  }

  /**
   * 名片识别.
   *
   * @param file 文件
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeBusinessCard(final MultipartFile file) {
    log.info("名片识别: {}", file.getOriginalFilename());
    OcrResult result = ocrService.recognizeBusinessCard(file);
    return toDTO(result);
  }

  /**
   * 名片识别（URL）. 安全：已验证URL防止SSRF攻击
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeBusinessCardByUrl(final String imageUrl) {
    urlSecurityValidator.validateImageUrl(imageUrl); // SSRF防护
    log.info("名片识别(URL): {}", imageUrl);
    OcrResult result = ocrService.recognizeBusinessCard(imageUrl);
    return toDTO(result);
  }

  /**
   * 发票识别.
   *
   * @param file 文件
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeInvoice(final MultipartFile file) {
    log.info("发票识别: {}", file.getOriginalFilename());
    OcrResult result = ocrService.recognizeInvoice(file);
    return toDTO(result);
  }

  /**
   * 发票识别（URL）. 安全：已验证URL防止SSRF攻击
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  public OcrResultDTO recognizeInvoiceByUrl(final String imageUrl) {
    urlSecurityValidator.validateImageUrl(imageUrl); // SSRF防护
    log.info("发票识别(URL): {}", imageUrl);
    OcrResult result = ocrService.recognizeInvoice(imageUrl);
    return toDTO(result);
  }

  /**
   * 转换为DTO.
   *
   * @param result OCR结果
   * @return DTO对象
   */
  private OcrResultDTO toDTO(final OcrResult result) {
    return OcrResultDTO.builder()
        .success(result.isSuccess())
        .errorMessage(result.getErrorMessage())
        .type(result.getType())
        .typeName(getTypeName(result.getType()))
        .rawText(result.getRawText())
        .data(result.getData())
        .confidence(result.getConfidence())
        // 银行回单
        .bankName(result.getBankName())
        .amount(result.getAmount())
        .transactionDate(result.getTransactionDate())
        .payerName(result.getPayerName())
        .payerAccount(result.getPayerAccount())
        .payeeName(result.getPayeeName())
        .payeeAccount(result.getPayeeAccount())
        .transactionNo(result.getTransactionNo())
        .remark(result.getRemark())
        // 身份证
        .name(result.getName())
        .idNumber(result.getIdNumber())
        .gender(result.getGender())
        .ethnicity(result.getEthnicity())
        .birthDate(result.getBirthDate())
        .address(result.getAddress())
        .issuingAuthority(result.getIssuingAuthority())
        .validFrom(result.getValidFrom())
        .validTo(result.getValidTo())
        // 营业执照
        .companyName(result.getCompanyName())
        .creditCode(result.getCreditCode())
        .companyType(result.getCompanyType())
        .legalRepresentative(result.getLegalRepresentative())
        .registeredCapital(result.getRegisteredCapital())
        .establishDate(result.getEstablishDate())
        .businessTerm(result.getBusinessTerm())
        .businessScope(result.getBusinessScope())
        .registeredAddress(result.getRegisteredAddress())
        // 名片
        .cardCompany(result.getCardCompany())
        .title(result.getTitle())
        .mobile(result.getMobile())
        .phone(result.getPhone())
        .email(result.getEmail())
        .website(result.getWebsite())
        // 发票
        .invoiceType(result.getInvoiceType())
        .invoiceCode(result.getInvoiceCode())
        .invoiceNo(result.getInvoiceNo())
        .invoiceDate(result.getInvoiceDate())
        .sellerName(result.getSellerName())
        .sellerTaxNo(result.getSellerTaxNo())
        .buyerName(result.getBuyerName())
        .buyerTaxNo(result.getBuyerTaxNo())
        .invoiceAmount(result.getInvoiceAmount())
        .taxAmount(result.getTaxAmount())
        .totalAmount(result.getTotalAmount())
        .build();
  }

  /**
   * 获取类型名称.
   *
   * @param type 类型
   * @return 类型名称
   */
  private String getTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "TEXT" -> "通用文字";
      case "BANK_RECEIPT" -> "银行回单";
      case "ID_CARD_FRONT" -> "身份证正面";
      case "ID_CARD_BACK" -> "身份证背面";
      case "BUSINESS_LICENSE" -> "营业执照";
      case "BUSINESS_CARD" -> "名片";
      case "INVOICE" -> "发票/票据";
      default -> type;
    };
  }
}
