package com.lawfirm.application.finance.service;

import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.finance.repository.ContractRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 合同编号生成服务 支持从系统配置读取编号规则
 *
 * <p>支持的变量： - {PREFIX} 前缀 - {YEAR} 年份(YYYY) - {YEAR_SHORT} 年份(YY) - {MONTH} 月份(MM) - {DAY} 日期(DD) -
 * {DATE} 日期(YYMMDD) - {DATE_FULL} 完整日期(YYYYMMDD) - {CONTRACT_TYPE} 合同类型简称（模板类型，如民代、刑辩、行代、顾问、非诉） -
 * {CASE_TYPE} 案件类型简称（民、刑、行、知、仲、执、顾、非） - {CASE_TYPE_CODE} 案件类型代码（CIVIL->MS, CRIMINAL->XS等） -
 * {FEE_TYPE} 收费类型简称（固、时、风、混） - {SEQUENCE} 序号（基于当天合同数量，前补0；如果指定了CONTRACT_TYPE则基于该类型独立统计） -
 * {SEQUENCE_YEAR} 年度序号（基于本年合同数量，前补0；如果指定了CONTRACT_TYPE则基于该类型独立统计） - {RANDOM} 随机字符 - {RANDOM_NUM}
 * 随机数字
 *
 * <p>示例规则： - {CONTRACT_TYPE}字第{SEQUENCE_YEAR}号 -> 民代字第0001号（每种模板类型独立编号） -
 * {YEAR}{CONTRACT_TYPE}字第{SEQUENCE_YEAR}号 -> 2026刑辩字第0001号 - {YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号
 * -> 2026民代字第0001号 - {PREFIX}{DATE}{RANDOM} -> HT2601051ABC -
 * {YEAR}{CASE_TYPE_CODE}-{SEQUENCE_YEAR} -> 2026MS-0001
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractNumberGenerator {

  /** 系统配置应用服务. */
  private final SysConfigAppService configAppService;

  /** 合同仓储. */
  private final ContractRepository contractRepository;

  /** 合同编号配置键. */
  public static final String CONFIG_KEY_PREFIX = "contract.number.prefix";

  /** 合同编号模式配置键. */
  public static final String CONFIG_KEY_PATTERN = "contract.number.pattern";

  /** 合同编号序号长度配置键. */
  public static final String CONFIG_KEY_SEQUENCE_LENGTH = "contract.number.sequence.length";

  /** 默认编号规则. */
  private static final String DEFAULT_PATTERN = "{PREFIX}{DATE}{RANDOM}";

  /** 默认前缀. */
  private static final String DEFAULT_PREFIX = "HT";

  /** 默认序号长度. */
  private static final int DEFAULT_SEQUENCE_LENGTH = 4;

  /** 案件类型映射 - 中文简称. */
  private static final Map<String, String> CASE_TYPE_CN_MAP = new LinkedHashMap<>();

  static {
    CASE_TYPE_CN_MAP.put("CIVIL", "民");
    CASE_TYPE_CN_MAP.put("CRIMINAL", "刑");
    CASE_TYPE_CN_MAP.put("ADMINISTRATIVE", "行");
    CASE_TYPE_CN_MAP.put("IP", "知");
    CASE_TYPE_CN_MAP.put("ARBITRATION", "仲");
    CASE_TYPE_CN_MAP.put("ENFORCEMENT", "执");
    CASE_TYPE_CN_MAP.put("LEGAL_COUNSEL", "顾");
    CASE_TYPE_CN_MAP.put("SPECIAL_SERVICE", "非");
    CASE_TYPE_CN_MAP.put("BANKRUPTCY", "破");
    // 国家赔偿
    CASE_TYPE_CN_MAP.put("STATE_COMP_ADMIN", "行赔");
    CASE_TYPE_CN_MAP.put("STATE_COMP_CRIMINAL", "刑赔");
  }

  /** 案件类型映射 - 代码. */
  private static final Map<String, String> CASE_TYPE_CODE_MAP = new LinkedHashMap<>();

  static {
    CASE_TYPE_CODE_MAP.put("CIVIL", "MS");
    CASE_TYPE_CODE_MAP.put("CRIMINAL", "XS");
    CASE_TYPE_CODE_MAP.put("ADMINISTRATIVE", "XZ");
    CASE_TYPE_CODE_MAP.put("IP", "ZS");
    CASE_TYPE_CODE_MAP.put("ARBITRATION", "ZC");
    CASE_TYPE_CODE_MAP.put("ENFORCEMENT", "ZX");
    CASE_TYPE_CODE_MAP.put("LEGAL_COUNSEL", "GW");
    CASE_TYPE_CODE_MAP.put("SPECIAL_SERVICE", "ZX");
    CASE_TYPE_CODE_MAP.put("BANKRUPTCY", "PC");
    // 国家赔偿
    CASE_TYPE_CODE_MAP.put("STATE_COMP_ADMIN", "XZPC");
    CASE_TYPE_CODE_MAP.put("STATE_COMP_CRIMINAL", "XSPC");
  }

  /** 收费类型映射 - 中文简称. */
  private static final Map<String, String> FEE_TYPE_CN_MAP = new LinkedHashMap<>();

  static {
    FEE_TYPE_CN_MAP.put("FIXED", "固");
    FEE_TYPE_CN_MAP.put("HOURLY", "时");
    FEE_TYPE_CN_MAP.put("CONTINGENCY", "风");
    FEE_TYPE_CN_MAP.put("MIXED", "混");
  }

  /** 合同模板类型映射 - 编号简称（用于独立编号） 每种模板类型独立编号，如：民代字001、刑辩字001. */
  private static final Map<String, String> CONTRACT_TYPE_SHORT_MAP = new LinkedHashMap<>();

  static {
    CONTRACT_TYPE_SHORT_MAP.put("CIVIL_PROXY", "民代");
    CONTRACT_TYPE_SHORT_MAP.put("ADMINISTRATIVE_PROXY", "行代");
    CONTRACT_TYPE_SHORT_MAP.put("CRIMINAL_DEFENSE", "刑辩");
    CONTRACT_TYPE_SHORT_MAP.put("LEGAL_COUNSEL", "顾问");
    CONTRACT_TYPE_SHORT_MAP.put("NON_LITIGATION", "非诉");
    CONTRACT_TYPE_SHORT_MAP.put("CUSTOM", "自定");
  }

  /**
   * 生成合同编号（无案件类型和收费类型）
   *
   * @return 合同编号
   */
  public String generate() {
    return generate(null, null, null);
  }

  /**
   * 生成合同编号（带案件类型和收费类型，兼容旧版本）
   *
   * @param caseType 案件类型
   * @param feeType 收费类型
   * @return 合同编号
   */
  public String generate(final String caseType, final String feeType) {
    return generate(caseType, feeType, null);
  }

  /**
   * 生成合同编号（带案件类型、收费类型和合同类型） 根据合同类型（模板类型）独立编号，每种模板类型独立计数
   *
   * @param caseType 案件类型
   * @param feeType 收费类型
   * @param contractType 合同类型（模板类型），如CIVIL_PROXY、CRIMINAL_DEFENSE等
   * @return 合同编号
   */
  public String generate(final String caseType, final String feeType, final String contractType) {
    String pattern = getConfigValue(CONFIG_KEY_PATTERN, DEFAULT_PATTERN);
    String prefix = getConfigValue(CONFIG_KEY_PREFIX, DEFAULT_PREFIX);
    int sequenceLength = getSequenceLength();

    // 生成编号
    String contractNo =
        buildContractNumber(
            pattern, prefix, sequenceLength, caseType, feeType, contractType, false);

    // 检查编号是否已存在，如果存在则重新生成（最多重试10次）
    int retryCount = 0;
    while (contractRepository.existsByContractNo(contractNo) && retryCount < 10) {
      log.warn("合同编号已存在，重新生成: {}", contractNo);
      contractNo =
          buildContractNumber(
              pattern, prefix, sequenceLength, caseType, feeType, contractType, false);
      retryCount++;
    }

    if (retryCount >= 10) {
      throw new BusinessException("生成合同编号失败，请检查编号规则配置");
    }

    return contractNo;
  }

  /**
   * 预览编号规则
   *
   * @param pattern 规则模板
   * @param prefix 前缀
   * @param sequenceLength 序号长度
   * @param caseType 案件类型（用于预览）
   * @param feeType 收费类型（用于预览）
   * @return 预览结果列表（多种案件类型示例）
   */
  public List<Map<String, String>> previewPattern(
      final String pattern,
      final String prefix,
      final Integer sequenceLength,
      final String caseType,
      final String feeType) {
    List<Map<String, String>> previews = new ArrayList<>();

    String finalPattern = pattern;
    if (!StringUtils.hasText(finalPattern)) {
      finalPattern = DEFAULT_PATTERN;
    }
    String finalPrefix = prefix;
    if (!StringUtils.hasText(finalPrefix)) {
      finalPrefix = DEFAULT_PREFIX;
    }
    int finalSequenceLength = sequenceLength != null ? sequenceLength : DEFAULT_SEQUENCE_LENGTH;
    if (finalSequenceLength < 1 || finalSequenceLength > 10) {
      finalSequenceLength = DEFAULT_SEQUENCE_LENGTH;
    }

    // 如果规则包含案件类型变量，生成多个案件类型的示例
    if (finalPattern.contains("{CASE_TYPE}") || finalPattern.contains("{CASE_TYPE_CODE}")) {
      // 使用指定的案件类型或遍历所有类型
      if (StringUtils.hasText(caseType) && CASE_TYPE_CN_MAP.containsKey(caseType)) {
        String preview =
            buildContractNumber(
                finalPattern, finalPrefix, finalSequenceLength, caseType, feeType, null, true);
        Map<String, String> item = new LinkedHashMap<>();
        item.put("caseType", caseType);
        item.put("caseTypeName", MatterConstants.getCaseTypeName(caseType));
        item.put("preview", preview);
        previews.add(item);
      } else {
        // 生成几个代表性案件类型的预览
        String[] sampleTypes = {"CIVIL", "CRIMINAL", "ADMINISTRATIVE", "LEGAL_COUNSEL"};
        for (String type : sampleTypes) {
          String preview =
              buildContractNumber(
                  finalPattern, finalPrefix, finalSequenceLength, type, feeType, null, true);
          Map<String, String> item = new LinkedHashMap<>();
          item.put("caseType", type);
          item.put("caseTypeName", MatterConstants.getCaseTypeName(type));
          item.put("preview", preview);
          previews.add(item);
        }
      }
    } else {
      // 不包含案件类型变量，生成单个预览
      String preview =
          buildContractNumber(
              finalPattern, finalPrefix, finalSequenceLength, caseType, feeType, null, true);
      Map<String, String> item = new LinkedHashMap<>();
      item.put("caseType", "");
      item.put("caseTypeName", "通用");
      item.put("preview", preview);
      previews.add(item);
    }

    return previews;
  }

  /**
   * 获取所有支持的变量及说明
   *
   * @return 支持的变量列表
   */
  public List<Map<String, String>> getSupportedVariables() {
    List<Map<String, String>> variables = new ArrayList<>();

    addVariable(variables, "{PREFIX}", "前缀", "配置的前缀值，如 HT");
    addVariable(variables, "{YEAR}", "年份", "4位年份，如 2026");
    addVariable(variables, "{YEAR_SHORT}", "年份简写", "2位年份，如 26");
    addVariable(variables, "{MONTH}", "月份", "2位月份，如 01");
    addVariable(variables, "{DAY}", "日期", "2位日期，如 05");
    addVariable(variables, "{DATE}", "日期组合", "YYMMDD格式，如 260105");
    addVariable(variables, "{DATE_FULL}", "完整日期", "YYYYMMDD格式，如 20260105");
    addVariable(variables, "{CONTRACT_TYPE}", "合同类型(模板类型)", "民代/刑辩/行代/顾问/非诉/自定（每种模板类型独立编号）");
    addVariable(variables, "{CASE_TYPE}", "案件类型(中文)", "民/刑/行/知/仲/执/顾/非/破");
    addVariable(variables, "{CASE_TYPE_CODE}", "案件类型(代码)", "MS/XS/XZ/ZS/ZC/ZX/GW/ZX/PC");
    addVariable(variables, "{FEE_TYPE}", "收费类型", "固/时/风/混");
    addVariable(variables, "{SEQUENCE}", "日序号", "当日第N份合同，前补0（如果指定了CONTRACT_TYPE则基于该类型独立统计）");
    addVariable(variables, "{SEQUENCE_YEAR}", "年度序号", "本年度第N份合同，前补0（如果指定了CONTRACT_TYPE则基于该类型独立统计）");
    addVariable(variables, "{RANDOM}", "随机字符", "随机字母+数字");
    addVariable(variables, "{RANDOM_NUM}", "随机数字", "随机数字");

    return variables;
  }

  /**
   * 获取推荐的编号规则模板
   *
   * @return 推荐的模板列表
   */
  public List<Map<String, String>> getRecommendedPatterns() {
    List<Map<String, String>> patterns = new ArrayList<>();

    addPattern(
        patterns,
        "模板独立编号",
        "{CONTRACT_TYPE}字第{SEQUENCE_YEAR}号",
        "民代字第0001号",
        "每种模板类型独立编号，如民代字001、刑辩字001");
    addPattern(
        patterns,
        "年度模板编号",
        "{YEAR}{CONTRACT_TYPE}字第{SEQUENCE_YEAR}号",
        "2026刑辩字第0001号",
        "年度+模板类型+独立编号");
    addPattern(
        patterns, "司法格式", "{YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号", "2026民代字第0001号", "适用于正式的法律文书编号");
    addPattern(
        patterns, "简洁格式", "{YEAR}{CASE_TYPE_CODE}-{SEQUENCE_YEAR}", "2026MS-0001", "简洁的年度+类型+序号格式");
    addPattern(patterns, "通用格式", "{PREFIX}{DATE}{RANDOM}", "HT260105ABCD", "前缀+日期+随机数，通用简单");
    addPattern(patterns, "年度流水", "{PREFIX}{YEAR}-{SEQUENCE_YEAR}", "HT2026-0001", "前缀+年度+年度流水号");
    addPattern(
        patterns,
        "完整格式",
        "{YEAR}{CASE_TYPE}{FEE_TYPE}字第{SEQUENCE_YEAR}号",
        "2026民固字第0001号",
        "包含案件类型和收费类型");
    addPattern(
        patterns, "日期流水", "{PREFIX}{DATE_FULL}-{SEQUENCE}", "HT20260105-0001", "前缀+完整日期+日流水号");
    addPattern(
        patterns,
        "分类编号",
        "{CASE_TYPE_CODE}{YEAR}{MONTH}-{SEQUENCE}",
        "MS202601-0001",
        "类型代码+年月+流水号");
    addPattern(
        patterns,
        "律所代码",
        "京{PREFIX}({YEAR}){CASE_TYPE}字第{SEQUENCE_YEAR}号",
        "京HT(2026)民字第0001号",
        "带地区代码的正式格式");

    return patterns;
  }

  /**
   * 获取案件类型选项
   *
   * @return 案件类型选项列表
   */
  public List<Map<String, String>> getCaseTypeOptions() {
    List<Map<String, String>> options = new ArrayList<>();
    for (Map.Entry<String, String> entry : CASE_TYPE_CN_MAP.entrySet()) {
      Map<String, String> option = new LinkedHashMap<>();
      option.put("value", entry.getKey());
      option.put("label", MatterConstants.getCaseTypeName(entry.getKey()));
      option.put("shortName", entry.getValue());
      option.put("code", CASE_TYPE_CODE_MAP.get(entry.getKey()));
      options.add(option);
    }
    return options;
  }

  /**
   * 根据规则构建合同编号（支持合同类型） 如果指定了contractType，序号将基于该类型独立统计
   *
   * @param pattern 编号规则模板
   * @param prefix 前缀
   * @param sequenceLength 序号长度
   * @param caseType 案件类型
   * @param feeType 收费类型
   * @param contractType 合同类型
   * @param isPreview 是否预览
   * @return 合同编号
   */
  private String buildContractNumber(
      final String pattern,
      final String prefix,
      final int sequenceLength,
      final String caseType,
      final String feeType,
      final String contractType,
      final boolean isPreview) {
    String finalPattern = pattern;
    if (!StringUtils.hasText(finalPattern)) {
      finalPattern = DEFAULT_PATTERN;
    }

    String result = finalPattern;
    LocalDate now = LocalDate.now();

    // 替换 {PREFIX}
    result = result.replace("{PREFIX}", prefix != null ? prefix : DEFAULT_PREFIX);

    // 替换日期相关变量
    result = result.replace("{DATE}", now.format(DateTimeFormatter.ofPattern("yyMMdd")));
    result = result.replace("{DATE_FULL}", now.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    result = result.replace("{YEAR}", now.format(DateTimeFormatter.ofPattern("yyyy")));
    result = result.replace("{YEAR_SHORT}", now.format(DateTimeFormatter.ofPattern("yy")));
    result = result.replace("{MONTH}", now.format(DateTimeFormatter.ofPattern("MM")));
    result = result.replace("{DAY}", now.format(DateTimeFormatter.ofPattern("dd")));

    // 替换合同类型（模板类型）- 用于独立编号
    if (StringUtils.hasText(contractType)) {
      String contractTypeShort = CONTRACT_TYPE_SHORT_MAP.getOrDefault(contractType, contractType);
      result = result.replace("{CONTRACT_TYPE}", contractTypeShort);
    } else {
      result = result.replace("{CONTRACT_TYPE}", "");
    }

    // 替换案件类型
    String caseTypeCn = CASE_TYPE_CN_MAP.getOrDefault(caseType, "民");
    String caseTypeCode = CASE_TYPE_CODE_MAP.getOrDefault(caseType, "MS");
    result = result.replace("{CASE_TYPE}", caseTypeCn);
    result = result.replace("{CASE_TYPE_CODE}", caseTypeCode);

    // 替换收费类型
    String feeTypeCn = FEE_TYPE_CN_MAP.getOrDefault(feeType, "固");
    result = result.replace("{FEE_TYPE}", feeTypeCn);

    // 替换序号（如果指定了contractType，则基于该类型独立统计）
    if (result.contains("{SEQUENCE}")) {
      String seq =
          isPreview
              ? formatSequence(1, sequenceLength)
              : generateDailySequence(sequenceLength, contractType);
      result = result.replace("{SEQUENCE}", seq);
    }
    if (result.contains("{SEQUENCE_YEAR}")) {
      String seq =
          isPreview
              ? formatSequence(1, sequenceLength)
              : generateYearlySequence(sequenceLength, contractType);
      result = result.replace("{SEQUENCE_YEAR}", seq);
    }

    // 替换随机字符
    result = result.replace("{RANDOM}", generateRandomString(sequenceLength));
    result = result.replace("{RANDOM_NUM}", generateRandomNumber(sequenceLength));

    return result;
  }

  private String generateRandomString(final int length) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
  }

  private String generateRandomNumber(final int length) {
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < length; i++) {
      sb.append(random.nextInt(10));
    }
    return sb.toString();
  }

  private String generateDailySequence(final int length, final String contractType) {
    long count;
    if (StringUtils.hasText(contractType)) {
      // 基于合同类型（模板类型）独立统计
      count = contractRepository.countByCreatedDateAndContractType(LocalDate.now(), contractType);
    } else {
      // 统计所有合同
      count = contractRepository.countByCreatedDate(LocalDate.now());
    }
    return formatSequence(count + 1, length);
  }

  private String generateYearlySequence(final int length, final String contractType) {
    long count;
    if (StringUtils.hasText(contractType)) {
      // 基于合同类型（模板类型）独立统计
      count =
          contractRepository.countByCreatedYearAndContractType(
              LocalDate.now().getYear(), contractType);
    } else {
      // 统计所有合同
      count = contractRepository.countByCreatedYear(LocalDate.now().getYear());
    }
    return formatSequence(count + 1, length);
  }

  private String formatSequence(final long sequence, final int length) {
    return String.format("%0" + length + "d", sequence);
  }

  private int getSequenceLength() {
    String lengthStr =
        getConfigValue(CONFIG_KEY_SEQUENCE_LENGTH, String.valueOf(DEFAULT_SEQUENCE_LENGTH));
    try {
      int length = Integer.parseInt(lengthStr);
      return (length >= 1 && length <= 10) ? length : DEFAULT_SEQUENCE_LENGTH;
    } catch (NumberFormatException e) {
      return DEFAULT_SEQUENCE_LENGTH;
    }
  }

  private String getConfigValue(final String key, final String defaultValue) {
    try {
      String value = configAppService.getConfigValue(key);
      return StringUtils.hasText(value) ? value : defaultValue;
    } catch (Exception e) {
      log.warn("获取系统配置失败: {}, 使用默认值: {}", key, defaultValue, e);
      return defaultValue;
    }
  }

  private void addVariable(
      final List<Map<String, String>> list,
      final String name,
      final String label,
      final String description) {
    Map<String, String> item = new LinkedHashMap<>();
    item.put("name", name);
    item.put("label", label);
    item.put("description", description);
    list.add(item);
  }

  private void addPattern(
      final List<Map<String, String>> list,
      final String name,
      final String pattern,
      final String example,
      final String description) {
    Map<String, String> item = new LinkedHashMap<>();
    item.put("name", name);
    item.put("pattern", pattern);
    item.put("example", example);
    item.put("description", description);
    list.add(item);
  }
}
