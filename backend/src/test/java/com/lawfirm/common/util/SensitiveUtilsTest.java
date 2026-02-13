package com.lawfirm.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * SensitiveUtils 单元测试
 *
 * <p>测试数据脱敏功能： - 手机号脱敏 - 身份证脱敏 - 姓名脱敏 - 邮箱脱敏 - 银行卡脱敏
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@DisplayName("SensitiveUtils 单元测试")
class SensitiveUtilsTest {

  @Nested
  @DisplayName("手机号脱敏")
  class PhoneTests {

    @Test
    @DisplayName("标准11位手机号")
    void maskPhone_Standard11Digits() {
      // Given
      String phone = "13812345678";

      // When
      String result = SensitiveUtils.maskPhone(phone);

      // Then
      assertEquals("138****5678", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("空值处理")
    void maskPhone_NullOrEmpty(String phone) {
      // When
      String result = SensitiveUtils.maskPhone(phone);

      // Then
      assertEquals(phone, result);
    }

    @Test
    @DisplayName("短手机号不处理")
    void maskPhone_ShortNumber() {
      // Given
      String phone = "12345";

      // When
      String result = SensitiveUtils.maskPhone(phone);

      // Then
      assertEquals(phone, result);
    }
  }

  @Nested
  @DisplayName("身份证脱敏")
  class IdCardTests {

    @Test
    @DisplayName("18位身份证")
    void maskIdCard_18Digits() {
      // Given
      String idCard = "110101199001011234";

      // When
      String result = SensitiveUtils.maskIdCard(idCard);

      // Then
      assertEquals("110101********1234", result);
    }

    @Test
    @DisplayName("15位身份证")
    void maskIdCard_15Digits() {
      // Given
      String idCard = "110101900101123";

      // When
      String result = SensitiveUtils.maskIdCard(idCard);

      // Then
      assertTrue(result.contains("*"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("空值处理")
    void maskIdCard_NullOrEmpty(String idCard) {
      // When
      String result = SensitiveUtils.maskIdCard(idCard);

      // Then
      assertEquals(idCard, result);
    }
  }

  @Nested
  @DisplayName("姓名脱敏")
  class NameTests {

    @Test
    @DisplayName("两字姓名")
    void maskName_TwoChars() {
      // Given
      String name = "张三";

      // When
      String result = SensitiveUtils.maskName(name);

      // Then
      assertEquals("张*", result);
    }

    @Test
    @DisplayName("三字姓名")
    void maskName_ThreeChars() {
      // Given
      String name = "张三丰";

      // When
      String result = SensitiveUtils.maskName(name);

      // Then
      assertEquals("张**", result);
    }

    @Test
    @DisplayName("单字姓名")
    void maskName_SingleChar() {
      // Given
      String name = "张";

      // When
      String result = SensitiveUtils.maskName(name);

      // Then
      assertEquals("张", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("空值处理")
    void maskName_NullOrEmpty(String name) {
      // When
      String result = SensitiveUtils.maskName(name);

      // Then
      assertEquals(name, result);
    }
  }

  @Nested
  @DisplayName("邮箱脱敏")
  class EmailTests {

    @Test
    @DisplayName("标准邮箱")
    void maskEmail_Standard() {
      // Given
      String email = "test@example.com";

      // When
      String result = SensitiveUtils.maskEmail(email);

      // Then
      assertTrue(result.contains("***"));
      assertTrue(result.endsWith("@example.com"));
    }

    @Test
    @DisplayName("短用户名邮箱")
    void maskEmail_ShortUsername() {
      // Given
      String email = "ab@test.com";

      // When
      String result = SensitiveUtils.maskEmail(email);

      // Then
      assertTrue(result.contains("@test.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("空值处理")
    void maskEmail_NullOrEmpty(String email) {
      // When
      String result = SensitiveUtils.maskEmail(email);

      // Then
      assertEquals(email, result);
    }
  }

  @Nested
  @DisplayName("银行卡脱敏")
  class BankCardTests {

    @Test
    @DisplayName("16位银行卡")
    void maskBankCard_16Digits() {
      // Given
      String bankCard = "6222021234567890";

      // When
      String result = SensitiveUtils.maskBankCard(bankCard);

      // Then
      assertTrue(result.startsWith("6222"));
      assertTrue(result.endsWith("7890"));
      assertTrue(result.contains("****"));
    }

    @Test
    @DisplayName("19位银行卡")
    void maskBankCard_19Digits() {
      // Given
      String bankCard = "6222021234567890123";

      // When
      String result = SensitiveUtils.maskBankCard(bankCard);

      // Then
      assertTrue(result.contains("****"));
    }
  }

  @Nested
  @DisplayName("通用脱敏")
  class GenericMaskTests {

    @ParameterizedTest
    @CsvSource({"12345678, 2, 2, 12****78", "abcdefgh, 3, 2, abc***gh"})
    @DisplayName("自定义前后保留位数")
    void mask_CustomPrefixSuffix(String input, int front, int end, String expected) {
      // When
      String result = SensitiveUtils.mask(input, front, end);

      // Then
      assertEquals(expected, result);
    }
  }

  @Nested
  @DisplayName("地址脱敏")
  class AddressTests {

    @Test
    @DisplayName("标准地址")
    void maskAddress_Standard() {
      // Given
      String address = "北京市朝阳区建国路100号";

      // When
      String result = SensitiveUtils.maskAddress(address);

      // Then
      assertTrue(result.contains("***"));
      assertTrue(result.startsWith("北京市朝阳区"));
    }
  }
}
