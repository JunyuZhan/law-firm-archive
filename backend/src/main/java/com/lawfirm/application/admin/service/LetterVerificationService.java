package com.lawfirm.application.admin.service;

import com.lawfirm.domain.admin.entity.LetterApplication;
import com.lawfirm.domain.admin.repository.LetterApplicationRepository;
import com.lawfirm.infrastructure.qrcode.QrCodeService;
import com.lawfirm.infrastructure.qrcode.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 函件验证服务
 * 基于基础设施层的二维码和验证码服务实现函件防伪验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LetterVerificationService {

    private final LetterApplicationRepository letterApplicationRepository;
    private final VerificationCodeService verificationCodeService;
    private final QrCodeService qrCodeService;

    /**
     * 公开验证网站基础URL（从配置读取）
     */
    @Value("${lawfirm.public.verify.url:https://verify.lawfirm.com}")
    private String publicVerifyUrl;

    /**
     * 生成函件验证码
     * 
     * @param application 函件申请
     * @return 验证码（Base64编码）
     */
    @Transactional
    public String generateVerificationCode(LetterApplication application) {
        // 如果已经打印过，使用打印时间；否则使用当前时间
        LocalDateTime timestamp = application.getPrintedAt() != null 
            ? application.getPrintedAt() 
            : LocalDateTime.now();
        
        return verificationCodeService.generateCode(
            VerificationCodeService.BUSINESS_TYPE_LETTER,
            application.getId(),
            application.getApplicationNo(),
            timestamp
        );
    }

    /**
     * 验证函件验证码
     * 
     * @param applicationNo 申请编号
     * @param verificationCode 验证码
     * @return 是否有效
     */
    public boolean verifyCode(String applicationNo, String verificationCode) {
        LetterApplication application = letterApplicationRepository.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LetterApplication>()
                .eq(LetterApplication::getApplicationNo, applicationNo)
                .eq(LetterApplication::getDeleted, false),
            false  // 不抛出异常，返回null
        );
        
        if (application == null) {
            log.warn("函件不存在: applicationNo={}", applicationNo);
            return false;
        }
        
        LocalDateTime timestamp = application.getPrintedAt() != null 
            ? application.getPrintedAt() 
            : application.getCreatedAt();
        
        return verificationCodeService.verifyCode(
            VerificationCodeService.BUSINESS_TYPE_LETTER,
            application.getId(),
            applicationNo,
            timestamp,
            verificationCode
        );
    }

    /**
     * 生成验证二维码URL
     * 
     * @param application 函件申请
     * @return 验证URL
     */
    public String generateVerificationUrl(LetterApplication application) {
        String verificationCode = generateVerificationCode(application);
        return verificationCodeService.generateVerificationUrl(
            publicVerifyUrl,
            VerificationCodeService.BUSINESS_TYPE_LETTER,
            application.getApplicationNo(),
            verificationCode
        );
    }

    /**
     * 生成验证二维码图片（Base64编码）
     * 
     * @param application 函件申请
     * @param size 二维码尺寸（像素），默认200
     * @return Base64编码的二维码图片
     */
    public String generateQrCodeBase64(LetterApplication application, Integer size) {
        String verificationUrl = generateVerificationUrl(application);
        return qrCodeService.generateQrCodeBase64(verificationUrl, size);
    }

    /**
     * 生成验证二维码图片（字节数组）
     * 
     * @param application 函件申请
     * @param size 二维码尺寸（像素），默认200
     * @return PNG格式的字节数组
     */
    public byte[] generateQrCodeBytes(LetterApplication application, Integer size) {
        String verificationUrl = generateVerificationUrl(application);
        return qrCodeService.generateQrCodeBytes(verificationUrl, size);
    }
}

