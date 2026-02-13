package com.lawfirm.application.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.finance.repository.ContractRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ContractNumberGenerator 单元测试
 *
 * <p>测试合同编号生成功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractNumberGenerator 合同编号生成测试")
class ContractNumberGeneratorTest {

  @Mock private SysConfigAppService configAppService;

  @Mock private ContractRepository contractRepository;

  @InjectMocks private ContractNumberGenerator generator;

  // ========== 基础生成测试 ==========

  @Test
  @DisplayName("应该生成基本的合同编号")
  void generate_shouldGenerateBasicNumber() {
    when(configAppService.getConfigValue(anyString())).thenReturn(null);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate();

    assertThat(result).isNotNull();
    assertThat(result).startsWith("HT"); // 默认前缀
    assertThat(result.length()).isGreaterThan(2);
  }

  @Test
  @DisplayName("编号冲突时应该重新生成")
  void generate_shouldRegenerateOnConflict() {
    when(configAppService.getConfigValue(anyString())).thenReturn(null);
    when(contractRepository.existsByContractNo(anyString()))
        .thenReturn(true) // 第一次冲突
        .thenReturn(false); // 第二次成功

    String result = generator.generate();

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("多次冲突应该抛出异常")
  void generate_shouldThrowExceptionAfterMaxRetries() {
    when(configAppService.getConfigValue(anyString())).thenReturn(null);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(true);

    assertThatThrownBy(() -> generator.generate())
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("生成合同编号失败");
  }

  // ========== 案件类型映射测试 ==========

  @Test
  @DisplayName("应该正确映射民事案件")
  void generate_shouldMapCivilCase() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{CASE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate("CIVIL", null);

    assertThat(result).contains("民"); // 民事案件的简称
  }

  @Test
  @DisplayName("应该正确映射刑事案件")
  void generate_shouldMapCriminalCase() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{CASE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate("CRIMINAL", null);

    assertThat(result).contains("刑");
  }

  @Test
  @DisplayName("应该正确映射行政案件")
  void generate_shouldMapAdministrativeCase() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{CASE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate("ADMINISTRATIVE", null);

    assertThat(result).contains("行");
  }

  @Test
  @DisplayName("应该正确映射国家赔偿案件")
  void generate_shouldMapStateCompensationCase() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{CASE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result1 = generator.generate("STATE_COMP_ADMIN", null);
    String result2 = generator.generate("STATE_COMP_CRIMINAL", null);

    assertThat(result1).contains("行赔"); // 行政国家赔偿
    assertThat(result2).contains("刑赔"); // 刑事国家赔偿
  }

  // ========== 收费类型映射测试 ==========

  @Test
  @DisplayName("应该正确映射固定收费")
  void generate_shouldMapFixedFee() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{FEE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate(null, "FIXED");

    assertThat(result).contains("固"); // 固定收费的简称
  }

  @Test
  @DisplayName("应该正确映射计时收费")
  void generate_shouldMapHourlyFee() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{FEE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate(null, "HOURLY");

    assertThat(result).contains("时");
  }

  @Test
  @DisplayName("应该正确映射风险代理")
  void generate_shouldMapContingencyFee() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{FEE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate(null, "CONTINGENCY");

    assertThat(result).contains("风");
  }

  @Test
  @DisplayName("应该根据合同类型生成编号")
  void generate_shouldIncludeContractType() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{CONTRACT_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDateAndContractType(any(), anyString())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate("CIVIL", null, "CIVIL_PROXY");

    assertThat(result).contains("民代"); // 民事代理的简称
  }

  // ========== 预览功能测试 ==========

  @Test
  @DisplayName("应该预览编号规则")
  void previewPattern_shouldReturnPreviews() {
    List<Map<String, String>> previews =
        generator.previewPattern("{YEAR}{CASE_TYPE}-{SEQUENCE}", "HT", 4, null, null);

    assertThat(previews).isNotNull();
    assertThat(previews).hasSizeGreaterThanOrEqualTo(4); // 至少包含4种案件类型的预览
  }

  @Test
  @DisplayName("应该预览指定案件类型")
  void previewPattern_shouldPreviewSpecificCaseType() {
    List<Map<String, String>> previews =
        generator.previewPattern("{YEAR}{CASE_TYPE}-{SEQUENCE}", "HT", 4, "CIVIL", null);

    assertThat(previews).hasSize(1);
    assertThat(previews.get(0).get("caseType")).isEqualTo("CIVIL");
  }

  @Test
  @DisplayName("预览应该使用默认值")
  void previewPattern_shouldUseDefaults() {
    List<Map<String, String>> previews =
        generator.previewPattern(
            "", // 使用默认 pattern
            null, // 使用默认 prefix
            null, // 使用默认 sequenceLength
            null, null);

    assertThat(previews).isNotNull();
    assertThat(previews).hasSizeGreaterThanOrEqualTo(1);
  }

  // ========== 变量列表测试 ==========

  @Test
  @DisplayName("应该返回支持的变量列表")
  void getSupportedVariables_shouldReturnVariables() {
    List<Map<String, String>> variables = generator.getSupportedVariables();

    assertThat(variables).isNotNull();
    assertThat(variables).hasSizeGreaterThan(10);

    // 检查关键变量存在
    assertThat(variables).anyMatch(v -> "{PREFIX}".equals(v.get("name")));
    assertThat(variables).anyMatch(v -> "{YEAR}".equals(v.get("name")));
    assertThat(variables).anyMatch(v -> "{CASE_TYPE}".equals(v.get("name")));
  }

  // ========== 推荐规则测试 ==========

  @Test
  @DisplayName("应该返回推荐的编号规则")
  void getRecommendedPatterns_shouldReturnRecommendations() {
    List<Map<String, String>> patterns = generator.getRecommendedPatterns();

    assertThat(patterns).isNotNull();
    assertThat(patterns).hasSizeGreaterThanOrEqualTo(5);
  }

  // ========== 案件类型选项测试 ==========

  @Test
  @DisplayName("应该返回案件类型选项")
  void getCaseTypeOptions_shouldReturnOptions() {
    List<Map<String, String>> options = generator.getCaseTypeOptions();

    assertThat(options).isNotNull();
    assertThat(options).hasSize(11); // 11种案件类型

    // 检查关键字段
    assertThat(options).anyMatch(o -> "CIVIL".equals(o.get("value")));
    assertThat(options).anyMatch(o -> "CRIMINAL".equals(o.get("value")));
    assertThat(options).anyMatch(o -> "STATE_COMP_ADMIN".equals(o.get("value")));
  }

  @Test
  @DisplayName("案件类型选项应该包含简称和代码")
  void getCaseTypeOptions_shouldIncludeShortNameAndCode() {
    List<Map<String, String>> options = generator.getCaseTypeOptions();

    Map<String, String> civilOption =
        options.stream().filter(o -> "CIVIL".equals(o.get("value"))).findFirst().orElse(null);

    assertThat(civilOption).isNotNull();
    assertThat(civilOption.get("shortName")).isEqualTo("民");
    assertThat(civilOption.get("code")).isEqualTo("MS");
  }

  // ========== 国家赔偿案件类型测试 ==========

  @Test
  @DisplayName("国家赔偿案件选项应该有正确的简称")
  void getCaseTypeOptions_stateCompensationShouldHaveCorrectShortName() {
    List<Map<String, String>> options = generator.getCaseTypeOptions();

    Map<String, String> adminOption =
        options.stream()
            .filter(o -> "STATE_COMP_ADMIN".equals(o.get("value")))
            .findFirst()
            .orElse(null);

    Map<String, String> criminalOption =
        options.stream()
            .filter(o -> "STATE_COMP_CRIMINAL".equals(o.get("value")))
            .findFirst()
            .orElse(null);

    assertThat(adminOption).isNotNull();
    assertThat(adminOption.get("shortName")).isEqualTo("行赔");

    assertThat(criminalOption).isNotNull();
    assertThat(criminalOption.get("shortName")).isEqualTo("刑赔");
  }

  // ========== 边界情况测试 ==========

  @Test
  @DisplayName("空案件类型应该使用默认值")
  void generate_shouldUseDefaultsForNullCaseType() {
    when(configAppService.getConfigValue(anyString())).thenReturn(null);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate(null, null);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("未知的案件类型应该使用默认值")
  void generate_shouldUseDefaultsForUnknownCaseType() {
    when(configAppService.getConfigValue(anyString())).thenReturn(null);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate("UNKNOWN_TYPE", null);

    assertThat(result).isNotNull();
  }

  // ========== 序号长度测试 ==========

  @Test
  @DisplayName("序号长度应该在1-10之间")
  void generate_shouldValidateSequenceLength() {
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("6");
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate();

    assertThat(result).contains("000001"); // 6位序号
  }

  @Test
  @DisplayName("应该根据案件类型和收费类型组合生成编号")
  void generate_shouldIncludeCaseTypeAndFeeType() {
    when(configAppService.getConfigValue("contract.number.pattern"))
        .thenReturn("{PREFIX}{CASE_TYPE}{FEE_TYPE}{SEQUENCE}");
    when(configAppService.getConfigValue("contract.number.prefix")).thenReturn("HT");
    when(configAppService.getConfigValue("contract.number.sequence.length")).thenReturn("4");
    when(contractRepository.countByCreatedDate(any())).thenReturn(0L);
    when(contractRepository.existsByContractNo(anyString())).thenReturn(false);

    String result = generator.generate("CIVIL", "FIXED");

    assertThat(result).contains("民");
    assertThat(result).contains("固");
  }
}
