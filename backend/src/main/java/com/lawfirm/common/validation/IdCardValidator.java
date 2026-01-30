package com.lawfirm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 身份证号校验器
 *
 * <p>支持18位身份证号，包含： - 格式验证（6位地区码 + 8位生日 + 3位顺序码 + 1位校验码） - 校验位验证（使用ISO 7064:1983, MOD 11-2算法）
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
public class IdCardValidator implements ConstraintValidator<IdCard, String> {

  /** 18位身份证正则 */
  private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

  /** 加权因子 */
  private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

  /** 校验码对照表 */
  private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

  /** 身份证前17位数字长度 */
  private static final int ID_CARD_PREFIX_LENGTH = 17;

  /** 校验码位置（第18位） */
  private static final int CHECK_CODE_POSITION = 17;

  /** 校验码计算模数 */
  private static final int CHECK_CODE_MODULUS = 11;

  /** 是否允许为空 */
  private boolean nullable;

  /** 是否严格模式（验证校验位） */
  private boolean strict;

  @Override
  public void initialize(final IdCard constraintAnnotation) {
    this.nullable = constraintAnnotation.nullable();
    this.strict = constraintAnnotation.strict();
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    // 允许为空
    if (!StringUtils.hasText(value)) {
      return nullable;
    }

    String idCard = value.trim().toUpperCase();

    // 基本格式验证
    if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
      return false;
    }

    // 严格模式：验证校验位
    if (strict) {
      return validateCheckCode(idCard);
    }

    return true;
  }

  /**
   * 验证校验位
   *
   * @param idCard 身份证号
   * @return 是否有效
   */
  private boolean validateCheckCode(final String idCard) {
    int sum = 0;
    for (int i = 0; i < ID_CARD_PREFIX_LENGTH; i++) {
      sum += (idCard.charAt(i) - '0') * WEIGHTS[i];
    }
    int mod = sum % CHECK_CODE_MODULUS;
    char expectedCheckCode = CHECK_CODES[mod];
    char actualCheckCode = idCard.charAt(CHECK_CODE_POSITION);

    return expectedCheckCode == actualCheckCode;
  }
}
