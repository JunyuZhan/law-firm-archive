# 函件二维码防伪验证系统设计文档

## 一、概述

本文档描述函件二维码防伪验证系统的完整设计方案，包括：
- 二维码生成和验证机制
- 公开验证API设计
- 数据同步方案
- 公开网站设计方案

## 二、系统架构

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│   内部系统      │         │   数据同步服务    │         │   公开网站      │
│  (内网部署)     │────────▶│   (可选中间层)    │────────▶│  (公网访问)     │
│                 │         │                  │         │                 │
│ - 函件管理      │         │ - 数据同步        │         │ - 验证查询      │
│ - 二维码生成    │         │ - 数据脱敏        │         │ - 客户服务      │
│ - 打印输出      │         │ - API网关        │         │ - 公众查询      │
└─────────────────┘         └──────────────────┘         └─────────────────┘
```

## 三、技术实现

### 3.1 基础设施层（Infrastructure）

#### 3.1.1 QrCodeService（二维码生成服务）

**位置**：`backend/src/main/java/com/lawfirm/infrastructure/qrcode/QrCodeService.java`

**功能**：
- 生成二维码图片（Base64编码）
- 生成二维码图片（字节数组）
- 支持自定义尺寸
- 支持添加Logo（可选）

**使用场景**：
- 函件防伪验证
- 合同电子签章
- 项目信息查询
- 其他需要二维码的业务场景

**示例代码**：
```java
@Autowired
private QrCodeService qrCodeService;

// 生成Base64编码的二维码
String qrCodeBase64 = qrCodeService.generateQrCodeBase64("https://verify.example.com/verify/letter?no=XXX&code=YYY", 200);

// 生成字节数组（用于下载）
byte[] qrCodeBytes = qrCodeService.generateQrCodeBytes("https://verify.example.com/verify/letter?no=XXX&code=YYY", 200);
```

#### 3.1.2 VerificationCodeService（验证码生成服务）

**位置**：`backend/src/main/java/com/lawfirm/infrastructure/qrcode/VerificationCodeService.java`

**功能**：
- 生成业务对象验证码（HMAC-SHA256）
- 验证验证码有效性
- 生成验证URL

**验证码生成算法**：
```
验证码 = Base64(HMAC-SHA256(业务类型|业务ID|业务编号|时间戳, 密钥))
```

**业务类型常量**：
- `BUSINESS_TYPE_LETTER` - 函件
- `BUSINESS_TYPE_CONTRACT` - 合同
- `BUSINESS_TYPE_MATTER` - 项目

**示例代码**：
```java
@Autowired
private VerificationCodeService verificationCodeService;

// 生成验证码
String code = verificationCodeService.generateCode(
    VerificationCodeService.BUSINESS_TYPE_LETTER,
    letterId,
    applicationNo,
    LocalDateTime.now()
);

// 验证验证码
boolean isValid = verificationCodeService.verifyCode(
    VerificationCodeService.BUSINESS_TYPE_LETTER,
    letterId,
    applicationNo,
    timestamp,
    verificationCode
);

