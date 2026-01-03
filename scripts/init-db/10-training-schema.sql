-- 培训管理模块数据库表结构
-- 创建时间: 2026-01-03

-- 培训计划表
CREATE TABLE IF NOT EXISTS hr_training (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '培训标题',
    training_type VARCHAR(50) NOT NULL COMMENT '培训类型：INTERNAL-内部培训, EXTERNAL-外部培训, ONLINE-在线培训',
    category VARCHAR(50) COMMENT '培训分类：LAW-法律知识, SKILL-业务技能, MANAGEMENT-管理能力, OTHER-其他',
    description TEXT COMMENT '培训描述',
    trainer VARCHAR(100) COMMENT '讲师/培训机构',
    location VARCHAR(200) COMMENT '培训地点',
    start_time TIMESTAMP NOT NULL COMMENT '开始时间',
    end_time TIMESTAMP NOT NULL COMMENT '结束时间',
    duration INTEGER COMMENT '培训时长（小时）',
    credits INTEGER DEFAULT 0 COMMENT '学分',
    max_participants INTEGER COMMENT '最大参与人数',
    current_participants INTEGER DEFAULT 0 COMMENT '当前报名人数',
    enroll_deadline DATE COMMENT '报名截止日期',
    status VARCHAR(50) DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿, PUBLISHED-已发布, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消',
    materials_url VARCHAR(500) COMMENT '培训材料URL',
    remarks TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 培训记录表（员工参与培训的记录）
CREATE TABLE IF NOT EXISTS hr_training_record (
    id BIGSERIAL PRIMARY KEY,
    training_id BIGINT NOT NULL COMMENT '培训ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    enroll_time TIMESTAMP COMMENT '报名时间',
    check_in_time TIMESTAMP COMMENT '签到时间',
    check_out_time TIMESTAMP COMMENT '签退时间',
    actual_duration DECIMAL(5,2) COMMENT '实际参与时长（小时）',
    status VARCHAR(50) DEFAULT 'ENROLLED' COMMENT '状态：ENROLLED-已报名, ATTENDED-已参加, ABSENT-缺席, CANCELLED-已取消',
    score DECIMAL(5,2) COMMENT '考核成绩',
    passed BOOLEAN COMMENT '是否通过',
    earned_credits INTEGER DEFAULT 0 COMMENT '获得学分',
    feedback TEXT COMMENT '培训反馈/评价',
    rating INTEGER COMMENT '评分（1-5）',
    certificate_url VARCHAR(500) COMMENT '证书URL',
    remarks TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_training_record_training FOREIGN KEY (training_id) REFERENCES hr_training(id),
    CONSTRAINT fk_training_record_employee FOREIGN KEY (employee_id) REFERENCES sys_user(id)
);

-- 角色菜单关联表（如果不存在）
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id),
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

-- 用户角色关联表（如果不存在）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

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
