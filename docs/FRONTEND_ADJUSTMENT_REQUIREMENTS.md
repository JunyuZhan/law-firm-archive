# 前端调整需求文档

> **更新时间**：2026-01-27  
> **调整原因**：数据库标准化项目完成后，后端DTO已更新，前端需要同步调整

---

## 📋 调整概览

数据库标准化项目完成后，后端DTO添加了新的存储结构字段。前端需要更新TypeScript类型定义以匹配后端返回的数据结构。

---

## ✅ 需要调整的内容

### 1. 所有包含文件存储的DTO类型定义 ✅ **已完成调整**

已更新以下DTO类型，添加了MinIO存储结构字段：

1. ✅ **SealApplicationDTO** - 用印申请
2. ✅ **ContractDTO** - 合同
3. ✅ **ExpenseDTO** - 费用
4. ✅ **InvoiceDTO** - 发票
5. ✅ **DocumentDTO** - 文档（2个文件）
6. ✅ **EvidenceDTO** - 证据（2个文件）
7. ✅ **ReportDTO** - 报表
8. ✅ **ApprovalDTO** - 审批

### 2. SealApplicationDTO 类型定义 ✅ **已完成**

**文件位置**：
- `frontend/apps/web-antd/src/api/document/seal.ts`
- `frontend/apps/web-antd/src/api/document/seal-types.ts`

**当前状态**：缺少存储结构字段

**需要添加的字段**：
```typescript
export interface SealApplicationDTO {
  // ... 现有字段 ...
  
  /**
   * 附件文件URL（向后兼容字段）
   */
  attachmentUrl?: string;
  
  /**
   * MinIO桶名称，默认law-firm
   */
  bucketName?: string;
  
  /**
   * 存储路径：seal/M_{matterId}/{YYYY-MM}/用印附件/
   */
  storagePath?: string;
  
  /**
   * 物理文件名：{YYYYMMDD}_{UUID}_{documentName}.{ext}
   */
  physicalName?: string;
  
  /**
   * 文件Hash值（SHA-256），用于去重和校验
   */
  fileHash?: string;
}
```

**调整说明**：
- 这些字段都是可选的（`?`），因为新创建的申请可能还没有上传附件
- 字段会在上传附件后自动填充
- `attachmentUrl` 是向后兼容字段，前端可以继续使用

---

### 2. TaskComment 相关类型定义 ⚠️ **需要检查**

**文件位置**：需要检查是否存在任务评论相关的API定义

**调整内容**：
如果前端有任务评论相关的代码，需要确保 `attachments` 字段支持新的JSONB格式：

**旧格式（字符串数组）**：
```typescript
attachments?: string[]; // 文件URL数组
```

**新格式（JSONB对象数组）**：
```typescript
attachments?: Array<{
  bucket_name: string;      // MinIO桶名称
  storage_path: string;     // 存储路径
  physical_name: string;    // 物理文件名
  file_hash: string;        // 文件Hash（SHA-256）
  original_name: string;    // 原始文件名
  file_url: string;         // 文件URL（向后兼容）
  file_size?: number;       // 文件大小（字节）
  mime_type?: string;       // MIME类型
}>;
```

**向后兼容**：
- 后端支持两种格式：
  - 旧格式：字符串数组（`["url1", "url2"]`）
  - 新格式：JSONB对象数组（包含完整存储信息）
- 前端可以继续使用 `file_url` 字段来显示和下载文件
- 如果需要使用新的存储结构字段，可以访问对象中的相应字段

---

### 3. 文件上传接口 ⚠️ **需要检查**

**接口**：`POST /api/tasks/{taskId}/comments/upload-attachment`

**返回格式**：
```typescript
{
  success: boolean;
  code: string;
  message: string;
  data: {
    bucket_name: string;
    storage_path: string;
    physical_name: string;
    file_hash: string;
    original_name: string;
    file_url: string;
    file_size?: number;
    mime_type?: string;
  };
}
```

**前端使用建议**：
- 可以直接使用返回的 `data` 对象添加到 `attachments` 数组
- 或者只使用 `file_url` 字段（向后兼容）

---

## 📝 调整步骤

