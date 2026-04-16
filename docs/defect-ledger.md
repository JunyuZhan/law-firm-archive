# 缺陷台账

用于记录测试中发现的问题、影响、修复版本和复测结果，形成闭环。

## 字段说明

| 字段 | 说明 |
| --- | --- |
| 缺陷编号 | 唯一缺陷编号 |
| 发现日期 | 首次发现日期 |
| 发现版本 | 首次发现时的部署版本 |
| 标题 | 问题标题 |
| 现象 | 具体报错或异常现象 |
| 影响范围 | 影响的业务链路 |
| 严重级别 | 高 / 中 / 低 |
| 根因 | 根因说明 |
| 修复版本 | 已修复的版本 |
| 修复提交 | Git 提交号 |
| 复测结果 | 已通过 / 未通过 / 未复测 |
| 备注 | 补充说明 |

## 缺陷记录

| 缺陷编号 | 发现日期 | 发现版本 | 标题 | 现象 | 影响范围 | 严重级别 | 根因 | 修复版本 | 修复提交 | 复测结果 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DEF-20260330-01 | 2026-03-30 | v0.1.0 | 开放入库时推送记录表缺少审计字段 | 调用 `/api/open/archive/receive` 返回 `400`，后端日志显示 `arc_push_record.created_by does not exist` | 入库主链路 | 高 | 初始化库脚本中 `arc_push_record / arc_callback_record` 表结构未与实体审计字段对齐 | v0.1.1 | f9bd658d | 已通过 | 已同步修复部署数据库与初始化脚本 |
| DEF-20260330-02 | 2026-03-30 | v0.1.0 | 上传电子文件后档案详情未同步文件统计 | 上传成功后档案详情仍显示 `files=null`、`fileCount=0`、`hasElectronic=false` | 保存与借阅前置链路 | 高 | `FileStorageServiceImpl` 上传/删除文件后未回写 `arc_archive` 文件统计 | v0.1.1 | f9bd658d | 已通过 | 已补回写逻辑与回归测试 |
| DEF-20260416-01 | 2026-04-16 | main（未发布） | JWT 签名密钥存在公开默认值 | 部署漏配 `jwt.secret` 时，系统仍会使用代码内固定字符串签发和校验 Token，攻击者可伪造访问/刷新令牌 | 登录认证、权限控制、开放接口保护 | 高 | `JwtUtils` 使用 `@Value` 默认值回退，未对密钥缺失或弱密钥做启动失败保护 | 1.0.0 | 见 main 历史* | 已通过 | 已改为构造注入强制校验；未配置、示例值、长度不足都会启动失败；补充 `JwtUtilsTest` 覆盖弱配置场景 |
| DEF-20260416-02 | 2026-04-16 | main（未发布） | 敏感配置加密与 JWT 共用同一密钥来源 | SMB 密码等敏感配置的 AES 密钥从 `jwt.secret` 派生，且同样受默认值影响，一旦 JWT 密钥泄露或误配，配置密文也可被解开 | 备份目标、SMB 凭证、敏感配置保护 | 高 | `SecretCryptoService` 直接复用 `jwt.secret`，未拆分独立加密主密钥 | 1.0.0 | 见 main 历史* | 已通过 | 已拆分独立配置 `security.crypto.secret`；未配置或长度不足会启动失败；新增 `SecretCryptoServiceTest` 验证加解密与弱配置拒绝 |
| DEF-20260416-03 | 2026-04-16 | main（未发布） | 全库恢复前仅检查两张表是否为空 | 执行恢复时只校验 `archive` 与 `digital_file` 表为空，其他表若已有数据，`psql` 可能部分执行后失败，留下半恢复状态 | 备份恢复、数据库一致性、应急演练 | 高 | `BackupServiceImpl.restoreDatabase` 的空库判断不完整，与“全库恢复”语义不一致 | 1.0.0 | 见 main 历史* | 已通过 | 已改为在执行恢复前扫描整个 `public` schema 的非空表，并在启动 `psql` 前直接阻断；补充 `BackupServiceTest` 覆盖任意业务表有数据的场景 |
| DEF-20260416-04 | 2026-04-16 | main（未发布） | 文件恢复流程信任备份索引路径，存在目录穿越风险 | 恶意篡改 `files-index.json` 后，可通过 `../../` 逃逸 `filesDir` 读取宿主机文件，再上传进 MinIO | 备份恢复、对象存储、主机文件安全 | 中 | `restoreObjectIfPresent` 直接 `resolve` 备份内路径，未做 `normalize`、前缀校验和对象名合法性校验 | 1.0.0 | 见 main 历史* | 已通过 | 已为恢复路径、本地 checksum 校验和 SMB checksum 校验统一增加规范化与目录边界校验；补充 `BackupServiceTest` 恶意路径场景 |
| DEF-20260416-05 | 2026-04-16 | main（未发布） | Token 黑名单键使用 `String.hashCode()`，存在碰撞误伤 | 两个不同 Token 发生哈希碰撞时，会共用同一个黑名单键，导致登出或吊销误伤其他有效会话 | 认证登出、Token 吊销、会话隔离 | 中 | `TokenBlacklistService` 使用 32 位非抗碰撞哈希作为 Redis Key 标识 | 1.0.0 | 见 main 历史* | 已通过 | 已改为 `SHA-256(token)` 十六进制摘要；补充 `TokenBlacklistServiceTest`，验证 `Aa/BB` 等 `hashCode` 碰撞字符串不会再映射到同一黑名单键 |
| DEF-20260416-06 | 2026-04-16 | main（未发布） | 内网来源被一律视为可信代理，客户端可伪造来源 IP | 当应用直接暴露在内网、VPN 或办公网时，请求方只要来自 `10.x/172.16.x/192.168.x` 等地址，就可自行伪造 `X-Forwarded-For/X-Real-IP`，影响限流、审计日志和公开访问记录 | 登录限流、开放接口限流、操作审计、公开借阅访问追踪 | 高 | `ClientIpUtils.isTrustedProxy` 将所有私网地址都当作可信代理，未改为“显式受信代理列表”模型 | 1.0.0 | 见 main 历史* | 已通过 | 已改为显式 `security.trusted-proxies` 白名单模型，默认仅信任 `127.0.0.1/::1`；补充 `ClientIpUtilsTest` 与 `RateLimitFilterTest` |
| DEF-20260416-07 | 2026-04-16 | main（未发布） | 禁用账号后旧 JWT 仍可继续访问 | 管理员禁用用户后，已签发 JWT 仍可通过认证过滤器建立安全上下文，直到自然过期 | 账号禁用、权限控制、会话失效 | 高 | `JwtAuthenticationFilter` 加载 `UserDetails` 后未检查 `isEnabled` / `isAccountNonLocked` 状态 | 1.0.0 | 见 main 历史* | 已通过 | 已在 `JwtAuthenticationFilter` 中显式拒绝已禁用或锁定账号；补充 `JwtAuthenticationFilterTest` |
| DEF-20260416-08 | 2026-04-16 | main（未发布） | 改密/重置密码后现有 JWT 会话未被吊销 | 管理员重置密码或用户自行改密后，既有 access/refresh token 仍能继续使用直到自然过期 | 账号安全、会话失效、密码泄露处置 | 高 | `UserServiceImpl.resetPassword/changePassword` 仅更新密码哈希，未调用 `TokenBlacklistService` 吊销现有会话 | 1.0.0 | 见 main 历史* | 已通过 | 已在改密和重置密码后统一调用 `blacklistUserTokens`；补充 `UserServiceTest` 校验吊销逻辑 |
| DEF-20260416-09 | 2026-04-16 | main（未发布） | 独立加密主密钥切换后历史 SMB 密文无法兼容解密 | 将敏感配置加密切到 `security.crypto.secret` 后，存量 `arc_backup_target.smb_password_encrypted` 仍按旧 `jwt.secret` 生成，升级后验证 SMB 目标、列目录和执行备份都会抛出“敏感配置解密失败” | SMB 备份目标、历史配置迁移、发布兼容性 | 高 | 加密主密钥已拆分，但缺少历史密文的兼容读取或重加密迁移机制 | 1.0.0 | 见 main 历史* | 已通过 | 已增加 `security.crypto.legacy-secret` 兼容解密通道，默认可回退读取旧 `jwt.secret`；新写入统一使用新密钥；补充 `SecretCryptoServiceTest` 验证新旧密钥兼容 |
| DEF-20260416-10 | 2026-04-16 | main（未发布） | 数据库恢复前全表空库检查与恢复入口元数据依赖冲突 | 恢复入口需要先读取 `arc_backup_target / sys_config` 等系统表来定位备份集和维护模式，但全表空库检查会把这些运行期元数据也算作“非空”，导致已配置环境几乎无法执行数据库恢复 | 备份恢复、运维演练、上线可用性 | 高 | 空库校验未区分“业务恢复目标表”和“恢复执行依赖的系统保留表”，同时数据库导出仍未显式避开这些保留表的数据 | 1.0.0 | 见 main 历史* | 已通过 | 已将数据库备份导出切换为 `--data-only` 并排除系统保留表数据；恢复前仅检查业务表是否为空，系统保留表不再阻断入口；补充 `BackupServiceTest` 验证保留表场景 |
| DEF-20260416-11 | 2026-04-16 | main（未发布） | 系统配置查询接口未对敏感键值做脱敏 | 管理端读取 `/configs`、`/configs/{key}`、`/configs/group/*` 时，会直接返回敏感配置值；同时配置更新日志也会把敏感值原样写入应用日志 | 配置安全、运维审计、管理端最小暴露面 | 高 | `ConfigServiceImpl` 直接回传 `SysConfig.configValue`，且未按配置键识别 `secret/password/token` 等敏感项 | 1.0.0 | 见 main 历史* | 已通过 | 已按键名统一脱敏敏感配置返回值，并同步隐藏日志中的敏感值；补充 `ConfigServiceTest`、`ConfigControllerTest` |
| DEF-20260416-12 | 2026-04-16 | main（未发布） | 管理员禁用账号时未立即吊销既有会话 | 用户状态改为 `DISABLED` 时，仅更新 `sys_user.status`，未写入用户级 Token 吊销标记；账号后续若重新启用，历史 refresh token 仍可能重新换取新会话 | 会话失效、账号封禁、应急处置 | 高 | `UserServiceImpl.updateStatus` 未复用改密/重置密码已有的 `blacklistUserTokens` 会话吊销逻辑 | 1.0.0 | 见 main 历史* | 已通过 | 已在禁用账号时同步吊销该用户全部会话；补充 `UserServiceTest` 覆盖禁用与重新设为 ACTIVE 两种场景 |
| DEF-20260416-13 | 2026-04-16 | main（未发布） | 开放借阅预览/下载短链绕过访问次数限制与文件审计 | 直接调用 `/open/borrow/access/{token}/preview/{fileId}` 或 `/download-url/{fileId}` 即可获取短时文件地址，但不会增加 `accessCount`、不会写预览/下载审计，导致 `maxAccessCount` 只约束入口页而不约束真实文件访问 | 开放借阅、文件访问控制、审计追踪 | 高 | `BorrowLinkServiceImpl.getFileAccessUrl` 只校验链接有效性并签发 MinIO 短链，未在真实文件访问入口落访问计数、下载计数和审计日志 | 1.0.0 | 见 main 历史* | 已通过 | 已在短链签发时统一校验文件归属、消耗访问次数、记录预览/下载审计；兼容下载记录接口改为仅校验不再双计；补充 `BorrowLinkServiceTest`、`OpenApiControllerTest` |
| DEF-20260416-14 | 2026-04-16 | main（未发布） | 开放撤销接口仅校验 API Key，未校验来源归属 | `/open/borrow/revoke/{linkId}` 只要携带任意有效外部来源 API Key，就能按 `linkId` 撤销不属于自己的电子借阅链接 | 开放接口授权、跨租户隔离、借阅可用性 | 高 | `OpenApiController` / `BorrowLinkServiceImpl.revoke` 未使用 `ApiKeyAuthFilter` 注入的 `externalSource` 做来源归属校验，且外部申请生成链接时未持久化来源系统编码 | 1.0.0 | 见 main 历史* | 已通过 | 已强制开放申请/撤销接口读取 `externalSource` 上下文；创建外部链接时写入 `sourceSystem`；撤销时仅允许来源方撤销自己创建的链接；补充 `OpenApiControllerTest`、`BorrowLinkServiceTest` |
| DEF-20260416-15 | 2026-04-16 | main（未发布） | 公开借阅 token 失效响应可区分状态，且限流桶过粗 | 公开借阅访问会返回“已撤销/已过期/次数上限”等细分原因，配合统一 `open` 限流桶，可为 token 探测提供状态回显；同时开放写接口与公开访问共享粗粒度额度，不利于针对爆破面单独收紧 | token 枚举/爆破面、开放借阅访问、限流治理 | 中 | `validateAndAccess` / `requireActiveLink` 暴露了细粒度失效原因；`RateLimitFilter` 对 `/open/**` 仅使用单一限流配置，未将公开借阅访问与开放写接口拆桶 | 1.0.0 | 见 main 历史* | 已通过 | 已将公开借阅失效响应收敛为统一文案“访问链接无效或已过期”；新增 `open_borrow_access` 与 `open_write` 两级限流桶，分别收紧公开访问和开放写接口；补充 `RateLimitFilterTest`、`OpenApiControllerTest` |
| DEF-20260416-16 | 2026-04-16 | main（未发布） | 用户实体经 REST 序列化可暴露密码哈希 | `GET /users/current` 等返回 `User` 时，JSON 默认包含 `password` 字段（哈希），扩大离线破解与日志泄露面 | 账号安全、接口最小暴露 | 高 | 实体未对 `password` 做序列化排除，且 `toString` 可能带入敏感字段 | 1.0.0 | 90e0070a* | 已通过 | `User` 增加 `@JsonIgnore` 与 `@ToString(exclude="password")`；`UserTest` 增加 Jackson 序列化与 toString 断言 |
| DEF-20260416-17 | 2026-04-16 | main（未发布） | 鉴定/销毁按档案查询未复用档案可读性校验 | `GET /appraisals/{id}`、`/appraisals/archive/{archiveId}`、`GET /destructions/archive/{archiveId}` 仅要求已登录，可横向读取他人档案关联记录 | 档案隐私、横向越权 | 高 | Service 层未在返回前调用与 `ArchiveService.getById` 一致的数据范围校验 | 1.0.0 | 90e0070a* | 已通过 | `AppraisalServiceImpl` / `DestructionServiceImpl` 注入 `ArchiveService`，读接口前调用 `getById(archiveId)`；补充 `AppraisalServiceTest` |
| DEF-20260416-18 | 2026-04-16 | main（未发布） | 报表权限展示与导航不一致、单测与角色归一化漂移 | 非系统管理员仍可见「操作日志」导出页签（后端已拒绝）；侧栏缺少「报表导出」；`SecurityUtilsTest`/`AuthControllerTest` 仍断言历史别名 `ADMIN` | 权限体验、CI 可信度 | 中 | 前后端展示未与 `SYSTEM_ADMIN` 收紧对齐；测试未随 `UserRoleUtils.normalize` 更新 | 1.0.0 | 90e0070a* | 已通过 | `ReportPage` 操作日志页签仅管理员；`MainLayout` 增加报表菜单；修正单测期望与 refresh mock；`UserRoleUtils`/`permission.js` 等与产品角色一致（见同批次变更） |

