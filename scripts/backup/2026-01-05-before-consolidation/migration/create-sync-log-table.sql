-- 创建数据同步日志表
-- 用于记录合同审批后数据同步到各模块的日志
-- Requirements: 1.5

CREATE TABLE IF NOT EXISTS sys_data_sync_log (
    id BIGSERIAL PRIMARY KEY,
    source_table VARCHAR(100) NOT NULL,
    source_id BIGINT NOT NULL,
    target_module VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    sync_data JSONB,
    sync_status VARCHAR(20) NOT NULL,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    synced_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sync_log_source ON sys_data_sync_log(source_table, source_id);
CREATE INDEX IF NOT EXISTS idx_sync_log_target_module ON sys_data_sync_log(target_module);
CREATE INDEX IF NOT EXISTS idx_sync_log_status ON sys_data_sync_log(sync_status);
CREATE INDEX IF NOT EXISTS idx_sync_log_synced_at ON sys_data_sync_log(synced_at);

-- 添加注释
COMMENT ON TABLE sys_data_sync_log IS '数据同步日志表';
COMMENT ON COLUMN sys_data_sync_log.id IS '主键ID';
COMMENT ON COLUMN sys_data_sync_log.source_table IS '源表名';
COMMENT ON COLUMN sys_data_sync_log.source_id IS '源记录ID';
COMMENT ON COLUMN sys_data_sync_log.target_module IS '目标模块：FINANCE-财务模块, ADMIN-行政模块';
COMMENT ON COLUMN sys_data_sync_log.operation_type IS '操作类型：CREATE-创建, UPDATE-更新, DELETE-删除';
COMMENT ON COLUMN sys_data_sync_log.sync_data IS '同步的数据内容（JSON格式）';
COMMENT ON COLUMN sys_data_sync_log.sync_status IS '同步状态：SUCCESS-成功, FAILED-失败, PENDING-待处理';
COMMENT ON COLUMN sys_data_sync_log.error_message IS '错误信息';
COMMENT ON COLUMN sys_data_sync_log.retry_count IS '重试次数';
COMMENT ON COLUMN sys_data_sync_log.synced_at IS '同步时间';
