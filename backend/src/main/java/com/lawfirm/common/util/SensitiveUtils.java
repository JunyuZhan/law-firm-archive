package com.lawfirm.common.util;

import java.math.BigDecimal;

/**
 * 敏感数据脱敏工具类
 * 
 * 功能：
 * - 手机号脱敏
 * - 身份证脱敏
 * - 姓名脱敏
 * - 邮箱脱敏
 * - 银行卡脱敏
 * - 地址脱敏
 * - 通用脱敏
 * 
 * @author system
 * @since 2026-01-10
 */
public class SensitiveUtils {

    private static final char MASK_CHAR = '*';

    private SensitiveUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 手机号脱敏
     * 138****1234
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证脱敏
     * 110101****0011
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 姓名脱敏
     * 张** 或 张*
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + repeat(MASK_CHAR, name.length() - 1);
    }

    /**
     * 邮箱脱敏
     * z****@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, 1);
        String suffix = email.substring(atIndex);
        int maskLength = Math.min(4, atIndex - 1);
        return prefix + repeat(MASK_CHAR, maskLength) + suffix;
    }

    /**
     * 银行卡脱敏
     * 6222****1234
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + repeat(MASK_CHAR, bankCard.length() - 8) + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 地址脱敏
     * 保留前6个字符和后4个字符
     *
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 10) {
            return address;
        }
        return address.substring(0, 6) + "****" + address.substring(address.length() - 4);
    }

    /**
     * 案号脱敏
     * (2024)京01民初****号
     *
     * @param caseNumber 案号
     * @return 脱敏后的案号
     */
    public static String maskCaseNumber(String caseNumber) {
        if (caseNumber == null || caseNumber.length() < 10) {
            return caseNumber;
        }
        // 保留前10个字符和最后1个字符
        return caseNumber.substring(0, 10) + "****" + caseNumber.substring(caseNumber.length() - 1);
    }

    /**
     * 金额脱敏
     * 显示为 ****
     *
     * @param amount 金额
     * @return 脱敏后的金额
     */
    public static String maskAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return "****";
    }

    /**
     * 合同编号脱敏
     * HT****0001
     *
     * @param contractNo 合同编号
     * @return 脱敏后的合同编号
     */
    public static String maskContractNo(String contractNo) {
        if (contractNo == null || contractNo.length() < 8) {
            return contractNo;
        }
        return contractNo.substring(0, 2) + "****" + contractNo.substring(contractNo.length() - 4);
    }

    /**
     * 律师证号脱敏
     * 保留前4位和后4位
     *
     * @param licenseNo 律师证号
     * @return 脱敏后的律师证号
     */
    public static String maskLawyerLicense(String licenseNo) {
        if (licenseNo == null || licenseNo.length() < 8) {
            return licenseNo;
        }
        return licenseNo.substring(0, 4) + "****" + licenseNo.substring(licenseNo.length() - 4);
    }

    /**
     * 通用脱敏
     * 保留前prefixLength位和后suffixLength位，中间用maskChar填充
     *
     * @param text         文本
     * @param prefixLength 前缀保留长度
     * @param suffixLength 后缀保留长度
     * @param maskChar     脱敏字符
     * @return 脱敏后的文本
     */
    public static String mask(String text, int prefixLength, int suffixLength, char maskChar) {
        if (text == null) {
            return null;
        }
        int length = text.length();
        if (length <= prefixLength + suffixLength) {
            return text;
        }
        String prefix = text.substring(0, prefixLength);
        String suffix = text.substring(length - suffixLength);
        int maskLength = length - prefixLength - suffixLength;
        return prefix + repeat(maskChar, maskLength) + suffix;
    }

    /**
     * 通用脱敏（使用默认脱敏字符*）
     *
     * @param text         文本
     * @param prefixLength 前缀保留长度
     * @param suffixLength 后缀保留长度
     * @return 脱敏后的文本
     */
    public static String mask(String text, int prefixLength, int suffixLength) {
        return mask(text, prefixLength, suffixLength, MASK_CHAR);
    }

    /**
     * 完全脱敏
     * 全部替换为*
     *
     * @param text 文本
     * @return 脱敏后的文本
     */
    public static String maskAll(String text) {
        if (text == null) {
            return null;
        }
        return repeat(MASK_CHAR, text.length());
    }

    /**
     * 固定长度脱敏
     * 替换为固定长度的*
     *
     * @param text       文本
     * @param maskLength 脱敏后的长度
     * @return 脱敏后的文本
     */
    public static String maskFixed(String text, int maskLength) {
        if (text == null) {
            return null;
        }
        return repeat(MASK_CHAR, maskLength);
    }

    /**
     * 重复字符
     */
    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}

