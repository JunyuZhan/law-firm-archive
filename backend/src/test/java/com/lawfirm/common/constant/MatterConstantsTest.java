package com.lawfirm.common.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * MatterConstants 单元测试
 *
 * <p>测试项目大类、案件类型、项目状态的名称映射功能
 */
@DisplayName("MatterConstants 常量类测试")
class MatterConstantsTest {

  // ========== 项目大类测试 ==========

  @Test
  @DisplayName("应该正确返回项目大类名称")
  void shouldReturnCorrectMatterTypeName() {
    assertThat(MatterConstants.getMatterTypeName("LITIGATION")).isEqualTo("诉讼案件");
    assertThat(MatterConstants.getMatterTypeName("NON_LITIGATION")).isEqualTo("非诉项目");
  }

  @Test
  @DisplayName("null 应该返回 null")
  void shouldReturnNullForNullMatterType() {
    assertThat(MatterConstants.getMatterTypeName(null)).isNull();
  }

  @Test
  @DisplayName("空字符串应该返回空字符串")
  void shouldReturnEmptyStringForEmptyMatterType() {
    assertThat(MatterConstants.getMatterTypeName("")).isEqualTo("");
  }

  @Test
  @DisplayName("未知的项目大类应该返回原值")
  void shouldReturnOriginalValueForUnknownMatterType() {
    assertThat(MatterConstants.getMatterTypeName("UNKNOWN_TYPE")).isEqualTo("UNKNOWN_TYPE");
  }

  // ========== 案件类型测试 ==========

  @ParameterizedTest
  @CsvSource({
    "CIVIL, 民事案件",
    "CRIMINAL, 刑事案件",
    "ADMINISTRATIVE, 行政案件",
    "BANKRUPTCY, 破产案件",
    "IP, 知识产权案件",
    "ARBITRATION, 仲裁案件",
    "COMMERCIAL_ARBITRATION, 商事仲裁",
    "LABOR_ARBITRATION, 劳动仲裁",
    "ENFORCEMENT, 执行案件",
    "LEGAL_COUNSEL, 法律顾问",
    "SPECIAL_SERVICE, 专项服务",
    "DUE_DILIGENCE, 尽职调查",
    "CONTRACT_REVIEW, 合同审查",
    "LEGAL_OPINION, 法律意见",
    "STATE_COMP_ADMIN, 行政国家赔偿",
    "STATE_COMP_CRIMINAL, 刑事国家赔偿"
  })
  @DisplayName("应该正确返回所有案件类型名称")
  void shouldReturnCorrectCaseTypeName(String caseType, String expectedName) {
    assertThat(MatterConstants.getCaseTypeName(caseType)).isEqualTo(expectedName);
  }

  @Test
  @DisplayName("null 应该返回 null")
  void shouldReturnNullForNullCaseType() {
    assertThat(MatterConstants.getCaseTypeName(null)).isNull();
  }

  @Test
  @DisplayName("空字符串应该返回空字符串")
  void shouldReturnEmptyStringForEmptyCaseType() {
    assertThat(MatterConstants.getCaseTypeName("")).isEqualTo("");
  }

  @Test
  @DisplayName("未知的案件类型应该返回原值")
  void shouldReturnOriginalValueForUnknownCaseType() {
    assertThat(MatterConstants.getCaseTypeName("UNKNOWN_CASE_TYPE")).isEqualTo("UNKNOWN_CASE_TYPE");
  }

  @Test
  @DisplayName("应该包含所有16个案件类型")
  void shouldContainAll16CaseTypes() {
    assertThat(MatterConstants.CASE_TYPE_NAME_MAP).hasSize(16);
    assertThat(MatterConstants.CASE_TYPE_NAME_MAP)
        .containsKeys(
            "CIVIL",
            "CRIMINAL",
            "ADMINISTRATIVE",
            "BANKRUPTCY",
            "IP",
            "ARBITRATION",
            "COMMERCIAL_ARBITRATION",
            "LABOR_ARBITRATION",
            "ENFORCEMENT",
            "LEGAL_COUNSEL",
            "SPECIAL_SERVICE",
            "DUE_DILIGENCE",
            "CONTRACT_REVIEW",
            "LEGAL_OPINION",
            "STATE_COMP_ADMIN",
            "STATE_COMP_CRIMINAL");
  }

