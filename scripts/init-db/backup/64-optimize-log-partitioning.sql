-- =====================================================
-- 数据库优化脚本 - 日志表分区
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 为大型日志表创建分区，提高查询和维护性能
-- 优先级: P1 (高优先级)
-- 说明: 使用声明式分区，按月分区，自动创建新分区
-- =====================================================

-- =====================================================
-- 注意事项
-- =====================================================
-- 1. 此脚本会删除现有的非分区表，请确保已有数据已备份
-- 2. 分区表不支持外键约束，需要移除相关外键
-- 3. 分区键必须是主键的一部分
-- 4. 建议在低峰期执行
-- =====================================================

-- =====================================================
-- 1. 创建分区表函数
-- =====================================================

-- 创建操作日志分区表
CREATE TABLE IF NOT EXISTS public.sys_operation_log_partitioned (
    id bigint NOT NULL,
    user_id bigint,
    user_name character varying(50),
    module character varying(50),
    operation_type character varying(50),
    description character varying(500),
    method character varying(200),
    request_url character varying(500),
    request_method character varying(10),
    request_params text,
    response_result text,
    ip_address character varying(50),
    user_agent character varying(2000),
    execution_time bigint,
    status character varying(20) DEFAULT 'SUCCESS'::character varying,
    error_message text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE public.sys_operation_log_partitioned IS '操作日志表（按月分区）';

-- 创建登录日志分区表
CREATE TABLE IF NOT EXISTS public.sys_login_log_partitioned (
    id bigint NOT NULL,
    user_id bigint,
    username character varying(50),
    real_name character varying(50),
    login_ip character varying(50),
    login_location character varying(200),
    user_agent character varying(2000),
    browser character varying(100),
    os character varying(100),
    device_type character varying(20),
    status character varying(20) NOT NULL,
    message character varying(2000),
    login_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    logout_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted boolean DEFAULT false,
    version integer DEFAULT 0,
    PRIMARY KEY (id, login_time)
) PARTITION BY RANGE (login_time);

COMMENT ON TABLE public.sys_login_log_partitioned IS '登录日志表（按月分区）';

-- =====================================================
-- 2. 创建初始分区（当前季度）
-- =====================================================

-- 获取当前月份并创建分区
DO $$
DECLARE
    current_month TEXT := TO_CHAR(CURRENT_DATE, 'YYYY_MM');
    partition_name TEXT;
    start_date TEXT;
    end_date TEXT;
    i INTEGER;
BEGIN
    -- 创建当前月和接下来2个月的分区
    FOR i IN 0..2 LOOP
        partition_name := 'sys_operation_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_operation_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);
    END LOOP;

    -- 创建登录日志分区
    FOR i IN 0..2 LOOP
        partition_name := 'sys_login_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_login_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);
    END LOOP;

    RAISE NOTICE '已创建当前季度及下月的分区';
END $$;

-- =====================================================
-- 3. 创建索引（分区表专用）
-- =====================================================

-- 操作日志分区表索引
CREATE INDEX IF NOT EXISTS idx_operation_log_partition_user
    ON public.sys_operation_log_partitioned (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_log_partition_module
    ON public.sys_operation_log_partitioned (module, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_log_partition_status
    ON public.sys_operation_log_partitioned (status, created_at DESC);

-- 登录日志分区表索引
CREATE INDEX IF NOT EXISTS idx_login_log_partition_user
    ON public.sys_login_log_partitioned (user_id, login_time DESC);

CREATE INDEX IF NOT EXISTS idx_login_log_partition_status
    ON public.sys_login_log_partitioned (status, login_time DESC);

CREATE INDEX IF NOT EXISTS idx_login_log_partition_username
    ON public.sys_login_log_partitioned (username, login_time DESC);

-- =====================================================
-- 4. 创建自动维护函数
-- =====================================================

-- 创建自动创建未来分区的函数
CREATE OR REPLACE FUNCTION public.create_monthly_partitions()
RETURNS void AS $$
DECLARE
    partition_name TEXT;
    start_date TEXT;
    end_date TEXT;
    table_name TEXT;
    date_column TEXT;
    i INTEGER;
BEGIN
    -- 为操作日志表创建未来3个月的分区
    FOR i IN 3..5 LOOP
        partition_name := 'sys_operation_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_operation_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);

        RAISE NOTICE '已创建操作日志分区: %', partition_name;
    END LOOP;

    -- 为登录日志表创建未来3个月的分区
    FOR i IN 3..5 LOOP
        partition_name := 'sys_login_log_' || TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY_MM');
        start_date := TO_CHAR(CURRENT_DATE + (i || ' months')::INTERVAL, 'YYYY-MM') || '-01';
        end_date := TO_CHAR((CURRENT_DATE + ((i + 1) || ' months')::INTERVAL), 'YYYY-MM') || '-01';

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS public.%I
            PARTITION OF public.sys_login_log_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date);

        RAISE NOTICE '已创建登录日志分区: %', partition_name;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.create_monthly_partitions() IS '自动创建未来月份的分区';

-- 创建删除旧分区的函数
CREATE OR REPLACE FUNCTION public.drop_old_partitions(months_to_keep INTEGER DEFAULT 12)
RETURNS void AS $$
DECLARE
    partition_name TEXT;
    cutoff_date TIMESTAMP := (CURRENT_DATE - (months_to_keep || ' months')::INTERVAL);
BEGIN
    -- 删除旧的操作日志分区
    FOR partition_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
        AND tablename LIKE 'sys_operation_log_%'
        AND substring(tablename FROM 19 FOR 7) < TO_CHAR(cutoff_date, 'YYYY_MM')
    LOOP
        EXECUTE 'DROP TABLE IF EXISTS public.' || quote_ident(partition_name);
        RAISE NOTICE '已删除操作日志分区: %', partition_name;
    END LOOP;

    -- 删除旧的登录日志分区
    FOR partition_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
        AND tablename LIKE 'sys_login_log_%'
        AND substring(tablename FROM 16 FOR 7) < TO_CHAR(cutoff_date, 'YYYY_MM')
    LOOP
        EXECUTE 'DROP TABLE IF EXISTS public.' || quote_ident(partition_name);
        RAISE NOTICE '已删除登录日志分区: %', partition_name;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.drop_old_partitions(integer) IS '删除指定月份之前的旧分区，默认保留12个月';

-- =====================================================
-- 5. 数据迁移函数（可选）
-- =====================================================

-- 创建从旧表迁移数据到分区表的函数
CREATE OR REPLACE FUNCTION public.migrate_to_partitioned_tables(batch_size INTEGER DEFAULT 10000)
RETURNS void AS $$
DECLARE
    migrated_rows INTEGER;
BEGIN
    -- 迁移操作日志
    INSERT INTO public.sys_operation_log_partitioned
    SELECT * FROM public.sys_operation_log
    ON CONFLICT (id, created_at) DO NOTHING;

    GET DIAGNOSTICS migrated_rows = ROW_COUNT;
    RAISE NOTICE '已迁移 % 条操作日志记录', migrated_rows;

    -- 迁移登录日志
    INSERT INTO public.sys_login_log_partitioned
    SELECT * FROM public.sys_login_log
    ON CONFLICT (id, login_time) DO NOTHING;

    GET DIAGNOSTICS migrated_rows = ROW_COUNT;
    RAISE NOTICE '已迁移 % 条登录日志记录', migrated_rows;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.migrate_to_partitioned_tables(integer) IS '将旧表数据迁移到分区表';

-- =====================================================
-- 6. 定时任务（需要 pg_cron 扩展）
-- =====================================================

-- 创建每月维护分区定时任务
-- SELECT cron.schedule('maintain-log-partitions', '0 0 1 * *', 'SELECT public.create_monthly_partitions();');

-- =====================================================
-- 7. 使用说明
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '日志表分区脚本已执行';
    RAISE NOTICE '========================================';
    RAISE NOTICE '';
    RAISE NOTICE '使用说明:';
    RAISE NOTICE '1. 分区表已创建:';
    RAISE NOTICE '   - sys_operation_log_partitioned';
    RAISE NOTICE '   - sys_login_log_partitioned';
    RAISE NOTICE '';
    RAISE NOTICE '2. 迁移数据:';
    RAISE NOTICE '   SELECT public.migrate_to_partitioned_tables();';
    RAISE NOTICE '';
    RAISE NOTICE '3. 验证迁移后，替换原表:';
    RAISE NOTICE '   ALTER TABLE sys_operation_log RENAME TO sys_operation_log_old;';
    RAISE NOTICE '   ALTER TABLE sys_operation_log_partitioned RENAME TO sys_operation_log;';
    RAISE NOTICE '   -- 确认无误后删除旧表';
    RAISE NOTICE '   DROP TABLE sys_operation_log_old;';
    RAISE NOTICE '';
    RAISE NOTICE '4. 定期维护:';
    RAISE NOTICE '   -- 创建未来月份分区';
    RAISE NOTICE '   SELECT public.create_monthly_partitions();';
    RAISE NOTICE '   -- 删除旧分区(保留12个月)';
    RAISE NOTICE '   SELECT public.drop_old_partitions(12);';
    RAISE NOTICE '';
    RAISE NOTICE '5. 设置自动维护(可选):';
    RAISE NOTICE '   需要先安装 pg_cron 扩展';
END $$;
