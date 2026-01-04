# Requirements Document

## Introduction

本文档定义了智慧律所管理系统中合同管理模块的完善需求。当前合同管理功能过于简单，仅作为"登记"功能，缺乏完整的合同生命周期管理。本次重构旨在将合同模块完善为一个功能完整的业务模块，支持合同的全流程管理。

## Glossary

- **Contract**: 委托合同，律师事务所与客户签订的法律服务协议
- **Contract_Template**: 合同模板，系统预设的标准合同格式
- **Payment_Schedule**: 付款计划，合同约定的分期付款安排
- **Contract_Participant**: 合同参与人，参与合同执行的律师团队成员
- **Trial_Stage**: 审理阶段，诉讼案件的进展阶段（一审、二审、再审、执行等）
- **Conflict_Check**: 利冲审查，检查是否存在利益冲突
- **Risk_Agency**: 风险代理，根据案件结果按比例收费的收费模式
- **Originator**: 案源人，带来客户/案件的律师
- **Lead_Lawyer**: 承办律师，主要负责案件的律师
- **Co_Counsel**: 协办律师，协助承办律师的律师

## Requirements

### Requirement 1: 合同基础信息扩展

**User Story:** As a 律师, I want to 记录完整的合同信息, so that I can 全面管理委托事项和相关方信息。

#### Acceptance Criteria

1. THE Contract SHALL include trial_stage field to record the litigation stage (FIRST_INSTANCE, SECOND_INSTANCE, RETRIAL, EXECUTION, NON_LITIGATION)
2. THE Contract SHALL include claim_amount field to record the disputed amount in decimal format
3. THE Contract SHALL include jurisdiction_court field to record the court with jurisdiction
4. THE Contract SHALL include opposing_party field to record the opposing party name
5. THE Contract SHALL include conflict_check_status field with values (PENDING, PASSED, FAILED, NOT_REQUIRED)
6. THE Contract SHALL include archive_status field with values (NOT_ARCHIVED, ARCHIVED, DESTROYED)
7. THE Contract SHALL include advance_travel_fee field to record prepaid travel expenses
8. THE Contract SHALL include risk_ratio field to record the contingency fee percentage (0-100)
9. THE Contract SHALL include seal_record field to store seal usage history in JSON format

### Requirement 2: 付款计划管理

**User Story:** As a 财务人员, I want to 管理合同的分期付款计划, so that I can 跟踪每期款项的收取情况。

#### Acceptance Criteria

1. WHEN a contract is created, THE System SHALL allow creating multiple payment schedule entries
2. THE Payment_Schedule SHALL include phase_name to describe the payment milestone (e.g., "签约款", "一审结束", "执行到位")
3. THE Payment_Schedule SHALL include amount field for the payment amount
4. THE Payment_Schedule SHALL include percentage field for contingency fee calculations
5. THE Payment_Schedule SHALL include planned_date for expected payment date
6. THE Payment_Schedule SHALL include actual_date for actual payment date
7. THE Payment_Schedule SHALL include status field with values (PENDING, PARTIAL, PAID, CANCELLED)
8. WHEN a payment is received, THE System SHALL update the corresponding payment schedule status
9. THE System SHALL calculate total planned amount and compare with contract total_amount

### Requirement 3: 合同参与人管理

**User Story:** As a 主管合伙人, I want to 管理合同的参与律师及其提成比例, so that I can 确保提成分配的准确性。

#### Acceptance Criteria

1. THE Contract_Participant SHALL support multiple participants per contract
2. THE Contract_Participant SHALL include role field with values (LEAD, CO_COUNSEL, ORIGINATOR, PARALEGAL)
3. THE Contract_Participant SHALL include commission_rate field for commission percentage
4. WHEN a contract is created, THE System SHALL require at least one LEAD participant
5. THE System SHALL validate that total commission_rate does not exceed 100%
6. WHEN contract participants are modified, THE System SHALL log the change history
7. THE Contract_Participant SHALL be synchronized with Matter_Participant when matter is created from contract

### Requirement 4: 合同状态流转

**User Story:** As a 律师, I want to 跟踪合同的完整生命周期, so that I can 了解合同当前状态和历史变更。

#### Acceptance Criteria

1. THE Contract SHALL support status values: DRAFT, PENDING, ACTIVE, REJECTED, TERMINATED, COMPLETED, EXPIRED
2. WHEN a contract status changes from DRAFT to PENDING, THE System SHALL trigger approval workflow
3. WHEN a contract is APPROVED, THE System SHALL change status to ACTIVE
4. WHEN a contract is REJECTED, THE System SHALL record rejection reason
5. WHEN all payments are received and matter is closed, THE System SHALL allow changing status to COMPLETED
6. WHEN contract expiry_date passes, THE System SHALL automatically change status to EXPIRED
7. THE System SHALL maintain a status change history log

### Requirement 5: 合同与项目关联

**User Story:** As a 律师, I want to 基于已审批合同创建项目, so that I can 确保项目有合法的委托依据。

#### Acceptance Criteria

1. WHEN creating a matter, THE System SHALL require a valid contract_id
2. THE Contract SHALL be in ACTIVE status before a matter can be created
3. THE System SHALL copy contract participants to matter participants when matter is created
4. THE System SHALL prevent one contract from being associated with multiple matters (one-to-one relationship)
5. WHEN a contract is terminated, THE System SHALL notify the associated matter

### Requirement 6: 合同查询与统计

**User Story:** As a 管理层, I want to 查询和统计合同数据, so that I can 了解业务开展情况。

#### Acceptance Criteria

1. THE System SHALL support filtering contracts by status, contract_type, fee_type, client_id, signer_id
2. THE System SHALL support filtering contracts by date range (sign_date, effective_date, expiry_date)
3. THE System SHALL support filtering contracts by claim_amount range
4. THE System SHALL provide contract statistics including total count, total amount, paid amount by status
5. THE System SHALL support exporting contract list to Excel format

### Requirement 7: 合同模板应用

**User Story:** As a 律师, I want to 基于模板快速创建合同, so that I can 提高合同起草效率。

#### Acceptance Criteria

1. WHEN creating a contract, THE System SHALL allow selecting a contract template
2. WHEN a template is selected, THE System SHALL populate contract fields from template defaults
3. THE System SHALL support variable substitution in template content (e.g., ${clientName}, ${totalAmount})
4. THE System SHALL allow modifying template-generated content before saving
5. THE Contract SHALL store the template_id for reference

