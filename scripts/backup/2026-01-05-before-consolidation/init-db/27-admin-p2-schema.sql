-- =====================================================
-- 行政后勤模块P2功能数据库表结构
-- 模块：M8 行政后勤
-- 包含：加班记录、外出登记、会议记录、资产盘点等
-- =====================================================

-- -----------------------------------------------------
-- 1. 加班申请表 (overtime_application) - M8-004
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS overtime_application (
    id BIGSERIAL PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL UNIQUE,          -- 申请编号
    user_id BIGINT NOT NULL,                            -- 申请人ID
    overtime_date DATE NOT NULL,                        -- 加班日期
    start_time TIME NOT NULL,                           -- 开始时间
    end_time TIME NOT NULL,                             -- 结束时间
    overtime_hours DECIMAL(4,2),                        -- 加班时长(小时)
    reason VARCHAR(500),                                -- 加班原因
    work_content VARCHAR(1000),                          -- 工作内容
    status VARCHAR(20) DEFAULT 'PENDING',               -- 状态: PENDING待审批/APPROVED已批准/REJECTED已拒绝
    approver_id BIGINT,                                 -- 审批人ID
    approved_at TIMESTAMP,                              -- 审批时间
    approval_comment VARCHAR(500),                      -- 审批意见
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_overtime_user ON overtime_application(user_id);
CREATE INDEX idx_overtime_date ON overtime_application(overtime_date);
CREATE INDEX idx_overtime_status ON overtime_application(status);

COMMENT ON TABLE overtime_application IS '加班申请表';
COMMENT ON COLUMN overtime_application.status IS '状态: PENDING待审批/APPROVED已批准/REJECTED已拒绝';

-- -----------------------------------------------------
-- 2. 外出登记表 (go_out_record) - M8-005
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS go_out_record (
    id BIGSERIAL PRIMARY KEY,
    record_no VARCHAR(50) NOT NULL UNIQUE,               -- 登记编号
    user_id BIGINT NOT NULL,                            -- 外出人ID
    out_time TIMESTAMP NOT NULL,                        -- 外出时间
    expected_return_time TIMESTAMP,                     -- 预计返回时间
    actual_return_time TIMESTAMP,                       -- 实际返回时间
    location VARCHAR(200),                              -- 外出地点
    reason VARCHAR(500) NOT NULL,                       -- 外出事由
    companions VARCHAR(500),                             -- 同行人员
    status VARCHAR(20) DEFAULT 'OUT',                  -- 状态: OUT外出中/RETURNED已返回
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_go_out_user ON go_out_record(user_id);
CREATE INDEX idx_go_out_time ON go_out_record(out_time);
CREATE INDEX idx_go_out_status ON go_out_record(status);

COMMENT ON TABLE go_out_record IS '外出登记表';
COMMENT ON COLUMN go_out_record.status IS '状态: OUT外出中/RETURNED已返回';

-- -----------------------------------------------------
-- 3. 会议记录表 (meeting_record) - M8-023
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS meeting_record (
    id BIGSERIAL PRIMARY KEY,
    record_no VARCHAR(50) NOT NULL UNIQUE,               -- 记录编号
    booking_id BIGINT,                                  -- 关联预约ID
    room_id BIGINT,                                     -- 会议室ID
    title VARCHAR(200) NOT NULL,                        -- 会议主题
    meeting_date DATE NOT NULL,                         -- 会议日期
    start_time TIME NOT NULL,                           -- 开始时间
    end_time TIME NOT NULL,                             -- 结束时间
    organizer_id BIGINT NOT NULL,                       -- 组织者ID
    attendees VARCHAR(1000),                            -- 参会人员(JSON数组)
    content TEXT,                                       -- 会议内容
    decisions TEXT,                                     -- 会议决议
    action_items TEXT,                                  -- 行动项(JSON格式)
    attachment_url VARCHAR(500),                        -- 附件URL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_meeting_record_booking ON meeting_record(booking_id);
CREATE INDEX idx_meeting_record_room ON meeting_record(room_id);
CREATE INDEX idx_meeting_record_date ON meeting_record(meeting_date);

COMMENT ON TABLE meeting_record IS '会议记录表';

-- -----------------------------------------------------
-- 4. 资产盘点表 (asset_inventory) - M8-033
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS asset_inventory (
    id BIGSERIAL PRIMARY KEY,
    inventory_no VARCHAR(50) NOT NULL UNIQUE,            -- 盘点编号
    inventory_date DATE NOT NULL,                       -- 盘点日期
    inventory_type VARCHAR(20) NOT NULL,                 -- 盘点类型: FULL全盘/PARTIAL抽盘
    department_id BIGINT,                               -- 盘点部门ID
    location VARCHAR(200),                              -- 盘点位置
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',           -- 状态: IN_PROGRESS进行中/COMPLETED已完成
    total_count INT DEFAULT 0,                          -- 应盘数量
    actual_count INT DEFAULT 0,                         -- 实盘数量
    surplus_count INT DEFAULT 0,                        -- 盘盈数量
    shortage_count INT DEFAULT 0,                       -- 盘亏数量
    remark VARCHAR(500),                                 -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_asset_inventory_date ON asset_inventory(inventory_date);
CREATE INDEX idx_asset_inventory_status ON asset_inventory(status);

COMMENT ON TABLE asset_inventory IS '资产盘点表';
COMMENT ON COLUMN asset_inventory.inventory_type IS '盘点类型: FULL全盘/PARTIAL抽盘';
COMMENT ON COLUMN asset_inventory.status IS '状态: IN_PROGRESS进行中/COMPLETED已完成';

-- -----------------------------------------------------
-- 5. 资产盘点明细表 (asset_inventory_detail) - M8-033
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS asset_inventory_detail (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL,                       -- 盘点ID
    asset_id BIGINT NOT NULL,                            -- 资产ID
    expected_status VARCHAR(20),                         -- 预期状态
    actual_status VARCHAR(20),                          -- 实际状态
    expected_location VARCHAR(200),                      -- 预期位置
    actual_location VARCHAR(200),                       -- 实际位置
    expected_user_id BIGINT,                            -- 预期使用人ID
    actual_user_id BIGINT,                              -- 实际使用人ID
    discrepancy_type VARCHAR(20),                        -- 差异类型: NORMAL正常/SURPLUS盘盈/SHORTAGE盘亏/LOCATION位置不符/STATUS状态不符
    discrepancy_desc VARCHAR(500),                      -- 差异说明
    remark VARCHAR(500),                                 -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_detail_inventory ON asset_inventory_detail(inventory_id);
CREATE INDEX idx_inventory_detail_asset ON asset_inventory_detail(asset_id);

COMMENT ON TABLE asset_inventory_detail IS '资产盘点明细表';
COMMENT ON COLUMN asset_inventory_detail.discrepancy_type IS '差异类型: NORMAL正常/SURPLUS盘盈/SHORTAGE盘亏/LOCATION位置不符/STATUS状态不符';

