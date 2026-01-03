package com.lawfirm.infrastructure.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 模拟OCR服务实现（开发测试用）
 * 生产环境应替换为真实OCR服务（如百度OCR、腾讯OCR等）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ocr.provider", havingValue = "mock", matchIfMissing = true)
public class MockOcrService implements OcrService {

    @Override
    public OcrResult recognizeText(MultipartFile file) {
        log.info("模拟OCR - 通用文字识别: {}", file.getOriginalFilename());
        return OcrResult.builder()
                .success(true)
                .type("TEXT")
                .rawText("这是模拟识别的文字内容。实际使用时请配置真实OCR服务。")
                .confidence(0.95)
                .build();
    }

    @Override
    public OcrResult recognizeText(String imageUrl) {
        log.info("模拟OCR - 通用文字识别(URL): {}", imageUrl);
        return recognizeText((MultipartFile) null);
    }

    @Override
    public OcrResult recognizeBankReceipt(MultipartFile file) {
        log.info("模拟OCR - 银行回单识别: {}", file != null ? file.getOriginalFilename() : "null");
        
        Map<String, Object> data = new HashMap<>();
        data.put("bankName", "中国工商银行");
        data.put("amount", "50000.00");
        data.put("transactionDate", "2026-01-03");
        data.put("payerName", "北京某某科技有限公司");
        data.put("payerAccount", "6222021234567890123");
        data.put("payeeName", "北京智慧律师事务所");
        data.put("payeeAccount", "6222029876543210987");
        data.put("transactionNo", "2026010312345678");
        data.put("remark", "法律服务费");

        return OcrResult.builder()
                .success(true)
                .type("BANK_RECEIPT")
                .data(data)
                .confidence(0.92)
                .bankName("中国工商银行")
                .amount(new BigDecimal("50000.00"))
                .transactionDate(LocalDate.of(2026, 1, 3))
                .payerName("北京某某科技有限公司")
                .payerAccount("6222021234567890123")
                .payeeName("北京智慧律师事务所")
                .payeeAccount("6222029876543210987")
                .transactionNo("2026010312345678")
                .remark("法律服务费")
                .build();
    }

    @Override
    public OcrResult recognizeBankReceipt(String imageUrl) {
        log.info("模拟OCR - 银行回单识别(URL): {}", imageUrl);
        return recognizeBankReceipt((MultipartFile) null);
    }

    @Override
    public OcrResult recognizeIdCard(MultipartFile file, boolean isFront) {
        log.info("模拟OCR - 身份证识别({}): {}", isFront ? "正面" : "背面", 
                file != null ? file.getOriginalFilename() : "null");
        
        Map<String, Object> data = new HashMap<>();
        
        if (isFront) {
            data.put("name", "张三");
            data.put("gender", "男");
            data.put("ethnicity", "汉");
            data.put("birthDate", "1990-01-15");
            data.put("address", "北京市朝阳区某某街道某某小区1号楼101室");
            data.put("idNumber", "110105199001150012");

            return OcrResult.builder()
                    .success(true)
                    .type("ID_CARD_FRONT")
                    .data(data)
                    .confidence(0.98)
                    .name("张三")
                    .gender("男")
                    .ethnicity("汉")
                    .birthDate(LocalDate.of(1990, 1, 15))
                    .address("北京市朝阳区某某街道某某小区1号楼101室")
                    .idNumber("110105199001150012")
                    .build();
        } else {
            data.put("issuingAuthority", "北京市公安局朝阳分局");
            data.put("validFrom", "2020-01-15");
            data.put("validTo", "2040-01-15");

            return OcrResult.builder()
                    .success(true)
                    .type("ID_CARD_BACK")
                    .data(data)
                    .confidence(0.97)
                    .issuingAuthority("北京市公安局朝阳分局")
                    .validFrom(LocalDate.of(2020, 1, 15))
                    .validTo(LocalDate.of(2040, 1, 15))
                    .build();
        }
    }

    @Override
    public OcrResult recognizeIdCard(String imageUrl, boolean isFront) {
        log.info("模拟OCR - 身份证识别(URL, {}): {}", isFront ? "正面" : "背面", imageUrl);
        return recognizeIdCard((MultipartFile) null, isFront);
    }

    @Override
    public OcrResult recognizeBusinessLicense(MultipartFile file) {
        log.info("模拟OCR - 营业执照识别: {}", file != null ? file.getOriginalFilename() : "null");
        
        Map<String, Object> data = new HashMap<>();
        data.put("companyName", "北京某某科技有限公司");
        data.put("creditCode", "91110105MA01ABCD12");
        data.put("companyType", "有限责任公司(自然人投资或控股)");
        data.put("legalRepresentative", "李四");
        data.put("registeredCapital", "1000万元人民币");
        data.put("establishDate", "2020-06-15");
        data.put("businessTerm", "2020-06-15至2050-06-14");
        data.put("businessScope", "技术开发、技术咨询、技术服务；软件开发；计算机系统服务");
        data.put("registeredAddress", "北京市朝阳区某某路100号");

        return OcrResult.builder()
                .success(true)
                .type("BUSINESS_LICENSE")
                .data(data)
                .confidence(0.95)
                .companyName("北京某某科技有限公司")
                .creditCode("91110105MA01ABCD12")
                .companyType("有限责任公司(自然人投资或控股)")
                .legalRepresentative("李四")
                .registeredCapital("1000万元人民币")
                .establishDate(LocalDate.of(2020, 6, 15))
                .businessTerm("2020-06-15至2050-06-14")
                .businessScope("技术开发、技术咨询、技术服务；软件开发；计算机系统服务")
                .registeredAddress("北京市朝阳区某某路100号")
                .build();
    }

    @Override
    public OcrResult recognizeBusinessLicense(String imageUrl) {
        log.info("模拟OCR - 营业执照识别(URL): {}", imageUrl);
        return recognizeBusinessLicense((MultipartFile) null);
    }
}
