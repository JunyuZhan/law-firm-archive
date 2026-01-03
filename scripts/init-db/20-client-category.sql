-- 为crm_client表添加客户分类字段
ALTER TABLE crm_client 
ADD COLUMN IF NOT EXISTS category VARCHAR(20) DEFAULT 'NORMAL';

COMMENT ON COLUMN crm_client.category IS '客户分类: VIP-重要客户, NORMAL-普通客户, POTENTIAL-潜在客户';

CREATE INDEX IF NOT EXISTS idx_crm_client_category ON crm_client(category);

