-- =====================================================
-- 采购管理模块数据库表结构
-- 模块：M8 行政后勤 - 采购管理
-- 功能：M8-040~043 采购申请、审批、入库、供应商管理
-- =====================================================

-- 供应商表
CREATE TABLE IF NOT EXISTS admin_supplier (
    id BIGSERIAL PRIMARY KEY,
    supplier_no VARCHAR(50) NOT NULL UNIQUE,        -- 供应商编号
    name VARCHAR(200) NOT NULL,                     -- 供应商名称
    supplier_type VARCHAR(20) NOT NULL,             -- 类型：GOODS-物品, SERVICE-服务, BOTH-综合
    contact_person VARCHAR(50),                     -- 联系人
    contact_phone VARCHAR(20),                      -- 联系电话
    contact_email VARCHAR(100),                     -- 联系邮箱
    address VARCHAR(500),                           -- 地址
    credit_code VARCHAR(50),                        -- 统一社会信用代码
    bank_name VARCHAR(100),                         -- 开户银行
    bank_account VARCHAR(50),                       -- 银行账号
    supply_scope TEXT,                              -- 供应范围
    rating VARCHAR(10) DEFAULT 'B',                 -- 评级：A-优秀, B-良好, C-一般, D-较差
    status VARCHAR(20) DEFAULT 'ACTIVE',            -- 状态：ACTIVE-正常, INACTIVE-停用
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 采购申请表
CREATE TABLE IF NOT EXISTS admin_purchase_request (
    id BIGSERIAL PRIMARY KEY,
    request_no VARCHAR(50) NOT NULL UNIQUE,         -- 申请单号
    title VARCHAR(200) NOT NULL,                    -- 申请标题
    applicant_id BIGINT NOT NULL,                   -- 申请人ID
    department_id BIGINT,                           -- 申请部门ID
    purchase_type VARCHAR(20) NOT NULL,             -- 采购类型：OFFICE-办公用品, IT-IT设备, FURNITURE-家具, SERVICE-服务, OTHER-其他
    estimated_amount DECIMAL(12,2),                 -- 预计总金额
    actual_amount DECIMAL(12,2),                    -- 实际总金额
    expected_date DATE,                             -- 期望到货日期
    reason TEXT,                                    -- 采购原因
    status VARCHAR(20) DEFAULT 'DRAFT',             -- 状态：DRAFT-草稿, PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, PURCHASING-采购中, COMPLETED-已完成, CANCELLED-已取消
    approver_id BIGINT,                             -- 审批人ID
    approval_date DATE,                             -- 审批日期
    approval_comment TEXT,                          -- 审批意见
    supplier_id BIGINT,                             -- 供应商ID
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 采购明细表
CREATE TABLE IF NOT EXISTS admin_purchase_item (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,                     -- 采购申请ID
    item_name VARCHAR(200) NOT NULL,                -- 物品名称
    specification VARCHAR(500),                     -- 规格型号
    unit VARCHAR(20),                               -- 单位
    quantity INTEGER NOT NULL DEFAULT 1,            -- 数量
    estimated_price DECIMAL(10,2),                  -- 预估单价
    actual_price DECIMAL(10,2),                     -- 实际单价
    estimated_amount DECIMAL(12,2),                 -- 预估金额
    actual_amount DECIMAL(12,2),                    -- 实际金额
    received_quantity INTEGER DEFAULT 0,            -- 已入库数量
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_purchase_item_request FOREIGN KEY (request_id) REFERENCES admin_purchase_request(id)
);

-- 采购入库记录表
CREATE TABLE IF NOT EXISTS admin_purchase_receive (
    id BIGSERIAL PRIMARY KEY,
    receive_no VARCHAR(50) NOT NULL UNIQUE,         -- 入库单号
    request_id BIGINT NOT NULL,                     -- 采购申请ID
    item_id BIGINT NOT NULL,                        -- 采购明细ID
    quantity INTEGER NOT NULL,                      -- 入库数量
    receive_date DATE NOT NULL,                     -- 入库日期
    receiver_id BIGINT NOT NULL,                    -- 入库人ID
    location VARCHAR(200),                          -- 存放位置
    convert_to_asset BOOLEAN DEFAULT FALSE,         -- 是否转为资产
    asset_id BIGINT,                                -- 关联资产ID
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_purchase_receive_request FOREIGN KEY (request_id) REFERENCES admin_purchase_request(id),
    CONSTRAINT fk_purchase_receive_item FOREIGN KEY (item_id) REFERENCES admin_purchase_item(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_supplier_no ON admin_supplier(supplier_no);
CREATE INDEX IF NOT EXISTS idx_supplier_type ON admin_supplier(supplier_type);
CREATE INDEX IF NOT EXISTS idx_supplier_status ON admin_supplier(status);
CREATE INDEX IF NOT EXISTS idx_purchase_request_no ON admin_purchase_request(request_no);
CREATE INDEX IF NOT EXISTS idx_purchase_request_applicant ON admin_purchase_request(applicant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_request_status ON admin_purchase_request(status);
CREATE INDEX IF NOT EXISTS idx_purchase_item_request ON admin_purchase_item(request_id);
CREATE INDEX IF NOT EXISTS idx_purchase_receive_request ON admin_purchase_receive(request_id);
CREATE INDEX IF NOT EXISTS idx_purchase_receive_item ON admin_purchase_receive(item_id);

-- 添加表注释
COMMENT ON TABLE admin_supplier IS '供应商信息表';
COMMENT ON TABLE admin_purchase_request IS '采购申请表';
COMMENT ON TABLE admin_purchase_item IS '采购明细表';
COMMENT ON TABLE admin_purchase_receive IS '采购入库记录表';

-- 插入示例供应商数据
INSERT INTO admin_supplier (supplier_no, name, supplier_type, contact_person, contact_phone, address, rating, status)
VALUES 
    ('SUP202601001', '京东企业购', 'GOODS', '张经理', '400-606-5500', '北京市亦庄经济开发区', 'A', 'ACTIVE'),
    ('SUP202601002', '得力办公', 'GOODS', '李经理', '400-185-0555', '浙江省宁波市北仑区', 'A', 'ACTIVE'),
    ('SUP202601003', '联想商用', 'GOODS', '王经理', '400-100-6000', '北京市海淀区', 'B', 'ACTIVE');
