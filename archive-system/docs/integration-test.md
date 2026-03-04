# 档案系统对接测试指南

## 接口概览

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/open/archive/receive` | POST | API Key | 接收档案推送 |
| `/open/borrow/apply` | POST | API Key | 申请电子借阅链接 |
| `/open/borrow/access/{token}` | GET | 无 | 访问借阅内容 |
| `/open/health` | GET | 无 | 健康检查 |

## 1. 健康检查

```bash
curl -X GET "http://localhost:8081/open/health"
```

预期响应：
```json
{
  "code": "200",
  "message": "success",
  "data": "ok"
}
```

## 2. 档案推送测试

### 2.1 获取 API Key

在档案系统中创建外部来源：
- 登录档案系统管理界面
- 进入「系统管理」→「外部来源」
- 新建来源，获取 API Key

### 2.2 生成签名

签名算法：
```
signature = SHA256(apiKey + timestamp + requestBody)
```

Java 示例：
```java
public String generateSignature(String apiKey, String timestamp, String body) {
    String data = apiKey + timestamp + body;
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(hash);
}
```

### 2.3 发送请求

```bash
API_KEY="your-api-key"
TIMESTAMP=$(date +%s)
BODY='{
  "sourceType": "CASE",
  "sourceId": "test-001",
  "title": "测试档案",
  "archiveType": "CASE_FILE",
  "retentionPeriod": "D10",
  "caseNo": "2024-TEST-001",
  "caseName": "测试案件",
  "files": [
    {
      "fileName": "测试文件.pdf",
      "downloadUrl": "https://example.com/test.pdf"
    }
  ]
}'

# 计算签名
SIGNATURE=$(echo -n "${API_KEY}${TIMESTAMP}${BODY}" | shasum -a 256 | cut -d' ' -f1)

curl -X POST "http://localhost:8081/open/archive/receive" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Signature: ${SIGNATURE}" \
  -d "${BODY}"
```

### 2.4 预期响应

成功：
```json
{
  "code": "200",
  "message": "档案接收成功",
  "data": {
    "archiveId": 123,
    "archiveNo": "ARC-2024-000001",
    "status": "PENDING",
    "message": "档案已接收，正在异步处理"
  }
}
```

失败：
```json
{
  "code": "VALIDATION_ERROR",
  "message": "档案题名不能为空"
}
```

## 3. 回调通知

如果请求中指定了 `callbackUrl`，档案系统处理完成后会发送回调：

```json
{
  "archiveId": 123,
  "archiveNo": "ARC-2024-000001",
  "sourceType": "CASE",
  "sourceId": "test-001",
  "status": "COMPLETED",
  "message": "档案处理完成",
  "processedAt": "2024-01-15T10:30:00"
}
```

## 4. 常见错误

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 401 | 未授权 | 检查 API Key 是否正确 |
| 403 | 签名验证失败 | 检查签名算法和时间戳 |
| 400 | 请求参数错误 | 检查必填字段 |
| 500 | 服务器内部错误 | 查看服务端日志 |

## 5. 管理系统集成示例

### Spring Boot 客户端

```java
@Service
public class ArchiveClient {
    
    private final RestTemplate restTemplate;
    private final String archiveUrl = "http://archive-system:8081";
    private final String apiKey = "your-api-key";
    
    public ArchiveReceiveResponse pushArchive(CaseArchiveDTO caseArchive) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String body = objectMapper.writeValueAsString(buildRequest(caseArchive));
        String signature = generateSignature(apiKey, timestamp, body);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        headers.set("X-Timestamp", timestamp);
        headers.set("X-Signature", signature);
        
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Result<ArchiveReceiveResponse>> response = restTemplate.exchange(
            archiveUrl + "/open/archive/receive",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<>() {}
        );
        
        return response.getBody().getData();
    }
    
    private ArchiveReceiveRequest buildRequest(CaseArchiveDTO caseArchive) {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("CASE");
        request.setSourceId(caseArchive.getCaseId().toString());
        request.setSourceNo(caseArchive.getCaseNo());
        request.setTitle(caseArchive.getCaseName());
        request.setArchiveType("CASE_FILE");
        request.setRetentionPeriod("PERMANENT");
        request.setCaseNo(caseArchive.getCaseNo());
        request.setCaseName(caseArchive.getCaseName());
        request.setClientName(caseArchive.getClientName());
        request.setLawyerName(caseArchive.getLawyerName());
        request.setCallbackUrl("http://law-firm/api/archive/callback");
        
        // 添加文件列表
        List<FileInfo> files = caseArchive.getDocuments().stream()
            .map(doc -> {
                FileInfo file = new FileInfo();
                file.setFileName(doc.getFileName());
                file.setDownloadUrl(doc.getDownloadUrl());
                file.setFileSize(doc.getFileSize());
                return file;
            })
            .collect(Collectors.toList());
        request.setFiles(files);
        
        return request;
    }
}
```

## 6. 测试检查清单

- [ ] 健康检查接口可访问
- [ ] API Key 已创建并配置
- [ ] 签名验证通过
- [ ] 档案推送成功返回 archiveId
- [ ] 回调通知正常接收
- [ ] 档案在管理界面可见
- [ ] 电子文件已正确存储
