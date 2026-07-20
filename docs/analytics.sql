-- Примеры аналитических запросов:

-- 1. Траты пользователей (сумма и количество оплаченных заказов)
SELECT
    user_id,
    COUNT(*) AS paid_orders_count,
    SUM(price) AS total_spent_geocredits
FROM orders
WHERE status = 'PAID'
GROUP BY user_id
ORDER BY total_spent_geocredits DESC;

-- 2. Популярность типов продуктов (кол-во + выручка)
SELECT
    product_type,
    COUNT(*)    AS cnt,
    SUM(price)  AS revenue
FROM orders
WHERE status = 'PAID'
GROUP BY product_type
ORDER BY revenue DESC;

-- 3. Динамика по месяцам (доход + количество заказов)
SELECT
    DATE_TRUNC('month', created_at) AS month,
    COUNT(*)                        AS orders_count,
    SUM(price)                      AS revenue
FROM orders
WHERE status = 'PAID'
GROUP BY month
ORDER BY month;

-- 4. Воронка статусов — сколько заказов в каждом статусе
SELECT
    status,
    COUNT(*) AS cnt
FROM orders
GROUP BY status
ORDER BY cnt DESC;

-- 5. Причины отказов (REJECTED / PAYMENT_FAILED)
SELECT
    failure_reason,
    COUNT(*) AS cnt
FROM orders
WHERE failure_reason IS NOT NULL
GROUP BY failure_reason
ORDER BY cnt DESC;

-- 6. Новые vs повторные пользователи
SELECT
    CASE
        WHEN rn = 1 THEN 'new'
        ELSE 'returning'
    END AS user_type,
    COUNT(*) AS orders_count
FROM (
    SELECT
        user_id,
        ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at) AS rn
    FROM orders
) sub
GROUP BY user_type;

-- 7. Среднее время от CREATED до PAID (в секундах)
SELECT
    AVG(EXTRACT(EPOCH FROM (updated_at - created_at))) AS avg_seconds_to_paid
FROM orders
WHERE status = 'PAID';

-- 8. Зависшие заказы (PAYMENT_PENDING дольше 5 минут)
SELECT
    id,
    user_id,
    product_type,
    price,
    created_at,
    updated_at
FROM orders
WHERE status = 'PAYMENT_PENDING'
  AND updated_at < NOW() - INTERVAL '5 minutes'
ORDER BY updated_at;

-- 9. Доля выручки по типу продуктов (в процентах)
SELECT
    product_type,
    SUM(price) AS revenue,
    ROUND(SUM(price) * 100.0 / SUM(SUM(price)) OVER (), 2) AS revenue_pct
FROM orders
WHERE status = 'PAID'
GROUP BY product_type
ORDER BY revenue DESC;
