# 卷宗材料自动归档功能开发计划

## 一、需求背景

当前系统的卷宗管理存在以下问题：
1. 卷宗目录项（如"收案审批表"、"委托合同"）只是占位符，需要用户手动上传
2. 但这些材料在系统其他模块中已经存在，造成重复工作
3. 业务流程已经产生的数据应该自动归档到卷宗

## 二、业务流程分析

```
创建客户 → 创建合同 → 合同审批（收案审批）→ 创建项目 → 项目卷宗
    ↓           ↓              ↓                    ↓
  客户信息    委托合同      审批记录             自动归档
```

## 三、自动归档材料清单

| 卷宗目录项 | 数据来源 | 触发时机 | 生成方式 |
|-----------|---------|---------|---------|
| 收案审批表 | 合同审批记录 | 项目创建时 | 基于模板生成PDF |
| 委托合同 | finance_contract | 项目创建时 | 关联已有合同/生成PDF |
| 授权委托书 | 项目+客户信息 | 项目创建时 | 基于模板生成PDF |
| 收费发票 | 财务开票记录 | 开票完成时 | 关联发票文件 |
| 结案审批表 | 结案审批记录 | 结案审批通过时 | 基于模板生成PDF |

## 四、开发任务

### 阶段一：数据模型增强 ✅ 已完成
- [x] 4.1.1 在 doc_document 表添加 source_type 和 source_id 字段
- [x] 4.1.2 更新 Document 实体类
- [x] 4.1.3 更新 DocumentDTO

### 阶段二：自动归档服务核心 ✅ 已完成
- [x] 4.2.1 创建 DossierAutoArchiveService 服务类
- [x] 4.2.2 实现卷宗目录项查找方法
- [x] 4.2.3 实现文档创建并关联到卷宗目录的方法

### 阶段三：收案审批表自动生成 ✅ 已完成
- [x] 4.3.1 创建收案审批表PDF模板
- [x] 4.3.2 实现审批表数据收集
- [x] 4.3.3 实现PDF生成逻辑
- [x] 4.3.4 在项目创建时触发自动归档

### 阶段四：委托合同自动归档 ✅ 已完成
- [x] 4.4.1 实现合同文档关联逻辑
- [x] 4.4.2 实现合同内容转PDF
- [x] 4.4.3 在项目创建时触发自动归档

### 阶段五：授权委托书自动生成 ✅ 已完成
- [x] 4.5.1 创建授权委托书PDF模板
- [x] 4.5.2 实现授权委托书数据收集
- [x] 4.5.3 实现PDF生成逻辑
- [x] 4.5.4 在项目创建时触发自动归档

### 阶段六：前端展示优化 ✅ 已完成
- [x] 4.6.1 卷宗目录显示文档来源标识
- [x] 4.6.2 支持上传签字版本
- [x] 4.6.3 区分系统生成版本和签字版本

## 五、技术方案

### 5.1 数据模型

```sql
-- doc_document 表新增字段
ALTER TABLE doc_document ADD COLUMN source_type VARCHAR(20);
-- SYSTEM_GENERATED: 系统自动生成
-- SYSTEM_LINKED: 系统自动关联（引用其他模块已有文件）
-- USER_UPLOADED: 用户上传
-- SIGNED_VERSION: 签字版本（用户上传的签字盖章版）

ALTER TABLE doc_document ADD COLUMN source_id BIGINT;
-- 关联的源数据ID（如合同ID、审批ID等）

ALTER TABLE doc_document ADD COLUMN source_module VARCHAR(50);
-- 来源模块：CONTRACT, APPROVAL, INVOICE 等
```

### 5.2 服务架构

```
MatterAppService (项目创建)
    ↓ 发布事件
MatterCreatedEvent
    ↓ 事件监听
DossierAutoArchiveService
    ├── archiveApprovalForm()      → 收案审批表
    ├── archiveContract()          → 委托合同
    └── archivePowerOfAttorney()   → 授权委托书
```

### 5.3 PDF生成方案

使用 Apache PDFBox 或 iText 生成PDF，基于HTML模板渲染。

## 六、测试要点

1. 创建合同并审批通过后创建项目，检查卷宗是否自动归档
2. 检查生成的PDF内容是否正确
3. 检查卷宗目录项的文档计数是否正确
4. 测试用户上传签字版本功能

## 七、开发进度

| 阶段 | 状态 | 开始时间 | 完成时间 |
|-----|------|---------|---------|
| 阶段一：数据模型 | ✅ 完成 | 2026-01-11 | 2026-01-11 |
| 阶段二：核心服务 | ✅ 完成 | 2026-01-11 | 2026-01-11 |
| 阶段三：收案审批表 | ✅ 完成 | 2026-01-11 | 2026-01-11 |
| 阶段四：委托合同 | ✅ 完成 | 2026-01-11 | 2026-01-11 |
| 阶段五：授权委托书 | ✅ 完成 | 2026-01-11 | 2026-01-11 |
| 阶段六：前端优化 | ✅ 完成 | 2026-01-11 | 2026-01-11 |

---

## 八、实现文件清单

### 后端新增/修改文件
1. `scripts/init-db/31-dossier-auto-archive.sql` - 数据库迁移脚本
2. `backend/.../entity/Document.java` - 添加 sourceType, sourceId, sourceModule 字段
3. `backend/.../dto/DocumentDTO.java` - 添加对应 DTO 字段
4. `backend/.../service/DossierAutoArchiveService.java` - 核心自动归档服务
5. `backend/.../service/PdfGeneratorService.java` - PDF 生成服务
6. `backend/.../service/MatterAppService.java` - 项目创建时触发归档
7. `backend/.../repository/DocumentRepository.java` - 添加查询方法
8. `backend/.../mapper/DocumentMapper.java` - 添加查询方法
9. `backend/.../util/SecurityUtils.java` - 添加 getUserIdOrDefault 方法
10. `backend/.../rest/DocumentController.java` - 支持 sourceType 参数
11. `backend/.../service/DocumentAppService.java` - 上传支持 sourceType

### 前端新增/修改文件
1. `frontend/.../api/document/types.ts` - 添加 sourceType 类型定义
2. `frontend/.../api/document/index.ts` - uploadFiles 支持 sourceType
3. `frontend/.../views/document/list/index.vue` - 显示来源标识、上传签字版本

---
*最后更新时间：2026-01-11 完成所有开发任务*

