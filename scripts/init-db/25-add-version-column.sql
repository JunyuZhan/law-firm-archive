-- =====================================================
-- 律师事务所管理系统 - 添加乐观锁版本列
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-10
-- 描述: 为所有业务表添加 version 列，用于乐观锁并发控制
-- 说明: BaseEntity 基类包含 version 字段，所有继承该基类的实体
--       对应的表都需要有 version 列
-- =====================================================

-- 批量为所有缺少 version 列的表添加该列
DO $$
DECLARE
    tbl_name TEXT;
BEGIN
    FOR tbl_name IN 
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = 'public' 
        AND t.table_type = 'BASE TABLE'
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.columns c 
            WHERE c.table_schema = t.table_schema 
            AND c.table_name = t.table_name 
            AND c.column_name = 'version'
        )
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN version INTEGER DEFAULT 1', tbl_name);
        EXECUTE format('UPDATE %I SET version = 1 WHERE version IS NULL', tbl_name);
        RAISE NOTICE 'Added version column to table: %', tbl_name;
    END LOOP;
END $$;

-- 添加注释
COMMENT ON COLUMN public.sys_user.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_role.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_menu.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_department.version IS '乐观锁版本号';
COMMENT ON COLUMN public.sys_config.version IS '乐观锁版本号';
COMMENT ON COLUMN public.crm_client.version IS '乐观锁版本号';
COMMENT ON COLUMN public.matter.version IS '乐观锁版本号';
COMMENT ON COLUMN public.finance_contract.version IS '乐观锁版本号';
COMMENT ON COLUMN public.finance_payment.version IS '乐观锁版本号';
COMMENT ON COLUMN public.finance_invoice.version IS '乐观锁版本号';
COMMENT ON COLUMN public.workbench_approval.version IS '乐观锁版本号';

