package com.lawfirm.infrastructure.ocr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 模拟OCR服务实现（开发测试用）.
 *
 * <p>生产环境应替换为真实OCR服务（如百度OCR、腾讯OCR等）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ocr.provider", havingValue = "mock", matchIfMissing = false)
public class MockOcrService implements OcrService {

  /** 高置信度 */
  private static final double HIGH_CONFIDENCE = 0.95;

  /** 中等置信度 */
  private static final double MEDIUM_CONFIDENCE = 0.92;

  /** 较低置信度 */
  private static final double LOW_CONFIDENCE = 0.93;

  /** 身份证识别置信度 */
  private static final double ID_CARD_CONFIDENCE = 0.98;

  /** 营业执照识别置信度 */
  private static final double BUSINESS_LICENSE_CONFIDENCE = 0.97;

  /** 默认年份 */
  private static final int DEFAULT_YEAR = 2026;

  /** 默认月份 */
  private static final int DEFAULT_MONTH = 1;

  /** 默认日期 */
  private static final int DEFAULT_DAY = 15;

  /** 身份证出生年份 */
  private static final int ID_BIRTH_YEAR = 1990;

  /** 营业执照成立年份 */
  private static final int LICENSE_ESTABLISH_YEAR = 2020;

  /** 营业执照有效期年份 */
  private static final int LICENSE_VALID_YEAR = 2040;

  /** 营业执照成立月份 */
  private static final int LICENSE_ESTABLISH_MONTH = 6;

