# 演示数据修复报告

## 修复日期
2026-01-15

## 修复文件
`scripts/init-db/30-demo-data-full.sql`

## 修复内容

### 1. schedule 表修复 ✅

| 问题 | 修复前 | 修复后 |
|------|--------|--------|
| 表名 | `schedule_event` | `schedule` |
| 字段名 | `event_type` | `schedule_type` |
| 状态值 | `COURT_HEARING` | `COURT` |
| 状态值 | `CLIENT_VISIT` | `APPOINTMENT` |
| 状态值 | `TRAINING` | `OTHER` |
| 字段 | 缺少 `created_by` | 添加 `created_by` |
| 序列 | `schedule_event_id_seq` | `schedule_id_seq` |

### 2. seal_application 表修复 ✅

| 问题 | 修复前 | 修复后 |
|------|--------|--------|
| 表名 | `admin_seal_application` | `seal_application` |
| 字段名 | `sealed_by` | `used_by` |
| 字段名 | `sealed_at` | `used_at` |
| 字段名 | `seal_count` | `copies` |
| 字段名 | `purpose` | `use_purpose` |
| 字段名 | `remark` | `use_remark` |
| 字段 | 缺少 `created_by` | 添加 `created_by` |
| 状态值 | `COMPLETED` | `USED` |
| 序列 | `admin_seal_application_id_seq` | `seal_application_id_seq` |

### 3. meeting_room_booking 数据删除 ✅

- 删除了 `admin_meeting_room_booking` 表的 INSERT 语句
- 添加注释说明：会议室预约功能需要先创建 `meeting_room_booking` 表

### 4. matter 状态修复 ✅

| matter_id | 项目名称 | 状态修复前 | 状态修复后 | 说明 |
|-----------|---------|-----------|-----------|------|
| 103 | 广州制造业公司劳动争议案 | ACTIVE | CLOSED | 已归档，标注已调解结案 |
| 105 | 李建军劳动争议案 | ACTIVE | CLOSED | 已归档，标注胜诉结案 |

### 5. 统计信息更新 ✅

- 更新了基础示例数据的完成提示
- 更新了行政管理数据的完成提示

## 修复后验证

### 外键关联验证

| 模块 | 表名 | 外键 | 状态 |
|------|------|------|------|
| 工作台 | schedule | user_id → sys_user.id | ✅ 正确 |
| 工作台 | schedule | matter_id → matter.id | ✅ 正确 |
| 行政 | seal_application | applicant_id → sys_user.id | ✅ 正确 |
| 行政 | seal_application | seal_id → admin_seal.id | ✅ 正确 |
| 行政 | seal_application | matter_id → matter.id | ✅ 正确 |
| 行政 | seal_application | approved_by → sys_user.id | ✅ 正确 |
| 行政 | seal_application | used_by → sys_user.id | ✅ 正确 |

### 数据一致性验证

| 检查项 | 状态 |
|--------|------|
| matter 与 archive 状态一致性 | ✅ 已修复 |
| 档案入库的项目状态为 CLOSED | ✅ 已修复 |
| 项目成员与项目关联 | ✅ 正确 |
| 任务与项目关联 | ✅ 正确 |

## 未修复项（需要额外处理）

### 1. meeting_room_booking 表缺失

**影响**: 会议室预约功能无法使用

**建议**: 在 Schema 中添加 `meeting_room_booking` 表

**参考表结构**:
```sql
CREATE TABLE public.meeting_room_booking (
    id bigint NOT NULL,
    room_id bigint NOT NULL,
    user_id bigint NOT NULL,
    booking_date date NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    purpose character varying(500),
    attendees character varying(500),
    status character varying(20) DEFAULT 'PENDING',
    approved_by bigint,
    approved_at timestamp without time zone,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted boolean DEFAULT false
);
```

### 2. 额外数据缺少关联 matter

**影响**: 额外客户（201-203）和合同（201-203）没有对应的 matter 数据

**建议**: 为额外客户和合同创建对应的 matter 记录

## 总结

- **修复问题**: 7 个高优先级问题
- **修复状态**: ✅ 全部完成
- **脚本状态**: 可正常执行

修复后的演示数据脚本可以正常使用，外键关联正确，数据一致性良好。
