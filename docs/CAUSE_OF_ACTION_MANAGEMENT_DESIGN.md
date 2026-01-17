# 案由/罪名管理功能设计文档

**生成时间**：2026-01-17  
**问题**：能否通过字典管理来增减案由/罪名？

---

## 📋 问题分析

### 当前状态

1. **案由/罪名数据存储**：
   - 表名：`sys_cause_of_action`
   - 数据量：1,627条（民事984 + 刑事483 + 行政160）
   - 数据来源：官方文档（法〔2025〕226号、227号等）

2. **现有功能**：
   - ✅ 查询接口：`CauseOfActionController`（只读）
   - ✅ 树形结构查询
   - ✅ 搜索功能
   - ❌ **缺少增删改功能**

3. **字典管理功能**：
   - 表结构：`sys_dict_type` + `sys_dict_item`
   - 功能：完整的CRUD管理
   - 用途：简单的键值对（如性别、学历等）

---

## 🔍 结构对比分析

### 案由/罪名表结构

```sql
sys_cause_of_action (
    id BIGSERIAL,
    code VARCHAR(20),              -- 案由代码（如 '8.1', 'C101', 'A1.1'）
    name VARCHAR(200),              -- 案由名称
    cause_type VARCHAR(20),         -- 类型: CIVIL, CRIMINAL, ADMIN
    category_code VARCHAR(20),      -- 所属大类代码（如 'P2', 'C1'）
    category_name VARCHAR(100),     -- 所属大类名称
    parent_code VARCHAR(20),        -- 父级案由代码（支持层级）
    level INT,                      -- 层级: 1=一级, 2=二级
    sort_order INT,                 -- 排序号
    is_active BOOLEAN               -- 是否启用
)
```

**特点**：
- ✅ 支持层级关系（parent_code）
- ✅ 支持分类管理（category_code, category_name）
- ✅ 支持多类型（民事/刑事/行政）
- ✅ 代码格式复杂（数字、字母+数字、层级代码）

### 字典管理表结构

```sql
sys_dict_type (
    id BIGINT,
    name VARCHAR(100),              -- 字典名称
    code VARCHAR(50),               -- 字典编码
    description VARCHAR(500),
    status VARCHAR(20),
    is_system BOOLEAN
)

sys_dict_item (
    id BIGINT,
    dict_type_id BIGINT,            -- 关联字典类型
    label VARCHAR(100),              -- 显示标签
    value VARCHAR(100),              -- 字典值
    description VARCHAR(500),
    sort_order INT,
    status VARCHAR(20)
)
```

**特点**：
- ✅ 简单的键值对结构
- ✅ 支持字典类型分组
- ❌ **不支持层级关系**
- ❌ **不支持复杂分类**
- ❌ **不支持多类型区分**

---

## ❌ 结论：不能直接用字典管理

### 原因分析

1. **结构不匹配**：
   - 案由/罪名需要层级关系（parent_code），字典不支持
   - 案由/罪名需要分类信息（category_code, category_name），字典不支持
   - 案由/罪名需要类型区分（CIVIL/CRIMINAL/ADMIN），字典只有单一类型

2. **代码格式复杂**：
   - 民事案由：`'1'`, `'8.1'`, `'14'`
   - 刑事罪名：`'C101'`, `'C717'`
   - 行政案由：`'A1'`, `'A1.1'`, `'A22'`
   - 字典只支持简单的 `value` 字段

3. **业务逻辑不同**：
   - 案由/罪名有严格的层级和分类体系
   - 字典管理主要用于简单的枚举值

---

## ✅ 推荐方案：创建专门的案由管理功能

### 方案概述

基于现有的 `sys_cause_of_action` 表，创建完整的CRUD管理功能，参考字典管理的UI设计，但适配案由/罪名的复杂结构。

### 功能需求

#### 1. 后端API设计

**新增接口**（在 `CauseOfActionController` 中添加）：

```java
// 创建案由/罪名
@PostMapping
public Result<CauseOfAction> createCause(@RequestBody CreateCauseCommand command)

// 更新案由/罪名
@PutMapping("/{id}")
public Result<CauseOfAction> updateCause(@PathVariable Long id, @RequestBody UpdateCauseCommand command)

// 删除案由/罪名（软删除或禁用）
@DeleteMapping("/{id}")
public Result<Void> deleteCause(@PathVariable Long id)

// 批量导入案由/罪名
@PostMapping("/batch")
public Result<Integer> batchImport(@RequestBody List<CreateCauseCommand> commands)

// 获取案由/罪名详情
@GetMapping("/{id}")
public Result<CauseOfAction> getCause(@PathVariable Long id)

// 获取案由/罪名列表（分页）
@GetMapping
public Result<PageResult<CauseOfAction>> listCauses(
    @RequestParam String type,
    @RequestParam(required = false) String categoryCode,
    @RequestParam(required = false) String keyword,
    @RequestParam(defaultValue = "1") Integer pageNum,
    @RequestParam(defaultValue = "20") Integer pageSize
)
```

#### 2. 前端页面设计

**页面路径**：`/system/cause-of-action`

**功能模块**：

1. **案由类型切换**：
   - 标签页：民事案由 | 刑事罪名 | 行政案由

2. **树形列表**：
   - 左侧：树形结构（按分类和层级展示）
   - 右侧：列表/详情

3. **操作功能**：
   - ✅ 新增（支持一级/二级）
   - ✅ 编辑
   - ✅ 删除（软删除，设置 is_active=false）
   - ✅ 启用/禁用
   - ✅ 排序调整
   - ✅ 批量导入（Excel/CSV）

