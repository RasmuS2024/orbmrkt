-- Analytics SQL: Payment Service Statistics
-- Сумма и количество оплаченных заказов по каждому user_id

SELECT
    user_id,
    COUNT(*) AS paid_orders_count,
    SUM(price) AS total_spent_geocredits
FROM orders
WHERE status = 'PAID'
GROUP BY user_id
ORDER BY total_spent_geocredits DESC;
