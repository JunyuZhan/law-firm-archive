-- =============================================
-- 案由/罪名数据表
-- =============================================

-- 案由类型枚举: CIVIL(民事), CRIMINAL(刑事), ADMIN(行政)
CREATE TABLE IF NOT EXISTS sys_cause_of_action (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,                    -- 案由代码，如 '14', '8.1'
    name VARCHAR(200) NOT NULL,                   -- 案由名称
    cause_type VARCHAR(20) NOT NULL,              -- 类型: CIVIL, CRIMINAL, ADMIN
    category_code VARCHAR(20),                    -- 所属大类代码，如 'P2'
    category_name VARCHAR(100),                   -- 所属大类名称，如 '婚姻家庭纠纷'
    parent_code VARCHAR(20),                      -- 父级案由代码（用于子案由）
    level INT DEFAULT 1,                          -- 层级: 1=一级案由, 2=二级案由
    sort_order INT DEFAULT 0,                     -- 排序号
    is_active BOOLEAN DEFAULT TRUE,               -- 是否启用
    version INT DEFAULT 1,                        -- 乐观锁版本号
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,                            -- 创建人ID
    updated_by BIGINT,                            -- 更新人ID
    deleted BOOLEAN DEFAULT FALSE,                -- 是否删除（软删除标记）
    UNIQUE(code, cause_type)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_cause_type ON sys_cause_of_action(cause_type);
CREATE INDEX IF NOT EXISTS idx_cause_code ON sys_cause_of_action(code);
CREATE INDEX IF NOT EXISTS idx_cause_category ON sys_cause_of_action(category_code);
CREATE INDEX IF NOT EXISTS idx_cause_parent ON sys_cause_of_action(parent_code);
CREATE INDEX IF NOT EXISTS idx_cause_active ON sys_cause_of_action(is_active);

COMMENT ON TABLE sys_cause_of_action IS '案由/罪名数据表';
COMMENT ON COLUMN sys_cause_of_action.code IS '案由代码';
COMMENT ON COLUMN sys_cause_of_action.name IS '案由名称';
COMMENT ON COLUMN sys_cause_of_action.cause_type IS '类型: CIVIL-民事, CRIMINAL-刑事, ADMIN-行政';
COMMENT ON COLUMN sys_cause_of_action.category_code IS '所属大类代码';
COMMENT ON COLUMN sys_cause_of_action.category_name IS '所属大类名称';
COMMENT ON COLUMN sys_cause_of_action.parent_code IS '父级案由代码';
COMMENT ON COLUMN sys_cause_of_action.level IS '层级';
COMMENT ON COLUMN sys_cause_of_action.sort_order IS '排序号';
COMMENT ON COLUMN sys_cause_of_action.is_active IS '是否启用';

-- =============================================
-- 创建案由查询函数
-- =============================================

-- 根据代码和类型获取案由名称
CREATE OR REPLACE FUNCTION get_cause_name(p_code VARCHAR, p_type VARCHAR DEFAULT 'CIVIL')
RETURNS VARCHAR AS $$
DECLARE
    v_name VARCHAR;
BEGIN
    SELECT name INTO v_name
    FROM sys_cause_of_action
    WHERE code = p_code AND cause_type = p_type AND is_active = TRUE
    LIMIT 1;
    
    RETURN COALESCE(v_name, p_code);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_cause_name IS '根据案由代码获取案由名称';

-- 注意：民事、刑事、行政案由数据已拆分到以下独立脚本（20-29区间为初始化数据）：
-- 22-civil-cause-of-action.sql   - 民事案由（2025年修订版，完整数据）
-- 23-criminal-cause-of-action.sql - 刑事罪名数据
-- 24-admin-cause-of-action.sql    - 行政案由数据
