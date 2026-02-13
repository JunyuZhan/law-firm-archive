package com.lawfirm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 中文姓名校验器
 *
 * <p>支持： - 中文汉字 - 少数民族名字中的点号（·） - 繁体字
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
public class ChineseNameValidator implements ConstraintValidator<ChineseName, String> {

  /** 中文姓名正则 支持汉字和中间的点号（用于少数民族名字，如：买买提·艾力） */
  private static final Pattern CHINESE_NAME_PATTERN =
      Pattern.compile("^[\\u4e00-\\u9fa5]+(·[\\u4e00-\\u9fa5]+)*$");

  /** 是否允许为空 */
  private boolean nullable;

  /** 最小长度 */
  private int min;

  /** 最大长度 */
  private int max;

  @Override
  public void initialize(final ChineseName constraintAnnotation) {
    this.nullable = constraintAnnotation.nullable();
    this.min = constraintAnnotation.min();
    this.max = constraintAnnotation.max();
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    // 允许为空
    if (!StringUtils.hasText(value)) {
      return nullable;
    }

    String name = value.trim();

    // 长度校验（不计算点号）
    String nameWithoutDot = name.replace("·", "");
    if (nameWithoutDot.length() < min || nameWithoutDot.length() > max) {
      return false;
    }

    // 格式校验
    return CHINESE_NAME_PATTERN.matcher(name).matches();
  }
}
