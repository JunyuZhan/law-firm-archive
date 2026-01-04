-- ============================================
-- 数据库迁移脚本：添加 currency 和 updated_by 字段
-- 版本: 1.0.2
-- 创建日期: 2026-01-04
-- ============================================

-- 为 finance_payment 表添加 currency 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'finance_payment' AND column_name = 'currency'
    ) THEN
        ALTER TABLE finance_payment ADD COLUMN currency VARCHAR(10) DEFAULT 'CNY';
        RAISE NOTICE '已为 finance_payment 表添加 currency 字段';
    ELSE
        RAISE NOTICE 'finance_payment 表已存在 currency 字段，跳过';
    END IF;
END $$;

-- 为 evidence 表添加 updated_by 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'evidence' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE evidence ADD COLUMN updated_by BIGINT;
        RAISE NOTICE '已为 evidence 表添加 updated_by 字段';
    ELSE
        RAISE NOTICE 'evidence 表已存在 updated_by 字段，跳过';
    END IF;
END $$;

-- 为 finance_fee 表添加 currency 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'finance_fee' AND column_name = 'currency'
    ) THEN
        ALTER TABLE finance_fee ADD COLUMN currency VARCHAR(10) DEFAULT 'CNY';
        RAISE NOTICE '已为 finance_fee 表添加 currency 字段';
    ELSE
        RAISE NOTICE 'finance_fee 表已存在 currency 字段，跳过';
    END IF;
END $$;

-- 为 timesheet 表添加 updated_by 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'timesheet' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE timesheet ADD COLUMN updated_by BIGINT;
        RAISE NOTICE '已为 timesheet 表添加 updated_by 字段';
    ELSE
        RAISE NOTICE 'timesheet 表已存在 updated_by 字段，跳过';
    END IF;
END $$;

-- 完成提示
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '迁移完成：currency 和 updated_by 字段已添加';
    RAISE NOTICE '========================================';
END $$;

