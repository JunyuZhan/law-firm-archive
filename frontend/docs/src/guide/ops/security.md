# 安全运维指南

本文档介绍系统的安全架构、防护机制及运维安全建议。

## 系统安全架构

```
┌────────────────────────────────────────────────────────────────────┐
│                          安全防护层                                 │
├────────────────────────────────────────────────────────────────────┤
│  宝塔防火墙 / 云安全组                                              │
│  ├── 端口控制：仅开放 80/443/22                                     │
│  └── IP 黑名单 / 防爆破                                            │
├────────────────────────────────────────────────────────────────────┤
│  Nginx 反向代理                                                     │
│  ├── HTTPS/SSL 证书                                                │
│  ├── 安全响应头                                                     │
│  └── 请求大小限制                                                   │
├────────────────────────────────────────────────────────────────────┤
│  应用安全层                                                         │
│  ├── JWT 认证                    ├── XSS 过滤                       │
│  ├── 方法级权限控制              ├── SSRF 防护                      │
│  ├── 分布式限流                  ├── 文件上传验证                   │
│  └── 数据脱敏/加密               └── SQL 参数化                     │
├────────────────────────────────────────────────────────────────────┤
│  数据层                                                             │
│  ├── AES-256 敏感字段加密                                           │
│  ├── BCrypt 密码哈希                                               │
│  └── 审计日志                                                       │
└────────────────────────────────────────────────────────────────────┘
```

## 已实现的安全措施

### 1. 认证与授权

| 机制 | 说明 | 配置位置 |
| --- | --- | --- |
| JWT 认证 | HMAC-SHA256 签名，支持双令牌机制 | `JwtTokenProvider.java` |
| Token 黑名单 | Redis 存储已失效 Token，防止重放 | `TokenBlacklistService.java` |
| BCrypt 加密 | 密码安全存储 | `SecurityConfig.java` |
| 方法级权限 | `@PreAuthorize` 注解控制 | 各 Controller |

### 2. 登录安全防护

系统实现了多层登录安全机制：

```
用户登录
   ↓
┌─────────────────────────────────────────────────────┐
│ 第1层：滑块验证（后端校验）                           │
│   防止机器人/脚本自动登录                            │
├─────────────────────────────────────────────────────┤
│ 第2层：账户锁定机制                                  │
│   失败 5 次 → 锁定 15 分钟                          │
│   失败 10 次 → 锁定 1 小时                          │
│   失败 20 次 → 锁定 24 小时                         │
├─────────────────────────────────────────────────────┤
│ 第3层：图形验证码                                    │
│   连续失败 3 次后触发                               │
├─────────────────────────────────────────────────────┤
│ 第4层：异地登录检测                                  │
│   检测到新位置 → 需要管理员许可码                    │
├─────────────────────────────────────────────────────┤
│ 第5层：IP 速率限制                                   │
│   每 IP 每分钟最多 10 次登录尝试                     │
└─────────────────────────────────────────────────────┘
```

#### 异地登录检测配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `security.location.enabled` | `true` | 是否启用异地登录检测 |
| `security.location.level` | `province` | 判断级别：`province`=省级，`city`=市级 |
| `security.location.permit-code.mode` | `fixed` | 许可码模式：`fixed`=固定码，`random`=随机码 |
| `security.location.permit-code.value` | `888888` | 固定许可码（仅 fixed 模式有效） |

**判断级别说明：**

| 模式               | 从 → 到             | 是否异地        |
| ------------------ | ------------------- | --------------- |
| `province`（省级） | 北京 → 上海         | ⚠️ 异地         |
| `province`（省级） | 广州 → 深圳         | ✅ 正常（同省） |
| `city`（市级）     | 广州 → 深圳         | ⚠️ 异地         |
| `city`（市级）     | 北京朝阳 → 北京海淀 | ✅ 正常（同市） |

**许可码模式说明：**

- **固定码模式** (`fixed`)：管理员设置固定密码，可提前告知员工出差时使用
- **随机码模式** (`random`)：每次异地登录生成 6 位随机码，邮件通知管理员

::: tip 使用建议

- 一般律所建议使用 **省级 + 固定码**：简单易用，员工出差前获取许可码即可
- 高安全要求场景使用 **市级 + 随机码**：每次异地登录都需管理员实时授权 :::

**异地登录流程：**

