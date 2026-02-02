# 客户服务系统对接文档

> 文档版本：v1.2  
> 创建日期：2026-02-02  
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

## 六点五、项目管理模块的项目详情推送功能设计

### 6.5.1 功能概述

项目管理模块的项目详情页面集成了客户服务推送功能，允许项目参与者将项目数据推送到客户服务系统，供客户查看项目进度和相关信息。

**组件位置**：
- 前端组件：项目详情页 → "客户服务" Tab
- 组件文件：`frontend/apps/web-antd/src/components/ClientServicePanel/index.vue`
- 集成位置：`frontend/apps/web-antd/src/views/matter/detail/index.vue`

**后端API**：
- 基础路径：`/matter/client-service/*`
- 控制器：`MatterClientServiceController`
- 服务层：`DataPushService`

**权限要求**：
- `matter:clientService:list`：查看推送记录和客户文件
- `matter:clientService:create`：推送数据和同步文件

### 6.5.2 页面布局设计

项目详情页采用Tab结构，包含多个功能模块：

```
项目详情页
├── 卷宗管理 Tab
├── 证据材料 Tab
├── 客户服务 Tab ← ClientServicePanel组件
│   ├── 推送配置区域
│   │   ├── 客户服务系统连接状态提示
│   │   ├── 推送配置开关
│   │   └── 默认推送范围设置
│   ├── 推送操作区域
│   │   ├── "推送数据"按钮
│   │   └── 推送记录列表（分页）
│   └── 客户文件区域
│       ├── 待同步文件列表
│       ├── 已同步文件列表
│       └── 文件操作（同步到卷宗/忽略）
└── 其他Tab...
```

### 6.5.3 推送消息功能设计

#### 6.5.3.1 推送触发流程

用户点击"推送数据"按钮后，系统执行以下检查：

1. **客户服务系统配置检查**
   - 检查是否存在类型为`CLIENT_SERVICE`的外部集成配置
   - 检查配置是否已启用（`enabled = true`）
   - 如果未配置或未启用，显示提示："客户服务系统未配置或未启用，请在【系统管理→外部系统集成】中配置"

2. **用户权限检查**
   - 验证当前用户是否有`matter:clientService:create`权限
   - 验证用户是否为项目参与者（通过`MatterParticipant`表检查）
   - 管理员可以操作所有项目

3. **项目状态检查**
   - 检查项目状态，`ARCHIVED`（已归档）和`CANCELLED`（已取消）状态不允许推送
   - 检查项目是否已删除（`deleted = false`）

4. **客户关联检查**
   - 检查项目是否关联客户（`clientId`不为空）
   - 如果请求中指定了`clientId`，验证该客户是否存在

#### 6.5.3.2 推送确认弹窗设计

推送确认弹窗包含以下配置项：

**数据范围选择（多选）**：
- `MATTER_INFO`：项目基本信息（项目名称、编号、类型、状态等）
- `MATTER_PROGRESS`：项目进度（当前阶段、整体进度、最近更新时间）
- `LAWYER_INFO`：承办律师（团队成员姓名、角色、联系方式（脱敏））
- `DEADLINE_INFO`：关键期限（诉讼时效、举证期限、开庭时间等）
- `TASK_LIST`：办理事项（待办事项标题、状态、进度）
- `DOCUMENT_LIST`：文书目录（文档名称列表，仅标题，不含文件）
- `DOCUMENT_FILES`：文书文件（推送选定的文档文件，需选择具体文档）
- `FEE_INFO`：费用信息（合同金额、已收款、待收款）

**有效期设置**：
- 字段名：`validDays`
- 默认值：30天
- 范围：1-365天
- 说明：数据在客户服务系统中的有效期，超过有效期后客户可能无法访问

**文档选择**（当选择`DOCUMENT_FILES`时）：
- 显示项目下的文档列表
- 支持多选文档
- 仅显示未删除的文档（`deleted = false`）

**备注信息**（可选）：
- 字段名：`remark`
- 类型：文本
- 用途：记录本次推送的备注信息

#### 6.5.3.3 推送执行流程

推送执行流程如下：

```
1. 前端调用 API
   POST /matter/client-service/push
   Body: {
     matterId: 456,
     clientId: 123,
     scopes: ["MATTER_INFO", "MATTER_PROGRESS"],
     validDays: 30,
     documentIds: [789, 790],
     remark: "首次推送"
   }

2. 后端验证
   ├── 验证项目存在性、状态、权限
   ├── 验证客户存在性
   └── 获取客户服务系统配置

3. 数据组装（buildMatterData）
   ├── 根据scopes过滤数据
   ├── 敏感信息脱敏处理
   └── 文档文件生成临时下载URL（24小时有效）

4. 创建推送记录（PushRecord）
   ├── 状态：PENDING（待推送）
   ├── 保存数据快照（JSON格式）
   └── 设置有效期（expiresAt）

5. 调用客户服务系统API
   POST {apiUrl}/matter/receive
   Headers: {
     Authorization: Bearer {apiKey}
   }
   Body: {
     clientId, clientName, matterData, validDays, scopes
   }

6. 更新推送记录
   ├── 成功：STATUS_SUCCESS
   │   ├── 保存externalId（外部系统返回的ID）
   │   └── 保存externalUrl（客户访问链接）
   └── 失败：STATUS_FAILED
       └── 保存错误信息（errorMessage）
```

**详细说明**：

1. **数据组装（buildMatterData）**：
   - 根据`scopes`参数过滤需要推送的数据
   - 对于`MATTER_INFO`：包含项目ID、编号、名称、类型、状态、创建日期
   - 对于`MATTER_PROGRESS`：包含当前阶段、最后更新时间
   - 对于`DOCUMENT_FILES`：为选定的文档生成临时下载URL（使用MinIO预签名URL，有效期24小时）

2. **数据快照保存**：
   - 将组装后的数据序列化为JSON格式
   - 保存到`PushRecord.dataSnapshot`字段
   - 用于审计和后续对比

