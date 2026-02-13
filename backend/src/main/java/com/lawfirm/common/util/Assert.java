package com.lawfirm.common.util;

import com.lawfirm.common.exception.BusinessException;
import java.util.Collection;
import java.util.Map;

/**
 * 断言工具类
 *
 * <p>功能： - 参数校验 - 条件断言 - 抛出业务异常
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
public final class Assert {

  private Assert() {
    // 工具类，禁止实例化
  }

  /**
   * 断言对象不为null
   *
   * @param object 对象
   * @param message 错误消息
   * @throws BusinessException 如果对象为null
   */
  public static void notNull(final Object object, final String message) {
    if (object == null) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言对象为null
   *
   * @param object 对象
   * @param message 错误消息
   * @throws BusinessException 如果对象不为null
   */
  public static void isNull(final Object object, final String message) {
    if (object != null) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言字符串不为空（非null且非空字符串）
   *
   * @param str 字符串
   * @param message 错误消息
   * @throws BusinessException 如果字符串为空
   */
  public static void notEmpty(final String str, final String message) {
    if (str == null || str.isEmpty()) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言字符串不为空白（非null且非空白字符串）
   *
   * @param str 字符串
   * @param message 错误消息
   * @throws BusinessException 如果字符串为空白
   */
  public static void notBlank(final String str, final String message) {
    if (str == null || str.trim().isEmpty()) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言条件为true
   *
   * @param expression 条件表达式
   * @param message 错误消息
   * @throws BusinessException 如果条件为false
   */
  public static void isTrue(final boolean expression, final String message) {
    if (!expression) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言条件为false
   *
   * @param expression 条件表达式
   * @param message 错误消息
   * @throws BusinessException 如果条件为true
   */
  public static void isFalse(final boolean expression, final String message) {
    if (expression) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言集合不为空
   *
   * @param collection 集合
   * @param message 错误消息
   * @throws BusinessException 如果集合为空
   */
  public static void notEmpty(final Collection<?> collection, final String message) {
    if (collection == null || collection.isEmpty()) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言Map不为空
   *
   * @param map Map
   * @param message 错误消息
   * @throws BusinessException 如果Map为空
   */
  public static void notEmpty(final Map<?, ?> map, final String message) {
    if (map == null || map.isEmpty()) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言数组不为空
   *
   * @param array 数组
   * @param message 错误消息
   * @throws BusinessException 如果数组为空
   */
  public static void notEmpty(final Object[] array, final String message) {
    if (array == null || array.length == 0) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言两个对象相等
   *
   * @param obj1 对象1
   * @param obj2 对象2
   * @param message 错误消息
   * @throws BusinessException 如果两个对象不相等
   */
  public static void equals(final Object obj1, final Object obj2, final String message) {
    if (obj1 == null && obj2 == null) {
      return;
    }
    if (obj1 == null || !obj1.equals(obj2)) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言两个对象不相等
   *
   * @param obj1 对象1
   * @param obj2 对象2
   * @param message 错误消息
   * @throws BusinessException 如果两个对象相等
   */
  public static void notEquals(final Object obj1, final Object obj2, final String message) {
    if (obj1 == null && obj2 == null) {
      throw new BusinessException(message);
    }
    if (obj1 != null && obj1.equals(obj2)) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言数值在指定范围内
   *
   * @param value 数值
   * @param min 最小值（包含）
   * @param max 最大值（包含）
   * @param message 错误消息
   * @throws BusinessException 如果数值不在范围内
   */
  public static void inRange(
      final long value, final long min, final long max, final String message) {
    if (value < min || value > max) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言数值大于指定值
   *
   * @param value 数值
   * @param minValue 最小值（不包含）
   * @param message 错误消息
   * @throws BusinessException 如果数值不大于最小值
   */
  public static void greaterThan(final long value, final long minValue, final String message) {
    if (value <= minValue) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言数值大于等于指定值
   *
   * @param value 数值
   * @param minValue 最小值（包含）
   * @param message 错误消息
   * @throws BusinessException 如果数值小于最小值
   */
  public static void greaterThanOrEqual(
      final long value, final long minValue, final String message) {
    if (value < minValue) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言数值小于指定值
   *
   * @param value 数值
   * @param maxValue 最大值（不包含）
   * @param message 错误消息
   * @throws BusinessException 如果数值不小于最大值
   */
  public static void lessThan(final long value, final long maxValue, final String message) {
    if (value >= maxValue) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言数值小于等于指定值
   *
   * @param value 数值
   * @param maxValue 最大值（包含）
   * @param message 错误消息
   * @throws BusinessException 如果数值大于最大值
   */
  public static void lessThanOrEqual(final long value, final long maxValue, final String message) {
    if (value > maxValue) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言字符串长度在指定范围内
   *
   * @param str 字符串
   * @param min 最小长度（包含）
   * @param max 最大长度（包含）
   * @param message 错误消息
   * @throws BusinessException 如果字符串长度不在范围内
   */
  public static void lengthInRange(
      final String str, final int min, final int max, final String message) {
    if (str == null) {
      if (min > 0) {
        throw new BusinessException(message);
      }
      return;
    }
    int length = str.length();
    if (length < min || length > max) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言对象是指定类型的实例
   *
   * @param type 期望类型
   * @param obj 对象
   * @param message 错误消息
   * @throws BusinessException 如果对象不是指定类型的实例
   */
  public static void isInstanceOf(final Class<?> type, final Object obj, final String message) {
    notNull(type, "Type must not be null");
    if (!type.isInstance(obj)) {
      throw new BusinessException(message);
    }
  }

  /**
   * 断言字符串匹配正则表达式
   *
   * @param str 字符串
   * @param regex 正则表达式
   * @param message 错误消息
   * @throws BusinessException 如果字符串不匹配正则表达式
   */
  public static void matches(final String str, final String regex, final String message) {
    if (str == null || !str.matches(regex)) {
      throw new BusinessException(message);
    }
  }

  /**
   * 直接抛出业务异常
   *
   * @param message 错误消息
   * @throws BusinessException 业务异常
   */
  public static void fail(final String message) {
    throw new BusinessException(message);
  }
}