  // ========== 项目状态测试 ==========

  @ParameterizedTest
  @CsvSource({
    "DRAFT, 草稿",
    "PENDING, 待审批",
    "ACTIVE, 进行中",
    "SUSPENDED, 已暂停",
    "PENDING_CLOSE, 待审批结案",
    "CLOSED, 已结案",
    "ARCHIVED, 已归档"
  })
  @DisplayName("应该正确返回所有项目状态名称")
  void shouldReturnCorrectMatterStatusName(String status, String expectedName) {
    assertThat(MatterConstants.getMatterStatusName(status)).isEqualTo(expectedName);
  }

  @Test
  @DisplayName("null 应该返回 null")
  void shouldReturnNullForNullStatus() {
    assertThat(MatterConstants.getMatterStatusName(null)).isNull();
  }

  @Test
  @DisplayName("空字符串应该返回空字符串")
  void shouldReturnEmptyStringForEmptyStatus() {
    assertThat(MatterConstants.getMatterStatusName("")).isEqualTo("");
  }

  @Test
  @DisplayName("未知的状态应该返回原值")
  void shouldReturnOriginalValueForUnknownStatus() {
    assertThat(MatterConstants.getMatterStatusName("UNKNOWN_STATUS")).isEqualTo("UNKNOWN_STATUS");
  }

  @Test
  @DisplayName("应该包含所有7个项目状态")
  void shouldContainAll7MatterStatuses() {
    assertThat(MatterConstants.MATTER_STATUS_NAME_MAP).hasSize(7);
    assertThat(MatterConstants.MATTER_STATUS_NAME_MAP)
        .containsKeys(
            "DRAFT", "PENDING", "ACTIVE", "SUSPENDED", "PENDING_CLOSE", "CLOSED", "ARCHIVED");
  }

  // ========== 完整性测试 ==========

  @Test
  @DisplayName("应该包含所有2个项目大类")
  void shouldContainAll2MatterTypes() {
    assertThat(MatterConstants.MATTER_TYPE_NAME_MAP).hasSize(2);
    assertThat(MatterConstants.MATTER_TYPE_NAME_MAP).containsKeys("LITIGATION", "NON_LITIGATION");
  }

  @Test
  @DisplayName("所有映射应该使用完整的中文名称")
  void shouldUseCompleteChineseNames() {
    // 验证知识产权使用完整名称
    assertThat(MatterConstants.getCaseTypeName("IP")).isEqualTo("知识产权案件");
    assertThat(MatterConstants.getCaseTypeName("IP")).isNotEqualTo("知识产权");

    // 验证状态使用完整名称
    assertThat(MatterConstants.getMatterStatusName("SUSPENDED")).isEqualTo("已暂停");
    assertThat(MatterConstants.getMatterStatusName("SUSPENDED")).isNotEqualTo("暂停");

    assertThat(MatterConstants.getMatterStatusName("CLOSED")).isEqualTo("已结案");
    assertThat(MatterConstants.getMatterStatusName("CLOSED")).isNotEqualTo("结案");

    assertThat(MatterConstants.getMatterStatusName("ARCHIVED")).isEqualTo("已归档");
    assertThat(MatterConstants.getMatterStatusName("ARCHIVED")).isNotEqualTo("归档");
  }

  // ========== 边界情况测试 ==========

  @Test
  @DisplayName("应该处理大小写敏感")
  void shouldBeCaseSensitive() {
    // 注意：映射是大小写敏感的，小写应该返回原值
    assertThat(MatterConstants.getMatterTypeName("litigation")).isEqualTo("litigation");
    assertThat(MatterConstants.getCaseTypeName("civil")).isEqualTo("civil");
    assertThat(MatterConstants.getMatterStatusName("draft")).isEqualTo("draft");
  }

  @Test
  @DisplayName("应该处理特殊字符")
  void shouldHandleSpecialCharacters() {
    assertThat(MatterConstants.getMatterTypeName("TYPE_WITH_UNDERSCORE"))
        .isEqualTo("TYPE_WITH_UNDERSCORE");
    assertThat(MatterConstants.getCaseTypeName("TYPE-WITH-DASH")).isEqualTo("TYPE-WITH-DASH");
  }

  // ========== 一致性测试 ==========

