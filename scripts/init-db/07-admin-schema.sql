-- =====================================================
-- 行政后勤模块数据库表结构
-- 模块：M8 行政后勤
-- 包含：考勤管理、请假管理、会议室管理
-- =====================================================

-- -----------------------------------------------------
-- 1. 考勤记录表 (attendance)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                          -- 员工ID
    attendance_date DATE NOT NULL,                    -- 考勤日期
    check_in_time TIMESTAMP,                          -- 签到时间
    check_out_time TIMESTAMP,                         -- 签退时间
    check_in_location VARCHAR(200),                   -- 签到地点
    check_out_location VARCHAR(200),                  -- 签退地点
    check_in_device VARCHAR(100),                     -- 签到设备
    check_out_device VARCHAR(100),                    -- 签退设备
    status VARCHAR(20) DEFAULT 'NORMAL',              -- 状态: NORMAL正常/LATE迟到/EARLY早退/ABSENT缺勤/LEAVE请假
    work_hours DECIMAL(4,2),                          -- 工作时长(小时)
    overtime_hours DECIMAL(4,2) DEFAULT 0,            -- 加班时长(小时)
    remark VARCHAR(500),                              -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_attendance_user_date UNIQUE (user_id, attendance_date)
);

CREATE INDEX idx_attendance_user ON attendance(user_id);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_status ON attendance(status);

COMMENT ON TABLE attendance IS '考勤记录表';
COMMENT ON COLUMN attendance.status IS '状态: NORMAL正常/LATE迟到/EARLY早退/ABSENT缺勤/LEAVE请假';

-- -----------------------------------------------------
-- 2. 请假类型表 (leave_type)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,                        -- 类型名称
    code VARCHAR(30) NOT NULL UNIQUE,                 -- 类型编码
    paid BOOLEAN DEFAULT TRUE,                        -- 是否带薪
    annual_limit DECIMAL(5,1),                        -- 年度限额(天)
    need_approval BOOLEAN DEFAULT TRUE,               -- 是否需要审批
    description VARCHAR(200),                         -- 描述
    sort_order INT DEFAULT 0,                         -- 排序
    enabled BOOLEAN DEFAULT TRUE,                     -- 是否启用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE leave_type IS '请假类型表';

-- 初始化请假类型
INSERT INTO leave_type (name, code, paid, annual_limit, need_approval, description, sort_order) VALUES
('年假', 'ANNUAL', TRUE, 15, TRUE, '带薪年假', 1),
('事假', 'PERSONAL', FALSE, NULL, TRUE, '个人事务请假', 2),
('病假', 'SICK', TRUE, NULL, TRUE, '因病请假', 3),
('婚假', 'MARRIAGE', TRUE, 10, TRUE, '结婚请假', 4),
('产假', 'MATERNITY', TRUE, 158, TRUE, '生育请假', 5),
('陪产假', 'PATERNITY', TRUE, 15, TRUE, '陪产请假', 6),
('丧假', 'BEREAVEMENT', TRUE, 3, TRUE, '丧事请假', 7),
('调休', 'COMPENSATORY', TRUE, NULL, TRUE, '加班调休', 8),
('外出', 'OUT', TRUE, NULL, TRUE, '外出办事', 9);

