# 客户服务系统对接文档

> 文档版本：v1.0  
> 创建日期：2026-01-15  
> 适用系统：律所管理系统

## 一、对接概述

### 1.1 对接目的

律所管理系统支持与客户服务系统对接，实现以下功能：

1. **项目数据推送**：将项目信息、进度、文档等数据推送到客户服务系统
2. **客户通知**：客户服务系统接收数据后，可通过短信、微信、邮件等渠道通知客户
3. **客户文件接收**：接收客户通过客服系统上传的文件
4. **数据同步**：保持律所系统与客服系统的数据一致性

### 1.2 对接架构

```
┌─────────────────┐         HTTP/HTTPS         ┌─────────────────┐
│  律所管理系统     │  ──────────────────────>  │  客户服务系统     │
│                 │                            │                 │
│  - 项目数据推送  │                            │  - 接收项目数据  │
│  - 文件接收接口  │                            │  - 通知客户      │
│  - 数据查询接口  │                            │  - 文件上传      │
└─────────────────┘                            └─────────────────┘
```

### 1.3 对接流程

1. **配置阶段**：在律所管理系统中配置客服系统的API地址和密钥
2. **推送阶段**：律所系统主动推送项目数据到客服系统
3. **接收阶段**：客服系统接收数据并通知客户
4. **回调阶段**：客服系统可回调律所系统（如文件删除通知）

---

## 二、对接标准

### 2.1 通信协议

- **协议**：HTTP/HTTPS
- **数据格式**：JSON
- **字符编码**：UTF-8

### 2.2 认证方式

客服系统需要提供API密钥，律所系统在请求时携带：

```
Authorization: Bearer {API密钥}
```

**重要说明**：
- API密钥在律所系统中以加密方式存储
- 调用API时会自动解密并使用
- 请确保API密钥具有足够的权限访问接收接口
- 建议定期更换API密钥以提高安全性

### 2.3 基础配置

在律所管理系统中配置客服系统信息：

- **集成类型**：`CLIENT_SERVICE`
- **API地址**：客服系统的完整API地址（如：`https://client-service.example.com/api`）
- **API密钥**：客服系统提供的认证密钥
- **认证方式**：`API_KEY`

---

## 三、API接口规范

### 3.1 接收项目数据接口

**接口说明**：律所系统推送项目数据到客服系统

**接口地址**：`POST {客服系统API地址}/matter/receive`

**请求头**：
```
Content-Type: application/json
Authorization: Bearer {API密钥}
```

