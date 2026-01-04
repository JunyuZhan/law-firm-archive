# Implementation Plan: 合同模块完善

## Overview

本实施计划将合同模块完善分为数据库扩展、后端实体和服务开发、前端页面更新三个主要阶段。采用渐进式开发，确保每个阶段完成后系统仍可正常运行。

## 当前状态分析

已有实现：
- `Contract.java` 实体存在，包含基础字段
- `ContractRepository` 和 `ContractMapper` 已存在
- `ContractAppService` 已实现基础 CRUD 和审批流程
- `ContractController` 已实现基础接口
- 前端合同管理页面已实现基础功能

待实现：
- Contract 实体扩展字段（trial_stage, claim_amount 等）
- ContractPaymentSchedule 实体和相关类
- ContractParticipant 实体和相关类
- 枚举类定义
- 付款计划和参与人管理 API
- 前端付款计划和参与人管理功能

## Tasks

- [x] 1. 数据库表结构扩展
  - [x] 1.1 扩展 finance_contract 表字段
    - 在 `scripts/init-db/01-schema.sql` 中添加新字段
    - 字段：trial_stage, claim_amount, jurisdiction_court, opposing_party, conflict_check_status, archive_status, advance_travel_fee, risk_ratio, seal_record
    - _Requirements: 1.1-1.9_
  - [x] 1.2 创建 contract_payment_schedule 表
    - 在 `scripts/init-db/01-schema.sql` 中添加新表
    - 字段：contract_id, phase_name, amount, percentage, planned_date, actual_date, status, remark
    - 添加索引和外键约束
    - _Requirements: 2.1-2.7_
  - [x] 1.3 创建 contract_participant 表
    - 在 `scripts/init-db/01-schema.sql` 中添加新表
    - 字段：contract_id, user_id, role, commission_rate, remark
    - 添加唯一约束 (contract_id, user_id)
    - _Requirements: 3.1-3.3_

- [x] 2. 后端实体层开发
  - [x] 2.1 扩展 Contract 实体
    - 在 `Contract.java` 中添加新字段：trialStage, claimAmount, jurisdictionCourt, opposingParty, conflictCheckStatus, archiveStatus, advanceTravelFee, riskRatio, sealRecord
    - 添加字段注释
    - _Requirements: 1.1-1.9_
  - [x] 2.2 创建枚举类
    - 创建 `TrialStage` 枚举（FIRST_INSTANCE, SECOND_INSTANCE, RETRIAL, EXECUTION, NON_LITIGATION）
    - 创建 `ConflictCheckStatus` 枚举（PENDING, PASSED, FAILED, NOT_REQUIRED）
    - 创建 `ArchiveStatus` 枚举（NOT_ARCHIVED, ARCHIVED, DESTROYED）
    - 创建 `PaymentScheduleStatus` 枚举（PENDING, PARTIAL, PAID, CANCELLED）
    - 创建 `ContractParticipantRole` 枚举（LEAD, CO_COUNSEL, ORIGINATOR, PARALEGAL）
    - _Requirements: 1.5, 1.6, 2.7, 3.2, 4.1_
  - [x] 2.3 创建 ContractPaymentSchedule 实体
    - 新建 `ContractPaymentSchedule.java` 实体类
    - 创建 `ContractPaymentScheduleRepository.java`
    - 创建 `ContractPaymentScheduleMapper.java`
    - _Requirements: 2.1-2.7_
  - [x] 2.4 创建 ContractParticipant 实体
    - 新建 `ContractParticipant.java` 实体类
    - 创建 `ContractParticipantRepository.java`
    - 创建 `ContractParticipantMapper.java`
    - _Requirements: 3.1-3.3_

- [ ] 3. 后端应用服务层开发
  - [x] 3.1 创建 DTO 和 Command 类
    - 创建 `ContractPaymentScheduleDTO.java`
    - 创建 `ContractParticipantDTO.java`
    - 创建 `CreatePaymentScheduleCommand.java`
    - 创建 `UpdatePaymentScheduleCommand.java`
    - 创建 `CreateParticipantCommand.java`
    - 创建 `UpdateParticipantCommand.java`
    - 更新 `ContractDTO.java` 添加新字段
    - 更新 `CreateContractCommand.java` 添加新字段
    - 更新 `UpdateContractCommand.java` 添加新字段
    - _Requirements: 1.1-1.9, 2.1, 3.1_
  - [x] 3.2 扩展 ContractAppService
    - 更新 createContract/updateContract 方法支持新字段
    - 添加付款计划 CRUD 方法
    - 添加参与人 CRUD 方法
    - 添加提成比例验证逻辑（总和不超过100%）
    - 添加付款计划金额验证逻辑（总和等于合同金额）
    - 添加承办律师必填验证
    - _Requirements: 2.8, 2.9, 3.4, 3.5_
  - [x] 3.3 实现合同-项目关联逻辑 ✅
    - 修改 MatterAppService 创建项目时复制合同参与人到项目参与人
    - 实现 copyContractParticipantsToMatter() 方法
    - 实现 mapContractRoleToMatterRole() 角色映射方法
    - 验证合同必须为 ACTIVE 状态（已在 createMatter 中实现）
    - 验证一对一关系（已在 createMatter 中实现）
    - _Requirements: 5.1-5.4_
  - [ ]* 3.4 编写属性测试 - 数据持久化
    - **Property 1: Contract Data Round-Trip**
    - **Property 2: Payment Schedule Data Round-Trip**
    - **Validates: Requirements 1.1-1.9, 2.2-2.7**
  - [ ]* 3.5 编写属性测试 - 业务规则
    - **Property 8: Lead Participant Required**
    - **Property 9: Commission Rate Total Validation**
    - **Property 14: Matter Requires Active Contract**
    - **Property 15: One-to-One Contract-Matter Relationship**
    - **Validates: Requirements 3.4, 3.5, 5.1, 5.2, 5.4**

