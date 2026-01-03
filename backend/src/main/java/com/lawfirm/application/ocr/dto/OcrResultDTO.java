package com.lawfirm.application.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * OCR识别结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResultDTO {

    private boolean success;
    private String errorMessage;
    private String type;
    private String typeName;
    private String rawText;
    private Map<String, Object> data;
    private Double confidence;

    // 银行回单
    private String bankName;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String payerName;
    private String payerAccount;
    private String payeeName;
    private String payeeAccount;
    private String transactionNo;
    private String remark;

    // 身份证
    private String name;
    private String idNumber;
    private String gender;
    private String ethnicity;
    private LocalDate birthDate;
    private String address;
    private String issuingAuthority;
    private LocalDate validFrom;
    private LocalDate validTo;

    // 营业执照
    private String companyName;
    private String creditCode;
    private String companyType;
    private String legalRepresentative;
    private String registeredCapital;
    private LocalDate establishDate;
    private String businessTerm;
    private String businessScope;
    private String registeredAddress;
}