  /**
   * 通用文字识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeText(final MultipartFile file) {
    log.info("模拟OCR - 通用文字识别: {}", file.getOriginalFilename());
    return OcrResult.builder()
        .success(true)
        .type("TEXT")
        .rawText("这是模拟识别的文字内容。实际使用时请配置真实OCR服务。")
        .confidence(HIGH_CONFIDENCE)
        .build();
  }

  /**
   * 通用文字识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeText(final String imageUrl) {
    log.info("模拟OCR - 通用文字识别(URL): {}", imageUrl);
    return recognizeText((MultipartFile) null);
  }

  /**
   * 银行回单识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeBankReceipt(final MultipartFile file) {
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
        .confidence(MEDIUM_CONFIDENCE)
        .bankName("中国工商银行")
        .amount(new BigDecimal("50000.00"))
        .transactionDate(LocalDate.of(DEFAULT_YEAR, DEFAULT_MONTH, 3))
        .payerName("北京某某科技有限公司")
        .payerAccount("6222021234567890123")
        .payeeName("北京智慧律师事务所")
        .payeeAccount("6222029876543210987")
        .transactionNo("2026010312345678")
        .remark("法律服务费")
        .build();
  }

  /**
   * 银行回单识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeBankReceipt(final String imageUrl) {
    log.info("模拟OCR - 银行回单识别(URL): {}", imageUrl);
    return recognizeBankReceipt((MultipartFile) null);
  }

  /**
   * 身份证识别
   *
   * @param file 图片文件
   * @param isFront 是否为正面
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeIdCard(final MultipartFile file, final boolean isFront) {
    log.info(
        "模拟OCR - 身份证识别({}): {}",
        isFront ? "正面" : "背面",
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
          .confidence(ID_CARD_CONFIDENCE)
          .name("张三")
          .gender("男")
          .ethnicity("汉")
          .birthDate(LocalDate.of(ID_BIRTH_YEAR, DEFAULT_MONTH, DEFAULT_DAY))
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
          .confidence(BUSINESS_LICENSE_CONFIDENCE)
          .issuingAuthority("北京市公安局朝阳分局")
          .validFrom(LocalDate.of(LICENSE_ESTABLISH_YEAR, DEFAULT_MONTH, DEFAULT_DAY))
          .validTo(LocalDate.of(LICENSE_VALID_YEAR, DEFAULT_MONTH, DEFAULT_DAY))
          .build();
    }
  }

  /**
   * 身份证识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @param isFront 是否为正面
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeIdCard(final String imageUrl, final boolean isFront) {
    log.info("模拟OCR - 身份证识别(URL, {}): {}", isFront ? "正面" : "背面", imageUrl);
    return recognizeIdCard((MultipartFile) null, isFront);
  }

  /**
   * 营业执照识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeBusinessLicense(final MultipartFile file) {
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
        .confidence(HIGH_CONFIDENCE)
        .companyName("北京某某科技有限公司")
        .creditCode("91110105MA01ABCD12")
        .companyType("有限责任公司(自然人投资或控股)")
        .legalRepresentative("李四")
        .registeredCapital("1000万元人民币")
        .establishDate(LocalDate.of(LICENSE_ESTABLISH_YEAR, LICENSE_ESTABLISH_MONTH, DEFAULT_DAY))
        .businessTerm("2020-06-15至2050-06-14")
        .businessScope("技术开发、技术咨询、技术服务；软件开发；计算机系统服务")
        .registeredAddress("北京市朝阳区某某路100号")
        .build();
  }

  /**
   * 营业执照识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeBusinessLicense(final String imageUrl) {
    log.info("模拟OCR - 营业执照识别(URL): {}", imageUrl);
    return recognizeBusinessLicense((MultipartFile) null);
  }

  /**
   * 名片识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeBusinessCard(final MultipartFile file) {
    log.info("模拟OCR - 名片识别: {}", file != null ? file.getOriginalFilename() : "null");

    Map<String, Object> data = new HashMap<>();
    data.put("name", "张律师");
    data.put("company", "北京智慧律师事务所");
    data.put("title", "高级合伙人");
    data.put("mobile", "13800138000");
    data.put("phone", "010-88888888");
    data.put("email", "zhangls@lawfirm.com");
    data.put("address", "北京市朝阳区建国门外大街1号国贸大厦A座1801");
    data.put("website", "www.lawfirm.com");

    return OcrResult.builder()
        .success(true)
        .type("BUSINESS_CARD")
        .data(data)
        .confidence(MEDIUM_CONFIDENCE)
        .name("张律师")
        .cardCompany("北京智慧律师事务所")
        .title("高级合伙人")
        .mobile("13800138000")
        .phone("010-88888888")
        .email("zhangls@lawfirm.com")
        .address("北京市朝阳区建国门外大街1号国贸大厦A座1801")
        .website("www.lawfirm.com")
        .build();
  }

  /**
   * 名片识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeBusinessCard(final String imageUrl) {
    log.info("模拟OCR - 名片识别(URL): {}", imageUrl);
    return recognizeBusinessCard((MultipartFile) null);
  }

  /**
   * 发票/票据识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeInvoice(final MultipartFile file) {
    log.info("模拟OCR - 发票识别: {}", file != null ? file.getOriginalFilename() : "null");

    Map<String, Object> data = new HashMap<>();
    data.put("invoiceType", "增值税普通发票");
    data.put("invoiceCode", "011001900211");
    data.put("invoiceNo", "12345678");
    data.put("invoiceDate", "2026-01-05");
    data.put("sellerName", "北京某某餐饮有限公司");
    data.put("sellerTaxNo", "91110105MA01ABCD12");
    data.put("buyerName", "北京智慧律师事务所");
    data.put("buyerTaxNo", "91110105MA01EFGH34");
    data.put("amount", "188.68");
    data.put("taxAmount", "11.32");
    data.put("totalAmount", "200.00");

    return OcrResult.builder()
        .success(true)
        .type("INVOICE")
        .data(data)
        .confidence(LOW_CONFIDENCE)
        .invoiceType("增值税普通发票")
        .invoiceCode("011001900211")
        .invoiceNo("12345678")
        .invoiceDate(LocalDate.of(DEFAULT_YEAR, DEFAULT_MONTH, 5))
        .sellerName("北京某某餐饮有限公司")
        .sellerTaxNo("91110105MA01ABCD12")
        .buyerName("北京智慧律师事务所")
        .buyerTaxNo("91110105MA01EFGH34")
        .invoiceAmount(new BigDecimal("188.68"))
        .taxAmount(new BigDecimal("11.32"))
        .totalAmount(new BigDecimal("200.00"))
        .build();
  }

  /**
   * 发票/票据识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Override
  public OcrResult recognizeInvoice(final String imageUrl) {
    log.info("模拟OCR - 发票识别(URL): {}", imageUrl);
    return recognizeInvoice((MultipartFile) null);
  }
}
