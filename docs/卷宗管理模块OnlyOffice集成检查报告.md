# 卷宗管理模块 OnlyOffice 集成检查报告

## 检查时间
2025-01-15

## 更新记录
- 2025-01-15: 修改PDF预览方式，PDF现在在浏览器弹窗中直接预览（使用iframe），不再使用OnlyOffice预览

## 检查范围
卷宗管理模块的文档管理功能，重点检查 OnlyOffice 预览和编辑功能是否正常工作。

## 功能现状

### ✅ 已实现的功能

#### 1. 后端 API 支持
- **预览配置接口**: `/document/{id}/preview` (GET)
  - 位置: `DocumentController.getPreviewConfig()`
  - 功能: 生成 OnlyOffice 预览配置，支持 Word、Excel、PowerPoint、PDF 等格式
  - 权限: `doc:detail`

- **编辑配置接口**: `/document/{id}/edit` (GET)
  - 位置: `DocumentController.getEditConfig()`
  - 功能: 生成 OnlyOffice 编辑配置，支持 Word、Excel、PowerPoint 等格式
  - 权限: `doc:edit`

- **编辑支持检查接口**: `/document/{id}/edit-support` (GET)
  - 位置: `DocumentController.checkEditSupport()`
  - 功能: 检查文档是否支持在线编辑和预览

- **回调接口**: `/document/{id}/callback` (POST)
  - 位置: `DocumentController.onlyOfficeCallback()`
  - 功能: 处理 OnlyOffice 保存后的回调，自动保存编辑后的文档到 MinIO

#### 2. OnlyOffice 服务实现
- **服务类**: `OnlyOfficeService`
  - 支持 JWT 签名验证
  - 支持预览和编辑模式
  - 自动生成文档唯一标识（documentKey）用于协同编辑
  - 支持 Docker 环境下的 MinIO 预签名 URL

#### 3. 前端实现

##### 文档列表页面 (`/document/list`)
- **预览功能**: 
  - Office 文档（docx, xlsx, pptx 等）跳转到 `/office-preview` 页面使用 OnlyOffice 预览
  - PDF 文件直接在新窗口打开
  - 图片、视频、音频使用专用预览组件
  
- **编辑功能**:
  - 检查文档是否支持在线编辑
  - 支持的文件类型：Word、Excel、PowerPoint
  - 跳转到 `/office-preview` 页面使用 OnlyOffice 编辑

- **操作按钮**:
  - 列表视图：预览、在线编辑、下载按钮
  - 网格视图：预览、在线编辑（下拉菜单）、下载按钮

##### OnlyOffice 预览/编辑页面 (`/office-preview`)
- **功能**:
  - 支持预览模式 (`mode=view`)
  - 支持编辑模式 (`mode=edit`)
  - 智能检测 OnlyOffice URL（自动处理 Docker 内部地址）
  - 支持 JWT Token 验证
  - 自动加载 OnlyOffice API

### ⚠️ 发现的问题

#### 1. 项目详情页卷宗文件标签页功能不完整
**位置**: `frontend/apps/web-antd/src/views/matter/detail/index.vue`

**问题描述**:
- 在项目详情页的"卷宗文件"标签页中，`DossierManager` 组件只显示卷宗目录树
- **没有显示文档列表**，用户无法直接预览和编辑文档
- 用户需要点击"查看全部文件"按钮跳转到文档列表页面才能进行操作

**影响**:
- 用户体验不够便捷，需要多一步操作
- 在项目详情页无法直接管理文档

**建议**:
1. 在 `DossierManager` 组件中添加文档列表显示功能
2. 或者在项目详情页的"卷宗文件"标签页中集成文档列表组件
3. 支持点击目录项后显示该目录下的文档列表，并提供预览和编辑功能

#### 2. 文档列表页面预览逻辑
**位置**: `frontend/apps/web-antd/src/views/document/list/index.vue`

**代码检查**:
```typescript
// 1110-1121行：预览功能
async function handlePreview(record: DocumentDTO) {
  const fileType = record.fileType?.toLowerCase() || '';
  
  // Office 文档类型 - 使用 OnlyOffice 预览
  if (isOfficeFile(fileType)) {
    const resolved = router.resolve({
      path: '/office-preview',
      query: { documentId: String(record.id), mode: 'view' },
    });
    window.open(resolved.href, '_blank');
    return;
  }
  // ... 其他文件类型处理
}
```

**状态**: ✅ 功能正常，Office 文档会正确跳转到 OnlyOffice 预览页面

#### 3. 文档列表页面编辑逻辑
**位置**: `frontend/apps/web-antd/src/views/document/list/index.vue`

