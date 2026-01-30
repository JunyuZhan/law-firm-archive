package com.lawfirm.common.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 字段变更记录工具类
 *
 * <p>用于对比两个对象的字段变更，生成变更记录
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public final class FieldChangeUtils {

  /** 字符串最大长度. */
  private static final int MAX_STRING_LENGTH = 500;

  private FieldChangeUtils() {
    // 工具类，禁止实例化
  }

  /**
   * 对比两个对象的所有字段变更
   *
   * @param oldObj 旧对象
   * @param newObj 新对象
   * @return 变更记录列表
   */
  public static List<FieldChange> compare(final Object oldObj, final Object newObj) {
    return compare(oldObj, newObj, null);
  }

  /**
   * 对比两个对象的指定字段变更
   *
   * @param oldObj 旧对象
   * @param newObj 新对象
   * @param fieldNames 要对比的字段名（null表示对比所有字段）
   * @return 变更记录列表
   */
  public static List<FieldChange> compare(
      final Object oldObj, final Object newObj, final Set<String> fieldNames) {
    List<FieldChange> changes = new ArrayList<>();

    if (oldObj == null && newObj == null) {
      return changes;
    }

    // 处理新增情况
    if (oldObj == null) {
      return getFieldsAsChanges(newObj, fieldNames, true);
    }

    // 处理删除情况
    if (newObj == null) {
      return getFieldsAsChanges(oldObj, fieldNames, false);
    }

    // 确保是同一类型
    if (!oldObj.getClass().equals(newObj.getClass())) {
      log.warn("对比的两个对象类型不一致: {} vs {}", oldObj.getClass(), newObj.getClass());
      return changes;
    }

    // 获取所有字段（包括父类）
    List<Field> fields = getAllFields(oldObj.getClass());

    for (Field field : fields) {
      // 过滤字段
      if (fieldNames != null && !fieldNames.contains(field.getName())) {
        continue;
      }

      // 跳过静态字段和transient字段
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
          || java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
        continue;
      }

      field.setAccessible(true);

      try {
        Object oldValue = field.get(oldObj);
        Object newValue = field.get(newObj);

        if (!Objects.equals(oldValue, newValue)) {
          changes.add(new FieldChange(field.getName(), toString(oldValue), toString(newValue)));
        }
      } catch (IllegalAccessException e) {
        log.warn("无法访问字段: {}", field.getName(), e);
      }
    }

    return changes;
  }

  /**
   * 将变更记录格式化为字符串
   *
   * @param changes 变更记录列表
   * @return 格式化后的字符串
   */
  public static String formatChanges(final List<FieldChange> changes) {
    if (changes == null || changes.isEmpty()) {
      return "无变更";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < changes.size(); i++) {
      FieldChange change = changes.get(i);
      sb.append(change.getFieldName())
          .append(": [")
          .append(change.getOldValue())
          .append("] -> [")
          .append(change.getNewValue())
          .append("]");
      if (i < changes.size() - 1) {
        sb.append("; ");
      }
    }
    return sb.toString();
  }

  /**
   * 获取类的所有字段（包括父类）
   *
   * @param clazz 类
   * @return 字段列表
   */
  private static List<Field> getAllFields(final Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    Class<?> currentClass = clazz;
    while (currentClass != null && currentClass != Object.class) {
      fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
      currentClass = currentClass.getSuperclass();
    }
    return fields;
  }

  /**
   * 将对象的字段转换为变更记录（用于新增/删除场景）
   *
   * @param obj 对象
   * @param fieldNames 字段名集合
   * @param isNew 是否新增
   * @return 变更记录列表
   */
  private static List<FieldChange> getFieldsAsChanges(
      final Object obj, final Set<String> fieldNames, final boolean isNew) {
    List<FieldChange> changes = new ArrayList<>();
    List<Field> fields = getAllFields(obj.getClass());

    for (Field field : fields) {
      if (fieldNames != null && !fieldNames.contains(field.getName())) {
        continue;
      }

      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
          || java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
        continue;
      }

      field.setAccessible(true);

      try {
        Object value = field.get(obj);
        if (value != null) {
          if (isNew) {
            changes.add(new FieldChange(field.getName(), null, toString(value)));
          } else {
            changes.add(new FieldChange(field.getName(), toString(value), null));
          }
        }
      } catch (IllegalAccessException e) {
        log.warn("无法访问字段: {}", field.getName(), e);
      }
    }

    return changes;
  }

  /**
   * 安全地将对象转换为字符串
   *
   * @param value 值
   * @return 字符串
   */
  private static String toString(final Object value) {
    if (value == null) {
      return null;
    }
    String str = value.toString();
    // 限制长度，避免日志过长
    if (str.length() > MAX_STRING_LENGTH) {
      return str.substring(0, MAX_STRING_LENGTH) + "...";
    }
    return str;
  }

  /** 字段变更记录 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FieldChange {
    /** 字段名 */
    private String fieldName;

    /** 旧值 */
    private String oldValue;

    /** 新值 */
    private String newValue;
  }
}
