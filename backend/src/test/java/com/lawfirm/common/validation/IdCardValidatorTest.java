package com.lawfirm.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * IdCardValidator 单元测试
 *
 * <p>测试身份证号验证功能
 */
@DisplayName("IdCardValidator 身份证号验证器测试")
class IdCardValidatorTest {

  private IdCardValidator validator;

  @BeforeEach
  void setUp() {
    validator = new IdCardValidator();
  }

  // ========== nullable = true (默认) ==========

  @Test
  @DisplayName("nullable=true: null 应该通过验证")
  void isValid_shouldReturnTrueForNullWhenNullable() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);
    assertThat(validator.isValid(null, null)).isTrue();
  }

  @Test
  @DisplayName("nullable=true: 空字符串应该通过验证")
  void isValid_shouldReturnTrueForEmptyWhenNullable() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);
    assertThat(validator.isValid("", null)).isTrue();
  }

  // ========== nullable = false ==========

  @Test
  @DisplayName("nullable=false: null 应该不通过验证")
  void isValid_shouldReturnFalseForNullWhenNotNullable() {
    IdCard annotation = createAnnotation(false, false);
    validator.initialize(annotation);
    assertThat(validator.isValid(null, null)).isFalse();
  }

  // ========== 有效的身份证号（非严格模式） ==========

  @Test
  @DisplayName("非严格模式: 应该接受18位格式正确的身份证号")
  void isValid_shouldAcceptValidFormatInNonStrictMode() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);

    assertThat(validator.isValid("110101199001011234", null)).isTrue();
    assertThat(validator.isValid("123456789012345678", null)).isTrue();
  }

  @Test
  @DisplayName("非严格模式: 应该接受带X的身份证号")
  void isValid_shouldAcceptXInNonStrictMode() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);

    assertThat(validator.isValid("11010119900101123X", null)).isTrue();
    assertThat(validator.isValid("11010119900101123x", null)).isTrue();
  }

  // ========== 严格模式验证 ==========

  @Test
  @DisplayName("严格模式: 应该接受校验位正确的身份证号")
  void isValid_shouldAcceptValidCheckCodeInStrictMode() {
    IdCard annotation = createAnnotation(true, true);
    validator.initialize(annotation);

    // 110101199001011234 计算校验位：
    // 1*7 + 1*9 + 0*10 + 1*5 + 0*8 + 1*4 + 1*2 + 9*1 + 9*6 + 0*3 + 0*7 + 1*9 + 0*10 + 1*5 + 1*8 +
    // 2*4 + 3*2
    // = 7 + 9 + 0 + 5 + 0 + 4 + 2 + 9 + 54 + 0 + 0 + 9 + 0 + 5 + 8 + 8 + 6 = 126
    // 126 % 11 = 5, CHECK_CODES[5] = '7'
    // 所以正确的身份证号应该是 110101199001011237
    assertThat(validator.isValid("110101199001011237", null)).isTrue();

    // 11010519491231002X - 这是一个真实的测试身份证号（校验位X）
    // 验证：权重求和后 mod 11 = 2, CHECK_CODES[2] = 'X'
    assertThat(validator.isValid("11010519491231002X", null)).isTrue();
    assertThat(validator.isValid("11010519491231002x", null)).isTrue();
  }

  @Test
  @DisplayName("严格模式: 应该拒绝校验位错误的身份证号")
  void isValid_shouldRejectInvalidCheckCodeInStrictMode() {
    IdCard annotation = createAnnotation(true, true);
    validator.initialize(annotation);

    // 正确的应该是 110101199001011237
    assertThat(validator.isValid("110101199001011230", null)).isFalse(); // 校验位错误
    assertThat(validator.isValid("110101199001011235", null)).isFalse(); // 校验位错误

    // 正确的应该是 11010519491231002X
    assertThat(validator.isValid("110105194912310021", null)).isFalse(); // 校验位错误
  }

  @Test
  @DisplayName("严格模式: 应该接受大写和小写X")
  void isValid_shouldAcceptBothXCasesInStrictMode() {
    IdCard annotation = createAnnotation(true, true);
    validator.initialize(annotation);

    // 11010519491231002X 的校验位是X
    assertThat(validator.isValid("11010519491231002X", null)).isTrue();
    assertThat(validator.isValid("11010519491231002x", null)).isTrue();
  }

  // ========== 格式验证 ==========

  @Test
  @DisplayName("应该拒绝不是18位的身份证号")
  void isValid_shouldRejectNon18Digit() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);

    assertThat(validator.isValid("12345678901234567", null)).isFalse(); // 17位
    assertThat(validator.isValid("1234567890123456789", null)).isFalse(); // 19位
  }

  @Test
  @DisplayName("应该拒绝包含非数字字符（最后一位除外）的身份证号")
  void isValid_shouldRejectNonNumericExceptLast() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);

    assertThat(validator.isValid("11010119900a011234", null)).isFalse();
    assertThat(validator.isValid("abcdefghij12345678x", null)).isFalse();
  }

  @Test
  @DisplayName("应该拒绝最后一位不是数字或X/x的身份证号")
  void isValid_shouldRejectInvalidLastChar() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);

    assertThat(validator.isValid("11010119900101123Y", null)).isFalse();
    assertThat(validator.isValid("11010119900101123#", null)).isFalse();
  }

  // ========== 边界情况 ==========

  @Test
  @DisplayName("应该处理前后有空格的身份证号")
  void isValid_shouldHandleIdCardWithSurroundingSpaces() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);

    assertThat(validator.isValid(" 110101199001011234 ", null)).isTrue();
  }

  @Test
  @DisplayName("应该处理全数字的18位身份证号")
  void isValid_shouldHandleAllNumeric18Digit() {
    IdCard annotation = createAnnotation(true, false);
    validator.initialize(annotation);

    assertThat(validator.isValid("123456789012345678", null)).isTrue();
  }

  // ========== 辅助方法 ==========

  private IdCard createAnnotation(boolean nullable, boolean strict) {
    return new IdCard() {
      @Override
      public String message() {
        return "身份证号格式不正确";
      }

      @Override
      public Class<?>[] groups() {
        return new Class<?>[0];
      }

      @Override
      @SuppressWarnings("unchecked")
      public Class<? extends Payload>[] payload() {
        return new Class[0];
      }

      @Override
      public boolean nullable() {
        return nullable;
      }

      @Override
      public boolean strict() {
        return strict;
      }

      @Override
      public Class<? extends IdCard> annotationType() {
        return IdCard.class;
      }
    };
  }
}
