INSERT INTO reward_catalog (reward_type, code, name, point_amount, active) VALUES
    ('POINT', 'POINT_BASE', '기본 포인트', 50, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO reward_catalog (reward_type, code, name, active) VALUES
    ('BADGE', 'SPOT_GWANGAN_BRIDGE', '광안대교 스팟', TRUE)
ON CONFLICT (code) DO NOTHING;
