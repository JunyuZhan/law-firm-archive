# 律师事务所管理系统 - 数据库现状评估报告

**评估日期**: 2026-01-15
**数据库**: PostgreSQL
**评估范围**: 所有数据库初始化脚本 (`scripts/init-db/`)

---

## 1. 执行摘要

本报告对律师事务所管理系统的数据库设计进行了全面评估，涵盖表结构、索引设计、约束关系、性能优化等方面。

### 总体评估

| 评估项 | 评分 | 说明 |
|--------|------|------|
| 表结构设计 | B+ | 表结构完整，但存在一些冗余字段 |
| 索引设计 | B | 索引数量适中，但缺少部分复合索引 |
| 约束完整性 | B- | 主键约束完整，外键约束不完整 |
| 数据完整性 | B+ | 使用了乐观锁和软删除 |
| 性能优化 | C+ | 缺少分区表、物化视图等高级优化 |
| 安全性 | B | 敏感字段未加密，缺少行级安全策略 |

### 关键指标

| 指标 | 数值 | 说明 |
|------|------|------|
| 总表数 | 149 | 覆盖16个业务模块 |
| 总索引数 | 416 | 平均每表2.8个索引 |
| 外键约束 | 51 | 约束覆盖率较低 |
| 序列数 | ~149 | 每个表使用独立序列 |

---

## 2. 数据库架构概览

### 2.1 模块划分

系统包含16个业务模块，按功能划分为：

| 模块 | 表数量 | 主要表 |
|------|--------|--------|
| 系统管理 (01) | 23 | sys_user, sys_role, sys_menu, sys_config |
| 客户管理 (02) | 11 | crm_client, crm_contact, crm_lead |
| 项目管理 (03) | 6 | matter, matter_participant, matter_deadline |
| 财务管理 (04) | 24 | finance_contract, finance_payment, finance_commission |
| 文档管理 (05) | 8 | doc_file, doc_version, dossier_template |
| 证据管理 (06) | 4 | evidence, evidence_list |
| 档案管理 (07) | 5 | archive, archive_location |
| 工时管理 (08) | 3 | timesheet, timer_session |
| 任务管理 (09) | 2 | task, task_comment |
| 行政管理 (10) | 9 | admin_seal, admin_meeting_room |
| 资产盘点 (11) | 2 | asset_inventory, asset_inventory_item |
| 知识库 (12) | 6 | kb_article, kb_case |
| 人力资源 (13) | 28 | hr_employee, hr_attendance, hr_payroll |
| 质量管理 (14) | 7 | quality_check, quality_risk |
| 工作台 (15) | 5 | wb_approval, wb_schedule |
| 合同模板 (16) | 4 | contract_template, template_participant |

### 2.2 脚本组织

```
scripts/init-db/
├── 00-extensions.sql           # PostgreSQL 扩展
├── 01-16-*-schema.sql           # 各模块表结构定义
├── 20-21-*-init-data.sql        # 基础初始化数据
├── 22-29-*-patch.sql            # 补丁脚本
├── 30-44-*-demo.sql             # 演示数据
└── 50-cause-of-action-schema.sql # 案由管理
```

---

## 3. 表结构分析

### 3.1 优点

1. **命名规范统一**
   - 表名采用模块前缀: `sys_`, `crm_`, `finance_`, `matter_`
   - 字段命名采用蛇形命名法 (snake_case)
   - 主键统一使用 `id` 字段

2. **审计字段完整**
   ```sql
   created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
   updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
   created_by bigint
   updated_by bigint
   deleted boolean DEFAULT false
   ```
   - 所有核心业务表都包含审计字段
   - 实现了软删除机制

3. **乐观锁支持**
   - `version` 字段用于并发控制
   - 在 `25-add-version-column.sql` 中统一添加

4. **注释完整**
   - 所有表和重要字段都有 `COMMENT` 注释
   - 便于维护和理解

### 3.2 存在的问题

#### 问题 1: 字段类型选择不当

