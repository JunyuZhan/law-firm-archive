package com.lawfirm.infrastructure.ocr;

import org.springframework.web.multipart.MultipartFile;

/**
 * OCR识别服务接口
 */
public interface OcrService {

    /**
     * 通用文字识别
     */
    OcrResult recognizeText(MultipartFile file);

    /**
     * 通用文字识别（通过URL）
     */
    OcrResult recognizeText(String imageUrl);

    /**
     * 银行回单识别
     */
    OcrResult recognizeBankReceipt(MultipartFile file);

    /**
     * 银行回单识别（通过URL）
     */
    OcrResult recognizeBankReceipt(String imageUrl);

    /**
     * 身份证识别
     */
    OcrResult recognizeIdCard(MultipartFile file, boolean isFront);

    /**
     * 身份证识别（通过URL）
     */
    OcrResult recognizeIdCard(String imageUrl, boolean isFront);

    /**
     * 营业执照识别
     */
    OcrResult recognizeBusinessLicense(MultipartFile file);

    /**
     * 营业执照识别（通过URL）
     */
    OcrResult recognizeBusinessLicense(String imageUrl);
}