**请求体**：
```json
{
  "clientId": 123,
  "clientName": "张三",
  "matterData": {
    "matterId": 456,
    "matterName": "张三诉李四合同纠纷案",
    "matterNo": "M20260115001",
    "matterType": "CIVIL",
    "status": "IN_PROGRESS",
    "progress": 30,
    "currentStage": "起诉阶段",
    "lawyers": [
      {
        "name": "王律师",
        "role": "主办律师",
        "phone": "138****5678"
      }
    ],
    "deadlines": [
      {
        "name": "举证期限",
        "date": "2026-02-01",
        "daysRemaining": 17
      }
    ],
    "tasks": [
      {
        "title": "准备起诉状",
        "status": "COMPLETED",
        "progress": 100
      }
    ],
    "documents": [
      {
        "name": "起诉状.docx",
        "uploadTime": "2026-01-15T10:00:00"
      }
    ],
    "feeInfo": {
      "contractAmount": 50000.00,
      "paidAmount": 20000.00,
      "pendingAmount": 30000.00
    },
    "latestUpdate": "2026-01-15: 已提交起诉状",
    "updatedAt": "2026-01-15T10:30:00"
  },
  "validDays": 30,
  "scopes": [
    "MATTER_INFO",
    "MATTER_PROGRESS",
    "LAWYER_INFO",
    "DEADLINE_INFO",
    "TASK_LIST",
    "DOCUMENT_LIST",
    "FEE_INFO"
  ]
}
```

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| clientId | number | 是 | 客户ID |
| clientName | string | 是 | 客户名称 |
| matterData | object | 是 | 项目数据对象 |
| matterData.matterId | number | 是 | 项目ID |
| matterData.matterName | string | 是 | 项目名称 |
| matterData.matterNo | string | 否 | 项目编号 |
| matterData.matterType | string | 否 | 项目类型（LITIGATION/NON_LITIGATION） |
| matterData.matterTypeName | string | 否 | 项目类型名称（诉讼案件/非诉项目） |
| matterData.caseType | string | 否 | 案件类型（CIVIL/CRIMINAL/ADMINISTRATIVE等） |
| matterData.caseTypeName | string | 否 | 案件类型名称（民事/刑事/行政等） |
| matterData.status | string | 否 | 项目状态（DRAFT/PENDING/ACTIVE/SUSPENDED/CLOSED/ARCHIVED） |
| matterData.statusName | string | 否 | 项目状态名称（草稿/待审批/进行中/暂停/已结案/已归档） |
| matterData.currentPhase | string | 否 | 当前阶段（PREPARATION/PROCESSING/COMPLETED） |
| matterData.currentPhaseName | string | 否 | 当前阶段名称（准备阶段/办理中/已完成） |
| matterData.progress | number | 否 | 项目进度（0-100） |
| matterData.lastUpdateTime | string | 否 | 最近更新时间（格式：yyyy-MM-dd HH:mm） |
| matterData.lawyerList | array | 否 | 承办律师列表（别名：teamMembers） |
| matterData.lawyerList[].name | string | 是 | 律师姓名 |
| matterData.lawyerList[].role | string | 否 | 角色（LEAD/CO_COUNSEL/PARALEGAL/TRAINEE） |
| matterData.lawyerList[].roleName | string | 否 | 角色名称（主办律师/协办律师/律师助理/实习律师） |
| matterData.lawyerList[].phone | string | 否 | 联系电话（已脱敏，如：138****5678） |
| matterData.lawyerList[].email | string | 否 | 邮箱（已脱敏，如：wa***@example.com） |
| matterData.deadlineList | array | 否 | 关键期限列表（别名：deadlines） |
| matterData.deadlineList[].name | string | 是 | 期限名称 |
| matterData.deadlineList[].type | string | 否 | 期限类型 |
| matterData.deadlineList[].deadline | string | 否 | 期限日期（格式：yyyy-MM-dd） |
| matterData.deadlineList[].status | string | 否 | 状态（PENDING/COMPLETED/OVERDUE） |
| matterData.deadlineList[].statusName | string | 否 | 状态名称（待处理/已完成/已逾期） |
| matterData.deadlineList[].remainingDays | number | 否 | 剩余天数 |
| matterData.taskList | array | 否 | 任务列表（别名：tasks） |
| matterData.taskList[].title | string | 是 | 任务标题 |
| matterData.taskList[].status | string | 否 | 状态（TODO/IN_PROGRESS/DONE/CANCELLED） |
| matterData.taskList[].statusName | string | 否 | 状态名称（待办/进行中/已完成/已取消） |
| matterData.taskList[].progress | number | 否 | 进度（0-100） |
| matterData.taskList[].dueDate | string | 否 | 截止日期（格式：yyyy-MM-dd） |
| matterData.contractAmount | number | 否 | 合同金额 |
| matterData.receivedAmount | number | 否 | 已收款金额 |
| matterData.pendingAmount | number | 否 | 待收款金额 |
| validDays | number | 否 | 数据有效期（天数），默认30天 |
| scopes | array | 是 | 数据范围列表 |

**响应格式**：

律所系统期望的响应格式为**扁平格式**（推荐），直接返回 `id` 和 `accessUrl`：

```json
{
  "id": "external-matter-id-12345",
  "accessUrl": "https://client-service.example.com/portal/matter/12345?token=xxx"
}
```

**或者**使用标准格式（也支持）：

