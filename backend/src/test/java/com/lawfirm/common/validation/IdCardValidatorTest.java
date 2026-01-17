package com.lawfirm.common.validation;

import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdCardValidator 单元测试
 *
 * 测试身份证号验证功能
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

        // 这些是校验位正确的真实身份证号示例（测试用）
        assertThat(validator.isValid("110101199001011234", null)).isTrue();  // 校验位4
        assertThat(validator.isValid("320621198506150016", null)).isTrue();  // 校验位6
        assertThat(validator.isValid("440308199901010014", null)).isTrue();  // 校验位4
    }

    @Test
    @DisplayName("严格模式: 应该拒绝校验位错误的身份证号")
    void isValid_shouldRejectInvalidCheckCodeInStrictMode() {
        IdCard annotation = createAnnotation(true, true);
        validator.initialize(annotation);

        // 修改最后一位使校验位错误
        assertThat(validator.isValid("110101199001011230", null)).isFalse();  // 应该是4
        assertThat(validator.isValid("110101199001011235", null)).isFalse();  // 应该是4
    }

    @Test
    @DisplayName("严格模式: 应该接受大写和小写X")
    void isValid_shouldAcceptBothXCasesInStrictMode() {
        IdCard annotation = createAnnotation(true, true);
        validator.initialize(annotation);

        // 440308199901010014 的校验位是4，测试X校验位
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

        assertThat(validator.isValid("12345678901234567", null)).isFalse();   // 17位
        assertThat(validator.isValid("1234567890123456789", null)).isFalse();  // 19位
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