**台账对齐说明（2026-04-16）**：**`见 main 历史*`** 表示 **DEF-20260416-01～15** 的修复已合入 `main` 但分散在多笔提交中，本表不逐条追溯哈希。**`90e0070a*`** 专指 **DEF-20260416-16～18**（用户序列化、鉴定/销毁读权限、报表 UI 与单测）及本次 **README/台账** 的合并提交 `90e0070a`。**正式发布或对外交付时**，请将「修复版本」改为实际镜像/标签版本，将「修复提交」改为发版说明或发版标签对应提交。

## 本轮代码评审修复方案

### DEF-20260416-01 JWT 签名密钥存在公开默认值

1. 移除 [JwtUtils.java](/Users/apple/Documents/Project/law-firm-archive/backend/src/main/java/com/archivesystem/security/JwtUtils.java) 中 `jwt.secret` 的默认值回退。
2. 改为通过 `@ConfigurationProperties` 或构造注入读取必填配置，在 Bean 初始化时校验：
   - 非空
   - 不等于示例值
   - 长度不少于 `32` 字节
3. 启动校验失败时直接抛出异常，阻止服务启动。
4. 补充单测：
   - 未配置密钥时启动失败
   - 弱密钥时启动失败
   - 合法密钥时可正常签发和校验 Token