```json
{
  "code": 200,
  "message": "接收成功",
  "data": {
    "id": "external-matter-id-12345",
    "accessUrl": "https://client-service.example.com/portal/matter/12345?token=xxx"
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 客服系统中的项目ID（外部ID），用于后续关联 |
| accessUrl | string | 否 | 客户访问链接，如果提供，律所系统会保存此链接供用户查看 |

**注意**：
- 如果使用扁平格式，响应体直接包含 `id` 和 `accessUrl` 字段
- 如果使用标准格式，`id` 和 `accessUrl` 在 `data` 对象中
- `accessUrl` 为可选字段，如果不提供，律所系统仍会保存推送记录，但不会显示访问链接

**错误响应**：

HTTP状态码应为非2xx，响应体格式：

```json
{
  "code": 400,
  "message": "请求参数错误"
}
```

或者直接返回错误信息字符串。

**常见错误码**：

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查请求体格式和必填字段 |
| 401 | 认证失败 | 检查API密钥是否正确 |
| 403 | 权限不足 | 联系管理员检查API密钥权限 |
| 500 | 服务器错误 | 联系客服系统技术支持 |

---

### 3.2 接收客户文件接口

**接口说明**：客服系统通知律所系统有新的客户上传文件

**接口地址**：`POST {律所系统API地址}/api/open/client/files`

**请求头**：
```
Content-Type: application/json
```

**请求体**：
```json
{
  "matterId": 456,
  "clientId": 123,
  "clientName": "张三",
  "fileName": "证据材料.pdf",
  "fileSize": 1024000,
  "fileType": "application/pdf",
  "fileCategory": "EVIDENCE",
  "description": "客户上传的证据材料",
  "externalFileId": "file-external-id-12345",
  "externalFileUrl": "https://client-service.example.com/files/xxx",
  "uploadedBy": "客户姓名",
  "uploadedAt": "2026-01-15T10:00:00"
}
```

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| matterId | number | 是 | 项目ID |
| clientId | number | 是 | 客户ID |
| clientName | string | 否 | 客户姓名 |
| fileName | string | 是 | 文件名称 |
| fileSize | number | 否 | 文件大小（字节） |
| fileType | string | 否 | 文件MIME类型（如：application/pdf） |
| fileCategory | string | 否 | 文件类别：EVIDENCE（证据材料）、CONTRACT（合同文件）、ID_CARD（身份证件）、OTHER（其他） |
| description | string | 否 | 文件描述 |
| externalFileId | string | 是 | 客服系统中的文件ID（用于后续删除回调） |
| externalFileUrl | string | 是 | 文件下载URL（律所系统会从此URL下载文件） |
| uploadedBy | string | 否 | 上传人姓名 |
| uploadedAt | string | 否 | 上传时间（ISO 8601格式，如：2026-01-15T10:00:00） |

**响应格式**：
```json
{
  "success": true,
  "code": "200",
  "message": "接收成功",
  "data": {
    "id": 789,
    "fileName": "证据材料.pdf",
    "status": "PENDING"
  }
}
```

---

### 3.3 文件删除回调接口

**接口说明**：客服系统删除文件后，回调通知律所系统

**接口地址**：`POST {律所系统API地址}/api/open/client/files/deleted`

**请求参数**：
```
externalFileId: string（必填）- 客服系统中的文件ID
```

**响应格式**：
```json
{
  "success": true,
  "code": "200",
  "message": "处理成功"
}
```

---

## 四、数据范围（Scopes）说明

律所系统支持按需推送不同范围的数据，通过 `scopes` 字段控制：

| Scope值 | 说明 | 包含数据 |
|---------|------|----------|
| MATTER_INFO | 项目基本信息 | 项目名称、编号、类型、状态等 |
| MATTER_PROGRESS | 项目进度 | 当前阶段、整体进度、最近更新时间 |
| LAWYER_INFO | 承办律师 | 团队成员姓名、角色、联系方式（脱敏） |
| DEADLINE_INFO | 关键期限 | 诉讼时效、举证期限、开庭时间等 |
| TASK_LIST | 办理事项 | 待办事项标题、状态、进度 |
| DOCUMENT_LIST | 文书材料 | 文档名称列表（仅标题，不含内容）**注意：当前版本暂未实现，请勿使用** |
| DOCUMENT_FILES | 文书文件 | 推送选定的文档文件（含下载URL），客户可下载**注意：当前版本暂未实现，请勿使用** |
| FEE_INFO | 费用信息 | 合同金额、已收款、待收款 |

**注意**：
- 可以同时推送多个范围的数据
- 联系方式会进行脱敏处理（如：`138****5678`）
- `DOCUMENT_LIST` 和 `DOCUMENT_FILES` 在当前版本中暂未实现，即使包含在 `scopes` 中也不会推送文档数据
- 文档相关功能计划在后续版本中实现

---

## 五、数据推送流程

### 5.1 推送触发方式

1. **手动推送**：用户在律所系统中手动触发推送
2. **自动推送**：配置自动推送后，项目更新时自动推送（待实现）

### 5.2 推送流程

```
1. 用户在律所系统中选择项目和客户
2. 选择要推送的数据范围（scopes）
3. 设置数据有效期（validDays）
4. 系统组装项目数据（根据scopes过滤）
5. 调用客服系统API推送数据
6. 保存推送记录
7. 客服系统接收数据并通知客户
```

### 5.3 推送记录

律所系统会记录每次推送的详细信息：

- 推送时间
- 推送状态（成功/失败/待推送）
- 推送的数据范围
- 外部系统返回的ID和访问链接
- 错误信息（如果失败）

---

## 六、示例代码

### 6.1 Java示例（客服系统接收接口）

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/matter")
public class MatterReceiveController {
    
    @PostMapping("/receive")
    public ResponseEntity<Map<String, Object>> receiveMatterData(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        
        // 1. 验证API密钥
        String token = authHeader.replace("Bearer ", "");
        if (!validateApiKey(token)) {
            return ResponseEntity.status(401)
                .body(Map.of("code", 401, "message", "认证失败"));
        }
        
        // 2. 解析请求数据
        Long clientId = Long.valueOf(request.get("clientId").toString());
        String clientName = request.get("clientName").toString();
        Map<String, Object> matterData = (Map<String, Object>) request.get("matterData");
        List<String> scopes = (List<String>) request.get("scopes");
        
        // 3. 保存数据到客服系统
        String externalId = saveMatterData(clientId, clientName, matterData, scopes);
        
        // 4. 生成客户访问链接
        String accessUrl = generateAccessUrl(externalId);
        
        // 5. 通知客户（短信/微信/邮件）
        notifyClient(clientId, matterData, accessUrl);
        
        // 6. 返回响应
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "接收成功",
            "data", Map.of(
                "id", externalId,
                "accessUrl", accessUrl
            )
        ));
    }
    
    private boolean validateApiKey(String token) {
        // 验证API密钥逻辑
        return true;
    }
    
    private String saveMatterData(Long clientId, String clientName, 
                                  Map<String, Object> matterData, 
                                  List<String> scopes) {
        // 保存数据逻辑
        return "external-matter-id-12345";
    }
    
    private String generateAccessUrl(String externalId) {
        // 生成访问链接逻辑
        return "https://client-service.example.com/portal/matter/" + externalId;
    }
    
    private void notifyClient(Long clientId, Map<String, Object> matterData, 
                             String accessUrl) {
        // 通知客户逻辑（短信/微信/邮件）
    }
}
```

