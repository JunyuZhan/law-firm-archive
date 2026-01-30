package com.lawfirm.infrastructure.ocr;

import org.springframework.web.multipart.MultipartFile;

/** OCR识别服务接口 */
public interface OcrService {

  /**
   * 通用文字识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  OcrResult recognizeText(MultipartFile file);

  /**
   * 通用文字识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  OcrResult recognizeText(String imageUrl);

  /**
   * 银行回单识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  OcrResult recognizeBankReceipt(MultipartFile file);

  /**
   * 银行回单识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  OcrResult recognizeBankReceipt(String imageUrl);

  /**
   * 身份证识别
   *
   * @param file 图片文件
   * @param isFront 是否为正面
   * @return OCR识别结果
   */
  OcrResult recognizeIdCard(MultipartFile file, boolean isFront);

  /**
   * 身份证识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @param isFront 是否为正面
   * @return OCR识别结果
   */
  OcrResult recognizeIdCard(String imageUrl, boolean isFront);

  /**
   * 营业执照识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  OcrResult recognizeBusinessLicense(MultipartFile file);

  /**
   * 营业执照识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  OcrResult recognizeBusinessLicense(String imageUrl);

  /**
   * 名片识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  OcrResult recognizeBusinessCard(MultipartFile file);

  /**
   * 名片识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  OcrResult recognizeBusinessCard(String imageUrl);

  /**
   * 发票/票据识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  OcrResult recognizeInvoice(MultipartFile file);

  /**
   * 发票/票据识别（通过URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  OcrResult recognizeInvoice(String imageUrl);
}