-- -----------------------------------------------------
-- 3. 请假申请表 (leave_application)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_application (
    id BIGSERIAL PRIMARY KEY,
    application_no VARCHAR(30) NOT NULL UNIQUE,       -- 申请编号
    user_id BIGINT NOT NULL,                          -- 申请人ID
    leave_type_id BIGINT NOT NULL,                    -- 请假类型ID
    start_time TIMESTAMP NOT NULL,                    -- 开始时间
    end_time TIMESTAMP NOT NULL,                      -- 结束时间
    duration DECIMAL(5,1) NOT NULL,                   -- 请假时长(天)
    reason VARCHAR(500) NOT NULL,                     -- 请假原因
    attachment_url VARCHAR(500),                      -- 附件URL(如病假条)
    status VARCHAR(20) DEFAULT 'PENDING',             -- 状态: PENDING待审批/APPROVED已批准/REJECTED已拒绝/CANCELLED已取消
    approver_id BIGINT,                               -- 审批人ID
    approved_at TIMESTAMP,                            -- 审批时间
    approval_comment VARCHAR(500),                    -- 审批意见
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_leave_app_user ON leave_application(user_id);
CREATE INDEX idx_leave_app_type ON leave_application(leave_type_id);
CREATE INDEX idx_leave_app_status ON leave_application(status);
CREATE INDEX idx_leave_app_time ON leave_application(start_time, end_time);

COMMENT ON TABLE leave_application IS '请假申请表';
COMMENT ON COLUMN leave_application.status IS '状态: PENDING待审批/APPROVED已批准/REJECTED已拒绝/CANCELLED已取消';

-- -----------------------------------------------------
-- 4. 假期余额表 (leave_balance)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_balance (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                          -- 员工ID
    leave_type_id BIGINT NOT NULL,                    -- 请假类型ID
    year INT NOT NULL,                                -- 年度
    total_days DECIMAL(5,1) NOT NULL DEFAULT 0,       -- 总天数
    used_days DECIMAL(5,1) NOT NULL DEFAULT 0,        -- 已用天数
    remaining_days DECIMAL(5,1) NOT NULL DEFAULT 0,   -- 剩余天数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_leave_balance UNIQUE (user_id, leave_type_id, year)
);

CREATE INDEX idx_leave_balance_user ON leave_balance(user_id);
CREATE INDEX idx_leave_balance_year ON leave_balance(year);

COMMENT ON TABLE leave_balance IS '假期余额表';

-- -----------------------------------------------------
-- 5. 会议室表 (meeting_room)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS meeting_room (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                       -- 会议室名称
    code VARCHAR(30) NOT NULL UNIQUE,                 -- 会议室编码
    location VARCHAR(200),                            -- 位置
    capacity INT NOT NULL,                            -- 容纳人数
    equipment VARCHAR(500),                           -- 设备(投影仪/白板/视频会议等)
    description VARCHAR(500),                         -- 描述
    status VARCHAR(20) DEFAULT 'AVAILABLE',           -- 状态: AVAILABLE可用/OCCUPIED占用/MAINTENANCE维护中
    enabled BOOLEAN DEFAULT TRUE,                     -- 是否启用
    sort_order INT DEFAULT 0,                         -- 排序
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE meeting_room IS '会议室表';
COMMENT ON COLUMN meeting_room.status IS '状态: AVAILABLE可用/OCCUPIED占用/MAINTENANCE维护中';

-- 初始化会议室
INSERT INTO meeting_room (name, code, location, capacity, equipment, description, sort_order) VALUES
('大会议室', 'ROOM-001', '3楼301', 30, '投影仪,白板,视频会议系统', '可容纳30人的大型会议室', 1),
('中会议室A', 'ROOM-002', '3楼302', 15, '投影仪,白板', '可容纳15人的中型会议室', 2),
('中会议室B', 'ROOM-003', '3楼303', 15, '投影仪,白板', '可容纳15人的中型会议室', 3),
('小会议室', 'ROOM-004', '3楼304', 8, '电视屏幕,白板', '可容纳8人的小型会议室', 4),
('洽谈室', 'ROOM-005', '1楼101', 6, '电视屏幕', '客户洽谈室', 5);

-- -----------------------------------------------------
-- 6. 会议预约表 (meeting_booking)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS meeting_booking (
    id BIGSERIAL PRIMARY KEY,
    booking_no VARCHAR(30) NOT NULL UNIQUE,           -- 预约编号
    room_id BIGINT NOT NULL,                          -- 会议室ID
    title VARCHAR(200) NOT NULL,                      -- 会议主题
    organizer_id BIGINT NOT NULL,                     -- 组织者ID
    start_time TIMESTAMP NOT NULL,                    -- 开始时间
    end_time TIMESTAMP NOT NULL,                      -- 结束时间
    attendees VARCHAR(1000),                          -- 参会人员(JSON数组)
    description VARCHAR(500),                         -- 会议描述
    status VARCHAR(20) DEFAULT 'BOOKED',              -- 状态: BOOKED已预约/IN_PROGRESS进行中/COMPLETED已完成/CANCELLED已取消
    reminder_sent BOOLEAN DEFAULT FALSE,              -- 是否已发送提醒
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_meeting_booking_room ON meeting_booking(room_id);
CREATE INDEX idx_meeting_booking_organizer ON meeting_booking(organizer_id);
CREATE INDEX idx_meeting_booking_time ON meeting_booking(start_time, end_time);
CREATE INDEX idx_meeting_booking_status ON meeting_booking(status);

COMMENT ON TABLE meeting_booking IS '会议预约表';
COMMENT ON COLUMN meeting_booking.status IS '状态: BOOKED已预约/IN_PROGRESS进行中/COMPLETED已完成/CANCELLED已取消';