### 6.2 Python示例（客服系统接收接口）

```python
from flask import Flask, request, jsonify
from functools import wraps

app = Flask(__name__)

def validate_api_key(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization', '')
        if not auth_header.startswith('Bearer '):
            return jsonify({'code': 401, 'message': '认证失败'}), 401
        
        token = auth_header.replace('Bearer ', '')
        if not validate_api_key_token(token):
            return jsonify({'code': 401, 'message': '认证失败'}), 401
        
        return f(*args, **kwargs)
    return decorated_function

@app.route('/api/matter/receive', methods=['POST'])
@validate_api_key
def receive_matter_data():
    data = request.json
    
    # 解析请求数据
    client_id = data.get('clientId')
    client_name = data.get('clientName')
    matter_data = data.get('matterData')
    scopes = data.get('scopes', [])
    valid_days = data.get('validDays', 30)
    
    # 保存数据到客服系统
    external_id = save_matter_data(client_id, client_name, matter_data, scopes)
    
    # 生成客户访问链接
    access_url = generate_access_url(external_id)
    
    # 通知客户
    notify_client(client_id, matter_data, access_url)
    
    # 返回响应（推荐使用扁平格式）
    return jsonify({
        'id': external_id,
        'accessUrl': access_url
    })
    
    # 或者使用标准格式（也支持）
    # return jsonify({
    #     'code': 200,
    #     'message': '接收成功',
    #     'data': {
    #         'id': external_id,
    #         'accessUrl': access_url
    #     }
    # })

def validate_api_key_token(token):
    # 验证API密钥逻辑
    return True

def save_matter_data(client_id, client_name, matter_data, scopes):
    # 保存数据逻辑
    return 'external-matter-id-12345'

def generate_access_url(external_id):
    # 生成访问链接逻辑
    return f'https://client-service.example.com/portal/matter/{external_id}'

def notify_client(client_id, matter_data, access_url):
    # 通知客户逻辑（短信/微信/邮件）
    pass

if __name__ == '__main__':
    app.run(debug=True)
```

