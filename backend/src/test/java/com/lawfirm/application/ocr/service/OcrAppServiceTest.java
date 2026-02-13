package com.lawfirm.application.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.ocr.dto.OcrResultDTO;
import com.lawfirm.common.util.UrlSecurityValidator;
import com.lawfirm.infrastructure.ocr.OcrResult;
import com.lawfirm.infrastructure.ocr.OcrService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

/** OcrAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OcrAppService OCR服务测试")
class OcrAppServiceTest {

  @Mock private OcrService ocrService;

  @Mock private UrlSecurityValidator urlSecurityValidator;

  @Mock private MultipartFile multipartFile;

  @InjectMocks private OcrAppService ocrAppService;

  @Nested
  @DisplayName("通用文字识别测试")
  class RecognizeTextTests {

    @Test
    @DisplayName("应该成功识别文字")
    void recognizeText_shouldSuccess() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder()
              .success(true)
              .type("TEXT")
              .rawText("识别出的文字内容")
              .confidence(0.95)
              .build();

      when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
      when(ocrService.recognizeText(any(MultipartFile.class))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeText(multipartFile);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getRawText()).isEqualTo("识别出的文字内容");
      assertThat(result.getTypeName()).isEqualTo("通用文字");
    }

    @Test
    @DisplayName("应该成功通过URL识别文字")
    void recognizeTextByUrl_shouldSuccess() {
      // Given
      String imageUrl = "https://example.com/image.jpg";
      OcrResult ocrResult =
          OcrResult.builder().success(true).type("TEXT").rawText("识别出的文字").build();

      doNothing().when(urlSecurityValidator).validateImageUrl(imageUrl);
      when(ocrService.recognizeText(imageUrl)).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeTextByUrl(imageUrl);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      verify(urlSecurityValidator).validateImageUrl(imageUrl);
    }
  }

  @Nested
  @DisplayName("银行回单识别测试")
  class RecognizeBankReceiptTests {

    @Test
    @DisplayName("应该成功识别银行回单")
    void recognizeBankReceipt_shouldSuccess() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder()
              .success(true)
              .type("BANK_RECEIPT")
              .bankName("中国银行")
              .amount(BigDecimal.valueOf(10000.00))
              .transactionDate(LocalDate.now())
              .payerName("付款人")
              .payerAccount("1234567890")
              .payeeName("收款人")
              .payeeAccount("0987654321")
              .transactionNo("TXN123456")
              .build();

      when(multipartFile.getOriginalFilename()).thenReturn("receipt.jpg");
      when(ocrService.recognizeBankReceipt(any(MultipartFile.class))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeBankReceipt(multipartFile);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getBankName()).isEqualTo("中国银行");
      assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000.00));
      assertThat(result.getTypeName()).isEqualTo("银行回单");
    }

    @Test
    @DisplayName("应该成功通过URL识别银行回单")
    void recognizeBankReceiptByUrl_shouldSuccess() {
      // Given
      String imageUrl = "https://example.com/receipt.jpg";
      OcrResult ocrResult = OcrResult.builder().success(true).type("BANK_RECEIPT").build();

      doNothing().when(urlSecurityValidator).validateImageUrl(imageUrl);
      when(ocrService.recognizeBankReceipt(imageUrl)).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeBankReceiptByUrl(imageUrl);

      // Then
      assertThat(result).isNotNull();
      verify(urlSecurityValidator).validateImageUrl(imageUrl);
    }
  }

  @Nested
  @DisplayName("身份证识别测试")
  class RecognizeIdCardTests {

    @Test
    @DisplayName("应该成功识别身份证正面")
    void recognizeIdCard_shouldSuccess_whenFront() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder()
              .success(true)
              .type("ID_CARD_FRONT")
              .name("张三")
              .idNumber("110101199001011234")
              .gender("男")
              .ethnicity("汉")
              .birthDate(LocalDate.of(1990, 1, 1))
              .address("北京市朝阳区")
              .build();

      when(multipartFile.getOriginalFilename()).thenReturn("idcard_front.jpg");
      when(ocrService.recognizeIdCard(any(MultipartFile.class), eq(true))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeIdCard(multipartFile, true);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getName()).isEqualTo("张三");
      assertThat(result.getIdNumber()).isEqualTo("110101199001011234");
      assertThat(result.getTypeName()).isEqualTo("身份证正面");
    }

    @Test
    @DisplayName("应该成功识别身份证背面")
    void recognizeIdCard_shouldSuccess_whenBack() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder()
              .success(true)
              .type("ID_CARD_BACK")
              .issuingAuthority("北京市公安局")
              .validFrom(LocalDate.of(2010, 1, 1))
              .validTo(LocalDate.of(2030, 1, 1))
              .build();

      when(multipartFile.getOriginalFilename()).thenReturn("idcard_back.jpg");
      when(ocrService.recognizeIdCard(any(MultipartFile.class), eq(false))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeIdCard(multipartFile, false);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getIssuingAuthority()).isEqualTo("北京市公安局");
      assertThat(result.getTypeName()).isEqualTo("身份证背面");
    }

    @Test
    @DisplayName("应该成功通过URL识别身份证")
    void recognizeIdCardByUrl_shouldSuccess() {
      // Given
      String imageUrl = "https://example.com/idcard.jpg";
      OcrResult ocrResult = OcrResult.builder().success(true).type("ID_CARD_FRONT").build();

      doNothing().when(urlSecurityValidator).validateImageUrl(imageUrl);
      when(ocrService.recognizeIdCard(imageUrl, true)).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeIdCardByUrl(imageUrl, true);

      // Then
      assertThat(result).isNotNull();
      verify(urlSecurityValidator).validateImageUrl(imageUrl);
    }
  }

  @Nested
  @DisplayName("营业执照识别测试")
  class RecognizeBusinessLicenseTests {

    @Test
    @DisplayName("应该成功识别营业执照")
    void recognizeBusinessLicense_shouldSuccess() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder()
              .success(true)
              .type("BUSINESS_LICENSE")
              .companyName("测试公司")
              .creditCode("91110000123456789X")
              .companyType("有限责任公司")
              .legalRepresentative("李四")
              .registeredCapital("1000万元")
              .establishDate(LocalDate.of(2020, 1, 1))
              .businessScope("技术开发")
              .registeredAddress("北京市海淀区")
              .build();

      when(multipartFile.getOriginalFilename()).thenReturn("license.jpg");
      when(ocrService.recognizeBusinessLicense(any(MultipartFile.class))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeBusinessLicense(multipartFile);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getCompanyName()).isEqualTo("测试公司");
      assertThat(result.getCreditCode()).isEqualTo("91110000123456789X");
      assertThat(result.getTypeName()).isEqualTo("营业执照");
    }

    @Test
    @DisplayName("应该成功通过URL识别营业执照")
    void recognizeBusinessLicenseByUrl_shouldSuccess() {
      // Given
      String imageUrl = "https://example.com/license.jpg";
      OcrResult ocrResult = OcrResult.builder().success(true).type("BUSINESS_LICENSE").build();

      doNothing().when(urlSecurityValidator).validateImageUrl(imageUrl);
      when(ocrService.recognizeBusinessLicense(imageUrl)).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeBusinessLicenseByUrl(imageUrl);

      // Then
      assertThat(result).isNotNull();
      verify(urlSecurityValidator).validateImageUrl(imageUrl);
    }
  }

  @Nested
  @DisplayName("名片识别测试")
  class RecognizeBusinessCardTests {

    @Test
    @DisplayName("应该成功识别名片")
    void recognizeBusinessCard_shouldSuccess() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder()
              .success(true)
              .type("BUSINESS_CARD")
              .cardCompany("测试公司")
              .title("经理")
              .mobile("13800138000")
              .email("test@example.com")
              .build();

      when(multipartFile.getOriginalFilename()).thenReturn("card.jpg");
      when(ocrService.recognizeBusinessCard(any(MultipartFile.class))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeBusinessCard(multipartFile);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getCardCompany()).isEqualTo("测试公司");
      assertThat(result.getMobile()).isEqualTo("13800138000");
      assertThat(result.getTypeName()).isEqualTo("名片");
    }
  }

  @Nested
  @DisplayName("发票识别测试")
  class RecognizeInvoiceTests {

    @Test
    @DisplayName("应该成功识别发票")
    void recognizeInvoice_shouldSuccess() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder()
              .success(true)
              .type("INVOICE")
              .invoiceType("增值税普通发票")
              .invoiceCode("1234567890")
              .invoiceNo("00012345")
              .invoiceDate(LocalDate.now())
              .sellerName("销售方")
              .sellerTaxNo("123456789012345")
              .buyerName("购买方")
              .buyerTaxNo("987654321098765")
              .invoiceAmount(BigDecimal.valueOf(1000.00))
              .taxAmount(BigDecimal.valueOf(130.00))
              .totalAmount(BigDecimal.valueOf(1130.00))
              .build();

      when(multipartFile.getOriginalFilename()).thenReturn("invoice.jpg");
      when(ocrService.recognizeInvoice(any(MultipartFile.class))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeInvoice(multipartFile);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getInvoiceCode()).isEqualTo("1234567890");
      assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1130.00));
      assertThat(result.getTypeName()).isEqualTo("发票/票据");
    }

    @Test
    @DisplayName("应该成功通过URL识别发票")
    void recognizeInvoiceByUrl_shouldSuccess() {
      // Given
      String imageUrl = "https://example.com/invoice.jpg";
      OcrResult ocrResult = OcrResult.builder().success(true).type("INVOICE").build();

      doNothing().when(urlSecurityValidator).validateImageUrl(imageUrl);
      when(ocrService.recognizeInvoice(imageUrl)).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeInvoiceByUrl(imageUrl);

      // Then
      assertThat(result).isNotNull();
      verify(urlSecurityValidator).validateImageUrl(imageUrl);
    }
  }

  @Nested
  @DisplayName("错误处理测试")
  class ErrorHandlingTests {

    @Test
    @DisplayName("应该处理识别失败的情况")
    void recognizeText_shouldHandleFailure() {
      // Given
      OcrResult ocrResult =
          OcrResult.builder().success(false).errorMessage("识别失败：图片格式不支持").type("TEXT").build();

      when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
      when(ocrService.recognizeText(any(MultipartFile.class))).thenReturn(ocrResult);

      // When
      OcrResultDTO result = ocrAppService.recognizeText(multipartFile);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getErrorMessage()).isEqualTo("识别失败：图片格式不支持");
    }
  }
}
