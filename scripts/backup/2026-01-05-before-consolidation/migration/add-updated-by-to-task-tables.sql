-- ============================================
-- 数据库迁移脚本：为任务相关表添加 updated_by 字段
-- 版本: 1.0.1
-- 创建日期: 2026-01-04
-- ============================================

-- 为 task 表添加 updated_by 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'task' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE task ADD COLUMN updated_by BIGINT;
        RAISE NOTICE '已为 task 表添加 updated_by 字段';
    ELSE
        RAISE NOTICE 'task 表已存在 updated_by 字段，跳过';
    END IF;
END $$;

-- 为 task_comment 表添加 updated_by, updated_at, deleted 字段
DO $$
BEGIN
    -- 添加 updated_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'task_comment' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE task_comment ADD COLUMN updated_by BIGINT;
        RAISE NOTICE '已为 task_comment 表添加 updated_by 字段';
    ELSE
        RAISE NOTICE 'task_comment 表已存在 updated_by 字段，跳过';
    END IF;

    -- 添加 updated_at
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'task_comment' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE task_comment ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE '已为 task_comment 表添加 updated_at 字段';
    ELSE
        RAISE NOTICE 'task_comment 表已存在 updated_at 字段，跳过';
    END IF;

    -- 添加 deleted
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'task_comment' AND column_name = 'deleted'
    ) THEN
        ALTER TABLE task_comment ADD COLUMN deleted BOOLEAN DEFAULT FALSE;
        RAISE NOTICE '已为 task_comment 表添加 deleted 字段';
    ELSE
        RAISE NOTICE 'task_comment 表已存在 deleted 字段，跳过';
    END IF;
END $$;

-- 为 schedule 表添加 updated_by 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'schedule' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE schedule ADD COLUMN updated_by BIGINT;
        RAISE NOTICE '已为 schedule 表添加 updated_by 字段';
    ELSE
        RAISE NOTICE 'schedule 表已存在 updated_by 字段，跳过';
    END IF;
END $$;

-- 完成提示
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '迁移完成：任务相关表的 updated_by 字段已添加';
    RAISE NOTICE '========================================';
END $$;

