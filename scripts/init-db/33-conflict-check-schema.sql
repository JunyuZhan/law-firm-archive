-- ============================================
-- 智慧律所管理系统 - 利冲检查模块数据库脚本
-- 版本: 1.0.0
-- 创建日期: 2026-01-03
-- ============================================

-- 利冲检查表
CREATE TABLE IF NOT EXISTS crm_conflict_check (
    id BIGSERIAL PRIMARY KEY,
    check_no VARCHAR(50) NOT NULL UNIQUE,
    check_type VARCHAR(20) NOT NULL,
    client_id BIGINT,
    matter_id BIGINT,
    client_name VARCHAR(200),
    opposing_party VARCHAR(200),
    related_parties TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    result_description TEXT,
    conflict_details TEXT,
    applicant_id BIGINT,
    reviewer_id BIGINT,
    reviewed_at TIMESTAMP,
    review_comment TEXT,
    remark TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_crm_conflict_check_check_no ON crm_conflict_check(check_no);
CREATE INDEX idx_crm_conflict_check_client_id ON crm_conflict_check(client_id);
CREATE INDEX idx_crm_conflict_check_matter_id ON crm_conflict_check(matter_id);
CREATE INDEX idx_crm_conflict_check_status ON crm_conflict_check(status);
CREATE INDEX idx_crm_conflict_check_applicant_id ON crm_conflict_check(applicant_id);

COMMENT ON TABLE crm_conflict_check IS '利冲检查表';
COMMENT ON COLUMN crm_conflict_check.check_no IS '检查编号';
COMMENT ON COLUMN crm_conflict_check.check_type IS '检查类型：NEW_CLIENT-新客户, NEW_MATTER-新案件, MANUAL-手动检查';
COMMENT ON COLUMN crm_conflict_check.status IS '检查状态：PENDING-待检查, CHECKING-检查中, PASSED-通过, CONFLICT-存在冲突, EXEMPTION_PENDING-豁免待审批, WAIVED-已豁免, REJECTED-已拒绝';

-- 利冲检查项表
CREATE TABLE IF NOT EXISTS crm_conflict_check_item (
    id BIGSERIAL PRIMARY KEY,
    check_id BIGINT NOT NULL,
    party_name VARCHAR(200) NOT NULL,
    party_type VARCHAR(20) NOT NULL,
    id_number VARCHAR(50),
    has_conflict BOOLEAN DEFAULT FALSE,
    conflict_detail TEXT,
    related_matter_id BIGINT,
    related_client_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_crm_conflict_check_item_check_id ON crm_conflict_check_item(check_id);
CREATE INDEX idx_crm_conflict_check_item_party_name ON crm_conflict_check_item(party_name);
CREATE INDEX idx_crm_conflict_check_item_has_conflict ON crm_conflict_check_item(has_conflict);

COMMENT ON TABLE crm_conflict_check_item IS '利冲检查项表';
COMMENT ON COLUMN crm_conflict_check_item.party_type IS '当事人类型：CLIENT-委托人, OPPOSING-对方当事人, RELATED-关联方';

