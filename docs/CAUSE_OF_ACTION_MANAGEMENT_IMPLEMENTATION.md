# 案由管理功能实现总结

**实现时间**：2026-01-17  
**功能状态**：✅ 后端API已完成

---

## 📋 实现概览

已成功实现案由/罪名的完整CRUD管理功能，支持通过系统界面进行案由/罪名的增删改查操作。

---

## ✅ 已完成的工作

### 1. 后端实现

#### 1.1 Command类

**文件**：
- `backend/src/main/java/com/lawfirm/application/system/command/CreateCauseCommand.java`
- `backend/src/main/java/com/lawfirm/application/system/command/UpdateCauseCommand.java`

**功能**：
- ✅ 创建案由命令（包含所有必需字段和验证）
- ✅ 更新案由命令（支持部分字段更新）

#### 1.2 Repository层

**文件**：
- `backend/src/main/java/com/lawfirm/domain/system/repository/CauseOfActionRepository.java`

**功能**：
- ✅ 继承 `AbstractRepository`，提供基础CRUD操作
- ✅ 支持 `getByIdOrThrow` 等便捷方法

#### 1.3 Mapper层扩展

**文件**：
- `backend/src/main/java/com/lawfirm/infrastructure/persistence/mapper/CauseOfActionMapper.java`

**新增方法**：
- ✅ `findByParentCode` - 根据父代码获取子案由
- ✅ `existsByCodeAndType` - 检查代码是否存在
- ✅ `getMaxSortOrder` - 获取最大排序号

#### 1.4 Service层扩展

**文件**：
- `backend/src/main/java/com/lawfirm/application/system/service/CauseOfActionService.java`

**新增方法**：
- ✅ `createCause` - 创建案由/罪名
  - 代码格式验证（民事/刑事/行政）
  - 代码唯一性检查
  - 层级关系验证
  - 自动生成排序号
  - 缓存清除

- ✅ `updateCause` - 更新案由/罪名
  - 层级关系验证
  - 字段更新
  - 缓存清除

- ✅ `deleteCause` - 删除案由/罪名（软删除）
  - 检查子案由
  - 设置为禁用状态
  - 缓存清除

- ✅ `getCauseById` - 获取案由详情

- ✅ `toggleCauseStatus` - 启用/禁用案由
  - 状态切换
  - 缓存清除

**验证规则**：
- ✅ 代码格式验证：
  - 民事案由：`数字` 或 `数字.数字`（如：`1`, `8.1`）
  - 刑事罪名：`C + 数字`（如：`C101`）
  - 行政案由：`A + 数字` 或 `A + 数字.数字`（如：`A1`, `A1.1`）

- ✅ 层级关系验证：
  - 二级案由必须指定父级案由代码
  - 父级案由必须存在且为一级案由

- ✅ 代码唯一性验证：
  - 同一类型下代码必须唯一

#### 1.5 Controller层扩展

**文件**：
- `backend/src/main/java/com/lawfirm/interfaces/rest/system/CauseOfActionController.java`

**新增接口**：
- ✅ `GET /causes/{id}` - 获取案由详情
- ✅ `POST /causes` - 创建案由/罪名
  - 权限：`system:cause:create`
  - 操作日志：创建案由

- ✅ `PUT /causes/{id}` - 更新案由/罪名
  - 权限：`system:cause:update`
  - 操作日志：更新案由

- ✅ `DELETE /causes/{id}` - 删除案由/罪名
  - 权限：`system:cause:delete`
  - 操作日志：删除案由

- ✅ `POST /causes/{id}/toggle` - 启用/禁用案由
  - 权限：`system:cause:update`
  - 操作日志：切换案由状态

**保留的查询接口**：
- ✅ `GET /causes/civil/tree` - 获取民事案由树
- ✅ `GET /causes/criminal/tree` - 获取刑事罪名树
- ✅ `GET /causes/admin/tree` - 获取行政案由树
- ✅ `GET /causes/search` - 搜索案由
- ✅ `GET /causes/name` - 获取案由名称

### 2. 权限配置

**文件**：
- `scripts/init-db/20-init-data.sql`

**新增菜单**：
- ✅ 案由管理主菜单（ID: 160）
  - 路径：`/system/cause-of-action`
  - 权限：`system:cause:list`
  - 图标：`FileTextOutlined`

- ✅ 创建案由按钮（ID: 161）
  - 权限：`system:cause:create`

- ✅ 更新案由按钮（ID: 162）
  - 权限：`system:cause:update`

- ✅ 删除案由按钮（ID: 163）
  - 权限：`system:cause:delete`

---

## 📊 API接口文档

### 创建案由

