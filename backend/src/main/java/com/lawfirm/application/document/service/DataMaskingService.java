package com.lawfirm.application.document.service;

import com.lawfirm.application.document.dto.MaskingMappingDTO;
import com.lawfirm.application.document.dto.MatterContextDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务
 * 用于将敏感信息进行脱敏处理，保护隐私数据
 * 支持脱敏还原：生成时保存映射关系，生成后可还原
 */
@Slf4j
@Service
public class DataMaskingService {

    // 身份证号正则
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "(\\d{6})\\d{8}(\\d{4})");
    
    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\d{3})\\d{4}(\\d{4})");
    
    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(\\w{1,3})\\w*@(\\w+\\.\\w+)");
    
    // 银行卡号正则
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile(
            "(\\d{4})\\d{8,12}(\\d{4})");
    
    // 统一社会信用代码正则
    private static final Pattern CREDIT_CODE_PATTERN = Pattern.compile(
            "(\\w{4})\\w{10}(\\w{4})");

    /**
     * 脱敏结果包装类
     * 包含脱敏后的上下文和映射关系
     */
    public static class MaskingResult {
        private final MatterContextDTO maskedContext;
        private final MaskingMappingDTO mapping;

        public MaskingResult(MatterContextDTO maskedContext, MaskingMappingDTO mapping) {
            this.maskedContext = maskedContext;
            this.mapping = mapping;
        }

        public MatterContextDTO getMaskedContext() {
            return maskedContext;
        }

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
    public MaskingResult maskContextWithMapping(MatterContextDTO context) {
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
            masked.setClients(context.getClients().stream()
                    .map(c -> maskClientInfoWithMapping(c, mapping))
                    .toList());
        }
        
        // 3. 脱敏参与人信息
        if (context.getParticipants() != null) {
            masked.setParticipants(context.getParticipants().stream()
                    .map(p -> maskParticipantInfoWithMapping(p, mapping))
                    .toList());
        }
        
        // 4. 文档信息
        if (context.getDocuments() != null) {
            masked.setDocuments(context.getDocuments().stream()
                    .map(d -> maskDocumentInfoWithMapping(d, mapping))
                    .toList());
        }
        
        log.info("项目上下文脱敏完成，映射条目数: {}", mapping.getMappings().size());
        return new MaskingResult(masked, mapping);
    }

    /**
     * 对项目上下文进行脱敏处理（不返回映射关系）
     */
    public MatterContextDTO maskContext(MatterContextDTO context) {
        return maskContextWithMapping(context).getMaskedContext();
    }

    /**
     * 对文本进行脱敏处理，并返回映射关系
     */
    public MaskingResult maskTextWithMapping(String text) {
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
     * 还原脱敏文本
     * 将 AI 生成的脱敏文书还原为包含真实信息的文书
     * 
     * @param maskedText 脱敏后的文本（AI 生成结果）
     * @param mapping 脱敏映射关系
     * @return 还原后的文本
     */
    public String restoreMaskedText(String maskedText, MaskingMappingDTO mapping) {
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
        List<MaskingMappingDTO.MappingEntry> sortedMappings = mapping.getMappings().stream()
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

    private MatterContextDTO.MatterInfo maskMatterInfoWithMapping(
            MatterContextDTO.MatterInfo info, MaskingMappingDTO mapping) {
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

    private MatterContextDTO.ClientInfo maskClientInfoWithMapping(
            MatterContextDTO.ClientInfo info, MaskingMappingDTO mapping) {
        MatterContextDTO.ClientInfo masked = new MatterContextDTO.ClientInfo();
        masked.setId(info.getId());
        masked.setName(maskNameWithMapping(info.getName(), "客户姓名", mapping));
        masked.setClientType(info.getClientType());
        masked.setCreditCode(maskCreditCodeWithMapping(info.getCreditCode(), mapping));
        masked.setIdCard(maskIdCardWithMapping(info.getIdCard(), mapping));
        masked.setLegalRepresentative(maskNameWithMapping(info.getLegalRepresentative(), "法定代表人", mapping));
        masked.setContactPerson(maskNameWithMapping(info.getContactPerson(), "联系人", mapping));
        masked.setContactPhone(maskPhoneWithMapping(info.getContactPhone(), mapping));
        masked.setContactEmail(maskEmailWithMapping(info.getContactEmail(), mapping));
        masked.setRegisteredAddress(maskAddressWithMapping(info.getRegisteredAddress(), mapping));
        masked.setRole(info.getRole());
        masked.setPrimary(info.isPrimary());
        return masked;
    }

    private MatterContextDTO.ParticipantInfo maskParticipantInfoWithMapping(
            MatterContextDTO.ParticipantInfo info, MaskingMappingDTO mapping) {
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

    private MatterContextDTO.DocumentInfo maskDocumentInfoWithMapping(
            MatterContextDTO.DocumentInfo info, MaskingMappingDTO mapping) {
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

    private String maskNameWithMapping(String name, String fieldName, MaskingMappingDTO mapping) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String masked = maskName(name);
        if (!masked.equals(name)) {
            mapping.addMapping(fieldName, name, masked);
        }
        return masked;
    }

    private String maskIdCardWithMapping(String idCard, MaskingMappingDTO mapping) {
        if (idCard == null || idCard.isEmpty()) {
            return idCard;
        }
        String masked = maskIdCard(idCard);
        if (!masked.equals(idCard)) {
            mapping.addMapping("身份证号", idCard, masked);
        }
        return masked;
    }

    private String maskPhoneWithMapping(String phone, MaskingMappingDTO mapping) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        String masked = maskPhone(phone);
        if (!masked.equals(phone)) {
            mapping.addMapping("手机号", phone, masked);
        }
        return masked;
    }

    private String maskEmailWithMapping(String email, MaskingMappingDTO mapping) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        String masked = maskEmail(email);
        if (!masked.equals(email)) {
            mapping.addMapping("邮箱", email, masked);
        }
        return masked;
    }

    private String maskCreditCodeWithMapping(String creditCode, MaskingMappingDTO mapping) {
        if (creditCode == null || creditCode.isEmpty()) {
            return creditCode;
        }
        String masked = maskCreditCode(creditCode);
        if (!masked.equals(creditCode)) {
            mapping.addMapping("统一社会信用代码", creditCode, masked);
        }
        return masked;
    }

    private String maskAddressWithMapping(String address, MaskingMappingDTO mapping) {
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
     * 对文本中的敏感信息进行脱敏，并记录映射
     */
    private String maskTextInternal(String text, MaskingMappingDTO mapping) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        
        // 脱敏身份证号
        Matcher idMatcher = ID_CARD_PATTERN.matcher(text);
        while (idMatcher.find()) {
            String original = idMatcher.group();
            String masked = original.replaceAll(ID_CARD_PATTERN.pattern(), "$1********$2");
            if (!masked.equals(original)) {
                mapping.addMapping("身份证号", original, masked);
            }
        }
        result = ID_CARD_PATTERN.matcher(result).replaceAll("$1********$2");
        
        // 脱敏手机号
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            String original = phoneMatcher.group();
            String masked = original.replaceAll(PHONE_PATTERN.pattern(), "$1****$2");
            if (!masked.equals(original)) {
                mapping.addMapping("手机号", original, masked);
            }
        }
        result = PHONE_PATTERN.matcher(result).replaceAll("$1****$2");
        
        // 脱敏邮箱
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            String original = emailMatcher.group();
            String masked = original.replaceAll(EMAIL_PATTERN.pattern(), "$1***@$2");
            if (!masked.equals(original)) {
                mapping.addMapping("邮箱", original, masked);
            }
        }
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1***@$2");
        
        // 脱敏银行卡号
        Matcher bankMatcher = BANK_CARD_PATTERN.matcher(text);
        while (bankMatcher.find()) {
            String original = bankMatcher.group();
            String masked = original.replaceAll(BANK_CARD_PATTERN.pattern(), "$1********$2");
            if (!masked.equals(original)) {
                mapping.addMapping("银行卡号", original, masked);
            }
        }
        result = BANK_CARD_PATTERN.matcher(result).replaceAll("$1********$2");
        
        return result;
    }

    // ========== 原有的脱敏方法 ==========

    public String maskName(String name) {
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

    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return idCard;
        }
        return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1********$2");
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    public String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        return EMAIL_PATTERN.matcher(email).replaceAll("$1***@$2");
    }

    public String maskCreditCode(String creditCode) {
        if (creditCode == null || creditCode.isEmpty()) {
            return creditCode;
        }
        return CREDIT_CODE_PATTERN.matcher(creditCode).replaceAll("$1**********$2");
    }

    public String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.isEmpty()) {
            return bankCard;
        }
        return BANK_CARD_PATTERN.matcher(bankCard).replaceAll("$1********$2");
    }

    public String maskAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        if (address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "***";
    }

    public String maskText(String text) {
        MaskingMappingDTO mapping = new MaskingMappingDTO();
        return maskTextInternal(text, mapping);
    }

    public boolean containsSensitiveInfo(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return ID_CARD_PATTERN.matcher(text).find() ||
               PHONE_PATTERN.matcher(text).find() ||
               EMAIL_PATTERN.matcher(text).find() ||
               BANK_CARD_PATTERN.matcher(text).find() ||
               CREDIT_CODE_PATTERN.matcher(text).find();
    }
}