3. **API调用**：
   - 使用`RestTemplate`发送HTTP请求
   - 请求头包含`Authorization: Bearer {apiKey}`
   - 请求体包含客户信息、项目数据、有效期、数据范围

4. **响应处理**：
   - 支持扁平格式：`{id, accessUrl}`
   - 支持标准格式：`{code, message, data: {id, accessUrl}}`
   - 自动识别两种格式并提取`id`和`accessUrl`

#### 6.5.3.4 数据脱敏处理

系统使用`SensitiveUtils`工具类对敏感信息进行脱敏处理：

**脱敏规则**：

| 数据类型 | 脱敏规则 | 示例 |
|---------|---------|------|
| 手机号 | 保留前3位和后4位，中间用`****`替代 | `138****5678` |
| 邮箱 | 保留@前第1个字符和@后完整域名 | `z****@example.com` |
| 身份证 | 保留前6位和后4位，中间用`********`替代 | `110101********0011` |
| 姓名 | 保留第1个字符，其余用`**`替代 | `张**` |
| 地址 | 保留前6位和后4位，中间用`****`替代 | `北京市海淀区****123号` |
| 费用金额 | 完全隐藏，显示为`****` | `****` |

**脱敏位置**：
- 律师联系方式（`leadLawyerContact`）
- 团队成员联系方式（`teamMembers[].contact`）
- 客户敏感信息（在推送前已脱敏）

**注意事项**：
- 脱敏后的数据仅用于客户查看，不影响系统内部使用
- 脱敏规则可根据业务需求调整

#### 6.5.3.5 推送记录展示

推送记录列表展示以下信息：

**列表字段**：
- **推送时间**：`createdAt`（格式：yyyy-MM-dd HH:mm:ss）
- **推送类型**：
  - `MANUAL`：手动推送
  - `AUTO`：自动推送（项目更新时）
  - `UPDATE`：数据更新推送
- **推送范围**：显示选定的数据范围（如：项目基本信息、项目进度）
- **推送状态**：
  - `PENDING`：待推送（客户服务系统未配置或未启用）
  - `SUCCESS`：成功（已推送到客户服务系统）
  - `FAILED`：失败（推送失败，显示错误信息）
- **外部系统ID**：`externalId`（客户服务系统返回的项目ID）
- **客户访问链接**：`externalUrl`（如果提供，可点击跳转）
- **错误信息**：`errorMessage`（失败时显示）

**功能特性**：
- 支持分页查询（默认每页20条）
- 支持按状态筛选（`PENDING`/`SUCCESS`/`FAILED`）
- 支持查看推送记录详情（包含完整数据快照）
- 支持查看最近一次成功推送

**API接口**：
- `GET /matter/client-service/records`：获取推送记录列表（分页）
- `GET /matter/client-service/records/{id}`：获取推送记录详情
- `GET /matter/client-service/latest`：获取最近一次成功推送

#### 6.5.3.6 推送配置管理

每个项目可以配置独立的推送设置（`PushConfig`）：

**配置项**：
- **是否启用推送**（`enabled`）：
  - 默认值：`false`
  - 启用后，推送功能才可用
- **默认推送范围**（`scopes`）：
  - 默认值：`["MATTER_INFO", "MATTER_PROGRESS", "LAWYER_INFO", "DEADLINE_INFO"]`
  - 推送时自动选中这些范围
- **是否自动推送**（`autoPushOnUpdate`）：
  - 默认值：`false`
  - 启用后，项目更新时自动推送（待实现）
- **默认有效期**（`validDays`）：
  - 默认值：30天
  - 推送时自动填充此值

**配置获取**：
- `GET /matter/client-service/config?matterId={id}&clientId={id}`：获取或创建推送配置
- `PUT /matter/client-service/config?matterId={id}`：更新推送配置

**配置联动**：
- 配置返回时包含客户服务系统连接状态（`clientServiceConnected`）
- 如果未连接，显示提示信息（`connectionMessage`）

### 6.5.4 客户文件接收功能

#### 6.5.4.1 文件接收流程

客户通过客户服务系统上传文件后，客户服务系统调用律所系统的接收接口：

**接收接口**：
- `POST /open/client/files`
- 控制器：`ClientFileOpenController`
- 服务层：`ClientFileService`

**接收流程**：
```
1. 客户服务系统调用接口
   POST /open/client/files
   Body: {
     matterId: 456,
     clientId: 123,
     clientName: "张三",
     fileName: "证据材料.pdf",
     fileSize: 1024000,
     fileType: "application/pdf",
     fileCategory: "EVIDENCE",
     externalFileId: "file-external-id-12345",
     externalFileUrl: "https://client-service.example.com/files/xxx",
     uploadedBy: "张三",
     uploadedAt: "2026-01-15T10:00:00"
   }

2. 后端验证
   ├── 验证项目存在性
   └── 检查文件是否已存在（防止重复推送）

3. 创建文件记录（ClientFile）
   ├── 状态：PENDING（待同步）
   ├── 保存外部文件ID和URL
   └── 记录上传信息

4. 返回响应
   {
     id: 789,
     fileName: "证据材料.pdf",
     status: "PENDING"
   }
```

**防重复机制**：
- 通过`externalFileId`检查文件是否已存在
- 如果已存在，直接返回现有记录，不重复创建

#### 6.5.4.2 文件同步流程

用户在前端选择文件并指定目标卷宗目录后，调用同步接口：

**同步接口**：
- `POST /matter/client-files/sync`
- 请求体：`{fileId, targetDossierId}`

