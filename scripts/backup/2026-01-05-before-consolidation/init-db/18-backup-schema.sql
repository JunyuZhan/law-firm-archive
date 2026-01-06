-- 系统备份记录表
CREATE TABLE IF NOT EXISTS sys_backup (
    id BIGSERIAL PRIMARY KEY,
    backup_no VARCHAR(50) NOT NULL UNIQUE, -- 备份编号
    backup_type VARCHAR(20) NOT NULL, -- 备份类型: FULL-全量, INCREMENTAL-增量, DATABASE-数据库, FILE-文件
    backup_name VARCHAR(200) NOT NULL, -- 备份名称
    backup_path VARCHAR(500) NOT NULL, -- 备份文件路径
    file_size BIGINT, -- 文件大小（字节）
    status VARCHAR(20) DEFAULT 'PENDING', -- 状态: PENDING-进行中, SUCCESS-成功, FAILED-失败
    backup_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 备份时间
    restore_time TIMESTAMP, -- 恢复时间
    description TEXT, -- 备份说明
    created_by BIGINT, -- 创建人ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE sys_backup IS '系统备份记录表';
COMMENT ON COLUMN sys_backup.backup_no IS '备份编号';
COMMENT ON COLUMN sys_backup.backup_type IS '备份类型: FULL-全量, INCREMENTAL-增量, DATABASE-数据库, FILE-文件';
COMMENT ON COLUMN sys_backup.backup_name IS '备份名称';
COMMENT ON COLUMN sys_backup.backup_path IS '备份文件路径';
COMMENT ON COLUMN sys_backup.file_size IS '文件大小（字节）';
COMMENT ON COLUMN sys_backup.status IS '状态: PENDING-进行中, SUCCESS-成功, FAILED-失败';
COMMENT ON COLUMN sys_backup.backup_time IS '备份时间';
COMMENT ON COLUMN sys_backup.restore_time IS '恢复时间';
COMMENT ON COLUMN sys_backup.description IS '备份说明';

CREATE INDEX idx_sys_backup_backup_type ON sys_backup(backup_type);
CREATE INDEX idx_sys_backup_status ON sys_backup(status);
CREATE INDEX idx_sys_backup_backup_time ON sys_backup(backup_time);

