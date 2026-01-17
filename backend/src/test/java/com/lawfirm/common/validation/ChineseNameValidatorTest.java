package com.lawfirm.common.validation;

import jakarta.validation.Payload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChineseNameValidator 单元测试
 *
 * 测试中文姓名验证功能
 */
@DisplayName("ChineseNameValidator 中文姓名验证器测试")
class ChineseNameValidatorTest {

    // ========== nullable = true (默认) ==========

    @ParameterizedTest
    @NullSource
    @DisplayName("nullable=true: null 应该通过验证")
    void isValid_shouldReturnTrueForNullWhenNullable(String value) {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid(value, null)).isTrue();
    }

    @Test
    @DisplayName("nullable=true: 空字符串应该通过验证")
    void isValid_shouldReturnTrueForEmptyWhenNullable() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid("", null)).isTrue();
    }

    // ========== nullable = false ==========

    @Test
    @DisplayName("nullable=false: null 应该不通过验证")
    void isValid_shouldReturnFalseForNullWhenNotNullable() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(false, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    @DisplayName("nullable=false: 空字符串应该不通过验证")
    void isValid_shouldReturnFalseForEmptyWhenNotNullable() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(false, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid("", null)).isFalse();
    }

    // ========== 有效的中文姓名 ==========

    @ParameterizedTest
    @ValueSource(strings = {
        "张三",      // 常见姓氏
        "欧阳修",    // 复姓
        "司马光",    // 复姓
        "买买提·艾力", // 少数民族名字
        "阿卜杜拉·穆罕默德", // 少数民族名字
        "王大锤",
        "李明华",
        "诸葛亮",
        "上官婉儿"
    })
    @DisplayName("应该接受有效的中文姓名")
    void isValid_shouldAcceptValidChineseNames(String name) {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid(name, null)).isTrue();
    }

    @Test
    @DisplayName("应该接受前后有空格的有效姓名")
    void isValid_shouldAcceptValidNameWithSurroundingSpaces() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid(" 张三 ", null)).isTrue();
    }

    // ========== 无效的姓名 ==========

    @ParameterizedTest
    @ValueSource(strings = {
        "张",          // 太短（1个字）
        "A",           // 纯英文字母
        "张三丰李",     // 太长（超过30个字）
        "123",         // 纯数字
        "张三123",     // 中英文数字混合
        "张三!",       // 包含特殊字符
        "张三。",       // 包含标点符号
        "Zhang San",   // 英文姓名
        "张三·",        // 以点号结尾
        "·张三"         // 以点号开头
    })
    @DisplayName("应该拒绝无效的姓名格式")
    void isValid_shouldRejectInvalidNames(String name) {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid(name, null)).isFalse();
    }

    // ========== 长度验证 ==========

    @Test
    @DisplayName("应该拒绝过短的姓名")
    void isValid_shouldRejectTooShortName() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid("张", null)).isFalse();
        assertThat(validator.isValid("李", null)).isFalse();
    }

    @Test
    @DisplayName("应该拒绝过长的姓名")
    void isValid_shouldRejectTooLongName() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        // 创建一个超过30个字的姓名
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 31; i++) {
            longName.append("张");
        }
        assertThat(validator.isValid(longName.toString(), null)).isFalse();
    }

    // ========== 自定义长度验证 ==========

    @Test
    @DisplayName("应该使用自定义的最小长度")
    void isValid_shouldUseCustomMinLength() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 3, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid("张三", null)).isFalse();   // 2个字
        assertThat(validator.isValid("张小三", null)).isTrue(); // 3个字
    }

    @Test
    @DisplayName("应该使用自定义的最大长度")
    void isValid_shouldUseCustomMaxLength() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 4);
        validator.initialize(annotation);

        assertThat(validator.isValid("张三李四王", null)).isFalse();  // 5个字 > max=4
        assertThat(validator.isValid("张三李四", null)).isTrue();    // 4个字 = max=4
    }

    // ========== 少数民族姓名（点号不计算在长度内） ==========

    @Test
    @DisplayName("少数民族名字中的点号不应计入长度")
    void isValid_shouldNotCountDotInLength() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        // "买买提·艾力" - 去掉点号是5个字，符合2-30的要求
        assertThat(validator.isValid("买买提·艾力", null)).isTrue();

        // "阿卜杜拉·穆罕默德" - 去掉点号是7个字
        assertThat(validator.isValid("阿卜杜拉·穆罕默德", null)).isTrue();
    }

    @Test
    @DisplayName("少数民族名字过短应该被拒绝")
    void isValid_shouldRejectShortMinorityName() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        // "阿·李" - 去掉点号是2个字，刚好符合
        assertThat(validator.isValid("阿·李", null)).isTrue();

        // "张·" - 去掉点号是1个字，不符合
        assertThat(validator.isValid("张·", null)).isFalse();
    }

    // ========== 边界情况 ==========

    @Test
    @DisplayName("应该只包含中文汉字和点号")
    void isValid_shouldOnlyAllowChineseAndDot() {
        ChineseNameValidator validator = new ChineseNameValidator();
        ChineseName annotation = createAnnotation(true, 2, 30);
        validator.initialize(annotation);

        assertThat(validator.isValid("张三·李四", null)).isTrue();   // 多个点号
        assertThat(validator.isValid("张三李四", null)).isTrue();    // 无点号
    }

    // ========== 辅助方法 ==========

    private ChineseName createAnnotation(boolean nullable, int min, int max) {
        return new ChineseName() {
            @Override
            public String message() {
                return "姓名格式不正确";
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
            public int min() {
                return min;
            }

            @Override
            public int max() {
                return max;
            }

            @Override
            public Class<? extends ChineseName> annotationType() {
                return ChineseName.class;
            }
        };
    }
}
