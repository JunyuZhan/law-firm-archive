package com.lawfirm.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NumberGenerator 单元测试
 *
 * <p>测试编号生成功能
 */
@DisplayName("NumberGenerator 编号生成工具测试")
class NumberGeneratorTest {

  @AfterEach
  void tearDown() {
    // 重置计数器以避免测试间干扰
    NumberGenerator.resetCounter();
  }

  // ========== 证据编号测试 ==========

  @Test
  @DisplayName("生成证据编号应该以 EV 开头")
  void generateEvidenceNo_shouldStartWithEV() {
    String result = NumberGenerator.generateEvidenceNo();
    assertThat(result).startsWith("EV");
    assertThat(result).hasSize(14); // EV + yyMMdd(6) + 序号(4) + 随机(2)
  }

  @Test
  @DisplayName("多次生成证据编号应该不同")
  void generateEvidenceNo_shouldBeUnique() {
    String no1 = NumberGenerator.generateEvidenceNo();
    String no2 = NumberGenerator.generateEvidenceNo();
    assertThat(no1).isNotEqualTo(no2);
  }

  // ========== 证据清单编号测试 ==========

  @Test
  @DisplayName("生成证据清单编号应该以 EL 开头")
  void generateEvidenceListNo_shouldStartWithEL() {
    String result = NumberGenerator.generateEvidenceListNo();
    assertThat(result).startsWith("EL");
    assertThat(result).hasSize(14);
  }

  @Test
  @DisplayName("多次生成证据清单编号应该不同")
  void generateEvidenceListNo_shouldBeUnique() {
    String no1 = NumberGenerator.generateEvidenceListNo();
    String no2 = NumberGenerator.generateEvidenceListNo();
    assertThat(no1).isNotEqualTo(no2);
  }

  // ========== 合同编号测试 ==========

  @Test
  @DisplayName("生成合同编号应该以 CT 开头")
  void generateContractNo_shouldStartWithCT() {
    String result = NumberGenerator.generateContractNo();
    assertThat(result).startsWith("CT");
    assertThat(result).hasSize(14);
  }

  @Test
  @DisplayName("多次生成合同编号应该不同")
  void generateContractNo_shouldBeUnique() {
    String no1 = NumberGenerator.generateContractNo();
    String no2 = NumberGenerator.generateContractNo();
    assertThat(no1).isNotEqualTo(no2);
  }

  // ========== 项目编号测试 ==========

  @Test
  @DisplayName("生成项目编号应该以 MT 开头")
  void generateMatterNo_shouldStartWithMT() {
    String result = NumberGenerator.generateMatterNo();
    assertThat(result).startsWith("MT");
    assertThat(result).hasSize(14);
  }

  @Test
  @DisplayName("多次生成项目编号应该不同")
  void generateMatterNo_shouldBeUnique() {
    String no1 = NumberGenerator.generateMatterNo();
    String no2 = NumberGenerator.generateMatterNo();
    assertThat(no1).isNotEqualTo(no2);
  }

  // ========== 通用编号生成测试 ==========

  @Test
  @DisplayName("自定义前缀的编号应该以指定前缀开头")
  void generateNo_shouldUseCustomPrefix() {
    String result = NumberGenerator.generateNo("XY");
    assertThat(result).startsWith("XY");
    assertThat(result).hasSize(14);
  }

  @Test
  @DisplayName("自定义前缀的编号应该唯一")
  void generateNo_shouldBeUnique() {
    String no1 = NumberGenerator.generateNo("AB");
    String no2 = NumberGenerator.generateNo("AB");
    assertThat(no1).isNotEqualTo(no2);
  }

  @Test
  @DisplayName("不同前缀的编号应该不同")
  void generateNo_differentPrefixesShouldDiffer() {
    String no1 = NumberGenerator.generateNo("AA");
    String no2 = NumberGenerator.generateNo("BB");
    assertThat(no1).isNotEqualTo(no2);
  }

  // ========== 带时间戳编号测试 ==========

  @Test
  @DisplayName("生成带时间戳的编号应该以指定前缀开头")
  void generateNoWithTimestamp_shouldUseCustomPrefix() {
    String result = NumberGenerator.generateNoWithTimestamp("TS");
    assertThat(result).startsWith("TS");
  }

  @Test
  @DisplayName("带时间戳的编号应该唯一")
  void generateNoWithTimestamp_shouldBeUnique() {
    String no1 = NumberGenerator.generateNoWithTimestamp("TS");
    // 等待1毫秒确保时间戳不同
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    String no2 = NumberGenerator.generateNoWithTimestamp("TS");
    assertThat(no1).isNotEqualTo(no2);
  }

  @Test
  @DisplayName("生成的编号应该包含日期部分")
  void generateNo_shouldContainDatePart() {
    String result = NumberGenerator.generateNo("EV");
    // 检查中间部分是否为6位数字（日期）
    String datePart = result.substring(2, 8);
    assertThat(datePart).matches("\\d{6}");
  }

  @Test
  @DisplayName("生成的编号应该包含序号部分")
  void generateNo_shouldContainSequencePart() {
    String result = NumberGenerator.generateNo("EV");
    // 检查序号部分是否为4位数字
    String seqPart = result.substring(8, 12);
    assertThat(seqPart).matches("\\d{4}");
  }

  @Test
  @DisplayName("生成的编号应该包含随机部分")
  void generateNo_shouldContainRandomPart() {
    String result = NumberGenerator.generateNo("EV");
    // 检查最后2位是否为字母数字
    String randomPart = result.substring(11, 13);
    assertThat(randomPart).matches("[A-Z0-9]{2}");
  }
}
