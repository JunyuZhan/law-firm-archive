package com.lawfirm.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MoneyUtils 单元测试
 *
 * 测试金额转换和格式化功能
 */
@DisplayName("MoneyUtils 金额工具类测试")
class MoneyUtilsTest {

    // ========== toChinese 测试 ==========

    @Test
    @DisplayName("null 应该返回空字符串")
    void toChinese_shouldReturnEmptyForNull() {
        assertThat(MoneyUtils.toChinese(null)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc", "invalid"})
    @DisplayName("无效输入应该返回空字符串")
    void toChinese_shouldReturnEmptyForInvalid(String input) {
        assertThat(MoneyUtils.toChinese(input)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "0, 零元整",
        "1, 壹元整",
        "10, 壹拾元整",
        "100, 壹佰元整",
        "1000, 壹仟元整",
        "10000, 壹万元整",
        "100000, 壹拾万元整",
        "10000000, 壹仟万元整",
        "100000000, 壹亿元整"
    })
    @DisplayName("应该正确转换整数金额")
    void toChinese_shouldConvertIntegers(String amount, String expected) {
        assertThat(MoneyUtils.toChinese(new BigDecimal(amount))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "100.50, 壹佰元伍角整",
        "100.05, 壹佰元零伍分",
        "100.55, 壹佰元伍角伍分",
        "123456.78, 壹拾贰万叁仟肆佰伍拾陆元柒角捌分",
        "10000.10, 壹万元壹角整"
    })
    @DisplayName("应该正确转换带小数的金额")
    void toChinese_shouldConvertDecimals(String amount, String expected) {
        assertThat(MoneyUtils.toChinese(new BigDecimal(amount))).isEqualTo(expected);
    }

    @Test
    @DisplayName("应该正确处理负数金额")
    void toChinese_shouldHandleNegative() {
        assertThat(MoneyUtils.toChinese(new BigDecimal("-100"))).isEqualTo("负壹佰元整");
        assertThat(MoneyUtils.toChinese(new BigDecimal("-100.50"))).isEqualTo("负壹佰元伍角整");
    }

    @ParameterizedTest
    @CsvSource({
        "100, 壹佰元整",
        "100.5, 壹佰元伍角整",
        "100.4, 壹佰元肆角整",
        "100.6, 壹佰元陆角整"
    })
    @DisplayName("应该四舍五入到分")
    void toChinese_shouldRoundHalfUp(String amount, String expected) {
        assertThat(MoneyUtils.toChinese(new BigDecimal(amount))).isEqualTo(expected);
    }

    @Test
    @DisplayName("应该支持不同输入类型")
    void toChinese_shouldSupportDifferentTypes() {
        assertThat(MoneyUtils.toChinese(100)).isEqualTo("壹佰元整");
        assertThat(MoneyUtils.toChinese(100L)).isEqualTo("壹佰元整");
        assertThat(MoneyUtils.toChinese(100.50)).isEqualTo("壹佰元伍角整");
        assertThat(MoneyUtils.toChinese("100")).isEqualTo("壹佰元整");
    }

    @Test
    @DisplayName("应该正确处理复杂的数字组合")
    void toChinese_shouldHandleComplexNumbers() {
        assertThat(MoneyUtils.toChinese(new BigDecimal("101"))).isEqualTo("壹佰零壹元整");
        assertThat(MoneyUtils.toChinese(new BigDecimal("110"))).isEqualTo("壹佰壹拾元整");
        assertThat(MoneyUtils.toChinese(new BigDecimal("1001"))).isEqualTo("壹仟零壹元整");
        assertThat(MoneyUtils.toChinese(new BigDecimal("1010"))).isEqualTo("壹仟零壹拾元整");
        assertThat(MoneyUtils.toChinese(new BigDecimal("1100"))).isEqualTo("壹仟壹佰元整");
    }

    // ========== toChineseYuan 测试 ==========

    @Test
    @DisplayName("toChineseYuan: null 应该返回空字符串")
    void toChineseYuan_shouldReturnEmptyForNull() {
        assertThat(MoneyUtils.toChineseYuan(null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "0, 零元整",
        "1, 壹元整",
        "100, 壹佰元整",
        "10000, 壹万元整",
        "123456, 壹拾贰万叁仟肆佰伍拾陆元整"
    })
    @DisplayName("toChineseYuan: 应该正确转换到元")
    void toChineseYuan_shouldConvertToYuan(String amount, String expected) {
        assertThat(MoneyUtils.toChineseYuan(new BigDecimal(amount))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "100.4, 壹佰元整",
        "100.5, 壹佰零壹元整",
        "100.49, 壹佰元整",
        "100.50, 壹佰零壹元整"
    })
    @DisplayName("toChineseYuan: 应该四舍五入到元")
    void toChineseYuan_shouldRoundHalfUp(String amount, String expected) {
        assertThat(MoneyUtils.toChineseYuan(new BigDecimal(amount))).isEqualTo(expected);
    }

    @Test
    @DisplayName("toChineseYuan: 应该正确处理负数")
    void toChineseYuan_shouldHandleNegative() {
        assertThat(MoneyUtils.toChineseYuan(new BigDecimal("-100"))).isEqualTo("负壹佰元整");
    }

    // ========== formatWithComma 测试 ==========

    @Test
    @DisplayName("formatWithComma: null 应该返回 0.00")
    void formatWithComma_shouldReturnZeroForNull() {
        assertThat(MoneyUtils.formatWithComma(null)).isEqualTo("0.00");
    }

    @Test
    @DisplayName("formatWithComma: 无效输入应该返回 0.00")
    void formatWithComma_shouldReturnZeroForInvalid() {
        assertThat(MoneyUtils.formatWithComma("")).isEqualTo("0.00");
        assertThat(MoneyUtils.formatWithComma("abc")).isEqualTo("0.00");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "0 | 0.00",
        "100 | 100.00",
        "1000 | 1,000.00",
        "10000 | 10,000.00",
        "100000 | 100,000.00",
        "1000000 | 1,000,000.00",
        "1234567.89 | 1,234,567.89"
    }, delimiter = '|')
    @DisplayName("formatWithComma: 应该正确添加千分位")
    void formatWithComma_shouldAddCommas(String amount, String expected) {
        assertThat(MoneyUtils.formatWithComma(new BigDecimal(amount))).isEqualTo(expected);
    }

    @Test
    @DisplayName("formatWithComma: 应该支持不同输入类型")
    void formatWithComma_shouldSupportDifferentTypes() {
        assertThat(MoneyUtils.formatWithComma(1000)).isEqualTo("1,000.00");
        assertThat(MoneyUtils.formatWithComma(1000L)).isEqualTo("1,000.00");
        assertThat(MoneyUtils.formatWithComma(1234.56)).isEqualTo("1,234.56");
        assertThat(MoneyUtils.formatWithComma("1000")).isEqualTo("1,000.00");
    }

    @Test
    @DisplayName("formatWithComma: 应该正确处理负数")
    void formatWithComma_shouldHandleNegative() {
        assertThat(MoneyUtils.formatWithComma(new BigDecimal("-1000"))).isEqualTo("-1,000.00");
    }
}
