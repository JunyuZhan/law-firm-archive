-- 创建导出日志表
-- 用于记录司法局报备等数据导出操作
-- Requirements: 6.7

CREATE TABLE IF NOT EXISTS sys_export_log (
    id BIGSERIAL PRIMARY KEY,
    export_type VARCHAR(50) NOT NULL,
    export_params JSONB,
    record_count INT,
    exported_by BIGINT NOT NULL,
    exported_at TIMESTAMP NOT NULL,
    file_name VARCHAR(200),
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_export_user FOREIGN KEY (exported_by) 
        REFERENCES sys_user(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_export_log_type ON sys_export_log(export_type);
CREATE INDEX IF NOT EXISTS idx_export_log_exported_at ON sys_export_log(exported_at);
CREATE INDEX IF NOT EXISTS idx_export_log_exported_by ON sys_export_log(exported_by);

-- 添加注释
COMMENT ON TABLE sys_export_log IS '导出日志表';
COMMENT ON COLUMN sys_export_log.id IS '主键ID';
COMMENT ON COLUMN sys_export_log.export_type IS '导出类型：JUDICIAL_FILING-司法局报备, CONTRACT_LIST-合同列表等';
COMMENT ON COLUMN sys_export_log.export_params IS '导出参数（JSON格式，如年月、筛选条件等）';
COMMENT ON COLUMN sys_export_log.record_count IS '导出记录数';
COMMENT ON COLUMN sys_export_log.exported_by IS '导出人ID';
COMMENT ON COLUMN sys_export_log.exported_at IS '导出时间';
COMMENT ON COLUMN sys_export_log.file_name IS '导出文件名';
COMMENT ON COLUMN sys_export_log.file_size IS '文件大小（字节）';