**同步流程**：
```
1. 前端调用同步接口
   POST /matter/client-files/sync
   Body: {
     fileId: 789,
     targetDossierId: 101
   }

2. 后端验证
   ├── 验证文件存在性
   ├── 验证文件状态（必须为PENDING）
   └── 验证卷宗目录存在性

3. 下载文件
   ├── 从externalFileUrl下载文件
   └── 验证文件内容不为空

4. 上传到MinIO
   ├── 存储路径：matters/{matterId}/client_uploads/
   ├── 文件名：UUID + 原始文件名
   └── 获取文件URL

5. 创建文档记录（Document）
   ├── 关联到项目（matterId）
   ├── 关联到卷宗目录项（dossierItemId）
   ├── 标记来源：CLIENT_PORTAL
   └── 保存文件信息（文件名、大小、类型等）

6. 更新文件状态
   ├── 状态：SYNCED（已同步）
   ├── 保存本地文档ID（localDocumentId）
   ├── 保存目标卷宗ID（targetDossierId）
   └── 记录同步时间和操作人

7. 通知客户服务系统删除文件
   POST {apiUrl}/files/delete
   Body: {
     fileId: "file-external-id-12345",
     action: "DELETE"
   }
```

**错误处理**：
- 下载失败：状态更新为`FAILED`，保存错误信息
- 上传失败：状态更新为`FAILED`，保存错误信息
- 删除通知失败：记录警告日志，不影响主流程

#### 6.5.4.3 文件忽略功能

用户可以选择忽略不需要同步的文件：

**忽略接口**：
- `POST /matter/client-files/{fileId}/ignore`

**忽略流程**：
```
1. 前端调用忽略接口
   POST /matter/client-files/789/ignore

2. 后端处理
   ├── 更新文件状态为DELETED
   └── 通知客户服务系统删除文件

3. 返回成功响应
```

**注意事项**：
- 忽略后的文件不会同步到卷宗
- 客户服务系统会删除该文件
- 文件记录保留在系统中，用于审计

#### 6.5.4.4 文件状态管理

文件状态流转如下：

```
PENDING（待同步）
  ├── syncToFolder() → SYNCED（已同步）
  ├── ignoreFile() → DELETED（已删除）
  └── syncToFolder()失败 → FAILED（同步失败）
```

**状态说明**：

| 状态 | 说明 | 可执行操作 |
|------|------|-----------|
| `PENDING` | 待同步 | 同步到卷宗、忽略 |
| `SYNCED` | 已同步到卷宗 | 查看文档 |
| `DELETED` | 已忽略/删除 | 无 |
| `FAILED` | 同步失败 | 重新同步、忽略 |

**文件列表API**：
- `GET /matter/client-files?matterId={id}&status={status}`：获取文件列表（分页）
- `GET /matter/client-files/pending?matterId={id}`：获取待同步文件列表
- `GET /matter/client-files/pending/count?matterId={id}`：获取待同步文件数量

---

## 六点六、系统管理模块的API对接配置设计

### 6.6.1 功能概述

系统管理模块提供了外部系统集成配置功能，允许管理员配置和管理与外部系统的API对接，包括客户服务系统、AI服务、OCR服务等。

**页面位置**：
- 前端页面：系统管理 → 外部系统集成
- 页面文件：`frontend/apps/web-antd/src/views/system/integration/index.vue`

**后端API**：
- 基础路径：`/system/external-integration/*`
- 控制器：`ExternalIntegrationController`
- 服务层：`ExternalIntegrationAppService`

**权限要求**：
- 仅管理员角色可访问：`ADMIN`、`SYSTEM_ADMIN`、`SUPER_ADMIN`
- 所有配置操作都需要管理员权限

### 6.6.2 页面布局设计

外部系统集成配置页面采用列表+Tab结构：

```
外部系统集成配置页面
├── 顶部操作栏
│   ├── "新增配置"按钮
│   └── 搜索框（按名称、编码搜索）
├── Tab分类
│   ├── 全部
│   ├── AI服务
│   ├── OCR服务
│   ├── 客户服务
│   └── 其他
└── 配置列表
    ├── 配置卡片1
    │   ├── 集成名称、编码、类型
    │   ├── API地址
    │   ├── 连接状态（已启用/已禁用）
    │   ├── 最后测试结果
    │   └── 操作按钮（编辑、启用/禁用、测试连接、删除）
    └── 配置卡片2...
```

### 6.6.3 API对接配置设计

#### 6.6.3.1 配置表单字段

配置表单包含以下字段：

**基本信息**：
- **集成编码**（`integrationCode`）：
  - 类型：字符串
  - 必填：是
  - 唯一性：是
  - 说明：唯一标识，如`CLIENT_SERVICE`、`AI_SERVICE`等
  - 格式：大写字母和下划线，如`CLIENT_SERVICE`

- **集成名称**（`integrationName`）：
  - 类型：字符串
  - 必填：是
  - 说明：显示名称，如"客户服务系统"、"AI服务"等

- **集成类型**（`integrationType`）：
  - 类型：枚举
  - 必填：是
  - 可选值：
    - `CLIENT_SERVICE`：客户服务系统
    - `AI`：AI服务
    - `OCR`：OCR服务
    - `ARCHIVE`：档案系统
    - `STORAGE`：存储服务
    - `NOTIFICATION`：通知服务
    - `OTHER`：其他

- **描述**（`description`）：
  - 类型：文本
  - 必填：否
  - 说明：配置的详细说明

**API配置**：
- **API地址**（`apiUrl`）：
  - 类型：URL字符串
  - 必填：是（启用时）
  - 格式：完整的HTTP/HTTPS URL，如`https://client-service.example.com/api`
  - 验证：URL格式验证

- **API密钥**（`apiKey`）：
  - 类型：字符串
  - 必填：是（启用时，与`apiSecret`二选一）
  - 存储：加密存储（AES-256-CBC）
  - 显示：脱敏显示（前4位 + "****" + 后4位）

- **API密钥Secret**（`apiSecret`）：
  - 类型：字符串
  - 必填：否（与`apiKey`二选一）
  - 存储：加密存储（AES-256-CBC）
  - 显示：仅显示是否已配置（`hasApiSecret`）

- **认证方式**（`authType`）：
  - 类型：枚举
  - 必填：是
  - 可选值：
    - `API_KEY`：API密钥认证
    - `BEARER_TOKEN`：Bearer Token认证
    - `BASIC`：Basic认证
    - `OAUTH2`：OAuth2认证
    - `WEBHOOK`：Webhook认证