### ✅ 步骤1：更新所有DTO类型定义（已完成）

已更新以下文件：
1. ✅ `frontend/apps/web-antd/src/api/document/seal.ts` - SealApplicationDTO
2. ✅ `frontend/apps/web-antd/src/api/document/seal-types.ts` - SealApplicationDTO
3. ✅ `frontend/apps/web-antd/src/api/finance/types.ts` - ContractDTO, ExpenseDTO, InvoiceDTO
4. ✅ `frontend/apps/web-antd/src/api/document/types.ts` - DocumentDTO
5. ✅ `frontend/apps/web-antd/src/api/document/index.ts` - DocumentDTO
6. ✅ `frontend/apps/web-antd/src/api/evidence/types.ts` - EvidenceDTO
7. ✅ `frontend/apps/web-antd/src/api/evidence/index.ts` - EvidenceDTO
8. ✅ `frontend/apps/web-antd/src/api/workbench/report.ts` - ReportDTO
9. ✅ `frontend/apps/web-antd/src/api/workbench/index.ts` - ApprovalDTO

### 步骤2：检查任务评论相关代码

1. 搜索前端代码中是否有任务评论相关的API调用
2. 如果有，检查 `attachments` 字段的使用方式
3. 根据实际情况决定是否需要调整

### 步骤3：测试验证

1. 测试用印申请详情页面，确认新字段不会导致错误
2. 测试任务评论附件上传功能（如果前端已实现）
3. 确认文件上传/下载功能正常

---

## ⚠️ 注意事项

### 向后兼容性

1. **attachmentUrl 字段**：
   - 后端保留了 `attachmentUrl` 字段用于向后兼容
   - 前端可以继续使用这个字段，无需立即调整

2. **attachments 字段格式**：
   - 后端支持两种格式（字符串数组和JSONB对象数组）
   - 前端可以继续使用字符串数组格式
   - 如果需要使用新的存储结构信息，可以访问对象字段

### 可选调整

以下调整是**可选的**，不影响现有功能：

1. **使用新的存储结构字段**：
   - 如果前端需要显示文件大小、MIME类型等信息
   - 或者需要使用文件Hash进行去重检查
   - 可以调整代码以使用新的JSONB格式

2. **文件下载优化**：
   - 可以使用 `storagePath` 和 `physicalName` 构建更精确的文件路径
   - 但这需要后端提供相应的下载接口支持

---

## 🔍 检查清单

- [x] 更新 `SealApplicationDTO` 类型定义（添加存储结构字段）
- [x] 更新 `ContractDTO` 类型定义（添加存储结构字段）
- [x] 更新 `ExpenseDTO` 类型定义（添加存储结构字段）
- [x] 更新 `InvoiceDTO` 类型定义（添加存储结构字段）
- [x] 更新 `DocumentDTO` 类型定义（添加存储结构字段）
- [x] 更新 `EvidenceDTO` 类型定义（添加存储结构字段）
- [x] 更新 `ReportDTO` 类型定义（添加存储结构字段）
- [x] 更新 `ApprovalDTO` 类型定义（添加存储结构字段）
- [ ] 检查任务评论相关代码（如果存在）
- [ ] 测试相关页面和功能
- [ ] 确认向后兼容性

---

## 📊 优先级

| 调整项 | 优先级 | 是否必须 | 状态 |
|--------|--------|----------|------|
| 所有DTO类型定义 | 🟡 中 | 建议（避免TypeScript类型错误） | ✅ 已完成 |
| TaskComment attachments格式 | 🟢 低 | 可选（向后兼容） | ⏳ 待检查 |
| 文件上传接口调用 | 🟢 低 | 可选（如果前端已实现） | ⏳ 待检查 |

---

## 📚 参考文档

- 后端DTO定义：`backend/src/main/java/com/lawfirm/application/document/dto/SealApplicationDTO.java`
- 数据库schema：`db_schema_final.sql`
- 项目完成报告：`docs/FINAL_PROJECT_COMPLETION_REPORT.md`

---

**更新时间**：2026-01-27  
**状态**：✅ 前端DTO类型定义已全部更新完成，共更新8个DTO类型，涉及9个文件
