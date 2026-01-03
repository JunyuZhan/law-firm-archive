-- 培训管理模块数据库表结构
-- 创建时间: 2026-01-03

-- 培训计划表
CREATE TABLE IF NOT EXISTS hr_training (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    training_type VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    trainer VARCHAR(100),
    location VARCHAR(200),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration INTEGER,
    credits INTEGER DEFAULT 0,
    max_participants INTEGER,
    current_participants INTEGER DEFAULT 0,
    enroll_deadline DATE,
    status VARCHAR(50) DEFAULT 'DRAFT',
    materials_url VARCHAR(500),
    remarks TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 培训记录表（员工参与培训的记录）
CREATE TABLE IF NOT EXISTS hr_training_record (
    id BIGSERIAL PRIMARY KEY,
    training_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    enroll_time TIMESTAMP,
    check_in_time TIMESTAMP,
    check_out_time TIMESTAMP,
    actual_duration DECIMAL(5,2),
    status VARCHAR(50) DEFAULT 'ENROLLED',
    score DECIMAL(5,2),
    passed BOOLEAN,
    earned_credits INTEGER DEFAULT 0,
    feedback TEXT,
    rating INTEGER,
    certificate_url VARCHAR(500),
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_training_record_training FOREIGN KEY (training_id) REFERENCES hr_training(id),
    CONSTRAINT fk_training_record_employee FOREIGN KEY (employee_id) REFERENCES sys_user(id)
);

-- 角色菜单关联表（如果不存在）
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id),
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

-- 用户角色关联表（如果不存在）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- 添加列注释
COMMENT ON COLUMN hr_training.title IS '培训标题';
COMMENT ON COLUMN hr_training.training_type IS '培训类型：INTERNAL-内部培训, EXTERNAL-外部培训, ONLINE-在线培训';
COMMENT ON COLUMN hr_training.category IS '培训分类：LAW-法律知识, SKILL-业务技能, MANAGEMENT-管理能力, OTHER-其他';
COMMENT ON COLUMN hr_training.description IS '培训描述';
COMMENT ON COLUMN hr_training.trainer IS '讲师/培训机构';
COMMENT ON COLUMN hr_training.location IS '培训地点';
COMMENT ON COLUMN hr_training.start_time IS '开始时间';
COMMENT ON COLUMN hr_training.end_time IS '结束时间';
COMMENT ON COLUMN hr_training.duration IS '培训时长（小时）';
COMMENT ON COLUMN hr_training.credits IS '学分';
COMMENT ON COLUMN hr_training.max_participants IS '最大参与人数';
COMMENT ON COLUMN hr_training.current_participants IS '当前报名人数';
COMMENT ON COLUMN hr_training.enroll_deadline IS '报名截止日期';
COMMENT ON COLUMN hr_training.status IS '状态：DRAFT-草稿, PUBLISHED-已发布, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消';
COMMENT ON COLUMN hr_training.materials_url IS '培训材料URL';
COMMENT ON COLUMN hr_training.remarks IS '备注';
COMMENT ON COLUMN hr_training.created_by IS '创建人';

COMMENT ON COLUMN hr_training_record.training_id IS '培训ID';
COMMENT ON COLUMN hr_training_record.employee_id IS '员工ID';
COMMENT ON COLUMN hr_training_record.enroll_time IS '报名时间';
COMMENT ON COLUMN hr_training_record.check_in_time IS '签到时间';
COMMENT ON COLUMN hr_training_record.check_out_time IS '签退时间';
COMMENT ON COLUMN hr_training_record.actual_duration IS '实际参与时长（小时）';
COMMENT ON COLUMN hr_training_record.status IS '状态：ENROLLED-已报名, ATTENDED-已参加, ABSENT-缺席, CANCELLED-已取消';
COMMENT ON COLUMN hr_training_record.score IS '考核成绩';
COMMENT ON COLUMN hr_training_record.passed IS '是否通过';
COMMENT ON COLUMN hr_training_record.earned_credits IS '获得学分';
COMMENT ON COLUMN hr_training_record.feedback IS '培训反馈/评价';
COMMENT ON COLUMN hr_training_record.rating IS '评分（1-5）';
COMMENT ON COLUMN hr_training_record.certificate_url IS '证书URL';
COMMENT ON COLUMN hr_training_record.remarks IS '备注';

COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';

COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_training_status ON hr_training(status);
CREATE INDEX IF NOT EXISTS idx_training_start_time ON hr_training(start_time);
CREATE INDEX IF NOT EXISTS idx_training_record_training ON hr_training_record(training_id);
CREATE INDEX IF NOT EXISTS idx_training_record_employee ON hr_training_record(employee_id);
CREATE INDEX IF NOT EXISTS idx_role_menu_role ON sys_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_user_role_user ON sys_user_role(user_id);

-- 添加注释
COMMENT ON TABLE hr_training IS '培训计划表';
COMMENT ON TABLE hr_training_record IS '培训记录表';
COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';
COMMENT ON TABLE sys_user_role IS '用户角色关联表';