// 生成验证URL
String url = verificationCodeService.generateVerificationUrl(
    "https://verify.example.com",
    VerificationCodeService.BUSINESS_TYPE_LETTER,
    applicationNo,
    code
);
```

### 3.2 应用层（Application）

#### 3.2.1 LetterVerificationService（函件验证服务）

**位置**：`backend/src/main/java/com/lawfirm/application/admin/service/LetterVerificationService.java`

**功能**：
- 生成函件验证码
- 验证函件真伪
- 生成函件验证二维码

**方法**：
- `generateVerificationCode(LetterApplication)` - 生成验证码
- `verifyCode(String applicationNo, String code)` - 验证验证码
- `generateVerificationUrl(LetterApplication)` - 生成验证URL
- `generateQrCodeBase64(LetterApplication, Integer)` - 生成二维码（Base64）
- `generateQrCodeBytes(LetterApplication, Integer)` - 生成二维码（字节数组）

### 3.3 接口层（Interfaces）

#### 3.3.1 LetterController（函件管理接口）

**新增接口**：

1. **获取函件验证二维码（Base64）**
   ```
   GET /api/admin/letter/application/{id}/qrcode?size=200
   ```
   返回：
   ```json
   {
     "success": true,
     "data": {
       "qrCodeBase64": "data:image/png;base64,...",
       "verificationUrl": "https://verify.example.com/verify/letter?no=XXX&code=YYY",
       "applicationNo": "LF20240101001"
     }
   }
   ```

2. **下载函件验证二维码图片**
   ```
   GET /api/admin/letter/application/{id}/qrcode/image?size=200
   ```
   返回：PNG图片（二进制流）

#### 3.3.2 PublicVerificationController（公开验证接口）

**位置**：`backend/src/main/java/com/lawfirm/interfaces/rest/open/PublicVerificationController.java`

**注意**：此接口不需要认证，面向公众开放

**接口**：

1. **验证函件真伪**
   ```
   GET /api/open/verify/letter?no={applicationNo}&code={verificationCode}
   ```
   返回：
   ```json
   {
     "success": true,
     "data": {
       "valid": true,
       "applicationNo": "LF20240101001",
       "letterTypeName": "介绍信",
       "targetUnit": "XX人民法院",
       "lawyerNames": "张三律师",
       "approvedAt": "2024-01-01 10:00:00",
       "printedAt": "2024-01-01 14:00:00",
       "message": "该函件真实有效"
     }
   }
   ```

## 四、前端实现

### 4.1 API接口

**文件**：`frontend/apps/web-antd/src/api/admin/letter.ts`

**新增接口**：
```typescript
/** 获取函件验证二维码（Base64） */
export function getLetterQrCode(id: number, size?: number) {
  return requestClient.get<{
    qrCodeBase64: string;
    verificationUrl: string;
    applicationNo: string;
  }>(`/admin/letter/application/${id}/qrcode`, {
    params: { size: size || 200 },
  });
}
```

### 4.2 打印功能集成

**文件**：`frontend/packages/utils/src/helpers/print.ts`

**修改**：
- `LetterPrintData` 接口添加 `qrCodeBase64?: string` 字段
- `generateLetterHtml()` 函数在页脚添加二维码显示

**二维码显示位置**：
- 位于函件页脚，盖章处下方
- 居中显示
- 包含提示文字："扫描二维码验证函件真伪"
- 显示函件编号

### 4.3 打印时自动获取二维码

**文件**：`frontend/apps/web-antd/src/views/admin/letter/index.vue`

**修改**：
- `handlePrintContent()` 函数改为异步
- 打印前调用 `getLetterQrCode()` 获取二维码
- 将二维码Base64数据传递给打印函数

## 五、数据同步方案

### 5.1 同步策略

由于系统可能部署在内网环境，需要设计数据同步机制，将验证所需的数据同步到公开网站。

#### 方案A：实时同步（推荐）

**实现方式**：
- 函件打印时，触发数据同步
- 同步到公开网站的数据库或缓存
- 公开网站直接查询同步的数据

**优点**：
- 数据实时性好
- 验证速度快

**缺点**：
- 需要网络连接
- 需要同步服务

#### 方案B：定时同步

**实现方式**：
- 定时任务（如每小时）同步已打印的函件数据
- 同步到公开网站

**优点**：
- 不依赖实时网络
- 实现简单

**缺点**：
- 数据有延迟
- 可能验证失败（数据未同步）

#### 方案C：API代理（推荐用于内网环境）

**实现方式**：
- 公开网站不存储数据
- 公开网站作为API代理，转发请求到内网系统
- 内网系统通过VPN或专线暴露API

**优点**：
- 数据不需要同步
- 数据实时性最好
- 安全性高（内网数据不暴露）

**缺点**：
- 需要网络连接
- 需要VPN或专线

### 5.2 同步数据内容

**需要同步的数据**（脱敏后）：
```typescript
interface LetterVerificationData {
  applicationNo: string;        // 申请编号
  letterTypeName: string;       // 函件类型名称
  targetUnit: string;           // 接收单位
  lawyerNames: string;          // 出函律师（脱敏）
  approvedAt: string;           // 审批时间
  printedAt: string;            // 打印时间
  verificationCode: string;     // 验证码
  // 不包含：客户信息、项目详情、函件内容等敏感信息
}
```

### 5.3 同步API设计

**接口**：`POST /api/admin/letter/sync/verification-data`

**请求体**：
```json
{
  "applicationId": 123,
  "syncTo": "PUBLIC_WEBSITE"  // 同步目标
}
```

**实现建议**：
- 可以集成到现有的外部系统集成功能中
- 使用 `ExternalIntegration` 实体配置同步目标
- 支持多个公开网站同步

## 六、公开网站设计方案

### 6.1 网站功能

#### 6.1.1 函件验证页面

**URL**：`https://verify.lawfirm.com/verify/letter`