**请求**：
```http
POST /causes
Content-Type: application/json

{
  "code": "8.3",
  "name": "个人信息保护纠纷（新增）",
  "causeType": "CIVIL",
  "categoryCode": "P1",
  "categoryName": "人格权纠纷",
  "parentCode": "8",
  "level": 2,
  "sortOrder": 803,
  "isActive": true
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "code": "8.3",
    "name": "个人信息保护纠纷（新增）",
    "causeType": "CIVIL",
    "categoryCode": "P1",
    "categoryName": "人格权纠纷",
    "parentCode": "8",
    "level": 2,
    "sortOrder": 803,
    "isActive": true
  }
}
```

### 更新案由

**请求**：
```http
PUT /causes/{id}
Content-Type: application/json

{
  "code": "8.3",
  "name": "个人信息保护纠纷（已更新）",
  "categoryCode": "P1",
  "categoryName": "人格权纠纷",
  "parentCode": "8",
  "level": 2,
  "sortOrder": 803,
  "isActive": true
}
```

### 删除案由

**请求**：
```http
DELETE /causes/{id}
```

**说明**：软删除，将 `isActive` 设置为 `false`

### 启用/禁用案由

**请求**：
```http
POST /causes/{id}/toggle
```

---

## 🔒 权限说明

| 权限代码 | 说明 | 菜单ID |
|---------|------|--------|
| `system:cause:list` | 查看案由列表 | 160 |
| `system:cause:create` | 创建案由 | 161 |
| `system:cause:update` | 更新案由 | 162 |
| `system:cause:delete` | 删除案由 | 163 |

---

## ⚠️ 注意事项

### 1. 系统内置案由

**当前实现**：
- 所有案由都可以通过API进行管理
- 建议在前端界面中区分系统内置案由和自定义案由

**建议**：
- 可以通过 `created_by` 字段判断（系统初始化时 `created_by` 为 NULL）
- 或添加 `is_system` 字段标记系统内置案由

### 2. 代码格式

**严格验证**：
- 民事案由：必须符合 `数字` 或 `数字.数字` 格式
- 刑事罪名：必须符合 `C + 数字` 格式
- 行政案由：必须符合 `A + 数字` 或 `A + 数字.数字` 格式

**错误示例**：
- ❌ 民事案由使用 `C1`（应为 `1`）
- ❌ 刑事罪名使用 `101`（应为 `C101`）
- ❌ 行政案由使用 `1`（应为 `A1`）

### 3. 层级关系

**规则**：
- 一级案由：`parentCode` 为 `NULL`，`level = 1`
- 二级案由：`parentCode` 必须指向存在的一级案由，`level = 2`

**验证**：
- 创建二级案由时，系统会自动验证父级案由是否存在
- 删除一级案由时，系统会检查是否有子案由

### 4. 缓存管理

**自动清除**：
- 创建、更新、删除案由时，会自动清除相关缓存
- 缓存key：`causeTree`、`causeName`

---

## 🚀 后续工作（前端实现）

### 待实现功能

1. **前端页面**：
   - [ ] 创建案由管理页面：`frontend/apps/web-antd/src/views/system/cause-of-action/index.vue`
   - [ ] 案由类型切换（民事/刑事/行政）
   - [ ] 树形列表展示
   - [ ] 创建/编辑表单
   - [ ] 删除确认对话框

2. **API接口**：
   - [ ] 在 `frontend/apps/web-antd/src/api/system/cause-of-action.ts` 中添加CRUD方法

3. **功能增强**：
   - [ ] 批量导入（Excel/CSV）
   - [ ] 拖拽排序
   - [ ] 系统内置案由标识
   - [ ] 数据导出

---

## 📝 测试建议

### 1. 单元测试

```java
// 测试代码格式验证
@Test
void testCreateCause_InvalidCodeFormat() {
    CreateCauseCommand command = new CreateCauseCommand();
    command.setCode("INVALID");
    command.setCauseType("CIVIL");
    // 应该抛出 BusinessException
}

// 测试层级关系验证
@Test
void testCreateCause_InvalidParent() {
    CreateCauseCommand command = new CreateCauseCommand();
    command.setCode("8.3");
    command.setParentCode("999"); // 不存在的父级
    // 应该抛出 BusinessException
}
```

### 2. 集成测试

- 测试完整的CRUD流程
- 测试权限控制
- 测试缓存清除

---

## ✅ 总结

**已完成**：
- ✅ 后端API完整实现
- ✅ 数据验证和业务逻辑
- ✅ 权限配置
- ✅ 缓存管理

**待完成**：
- ⏳ 前端页面实现
- ⏳ 前端API接口
- ⏳ 功能测试

**状态**：后端功能已完成，可以开始前端开发。