### 6.3 Node.js示例（客服系统接收接口）

```javascript
const express = require('express');
const app = express();

app.use(express.json());

// 验证API密钥中间件
function validateApiKey(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ code: 401, message: '认证失败' });
    }
    
    const token = authHeader.replace('Bearer ', '');
    if (!validateApiKeyToken(token)) {
        return res.status(401).json({ code: 401, message: '认证失败' });
    }
    
    next();
}

// 接收项目数据接口
app.post('/api/matter/receive', validateApiKey, (req, res) => {
    const { clientId, clientName, matterData, scopes, validDays } = req.body;
    
    // 保存数据到客服系统
    const externalId = saveMatterData(clientId, clientName, matterData, scopes);
    
    // 生成客户访问链接
    const accessUrl = generateAccessUrl(externalId);
    
    // 通知客户
    notifyClient(clientId, matterData, accessUrl);
    
    // 返回响应（推荐使用扁平格式）
    res.json({
        id: externalId,
        accessUrl: accessUrl
    });
    
    // 或者使用标准格式（也支持）
    // res.json({
    //     code: 200,
    //     message: '接收成功',
    //     data: {
    //         id: externalId,
    //         accessUrl: accessUrl
    //     }
    // });
});

function validateApiKeyToken(token) {
    // 验证API密钥逻辑
    return true;
}

function saveMatterData(clientId, clientName, matterData, scopes) {
    // 保存数据逻辑
    return 'external-matter-id-12345';
}

function generateAccessUrl(externalId) {
    // 生成访问链接逻辑
    return `https://client-service.example.com/portal/matter/${externalId}`;
}

function notifyClient(clientId, matterData, accessUrl) {
    // 通知客户逻辑（短信/微信/邮件）
}

app.listen(3000, () => {
    console.log('客服系统API服务启动在端口 3000');
});
```

---

## 七、配置说明

### 7.1 律所系统配置

在律所管理系统中配置客服系统：

1. 进入 **系统管理** > **外部集成** > **客户服务系统**
2. 填写配置信息：
   - **集成名称**：客户服务系统
   - **API地址**：`https://client-service.example.com/api`
   - **API密钥**：客服系统提供的密钥
   - **认证方式**：API_KEY
   - **通知渠道**：SMS, WECHAT, EMAIL（可选）
   - **默认有效期**：30天
