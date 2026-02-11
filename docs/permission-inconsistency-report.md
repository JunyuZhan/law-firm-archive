# 前后端权限码一致性检查报告

> 生成时间：2026-02-11
> 
> **状态：已修复**

## ✅ 已修复的不一致问题

### 1. 用户管理模块 - **已修复**

| 修复前 | 修复后 | 文件 |
|---------|---------|------|
| `user:create` | `sys:user:create` | user/index.vue |
| `user:edit` | `sys:user:update` | user/index.vue |
| `user:delete` | `sys:user:delete` | user/index.vue |
| `user:reset-password` | `sys:user:update` | user/index.vue |

---

### 2. 客户管理模块 - **已修复**

| 修复前 | 修复后 | 文件 |
|---------|---------|------|
| `client:edit` | `client:update` | client/index.vue |

---

### 3. 案件管理模块 - **已修复**

| 修复前 | 修复后 | 文件 |
|---------|---------|------|
| `matter:edit` | `matter:update` | matter/list/index.vue |

---

### 4. 系统配置模块 - **无需修复**

| 前端使用 | 后端定义 | 状态 |
|---------|---------|------|
| `sys:config:list` | `sys:config:list` | ✅ 一致 |
| `system:cause:create` | `system:cause:create` | ✅ 一致 |
| `system:cause:update` | `system:cause:update` | ✅ 一致 |
| `system:cause:delete` | `system:cause:delete` | ✅ 一致 |

---

## ✅ 已确认一致的权限码

| 模块 | 权限码 |
|------|--------|
| 财务收款 | `fee:payment`, `fee:amendment:list` |
| 提成规则 | `finance:commission:rule:*` |
| 文档管理 | `doc:seal:list`, `doc:template:list`, `doc:list` |
| 审批 | `approval:approve` |
| 会议室 | `admin:meeting:manage` |
| AI | `ai:usage:view:my`, `ai:billing:view` |
| 合同 | `contract:create`, `matter:contract:create` |

---

## 🔧 修复清单

### 高优先级（会导致按钮不显示）

- [ ] `user:create` → `sys:user:create`
- [ ] `user:edit` → `sys:user:update`
- [ ] `user:delete` → `sys:user:delete`
- [ ] `user:reset-password` → 后端添加权限定义或前端改为正确的码
- [ ] `client:edit` → `client:update`
- [ ] `matter:edit` → `matter:update`

### 统一命名建议

建议在项目中统一权限码命名规范：
1. **系统管理**：统一使用 `sys:` 前缀（如 `sys:user:*`, `sys:role:*`）
2. **操作动词**：统一使用 `create`/`update`/`delete`/`list`/`view`
3. **避免** `edit`，统一用 `update`

---

## 📋 完整检查方法

### 方法 1：启动服务后测试

1. 用 admin 账号登录
2. 打开浏览器开发者工具 → Network
3. 访问各页面，观察 `/api/auth/me` 返回的 permissions 列表
4. 对比前端代码中使用的权限码

### 方法 2：前端控制台检查

```javascript
// 在浏览器控制台执行
const accessStore = useAccessStore();
console.log('当前用户权限码:', accessStore.accessCodes);

// 检查某个权限码是否存在
console.log('是否有 user:create:', accessStore.accessCodes.includes('user:create'));
console.log('是否有 sys:user:create:', accessStore.accessCodes.includes('sys:user:create'));
```

### 方法 3：添加调试日志

在 `frontend/apps/web-antd/src/hooks/usePermission.ts` 中临时添加：

```typescript
const hasPermission = (code: string): boolean => {
  const result = accessStore.accessCodes.includes(code);
  if (!result) {
    console.warn(`[权限检查] 用户没有权限: ${code}`);
    console.log('[权限检查] 用户拥有的权限:', accessStore.accessCodes);
  }
  return result;
};
```

---

## 📊 权限码统计

| 类别 | 数量 |
|------|------|
| 前端使用的权限码 | ~30 |
| 后端 @RequirePermission 定义 | ~200+ |
| sys_menu 中定义 | ~150+ |
| **发现的不一致** | **6** |

---

## 结论

主要问题是**命名不一致**：
1. 前端用 `user:*`，后端用 `sys:user:*`
2. 前端用 `edit`，后端用 `update`

修复这 6 个不一致的权限码后，前端按钮权限控制应该能正常工作。