- **额外配置**（`extraConfig`）：
  - 类型：JSON对象
  - 必填：否
  - 说明：可存储额外的配置信息，如：
    ```json
    {
      "notifyChannels": ["SMS", "WECHAT", "EMAIL"],
      "defaultValidDays": 30,
      "apiEndpoints": {
        "push": "/matter/receive",
        "revoke": "/matter/revoke"
      }
    }
    ```

**状态配置**：
- **是否启用**（`enabled`）：
  - 类型：布尔值
  - 默认值：`false`
  - 说明：只有启用后，系统才会使用该配置调用外部API

#### 6.6.3.2 配置存储设计

**数据库表**：
- 表名：`sys_external_integration`
- 实体类：`ExternalIntegration`

**字段设计**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | BIGINT | 主键 |
| `integration_code` | VARCHAR(50) | 集成编码（唯一） |
| `integration_name` | VARCHAR(100) | 集成名称 |
| `integration_type` | VARCHAR(50) | 集成类型 |
| `description` | TEXT | 描述 |
| `api_url` | VARCHAR(500) | API地址 |
| `api_key` | TEXT | API密钥（加密存储） |
| `api_secret` | TEXT | API密钥Secret（加密存储） |
| `auth_type` | VARCHAR(50) | 认证方式 |
| `extra_config` | JSONB | 额外配置（JSON格式） |
| `enabled` | BOOLEAN | 是否启用 |
| `last_test_time` | TIMESTAMP | 最后测试时间 |
| `last_test_result` | VARCHAR(20) | 最后测试结果（SUCCESS/FAILED） |
| `last_test_message` | TEXT | 最后测试消息 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 更新时间 |

**API密钥加密存储**：

1. **加密算法**：AES-256-CBC
2. **密钥派生**：使用SHA-256从配置的密钥派生32字节密钥
3. **IV派生**：使用SHA-256从配置的IV派生16字节IV
4. **存储流程**：
   ```
   明文API密钥
     ↓
   AES-256-CBC加密
     ↓
   密文（Base64编码）
     ↓
   存储到数据库
   ```
5. **读取流程**：
   ```
   数据库密文
     ↓
   Base64解码
     ↓
   AES-256-CBC解密
     ↓
   明文API密钥（仅内部使用）
   ```

**API密钥脱敏显示**：

- 前端显示：前4位 + "****" + 后4位
- 后端返回DTO时自动脱敏（`ExternalIntegrationDTO`）
- 仅管理员在编辑时可查看完整密钥（解密后）

#### 6.6.3.3 配置校验设计

**创建时校验**：
1. **集成编码唯一性检查**：
   - 查询数据库中是否已存在相同的`integrationCode`
   - 如果存在，抛出异常："集成编码已存在: {code}"

2. **API地址格式验证**：
   - 验证URL格式是否正确
   - 验证协议是否为HTTP或HTTPS

3. **必填字段验证**：
   - `integrationCode`：必填
   - `integrationName`：必填
   - `integrationType`：必填
   - `authType`：必填

**启用时校验**：
1. **API地址必须配置**：
   - 检查`apiUrl`是否为空
   - 如果为空，抛出异常："请先配置API地址"

2. **API密钥必须配置**：
   - 检查`apiKey`或`apiSecret`至少配置一个
   - 如果都未配置，抛出异常："请先配置API密钥"

**更新时校验**：
1. **配置存在性检查**：
   - 验证配置ID是否存在
   - 如果不存在，抛出异常："集成配置不存在"

2. **敏感信息修改审计**：
   - 检测`apiKey`或`apiSecret`是否被修改
   - 如果修改，记录警告日志：
     ```
     【敏感操作】集成配置敏感信息已修改: 
     integrationCode={code}, 
     operator={userId}, 
     operatorName={username}
     ```

### 6.6.4 API对接功能设计

#### 6.6.4.1 创建配置

**接口**：`POST /system/external-integration`

**权限检查**：
- 验证当前用户是否为管理员角色
- 如果不是，抛出异常："权限不足：只有管理员才能创建集成配置"

**创建流程**：
```
1. 权限验证
   └── 检查是否为管理员

2. 唯一性检查
   └── 检查integrationCode是否已存在

3. 创建配置实体
   ├── 设置基本信息
   ├── 加密存储API密钥（如果提供）
   └── 默认禁用状态（enabled = false）

4. 保存到数据库
   └── 记录操作日志
```

**返回结果**：
- 返回创建的配置DTO（API密钥已脱敏）

#### 6.6.4.2 更新配置

**接口**：`PUT /system/external-integration/{id}`

**权限检查**：
- 验证当前用户是否为管理员角色

**更新流程**：
```
1. 权限验证
   └── 检查是否为管理员

2. 配置存在性检查
   └── 验证配置ID是否存在

3. 更新字段
   ├── 更新基本信息
   ├── 如果apiKey有变化，重新加密存储
   ├── 如果apiSecret有变化，重新加密存储
   └── 更新操作时间

4. 敏感信息修改审计
   └── 如果修改了敏感信息，记录警告日志

5. 保存到数据库
```

**注意事项**：
- 如果`apiKey`或`apiSecret`字段为空，不会清空现有值
- 只有提供了新值才会更新

#### 6.6.4.3 启用/禁用配置

**启用接口**：`POST /system/external-integration/{id}/enable`

**启用流程**：
```
1. 权限验证
   └── 检查是否为管理员

2. 配置存在性检查

3. 启用前校验
   ├── API地址必须配置
   └── API密钥或Secret至少配置一个

4. 更新启用状态
   └── enabled = true

5. 记录操作日志
```

**禁用接口**：`POST /system/external-integration/{id}/disable`

**禁用流程**：
```
1. 权限验证
   └── 检查是否为管理员

2. 配置存在性检查

3. 更新禁用状态
   └── enabled = false

4. 记录操作日志
```

**状态影响**：
- 启用后：系统可使用该配置调用外部API
- 禁用后：系统停止使用该配置，相关功能会提示"未配置或未启用"

#### 6.6.4.4 测试连接

