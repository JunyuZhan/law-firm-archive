package com.lawfirm.infrastructure.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PaddleOCR服务实现
 * 调用独立部署的PaddleOCR Python服务
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ocr.provider", havingValue = "paddle")
public class PaddleOcrService implements OcrService {

    @Value("${ocr.paddle.url:http://localhost:8001}")
    private String paddleOcrUrl;

    @Value("${ocr.timeout:120000}")
    private int timeout;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PaddleOcrService() {
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        // 配置RestTemplate超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时10秒
        factory.setReadTimeout(timeout); // 读取超时（OCR识别可能需要较长时间，默认120秒）
        
        this.restTemplate = new RestTemplate(factory);
        log.info("PaddleOCR服务初始化完成，超时时间: {}ms", timeout);
    }

    @Override
    public OcrResult recognizeText(MultipartFile file) {
        try {
            String result = callPaddleOcr(file, "/ocr/general");
            return parseGeneralResult(result);
        } catch (Exception e) {
            log.error("通用文字识别失败", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeText(String imageUrl) {
        try {
            String result = callPaddleOcrByUrl(imageUrl, "/ocr/general");
            return parseGeneralResult(result);
        } catch (Exception e) {
            log.error("通用文字识别失败(URL)", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeBankReceipt(MultipartFile file) {
        try {
            String result = callPaddleOcr(file, "/ocr/bank_receipt");
            return parseBankReceiptResult(result);
        } catch (Exception e) {
            log.error("银行回单识别失败", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeBankReceipt(String imageUrl) {
        try {
            String result = callPaddleOcrByUrl(imageUrl, "/ocr/bank_receipt");
            return parseBankReceiptResult(result);
        } catch (Exception e) {
            log.error("银行回单识别失败(URL)", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeIdCard(MultipartFile file, boolean isFront) {
        try {
            String endpoint = isFront ? "/ocr/idcard_front" : "/ocr/idcard_back";
            String result = callPaddleOcr(file, endpoint);
            return parseIdCardResult(result, isFront);
        } catch (Exception e) {
            log.error("身份证识别失败", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeIdCard(String imageUrl, boolean isFront) {
        try {
            String endpoint = isFront ? "/ocr/idcard_front" : "/ocr/idcard_back";
            String result = callPaddleOcrByUrl(imageUrl, endpoint);
            return parseIdCardResult(result, isFront);
        } catch (Exception e) {
            log.error("身份证识别失败(URL)", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeBusinessLicense(MultipartFile file) {
        try {
            String result = callPaddleOcr(file, "/ocr/business_license");
            return parseBusinessLicenseResult(result);
        } catch (Exception e) {
            log.error("营业执照识别失败", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeBusinessLicense(String imageUrl) {
        try {
            String result = callPaddleOcrByUrl(imageUrl, "/ocr/business_license");
            return parseBusinessLicenseResult(result);
        } catch (Exception e) {
            log.error("营业执照识别失败(URL)", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 调用PaddleOCR服务（文件上传）
     */
    private String callPaddleOcr(MultipartFile file, String endpoint) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                paddleOcrUrl + endpoint,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("OCR服务返回错误: " + response.getStatusCode());
        }
    }

    /**
     * 调用PaddleOCR服务（URL方式）
     */
    private String callPaddleOcrByUrl(String imageUrl, String endpoint) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("image_url", imageUrl);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                paddleOcrUrl + endpoint,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("OCR服务返回错误: " + response.getStatusCode());
        }
    }


    /**
     * 解析通用文字识别结果
     */
    private OcrResult parseGeneralResult(String jsonResult) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResult);
        
        StringBuilder textBuilder = new StringBuilder();
        JsonNode results = root.path("result");
        double totalConfidence = 0;
        int count = 0;
        
        if (results.isArray()) {
            for (JsonNode item : results) {
                String text = item.path("text").asText("");
                double confidence = item.path("confidence").asDouble(0);
                textBuilder.append(text).append("\n");
                totalConfidence += confidence;
                count++;
            }
        }

        return OcrResult.builder()
                .success(true)
                .type("TEXT")
                .rawText(textBuilder.toString().trim())
                .confidence(count > 0 ? totalConfidence / count : 0)
                .build();
    }

    /**
     * 解析银行回单识别结果
     */
    private OcrResult parseBankReceiptResult(String jsonResult) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResult);
        JsonNode data = root.path("result");
        
        Map<String, Object> resultData = new HashMap<>();
        
        String bankName = data.path("bank_name").asText("");
        String amountStr = data.path("amount").asText("0");
        String dateStr = data.path("transaction_date").asText("");
        String payerName = data.path("payer_name").asText("");
        String payerAccount = data.path("payer_account").asText("");
        String payeeName = data.path("payee_name").asText("");
        String payeeAccount = data.path("payee_account").asText("");
        String transactionNo = data.path("transaction_no").asText("");
        String remark = data.path("remark").asText("");
        
        resultData.put("bankName", bankName);
        resultData.put("amount", amountStr);
        resultData.put("transactionDate", dateStr);
        resultData.put("payerName", payerName);
        resultData.put("payerAccount", payerAccount);
        resultData.put("payeeName", payeeName);
        resultData.put("payeeAccount", payeeAccount);
        resultData.put("transactionNo", transactionNo);
        resultData.put("remark", remark);

        BigDecimal amount = parseAmount(amountStr);
        LocalDate transactionDate = parseDate(dateStr);

        return OcrResult.builder()
                .success(true)
                .type("BANK_RECEIPT")
                .data(resultData)
                .confidence(data.path("confidence").asDouble(0.9))
                .bankName(bankName)
                .amount(amount)
                .transactionDate(transactionDate)
                .payerName(payerName)
                .payerAccount(payerAccount)
                .payeeName(payeeName)
                .payeeAccount(payeeAccount)
                .transactionNo(transactionNo)
                .remark(remark)
                .build();
    }

    /**
     * 解析身份证识别结果
     */
    private OcrResult parseIdCardResult(String jsonResult, boolean isFront) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResult);
        JsonNode data = root.path("result");
        
        Map<String, Object> resultData = new HashMap<>();
        
        // 提取原始文本
        String rawText = data.path("raw_text").asText("");
        if (rawText.isEmpty()) {
            // 如果没有raw_text字段，尝试从result中提取
            rawText = root.path("raw_text").asText("");
        }
        
        if (isFront) {
            String name = data.path("name").asText("");
            String gender = data.path("gender").asText("");
            String ethnicity = data.path("ethnicity").asText("");
            String birthDateStr = data.path("birth_date").asText("");
            String address = data.path("address").asText("");
            String idNumber = data.path("id_number").asText("");
            
            resultData.put("name", name);
            resultData.put("gender", gender);
            resultData.put("ethnicity", ethnicity);
            resultData.put("birthDate", birthDateStr);
            resultData.put("address", address);
            resultData.put("idNumber", idNumber);
            resultData.put("rawText", rawText);

            return OcrResult.builder()
                    .success(true)
                    .type("ID_CARD_FRONT")
                    .rawText(rawText)
                    .data(resultData)
                    .confidence(data.path("confidence").asDouble(0.95))
                    .name(name)
                    .gender(gender)
                    .ethnicity(ethnicity)
                    .birthDate(parseDate(birthDateStr))
                    .address(address)
                    .idNumber(idNumber)
                    .build();
        } else {
            String issuingAuthority = data.path("issuing_authority").asText("");
            String validFromStr = data.path("valid_from").asText("");
            String validToStr = data.path("valid_to").asText("");
            
            resultData.put("issuingAuthority", issuingAuthority);
            resultData.put("validFrom", validFromStr);
            resultData.put("validTo", validToStr);

            return OcrResult.builder()
                    .success(true)
                    .type("ID_CARD_BACK")
                    .data(resultData)
                    .confidence(data.path("confidence").asDouble(0.95))
                    .issuingAuthority(issuingAuthority)
                    .validFrom(parseDate(validFromStr))
                    .validTo(parseDate(validToStr))
                    .build();
        }
    }

    /**
     * 解析营业执照识别结果
     */
    private OcrResult parseBusinessLicenseResult(String jsonResult) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResult);
        JsonNode data = root.path("result");
        
        Map<String, Object> resultData = new HashMap<>();
        
        // 提取原始文本
        String rawText = data.path("raw_text").asText("");
        if (rawText.isEmpty()) {
            rawText = root.path("raw_text").asText("");
        }
        
        String companyName = data.path("company_name").asText("");
        String creditCode = data.path("credit_code").asText("");
        String companyType = data.path("company_type").asText("");
        String legalRepresentative = data.path("legal_representative").asText("");
        String registeredCapital = data.path("registered_capital").asText("");
        String establishDateStr = data.path("establish_date").asText("");
        String businessTerm = data.path("business_term").asText("");
        String businessScope = data.path("business_scope").asText("");
        String registeredAddress = data.path("registered_address").asText("");
        
        resultData.put("companyName", companyName);
        resultData.put("creditCode", creditCode);
        resultData.put("companyType", companyType);
        resultData.put("legalRepresentative", legalRepresentative);
        resultData.put("registeredCapital", registeredCapital);
        resultData.put("establishDate", establishDateStr);
        resultData.put("businessTerm", businessTerm);
        resultData.put("businessScope", businessScope);
        resultData.put("registeredAddress", registeredAddress);
        resultData.put("rawText", rawText);

        return OcrResult.builder()
                .success(true)
                .type("BUSINESS_LICENSE")
                .rawText(rawText)
                .data(resultData)
                .confidence(data.path("confidence").asDouble(0.9))
                .companyName(companyName)
                .creditCode(creditCode)
                .companyType(companyType)
                .legalRepresentative(legalRepresentative)
                .registeredCapital(registeredCapital)
                .establishDate(parseDate(establishDateStr))
                .businessTerm(businessTerm)
                .businessScope(businessScope)
                .registeredAddress(registeredAddress)
                .build();
    }

    /**
     * 解析金额
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 移除非数字字符（保留小数点）
        String cleaned = amountStr.replaceAll("[^0-9.]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 解析日期
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // 尝试多种日期格式
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy年MM月dd日", "yyyyMMdd"};
        String cleaned = dateStr.replaceAll("[年月]", "-").replaceAll("日", "");
        
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public OcrResult recognizeBusinessCard(MultipartFile file) {
        try {
            String result = callPaddleOcr(file, "/ocr/business_card");
            return parseBusinessCardResult(result);
        } catch (Exception e) {
            log.error("名片识别失败", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeBusinessCard(String imageUrl) {
        try {
            String result = callPaddleOcrByUrl(imageUrl, "/ocr/business_card");
            return parseBusinessCardResult(result);
        } catch (Exception e) {
            log.error("名片识别失败(URL)", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeInvoice(MultipartFile file) {
        try {
            String result = callPaddleOcr(file, "/ocr/invoice");
            return parseInvoiceResult(result);
        } catch (Exception e) {
            log.error("发票识别失败", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResult recognizeInvoice(String imageUrl) {
        try {
            String result = callPaddleOcrByUrl(imageUrl, "/ocr/invoice");
            return parseInvoiceResult(result);
        } catch (Exception e) {
            log.error("发票识别失败(URL)", e);
            return OcrResult.builder()
                    .success(false)
                    .errorMessage("识别失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析名片识别结果
     */
    private OcrResult parseBusinessCardResult(String jsonResult) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResult);
        JsonNode data = root.path("result");
        
        Map<String, Object> resultData = new HashMap<>();
        
        String name = data.path("name").asText("");
        String company = data.path("company").asText("");
        String title = data.path("title").asText("");
        String mobile = data.path("mobile").asText("");
        String phone = data.path("phone").asText("");
        String email = data.path("email").asText("");
        String address = data.path("address").asText("");
        String website = data.path("website").asText("");
        
        resultData.put("name", name);
        resultData.put("company", company);
        resultData.put("title", title);
        resultData.put("mobile", mobile);
        resultData.put("phone", phone);
        resultData.put("email", email);
        resultData.put("address", address);
        resultData.put("website", website);

        return OcrResult.builder()
                .success(true)
                .type("BUSINESS_CARD")
                .data(resultData)
                .rawText(data.path("raw_text").asText(""))
                .confidence(data.path("confidence").asDouble(0.9))
                .name(name)
                .cardCompany(company)
                .title(title)
                .mobile(mobile)
                .phone(phone)
                .email(email)
                .address(address)
                .website(website)
                .build();
    }

    /**
     * 解析发票识别结果
     */
    private OcrResult parseInvoiceResult(String jsonResult) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResult);
        JsonNode data = root.path("result");
        
        Map<String, Object> resultData = new HashMap<>();
        
        String invoiceType = data.path("invoice_type").asText("");
        String invoiceCode = data.path("invoice_code").asText("");
        String invoiceNo = data.path("invoice_no").asText("");
        String invoiceDateStr = data.path("invoice_date").asText("");
        String sellerName = data.path("seller_name").asText("");
        String sellerTaxNo = data.path("seller_tax_no").asText("");
        String buyerName = data.path("buyer_name").asText("");
        String buyerTaxNo = data.path("buyer_tax_no").asText("");
        String amountStr = data.path("amount").asText("0");
        String taxAmountStr = data.path("tax_amount").asText("0");
        String totalAmountStr = data.path("total_amount").asText("0");
        
        resultData.put("invoiceType", invoiceType);
        resultData.put("invoiceCode", invoiceCode);
        resultData.put("invoiceNo", invoiceNo);
        resultData.put("invoiceDate", invoiceDateStr);
        resultData.put("sellerName", sellerName);
        resultData.put("sellerTaxNo", sellerTaxNo);
        resultData.put("buyerName", buyerName);
        resultData.put("buyerTaxNo", buyerTaxNo);
        resultData.put("amount", amountStr);
        resultData.put("taxAmount", taxAmountStr);
        resultData.put("totalAmount", totalAmountStr);

        return OcrResult.builder()
                .success(true)
                .type("INVOICE")
                .data(resultData)
                .rawText(data.path("raw_text").asText(""))
                .confidence(data.path("confidence").asDouble(0.9))
                .invoiceType(invoiceType)
                .invoiceCode(invoiceCode)
                .invoiceNo(invoiceNo)
                .invoiceDate(parseDate(invoiceDateStr))
                .sellerName(sellerName)
                .sellerTaxNo(sellerTaxNo)
                .buyerName(buyerName)
                .buyerTaxNo(buyerTaxNo)
                .invoiceAmount(parseAmount(amountStr))
                .taxAmount(parseAmount(taxAmountStr))
                .totalAmount(parseAmount(totalAmountStr))
                .build();
    }
}
