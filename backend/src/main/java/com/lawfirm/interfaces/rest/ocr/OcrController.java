package com.lawfirm.interfaces.rest.ocr;

import com.lawfirm.application.ocr.dto.OcrResultDTO;
import com.lawfirm.application.ocr.service.OcrAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR识别接口
 */
@Tag(name = "OCR识别", description = "文档OCR识别相关接口")
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrAppService ocrAppService;

    @Operation(summary = "通用文字识别")
    @PostMapping("/text")
    @OperationLog(module = "OCR识别", action = "通用文字识别")
    public Result<OcrResultDTO> recognizeText(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
        return Result.success(ocrAppService.recognizeText(file));
    }

    @Operation(summary = "通用文字识别（URL）")
    @PostMapping("/text/url")
    @OperationLog(module = "OCR识别", action = "通用文字识别(URL)")
    public Result<OcrResultDTO> recognizeTextByUrl(
            @Parameter(description = "图片URL") @RequestParam String imageUrl) {
        return Result.success(ocrAppService.recognizeTextByUrl(imageUrl));
    }

    @Operation(summary = "银行回单识别")
    @PostMapping("/bank-receipt")
    @OperationLog(module = "OCR识别", action = "银行回单识别")
    public Result<OcrResultDTO> recognizeBankReceipt(
            @Parameter(description = "银行回单图片") @RequestParam("file") MultipartFile file) {
        return Result.success(ocrAppService.recognizeBankReceipt(file));
    }

    @Operation(summary = "银行回单识别（URL）")
    @PostMapping("/bank-receipt/url")
    @OperationLog(module = "OCR识别", action = "银行回单识别(URL)")
    public Result<OcrResultDTO> recognizeBankReceiptByUrl(
            @Parameter(description = "图片URL") @RequestParam String imageUrl) {
        return Result.success(ocrAppService.recognizeBankReceiptByUrl(imageUrl));
    }

    @Operation(summary = "身份证识别")
    @PostMapping("/id-card")
    @OperationLog(module = "OCR识别", action = "身份证识别")
    public Result<OcrResultDTO> recognizeIdCard(
            @Parameter(description = "身份证图片") @RequestParam("file") MultipartFile file,
            @Parameter(description = "是否正面") @RequestParam(defaultValue = "true") boolean isFront) {
        return Result.success(ocrAppService.recognizeIdCard(file, isFront));
    }

    @Operation(summary = "身份证识别（URL）")
    @PostMapping("/id-card/url")
    @OperationLog(module = "OCR识别", action = "身份证识别(URL)")
    public Result<OcrResultDTO> recognizeIdCardByUrl(
            @Parameter(description = "图片URL") @RequestParam String imageUrl,
            @Parameter(description = "是否正面") @RequestParam(defaultValue = "true") boolean isFront) {
        return Result.success(ocrAppService.recognizeIdCardByUrl(imageUrl, isFront));
    }

    @Operation(summary = "营业执照识别")
    @PostMapping("/business-license")
    @OperationLog(module = "OCR识别", action = "营业执照识别")
    public Result<OcrResultDTO> recognizeBusinessLicense(
            @Parameter(description = "营业执照图片") @RequestParam("file") MultipartFile file) {
        return Result.success(ocrAppService.recognizeBusinessLicense(file));
    }

    @Operation(summary = "营业执照识别（URL）")
    @PostMapping("/business-license/url")
    @OperationLog(module = "OCR识别", action = "营业执照识别(URL)")
    public Result<OcrResultDTO> recognizeBusinessLicenseByUrl(
            @Parameter(description = "图片URL") @RequestParam String imageUrl) {
        return Result.success(ocrAppService.recognizeBusinessLicenseByUrl(imageUrl));
    }
}