**接口**：`POST /system/external-integration/{id}/test`

**权限检查**：
- 验证当前用户是否为管理员角色

**测试流程**：
```
1. 权限验证
   └── 检查是否为管理员

2. 配置存在性检查

3. API地址检查
   └── 如果未配置，返回错误

4. 根据集成类型选择测试方式
   ├── AI类型集成
   │   └── 调用 /models 接口验证API Key有效性
   └── 其他类型集成
       └── 发送HTTP HEAD请求测试连通性

5. 记录测试结果
   ├── lastTestTime：当前时间
   ├── lastTestResult：SUCCESS/FAILED
   └── lastTestMessage：测试消息

6. 返回测试结果
```

**AI类型集成测试**：
- 调用`{apiUrl}/models`接口
- 请求头：`Authorization: Bearer {apiKey}`
- 验证响应码：
  - `200`：API Key验证成功
  - `401`：API Key无效或已过期
  - `403`：API Key权限不足
  - `429`：API调用次数超限
  - 其他：API验证失败

**其他类型集成测试**：
- 发送HTTP HEAD请求到`apiUrl`
- 设置连接超时：5秒
- 设置读取超时：5秒
- 验证响应码：
  - `200-399`：连接成功
  - 其他：连接失败

**测试结果展示**：
- 成功：显示"连接成功，响应码: 200"
- 失败：显示具体错误信息，如"API Key无效或已过期 (401)"

### 6.6.5 配置使用流程

#### 6.6.5.1 业务代码获取配置

业务代码通过服务层方法获取配置：

**获取方法**：
```java
// 获取第一个启用的客户服务系统配置
ExternalIntegration integration = 
    externalIntegrationAppService.getFirstEnabledIntegrationByType("CLIENT_SERVICE");

// 获取包含解密后API密钥的配置（仅内部使用）
ExternalIntegration integration = 
    externalIntegrationAppService.getIntegrationWithDecryptedKeys(integrationId);
```

**返回内容**：
- 包含解密后的`apiKey`和`apiSecret`（如果配置了）
- 仅用于内部API调用，不返回给前端

#### 6.6.5.2 API调用流程

业务代码使用配置调用外部API的流程：

```
1. 获取客户服务系统配置
   └── getFirstEnabledIntegrationByType("CLIENT_SERVICE")

2. 检查配置是否启用
   └── if (integration == null || !integration.getEnabled()) {
         // 配置未启用，返回错误
       }

3. 解密API密钥
   └── 使用AesEncryptionService解密apiKey

4. 构建请求
   ├── URL：integration.getApiUrl() + "/matter/receive"
   ├── 方法：POST
   ├── 请求头：
   │   ├── Content-Type: application/json
   │   └── Authorization: Bearer {解密后的apiKey}
   └── 请求体：JSON格式的项目数据

5. 发送HTTP请求
   └── 使用RestTemplate发送请求

6. 处理响应
   ├── 成功：提取id和accessUrl
   └── 失败：记录错误信息
```

### 6.6.6 配置状态管理

#### 6.6.6.1 状态字段

配置包含以下状态字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `enabled` | BOOLEAN | 是否启用 |
| `lastTestTime` | TIMESTAMP | 最后测试时间 |
| `lastTestResult` | VARCHAR(20) | 最后测试结果（SUCCESS/FAILED） |
| `lastTestMessage` | TEXT | 最后测试消息 |

#### 6.6.6.2 状态联动

**配置启用状态联动**：
- 配置启用后（`enabled = true`）：
  - 项目管理模块的推送功能可用
  - 系统可以使用该配置调用外部API
- 配置禁用后（`enabled = false`）：
  - 推送功能显示"客户服务系统未配置或未启用"
  - 系统停止使用该配置

**测试结果联动**：
- 测试成功（`lastTestResult = SUCCESS`）：
  - 配置可信度高，可以安全使用
- 测试失败（`lastTestResult = FAILED`）：
  - 配置可能存在问题，需要检查
  - 不影响配置的启用状态，但会显示警告

**状态展示**：
- 前端显示配置的连接状态图标：
  - ✅ 绿色：已启用且测试成功
  - ⚠️ 黄色：已启用但测试失败
  - ❌ 灰色：已禁用

---

## 六点七、数据库表结构说明

### 6.7.1 推送记录表（openapi_push_record）

记录向客户服务系统推送的项目数据历史。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | BIGINT | 主键 |
| `matter_id` | BIGINT | 项目ID |
| `client_id` | BIGINT | 客户ID |
| `push_type` | VARCHAR(20) | 推送类型：MANUAL（手动）、AUTO（自动）、UPDATE（更新） |
| `scopes` | VARCHAR(500) | 推送范围（逗号分隔） |
| `data_snapshot` | JSONB | 推送的数据快照（脱敏后的数据，JSON格式） |
| `external_id` | VARCHAR(100) | 客户服务系统返回的数据ID |
| `external_url` | VARCHAR(500) | 客户服务系统返回的客户访问链接 |
| `status` | VARCHAR(20) | 状态：PENDING（待推送）、SUCCESS（成功）、FAILED（失败） |
| `error_message` | TEXT | 错误信息 |
| `retry_count` | INTEGER | 重试次数 |
| `expires_at` | TIMESTAMP | 数据在客户服务系统中的有效期 |
| `created_at` | TIMESTAMP | 创建时间 |
| `created_by` | BIGINT | 创建人ID |
| `updated_at` | TIMESTAMP | 更新时间 |
| `deleted` | BOOLEAN | 是否删除 |
| `version` | INTEGER | 乐观锁版本号 |

**索引**：
- `idx_push_record_matter`：`matter_id`
- `idx_push_record_client`：`client_id`
- `idx_push_record_status`：`status`

### 6.7.2 推送配置表（openapi_push_config）

