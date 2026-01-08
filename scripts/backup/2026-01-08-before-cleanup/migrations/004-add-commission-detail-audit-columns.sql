-- 迁移脚本：为 finance_commission_detail 表添加审计字段
-- 日期：2026-01-05
-- 说明：修复 BaseEntity 需要的 created_by, updated_by 字段

-- 添加 created_by 字段
ALTER TABLE finance_commission_detail ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- 添加 updated_by 字段
ALTER TABLE finance_commission_detail ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- 添加字段注释
COMMENT ON COLUMN finance_commission_detail.created_by IS '创建人ID';
COMMENT ON COLUMN finance_commission_detail.updated_by IS '更新人ID';

