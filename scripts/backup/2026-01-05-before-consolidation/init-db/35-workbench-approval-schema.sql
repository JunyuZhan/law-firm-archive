-- ============================================
-- 智慧律所管理系统 - 审批管理模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-04
-- ============================================

-- 审批记录表
CREATE TABLE IF NOT EXISTS workbench_approval (
    id BIGSERIAL PRIMARY KEY,
    approval_no VARCHAR(50) NOT NULL UNIQUE,
    
    -- 业务信息
    business_type VARCHAR(50) NOT NULL,
    business_id BIGINT NOT NULL,
    business_no VARCHAR(50),
    business_title VARCHAR(200),
    
    -- 发起人信息
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(100),
    
    -- 审批人信息
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(100),
    
    -- 审批状态
    status VARCHAR(20) DEFAULT 'PENDING',
    comment TEXT,
    approved_at TIMESTAMP,
    
    -- 优先级和紧急程度
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    urgency VARCHAR(20) DEFAULT 'NORMAL',
    
    -- 业务数据快照
    business_snapshot TEXT,
    
    -- 基础字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE workbench_approval IS '审批记录表';
COMMENT ON COLUMN workbench_approval.approval_no IS '审批编号';
COMMENT ON COLUMN workbench_approval.business_type IS '业务类型：CONTRACT-合同, SEAL_APPLICATION-用印申请, CONFLICT_CHECK-利冲检查, EXPENSE-费用报销, etc.';
COMMENT ON COLUMN workbench_approval.business_id IS '业务ID（关联具体业务表的主键）';
COMMENT ON COLUMN workbench_approval.business_no IS '业务编号（如合同编号、用印申请编号等）';
COMMENT ON COLUMN workbench_approval.business_title IS '业务标题';
COMMENT ON COLUMN workbench_approval.applicant_id IS '发起人ID';
COMMENT ON COLUMN workbench_approval.applicant_name IS '发起人姓名';
COMMENT ON COLUMN workbench_approval.approver_id IS '审批人ID';
COMMENT ON COLUMN workbench_approval.approver_name IS '审批人姓名';
COMMENT ON COLUMN workbench_approval.status IS '审批状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消';
COMMENT ON COLUMN workbench_approval.comment IS '审批意见';
COMMENT ON COLUMN workbench_approval.approved_at IS '审批时间';
COMMENT ON COLUMN workbench_approval.priority IS '优先级：HIGH-高, MEDIUM-中, LOW-低';
COMMENT ON COLUMN workbench_approval.urgency IS '紧急程度：URGENT-紧急, NORMAL-普通';
COMMENT ON COLUMN workbench_approval.business_snapshot IS '业务数据快照（JSON格式，保存审批时的业务数据）';

-- 创建索引
CREATE INDEX idx_workbench_approval_business ON workbench_approval(business_type, business_id);
CREATE INDEX idx_workbench_approval_approver ON workbench_approval(approver_id, status);
CREATE INDEX idx_workbench_approval_applicant ON workbench_approval(applicant_id);
CREATE INDEX idx_workbench_approval_status ON workbench_approval(status);
CREATE INDEX idx_workbench_approval_created_at ON workbench_approval(created_at DESC);