项目级别的客户服务推送设置。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | BIGINT | 主键 |
| `matter_id` | BIGINT | 项目ID（唯一） |
| `client_id` | BIGINT | 客户ID |
| `enabled` | BOOLEAN | 是否启用推送 |
| `scopes` | VARCHAR(500) | 默认推送范围（逗号分隔） |
| `auto_push_on_update` | BOOLEAN | 项目更新时是否自动推送 |
| `valid_days` | INTEGER | 数据有效期（天） |
| `created_at` | TIMESTAMP | 创建时间 |
| `created_by` | BIGINT | 创建人ID |
| `updated_at` | TIMESTAMP | 更新时间 |
| `updated_by` | BIGINT | 更新人ID |
| `deleted` | BOOLEAN | 是否删除 |
| `version` | INTEGER | 乐观锁版本号 |

**索引**：
- `idx_push_config_matter`：`matter_id`（唯一索引）

### 6.7.3 客户文件表（openapi_client_file）

存储客户服务系统推送的客户上传文件。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | BIGINT | 主键 |
| `matter_id` | BIGINT | 项目ID |
| `client_id` | BIGINT | 客户ID |
| `client_name` | VARCHAR(200) | 客户姓名 |
| `file_name` | VARCHAR(255) | 文件名 |
| `original_file_name` | VARCHAR(255) | 原始文件名 |
| `file_size` | BIGINT | 文件大小（字节） |
| `file_type` | VARCHAR(100) | 文件类型（MIME类型） |
| `file_category` | VARCHAR(50) | 文件类别：EVIDENCE（证据材料）、CONTRACT（合同文件）、ID_CARD（身份证件）、OTHER（其他） |
| `description` | TEXT | 文件描述 |
| `external_file_id` | VARCHAR(255) | 客服系统中的文件ID（唯一） |
| `external_file_url` | VARCHAR(1000) | 客服系统中的文件下载URL |
| `uploaded_by` | VARCHAR(100) | 上传人 |
| `uploaded_at` | TIMESTAMP | 上传时间 |
| `status` | VARCHAR(20) | 状态：PENDING（待同步）、SYNCED（已同步）、DELETED（已删除）、FAILED（同步失败） |
| `local_document_id` | BIGINT | 同步后的本地文档ID |
| `target_dossier_id` | BIGINT | 同步到的卷宗目录ID |
| `synced_at` | TIMESTAMP | 同步时间 |
| `synced_by` | BIGINT | 同步操作人 |
| `error_message` | TEXT | 错误信息 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 更新时间 |
| `created_by` | BIGINT | 创建人ID |
| `updated_by` | BIGINT | 更新人ID |
| `deleted` | BOOLEAN | 是否删除 |
| `version` | INTEGER | 乐观锁版本号 |

**索引**：
- `idx_client_file_matter`：`matter_id`
- `idx_client_file_client`：`client_id`
- `idx_client_file_status`：`status`
- `idx_client_file_external`：`external_file_id`（唯一索引）
- `idx_client_file_created`：`created_at`（降序）

### 6.7.4 外部集成配置表（sys_external_integration）

存储外部系统集成配置，包括客户服务系统、AI服务、OCR服务等。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | BIGINT | 主键 |
| `integration_code` | VARCHAR(50) | 集成编码（唯一） |
| `integration_name` | VARCHAR(100) | 集成名称 |
| `integration_type` | VARCHAR(50) | 集成类型：CLIENT_SERVICE、AI、OCR、ARCHIVE等 |
| `description` | TEXT | 描述 |
| `api_url` | VARCHAR(500) | API地址 |
| `api_key` | TEXT | API密钥（加密存储） |
| `api_secret` | TEXT | API密钥Secret（加密存储） |
| `auth_type` | VARCHAR(50) | 认证方式：API_KEY、BEARER_TOKEN、BASIC、OAUTH2、WEBHOOK |
| `extra_config` | JSONB | 额外配置（JSON格式） |
| `enabled` | BOOLEAN | 是否启用 |
| `last_test_time` | TIMESTAMP | 最后测试时间 |
| `last_test_result` | VARCHAR(20) | 最后测试结果：SUCCESS、FAILED |
| `last_test_message` | TEXT | 最后测试消息 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 更新时间 |
| `created_by` | BIGINT | 创建人ID |
| `updated_by` | BIGINT | 更新人ID |
| `deleted` | BOOLEAN | 是否删除 |
| `version` | INTEGER | 乐观锁版本号 |

**索引**：
- `idx_external_integration_code`：`integration_code`（唯一索引）
- `idx_external_integration_type`：`integration_type`
- `idx_external_integration_enabled`：`enabled`

---

## 六点八、API接口完整列表

### 6.8.1 推送相关接口

#### POST /matter/client-service/push
推送项目数据到客户服务系统。

**权限**：`matter:clientService:create`

**请求体**：
```json
{
  "matterId": 456,
  "clientId": 123,
  "scopes": ["MATTER_INFO", "MATTER_PROGRESS"],
  "validDays": 30,
  "documentIds": [789, 790],
  "remark": "首次推送"
}
```

**响应**：
```json
{
  "success": true,
  "code": "200",
  "data": {
    "id": 1001,
    "matterId": 456,
    "matterName": "张三诉李四合同纠纷案",
    "clientId": 123,
    "clientName": "张三",
    "pushType": "MANUAL",
    "scopes": ["MATTER_INFO", "MATTER_PROGRESS"],
    "status": "SUCCESS",
    "externalId": "external-matter-id-12345",
    "externalUrl": "https://client-service.example.com/portal/matter/12345",
    "expiresAt": "2026-03-02T10:00:00",
    "createdAt": "2026-02-02T10:00:00"
  }
}
```

#### GET /matter/client-service/records
获取推送记录列表（分页）。

**权限**：`matter:clientService:list`

**查询参数**：
- `matterId`（必填）：项目ID
- `status`（可选）：状态筛选（PENDING/SUCCESS/FAILED）
- `pageNum`（可选，默认1）：页码
- `pageSize`（可选，默认20）：每页数量