```
员工从新位置登录
        ↓
系统检测到异地，弹出许可码输入框
        ↓
┌─────────────────────────────────┐
│ 固定码模式：                     │
│   员工输入预先获取的许可码        │
├─────────────────────────────────┤
│ 随机码模式：                     │
│   系统生成许可码并邮件通知管理员   │
│   员工联系管理员获取许可码        │
└─────────────────────────────────┘
        ↓
输入正确许可码 → 登录成功
（该位置自动加入常用位置列表）
```

::: warning IP 数据库异地登录检测依赖 ip2region 离线数据库。请确保 `backend/src/main/resources/ip2region/ip2region.xdb` 文件存在。

下载方式：

```bash
cd backend/src/main/resources/ip2region
wget -O ip2region.xdb "https://gitee.com/lionsoul/ip2region/raw/master/data/ip2region_v4.xdb"
```

:::

### 3. 接口防护

| 机制       | 说明                       | 配置位置                    |
| ---------- | -------------------------- | --------------------------- |
| 分布式限流 | 基于 Redis，IP/用户级限流  | `RateLimiterAspect.java`    |
| XSS 过滤   | 拦截危险脚本标签           | `XssFilter.java`            |
| SSRF 防护  | 拦截内网 IP 和云元数据地址 | `UrlSecurityValidator.java` |
| 输入验证   | Bean Validation 参数校验   | 各 Command 类               |

**限流配置示例：**

```java
// 登录接口：每 IP 每分钟最多 10 次
@RateLimiter(key = "login", rate = 10, interval = 60,
             limitType = RateLimiter.LimitType.IP)
```

### 3. 文件上传安全

系统实现了多层文件验证：

```java
// FileValidator.java 验证流程
1. 文件大小验证（最大 100MB）
2. 扩展名白名单验证（禁止 exe、php、jsp 等）
3. MIME 类型验证
4. 文件魔数（签名）验证，防止伪装
```

**允许的文件类型：**

- 文档：pdf, doc, docx, xls, xlsx, ppt, pptx, txt, rtf
- 图片：jpg, jpeg, png, gif, bmp, webp, svg
- 压缩：zip, rar, 7z

### 4. 数据保护

| 机制             | 说明                                 |
| ---------------- | ------------------------------------ |
| AES-256 加密     | 敏感字段（身份证、银行卡等）加密存储 |
| 数据脱敏         | 支持 13+ 种敏感信息自动脱敏          |
| HMAC-SHA256 签名 | 数据完整性验证                       |

**支持的脱敏类型：**

- 身份证号、手机号、固定电话
- 银行卡号、统一社会信用代码
- 邮箱、IP 地址
- 护照、车牌号、港澳台通行证
- 军官证、房产证号

### 5. 审计日志

通过 `@OperationLog` 注解记录关键操作：

```java
@OperationLog(module = "文档管理", action = "上传文件")
public Result<DocumentDTO> uploadFile(...) { }
```

## 安全评估

| 攻击类型   | 防护能力 | 说明                             |
| ---------- | -------- | -------------------------------- |
| SQL 注入   | ✅ 强    | MyBatis 参数化查询               |
| XSS        | ✅ 强    | XssFilter + Jackson 反序列化过滤 |
| CSRF       | ✅ 强    | 无状态 JWT，无需 CSRF Token      |
| 暴力破解   | ✅ 强    | IP 限流 + 验证码 + 账户锁定      |
| 凭证泄露   | ✅ 强    | 异地登录检测 + 管理员许可码      |
| Token 劫持 | ⚡ 中    | 黑名单机制，建议启用 HTTPS       |
| 数据泄露   | ✅ 强    | AES 加密 + 脱敏 + 权限控制       |
| 文件上传   | ✅ 强    | 多层验证（白名单+魔数）          |
| SSRF       | ✅ 强    | 内网 IP 拦截                     |
| 重放攻击   | ✅ 强    | Refresh Token 一次性使用         |

## 生产环境安全配置

### 1. 端口安全（宝塔/防火墙）

**只开放以下端口：**

| 端口      | 服务       | 是否对外开放    |
| --------- | ---------- | --------------- |
| 80        | HTTP       | ✅ 必须开放     |
| 443       | HTTPS      | ✅ 生产环境必须 |
| 22        | SSH        | ⚠️ 限制 IP 访问 |
| 8080      | 后端 API   | ❌ 仅内部访问   |
| 5432      | PostgreSQL | ❌ 仅内部访问   |
| 6379      | Redis      | ❌ 仅内部访问   |
| 9000/9001 | MinIO      | ❌ 仅内部访问   |

### 2. Nginx 安全响应头

在 nginx 配置中添加：

