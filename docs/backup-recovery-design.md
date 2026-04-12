# 备份与恢复中心设计方案

## 1. 目标

本方案面向律师事务所电子档案系统，目标不是仅做“导出备份文件”，而是构建一套可重建、可恢复、可审计的备份恢复能力。

业务目标：

- 当系统服务器损坏或需要迁移时，可以在新环境重新部署空系统。
- 由系统管理员在后台填写备份源配置后，选择备份集执行恢复。
- 恢复完成后，系统配置、数据库数据、电子档案文件可重新接管，系统继续使用。

一句话定义：

- 让档案系统具备“像新手机登录云备份账户后恢复数据”一样的重建能力。

## 2. 设计原则

- 电子档案优先：恢复能力必须覆盖电子文件，不仅是数据库。
- 备份集标准化：系统只识别结构化备份集，不执行用户自定义命令。
- 最小可用优先：第一阶段先支持本地目录和 SMB/NAS，不做任意远程命令执行。
- 审计留痕：备份、下载、恢复、失败都必须留日志。
- 恢复优先于高可用：第一阶段先确保“可恢复”，而不是先上复杂集群。

## 3. 适用场景

- 生产服务器硬件故障后在新主机重建系统
- 从旧服务器迁移到新服务器
- 测试环境从生产备份脱敏恢复
- 律所 IT 例行备份与抽样恢复演练

## 4. 第一期功能范围

### 4.1 备份中心

后台提供“备份中心”页面，支持：

- 配置备份目标
- 设置备份周期
- 设置保留策略
- 手动触发备份
- 查看备份执行记录
- 查看备份明细与校验结果

第一期只支持两类备份目标：

- 本地目录
- SMB/NAS 共享目录

### 4.2 恢复中心

后台提供“恢复中心”页面，支持：

- 录入备份源配置
- 测试备份源连通性
- 浏览可恢复备份集
- 选择备份集执行恢复
- 查看恢复日志与恢复结果

### 4.3 备份内容

第一期备份内容分三类：

- 数据库备份
- 电子档案文件备份
- 系统配置备份

不纳入第一期的内容：

- Redis 临时缓存
- RabbitMQ 临时队列状态
- Elasticsearch 索引数据

说明：

- Redis 可重建
- RabbitMQ 消息状态不作为恢复主依据
- Elasticsearch 应在恢复后通过重建索引任务恢复

## 5. 备份集标准

每次备份生成一个完整备份集，目录建议如下：

```text
backup-set/
  manifest.json
  checksums.txt
  database/
    archive-system.sql.gz
  files/
    archive-system-files.tar.zst
  config/
    sys-config.json
    backup-config.json
```

### 5.1 manifest.json 内容

建议字段：

- backupId
- backupType
- createdAt
- appVersion
- schemaVersion
- sourceHost
- databaseFile
- fileArchive
- configFiles
- fileCount
- totalBytes
- checksumAlgorithm
- status

### 5.2 checksums.txt

用于校验恢复前文件完整性，至少包含：

- 数据库备份文件哈希
- 电子文件归档包哈希
- 配置文件哈希

## 6. 恢复流程

恢复流程必须是标准化向导，不允许用户通过后台填 shell 命令。

标准恢复步骤：

1. 新系统部署完成，进入维护模式
2. 系统管理员配置备份源
3. 系统读取备份目录并列出可用备份集
4. 选择备份集并执行完整性校验
5. 校验通过后执行恢复
6. 恢复数据库
7. 恢复电子档案文件
8. 恢复系统配置
9. 触发重建索引
10. 输出恢复报告
11. 退出维护模式

## 7. 安全要求

恢复能力属于高危操作，必须限制：

- 仅 `SYSTEM_ADMIN` 可见和可执行
- 必须二次确认
- 恢复前必须要求系统进入维护模式
- 恢复期间禁止普通业务写入
- 全量记录审计日志
- 禁止后台输入任意命令
- SMB 凭证需加密存储

## 8. 技术实现建议

### 8.1 备份目标模型

建议支持的目标类型：

- `LOCAL`
- `SMB`

建议字段：

- id
- name
- targetType
- enabled
- localPath
- smbHost
- smbShare
- smbUsername
- smbPasswordEncrypted
- smbSubPath
- remarks
- lastVerifiedAt
- createdAt
- updatedAt

### 8.2 备份任务模型

建议记录：

- id
- backupId
- targetId
- triggerType
- backupScope
- status
- startedAt
- finishedAt
- fileCount
- totalBytes
- errorMessage
- operatorId

### 8.3 恢复任务模型

建议记录：

- id
- restoreId
- sourceType
- targetBackupId
- status
- verifyStatus
- startedAt
- finishedAt
- restoredDatabase
- restoredFiles
- restoredConfig
- rebuildIndexStatus
- errorMessage
- operatorId

## 9. 后端接口建议

第一阶段建议接口：

- `GET /api/backups/targets`
- `POST /api/backups/targets`
- `PUT /api/backups/targets/{id}`
- `POST /api/backups/targets/{id}/verify`
- `GET /api/backups/jobs`
- `POST /api/backups/run`
- `GET /api/backups/jobs/{id}`
- `GET /api/backups/sets`
- `GET /api/restores/sets`
- `POST /api/restores/verify`
- `POST /api/restores/run`
- `GET /api/restores/jobs`
- `GET /api/restores/jobs/{id}`

## 10. 前端页面建议

系统管理下新增两个页面：

- 备份中心
- 恢复中心

备份中心包含：

- 备份目标配置
- 备份计划配置
- 手动执行
- 执行台账

恢复中心包含：

- 恢复源配置
- 备份集浏览
- 恢复向导
- 恢复结果

## 11. 第一期最小实现

为了控制复杂度，第一阶段建议做到：

- 支持本地目录和 SMB 备份目标
- 支持数据库备份导出
- 支持电子文件目录归档
- 支持系统配置导出
- 支持手动执行备份
- 支持定时备份
- 支持查看备份记录
- 支持从备份集恢复数据库、文件、系统配置
- 支持恢复后重建 Elasticsearch 索引

第一阶段暂不做：

- 多云对象存储备份
- 备份集在线预览
- 差异块级备份
- 多租户恢复
- 自动跨机热备

## 12. 与高可用的关系

本方案主要解决“数据安全”和“系统可恢复”问题。

它不等于完整高可用集群，但它是高可用前必须完成的基础设施。

推荐路线：

1. 先完成备份、校验、恢复、审计
2. 再考虑 PostgreSQL 主从、MinIO 双副本、后端多实例等高可用能力

## 13. 验收标准

验收通过至少要满足：

- 可配置一个 SMB/NAS 备份目标
- 可手动生成完整备份集
- 可按计划自动生成完整备份集
- 可查看最近备份记录和结果
- 可在新环境连接备份源并识别备份集
- 可恢复数据库、电子文件、系统配置
- 恢复后可登录系统并查看已恢复档案
- 可触发索引重建并恢复检索能力
- 整个过程有完整审计日志

## 14. 当前结论

对律师事务所电子档案系统，第一阶段不必先做复杂高可用集群。

最合理的路线是：

- 把“备份中心 + 恢复中心 + 备份集标准 + 恢复向导”做成系统正式能力
- 先保证服务器坏了以后，能在新环境恢复并继续使用

这比单纯依赖 RAID 或手工拷目录更可靠，也更符合档案系统长期运行要求。