**响应**：
```json
{
  "success": true,
  "code": "200",
  "data": {
    "list": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

#### GET /matter/client-service/records/{id}
获取推送记录详情。

**权限**：`matter:clientService:list`

#### GET /matter/client-service/latest
获取最近一次成功推送。

**权限**：`matter:clientService:list`

**查询参数**：
- `matterId`（必填）：项目ID

#### GET /matter/client-service/config
获取或创建推送配置。

**权限**：`matter:clientService:list`

**查询参数**：
- `matterId`（必填）：项目ID
- `clientId`（必填）：客户ID

#### PUT /matter/client-service/config
更新推送配置。

**权限**：`matter:clientService:create`

**查询参数**：
- `matterId`（必填）：项目ID

**请求体**：
```json
{
  "enabled": true,
  "scopes": ["MATTER_INFO", "MATTER_PROGRESS"],
  "autoPushOnUpdate": false,
  "validDays": 30
}
```

#### GET /matter/client-service/statistics
获取推送统计信息。

**权限**：`matter:clientService:list`

**查询参数**：
- `matterId`（必填）：项目ID

**响应**：
```json
{
  "success": true,
  "code": "200",
  "data": {
    "totalPushCount": 10,
    "lastPushTime": "2026-02-02T10:00:00",
    "lastPushStatus": "SUCCESS",
    "externalUrl": "https://client-service.example.com/portal/matter/12345"
  }
}
```

#### GET /matter/client-service/scopes
获取可推送的数据范围选项。

**权限**：`matter:clientService:list`

**响应**：
```json
{
  "success": true,
  "code": "200",
  "data": [
    {
      "value": "MATTER_INFO",
      "label": "项目基本信息",
      "description": "项目名称、编号、类型、状态等"
    },
    ...
  ]
}
```

### 6.8.2 文件相关接口

#### GET /matter/client-files
获取客户文件列表（分页）。

**权限**：`matter:clientService:list`

**查询参数**：
- `matterId`（必填）：项目ID
- `status`（可选）：状态筛选（PENDING/SYNCED/DELETED/FAILED）
- `pageNum`（可选，默认1）：页码
- `pageSize`（可选，默认20）：每页数量

#### GET /matter/client-files/pending
获取待同步文件列表。

**权限**：`matter:clientService:list`

**查询参数**：
- `matterId`（必填）：项目ID

#### GET /matter/client-files/pending/count
获取待同步文件数量。

**权限**：`matter:clientService:list`

**查询参数**：
- `matterId`（必填）：项目ID

**响应**：
```json
{
  "success": true,
  "code": "200",
  "data": {
    "count": 5
  }
}
```

#### POST /matter/client-files/sync
同步文件到卷宗。

**权限**：`matter:clientService:create`

**请求体**：
```json
{
  "fileId": 789,
  "targetDossierId": 101
}
```

#### POST /matter/client-files/sync/batch
批量同步文件到卷宗。

**权限**：`matter:clientService:create`

**请求体**：
```json
[
  {
    "fileId": 789,
    "targetDossierId": 101
  },
  {
    "fileId": 790,
    "targetDossierId": 102
  }
]
```

#### POST /matter/client-files/{fileId}/ignore
忽略文件（不同步到卷宗）。

**权限**：`matter:clientService:create`

### 6.8.3 配置相关接口（系统管理）

#### GET /system/external-integration
获取集成配置列表（分页）。

**权限**：管理员角色

**查询参数**：
- `integrationType`（可选）：集成类型筛选
- `enabled`（可选）：是否启用筛选
- `keyword`（可选）：关键词搜索（名称、编码）
- `pageNum`（可选，默认1）：页码
- `pageSize`（可选，默认20）：每页数量

#### GET /system/external-integration/all
获取所有集成配置列表。

**权限**：管理员角色

#### GET /system/external-integration/{id}
获取集成配置详情。

**权限**：管理员角色

#### POST /system/external-integration
创建集成配置。

**权限**：管理员角色

**请求体**：
```json
{
  "integrationCode": "CLIENT_SERVICE",
  "integrationName": "客户服务系统",
  "integrationType": "CLIENT_SERVICE",
  "apiUrl": "https://client-service.example.com/api",
  "apiKey": "your-api-key-here",
  "apiSecret": "your-api-secret-here",
  "authType": "API_KEY",
  "extraConfig": {
    "notifyChannels": ["SMS", "WECHAT", "EMAIL"],
    "defaultValidDays": 30
  },
  "description": "客户服务系统对接配置"
}
```

#### PUT /system/external-integration/{id}
更新集成配置。

**权限**：管理员角色

#### POST /system/external-integration/{id}/enable
启用集成配置。

**权限**：管理员角色

#### POST /system/external-integration/{id}/disable
禁用集成配置。

**权限**：管理员角色

#### POST /system/external-integration/{id}/test
测试连接。

**权限**：管理员角色

**响应**：
```json
{
  "success": true,
  "code": "200",
  "data": {
    "id": 101,
    "integrationCode": "CLIENT_SERVICE",
    "integrationName": "客户服务系统",
    "lastTestTime": "2026-02-02T10:00:00",
    "lastTestResult": "SUCCESS",
    "lastTestMessage": "连接成功，响应码: 200"
  }
}
```

### 6.8.4 开放接口（供客户服务系统调用）

#### POST /open/client/files
接收客户上传的文件。

**权限**：无需认证（客户服务系统调用）

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
  "uploadedBy": "张三",
  "uploadedAt": "2026-01-15T10:00:00"
}
```

#### POST /open/client/files/deleted
文件删除回调。

**权限**：无需认证（客户服务系统调用）

**查询参数**：
- `externalFileId`（必填）：外部文件ID

---

## 六点九、权限和安全设计说明

### 6.9.1 权限设计

#### 6.9.1.1 项目级别权限

项目级别的客户服务功能权限：

| 权限码 | 权限名称 | 说明 | 适用角色 |
|--------|---------|------|----------|
| `matter:clientService:list` | 查看客户服务 | 查看推送记录和客户文件列表 | 项目参与者、管理员 |
| `matter:clientService:create` | 推送数据/同步文件 | 推送项目数据、同步客户文件到卷宗 | 项目参与者、管理员 |