5. 更新部署文档、`.env` 模板和发布清单，把 `jwt.secret` 标记为必填项。

### DEF-20260416-02 敏感配置加密与 JWT 共用同一密钥来源

1. 新增独立配置项，例如 `security.crypto.secret`，仅供 [SecretCryptoService.java](/Users/apple/Documents/Project/law-firm-archive/backend/src/main/java/com/archivesystem/security/SecretCryptoService.java) 使用。
2. 启动时校验该密钥已配置且满足最低强度要求，禁止回退到任何代码内默认值。
3. 为历史数据设计迁移流程：
   - 使用旧密钥解密现有密文
   - 使用新密钥重新加密后回写
   - 迁移完成后移除旧逻辑
4. 补充单测和回归用例：
   - 新旧密钥迁移成功
   - SMB 凭证保存、读取、校验链路不回归
5. 更新运维文档，明确认证密钥与配置加密密钥分离管理。

### DEF-20260416-03 全库恢复前仅检查两张表是否为空

1. 调整 [BackupServiceImpl.java](/Users/apple/Documents/Project/law-firm-archive/backend/src/main/java/com/archivesystem/service/impl/BackupServiceImpl.java) 的恢复前检查逻辑，不再只看 `archive` 和 `digital_file`。
2. 推荐方案二选一：
   - 方案 A：查询业务 schema 全量表，确认都为空后才允许恢复。
   - 方案 B：恢复到新建数据库或临时库，完成后再切换连接。
