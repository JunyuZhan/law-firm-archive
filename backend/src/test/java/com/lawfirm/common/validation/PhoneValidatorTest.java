package com.lawfirm.common.validation;

import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PhoneValidator 单元测试
 *
 * 测试手机号验证功能
 */
@DisplayName("PhoneValidator 手机号验证器测试")
class PhoneValidatorTest {

    private PhoneValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PhoneValidator();
    }

    // ========== nullable = true (默认) ==========

    @Test
    @DisplayName("nullable=true: null 应该通过验证")
    void isValid_shouldReturnTrueForNullWhenNullable() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("nullable=true: 空字符串应该通过验证")
    void isValid_shouldReturnTrueForEmptyWhenNullable() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    @DisplayName("nullable=true: 只有空格的字符串应该通过验证")
    void isValid_shouldReturnTrueForBlankWhenNullable() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    // ========== nullable = false ==========

    @Test
    @DisplayName("nullable=false: null 应该不通过验证")
    void isValid_shouldReturnFalseForNullWhenNotNullable() {
        Phone annotation = createAnnotation(false);
        validator.initialize(annotation);
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    @DisplayName("nullable=false: 空字符串应该不通过验证")
    void isValid_shouldReturnFalseForEmptyWhenNotNullable() {
        Phone annotation = createAnnotation(false);
        validator.initialize(annotation);
        assertThat(validator.isValid("", null)).isFalse();
    }

    // ========== 有效的手机号 ==========

    @Test
    @DisplayName("应该接受有效的11位手机号")
    void isValid_shouldAcceptValidPhoneNumbers() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);

        // 各运营商号段
        assertThat(validator.isValid("13812345678", null)).isTrue();  // 中国移动
        assertThat(validator.isValid("15912345678", null)).isTrue();
        assertThat(validator.isValid("18812345678", null)).isTrue();

        assertThat(validator.isValid("13012345678", null)).isTrue();  // 中国联通
        assertThat(validator.isValid("18612345678", null)).isTrue();

        assertThat(validator.isValid("13312345678", null)).isTrue();  // 中国电信
        assertThat(validator.isValid("19912345678", null)).isTrue();

        assertThat(validator.isValid("17012345678", null)).isTrue();  // 虚拟运营商
        assertThat(validator.isValid("19112345678", null)).isTrue();
    }

    // ========== 带格式的手机号 ==========

    @Test
    @DisplayName("应该接受带空格的手机号")
    void isValid_shouldAcceptPhoneWithSpaces() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);
        assertThat(validator.isValid("138 1234 5678", null)).isTrue();
    }

    @Test
    @DisplayName("应该接受带连字符的手机号")
    void isValid_shouldAcceptPhoneWithHyphens() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);
        assertThat(validator.isValid("138-1234-5678", null)).isTrue();
    }

    // ========== 无效的手机号 ==========

    @Test
    @DisplayName("应该拒绝非1开头的号码")
    void isValid_shouldRejectNon1StartingNumbers() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);

        assertThat(validator.isValid("23812345678", null)).isFalse();
        assertThat(validator.isValid("03812345678", null)).isFalse();
    }

    @Test
    @DisplayName("应该拒绝第二位不是3-9的号码")
    void isValid_shouldRejectInvalidSecondDigit() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);

        assertThat(validator.isValid("10812345678", null)).isFalse();
        assertThat(validator.isValid("12812345678", null)).isFalse();
        assertThat(validator.isValid("10812345678", null)).isFalse();
    }

    @Test
    @DisplayName("应该拒绝长度不足11位的号码")
    void isValid_shouldRejectShortNumbers() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);

        assertThat(validator.isValid("1381234567", null)).isFalse();   // 10位
        assertThat(validator.isValid("138123456", null)).isFalse();    // 9位
    }

    @Test
    @DisplayName("应该拒绝超过11位的号码")
    void isValid_shouldRejectLongNumbers() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);

        assertThat(validator.isValid("138123456789", null)).isFalse();  // 12位
    }

    @Test
    @DisplayName("应该拒绝包含非数字字符的号码（除了空格和连字符）")
    void isValid_shouldRejectNonNumericCharacters() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);

        assertThat(validator.isValid("138a2345678", null)).isFalse();
        assertThat(validator.isValid("138-2345-abcd", null)).isFalse();
    }

    @Test
    @DisplayName("应该拒绝全空格号码")
    void isValid_shouldRejectAllSpaces() {
        Phone annotation = createAnnotation(false);
        validator.initialize(annotation);

        assertThat(validator.isValid("         ", null)).isFalse();
    }

    // ========== 边界情况 ==========

    @Test
    @DisplayName("应该处理前后有空格的有效号码")
    void isValid_shouldHandleValidNumberWithSurroundingSpaces() {
        Phone annotation = createAnnotation(true);
        validator.initialize(annotation);

        // 前后空格会被视为有文本，但trim后才是有效的
        // 空格会被移除再校验
        assertThat(validator.isValid(" 13812345678 ", null)).isTrue();
    }

    // ========== 辅助方法 ==========

    private Phone createAnnotation(boolean nullable) {
        return new Phone() {
            @Override
            public String message() {
                return "手机号格式不正确";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public boolean nullable() {
                return nullable;
            }

            @Override
            public Class<? extends Phone> annotationType() {
                return Phone.class;
            }
        };
    }
}
