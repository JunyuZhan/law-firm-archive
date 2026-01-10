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
 * 安全说明：所有URL接口都经过SSRF防护验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrAppService {

    private final OcrService ocrService;
    private final UrlSecurityValidator urlSecurityValidator;

    /**
     * 通用文字识别
     */
    public OcrResultDTO recognizeText(MultipartFile file) {
        log.info("通用文字识别: {}", file.getOriginalFilename());
        OcrResult result = ocrService.recognizeText(file);
        return toDTO(result);
    }

    /**
     * 通用文字识别（URL）
     * 安全：已验证URL防止SSRF攻击
     */
    public OcrResultDTO recognizeTextByUrl(String imageUrl) {
        urlSecurityValidator.validateImageUrl(imageUrl);  // SSRF防护
        log.info("通用文字识别(URL): {}", imageUrl);
        OcrResult result = ocrService.recognizeText(imageUrl);
        return toDTO(result);
    }

    /**
     * 银行回单识别
     */
    public OcrResultDTO recognizeBankReceipt(MultipartFile file) {
        log.info("银行回单识别: {}", file.getOriginalFilename());
        OcrResult result = ocrService.recognizeBankReceipt(file);
        return toDTO(result);
    }

    /**
     * 银行回单识别（URL）
     * 安全：已验证URL防止SSRF攻击
     */
    public OcrResultDTO recognizeBankReceiptByUrl(String imageUrl) {
        urlSecurityValidator.validateImageUrl(imageUrl);  // SSRF防护
        log.info("银行回单识别(URL): {}", imageUrl);
        OcrResult result = ocrService.recognizeBankReceipt(imageUrl);
        return toDTO(result);
    }

    /**
     * 身份证识别
     */
    public OcrResultDTO recognizeIdCard(MultipartFile file, boolean isFront) {
        log.info("身份证识别({}): {}", isFront ? "正面" : "背面", file.getOriginalFilename());
        OcrResult result = ocrService.recognizeIdCard(file, isFront);
        return toDTO(result);
    }

    /**
     * 身份证识别（URL）
     * 安全：已验证URL防止SSRF攻击
     */
    public OcrResultDTO recognizeIdCardByUrl(String imageUrl, boolean isFront) {
        urlSecurityValidator.validateImageUrl(imageUrl);  // SSRF防护
        log.info("身份证识别(URL, {}): {}", isFront ? "正面" : "背面", imageUrl);
        OcrResult result = ocrService.recognizeIdCard(imageUrl, isFront);
        return toDTO(result);
    }

    /**
     * 营业执照识别
     */
    public OcrResultDTO recognizeBusinessLicense(MultipartFile file) {
        log.info("营业执照识别: {}", file.getOriginalFilename());
        OcrResult result = ocrService.recognizeBusinessLicense(file);
        return toDTO(result);
    }

    /**
     * 营业执照识别（URL）
     * 安全：已验证URL防止SSRF攻击
     */
    public OcrResultDTO recognizeBusinessLicenseByUrl(String imageUrl) {
        urlSecurityValidator.validateImageUrl(imageUrl);  // SSRF防护
        log.info("营业执照识别(URL): {}", imageUrl);
        OcrResult result = ocrService.recognizeBusinessLicense(imageUrl);
        return toDTO(result);
    }

    /**
     * 名片识别
     */
    public OcrResultDTO recognizeBusinessCard(MultipartFile file) {
        log.info("名片识别: {}", file.getOriginalFilename());
        OcrResult result = ocrService.recognizeBusinessCard(file);
        return toDTO(result);
    }

    /**
     * 名片识别（URL）
     * 安全：已验证URL防止SSRF攻击
     */
    public OcrResultDTO recognizeBusinessCardByUrl(String imageUrl) {
        urlSecurityValidator.validateImageUrl(imageUrl);  // SSRF防护
        log.info("名片识别(URL): {}", imageUrl);
        OcrResult result = ocrService.recognizeBusinessCard(imageUrl);
        return toDTO(result);
    }

    /**
     * 发票识别
     */
    public OcrResultDTO recognizeInvoice(MultipartFile file) {
        log.info("发票识别: {}", file.getOriginalFilename());
        OcrResult result = ocrService.recognizeInvoice(file);
        return toDTO(result);
    }

    /**
     * 发票识别（URL）
     * 安全：已验证URL防止SSRF攻击
     */
    public OcrResultDTO recognizeInvoiceByUrl(String imageUrl) {
        urlSecurityValidator.validateImageUrl(imageUrl);  // SSRF防护
        log.info("发票识别(URL): {}", imageUrl);
        OcrResult result = ocrService.recognizeInvoice(imageUrl);
        return toDTO(result);
    }

    private OcrResultDTO toDTO(OcrResult result) {
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

    private String getTypeName(String type) {
        if (type == null) return null;
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