  @Test
  @DisplayName("映射应该与前端常量一致")
  void shouldBeConsistentWithFrontendConstants() {
    // 验证关键类型的一致性
    assertThat(MatterConstants.getMatterTypeName("LITIGATION")).isEqualTo("诉讼案件");
    assertThat(MatterConstants.getMatterTypeName("NON_LITIGATION")).isEqualTo("非诉项目");

    assertThat(MatterConstants.getCaseTypeName("CIVIL")).isEqualTo("民事案件");
    assertThat(MatterConstants.getCaseTypeName("CRIMINAL")).isEqualTo("刑事案件");
    assertThat(MatterConstants.getCaseTypeName("ADMINISTRATIVE")).isEqualTo("行政案件");
  }

  // ========== 代理阶段测试 ==========

  @ParameterizedTest
  @CsvSource({
    "FIRST_INSTANCE, 一审",
    "SECOND_INSTANCE, 二审",
    "RETRIAL, 再审",
    "EXECUTION, 执行",
    "ARBITRATION, 仲裁阶段",
    "INVESTIGATION, 侦查阶段",
    "PROSECUTION_REVIEW, 审查起诉",
    "DEATH_PENALTY_REVIEW, 死刑复核",
    "ADMINISTRATIVE_RECONSIDERATION, 行政复议",
    "EXECUTION_OBJECTION, 执行异议",
    "EXECUTION_REVIEW, 执行复议",
    "NON_LITIGATION, 非诉服务",
    "COMPENSATION_APPLICATION, 赔偿申请",
    "COMPENSATION_DECISION, 赔偿决定",
    "ADMIN_RECONSIDERATION, 行政复议",
    "ADMIN_LITIGATION, 行政赔偿诉讼",
    "CRIMINAL_TERMINATION, 刑事诉讼终结确认",
    "CRIMINAL_REVIEW, 刑事赔偿复议",
    "COMPENSATION_COMMITTEE, 赔偿委员会",
    "COMMITTEE_REVIEW, 上级赔偿委员会",
    "PAYMENT, 支付赔偿金"
  })
  @DisplayName("应该正确返回所有代理阶段名称")
  void shouldReturnCorrectLitigationStageName(String stage, String expectedName) {
    assertThat(MatterConstants.getLitigationStageName(stage)).isEqualTo(expectedName);
  }

  @Test
  @DisplayName("未知代理阶段应该返回原值")
  void shouldReturnOriginalValueForUnknownStage() {
    assertThat(MatterConstants.getLitigationStageName("UNKNOWN_STAGE")).isEqualTo("UNKNOWN_STAGE");
  }

  @Test
  @DisplayName("null 代理阶段应该返回 null")
  void shouldReturnNullForNullStage() {
    assertThat(MatterConstants.getLitigationStageName(null)).isNull();
  }

  @Test
  @DisplayName("应该包含所有代理阶段")
  void shouldContainAllLitigationStages() {
    assertThat(MatterConstants.LITIGATION_STAGE_NAME_MAP).hasSize(21);
    assertThat(MatterConstants.LITIGATION_STAGE_NAME_MAP)
        .containsKeys(
            "FIRST_INSTANCE",
            "SECOND_INSTANCE",
            "RETRIAL",
            "EXECUTION",
            "ARBITRATION",
            "INVESTIGATION",
            "PROSECUTION_REVIEW",
            "DEATH_PENALTY_REVIEW",
            "ADMINISTRATIVE_RECONSIDERATION",
            "EXECUTION_OBJECTION",
            "EXECUTION_REVIEW",
            "NON_LITIGATION",
            "COMPENSATION_APPLICATION",
            "COMPENSATION_DECISION",
            "ADMIN_RECONSIDERATION",
            "ADMIN_LITIGATION",
            "CRIMINAL_TERMINATION",
            "CRIMINAL_REVIEW",
            "COMPENSATION_COMMITTEE",
            "COMMITTEE_REVIEW",
            "PAYMENT");
  }

  @Test
  @DisplayName("应该正确返回国家赔偿案件类型名称")
  void shouldReturnCorrectStateCompensationCaseNames() {
    assertThat(MatterConstants.getCaseTypeName("STATE_COMP_ADMIN")).isEqualTo("行政国家赔偿");
    assertThat(MatterConstants.getCaseTypeName("STATE_COMP_CRIMINAL")).isEqualTo("刑事国家赔偿");
  }
}
