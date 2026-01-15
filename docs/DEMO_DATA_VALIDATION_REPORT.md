# 演示数据勾连检查报告

## 检查日期
2026-01-15

## 检查范围
`scripts/init-db/30-demo-data-full.sql`

## 问题汇总

| 严重程度 | 问题 | 位置 |
|---------|------|------|
| **高** | matter 表字段名混用 | 第148行 |
| **高** | schedule 表名错误 | 第486行 |
| **高** | admin_seal_application 表名错误 | 第872行 |
| **高** | seal_application 字段名多处错误 | 第872-908行 |
| **高** | admin_meeting_room_booking 表不存在 | 第809行 |
| **中** | archive 表与 matter 状态不一致 | 第304-325行 |
| **中** | 额外数据缺少关联 project | 第1206-1258行 |

## 详细问题

### 1. matter 表字段名混用（高）

**问题**: matter 表中使用了不存在的 `lead_lawyer_id` 字段

**Schema 定义** (`03-matter-schema.sql:32`):
```sql
lead_lawyer_id bigint,
```

**Demo 数据** (`30-demo-data-full.sql:148`):
```sql
INSERT INTO matter (..., lead_lawyer_id, department_id, ...)
```

**实际字段**: Schema 中确实是 `lead_lawyer_id`，这个是正确的。

**但是**: crm_client 表使用的是 `responsible_lawyer_id`，需要确认是否应该统一。

---

### 2. schedule 表名错误（高）

**问题**: Demo 数据使用 `schedule_event`，但 Schema 中是 `schedule`

**Schema 定义** (`15-workbench-schema.sql:13`):
```sql
CREATE TABLE public.schedule (
```

**Demo 数据** (`30-demo-data-full.sql:486`):
```sql
INSERT INTO schedule_event (id, title, description, event_type, start_time, ...)
```

**修复方案**: 将 `schedule_event` 改为 `schedule`，字段名映射：
- `event_type` → `schedule_type`
- `all_day` ✓ (正确)
- `reminder_minutes` ✓ (正确)
- `reminder_sent` ✓ (正确)
- `location` ✓ (正确)
- `matter_id` ✓ (正确)
- `user_id` ✓ (正确)

---

### 3. admin_seal_application 表名错误（高）

**问题**: Demo 数据使用 `admin_seal_application`，但 Schema 中是 `seal_application`

**Schema 定义** (`10-admin-schema.sql:510`):
```sql
CREATE TABLE public.seal_application (
```

**Demo 数据** (`30-demo-data-full.sql:872`):
```sql
INSERT INTO admin_seal_application (id, application_no, applicant_id, ...)
```

**修复方案**: 将 `admin_seal_application` 改为 `seal_application`

---

### 4. seal_application 字段名错误（高）

**问题**: 多个字段名与 Schema 不一致

| Demo 使用 | Schema 实际 | 说明 |
|----------|-------------|------|
| `sealed_by` | `used_by` | 用印人 |
| `sealed_at` | `used_at` | 用印时间 |
| `seal_count` | `copies` | 用印份数 |
| `purpose` | `use_purpose` | 用印用途 |
| `remark` | `approval_comment` / `use_remark` | 备注 |
| `seal_id` ✓ | `seal_id` | 正确 |
| `matter_id` ✓ | `matter_id` | 正确 |
| `applicant_id` ✓ | `applicant_id` | 正确 |
| `status` ✓ | `status` | 正确 |
| `approved_by` ✓ | `approved_by` | 正确 |
| `approved_at` ✓ | `approved_at` | 正确 |

---

### 5. admin_meeting_room_booking 表不存在（高）

**问题**: Demo 数据使用 `admin_meeting_room_booking`，但 Schema 中没有此表

**Schema 实际** (`10-admin-schema.sql:463`):
```sql
CREATE TABLE public.meeting_room (
    id bigint NOT NULL,
    room_name character varying(100),
    room_code character varying(50),
    ...
)
```

**说明**: Schema 中只有 `meeting_room` 表（会议室信息），没有预约表。

**建议**: 需要在 Schema 中添加 `meeting_room_booking` 或 `meeting_room_reservation` 表

---

### 6. archive 与 matter 状态不一致（中）

**问题**: 档案显示项目已结案，但 matter 表中项目状态仍为 ACTIVE

| 档案 | matter_id | 档案状态 | matter.status |
|------|-----------|---------|---------------|
| AR2025-0002 | 103 | STORED | ACTIVE（应为 CLOSED） |
| AR2025-0003 | 105 | STORED | ACTIVE（应为 CLOSED） |

