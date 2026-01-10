package com.lawfirm.interfaces.rest.open;

import com.lawfirm.application.admin.service.LetterVerificationService;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.admin.entity.LetterApplication;
import com.lawfirm.domain.admin.repository.LetterApplicationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

/**
 * 公开验证接口
 * 用于向社会公众提供函件、合同等业务对象的真伪验证服务
 * 
 * 注意：此接口不需要认证，面向公众开放
 * 
 * @author LawFirm
 */
@Slf4j
@Tag(name = "公开验证", description = "函件、合同等业务对象真伪验证接口（公开访问）")
@RestController
@RequestMapping("/open/verify")
@RequiredArgsConstructor
public class PublicVerificationController {

    private final LetterVerificationService letterVerificationService;
    private final LetterApplicationRepository letterApplicationRepository;

    /**
     * 验证函件真伪
     * 
     * @param no 函件申请编号
     * @param code 验证码
     * @return 验证结果
     */
    @Operation(summary = "验证函件真伪", description = "通过申请编号和验证码验证函件真伪")
    @GetMapping("/letter")
    public Result<LetterVerificationResult> verifyLetter(
            @RequestParam("no") String applicationNo,
            @RequestParam("code") String verificationCode) {
        
        try {
            boolean isValid = letterVerificationService.verifyCode(applicationNo, verificationCode);
            
            LetterVerificationResult result = new LetterVerificationResult();
            result.setValid(isValid);
            result.setApplicationNo(applicationNo);
            
            if (isValid) {
                // 验证通过，返回函件基本信息（脱敏）
                LetterApplication application = letterApplicationRepository.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LetterApplication>()
                        .eq(LetterApplication::getApplicationNo, applicationNo)
                        .eq(LetterApplication::getDeleted, false),
                    false  // 不抛出异常，返回null
                );
                
                if (application != null) {
                    result.setLetterTypeName(getLetterTypeName(application.getLetterType()));
                    result.setTargetUnit(application.getTargetUnit());
                    result.setLawyerNames(application.getLawyerNames());
                    if (application.getApprovedAt() != null) {
                        result.setApprovedAt(application.getApprovedAt()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    if (application.getPrintedAt() != null) {
                        result.setPrintedAt(application.getPrintedAt()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    result.setMessage("该函件真实有效");
                }
            } else {
                result.setMessage("验证失败，该函件可能无效或验证码错误");
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证函件失败: applicationNo={}, error={}", applicationNo, e.getMessage(), e);
            LetterVerificationResult result = new LetterVerificationResult();
            result.setValid(false);
            result.setApplicationNo(applicationNo);
            result.setMessage("验证失败：" + e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 函件验证结果DTO
     */
    @Data
    public static class LetterVerificationResult {
        /** 是否有效 */
        private Boolean valid;
        
        /** 申请编号 */
        private String applicationNo;
        
        /** 函件类型名称 */
        private String letterTypeName;
        
        /** 接收单位 */
        private String targetUnit;
        
        /** 出函律师 */
        private String lawyerNames;
        
        /** 审批时间 */
        private String approvedAt;
        
        /** 打印时间 */
        private String printedAt;
        
        /** 验证消息 */
        private String message;
    }

    /**
     * 获取函件类型名称（简化版，实际应该从字典或服务获取）
     */
    private String getLetterTypeName(String letterType) {
        if (letterType == null) return "函件";
        return switch (letterType) {
            case "INTRODUCTION" -> "介绍信";
            case "MEETING" -> "会见函";
            case "INVESTIGATION" -> "调查函";
            case "FILE_REVIEW" -> "阅卷函";
            case "LEGAL_OPINION" -> "法律意见函";
            default -> "函件";
        };
    }
}