3. 点击 **测试** 验证配置是否正确
4. 启用集成

### 7.2 客服系统配置

客服系统需要：

1. 提供API密钥给律所系统
2. 实现 `/matter/receive` 接口接收数据
3. 实现客户通知功能（短信/微信/邮件）
4. 实现客户访问门户（可选）

---

## 八、常见问题

### Q1: 推送失败怎么办？

**A**: 检查以下几点：
1. API地址是否正确
2. API密钥是否正确
3. 网络是否通畅
4. 客服系统接口是否正常
5. 查看律所系统中的推送记录，查看错误信息

### Q2: 数据推送后，客户多久能收到通知？

**A**: 取决于客服系统的通知机制，通常：
- 短信通知：即时（1-5分钟）
- 微信通知：即时（1-5分钟）
- 邮件通知：可能延迟（5-30分钟）

### Q3: 可以只推送部分数据吗？

**A**: 可以，通过 `scopes` 字段控制推送的数据范围。例如：
- 只推送基本信息：`["MATTER_INFO"]`
- 推送进度和律师信息：`["MATTER_PROGRESS", "LAWYER_INFO"]`

**注意**：`DOCUMENT_LIST` 和 `DOCUMENT_FILES` 在当前版本中暂未实现，即使包含在 `scopes` 中也不会推送文档数据。

### Q4: 数据有效期是什么意思？

**A**: `validDays` 表示数据在客服系统中的有效期。超过有效期后，客户可能无法访问该数据。建议根据项目周期设置合理的有效期。

### Q5: 推送的数据会包含敏感信息吗？

**A**: 不会。系统会对敏感信息进行脱敏处理：
- 手机号：`138****5678`
- 邮箱：`abc***@example.com`
- 文档内容：仅推送文档名称，不推送文件内容

### Q6: 可以多次推送同一个项目吗？

**A**: 可以。每次推送都会创建新的推送记录，客服系统可以根据最新推送更新数据。

### Q7: 推送失败会重试吗？

**A**: 目前不支持自动重试。如果推送失败，可以在律所系统中查看错误信息，修复问题后手动重新推送。

### Q8: 客服系统如何通知律所系统文件已删除？

**A**: 调用律所系统的回调接口：
```
POST {律所系统API地址}/api/open/client/files/deleted?externalFileId=xxx
```

### Q9: 响应格式必须使用扁平格式吗？

**A**: 推荐使用扁平格式（直接返回 `id` 和 `accessUrl`），但标准格式（`{code, data: {id, accessUrl}}`）也支持。律所系统会自动识别两种格式。

### Q10: API密钥是明文还是加密的？

**A**: API密钥在律所系统中以加密方式存储，但在调用API时会自动解密。客服系统只需要提供明文密钥给律所系统管理员配置即可。

### Q11: 文件接收后，律所系统会自动下载吗？

**A**: 不会自动下载。文件接收后，律所系统会创建待同步记录，管理员需要手动选择同步到卷宗目录。同步时，律所系统会从 `externalFileUrl` 下载文件并存储到MinIO。

### Q12: 律所系统同步文件后，会通知客服系统删除文件吗？

**A**: 会的。当律所系统成功同步文件后，会自动调用客服系统的删除接口：
```
POST {客服系统API地址}/files/delete
Authorization: Bearer {API密钥}
Content-Type: application/json

{
  "fileId": "file-external-id-12345",
  "action": "DELETE"
}
```

---

## 九、技术支持

### 9.1 联系方式

- **律所系统技术支持**：请联系系统管理员
- **API文档**：访问律所系统Swagger文档（开发/测试环境）

### 9.2 日志查看

推送记录和错误信息可以在律所系统中查看：
- **路径**：项目管理 > 项目详情 > 客户服务 > 推送记录

### 9.3 测试环境

建议先在测试环境进行对接测试：
- 测试API地址配置
- 测试数据推送功能
- 测试客户通知功能
- 验证数据格式和内容