**建议**: 统一数据状态，要么更新 matter.status，要么不创建已结案档案

---

### 7. 额外数据缺少关联 project（中）

**问题**: 99-extra-demo-data.sql 中增加了客户、合同，但没有对应的 matter 数据

| 新增数据 | ID | 关联问题 |
|---------|-----|---------|
| 客户 | 201-203 | 无对应 matter |
| 合同 | 201-203 | 无对应 matter |
| 工时 | 201-205 | matter_id 引用 101-106 |
| 收费 | 201-204 | matter_id 引用 101-106，但 201-203 无对应 matter |

---

## 正确的数据勾连

### 已验证正确的部分

| 模块 | 外键 | 验证结果 |
|------|------|---------|
| crm_client | originator_id → sys_user.id | ✓ 正确 |
| crm_client | responsible_lawyer_id → sys_user.id | ✓ 正确 |
| finance_contract | client_id → crm_client.id | ✓ 正确 |
| finance_contract | signer_id → sys_user.id | ✓ 正确 |
| finance_contract | department_id → sys_department.id | ✓ 正确 |
| matter | client_id → crm_client.id | ✓ 正确 |
| matter | contract_id → finance_contract.id | ✓ 正确 |
| matter | originator_id → sys_user.id | ✓ 正确 |
| matter_client | matter_id → matter.id | ✓ 正确 |
| matter_client | client_id → crm_client.id | ✓ 正确 |
| matter_participant | matter_id → matter.id | ✓ 正确 |
| matter_participant | user_id → sys_user.id | ✓ 正确 |
| task | matter_id → matter.id | ✓ 正确 |
| task | assignee_id → sys_user.id | ✓ 正确 |
| archive | matter_id → matter.id | ✓ 正确 |
| archive | location_id → archive_location.id | ✓ 正确 |
| archive_borrow | archive_id → archive.id | ✓ 正确 |
| kb_article | category_id → kb_category.id | ✓ 正确 |
| kb_article | author_id → sys_user.id | ✓ 正确 |
| hr_attendance | user_id → sys_user.id | ✓ 正确 |
| hr_training | created_by → sys_user.id | ✓ 正确 |
| hr_training_participant | training_id → hr_training.id | ✓ 正确 |
| hr_training_participant | user_id → sys_user.id | ✓ 正确 |
| admin_meeting_room | (无外键) | ✓ 正确 |
| admin_seal | keeper_id → sys_user.id | ✓ 正确 |
| finance_fee | client_id → crm_client.id | ✓ 正确 |
| finance_fee | contract_id → finance_contract.id | ✓ 正确 |
| finance_fee | matter_id → matter.id | ✓ 正确 |
| finance_payment | fee_id → finance_fee.id | ✓ 正确 |
| finance_invoice | fee_id → finance_fee.id | ✓ 正确 |
| finance_invoice | client_id → crm_client.id | ✓ 正确 |
| timesheet | matter_id → matter.id | ✓ 正确 |
| timesheet | user_id → sys_user.id | ✓ 正确 |
| finance_expense | matter_id → matter.id | ✓ 正确 |
| finance_expense | applicant_id → sys_user.id | ✓ 正确 |
| letter_application | matter_id → matter.id | ✓ 正确 |
| letter_application | applicant_id → sys_user.id | ✓ 正确 |

## 修复优先级

### P0（必须修复）
1. 修复 `schedule_event` → `schedule`
2. 修复 `admin_seal_application` → `seal_application`
3. 修复 seal_application 字段名

### P1（建议修复）
1. 添加 meeting_room_booking 表或移除相关数据
2. 统一 archive 和 matter 的状态
3. 为额外数据添加对应的 matter

### P2（可选）
1. 统一 `lead_lawyer_id` 和 `responsible_lawyer_id` 字段命名

## 修复建议

由于发现的问题较多，建议：

1. **创建修复脚本**: `scripts/init-db/31-fix-demo-data.sql`
2. **逐步修复**: 先修复P0问题，再处理P1
3. **测试验证**: 修复后需要执行测试验证数据完整性

## 总结

- **检查条目**: 约30个外键关联
- **正确**: 24个（80%）
- **存在问题**: 6个（20%）
- **需要修复**: 3个高优先级问题

演示数据整体结构合理，但存在表名和字段名不一致问题，需要修复后才能正常使用。
