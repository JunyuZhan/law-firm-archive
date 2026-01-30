package com.lawfirm.application.document.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 脱敏映射数据传输对象 记录原始值与脱敏值的对应关系，用于还原. */
@Data
public class MaskingMappingDTO {

  /** 映射条目列表. */
  private List<MappingEntry> mappings = new ArrayList<>();

  /**
   * 添加映射.
   *
   * @param fieldName 字段名称
   * @param originalValue 原始值
   * @param maskedValue 脱敏后的值
   */
  public void addMapping(
      final String fieldName, final String originalValue, final String maskedValue) {
    // 避免重复添加
    for (MappingEntry entry : mappings) {
      if (entry.getOriginalValue().equals(originalValue)
          && entry.getMaskedValue().equals(maskedValue)) {
        return;
      }
    }
    mappings.add(new MappingEntry(fieldName, originalValue, maskedValue));
  }

  /**
   * 合并另一个映射.
   *
   * @param other 另一个脱敏映射DTO
   */
  public void merge(final MaskingMappingDTO other) {
    if (other != null && other.getMappings() != null) {
      for (MappingEntry entry : other.getMappings()) {
        addMapping(entry.getFieldName(), entry.getOriginalValue(), entry.getMaskedValue());
      }
    }
  }

  /**
   * 判断是否有映射.
   *
   * @return 是否为空
   */
  public boolean isEmpty() {
    return mappings == null || mappings.isEmpty();
  }

  /** 映射条目. */
  @Data
  public static class MappingEntry {
    /** 字段名称（描述性）. */
    private String fieldName;

    /** 原始值. */
    private String originalValue;

    /** 脱敏后的值. */
    private String maskedValue;

    /** 默认构造函数. */
    public MappingEntry() {}

    /**
     * 构造函数.
     *
     * @param fieldName 字段名称
     * @param originalValue 原始值
     * @param maskedValue 脱敏后的值
     */
    public MappingEntry(
        final String fieldName, final String originalValue, final String maskedValue) {
      this.fieldName = fieldName;
      this.originalValue = originalValue;
      this.maskedValue = maskedValue;
    }
  }
}