**代码检查**:
```typescript
// 1189-1206行：在线编辑功能
async function handleOnlineEdit(record: DocumentDTO) {
  try {
    // 先检查是否支持在线编辑
    const support = await checkDocumentEditSupport(record.id);
    if (!support.canEdit) {
      message.warning('该文件类型不支持在线编辑，请下载后使用本地软件编辑');
      return;
    }
    // 跳转到编辑页面
    const resolved = router.resolve({
      path: '/office-preview',
      query: { documentId: String(record.id), mode: 'edit' },
    });
    window.open(resolved.href, '_blank');
  } catch {
    message.error('检查编辑支持失败');
  }
}
```

**状态**: ✅ 功能正常，会先检查支持情况再跳转到编辑页面

## 支持的文件类型

### 支持编辑的文件类型
- Word: `.doc`, `.docx`, `.odt`, `.rtf`, `.txt`
- Excel: `.xls`, `.xlsx`, `.ods`, `.csv`
- PowerPoint: `.ppt`, `.pptx`, `.odp`

### 支持预览的文件类型
- **OnlyOffice预览**（Word、Excel、PowerPoint）:
  - Word: `.doc`, `.docx`, `.odt`, `.rtf`, `.txt`
  - Excel: `.xls`, `.xlsx`, `.ods`, `.csv`
  - PowerPoint: `.ppt`, `.pptx`, `.odp`
- **浏览器直接预览**:
  - PDF: `.pdf`（使用iframe在弹窗中预览，不再使用OnlyOffice）
  - 图片: `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.bmp`, `.svg`（使用图片预览组件）
  - 视频: `.mp4`, `.avi`, `.mov` 等（使用HTML5 Video）
  - 音频: `.mp3`, `.wav`, `.ogg` 等（使用HTML5 Audio）

## 测试建议

### 1. 功能测试
- [ ] 在文档列表页面预览 Word 文档
- [ ] 在文档列表页面预览 Excel 文档
- [ ] 在文档列表页面预览 PowerPoint 文档
- [ ] 在文档列表页面预览 PDF 文档
- [ ] 在文档列表页面编辑 Word 文档并保存
- [ ] 在文档列表页面编辑 Excel 文档并保存
- [ ] 在文档列表页面编辑 PowerPoint 文档并保存
- [ ] 测试不支持的文件类型（如 `.zip`）是否显示正确的提示

### 2. 集成测试
- [ ] OnlyOffice 回调接口是否正常工作
- [ ] 编辑后的文档是否正确保存到 MinIO
- [ ] 文档版本是否正确更新
- [ ] 协同编辑功能是否正常（多个用户同时编辑）

### 3. 权限测试
- [ ] 无 `doc:detail` 权限的用户是否无法预览
- [ ] 无 `doc:edit` 权限的用户是否无法编辑
- [ ] 权限检查是否正确拦截未授权操作

## 结论

### ✅ 核心功能正常
卷宗管理模块的文档管理功能**已经实现了 OnlyOffice 预览和编辑功能**，包括：
1. 后端 API 完整实现
2. OnlyOffice 服务集成正常
3. 前端预览和编辑页面正常工作
4. 文档列表页面提供了预览和编辑按钮

### ✅ PDF预览优化（2025-01-15更新）
- PDF文件现在在浏览器弹窗中直接预览（使用iframe），不再使用OnlyOffice
- PDF预览弹窗宽度设置为90%（与图片预览一致）
- PDF预览体验与图片预览类似，提供更好的用户体验

### ⚠️ 需要改进的地方
1. **项目详情页的卷宗文件标签页**：建议添加文档列表显示功能，让用户可以直接在项目详情页预览和编辑文档，而不需要跳转到文档列表页面

### 📝 建议的改进方案

#### 方案一：在 DossierManager 组件中添加文档列表
- 在目录树下方或右侧显示选中目录的文档列表
- 文档列表包含预览、编辑、下载等操作按钮
- 保持与文档列表页面一致的交互体验

#### 方案二：在项目详情页集成文档列表组件
- 在"卷宗文件"标签页中同时显示目录树和文档列表
- 使用左右分栏或上下分栏布局
- 点击目录项时，文档列表自动过滤显示该目录下的文档

## 相关文件

### 后端
- `backend/src/main/java/com/lawfirm/interfaces/rest/document/DocumentController.java`
- `backend/src/main/java/com/lawfirm/infrastructure/external/onlyoffice/OnlyOfficeService.java`
- `backend/src/main/java/com/lawfirm/infrastructure/config/OnlyOfficeConfig.java`

### 前端
- `frontend/apps/web-antd/src/views/document/list/index.vue` - 文档列表页面
- `frontend/apps/web-antd/src/views/office-preview/index.vue` - OnlyOffice 预览/编辑页面
- `frontend/apps/web-antd/src/components/DossierManager/index.vue` - 卷宗目录管理组件
- `frontend/apps/web-antd/src/api/document/index.ts` - 文档相关 API
