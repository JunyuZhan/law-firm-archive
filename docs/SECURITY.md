# 档案管理系统安全加固文档

## 概述

本文档描述了档案管理系统的安全加固措施，涵盖后端API安全、前端安全、数据保护等方面。

## 一、后端安全措施

### 1.1 认证与授权

#### JWT Token安全
- **Token黑名单机制**：登出时Token加入Redis黑名单，防止Token被盗用
- **用户级Token吊销**：支持强制登出用户所有会话
- **Token过期**：访问Token 24小时，刷新Token 7天

#### 登录安全
- **登录失败锁定**：5次失败后锁定账号30分钟
- **IP限制**：同一IP多次失败会被临时封禁
- **登录IP记录**：记录用户最后登录IP

相关代码：
- `LoginSecurityService.java` - 登录安全服务
- `TokenBlacklistService.java` - Token黑名单服务

### 1.2 速率限制

防止暴力攻击和DoS攻击：

| 接口类型 | 限制 |
|---------|------|
| 登录接口 | 10次/分钟 |
| 文件上传 | 20次/分钟 |
| Open API | 30次/分钟 |
| 通用接口 | 100次/分钟 |

相关代码：`RateLimitFilter.java`

### 1.3 API安全

#### Open API保护
- 需要API Key认证
- 配置项：`security.api-key.enabled`
- 请求头：`X-API-Key`

#### Actuator端点保护
- 只开放 `/actuator/health` 和 `/actuator/info`
- 敏感端点需要管理员权限
- 禁用危险端点（heapdump, threaddump等）

### 1.4 安全响应头

自动添加的安全响应头：
- `X-Frame-Options: SAMEORIGIN` - 防止点击劫持
- `X-XSS-Protection: 1; mode=block` - XSS防护
- `X-Content-Type-Options: nosniff` - 防止MIME嗅探
- `Content-Security-Policy` - 内容安全策略
- `Referrer-Policy: strict-origin-when-cross-origin` - 限制Referrer泄露
- `Permissions-Policy` - 限制浏览器功能

相关代码：`SecurityHeadersFilter.java`

### 1.5 输入验证

#### XSS防护
- 自动过滤危险HTML标签和JavaScript代码
- 转义特殊字符

相关代码：`XssFilter.java`

#### SQL注入防护
- 使用MyBatis-Plus参数化查询
- 额外的SQL注入模式检测
- 排序字段白名单验证

相关代码：`SqlInjectionValidator.java`

### 1.6 CORS配置

严格的跨域配置：
```yaml
security:
  cors:
    allowed-origins: http://localhost:3001
```

只允许配置的来源访问，不再使用通配符。

## 二、前端安全措施

### 2.1 安全存储

- 使用`secureStorage`替代直接localStorage操作
- 刷新Token存储在sessionStorage（关闭浏览器失效）
- 30分钟无操作自动登出

相关代码：`src/utils/security.js`

### 2.2 XSS防护

- `escapeHtml()` - HTML转义
- `sanitizeInput()` - 输入清理
- `containsXss()` - XSS检测

### 2.3 Content Security Policy

在`index.html`中配置CSP元标签：
- 限制脚本来源
- 限制样式来源
- 限制图片来源
- 禁止iframe嵌套

### 2.4 自动安全功能

- 空闲超时自动登出（30分钟）
- 跨标签页Token同步
- 安全的URL重定向

## 三、数据库安全

### 3.1 新增表结构

已集成到初始化脚本 `scripts/init-db/01-schema.sql`：

```sql
-- API Key管理表（Open API认证）
sys_api_key

-- 安全审计日志表（记录登录等安全事件）
sys_security_audit

-- IP黑名单表（封禁恶意IP）
sys_ip_blacklist
```

用户表新增字段：`last_login_ip` - 记录最后登录IP

### 3.2 安全审计

记录以下事件：
- 登录成功/失败
- 账号锁定/解锁
- 密码修改
- 可疑活动

## 四、配置清单

### 环境变量

```bash
# JWT密钥（必须修改！）
JWT_SECRET=your-secure-random-secret-key-here

# CORS允许的来源
CORS_ALLOWED_ORIGINS=https://your-domain.com

# API Key功能开关
API_KEY_ENABLED=true
```

### 生产环境检查清单

- [ ] 修改JWT_SECRET为强密钥
- [ ] 配置正确的CORS_ALLOWED_ORIGINS
- [ ] 启用HTTPS
- [ ] 禁用API文档（/doc.html）
- [ ] 配置防火墙规则
- [ ] 配置数据库访问控制
- [ ] 配置Redis密码
- [ ] 删除测试API Key
- [ ] 启用数据库连接加密

## 五、API Key管理

### 生成API Key

通过Redis命令或管理接口生成：

```redis
HSET api_key:your-api-key sourceName "律所管理系统" description "生产环境API Key" createdAt "2026-02-14T12:00:00"
```

### 使用API Key

在请求头中添加：
```
X-API-Key: your-api-key
```

## 六、监控与告警

建议配置以下监控：

1. **登录失败监控**：大量登录失败可能表示暴力攻击
2. **速率限制触发监控**：频繁触发可能表示攻击
3. **可疑活动监控**：检测异常行为
4. **Token黑名单大小监控**：过大可能表示问题

## 七、前端路由权限控制

### 角色定义

```javascript
SYSTEM_ADMIN    // 系统管理员 - 最高权限
SECURITY_ADMIN  // 安全保密员 - 用户管理权限
AUDIT_ADMIN     // 安全审计员 - 日志查看权限
ARCHIVIST       // 档案员 - 档案管理权限
USER            // 普通用户 - 基本查看权限
```

### 权限配置示例

```javascript
// 系统管理 - 仅管理员可访问
{
  path: 'system/users',
  meta: { roles: ['SYSTEM_ADMIN', 'SECURITY_ADMIN'] }
}
```

### 无权限页面

访问无权限页面时自动跳转到 `/forbidden`。

## 八、更新日志

### v2.0.0 (2026-02-14)
- 添加速率限制
- 添加登录失败锁定
- 添加JWT Token黑名单
- 添加安全响应头
- 添加XSS/SQL注入防护
- 加固CORS配置
- 添加API Key认证
- 前端安全加固
- 添加安全审计日志
- 登录页验证码
- 安全重定向验证
- 密码强度验证
- 前端路由权限控制