3. 恢复执行前输出预检查报告，明确：
   - 非空表列表
   - 当前数据库连接信息
   - 是否允许继续
4. 若预检查失败，必须在执行 `psql` 前阻断，不允许进入部分恢复状态。
5. 补充测试：
   - 任意非关键表有数据时恢复被拒绝
   - 全空库时恢复允许执行
   - 恢复失败时状态与报告正确落库

### DEF-20260416-04 文件恢复流程存在目录穿越风险

1. 在 `restoreObjectIfPresent` 中对 `filesDir.resolve(objectPath)` 的结果执行 `normalize()`。
2. 校验规范化后的路径必须以 `filesDir.normalize()` 为前缀，否则直接拒绝恢复并记录安全日志。
3. 在上传前补做对象路径合法性校验，避免非法对象名进入 MinIO。
4. 对 `files-index.json` 增加完整性校验或至少在 manifest/checksum 校验通过后才允许进入恢复阶段。
5. 补充安全测试：
   - `../../` 路径被拒绝
   - 绝对路径被拒绝
   - 合法相对路径仍可正常恢复

### DEF-20260416-05 Token 黑名单键存在哈希碰撞误伤

1. 将 [TokenBlacklistService.java](/Users/apple/Documents/Project/law-firm-archive/backend/src/main/java/com/archivesystem/security/TokenBlacklistService.java) 的键生成逻辑改为 `SHA-256(token)`。
2. 统一编码格式，建议使用十六进制小写字符串，保证不同节点生成结果一致。
3. 如线上已有旧键，可在一个过渡版本内同时读取新旧键，待旧 Token 自然过期后删除兼容逻辑。
4. 补充测试：
   - 黑名单写入/读取命中
   - 不同 Token 不会互相污染
   - 登出、刷新、强制下线链路回归通过

