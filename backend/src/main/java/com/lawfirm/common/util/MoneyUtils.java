package com.lawfirm.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具类
 * 提供金额转中文大写等功能
 */
public class MoneyUtils {

    private static final String[] CN_UPPER_NUMBER = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] CN_UPPER_UNIT = {"", "拾", "佰", "仟"};
    private static final String[] CN_GROUP_UNIT = {"", "万", "亿", "兆"};
    private static final String CN_FULL = "整";
    private static final String CN_NEGATIVE = "负";
    private static final String CN_YUAN = "元";
    private static final String CN_JIAO = "角";
    private static final String CN_FEN = "分";
    private static final String CN_ZERO = "零";

    /**
     * 将金额转换为中文大写
     * 例如：10000.50 -> 壹万元伍角整
     *       123456.78 -> 壹拾贰万叁仟肆佰伍拾陆元柒角捌分
     *
     * @param amount 金额（支持 BigDecimal、Double、Long、Integer、String）
     * @return 中文大写金额
     */
    public static String toChinese(Object amount) {
        if (amount == null) {
            return "";
        }

        BigDecimal money;
        if (amount instanceof BigDecimal) {
            money = (BigDecimal) amount;
        } else if (amount instanceof Double) {
            money = BigDecimal.valueOf((Double) amount);
        } else if (amount instanceof Long) {
            money = BigDecimal.valueOf((Long) amount);
        } else if (amount instanceof Integer) {
            money = BigDecimal.valueOf((Integer) amount);
        } else {
            try {
                money = new BigDecimal(amount.toString());
            } catch (NumberFormatException e) {
                return "";
            }
        }

        // 四舍五入到分
        money = money.setScale(2, RoundingMode.HALF_UP);

        StringBuilder result = new StringBuilder();

        // 处理负数
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            result.append(CN_NEGATIVE);
            money = money.abs();
        }

        // 分离整数部分和小数部分
        long integerPart = money.longValue();
        int decimalPart = money.subtract(BigDecimal.valueOf(integerPart))
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        // 处理整数部分
        if (integerPart == 0) {
            result.append(CN_UPPER_NUMBER[0]);
        } else {
            result.append(convertIntegerPart(integerPart));
        }

        result.append(CN_YUAN);

        // 处理小数部分
        int jiao = decimalPart / 10;
        int fen = decimalPart % 10;

        if (jiao == 0 && fen == 0) {
            result.append(CN_FULL);
        } else {
            if (jiao == 0) {
                result.append(CN_ZERO);
            } else {
                result.append(CN_UPPER_NUMBER[jiao]).append(CN_JIAO);
            }
            if (fen == 0) {
                result.append(CN_FULL);
            } else {
                result.append(CN_UPPER_NUMBER[fen]).append(CN_FEN);
            }
        }

        return result.toString();
    }

    /**
     * 转换整数部分
     */
    private static String convertIntegerPart(long number) {
        if (number == 0) {
            return CN_UPPER_NUMBER[0];
        }

        StringBuilder result = new StringBuilder();
        int groupIndex = 0;
        boolean needZero = false;

        while (number > 0) {
            int group = (int) (number % 10000);
            number /= 10000;

            String groupStr = convertGroup(group, needZero && group < 1000);
            
            if (group > 0) {
                result.insert(0, groupStr + CN_GROUP_UNIT[groupIndex]);
                needZero = (group < 1000);
            } else if (result.length() > 0) {
                needZero = true;
            }

            groupIndex++;
        }

        return result.toString();
    }

    /**
     * 转换四位数组（0-9999）
     */
    private static String convertGroup(int number, boolean prefixZero) {
        if (number == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        
        if (prefixZero) {
            result.append(CN_ZERO);
        }

        int[] digits = new int[4];
        for (int i = 0; i < 4; i++) {
            digits[i] = number % 10;
            number /= 10;
        }

        boolean hasValue = false;
        for (int i = 3; i >= 0; i--) {
            if (digits[i] != 0) {
                result.append(CN_UPPER_NUMBER[digits[i]]).append(CN_UPPER_UNIT[i]);
                hasValue = true;
            } else if (hasValue && i > 0 && (digits[i - 1] != 0 || digits[i] == 0 && needsZero(digits, i))) {
                // 中间的零需要读出
                if (!result.toString().endsWith(CN_ZERO)) {
                    result.append(CN_ZERO);
                }
            }
        }

        // 去除末尾的零
        String str = result.toString();
        while (str.endsWith(CN_ZERO)) {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    /**
     * 判断是否需要添加零
     */
    private static boolean needsZero(int[] digits, int currentIndex) {
        for (int i = currentIndex - 1; i >= 0; i--) {
            if (digits[i] != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化金额显示（带千分位）
     * 例如：1234567.89 -> 1,234,567.89
     *
     * @param amount 金额
     * @return 格式化后的字符串
     */
    public static String formatWithComma(Object amount) {
        if (amount == null) {
            return "0.00";
        }

        BigDecimal money;
        if (amount instanceof BigDecimal) {
            money = (BigDecimal) amount;
        } else if (amount instanceof Double) {
            money = BigDecimal.valueOf((Double) amount);
        } else if (amount instanceof Long) {
            money = BigDecimal.valueOf((Long) amount);
        } else if (amount instanceof Integer) {
            money = BigDecimal.valueOf((Integer) amount);
        } else {
            try {
                money = new BigDecimal(amount.toString());
            } catch (NumberFormatException e) {
                return "0.00";
            }
        }

        return String.format("%,.2f", money);
    }

    /**
     * 简化的金额大写（不带角分）
     * 例如：10000 -> 壹万元整
     *
     * @param amount 金额
     * @return 中文大写（精确到元）
     */
    public static String toChineseYuan(Object amount) {
        if (amount == null) {
            return "";
        }

        BigDecimal money;
        if (amount instanceof BigDecimal) {
            money = (BigDecimal) amount;
        } else if (amount instanceof Double) {
            money = BigDecimal.valueOf((Double) amount);
        } else if (amount instanceof Long) {
            money = BigDecimal.valueOf((Long) amount);
        } else if (amount instanceof Integer) {
            money = BigDecimal.valueOf((Integer) amount);
        } else {
            try {
                money = new BigDecimal(amount.toString());
            } catch (NumberFormatException e) {
                return "";
            }
        }

        // 四舍五入到元
        money = money.setScale(0, RoundingMode.HALF_UP);

        StringBuilder result = new StringBuilder();

        // 处理负数
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            result.append(CN_NEGATIVE);
            money = money.abs();
        }

        long integerPart = money.longValue();

        if (integerPart == 0) {
            result.append(CN_UPPER_NUMBER[0]);
        } else {
            result.append(convertIntegerPart(integerPart));
        }

        result.append(CN_YUAN).append(CN_FULL);

        return result.toString();
    }
}

