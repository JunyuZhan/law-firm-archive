-- 说明：此脚本用于记录需要重新计算提成的 Payment 记录
-- 实际的提成计算需要通过 API 调用：POST /finance/commission/calculate/{paymentId}
-- 
-- 执行方式: 
-- 1. 先执行此脚本查看需要重新计算的记录
-- 2. 然后通过 API 或前端界面触发重新计算

-- 查看已确认但没有提成记录的 Payment
SELECT 
    p.id AS payment_id,
    p.payment_no,
    p.matter_id,
    p.amount,
    p.status,
    m.name AS matter_name,
    '需要调用 API: POST /finance/commission/calculate/' || p.id AS action
FROM finance_payment p
LEFT JOIN matter m ON p.matter_id = m.id
LEFT JOIN finance_commission c ON c.payment_id = p.id
WHERE p.status = 'CONFIRMED'
  AND p.deleted = false
  AND c.id IS NULL
  AND p.matter_id IS NOT NULL
ORDER BY p.id;

-- 提示信息
SELECT '请使用以下 curl 命令重新计算提成（需要先登录获取 token）:' AS info;
SELECT 'curl -X POST http://localhost:8080/api/finance/commission/calculate/' || p.id || ' -H "Authorization: Bearer <token>"' AS command
FROM finance_payment p
LEFT JOIN finance_commission c ON c.payment_id = p.id
WHERE p.status = 'CONFIRMED'
  AND p.deleted = false
  AND c.id IS NULL
  AND p.matter_id IS NOT NULL
ORDER BY p.id;