### DEF-20260416-06 内网来源被一律视为可信代理

1. 将 [ClientIpUtils.java](/Users/apple/Documents/Project/law-firm-archive/backend/src/main/java/com/archivesystem/common/util/ClientIpUtils.java) 的信任模型改为“显式配置的代理白名单”，不要把整个私网网段直接视为可信代理。
2. 新增配置项，例如 `security.trusted-proxies`，支持单 IP 或 CIDR。
3. 仅当 `remoteAddr` 命中受信代理列表时，才解析 `X-Forwarded-For/X-Real-IP`；否则始终使用 `remoteAddr`。
4. 对解析出的转发头做基础合法性校验，拒绝空值、非法 IP、超长头值和多余注入内容。
5. 补充测试：
   - 私网客户端直连时无法伪造 `X-Forwarded-For`
   - 受信代理转发时仍能正确提取真实客户端 IP
   - `RateLimitFilter`、公开访问记录、操作日志中的 IP 记录保持正确

### DEF-20260416-09 独立加密主密钥切换后历史 SMB 密文无法兼容解密

1. 在 [SecretCryptoService.java](/Users/apple/Documents/Project/law-firm-archive/backend/src/main/java/com/archivesystem/security/SecretCryptoService.java) 中保留“新密钥加密、旧密钥兼容解密”迁移窗口。
2. 新增可选配置 `security.crypto.legacy-secret`，默认可继承旧 `jwt.secret`，仅用于读取历史密文。
3. 所有新写入敏感配置统一使用 `security.crypto.secret`，避免继续扩大旧密钥覆盖面。
4. 补充单测：
   - 新密钥加密/解密成功
   - 旧密钥生成的密文在迁移期可被兼容解密
   - 弱密钥和空密钥仍会启动失败