```nginx
server {
    # 防止 MIME 类型嗅探
    add_header X-Content-Type-Options "nosniff" always;

    # 防止点击劫持
    add_header X-Frame-Options "SAMEORIGIN" always;

    # XSS 防护（现代浏览器）
    add_header X-XSS-Protection "1; mode=block" always;

    # 控制 Referer 信息
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # HTTPS 强制（启用 HTTPS 后）
    # add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 内容安全策略（可选，根据实际需求调整）
    # add_header Content-Security-Policy "default-src 'self'" always;
}
```

### 3. 密钥安全

系统部署时会自动生成强随机密钥，保存在 `.env` 文件：

```bash
# 一键部署自动生成
JWT_SECRET=<64字符随机密钥>
DB_PASSWORD=<强随机密码>
MINIO_ACCESS_KEY=<随机访问密钥>
MINIO_SECRET_KEY=<强随机密码>
REDIS_PASSWORD=<强随机密码>
ONLYOFFICE_JWT_SECRET=<64字符随机密钥>
OCR_API_KEY=<64字符随机密钥>
```

::: warning 重要

- 妥善保管 `.env` 文件，不要提交到 Git
- 定期轮换密钥（建议每季度）
- 不同环境使用不同密钥 :::

### 4. 运行安全检查

部署前运行安全检查脚本：

```bash
# 完整安全检查
./scripts/security-check.sh

# 检查内容：
# - 环境变量配置
# - 默认密钥检测
# - SSL/TLS 配置
# - 敏感文件检查
# - 依赖漏洞提示
```

## 日常安全运维

### 1. 定期安全检查

**每日：**

- 检查系统日志中的异常登录
- 检查限流拦截记录

**每周：**

- 审查操作日志
- 检查备份完整性

**每月：**

- 运行依赖漏洞扫描
- 审计用户权限
- 检查 SSL 证书有效期

### 2. 依赖安全扫描

```bash
# 前端依赖检查
cd frontend && pnpm audit

# 后端依赖检查（需要添加 OWASP 插件）
cd backend && mvn org.owasp:dependency-check-maven:check
```

### 3. 日志安全审计

关注以下日志模式：

```bash
# 登录失败（可能是暴力破解）
grep "登录失败" /var/log/law-firm/app.log

# 权限拒绝（可能是越权尝试）
grep "权限不足" /var/log/law-firm/app.log

# 限流拦截
grep "Rate limit exceeded" /var/log/law-firm/app.log

# XSS 攻击检测
grep "XSS attack detected" /var/log/law-firm/app.log
```

### 4. 安全事件响应

**发现异常时：**

1. **封禁攻击 IP**

   ```bash
   # 宝塔面板：安全 → 防火墙 → 添加 IP 黑名单
   # 或使用 iptables
   iptables -A INPUT -s 攻击IP -j DROP
   ```

2. **强制用户重新登录**

   ```sql
   -- 清除所有会话
   DELETE FROM sys_user_session WHERE user_id = 受影响用户ID;
   ```

3. **检查数据完整性**
   ```sql
   -- 检查近期修改
   SELECT * FROM sys_operation_log
   WHERE created_at > NOW() - INTERVAL '24 hours'
   ORDER BY created_at DESC;
   ```

## Service Worker 安全策略

前端 PWA 实现了安全的缓存策略：

| API 类型                               | 缓存策略   | 说明             |
| -------------------------------------- | ---------- | ---------------- |
| 敏感 API (`/auth/`, `/admin/`, `/hr/`) | 不缓存     | 包含用户隐私数据 |
| 公共 API (`/dict/`, `/config/`)        | 持久化缓存 | 公共只读数据     |
| 业务 API (`/matter/`, `/document/`)    | 内存缓存   | 不持久化到磁盘   |

## 安全配置检查清单

部署前请确认：

- [ ] 所有默认密钥已更换
- [ ] HTTPS 已启用（生产环境必须）
- [ ] 防火墙仅开放必要端口
- [ ] Swagger UI 已关闭（生产环境）
- [ ] 数据库不暴露外部端口
- [ ] Redis 已设置密码
- [ ] MinIO 已修改默认密钥
- [ ] 日志级别设置为 info（非 debug）
- [ ] 自动备份已配置
- [ ] 监控告警已配置

## 相关资源

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security 最佳实践](https://spring.io/guides/topicals/spring-security-architecture)
- [JWT 安全规范 RFC 8725](https://datatracker.ietf.org/doc/html/rfc8725)
