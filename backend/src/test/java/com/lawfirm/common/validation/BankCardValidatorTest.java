package com.lawfirm.common.validation;

import jakarta.validation.Payload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BankCardValidator 单元测试
 *
 * 测试银行卡号验证功能
 */
@DisplayName("BankCardValidator 银行卡号验证器测试")
class BankCardValidatorTest {

    // ========== nullable = true (默认) ==========

    @Test
    @DisplayName("nullable=true: null 应该通过验证")
    void isValid_shouldReturnTrueForNullWhenNullable() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("nullable=true: 空字符串应该通过验证")
    void isValid_shouldReturnTrueForEmptyWhenNullable() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid("", null)).isTrue();
    }

    // ========== nullable = false ==========

    @Test
    @DisplayName("nullable=false: null 应该不通过验证")
    void isValid_shouldReturnFalseForNullWhenNotNullable() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(false, false);
        validator.initialize(annotation);

        assertThat(validator.isValid(null, null)).isFalse();
    }

    // ========== 有效的银行卡号（格式验证） ==========

    @ParameterizedTest
    @ValueSource(strings = {
        "1234567890123456",   // 16位
        "12345678901234567",  // 17位
        "123456789012345678",  // 18位
        "1234567890123456789"  // 19位
    })
    @DisplayName("应该接受16-19位数字的银行卡号")
    void isValid_shouldAcceptValidLengthBankCards(String cardNumber) {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid(cardNumber, null)).isTrue();
    }

    @Test
    @DisplayName("应该接受带空格的银行卡号")
    void isValid_shouldAcceptBankCardWithSpaces() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid("1234 5678 9012 3456", null)).isTrue();
        assertThat(validator.isValid("1234 5678 9012 3456 789", null)).isTrue();
    }

    @Test
    @DisplayName("应该接受带连字符的银行卡号")
    void isValid_shouldAcceptBankCardWithHyphens() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid("1234-5678-9012-3456", null)).isTrue();
    }

    // ========== 无效的银行卡号 ==========

    @Test
    @DisplayName("应该拒绝长度不足16位的银行卡号")
    void isValid_shouldRejectShortBankCards() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid("123456789012345", null)).isFalse();   // 15位
        assertThat(validator.isValid("123456789012345", null)).isFalse();   // 15位
    }

    @Test
    @DisplayName("应该拒绝超过19位的银行卡号")
    void isValid_shouldRejectLongBankCards() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid("12345678901234567890", null)).isFalse();  // 20位
    }

    @Test
    @DisplayName("应该拒绝包含非数字字符的银行卡号")
    void isValid_shouldRejectNonNumericBankCards() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid("1234abcd5678901234", null)).isFalse();
        assertThat(validator.isValid("1234-5678-9012-abcd", null)).isFalse();
    }

    // ========== Luhn算法验证 ==========

    @Test
    @DisplayName("Luhn验证: 应该接受通过Luhn算法的银行卡号")
    void isValid_shouldAcceptLuhnValidBankCards() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, true);
        validator.initialize(annotation);

        // 这些是真实有效的银行卡号（测试数据）
        // 4111111111111111 - Visa测试卡号（通过Luhn）
        assertThat(validator.isValid("4111111111111111", null)).isTrue();

        // 4242424242424242 - Stripe测试卡号（通过Luhn）
        assertThat(validator.isValid("4242424242424242", null)).isTrue();

        // 5555555555554444 - MasterCard测试卡号（通过Luhn）
        assertThat(validator.isValid("5555555555554444", null)).isTrue();
    }

    @Test
    @DisplayName("Luhn验证: 应该拒绝未通过Luhn算法的银行卡号")
    void isValid_shouldRejectLuhnInvalidBankCards() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, true);
        validator.initialize(annotation);

        // 修改一位使Luhn校验失败
        assertThat(validator.isValid("4111111111111112", null)).isFalse();
        assertThat(validator.isValid("4242424242424241", null)).isFalse();
        assertThat(validator.isValid("1234567890123456", null)).isFalse();
    }

    @Test
    @DisplayName("Luhn验证: 格式正确但Luhn校验失败的应该被拒绝")
    void isValid_shouldRejectFormatValidButLuhnInvalid() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, true);
        validator.initialize(annotation);

        // 全1的16位数字不通过Luhn
        assertThat(validator.isValid("1111111111111111", null)).isFalse();
    }

    // ========== 边界情况 ==========

    @Test
    @DisplayName("应该处理前后有空格的银行卡号")
    void isValid_shouldHandleBankCardWithSurroundingSpaces() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid(" 1234567890123456 ", null)).isTrue();
    }

    @Test
    @DisplayName("应该处理混合空格和连字符的银行卡号")
    void isValid_shouldHandleMixedSpacesAndHyphens() {
        BankCardValidator validator = new BankCardValidator();
        BankCard annotation = createAnnotation(true, false);
        validator.initialize(annotation);

        assertThat(validator.isValid("1234-5678 9012-3456", null)).isTrue();
    }

    // ========== 辅助方法 ==========

    private BankCard createAnnotation(boolean nullable, boolean luhn) {
        return new BankCard() {
            @Override
            public String message() {
                return "银行卡号格式不正确";
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
            public boolean luhn() {
                return luhn;
            }

            @Override
            public Class<? extends BankCard> annotationType() {
                return BankCard.class;
            }
        };
    }
}
