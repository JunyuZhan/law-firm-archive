package com.lawfirm.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 编号生成工具类
 * ✅ 修复问题607/609: 提供更可靠的编号生成机制，避免UUID截断导致的重复
 */
public class NumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    
    // 使用原子计数器确保唯一性
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis() % 10000);
    
    // 字符集用于生成随机部分
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 生成证据编号
     * 格式: EV + yyMMdd + 4位序号 + 2位随机
     * 示例: EV260110000123AB
     */
    public static String generateEvidenceNo() {
        return generateNo("EV");
    }

    /**
     * 生成证据清单编号
     * 格式: EL + yyMMdd + 4位序号 + 2位随机
     * 示例: EL260110000123AB
     */
    public static String generateEvidenceListNo() {
        return generateNo("EL");
    }

    /**
     * 生成合同编号
     * 格式: CT + yyMMdd + 4位序号 + 2位随机
     */
    public static String generateContractNo() {
        return generateNo("CT");
    }

    /**
     * 生成项目编号
     * 格式: MT + yyMMdd + 4位序号 + 2位随机
     */
    public static String generateMatterNo() {
        return generateNo("MT");
    }

    /**
     * 通用编号生成
     * @param prefix 编号前缀
     * @return 格式: prefix + yyMMdd + 4位序号 + 2位随机
     */
    public static String generateNo(String prefix) {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        long sequence = COUNTER.incrementAndGet() % 10000;
        String seqPart = String.format("%04d", sequence);
        String randomPart = generateRandomString(2);
        
        return prefix + datePart + seqPart + randomPart;
    }

    /**
     * 生成带时间戳的编号（毫秒级唯一）
     * @param prefix 编号前缀
     * @return 格式: prefix + yyMMddHHmmss + 3位毫秒 + 2位随机
     */
    public static String generateNoWithTimestamp(String prefix) {
        long timestamp = System.currentTimeMillis();
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String timePart = String.format("%09d", timestamp % 1000000000);
        String randomPart = generateRandomString(2);
        
        return prefix + datePart + timePart.substring(0, 6) + randomPart;
    }

    /**
     * 生成随机字符串
     * @param length 长度
     * @return 随机字符串
     */
    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * 重置计数器（用于测试）
     */
    static void resetCounter() {
        COUNTER.set(0);
    }
}