| 表 | 字段 | 当前类型 | 建议类型 | 原因 |
|----|------|----------|----------|------|
| sys_user | password | varchar(200) | - | 建议使用单独的认证表 |
| sys_user_session | token | varchar(2000) | text | JWT token 可能超长 |
| sys_operation_log | request_params | text | jsonb | 更适合 JSON 查询 |
| sys_config | config_value | varchar(2000) | text | 限制可能不够 |

#### 问题 2: 冗余字段

```sql
-- finance_commission 表存在大量冗余
CREATE TABLE finance_commission (
    ...
    payment_amount numeric(15,2),      -- 冗余，可从 payment_id 关联
    commission_base numeric(15,2),     -- 可计算得出
    tax_amount numeric(15,2),          -- 可计算得出
    management_fee numeric(15,2),      -- 可计算得出
    net_commission numeric(15,2),      -- 可计算得出
    originator_commission numeric(15,2), -- 冗余
    gross_amount numeric(15,2),         -- 冗余
    cost_amount numeric(15,2),          -- 冗余
    net_amount numeric(15,2),           -- 冗余
    ...
);
```

#### 问题 3: 缺少检查约束

```sql
-- 建议添加的约束
ALTER TABLE sys_user
    ADD CONSTRAINT chk_user_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$');

ALTER TABLE sys_user
    ADD CONSTRAINT chk_user_phone CHECK (phone ~* '^[0-9]{11}$');

ALTER TABLE finance_payment
    ADD CONSTRAINT chk_payment_amount CHECK (amount > 0);
```

---

## 4. 索引分析

### 4.1 索引统计

| 模块 | 表数 | 索引数 | 平均索引/表 |
|------|------|--------|-------------|
| 系统管理 | 23 | 52 | 2.26 |
| 客户管理 | 11 | 35 | 3.18 |
| 财务管理 | 24 | 73 | 3.04 |
| 其他模块 | 91 | 256 | 2.81 |

### 4.2 索引设计优点

1. **外键字段基本都有索引**
2. **常用查询字段已建索引** (status, created_at, user_id)
3. **唯一约束合理** (username, client_no, lead_no)

### 4.3 索引设计问题

#### 问题 1: 缺少复合索引

```sql
-- 当前只有单列索引
CREATE INDEX idx_crm_client_status ON crm_client USING btree (status);
CREATE INDEX idx_crm_client_type ON crm_client USING btree (client_type);

-- 建议添加复合索引
CREATE INDEX idx_client_status_type ON crm_client USING btree (status, client_type);
CREATE INDEX idx_client_responsible_status ON crm_client USING btree (responsible_lawyer_id, status);
```

#### 问题 2: 缺少部分索引

```sql
-- 对于有 deleted 字段的表，应使用部分索引
CREATE INDEX idx_client_active ON crm_client (id) WHERE deleted = false;
CREATE INDEX idx_user_active ON sys_user (id) WHERE deleted = false;
```

#### 问题 3: 缺少覆盖索引

```sql
-- 查询 sys_login_log 时经常需要多个字段
CREATE INDEX idx_login_log_user_status_time
    ON sys_login_log (user_id, status, login_time)
    INCLUDE (username, login_ip);
```

#### 问题 4: 全文索引缺失

```sql
-- 需要全文搜索的字段
CREATE INDEX idx_client_name_trgm ON crm_client USING gin (name gin_trgm_ops);
CREATE INDEX idx_matter_title_trgm ON matter USING gin (title gin_trgm_ops);
```

---

## 5. 约束分析

### 5.1 主键约束

- ✅ 所有表都有主键
- ✅ 主键统一使用 `id` 字段
- ✅ 使用序列自增

### 5.2 外键约束

**问题**: 外键约束覆盖率低

| 表 | 外键字段 | 是否有约束 |
|----|----------|------------|
| crm_client | originator_id | ❌ 无 |
| crm_client | responsible_lawyer_id | ❌ 无 |
| crm_lead | originator_id | ❌ 无 |
| matter | responsible_lawyer_id | ❌ 无 |
| finance_commission | originator_id | ❌ 无 |
| finance_commission | contract_id | ❌ 无 |
| finance_commission | matter_id | ❌ 无 |
| finance_commission | client_id | ❌ 无 |

