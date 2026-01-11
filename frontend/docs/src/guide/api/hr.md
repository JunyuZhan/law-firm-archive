# 人力接口

人力资源管理接口。

## 员工管理

### 员工列表

```
GET /api/hr/employee/list
```

查询参数：

| 参数         | 类型   | 说明         |
| ------------ | ------ | ------------ |
| pageNum      | number | 页码         |
| pageSize     | number | 每页数量     |
| name         | string | 员工姓名     |
| employeeNo   | string | 员工编号     |
| departmentId | number | 部门ID       |
| position     | string | 职位         |
| status       | string | 员工状态     |

### 创建员工

```
POST /api/hr/employee
```

请求示例：

```json
{
  "name": "张三",
  "employeeNo": "EMP2026001",
  "gender": "MALE",
  "birthday": "1990-01-01",
  "idCard": "110101199001011234",
  "mobile": "13800138000",
  "email": "zhangsan@example.com",
  "departmentId": 1,
  "position": "律师",
  "entryDate": "2026-01-01",
  "employeeType": "FULL_TIME",
  "remark": "新入职员工"
}
```

### 员工详情

```
GET /api/hr/employee/{id}
```

### 更新员工

```
PUT /api/hr/employee/{id}
```

### 删除员工

```
DELETE /api/hr/employee/{id}
```

### 员工状态

| 值               | 说明       |
| ---------------- | ---------- |
| PROBATION        | 试用期     |
| REGULAR          | 正式       |
| RESIGNED         | 已离职     |
| RETIRED          | 已退休     |

### 员工类型

| 值               | 说明       |
| ---------------- | ---------- |
| FULL_TIME        | 全职       |
| PART_TIME        | 兼职       |
| INTERN           | 实习生     |

## 考勤管理

### 考勤记录列表

```
GET /api/hr/attendance/list
```

查询参数：

| 参数         | 类型   | 说明         |
| ------------ | ------ | ------------ |
| employeeId   | number | 员工ID       |
| startDate    | string | 开始日期     |
| endDate      | string | 结束日期     |
| attendanceType | string | 考勤类型   |

### 打卡记录

```
POST /api/hr/attendance/clock
```

请求示例：

```json
{
  "employeeId": 1,
  "clockType": "IN",
  "clockTime": "2026-01-12 09:00:00",
  "location": "公司前台",
  "remark": "正常上班"
}
```

### 考勤类型

| 值               | 说明       |
| ---------------- | ---------- |
| IN               | 上班打卡   |
| OUT              | 下班打卡   |
| LEAVE_EARLY      | 早退       |
| LATE             | 迟到       |
| ABSENT           | 缺勤       |

## 请假管理

### 请假申请

```
POST /api/hr/leave/apply
```

请求示例：

```json
{
  "employeeId": 1,
  "leaveType": "ANNUAL",
  "startDate": "2026-01-15",
  "endDate": "2026-01-17",
  "days": 3,
  "reason": "个人事务",
  "contactPhone": "13800138000"
}
```

### 请假审批

```
POST /api/hr/leave/{id}/approve
```

请求示例：

```json
{
  "approved": true,
  "remark": "同意请假"
}
```

### 请假记录列表

```
GET /api/hr/leave/list
```

### 请假类型

| 值               | 说明       |
| ---------------- | ---------- |
| ANNUAL           | 年假       |
| SICK             | 病假       |
| PERSONAL         | 事假       |
| MARRIAGE         | 婚假       |
| MATERNITY        | 产假       |
| PATERNITY        | 陪产假     |
| BEREAVEMENT      | 丧假       |

## 薪资管理

### 工资条列表

```
GET /api/hr/payroll/list
```

查询参数：

| 参数         | 类型   | 说明         |
| ------------ | ------ | ---------- |
| employeeId   | number | 员工ID     |
| month        | string | 月份       |
| status       | string | 状态       |

### 生成工资表

```
POST /api/hr/payroll/generate
```

请求示例：

```json
{
  "month": "2026-01",
  "employeeIds": [1, 2, 3]
}
```

### 工资条详情

```
GET /api/hr/payroll/{id}
```

### 确认工资表

```
POST /api/hr/payroll/{id}/confirm
```

### 工资项

| 项           | 说明         |
| ------------ | ------------ |
| baseSalary   | 基本工资     |
| performance  | 绩效奖金     |
| allowance    | 津贴补贴     |
| overtimePay  | 加班费       |
| commission   | 提成         |
| tax          | 个人所得税   |
| insurance    | 社保公积金   |
| netSalary    | 实发工资     |

## 转正管理

### 转正申请

```
POST /api/hr/regularization/apply
```

请求示例：

```json
{
  "employeeId": 1,
  "applyDate": "2026-04-01",
  "probationSummary": "试用期表现良好",
  "selfEvaluation": "已经熟悉工作流程",
  "expectedSalary": 8000.00
}
```

### 转正审批

```
POST /api/hr/regularization/{id}/approve
```

## 离职管理

### 离职申请

```
POST /api/hr/resignation/apply
```

请求示例：

```json
{
  "employeeId": 1,
  "resignDate": "2026-06-30",
  "reason": "个人发展",
  "handoverPlan": "已完成工作交接",
  "remark": "感谢公司的培养"
}
```

### 离职审批

```
POST /api/hr/resignation/{id}/approve
```

---

## 文档更新记录

| 更新时间 | 更新内容 | 操作人 |
|----------|----------|--------|
| 2026-01-11 | 创建人力接口文档骨架 | AI Assistant |