- [x] 4. Checkpoint - 后端核心功能验证 ✅
  - 编译通过，无错误
  - 修复了 ContractTemplate 相关类的包路径问题
  - 数据库表结构已在 01-schema.sql 中定义

- [x] 5. 后端接口层开发 ✅
  - [x] 5.1 扩展 ContractController（matter 模块）
    - 创建 `/matter/contract` 路径下的完整 CRUD 接口
    - 添加付款计划 CRUD 接口：GET/POST/PUT/DELETE `/matter/contract/{id}/payment-schedules`
    - 添加参与人 CRUD 接口：GET/POST/PUT/DELETE `/matter/contract/{id}/participants`
    - 添加合同统计接口：GET `/matter/contract/statistics`
    - _Requirements: 2.1, 3.1, 6.4_
  - [x] 5.2 扩展查询过滤功能
    - 更新 ContractQueryDTO 添加新筛选字段
    - 支持 fee_type, signer_id, department_id 筛选
    - 支持 effective_date, expiry_date 日期范围查询
    - 支持 total_amount, claim_amount 金额范围查询
    - 支持 trial_stage, conflict_check_status, archive_status 筛选
    - _Requirements: 6.1, 6.2, 6.3_
  - [ ]* 5.3 编写属性测试 - 查询功能
    - **Property 16: Contract Query Filtering**
    - **Property 17: Contract Statistics Accuracy**
    - **Validates: Requirements 6.1-6.4**

- [x] 6. 合同模板功能完善 ✅
  - [x] 6.1 实现模板字段填充
    - 修改 ContractAppService 添加 createFromTemplate 方法
    - 选择模板时自动填充 contract_type, fee_type, content 字段
    - _Requirements: 7.2_
  - [x] 6.2 实现变量替换功能
    - 在 ContractAppService 中实现 processTemplateVariables 方法
    - 支持 ${clientName}, ${totalAmount}, ${signDate}, ${lawyerName}, ${contractNo}, ${effectiveDate}, ${expiryDate}, ${claimAmount}, ${jurisdictionCourt}, ${opposingParty} 等变量
    - 添加 previewTemplateContent 方法用于前端预览
    - _Requirements: 7.3_
  - [ ]* 6.3 编写属性测试 - 模板功能
    - **Property 18: Template Field Population**
    - **Property 19: Template Variable Substitution**
    - **Validates: Requirements 7.2, 7.3**

- [x] 7. Checkpoint - 后端完整功能验证 ✅
  - 编译通过，无错误
  - API 接口已实现：
    - `/matter/contract/*` - 合同 CRUD
    - `/matter/contract/{id}/payment-schedules/*` - 付款计划 CRUD
    - `/matter/contract/{id}/participants/*` - 参与人 CRUD
    - `/matter/contract/statistics` - 统计接口
    - `/matter/contract/from-template/{templateId}` - 基于模板创建
    - `/matter/contract/template/{templateId}/preview` - 模板预览

- [x] 8. 前端页面更新 ✅
  - [x] 8.0 更新前端类型定义和 API
    - 更新 `ContractDTO` 添加扩展字段（trialStage, claimAmount, jurisdictionCourt 等）
    - 添加 `ContractPaymentScheduleDTO` 和 `ContractParticipantDTO` 类型
    - 更新 `ContractQuery` 添加新筛选字段
    - 添加付款计划 CRUD API 函数
    - 添加参与人 CRUD API 函数
    - 添加统计和模板相关 API 函数
  - [x] 8.1 更新合同表单
    - 添加审理阶段选择（TrialStage 下拉框）
    - 添加标的金额输入
    - 添加管辖法院输入
    - 添加对方当事人输入
    - 添加利冲审查状态选择
    - 添加预支差旅费输入
    - 添加风险代理比例输入（0-100%，仅风险代理时显示）
    - _Requirements: 1.1-1.9_
  - [x] 8.2 添加付款计划管理
    - 在合同详情页添加付款计划 Tab
    - 实现付款计划列表展示
    - 实现添加/编辑/删除付款计划功能
    - 显示付款计划总额与合同金额对比
    - _Requirements: 2.1-2.7_
  - [x] 8.3 添加参与人管理
    - 在合同详情页添加参与人 Tab
    - 实现参与人列表展示
    - 实现添加/编辑/删除参与人功能
    - 显示提成比例总和验证
    - _Requirements: 3.1-3.5_
  - [x] 8.4 更新合同列表页
    - 添加收费方式筛选
    - 添加签约人筛选
    - 添加统计信息卡片展示（合同总数、生效中、总金额、待收金额）
    - _Requirements: 6.1-6.4_

- [x] 9. Final Checkpoint - 完整功能验证 ✅
  - 后端编译通过
  - 数据库已重置
  - 前端页面已更新
  - 路由映射冲突已解决（从 MatterController 移除了重复的合同端点）
  - API 测试通过：`/matter/contract/statistics` 返回正确数据

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 数据库修改需要重置数据库才能生效：`./scripts/reset-db.sh`
- 后端使用 Java + Spring Boot + MyBatis-Plus
- 前端使用 Vue 3 + Ant Design Vue
- 属性测试使用 jqwik 库
- 现有 ContractAppService 已实现基础 CRUD 和审批流程，需在此基础上扩展

