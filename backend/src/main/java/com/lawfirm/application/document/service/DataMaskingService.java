package com.lawfirm.application.document.service;

import com.lawfirm.application.document.dto.MaskingMappingDTO;
import com.lawfirm.application.document.dto.MatterContextDTO;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 数据脱敏服务 用于将敏感信息进行脱敏处理，保护隐私数据 支持脱敏还原：生成时保存映射关系，生成后可还原 */
@Slf4j
@Service
public class DataMaskingService {

  /** 地址保留前缀长度。 */
  private static final int ADDRESS_PREFIX_LEN = 6;

  // ========== 基础敏感信息正则 ==========

  /** 身份证号正则（18位）。 */
  private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\d{3}[0-9Xx])");

  /** 手机号正则（11位，1开头）。 */
  private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

  /** 固定电话正则（区号-号码 或 纯数字）。 */
  private static final Pattern LANDLINE_PATTERN =
      Pattern.compile("(0\\d{2,3})[- ]?(\\d{4})(\\d{4})");

  /** 邮箱正则。 */
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("([a-zA-Z0-9_.+-]{1,3})[a-zA-Z0-9_.+-]*@([a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)");

  /** 银行卡号正则（16-19位数字）。 */
  private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,11}(\\d{4})");

  /** 统一社会信用代码正则（18位）。 */
  private static final Pattern CREDIT_CODE_PATTERN =
      Pattern.compile("([0-9A-Z]{4})[0-9A-Z]{10}([0-9A-Z]{4})");

  // ========== 新增敏感信息正则 ==========

  /** 护照号码正则（字母开头+8位数字，或纯9位数字）。 */
  private static final Pattern PASSPORT_PATTERN =
      Pattern.compile("([A-Z])\\d{4}(\\d{4})|([A-Z]{2})\\d{5}(\\d{2})");

  /** 车牌号正则（普通车牌和新能源车牌）。 */
  private static final Pattern LICENSE_PLATE_PATTERN =
      Pattern.compile("([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-Z])[A-Z0-9]{3}([A-Z0-9]{2,3})");

  /** 港澳通行证正则（C/W开头+8位数字）。 */
  private static final Pattern HK_MACAO_PERMIT_PATTERN = Pattern.compile("([CW])\\d{4}(\\d{4})");

  /** 台湾通行证正则（T开头+8位数字）。 */
  private static final Pattern TAIWAN_PERMIT_PATTERN = Pattern.compile("([T])\\d{4}(\\d{4})");

  /** 军官证正则（多种格式）。 */
  private static final Pattern MILITARY_ID_PATTERN =
      Pattern.compile("(军字第|士字第|文字第|[军士文])?([0-9]{4})[0-9]{4}([0-9]{4})");

  /** IP地址正则（IPv4）。 */
  private static final Pattern IP_ADDRESS_PATTERN =
      Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

  /** 房产证号/不动产权证号正则。 */
  private static final Pattern PROPERTY_CERT_PATTERN =
      Pattern.compile(
          "(京|津|沪|渝|冀|豫|云|辽|黑|湘|皖|鲁|新|苏|浙|赣|鄂|桂|甘|晋|蒙|陕|吉|闽|贵|粤|青|藏|川|宁|琼)?"
              + "(\\(\\d{4}\\)|\\d{4})[不动产权第|房权证|房产证]*([0-9]{4})[0-9]{4}([0-9]{4})号?");

  /** 脱敏结果包装类 包含脱敏后的上下文和映射关系 */
  public static class MaskingResult {
    /** 脱敏后的上下文。 */
    private final MatterContextDTO maskedContext;

    /** 脱敏映射关系。 */
    private final MaskingMappingDTO mapping;

    /**
     * 构造函数。
     *
     * @param maskedContext 脱敏后的上下文
     * @param mapping 脱敏映射关系
     */
    public MaskingResult(final MatterContextDTO maskedContext, final MaskingMappingDTO mapping) {
      this.maskedContext = maskedContext;
      this.mapping = mapping;
    }

    /**
     * 获取脱敏后的上下文。
     *
     * @return 脱敏后的上下文
     */
    public MatterContextDTO getMaskedContext() {
      return maskedContext;
    }

    /**
     * 获取脱敏映射关系。
     *
     * @return 脱敏映射关系
     */
    public MaskingMappingDTO getMapping() {
      return mapping;
    }
  }

  /**
   * 对项目上下文进行脱敏处理，并返回映射关系
   *
   * @param context 原始上下文
   * @return 脱敏结果（包含脱敏后的上下文和映射关系）
   */
  public MaskingResult maskContextWithMapping(final MatterContextDTO context) {
    if (context == null) {
      return new MaskingResult(null, new MaskingMappingDTO());
    }

    log.info("开始对项目上下文进行脱敏处理（带映射）");

    MaskingMappingDTO mapping = new MaskingMappingDTO();
    MatterContextDTO masked = new MatterContextDTO();
    masked.setMasked(true);

    // 1. 脱敏项目信息
    if (context.getMatter() != null) {
      masked.setMatter(maskMatterInfoWithMapping(context.getMatter(), mapping));
    }

    // 2. 脱敏客户信息
    if (context.getClients() != null) {
      masked.setClients(
          context.getClients().stream().map(c -> maskClientInfoWithMapping(c, mapping)).toList());
    }

    // 3. 脱敏参与人信息
    if (context.getParticipants() != null) {
      masked.setParticipants(
          context.getParticipants().stream()
              .map(p -> maskParticipantInfoWithMapping(p, mapping))
              .toList());
    }

    // 4. 文档信息
    if (context.getDocuments() != null) {
      masked.setDocuments(
          context.getDocuments().stream()
              .map(d -> maskDocumentInfoWithMapping(d, mapping))
              .toList());
    }

    log.info("项目上下文脱敏完成，映射条目数: {}", mapping.getMappings().size());
    return new MaskingResult(masked, mapping);
  }

  /**
   * 对项目上下文进行脱敏处理（不返回映射关系）。
   *
   * @param context 原始上下文
   * @return 脱敏后的上下文
   */
  public MatterContextDTO maskContext(final MatterContextDTO context) {
    return maskContextWithMapping(context).getMaskedContext();
  }

  /**
   * 对文本进行脱敏处理，并返回映射关系。
   *
   * @param text 原始文本
   * @return 脱敏结果（包含脱敏后的上下文和映射关系）
   */
  public MaskingResult maskTextWithMapping(final String text) {
    if (text == null || text.isEmpty()) {
      return new MaskingResult(null, new MaskingMappingDTO());
    }

    MaskingMappingDTO mapping = new MaskingMappingDTO();
    String maskedText = maskTextInternal(text, mapping);

    // 创建一个只包含文档内容的上下文
    MatterContextDTO context = new MatterContextDTO();
    context.setMasked(true);
    MatterContextDTO.DocumentInfo doc = new MatterContextDTO.DocumentInfo();
    doc.setContent(maskedText);
    context.setDocuments(List.of(doc));

    return new MaskingResult(context, mapping);
  }

  /**
   * 还原脱敏文本 将 AI 生成的脱敏文书还原为包含真实信息的文书。
   *
   * @param maskedText 脱敏后的文本（AI 生成结果）
   * @param mapping 脱敏映射关系
   * @return 还原后的文本
   */
  public String restoreMaskedText(final String maskedText, final MaskingMappingDTO mapping) {
    if (maskedText == null || maskedText.isEmpty()) {
      return maskedText;
    }
    if (mapping == null || mapping.getMappings().isEmpty()) {
      log.warn("没有脱敏映射关系，无法还原");
      return maskedText;
    }

    log.info("开始还原脱敏文本，映射条目数: {}", mapping.getMappings().size());

    String result = maskedText;

    // 按脱敏值长度降序排序，避免短字符串替换导致问题
    List<MaskingMappingDTO.MappingEntry> sortedMappings =
        mapping.getMappings().stream()
            .sorted((a, b) -> b.getMaskedValue().length() - a.getMaskedValue().length())
            .toList();

    for (MaskingMappingDTO.MappingEntry entry : sortedMappings) {
      String maskedValue = entry.getMaskedValue();
      String originalValue = entry.getOriginalValue();

      if (maskedValue != null && originalValue != null && !maskedValue.equals(originalValue)) {
        // 使用精确匹配替换
        result = result.replace(maskedValue, originalValue);
      }
    }

    log.info("脱敏文本还原完成");
    return result;
  }

  // ========== 带映射的脱敏方法 ==========

  /**
   * 对项目信息进行脱敏处理，并记录映射关系。
   *
   * @param info 原始项目信息
   * @param mapping 脱敏映射关系
   * @return 脱敏后的项目信息
   */
  private MatterContextDTO.MatterInfo maskMatterInfoWithMapping(
      final MatterContextDTO.MatterInfo info, final MaskingMappingDTO mapping) {
    MatterContextDTO.MatterInfo masked = new MatterContextDTO.MatterInfo();
    masked.setId(info.getId());
    masked.setMatterNo(info.getMatterNo());
    masked.setName(info.getName());
    masked.setMatterType(info.getMatterType());
    masked.setCaseType(info.getCaseType());
    masked.setStatus(info.getStatus());
    masked.setDescription(maskTextInternal(info.getDescription(), mapping));
    masked.setCourt(info.getCourt());
    masked.setCaseNo(info.getCaseNo());
    masked.setOpposingParty(maskNameWithMapping(info.getOpposingParty(), "对方当事人", mapping));
    masked.setClaimAmount(info.getClaimAmount());
    masked.setDisputeAmount(info.getDisputeAmount());
    return masked;
  }

  /**
   * 对客户信息进行脱敏处理，并记录映射关系。
   *
   * @param info 原始客户信息
   * @param mapping 脱敏映射关系
   * @return 脱敏后的客户信息
   */
  private MatterContextDTO.ClientInfo maskClientInfoWithMapping(
      final MatterContextDTO.ClientInfo info, final MaskingMappingDTO mapping) {
    MatterContextDTO.ClientInfo masked = new MatterContextDTO.ClientInfo();
    masked.setId(info.getId());
    masked.setName(maskNameWithMapping(info.getName(), "客户姓名", mapping));
    masked.setClientType(info.getClientType());
    masked.setCreditCode(maskCreditCodeWithMapping(info.getCreditCode(), mapping));
    masked.setIdCard(maskIdCardWithMapping(info.getIdCard(), mapping));
    masked.setLegalRepresentative(
        maskNameWithMapping(info.getLegalRepresentative(), "法定代表人", mapping));
    masked.setContactPerson(maskNameWithMapping(info.getContactPerson(), "联系人", mapping));
    masked.setContactPhone(maskPhoneWithMapping(info.getContactPhone(), mapping));
    masked.setContactEmail(maskEmailWithMapping(info.getContactEmail(), mapping));
    masked.setRegisteredAddress(maskAddressWithMapping(info.getRegisteredAddress(), mapping));
    masked.setRole(info.getRole());
    masked.setPrimary(info.isPrimary());
    return masked;
  }

  /**
   * 对参与人信息进行脱敏处理，并记录映射关系。
   *
   * @param info 原始参与人信息
   * @param mapping 脱敏映射关系
   * @return 脱敏后的参与人信息
   */
  private MatterContextDTO.ParticipantInfo maskParticipantInfoWithMapping(
      final MatterContextDTO.ParticipantInfo info, final MaskingMappingDTO mapping) {
    MatterContextDTO.ParticipantInfo masked = new MatterContextDTO.ParticipantInfo();
    masked.setUserId(info.getUserId());
    // 律师姓名保留
    masked.setName(info.getName());
    masked.setRole(info.getRole());
    masked.setPhone(maskPhoneWithMapping(info.getPhone(), mapping));
    masked.setEmail(maskEmailWithMapping(info.getEmail(), mapping));
    // 执业证号保留
    masked.setLawyerLicenseNo(info.getLawyerLicenseNo());
    return masked;
  }

  /**
   * 对文档信息进行脱敏处理，并记录映射关系。
   *
   * @param info 原始文档信息
   * @param mapping 脱敏映射关系
   * @return 脱敏后的文档信息
   */
  private MatterContextDTO.DocumentInfo maskDocumentInfoWithMapping(
      final MatterContextDTO.DocumentInfo info, final MaskingMappingDTO mapping) {
    MatterContextDTO.DocumentInfo masked = new MatterContextDTO.DocumentInfo();
    masked.setId(info.getId());
    masked.setTitle(info.getTitle());
    masked.setFileName(info.getFileName());
    masked.setFileType(info.getFileType());
    masked.setCategory(info.getCategory());
    masked.setDescription(info.getDescription());
    // 文档内容脱敏
    masked.setContent(maskTextInternal(info.getContent(), mapping));
    return masked;
  }

  // ========== 具体脱敏方法（带映射记录） ==========

  /**
   * 对姓名进行脱敏处理，并记录映射关系。
   *
   * @param name 原始姓名
   * @param fieldName 字段名称
   * @param mapping 脱敏映射关系
   * @return 脱敏后的姓名
   */
  private String maskNameWithMapping(
      final String name, final String fieldName, final MaskingMappingDTO mapping) {
    if (name == null || name.isEmpty()) {
      return name;
    }
    String masked = maskName(name);
    if (!masked.equals(name)) {
      mapping.addMapping(fieldName, name, masked);
    }
    return masked;
  }

  /**
   * 对身份证号进行脱敏处理，并记录映射关系。
   *
   * @param idCard 原始身份证号
   * @param mapping 脱敏映射关系
   * @return 脱敏后的身份证号
   */
  private String maskIdCardWithMapping(final String idCard, final MaskingMappingDTO mapping) {
    if (idCard == null || idCard.isEmpty()) {
      return idCard;
    }
    String masked = maskIdCard(idCard);
    if (!masked.equals(idCard)) {
      mapping.addMapping("身份证号", idCard, masked);
    }
    return masked;
  }

  /**
   * 对手机号进行脱敏处理，并记录映射关系。
   *
   * @param phone 原始手机号
   * @param mapping 脱敏映射关系
   * @return 脱敏后的手机号
   */
  private String maskPhoneWithMapping(final String phone, final MaskingMappingDTO mapping) {
    if (phone == null || phone.isEmpty()) {
      return phone;
    }
    String masked = maskPhone(phone);
    if (!masked.equals(phone)) {
      mapping.addMapping("手机号", phone, masked);
    }
    return masked;
  }

  /**
   * 对邮箱进行脱敏处理，并记录映射关系。
   *
   * @param email 原始邮箱
   * @param mapping 脱敏映射关系
   * @return 脱敏后的邮箱
   */
  private String maskEmailWithMapping(final String email, final MaskingMappingDTO mapping) {
    if (email == null || email.isEmpty()) {
      return email;
    }
    String masked = maskEmail(email);
    if (!masked.equals(email)) {
      mapping.addMapping("邮箱", email, masked);
    }
    return masked;
  }

  /**
   * 对统一社会信用代码进行脱敏处理，并记录映射关系。
   *
   * @param creditCode 原始统一社会信用代码
   * @param mapping 脱敏映射关系
   * @return 脱敏后的统一社会信用代码
   */
  private String maskCreditCodeWithMapping(
      final String creditCode, final MaskingMappingDTO mapping) {
    if (creditCode == null || creditCode.isEmpty()) {
      return creditCode;
    }
    String masked = maskCreditCode(creditCode);
    if (!masked.equals(creditCode)) {
      mapping.addMapping("统一社会信用代码", creditCode, masked);
    }
    return masked;
  }

  /**
   * 对地址进行脱敏处理，并记录映射关系。
   *
   * @param address 原始地址
   * @param mapping 脱敏映射关系
   * @return 脱敏后的地址
   */
  private String maskAddressWithMapping(final String address, final MaskingMappingDTO mapping) {
    if (address == null || address.isEmpty()) {
      return address;
    }
    String masked = maskAddress(address);
    if (!masked.equals(address)) {
      mapping.addMapping("地址", address, masked);
    }
    return masked;
  }

  /**
   * 对文本中的敏感信息进行脱敏，并记录映射。
   *
   * <p>脱敏顺序很重要：先处理长模式，再处理短模式，避免误匹配
   *
   * @param text 原始文本
   * @param mapping 脱敏映射关系
   * @return 脱敏后的文本
   */
  private String maskTextInternal(final String text, final MaskingMappingDTO mapping) {
    if (text == null || text.isEmpty()) {
      return text;
    }

    String result = text;

    // 1. 脱敏身份证号（18位，优先处理）
    result = maskPatternInText(result, ID_CARD_PATTERN, "身份证号", "$1********$2", mapping);

    // 2. 脱敏银行卡号（16-19位）
    result = maskPatternInText(result, BANK_CARD_PATTERN, "银行卡号", "$1********$2", mapping);

    // 3. 脱敏统一社会信用代码（在文本中识别）
    result = maskPatternInText(result, CREDIT_CODE_PATTERN, "统一社会信用代码", "$1**********$2", mapping);

    // 4. 脱敏护照号码
    result = maskPatternInText(result, PASSPORT_PATTERN, "护照号码", "$1****$2", mapping);

    // 5. 脱敏港澳通行证
    result = maskPatternInText(result, HK_MACAO_PERMIT_PATTERN, "港澳通行证", "$1****$2", mapping);

    // 6. 脱敏台湾通行证
    result = maskPatternInText(result, TAIWAN_PERMIT_PATTERN, "台湾通行证", "$1****$2", mapping);

    // 7. 脱敏军官证
    result = maskPatternInText(result, MILITARY_ID_PATTERN, "军官证号", "$1$2****$3", mapping);

    // 8. 脱敏房产证号
    result = maskPatternInText(result, PROPERTY_CERT_PATTERN, "不动产权证号", "$1$2$3****$4号", mapping);

    // 9. 脱敏车牌号
    result = maskPatternInText(result, LICENSE_PLATE_PATTERN, "车牌号", "$1***$2", mapping);

    // 10. 脱敏固定电话
    result = maskPatternInText(result, LANDLINE_PATTERN, "固定电话", "$1-****$3", mapping);

    // 11. 脱敏手机号（11位）
    result = maskPatternInText(result, PHONE_PATTERN, "手机号", "$1****$2", mapping);

    // 12. 脱敏邮箱
    result = maskPatternInText(result, EMAIL_PATTERN, "邮箱", "$1***@$2", mapping);

    // 13. 脱敏IP地址
    result = maskPatternInText(result, IP_ADDRESS_PATTERN, "IP地址", "$1.$2.***.***", mapping);

    return result;
  }

  /**
   * 通用的文本脱敏方法。
   *
   * @param text 原始文本
   * @param pattern 匹配模式
   * @param fieldName 字段名称（用于映射记录）
   * @param replacement 替换模式
   * @param mapping 映射记录
   * @return 脱敏后的文本
   */
  private String maskPatternInText(
      final String text,
      final Pattern pattern,
      final String fieldName,
      final String replacement,
      final MaskingMappingDTO mapping) {
    Matcher matcher = pattern.matcher(text);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String original = matcher.group();
      String masked = matcher.replaceFirst(replacement);
      // 重新匹配以获取正确的替换结果
      Matcher singleMatcher = pattern.matcher(original);
      if (singleMatcher.matches()) {
        masked = singleMatcher.replaceAll(replacement);
      }
      if (!masked.equals(original)) {
        mapping.addMapping(fieldName, original, masked);
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
    }
    matcher.appendTail(sb);

    return sb.toString();
  }

  // ========== 原有的脱敏方法 ==========

  /**
   * 脱敏姓名。
   *
   * @param name 原始姓名
   * @return 脱敏后的姓名
   */
  public String maskName(final String name) {
    if (name == null || name.isEmpty()) {
      return name;
    }
    if (name.length() == 1) {
      return "*";
    } else if (name.length() == 2) {
      return name.charAt(0) + "*";
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(name.charAt(0));
      for (int i = 1; i < name.length() - 1; i++) {
        sb.append("*");
      }
      sb.append(name.charAt(name.length() - 1));
      return sb.toString();
    }
  }

  /**
   * 脱敏身份证号。
   *
   * @param idCard 原始身份证号
   * @return 脱敏后的身份证号
   */
  public String maskIdCard(final String idCard) {
    if (idCard == null || idCard.isEmpty()) {
      return idCard;
    }
    return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1********$2");
  }

  /**
   * 脱敏手机号。
   *
   * @param phone 原始手机号
   * @return 脱敏后的手机号
   */
  public String maskPhone(final String phone) {
    if (phone == null || phone.isEmpty()) {
      return phone;
    }
    return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
  }

  /**
   * 脱敏邮箱。
   *
   * @param email 原始邮箱
   * @return 脱敏后的邮箱
   */
  public String maskEmail(final String email) {
    if (email == null || email.isEmpty()) {
      return email;
    }
    return EMAIL_PATTERN.matcher(email).replaceAll("$1***@$2");
  }

  /**
   * 脱敏统一社会信用代码。
   *
   * @param creditCode 原始统一社会信用代码
   * @return 脱敏后的统一社会信用代码
   */
  public String maskCreditCode(final String creditCode) {
    if (creditCode == null || creditCode.isEmpty()) {
      return creditCode;
    }
    return CREDIT_CODE_PATTERN.matcher(creditCode).replaceAll("$1**********$2");
  }

  /**
   * 脱敏银行卡号。
   *
   * @param bankCard 原始银行卡号
   * @return 脱敏后的银行卡号
   */
  public String maskBankCard(final String bankCard) {
    if (bankCard == null || bankCard.isEmpty()) {
      return bankCard;
    }
    return BANK_CARD_PATTERN.matcher(bankCard).replaceAll("$1********$2");
  }

  /**
   * 脱敏地址。
   *
   * @param address 原始地址
   * @return 脱敏后的地址
   */
  public String maskAddress(final String address) {
    if (address == null || address.isEmpty()) {
      return address;
    }
    if (address.length() <= ADDRESS_PREFIX_LEN) {
      return address;
    }
    return address.substring(0, ADDRESS_PREFIX_LEN) + "***";
  }

  /**
   * 脱敏文本（不记录映射）。
   *
   * @param text 原始文本
   * @return 脱敏后的文本
   */
  public String maskText(final String text) {
    MaskingMappingDTO mapping = new MaskingMappingDTO();
    return maskTextInternal(text, mapping);
  }

  /**
   * 检查文本是否包含敏感信息。
   *
   * @param text 待检查的文本
   * @return 如果包含敏感信息返回true，否则返回false
   */
  public boolean containsSensitiveInfo(final String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    return ID_CARD_PATTERN.matcher(text).find()
        || PHONE_PATTERN.matcher(text).find()
        || LANDLINE_PATTERN.matcher(text).find()
        || EMAIL_PATTERN.matcher(text).find()
        || BANK_CARD_PATTERN.matcher(text).find()
        || CREDIT_CODE_PATTERN.matcher(text).find()
        || PASSPORT_PATTERN.matcher(text).find()
        || LICENSE_PLATE_PATTERN.matcher(text).find()
        || HK_MACAO_PERMIT_PATTERN.matcher(text).find()
        || TAIWAN_PERMIT_PATTERN.matcher(text).find()
        || MILITARY_ID_PATTERN.matcher(text).find()
        || PROPERTY_CERT_PATTERN.matcher(text).find()
        || IP_ADDRESS_PATTERN.matcher(text).find();
  }

  // ========== 新增的单独脱敏方法 ==========

  /**
   * 脱敏固定电话。
   *
   * @param landline 原始固定电话
   * @return 脱敏后的固定电话
   */
  public String maskLandline(final String landline) {
    if (landline == null || landline.isEmpty()) {
      return landline;
    }
    return LANDLINE_PATTERN.matcher(landline).replaceAll("$1-****$3");
  }

  /**
   * 脱敏护照号码。
   *
   * @param passport 原始护照号码
   * @return 脱敏后的护照号码
   */
  public String maskPassport(final String passport) {
    if (passport == null || passport.isEmpty()) {
      return passport;
    }
    return PASSPORT_PATTERN.matcher(passport).replaceAll("$1****$2");
  }

  /**
   * 脱敏车牌号。
   *
   * @param plate 原始车牌号
   * @return 脱敏后的车牌号
   */
  public String maskLicensePlate(final String plate) {
    if (plate == null || plate.isEmpty()) {
      return plate;
    }
    return LICENSE_PLATE_PATTERN.matcher(plate).replaceAll("$1***$2");
  }

  /**
   * 脱敏港澳通行证。
   *
   * @param permit 原始港澳通行证号
   * @return 脱敏后的港澳通行证号
   */
  public String maskHkMacaoPermit(final String permit) {
    if (permit == null || permit.isEmpty()) {
      return permit;
    }
    return HK_MACAO_PERMIT_PATTERN.matcher(permit).replaceAll("$1****$2");
  }

  /**
   * 脱敏台湾通行证。
   *
   * @param permit 原始台湾通行证号
   * @return 脱敏后的台湾通行证号
   */
  public String maskTaiwanPermit(final String permit) {
    if (permit == null || permit.isEmpty()) {
      return permit;
    }
    return TAIWAN_PERMIT_PATTERN.matcher(permit).replaceAll("$1****$2");
  }

  /**
   * 脱敏IP地址。
   *
   * @param ip 原始IP地址
   * @return 脱敏后的IP地址
   */
  public String maskIpAddress(final String ip) {
    if (ip == null || ip.isEmpty()) {
      return ip;
    }
    return IP_ADDRESS_PATTERN.matcher(ip).replaceAll("$1.$2.***.***");
  }
}
