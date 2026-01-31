# PWA 安全与性能评估报告

> 智慧律所管理系统 PWA 集成后的安全风险与性能问题分析

**评估日期**: 2026-01-17  
**最后更新**: 2026-01-17  
**评估范围**: PWA 配置、Service Worker、缓存策略  
**状态**: ✅ 高风险问题已修复

---

## 一、当前配置说明

### Service Worker 实现方式

项目使用**手动实现的 Service Worker**（`/public/sw.js`），而非 VitePWA 插件生成。

```javascript
// frontend/apps/web-antd/src/main.ts:35
navigator.serviceWorker.register('/sw.js')
```

**原因**：避免 vite-plugin-pwa 与 Vite 7 的兼容性问题。

---

## 二、安全风险评估

### ✅ 已修复：敏感数据缓存泄露风险

**问题描述**（已修复）：

原配置对 `/api/*` 路径的所有请求进行缓存，会导致敏感数据被存储在用户设备上。

**修复方案**：

现在只缓存安全的只读 API，敏感业务数据不再缓存：

```javascript
// sw.js - 当前配置
const SAFE_API_PATTERNS = [
  '/api/dict/',           // 字典数据
  '/api/config/',         // 系统配置
  '/api/causes/',         // 案由/罪名数据
  '/api/public/',         // 公共接口
  '/api/menu/',           // 菜单数据
  '/api/department/',     // 部门数据（公共）
];

// API 请求处理
if (url.pathname.startsWith('/api/')) {
  const isSafeApi = SAFE_API_PATTERNS.some(pattern => 
    url.pathname.startsWith(pattern)
  );
  
  if (isSafeApi) {
    // 安全 API - 网络优先，支持离线缓存
    event.respondWith(networkFirst(request, API_CACHE_NAME));
  }
  // 敏感 API（案件、客户、财务等）- 不缓存
  return;
}
```

**不再缓存的敏感 API**：

| API 类型 | 数据示例 | 安全状态 |
|---------|---------|---------|
| `/api/matter/*` | 案件详情、当事人信息 | ✅ 不缓存 |
| `/api/crm/*` | 客户联系方式、财务记录 | ✅ 不缓存 |
| `/api/document/*` | 合同内容、内部文档 | ✅ 不缓存 |
| `/api/finance/*` | 收费记录、发票信息 | ✅ 不缓存 |
| `/api/hr/*` | 员工薪资、考勤记录 | ✅ 不缓存 |

---

### 🟡 中风险问题

#### 1. 缓存数据无加密

**问题描述**：

Cache API 存储的数据是**明文**，没有加密保护。

**验证方式**：
```javascript
// 在浏览器控制台执行，查看缓存内容
caches.open('law-firm-api-v3').then(cache => 
  cache.keys().then(keys => console.log('已缓存:', keys.map(r => r.url)))
);
```

**缓解措施**：
- ✅ 已排除敏感 API，只缓存公共数据
- ⚠️ 建议在公共设备使用后清除缓存

**严重程度**: 🟡 **中**（已通过排除敏感数据缓解）

---

#### 2. 潜在的缓存投毒攻击

**问题描述**：

如果中间人攻击成功，攻击者可能注入恶意缓存数据。

**当前缓解措施**：
- ✅ 强制 HTTPS（PWA 要求）
- ⚠️ 未配置 integrity 校验

**严重程度**: 🟡 **中**

---

### 🟢 低风险/已缓解

#### 3. Service Worker 作用域隔离

**当前状态**：✅ 正确

Service Worker 注册在根路径 `/`，作用域清晰。

---

#### 4. HTTPS 强制

**当前状态**：✅ 正确

PWA 标准要求 HTTPS，已缓解中间人攻击风险。

---

## 三、性能问题评估

### 🟡 中等性能问题

#### 1. 强制刷新可能导致数据丢失

**问题描述**：

```javascript
// sw.js
return self.skipWaiting();  // 立即激活
return self.clients.claim(); // 立即接管
```

**风险场景**：

用户正在填写表单，新版本 SW 到达后刷新可能导致未保存数据丢失。

**当前缓解**：
- ✅ 更新提示让用户选择刷新时机
- ⚠️ 建议添加表单自动保存功能

**严重程度**: 🟡 **中**

---

#### 2. 定期更新检查

**问题描述**：

```javascript
// main.ts
setInterval(() => {
  registration.update();
}, 60 * 60 * 1000);  // 每小时检查一次
```