**功能**：
- 输入申请编号和验证码
- 显示验证结果
- 显示函件基本信息（脱敏）

**页面设计**：
```
┌─────────────────────────────────────┐
│        函件真伪验证                  │
├─────────────────────────────────────┤
│  申请编号：[输入框]                  │
│  验证码：  [输入框]                  │
│  [验证] 按钮                         │
├─────────────────────────────────────┤
│  验证结果：                          │
│  ✓ 该函件真实有效                    │
│                                      │
│  函件类型：介绍信                    │
│  接收单位：XX人民法院                │
│  出函律师：张三律师                  │
│  审批时间：2024-01-01 10:00:00      │
│  打印时间：2024-01-01 14:00:00      │
└─────────────────────────────────────┘
```

#### 6.1.2 扫码验证页面

**URL**：`https://verify.lawfirm.com/verify/letter?no={no}&code={code}`

**功能**：
- 自动识别URL参数
- 自动调用验证API
- 显示验证结果

### 6.2 技术栈建议

**前端**：
- Vue 3 + Vite
- 响应式设计，支持移动端
- 简洁的UI设计

**后端**：
- Spring Boot（与主系统一致）
- 独立的数据库（仅存储验证数据）
- 或通过API代理访问主系统

### 6.3 部署方案

#### 方案1：独立部署（推荐）

- 公开网站独立部署在公网服务器
- 通过API同步数据
- 安全性高，不影响内网系统

#### 方案2：反向代理

- 公开网站部署在内网
- 通过Nginx反向代理暴露到公网
- 配置防火墙规则限制访问

## 七、配置说明

### 7.1 后端配置

**application.yml**：
```yaml
lawfirm:
  # 验证码密钥（生产环境必须修改）
  verification:
    secret: your-strong-secret-key-change-in-production
  
  # 公开验证网站URL
  public:
    verify:
      url: https://verify.lawfirm.com
```

### 7.2 前端配置

**环境变量**：
```env
VITE_PUBLIC_VERIFY_URL=https://verify.lawfirm.com
```

## 八、安全考虑

### 8.1 验证码安全性

- 使用HMAC-SHA256算法，不可逆
- 密钥存储在配置文件中，生产环境必须使用强密钥
- 验证码包含时间戳，防止重放攻击

### 8.2 数据脱敏

- 公开验证接口只返回必要信息
- 不返回客户信息、项目详情等敏感数据
- 不返回函件完整内容

### 8.3 API安全

- 公开验证接口不需要认证，但需要限流
- 建议使用API网关进行限流和防护
- 记录验证日志，监控异常访问

## 九、扩展性

### 9.1 支持其他业务对象

通过 `VerificationCodeService` 可以轻松扩展：
- 合同验证：`BUSINESS_TYPE_CONTRACT`
- 项目验证：`BUSINESS_TYPE_MATTER`
- 其他业务对象

### 9.2 短链接服务

可以集成短链接服务，生成更短的验证URL：
- 自建短链接服务
- 使用第三方服务（如新浪短链接）

## 十、实施步骤

### 阶段1：基础设施（已完成）
- ✅ 创建 `QrCodeService`
- ✅ 创建 `VerificationCodeService`
- ✅ 添加ZXing依赖

### 阶段2：后端API（进行中）
- ✅ 创建 `LetterVerificationService`
- ✅ 创建公开验证API
- ✅ 添加二维码获取API
- ⏳ 添加数据同步API

### 阶段3：前端集成（进行中）
- ✅ 添加前端API接口
- ✅ 修改打印函数支持二维码
- ⏳ 在打印预览中显示二维码

### 阶段4：公开网站（待实施）
- ⏳ 设计公开网站UI
- ⏳ 实现验证页面
- ⏳ 部署公开网站

### 阶段5：数据同步（待实施）
- ⏳ 实现数据同步服务
- ⏳ 配置同步规则
- ⏳ 测试同步功能

## 十一、测试建议

### 11.1 单元测试

- 测试验证码生成和验证
- 测试二维码生成
- 测试验证API

### 11.2 集成测试

- 测试打印时二维码生成
- 测试公开验证流程
- 测试数据同步

### 11.3 安全测试

- 测试验证码防重放
- 测试API限流
- 测试数据脱敏

## 十二、后续优化

1. **二维码美化**：添加Logo、边框等
2. **批量生成**：支持批量生成二维码
3. **统计功能**：统计验证次数、来源等
4. **通知功能**：验证时通知相关人员
5. **移动端优化**：优化移动端验证体验

