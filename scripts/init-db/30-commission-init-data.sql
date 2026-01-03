-- ============================================
-- 提成规则初始数据
-- ============================================

INSERT INTO finance_commission_rule (rule_code, rule_name, firm_retention_rate, originator_rate, tax_rate, management_fee_rate, rate_tiers, is_default, active) VALUES
('DEFAULT', '默认提成规则', 0.30, 0.20, 0.0672, 0.15, 
'[{"minAmount": 0, "maxAmount": 100000, "rate": 0.30},
  {"minAmount": 100000, "maxAmount": 500000, "rate": 0.35},
  {"minAmount": 500000, "maxAmount": 1000000, "rate": 0.40},
  {"minAmount": 1000000, "maxAmount": null, "rate": 0.45}]'::jsonb,
TRUE, TRUE)
ON CONFLICT (rule_code) DO NOTHING;