4. **搜索过滤**：
   - 按分类筛选
   - 按关键词搜索
   - 按启用状态筛选

#### 3. 数据验证规则

**创建/更新时的验证**：

```java
// 1. 代码唯一性验证
- 同一类型（cause_type）下，code 必须唯一
- 检查 UNIQUE(code, cause_type) 约束

// 2. 层级关系验证
- 二级案由必须指定 parent_code
- parent_code 必须存在且为一级案由
- level 必须与 parent_code 匹配

// 3. 代码格式验证
- 民事案由：数字或 数字.数字（如 '1', '8.1'）
- 刑事罪名：C + 数字（如 'C101'）
- 行政案由：A + 数字 或 A + 数字.数字（如 'A1', 'A1.1'）

// 4. 分类验证
- category_code 和 category_name 必须匹配
- 分类必须符合对应类型的规范
```

#### 4. 权限控制

**菜单权限**：
- `system:cause:list` - 查看案由列表
- `system:cause:create` - 创建案由
- `system:cause:update` - 更新案由
- `system:cause:delete` - 删除案由
- `system:cause:import` - 批量导入

**数据权限**：
- 系统内置案由（官方案由）建议只读，不允许删除
- 自定义案由可以完全管理

---

## 🎯 实现步骤

### 阶段一：后端API开发

1. **创建Command类**：
   ```java
   CreateCauseCommand
   UpdateCauseCommand
   ```

2. **扩展Service**：
   - 在 `CauseOfActionService` 中添加 CRUD 方法
   - 添加数据验证逻辑
   - 添加批量导入功能

3. **扩展Controller**：
   - 在 `CauseOfActionController` 中添加增删改接口

4. **添加权限配置**：
   - 在菜单数据中添加案由管理菜单
   - 配置相关权限

### 阶段二：前端页面开发

1. **创建页面组件**：
   - `src/views/system/cause-of-action/index.vue`
   - 参考字典管理页面的设计

2. **API接口**：
   - 在 `src/api/system/cause-of-action.ts` 中添加 CRUD 方法

3. **表单组件**：
   - 创建/编辑表单
   - 支持选择父级案由
   - 支持选择分类

4. **树形组件**：
   - 使用 Ant Design Vue 的 Tree 组件
   - 支持拖拽排序（可选）

### 阶段三：数据迁移和初始化

1. **标记系统内置案由**：
   - 在表中添加 `is_system` 字段（可选）
   - 初始化数据标记为系统内置

2. **数据验证脚本**：
   - 验证现有数据的完整性
   - 修复数据问题

---

## 📊 与字典管理的对比

| 特性 | 字典管理 | 案由管理（推荐方案） |
|------|---------|-------------------|
| **表结构** | sys_dict_type + sys_dict_item | sys_cause_of_action |
| **层级关系** | ❌ 不支持 | ✅ 支持（parent_code） |
| **分类管理** | ❌ 简单分组 | ✅ 完整分类体系 |
| **多类型** | ❌ 单一类型 | ✅ 三种类型（民事/刑事/行政） |
| **代码格式** | 简单字符串 | 复杂格式（数字、字母+数字） |
| **业务逻辑** | 简单枚举 | 复杂业务规则 |
| **UI设计** | 可参考 | 需要适配复杂结构 |

---

## 💡 设计建议

### 1. 系统内置 vs 自定义案由

**建议方案**：
- 系统内置案由（官方案由）：只读，不允许删除
- 自定义案由：完全可管理

**实现方式**：
- 添加 `is_system` 字段（可选）
- 或通过 `created_by` 字段判断（系统初始化时 created_by 为 NULL）

### 2. 代码生成规则

**建议方案**：
- 一级案由：手动输入或自动生成（按分类递增）
- 二级案由：自动生成（父代码 + '.' + 序号）

**示例**：
```java
// 民事案由
一级：'1', '2', '3', ... '514'
二级：'8.1', '8.2', '22.1', '22.2', ...

// 刑事罪名
一级：'C101', 'C102', ... 'C717'
（刑事罪名通常只有一级）

// 行政案由
一级：'A1', 'A2', ... 'A22'
二级：'A1.1', 'A1.2', ... 'A18.11'
```

### 3. 批量导入功能

**支持格式**：
- Excel（.xlsx）
- CSV（.csv）

**导入模板**：
```
code | name | cause_type | category_code | category_name | parent_code | level | sort_order
```

### 4. 数据同步

**建议**：
- 定期同步官方更新的案由数据
- 保留自定义案由
- 合并冲突处理

---

## ✅ 总结

1. **不能直接用字典管理**：案由/罪名结构太复杂，字典管理无法满足需求

2. **推荐创建专门的案由管理功能**：
   - 基于 `sys_cause_of_action` 表
   - 参考字典管理的UI设计
   - 适配案由/罪名的复杂结构

3. **核心功能**：
   - ✅ 完整的CRUD操作
   - ✅ 树形结构展示
   - ✅ 层级关系管理
   - ✅ 分类管理
   - ✅ 批量导入
   - ✅ 权限控制

4. **实施优先级**：
   - P0：基础CRUD功能
   - P1：树形展示和层级管理
   - P2：批量导入和高级功能

---

## 📝 后续工作

- [ ] 设计详细的API接口文档
- [ ] 设计前端页面原型
- [ ] 实现后端CRUD功能
- [ ] 实现前端管理页面
- [ ] 添加数据验证和权限控制
- [ ] 编写使用文档
