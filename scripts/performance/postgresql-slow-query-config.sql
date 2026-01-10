-- =====================================================
-- PostgreSQL 慢查询日志配置
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-10
-- 描述: 开启慢查询日志，用于性能分析
-- 作者: Kiro-1
-- =====================================================

-- 注意：以下配置需要数据库管理员权限执行
-- 修改后需要重启 PostgreSQL 或 reload 配置

-- =====================================================
-- 1. 查看当前配置
-- =====================================================

-- 查看当前慢查询相关配置
SELECT name, setting, unit, short_desc 
FROM pg_settings 
WHERE name IN (
    'log_min_duration_statement',
    'log_statement',
    'log_duration',
    'log_line_prefix',
    'log_checkpoints',
    'log_connections',
    'log_disconnections',
    'log_lock_waits'
);

-- =====================================================
-- 2. 配置建议（postgresql.conf）
-- =====================================================

-- 以下配置需要添加到 postgresql.conf 文件中：

/*
# 慢查询日志配置
log_min_duration_statement = 1000       # 记录超过 1 秒的查询（单位：毫秒）
log_statement = 'none'                   # 不记录所有 SQL（避免日志过大）
log_duration = off                       # 不记录所有查询耗时
log_checkpoints = on                     # 记录检查点
log_connections = on                     # 记录连接
log_disconnections = on                  # 记录断开
log_lock_waits = on                      # 记录锁等待

# 日志格式
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_timezone = 'Asia/Shanghai'

# 日志文件配置
logging_collector = on
log_directory = 'pg_log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d                    # 每天轮转
log_rotation_size = 100MB                # 或达到 100MB 轮转
*/

-- =====================================================
-- 3. 实时配置（不需要重启）
-- =====================================================

-- 临时开启慢查询日志（当前会话有效）
-- SET log_min_duration_statement = 1000;

-- 超级用户可以修改全局配置（需要 reload）
-- ALTER SYSTEM SET log_min_duration_statement = 1000;
-- SELECT pg_reload_conf();

-- =====================================================
-- 4. 慢查询分析 SQL
-- =====================================================

-- 分析慢查询（如果启用了 pg_stat_statements 扩展）
-- 需要先创建扩展：CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- 查看最耗时的 TOP 20 SQL
/*
SELECT 
    round(total_exec_time::numeric, 2) AS total_time_ms,
    calls,
    round(mean_exec_time::numeric, 2) AS avg_time_ms,
    round(max_exec_time::numeric, 2) AS max_time_ms,
    rows,
    query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
*/

-- 查看平均执行时间最长的 SQL
/*
SELECT 
    round(mean_exec_time::numeric, 2) AS avg_time_ms,
    calls,
    round(total_exec_time::numeric, 2) AS total_time_ms,
    query
FROM pg_stat_statements
WHERE calls > 10
ORDER BY mean_exec_time DESC
LIMIT 20;
*/

-- 查看调用次数最多的 SQL
/*
SELECT 
    calls,
    round(total_exec_time::numeric, 2) AS total_time_ms,
    round(mean_exec_time::numeric, 2) AS avg_time_ms,
    query
FROM pg_stat_statements
ORDER BY calls DESC
LIMIT 20;
*/

-- =====================================================
-- 5. 索引使用分析
-- =====================================================

-- 未使用的索引
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- 表扫描统计（发现缺少索引的表）
SELECT 
    schemaname,
    relname AS tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    n_tup_ins,
    n_tup_upd,
    n_tup_del
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_scan DESC
LIMIT 20;

-- =====================================================
-- 6. 锁等待分析
-- =====================================================

-- 查看当前锁等待
SELECT 
    blocked_locks.pid AS blocked_pid,
    blocked_activity.usename AS blocked_user,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query AS blocked_statement,
    blocking_activity.query AS current_statement_in_blocking_process
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity 
    ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks 
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity 
    ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- =====================================================
-- 7. 定期维护建议
-- =====================================================

-- 重置统计信息（慎用）
-- SELECT pg_stat_reset();

-- 分析所有表（更新统计信息）
-- ANALYZE;

-- 清理死元组
-- VACUUM ANALYZE;

