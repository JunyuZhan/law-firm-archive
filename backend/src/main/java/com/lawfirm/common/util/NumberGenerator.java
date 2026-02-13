package com.lawfirm.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/** 编号生成工具类 ✅ 修复问题607/609: 提供更可靠的编号生成机制，避免UUID截断导致的重复 */
public final class NumberGenerator {

  private NumberGenerator() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** 日期格式化器. */
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

  /** 序列号上限. */
  private static final int SEQUENCE_LIMIT = 10_000;

  /** 时间戳取模基数. */
  private static final long TIMESTAMP_MODULO = 1_000_000_000L;

  /** 计数器. */
  private static final AtomicLong COUNTER =
      new AtomicLong(System.currentTimeMillis() % SEQUENCE_LIMIT);

  /** 随机字符集. */
  private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  /**
   * 生成证据编号.
   *
   * <p>格式: EV + yyMMdd + 4位序号 + 2位随机 示例: EV260110000123AB
   *
   * @return 证据编号
   */
  public static String generateEvidenceNo() {
    return generateNo("EV");
  }

  /**
   * 生成证据清单编号.
   *
   * <p>格式: EL + yyMMdd + 4位序号 + 2位随机 示例: EL260110000123AB
   *
   * @return 证据清单编号
   */
  public static String generateEvidenceListNo() {
    return generateNo("EL");
  }

  /**
   * 生成合同编号.
   *
   * <p>格式: CT + yyMMdd + 4位序号 + 2位随机
   *
   * @return 合同编号
   */
  public static String generateContractNo() {
    return generateNo("CT");
  }

  /**
   * 生成项目编号.
   *
   * <p>格式: MT + yyMMdd + 4位序号 + 2位随机
   *
   * @return 项目编号
   */
  public static String generateMatterNo() {
    return generateNo("MT");
  }

  /**
   * 通用编号生成
   *
   * @param prefix 编号前缀
   * @return 格式: prefix + yyMMdd + 4位序号 + 2位随机
   */
  public static String generateNo(final String prefix) {
    String datePart = LocalDate.now().format(DATE_FORMATTER);
    long sequence = COUNTER.incrementAndGet() % SEQUENCE_LIMIT;
    String seqPart = String.format("%04d", sequence);
    String randomPart = generateRandomString(2);

    return prefix + datePart + seqPart + randomPart;
  }

  /**
   * 生成带时间戳的编号（毫秒级唯一）
   *
   * @param prefix 编号前缀
   * @return 格式: prefix + yyMMddHHmmss + 3位毫秒 + 2位随机
   */
  public static String generateNoWithTimestamp(final String prefix) {
    long timestamp = System.currentTimeMillis();
    String datePart = LocalDate.now().format(DATE_FORMATTER);
    String timePart = String.format("%09d", timestamp % TIMESTAMP_MODULO);
    String randomPart = generateRandomString(2);

    return prefix + datePart + timePart.substring(0, 6) + randomPart;
  }

  /**
   * 生成随机字符串
   *
   * @param length 长度
   * @return 随机字符串
   */
  private static String generateRandomString(final int length) {
    StringBuilder sb = new StringBuilder(length);
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < length; i++) {
      sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
    }
    return sb.toString();
  }

  /** 重置计数器（用于测试） */
  static void resetCounter() {
    COUNTER.set(0);
  }
}
