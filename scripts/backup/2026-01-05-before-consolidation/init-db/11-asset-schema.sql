-- =====================================================
-- 资产管理模块数据库表结构
-- 模块：M8 行政后勤 - 资产管理
-- 功能：M8-030~034 资产登记、领用、归还、盘点、报废
-- =====================================================

-- 资产表
CREATE TABLE IF NOT EXISTS admin_asset (
    id BIGSERIAL PRIMARY KEY,
    asset_no VARCHAR(50) NOT NULL UNIQUE,           -- 资产编号
    name VARCHAR(200) NOT NULL,                     -- 资产名称
    category VARCHAR(50) NOT NULL,                  -- 资产类别：OFFICE-办公设备, IT-IT设备, FURNITURE-家具, VEHICLE-车辆, OTHER-其他
    brand VARCHAR(100),                             -- 品牌
    model VARCHAR(100),                             -- 型号
    specification VARCHAR(500),                     -- 规格参数
    serial_number VARCHAR(100),                     -- 序列号/出厂编号
    purchase_date DATE,                             -- 购置日期
    purchase_price DECIMAL(12,2),                   -- 购置价格
    supplier VARCHAR(200),                          -- 供应商
    warranty_expire_date DATE,                      -- 保修到期日
    useful_life INTEGER,                            -- 使用年限（月）
    location VARCHAR(200),                          -- 存放位置
    current_user_id BIGINT,                         -- 当前使用人ID
    department_id BIGINT,                           -- 所属部门ID
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',     -- 状态：IDLE-闲置, IN_USE-使用中, MAINTENANCE-维修中, SCRAPPED-已报废
    image_url VARCHAR(500),                         -- 资产图片URL
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 资产操作记录表
CREATE TABLE IF NOT EXISTS admin_asset_record (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL,                       -- 资产ID
    record_type VARCHAR(20) NOT NULL,               -- 记录类型：RECEIVE-领用, RETURN-归还, TRANSFER-转移, MAINTENANCE-维修, SCRAP-报废
    operator_id BIGINT NOT NULL,                    -- 操作人ID
    from_user_id BIGINT,                            -- 原使用人ID（转移/归还时）
    to_user_id BIGINT,                              -- 新使用人ID（领用/转移时）
    operate_date DATE NOT NULL,                     -- 操作日期
    expected_return_date DATE,                      -- 预计归还日期（领用时）
    actual_return_date DATE,                        -- 实际归还日期
    reason TEXT,                                    -- 原因说明
    maintenance_cost DECIMAL(10,2),                 -- 维修费用（维修时）
    approval_status VARCHAR(20) DEFAULT 'PENDING', -- 审批状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝
    approver_id BIGINT,                             -- 审批人ID
    approval_comment TEXT,                          -- 审批意见
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_asset_record_asset FOREIGN KEY (asset_id) REFERENCES admin_asset(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_asset_no ON admin_asset(asset_no);
CREATE INDEX IF NOT EXISTS idx_asset_category ON admin_asset(category);
CREATE INDEX IF NOT EXISTS idx_asset_status ON admin_asset(status);
CREATE INDEX IF NOT EXISTS idx_asset_department ON admin_asset(department_id);
CREATE INDEX IF NOT EXISTS idx_asset_current_user ON admin_asset(current_user_id);
CREATE INDEX IF NOT EXISTS idx_asset_record_asset ON admin_asset_record(asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_record_type ON admin_asset_record(record_type);
CREATE INDEX IF NOT EXISTS idx_asset_record_operator ON admin_asset_record(operator_id);

-- 添加表注释
COMMENT ON TABLE admin_asset IS '资产信息表';
COMMENT ON TABLE admin_asset_record IS '资产操作记录表';

-- 添加字段注释
COMMENT ON COLUMN admin_asset.asset_no IS '资产编号';
COMMENT ON COLUMN admin_asset.name IS '资产名称';
COMMENT ON COLUMN admin_asset.category IS '资产类别：OFFICE-办公设备, IT-IT设备, FURNITURE-家具, VEHICLE-车辆, OTHER-其他';
COMMENT ON COLUMN admin_asset.status IS '状态：IDLE-闲置, IN_USE-使用中, MAINTENANCE-维修中, SCRAPPED-已报废';
COMMENT ON COLUMN admin_asset_record.record_type IS '记录类型：RECEIVE-领用, RETURN-归还, TRANSFER-转移, MAINTENANCE-维修, SCRAP-报废';
COMMENT ON COLUMN admin_asset_record.approval_status IS '审批状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝';

-- 插入示例数据
INSERT INTO admin_asset (asset_no, name, category, brand, model, purchase_date, purchase_price, location, status, remarks)
VALUES 
    ('IT202601001', 'MacBook Pro 14寸', 'IT', 'Apple', 'MacBook Pro 14 M3', '2026-01-01', 16999.00, '办公区A-01', 'IDLE', '新购置笔记本电脑'),
    ('IT202601002', 'Dell显示器27寸', 'IT', 'Dell', 'U2723QE', '2026-01-01', 4599.00, '办公区A-01', 'IDLE', '4K显示器'),
    ('OF202601001', '办公桌', 'FURNITURE', '震旦', 'ZD-1800', '2026-01-01', 2800.00, '办公区B-03', 'IDLE', '1.8米办公桌'),
    ('OF202601002', '办公椅', 'FURNITURE', '西昊', 'M57', '2026-01-01', 1299.00, '办公区B-03', 'IDLE', '人体工学椅');