**权限验证**：
- 所有项目级别的API接口都会验证用户是否为项目参与者
- 管理员可以操作所有项目
- 非项目参与者无法访问项目数据

#### 6.9.1.2 系统级别权限

系统级别的外部集成配置权限：

| 权限 | 说明 | 适用角色 |
|------|------|----------|
| 管理员角色 | 配置和管理外部系统集成 | ADMIN、SYSTEM_ADMIN、SUPER_ADMIN |

**权限验证**：
- 所有配置相关的API接口都会验证用户是否为管理员角色
- 非管理员无法访问配置功能

#### 6.9.1.3 权限分配

权限通过角色菜单关联表（`sys_role_menu`）分配：

**管理员角色**：
- `matter:clientService:list`
- `matter:clientService:create`
- 系统管理权限（外部集成配置）

**律师角色**：
- `matter:clientService:list`
- `matter:clientService:create`

### 6.9.2 安全设计

#### 6.9.2.1 API密钥加密存储

**加密算法**：AES-256-CBC

**密钥管理**：
- 加密密钥和IV从配置文件读取（`lawfirm.security.aes-key`、`lawfirm.security.aes-iv`）
- 使用SHA-256派生32字节密钥和16字节IV
- 密钥和IV不存储在数据库中

**存储流程**：
1. 管理员输入明文API密钥
2. 使用AES-256-CBC加密
3. Base64编码后存储到数据库

**读取流程**：
1. 从数据库读取密文
2. Base64解码
3. 使用AES-256-CBC解密
4. 返回明文（仅内部使用，不返回给前端）

**安全措施**：
- API密钥在数据库中始终以密文形式存储
- 前端显示时自动脱敏（前4位 + "****" + 后4位）
- 仅管理员在编辑时可查看完整密钥（解密后）
- 敏感信息修改时记录审计日志

#### 6.9.2.2 敏感信息脱敏

**脱敏规则**：
- 手机号：`138****5678`
- 邮箱：`z****@example.com`
- 身份证：`110101********0011`
- 姓名：`张**`
- 地址：保留前6位和后4位
- 费用金额：完全隐藏（`****`）

**脱敏位置**：
- 推送到客户服务系统的数据自动脱敏
- 前端显示时脱敏
- 日志记录时脱敏

#### 6.9.2.3 数据传输安全

**通信协议**：
- 生产环境：强制使用HTTPS
- 开发/测试环境：可使用HTTP

**请求认证**：
- 使用Bearer Token认证
- Token从配置的API密钥获取
- 每次请求都携带认证头

**数据完整性**：
- 使用JSON格式传输数据
- 请求和响应都进行格式验证
- 错误响应包含详细错误信息

#### 6.9.2.4 操作审计日志

**记录内容**：
- 推送操作：记录推送时间、操作人、推送范围、推送结果
- 文件同步：记录同步时间、操作人、文件信息、同步结果
- 配置修改：记录修改时间、操作人、修改内容
- 敏感信息修改：记录警告日志，包含操作人信息

**日志存储**：
- 推送记录保存在`openapi_push_record`表
- 文件同步记录保存在`openapi_client_file`表
- 配置修改记录在应用日志中
- 敏感信息修改记录警告级别的应用日志

#### 6.9.2.5 错误处理安全

**错误信息处理**：
- 不暴露系统内部错误详情给外部系统
- 不暴露数据库结构信息
- 不暴露敏感配置信息
- 错误信息仅记录在系统日志中

**异常处理**：
- 网络异常：记录错误，不抛出敏感信息
- 认证失败：返回401，不暴露密钥信息
- 数据验证失败：返回400，包含验证错误信息
- 系统异常：返回500，记录详细日志

#### 6.9.2.6 文件安全

**文件下载**：
- 使用临时下载URL（预签名URL）
- URL有效期：24小时
- URL包含签名验证，防止未授权访问

**文件存储**：
- 文件存储在MinIO对象存储中
- 存储路径：`matters/{matterId}/client_uploads/`
- 文件名使用UUID，防止文件名冲突和路径遍历攻击

**文件同步**：
- 验证文件来源（externalFileId）
- 验证文件大小和类型
- 同步后通知客户服务系统删除原文件

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
| v1.2 | 2026-02-02 | 添加项目管理模块推送功能设计和系统管理模块API对接配置设计 |
| v1.1 | 2026-01-15 | 完善文档，根据实际代码核实所有接口细节 |
| v1.0 | 2026-01-15 | 初始版本，包含基础对接功能 |

**v1.2 更新内容**：
- 添加"六点五、项目管理模块的项目详情推送功能设计"章节
  - 详细说明推送功能的UI设计、页面布局
  - 完整描述推送触发流程、确认弹窗设计、执行流程
  - 说明数据脱敏处理规则和实现方式
  - 介绍推送记录展示和配置管理功能
  - 详细说明客户文件接收、同步、忽略功能
- 添加"六点六、系统管理模块的API对接配置设计"章节
  - 详细说明配置表单字段和存储设计
  - 完整描述API密钥加密存储机制（AES-256-CBC）
  - 说明配置校验规则和错误处理
  - 介绍创建、更新、启用/禁用、测试连接功能
  - 说明配置使用流程和状态管理
- 添加"六点七、数据库表结构说明"章节
  - 详细说明推送记录表（openapi_push_record）结构
  - 详细说明推送配置表（openapi_push_config）结构
  - 详细说明客户文件表（openapi_client_file）结构
  - 详细说明外部集成配置表（sys_external_integration）结构
- 添加"六点八、API接口完整列表"章节
  - 推送相关接口（8个接口）
  - 文件相关接口（6个接口）
  - 配置相关接口（7个接口）
  - 开放接口（2个接口）
- 添加"六点九、权限和安全设计说明"章节
  - 详细说明项目级别和系统级别权限设计
  - 说明API密钥加密存储和安全措施
  - 说明敏感信息脱敏规则
  - 说明数据传输安全和操作审计日志
  - 说明错误处理和文件安全机制

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