---

## 十、更新日志

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.1 | 2026-01-15 | 完善文档，根据实际代码核实所有接口细节 |
| v1.0 | 2026-01-15 | 初始版本，包含基础对接功能 |

**v1.1 更新内容**：
- 明确响应格式（扁平格式和标准格式都支持）
- 完善文件接收接口字段说明
- 明确 DOCUMENT_LIST 和 DOCUMENT_FILES scope 暂未实现
- 补充 API 密钥加密存储说明
- 完善数据字段说明（根据实际 PortalMatterDTO 结构）
- 补充文件删除回调接口说明
- 完善示例代码（Java/Python/Node.js）
- 补充更多常见问题

---

## 附录：完整请求示例

### 推送项目数据完整示例

**注意**：这是律所系统发送给客服系统的请求示例，供客服系统开发参考。

```bash
curl -X POST "https://client-service.example.com/api/matter/receive" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-api-key-here" \
  -d '{
    "clientId": 123,
    "clientName": "张三",
    "matterData": {
      "matterId": 456,
      "matterName": "张三诉李四合同纠纷案",
      "matterNo": "M20260115001",
      "matterType": "CIVIL",
      "matterTypeName": "民事",
      "status": "ACTIVE",
      "statusName": "进行中",
      "currentPhase": "PROCESSING",
      "currentPhaseName": "办理中",
      "progress": 50,
      "lastUpdateTime": "2026-01-15 10:30",
      "lawyerList": [
        {
          "name": "王律师",
          "role": "LEAD",
          "roleName": "主办律师",
          "phone": "138****5678",
          "email": "wa***@example.com"
        }
      ],
      "deadlineList": [
        {
          "name": "举证期限",
          "type": "EVIDENCE_DEADLINE",
          "deadline": "2026-02-01",
          "status": "PENDING",
          "statusName": "待处理",
          "remainingDays": 17
        }
      ],
      "taskList": [
        {
          "title": "准备起诉状",
          "status": "DONE",
          "statusName": "已完成",
          "progress": 100,
          "dueDate": "2026-01-10"
        }
      ],
      "contractAmount": 50000.00,
      "receivedAmount": 20000.00,
      "pendingAmount": 30000.00
    },
    "validDays": 30,
    "scopes": [
      "MATTER_INFO",
      "MATTER_PROGRESS",
      "LAWYER_INFO",
      "DEADLINE_INFO",
      "TASK_LIST",
      "DOCUMENT_LIST",
      "FEE_INFO"
    ]
  }'
```

### 响应示例

**推荐格式（扁平格式）**：
```json
{
  "id": "external-matter-id-12345",
  "accessUrl": "https://client-service.example.com/portal/matter/12345?token=xxx"
}
```

**标准格式（也支持）**：
```json
{
  "code": 200,
  "message": "接收成功",
  "data": {
    "id": "external-matter-id-12345",
    "accessUrl": "https://client-service.example.com/portal/matter/12345?token=xxx"
  }
}
```

### 接收客户文件完整示例

**注意**：这是客服系统发送给律所系统的请求示例。

```bash
curl -X POST "https://law-firm-system.example.com/api/open/client/files" \
  -H "Content-Type: application/json" \
  -d '{
    "matterId": 456,
    "clientId": 123,
    "clientName": "张三",
    "fileName": "证据材料.pdf",
    "fileSize": 1024000,
    "fileType": "application/pdf",
    "fileCategory": "EVIDENCE",
    "description": "客户上传的证据材料",
    "externalFileId": "file-external-id-12345",
    "externalFileUrl": "https://client-service.example.com/files/xxx",
    "uploadedBy": "张三",
    "uploadedAt": "2026-01-15T10:00:00"
  }'
```

**响应示例**：
```json
{
  "success": true,
  "code": "200",
  "message": "接收成功",
  "data": {
    "id": 789,
    "fileName": "证据材料.pdf",
    "status": "PENDING"
  }
}
```

---

**文档结束**