5. 发布说明中要求在迁移窗口内逐步重保存 SMB 凭证，待历史密文全部完成重加密后再移除兼容配置。

### DEF-20260416-10 数据库恢复前全表空库检查与恢复入口元数据依赖冲突

1. 将 [BackupServiceImpl.java](/Users/apple/Documents/Project/law-firm-archive/backend/src/main/java/com/archivesystem/service/impl/BackupServiceImpl.java) 的恢复前校验范围调整为“仅检查业务恢复目标表”，排除恢复入口依赖的系统保留表。
2. 将数据库导出命令切换为 `pg_dump --data-only`，并排除系统保留表数据，避免恢复时与运行期元数据冲突。
3. 恢复执行前继续输出业务表非空列表，并在存在业务数据时阻断恢复，防止部分回灌。
4. 补充测试：
   - 任意业务表有数据时恢复被拒绝
   - 仅系统保留表有数据时恢复入口仍可继续执行
   - 导出兼容当前恢复策略，不再依赖“全库绝对空库”
5. 发布说明中明确：数据库恢复保留系统运行与运维元数据，重点回灌业务数据。

## 修复优先级建议

- 第一批立即处理：无
- 第二批紧随其后：无
- 已完成待发布：`DEF-20260416-01`～`DEF-20260416-18`（其中 01～15 为安全加固批次，16～18 为 2026-04-16 权限与台账对齐批次）
- 发布建议：进入发布前，建议统一做一轮安全与恢复专项回归，再对外承诺“备份恢复增强”和“安全加固”能力

## 当前状态

- 已关闭缺陷：`20`（含 DEF-20260416-16～18）
- 未关闭缺陷：`0`
- 当前仓库 **VERSION**：`1.0.0`（与 `backend/pom.xml` 等同步脚本一致；部署环境仍可能使用历史镜像号如 `v0.1.7`，以现场标签为准）
- 当前待修复最高风险等级：`无`