**影响评估**：

| 用户规模 | 额外请求/天 | 服务器负载 |
|---------|------------|-----------|
| 100 用户 | 2,400 | 可忽略 |
| 1,000 用户 | 24,000 | 低 |

**严重程度**: 🟢 **低**

---

## 四、缓存策略总结

### 当前缓存配置

| 资源类型 | 缓存策略 | 缓存名称 | 说明 |
|---------|---------|---------|------|
| 静态资源 (js/css/图片) | CacheFirst | law-firm-static-v3 | 优先使用缓存 |
| 安全 API (字典/配置) | NetworkFirst | law-firm-api-v3 | 网络优先，离线可用 |
| 敏感 API (业务数据) | 不缓存 | - | 直接网络请求 |
| 页面导航 | NetworkFirst | law-firm-cache-v3 | 网络优先，离线显示 offline.html |

### 安全的可缓存 API

```javascript
const SAFE_API_PATTERNS = [
  '/api/dict/',           // 字典数据
  '/api/config/',         // 系统配置
  '/api/causes/',         // 案由/罪名数据
  '/api/public/',         // 公共接口
  '/api/menu/',           // 菜单数据
  '/api/department/',     // 部门数据
];
```

---

## 五、安全检查清单

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 敏感 API 不被缓存 | ✅ 已修复 | 只缓存公共只读数据 |
| 使用 HTTPS | ✅ 已配置 | PWA 强制要求 |
| Service Worker 作用域正确 | ✅ 已配置 | 限制在域名根路径 |
| 缓存数据有访问控制 | ⚠️ 不适用 | Cache API 不支持，已排除敏感数据 |
| 更新提示用户友好 | ✅ 已实现 | 用户可选择更新时机 |
| 离线时显示友好提示 | ✅ 已实现 | 显示 offline.html |

---

## 六、风险汇总

| 风险等级 | 数量 | 说明 |
|---------|------|------|
| 🔴 高 | 0 | ~~敏感数据缓存~~ → 已修复 |
| 🟡 中 | 2 | 缓存无加密（已缓解）、强制刷新 |
| 🟢 低 | 3 | 已缓解或影响小 |

---

## 七、后续优化建议

### 可选优化（低优先级）

#### 1. 添加缓存清理功能

在设置页面添加"清除缓存"按钮：

```typescript
async function clearPwaCache() {
  if ('serviceWorker' in navigator) {
    const registrations = await navigator.serviceWorker.getRegistrations();
    for (const registration of registrations) {
      await registration.unregister();
    }
  }
  const cacheNames = await caches.keys();
  await Promise.all(cacheNames.map(name => caches.delete(name)));
  window.location.reload();
}
```

#### 2. 表单自动保存

防止更新刷新时数据丢失：

```typescript
// 监听表单变化，自动保存到 localStorage
const formAutoSave = {
  save(key: string, data: any) {
    localStorage.setItem(`form-draft-${key}`, JSON.stringify(data));
  },
  restore(key: string) {
    const saved = localStorage.getItem(`form-draft-${key}`);
    return saved ? JSON.parse(saved) : null;
  },
  clear(key: string) {
    localStorage.removeItem(`form-draft-${key}`);
  }
};
```

---

## 八、测试验证

### 安全测试命令

```javascript
// 在浏览器控制台执行，检查缓存内容
caches.open('law-firm-api-v3').then(cache =>
  cache.keys().then(keys =>
    console.log('已缓存的 API:', keys.map(req => req.url))
  )
);

// 验证敏感数据未被缓存
caches.open('law-firm-api-v3').then(cache =>
  cache.match('/api/matter/list').then(resp =>
    resp ? console.log('⚠️ 敏感数据被缓存!') : console.log('✅ 敏感数据未缓存')
  )
);

// 验证安全数据可缓存
caches.open('law-firm-api-v3').then(cache =>
  cache.match('/api/causes/civil/tree').then(resp =>
    resp ? console.log('✅ 安全数据已缓存') : console.log('未缓存（需先访问）')
  )
);
```

### 性能测试

1. Chrome DevTools → **Lighthouse** → PWA 审计
2. Application → Service Workers → 检查状态
3. Application → Cache Storage → 验证缓存内容

---

**报告生成时间**: 2026-01-17  
**最后修复时间**: 2026-01-17  
**下次复核建议**: 重大功能更新后
