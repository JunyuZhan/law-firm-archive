package com.lawfirm.interfaces.rest.ocr;

import com.lawfirm.application.ocr.dto.OcrResultDTO;
import com.lawfirm.application.ocr.service.OcrAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** OCR识别接口 */
@Tag(name = "OCR识别", description = "文档OCR识别相关接口")
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

  /** OCR应用服务 */
  private final OcrAppService ocrAppService;

  /**
   * 通用文字识别
   *
   * @param file 图片文件
   * @return OCR识别结果
   */
  @Operation(summary = "通用文字识别")
  @PostMapping("/text")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "通用文字识别")
  public Result<OcrResultDTO> recognizeText(
      @Parameter(description = "图片文件") @RequestParam("file") final MultipartFile file) {
    FileValidator.validate(file);
    return Result.success(ocrAppService.recognizeText(file));
  }

  /**
   * 通用文字识别（URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Operation(summary = "通用文字识别（URL）")
  @PostMapping("/text/url")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "通用文字识别(URL)")
  public Result<OcrResultDTO> recognizeTextByUrl(
      @Parameter(description = "图片URL") @RequestParam final String imageUrl) {
    return Result.success(ocrAppService.recognizeTextByUrl(imageUrl));
  }

  /**
   * 银行回单识别
   *
   * @param file 银行回单图片
   * @return OCR识别结果
   */
  @Operation(summary = "银行回单识别")
  @PostMapping("/bank-receipt")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "银行回单识别")
  public Result<OcrResultDTO> recognizeBankReceipt(
      @Parameter(description = "银行回单图片") @RequestParam("file") final MultipartFile file) {
    FileValidator.validate(file);
    return Result.success(ocrAppService.recognizeBankReceipt(file));
  }

  /**
   * 银行回单识别（URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Operation(summary = "银行回单识别（URL）")
  @PostMapping("/bank-receipt/url")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "银行回单识别(URL)")
  public Result<OcrResultDTO> recognizeBankReceiptByUrl(
      @Parameter(description = "图片URL") @RequestParam final String imageUrl) {
    return Result.success(ocrAppService.recognizeBankReceiptByUrl(imageUrl));
  }

  /**
   * 身份证识别
   *
   * @param file 身份证图片
   * @param isFront 是否正面
   * @return OCR识别结果
   */
  @Operation(summary = "身份证识别")
  @PostMapping("/id-card")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "身份证识别")
  public Result<OcrResultDTO> recognizeIdCard(
      @Parameter(description = "身份证图片") @RequestParam("file") final MultipartFile file,
      @Parameter(description = "是否正面") @RequestParam(defaultValue = "true") final boolean isFront) {
    FileValidator.validate(file);
    return Result.success(ocrAppService.recognizeIdCard(file, isFront));
  }

  /**
   * 身份证识别（URL）
   *
   * @param imageUrl 图片URL
   * @param isFront 是否正面
   * @return OCR识别结果
   */
  @Operation(summary = "身份证识别（URL）")
  @PostMapping("/id-card/url")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "身份证识别(URL)")
  public Result<OcrResultDTO> recognizeIdCardByUrl(
      @Parameter(description = "图片URL") @RequestParam final String imageUrl,
      @Parameter(description = "是否正面") @RequestParam(defaultValue = "true") final boolean isFront) {
    return Result.success(ocrAppService.recognizeIdCardByUrl(imageUrl, isFront));
  }

  /**
   * 营业执照识别
   *
   * @param file 营业执照图片
   * @return OCR识别结果
   */
  @Operation(summary = "营业执照识别")
  @PostMapping("/business-license")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "营业执照识别")
  public Result<OcrResultDTO> recognizeBusinessLicense(
      @Parameter(description = "营业执照图片") @RequestParam("file") final MultipartFile file) {
    FileValidator.validate(file);
    return Result.success(ocrAppService.recognizeBusinessLicense(file));
  }

  /**
   * 营业执照识别（URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Operation(summary = "营业执照识别（URL）")
  @PostMapping("/business-license/url")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "营业执照识别(URL)")
  public Result<OcrResultDTO> recognizeBusinessLicenseByUrl(
      @Parameter(description = "图片URL") @RequestParam final String imageUrl) {
    return Result.success(ocrAppService.recognizeBusinessLicenseByUrl(imageUrl));
  }

  /**
   * 名片识别
   *
   * @param file 名片图片
   * @return OCR识别结果
   */
  @Operation(summary = "名片识别")
  @PostMapping("/business-card")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "名片识别")
  public Result<OcrResultDTO> recognizeBusinessCard(
      @Parameter(description = "名片图片") @RequestParam("file") final MultipartFile file) {
    FileValidator.validate(file);
    return Result.success(ocrAppService.recognizeBusinessCard(file));
  }

  /**
   * 名片识别（URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Operation(summary = "名片识别（URL）")
  @PostMapping("/business-card/url")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "名片识别(URL)")
  public Result<OcrResultDTO> recognizeBusinessCardByUrl(
      @Parameter(description = "图片URL") @RequestParam final String imageUrl) {
    return Result.success(ocrAppService.recognizeBusinessCardByUrl(imageUrl));
  }

  /**
   * 发票/票据识别
   *
   * @param file 发票图片
   * @return OCR识别结果
   */
  @Operation(summary = "发票/票据识别")
  @PostMapping("/invoice")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "发票识别")
  public Result<OcrResultDTO> recognizeInvoice(
      @Parameter(description = "发票图片") @RequestParam("file") final MultipartFile file) {
    FileValidator.validate(file);
    return Result.success(ocrAppService.recognizeInvoice(file));
  }

  /**
   * 发票/票据识别（URL）
   *
   * @param imageUrl 图片URL
   * @return OCR识别结果
   */
  @Operation(summary = "发票/票据识别（URL）")
  @PostMapping("/invoice/url")
  @RequirePermission("ocr:use")
  @OperationLog(module = "OCR识别", action = "发票识别(URL)")
  public Result<OcrResultDTO> recognizeInvoiceByUrl(
      @Parameter(description = "图片URL") @RequestParam final String imageUrl) {
    return Result.success(ocrAppService.recognizeInvoiceByUrl(imageUrl));
  }
}