**建议添加的外键**:

```sql
-- 客户表外键
ALTER TABLE crm_client
    ADD CONSTRAINT fk_client_originator FOREIGN KEY (originator_id) REFERENCES sys_user(id),
    ADD CONSTRAINT fk_client_lawyer FOREIGN KEY (responsible_lawyer_id) REFERENCES sys_user(id);

-- 案源表外键
ALTER TABLE crm_lead
    ADD CONSTRAINT fk_lead_originator FOREIGN KEY (originator_id) REFERENCES sys_user(id),
    ADD CONSTRAINT fk_lead_responsible FOREIGN KEY (responsible_user_id) REFERENCES sys_user(id);

-- 财务提成外键
ALTER TABLE finance_commission
    ADD CONSTRAINT fk_commission_payment FOREIGN KEY (payment_id) REFERENCES finance_payment(id),
    ADD CONSTRAINT fk_commission_contract FOREIGN KEY (contract_id) REFERENCES finance_contract(id),
    ADD CONSTRAINT fk_commission_matter FOREIGN KEY (matter_id) REFERENCES matter(id),
    ADD CONSTRAINT fk_commission_client FOREIGN KEY (client_id) REFERENCES crm_client(id),
    ADD CONSTRAINT fk_commission_originator FOREIGN KEY (originator_id) REFERENCES sys_user(id);
```

### 5.3 唯一约束

- ✅ 业务编号字段都有唯一约束 (client_no, lead_no, check_no 等)
- ✅ 用户名唯一 (sys_user.username)
- ✅ 角色代码唯一 (sys_role.role_code)

### 5.4 检查约束

- ❌ 缺少字段值范围检查
- ❌ 缺少枚举值检查
- ❌ 缺少邮箱/电话格式检查

---

## 6. 性能优化建议

### 6.1 分区表建议

对于日志类大表，建议使用分区：

```sql
-- 操作日志按月分区
CREATE TABLE sys_operation_log (
    -- 原有字段
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE sys_operation_log_2026_01 PARTITION OF sys_operation_log
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

建议分区的表：
- `sys_operation_log` - 按月分区
- `sys_login_log` - 按月分区
- `sys_notification` - 按月分区

### 6.2 物化视图建议

```sql
-- 客户统计汇总
CREATE MATERIALIZED VIEW mv_client_summary AS
SELECT
    c.status,
    c.client_type,
    COUNT(*) as count,
    COUNT(DISTINCT m.id) as matter_count,
    COALESCE(SUM(fc.amount), 0) as total_amount
FROM crm_client c
LEFT JOIN matter m ON m.client_id = c.id
LEFT JOIN finance_contract fc ON fc.client_id = c.id
WHERE c.deleted = false
GROUP BY c.status, c.client_type;

-- 刷新策略
CREATE REFRESH MATERIALIZED VIEW CONCURRENTLY mv_client_summary;
```

### 6.3 查询优化建议

1. **使用 EXPLAIN ANALYZE 分析慢查询**
2. **增加 work_mem 参数** (用于排序和哈希表)
3. **增加 shared_buffers** (建议为系统内存的 25%)
4. **启用并行查询** (max_parallel_workers_per_gather)

### 6.4 连接池配置

建议使用 PgBouncer 作为连接池：

```ini
[databases]
law_firm = host=localhost port=5432 dbname=law_firm

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 50
reserve_pool_size = 10
reserve_pool_timeout = 3
```

---

## 7. 安全性建议

### 7.1 敏感数据加密

```sql
-- 创建加密扩展
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 密码字段应使用哈希存储
ALTER TABLE sys_user
    ALTER COLUMN password TYPE text USING pgp_sym_encrypt(password, 'encryption_key');

-- 或应用层加密后存储
```

### 7.2 行级安全策略

```sql
-- 启用行级安全
ALTER TABLE sys_user ENABLE ROW LEVEL SECURITY;

