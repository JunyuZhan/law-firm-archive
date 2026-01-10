package com.lawfirm.application.finance.service;

import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.domain.finance.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 合同编号生成服务
 * 支持从系统配置读取编号规则
 * 
 * 支持的变量：
 * - {PREFIX} 前缀
 * - {YEAR} 年份(YYYY)
 * - {YEAR_SHORT} 年份(YY)
 * - {MONTH} 月份(MM)
 * - {DAY} 日期(DD)
 * - {DATE} 日期(YYMMDD)
 * - {DATE_FULL} 完整日期(YYYYMMDD)
 * - {CASE_TYPE} 案件类型简称（民、刑、行、知、仲、执、顾、非）
 * - {CASE_TYPE_CODE} 案件类型代码（CIVIL->MS, CRIMINAL->XS等）
 * - {FEE_TYPE} 收费类型简称（固、时、风、混）
 * - {SEQUENCE} 序号（基于当天合同数量，前补0）
 * - {SEQUENCE_YEAR} 年度序号（基于本年合同数量）
 * - {RANDOM} 随机字符
 * - {RANDOM_NUM} 随机数字
 * 
 * 示例规则：
 * - {YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号 -> 2026民代字第0001号
 * - {PREFIX}{DATE}{RANDOM} -> HT2601051ABC
 * - {YEAR}{CASE_TYPE_CODE}-{SEQUENCE_YEAR} -> 2026MS-0001
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractNumberGenerator {

    private final SysConfigAppService configAppService;
    private final ContractRepository contractRepository;

    /**
     * 合同编号配置键
     */
    public static final String CONFIG_KEY_PREFIX = "contract.number.prefix";
    public static final String CONFIG_KEY_PATTERN = "contract.number.pattern";
    public static final String CONFIG_KEY_SEQUENCE_LENGTH = "contract.number.sequence.length";

    /**
     * 默认编号规则
     */
    private static final String DEFAULT_PATTERN = "{PREFIX}{DATE}{RANDOM}";
    private static final String DEFAULT_PREFIX = "HT";
    private static final int DEFAULT_SEQUENCE_LENGTH = 4;

    /**
     * 案件类型映射 - 中文简称
     */
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
    }

    /**
     * 案件类型映射 - 代码
     */
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
    }

    /**
     * 收费类型映射 - 中文简称
     */
    private static final Map<String, String> FEE_TYPE_CN_MAP = new LinkedHashMap<>();
    static {
        FEE_TYPE_CN_MAP.put("FIXED", "固");
        FEE_TYPE_CN_MAP.put("HOURLY", "时");
        FEE_TYPE_CN_MAP.put("CONTINGENCY", "风");
        FEE_TYPE_CN_MAP.put("MIXED", "混");
    }

    /**
     * 生成合同编号（无案件类型和收费类型）
     */
    public String generate() {
        return generate(null, null);
    }

    /**
     * 生成合同编号（带案件类型和收费类型）
     */
    public String generate(String caseType, String feeType) {
        String pattern = getConfigValue(CONFIG_KEY_PATTERN, DEFAULT_PATTERN);
        String prefix = getConfigValue(CONFIG_KEY_PREFIX, DEFAULT_PREFIX);
        int sequenceLength = getSequenceLength();

        // 生成编号
        String contractNo = buildContractNumber(pattern, prefix, sequenceLength, caseType, feeType, false);
        
        // 检查编号是否已存在，如果存在则重新生成（最多重试10次）
        int retryCount = 0;
        while (contractRepository.existsByContractNo(contractNo) && retryCount < 10) {
            log.warn("合同编号已存在，重新生成: {}", contractNo);
            contractNo = buildContractNumber(pattern, prefix, sequenceLength, caseType, feeType, false);
            retryCount++;
        }
        
        if (retryCount >= 10) {
            throw new BusinessException("生成合同编号失败，请检查编号规则配置");
        }
        
        return contractNo;
    }

    /**
     * 预览编号规则
     * @param pattern 规则模板
     * @param prefix 前缀
     * @param sequenceLength 序号长度
     * @param caseType 案件类型（用于预览）
     * @param feeType 收费类型（用于预览）
     * @return 预览结果列表（多种案件类型示例）
     */
    public List<Map<String, String>> previewPattern(String pattern, String prefix, Integer sequenceLength, 
                                                      String caseType, String feeType) {
        List<Map<String, String>> previews = new ArrayList<>();
        
        if (!StringUtils.hasText(pattern)) {
            pattern = DEFAULT_PATTERN;
        }
        if (!StringUtils.hasText(prefix)) {
            prefix = DEFAULT_PREFIX;
        }
        if (sequenceLength == null || sequenceLength < 1 || sequenceLength > 10) {
            sequenceLength = DEFAULT_SEQUENCE_LENGTH;
        }

        // 如果规则包含案件类型变量，生成多个案件类型的示例
        if (pattern.contains("{CASE_TYPE}") || pattern.contains("{CASE_TYPE_CODE}")) {
            // 使用指定的案件类型或遍历所有类型
            if (StringUtils.hasText(caseType) && CASE_TYPE_CN_MAP.containsKey(caseType)) {
                String preview = buildContractNumber(pattern, prefix, sequenceLength, caseType, feeType, true);
                Map<String, String> item = new LinkedHashMap<>();
                item.put("caseType", caseType);
                item.put("caseTypeName", MatterConstants.getCaseTypeName(caseType));
                item.put("preview", preview);
                previews.add(item);
            } else {
                // 生成几个代表性案件类型的预览
                String[] sampleTypes = {"CIVIL", "CRIMINAL", "ADMINISTRATIVE", "LEGAL_COUNSEL"};
                for (String type : sampleTypes) {
                    String preview = buildContractNumber(pattern, prefix, sequenceLength, type, feeType, true);
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("caseType", type);
                    item.put("caseTypeName", MatterConstants.getCaseTypeName(type));
                    item.put("preview", preview);
                    previews.add(item);
                }
            }
        } else {
            // 不包含案件类型变量，生成单个预览
            String preview = buildContractNumber(pattern, prefix, sequenceLength, caseType, feeType, true);
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
        addVariable(variables, "{CASE_TYPE}", "案件类型(中文)", "民/刑/行/知/仲/执/顾/非/破");
        addVariable(variables, "{CASE_TYPE_CODE}", "案件类型(代码)", "MS/XS/XZ/ZS/ZC/ZX/GW/ZX/PC");
        addVariable(variables, "{FEE_TYPE}", "收费类型", "固/时/风/混");
        addVariable(variables, "{SEQUENCE}", "日序号", "当日第N份合同，前补0");
        addVariable(variables, "{SEQUENCE_YEAR}", "年度序号", "本年度第N份合同，前补0");
        addVariable(variables, "{RANDOM}", "随机字符", "随机字母+数字");
        addVariable(variables, "{RANDOM_NUM}", "随机数字", "随机数字");
        
        return variables;
    }

    /**
     * 获取推荐的编号规则模板
     */
    public List<Map<String, String>> getRecommendedPatterns() {
        List<Map<String, String>> patterns = new ArrayList<>();
        
        addPattern(patterns, "司法格式", "{YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号", 
                   "2026民代字第0001号", "适用于正式的法律文书编号");
        addPattern(patterns, "简洁格式", "{YEAR}{CASE_TYPE_CODE}-{SEQUENCE_YEAR}", 
                   "2026MS-0001", "简洁的年度+类型+序号格式");
        addPattern(patterns, "通用格式", "{PREFIX}{DATE}{RANDOM}", 
                   "HT260105ABCD", "前缀+日期+随机数，通用简单");
        addPattern(patterns, "年度流水", "{PREFIX}{YEAR}-{SEQUENCE_YEAR}", 
                   "HT2026-0001", "前缀+年度+年度流水号");
        addPattern(patterns, "完整格式", "{YEAR}{CASE_TYPE}{FEE_TYPE}字第{SEQUENCE_YEAR}号", 
                   "2026民固字第0001号", "包含案件类型和收费类型");
        addPattern(patterns, "日期流水", "{PREFIX}{DATE_FULL}-{SEQUENCE}", 
                   "HT20260105-0001", "前缀+完整日期+日流水号");
        addPattern(patterns, "分类编号", "{CASE_TYPE_CODE}{YEAR}{MONTH}-{SEQUENCE}", 
                   "MS202601-0001", "类型代码+年月+流水号");
        addPattern(patterns, "律所代码", "京{PREFIX}({YEAR}){CASE_TYPE}字第{SEQUENCE_YEAR}号", 
                   "京HT(2026)民字第0001号", "带地区代码的正式格式");
        
        return patterns;
    }

    /**
     * 获取案件类型选项
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
     * 根据规则构建合同编号
     */
    private String buildContractNumber(String pattern, String prefix, int sequenceLength, 
                                        String caseType, String feeType, boolean isPreview) {
        if (!StringUtils.hasText(pattern)) {
            pattern = DEFAULT_PATTERN;
        }
        
        String result = pattern;
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
        
        // 替换案件类型
        String caseTypeCn = CASE_TYPE_CN_MAP.getOrDefault(caseType, "民");
        String caseTypeCode = CASE_TYPE_CODE_MAP.getOrDefault(caseType, "MS");
        result = result.replace("{CASE_TYPE}", caseTypeCn);
        result = result.replace("{CASE_TYPE_CODE}", caseTypeCode);
        
        // 替换收费类型
        String feeTypeCn = FEE_TYPE_CN_MAP.getOrDefault(feeType, "固");
        result = result.replace("{FEE_TYPE}", feeTypeCn);
        
        // 替换序号
        if (result.contains("{SEQUENCE}")) {
            String seq = isPreview ? formatSequence(1, sequenceLength) : generateDailySequence(sequenceLength);
            result = result.replace("{SEQUENCE}", seq);
        }
        if (result.contains("{SEQUENCE_YEAR}")) {
            String seq = isPreview ? formatSequence(1, sequenceLength) : generateYearlySequence(sequenceLength);
            result = result.replace("{SEQUENCE_YEAR}", seq);
        }
        
        // 替换随机字符
        result = result.replace("{RANDOM}", generateRandomString(sequenceLength));
        result = result.replace("{RANDOM_NUM}", generateRandomNumber(sequenceLength));
        
        return result;
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateDailySequence(int length) {
        long count = contractRepository.countByCreatedDate(LocalDate.now());
        return formatSequence(count + 1, length);
    }

    private String generateYearlySequence(int length) {
        long count = contractRepository.countByCreatedYear(LocalDate.now().getYear());
        return formatSequence(count + 1, length);
    }

    private String formatSequence(long sequence, int length) {
        return String.format("%0" + length + "d", sequence);
    }

    private int getSequenceLength() {
        String lengthStr = getConfigValue(CONFIG_KEY_SEQUENCE_LENGTH, String.valueOf(DEFAULT_SEQUENCE_LENGTH));
        try {
            int length = Integer.parseInt(lengthStr);
            return (length >= 1 && length <= 10) ? length : DEFAULT_SEQUENCE_LENGTH;
        } catch (NumberFormatException e) {
            return DEFAULT_SEQUENCE_LENGTH;
        }
    }

    private String getConfigValue(String key, String defaultValue) {
        try {
            String value = configAppService.getConfigValue(key);
            return StringUtils.hasText(value) ? value : defaultValue;
        } catch (Exception e) {
            log.warn("获取系统配置失败: {}, 使用默认值: {}", key, defaultValue, e);
            return defaultValue;
        }
    }


    private void addVariable(List<Map<String, String>> list, String name, String label, String description) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("label", label);
        item.put("description", description);
        list.add(item);
    }

    private void addPattern(List<Map<String, String>> list, String name, String pattern, 
                            String example, String description) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("pattern", pattern);
        item.put("example", example);
        item.put("description", description);
        list.add(item);
    }
}