-- 用户只能看到本部门的数据
CREATE POLICY user_department_policy ON sys_user
    FOR SELECT
    USING (department_id = (
        SELECT department_id FROM sys_user WHERE id = current_setting('app.user_id')::bigint
    ));
```

### 7.3 审计日志

建议使用 PostgreSQL 的审计扩展或触发器记录敏感操作：

```sql
CREATE OR REPLACE FUNCTION audit_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO audit_log (table_name, operation, old_data)
        VALUES (TG_TABLE_NAME, 'DELETE', row_to_json(OLD));
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO audit_log (table_name, operation, old_data, new_data)
        VALUES (TG_TABLE_NAME, 'UPDATE', row_to_json(OLD), row_to_json(NEW));
        RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO audit_log (table_name, operation, new_data)
        VALUES (TG_TABLE_NAME, 'INSERT', row_to_json(NEW));
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

---

## 8. 数据完整性建议

### 8.1 触发器自动更新 updated_at

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为所有需要的表添加触发器
CREATE TRIGGER update_sys_user_updated_at
    BEFORE UPDATE ON sys_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 8.2 软删除级联

```sql
-- 当用户被删除时，级联软删除关联数据
CREATE OR REPLACE FUNCTION soft_delete_user()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted = true AND OLD.deleted = false THEN
        UPDATE sys_user_role SET deleted = true WHERE user_id = NEW.id;
        UPDATE sys_user_session SET deleted = true WHERE user_id = NEW.id;
        -- 其他关联表...
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

## 9. 迁移和版本管理建议

### 9.1 使用 Flyway 或 Liquibase

当前使用脚本编号管理版本，建议使用专业的迁移工具：

```bash
# Flyway 目录结构
db/migration/
├── V1__init_extensions.sql
├── V2__create_system_tables.sql
├── V3__create_client_tables.sql
├── V3.1__add_client_indexes.sql
└── V4__init_system_data.sql
```

### 9.2 回滚脚本

为每个变更脚本准备对应的回滚脚本：

```sql
-- V4__add_version_column.sql
ALTER TABLE sys_user ADD COLUMN version integer DEFAULT 0;

-- U4__add_version_column.sql (回滚)
ALTER TABLE sys_user DROP COLUMN version;
```

---

## 10. 优化实施优先级

### P0 - 紧急 (立即实施)

| 项目 | 影响 | 工作量 |
|------|------|--------|
| 添加缺失的外键约束 | 数据一致性 | 2天 |
| 添加部分索引 | 查询性能 | 1天 |
| 配置连接池 | 稳定性 | 0.5天 |

### P1 - 高优先级 (1-2周内)

| 项目 | 影响 | 工作量 |
|------|------|--------|
| 添加复合索引 | 查询性能 | 2天 |
| 添加检查约束 | 数据完整性 | 1天 |
| 日志表分区 | 存储和性能 | 2天 |

### P2 - 中优先级 (1个月内)

| 项目 | 影响 | 工作量 |
|------|------|--------|
| 敏感数据加密 | 安全性 | 3天 |
| 添加物化视图 | 复杂查询性能 | 2天 |
| 行级安全策略 | 安全性 | 2天 |

### P3 - 低优先级 (后续优化)

| 项目 | 影响 | 工作量 |
|------|------|--------|
| 字段类型优化 | 维护性 | 3天 |
| 使用迁移工具 | 开发效率 | 3天 |
| 审计日志增强 | 安全性 | 2天 |

---

## 11. 结论

律师事务所管理系统的数据库设计整体良好，表结构清晰、注释完整、索引数量适中。但存在以下需要改进的地方：

1. **外键约束不完整** - 建议补全所有外键约束
2. **缺少复合索引** - 常见查询模式需要复合索引
3. **日志表未分区** - 长期运行后可能影响性能
4. **敏感数据未加密** - 建议对密码等敏感字段加密

建议按照优先级逐步实施优化措施，以提高系统的可靠性和性能。
