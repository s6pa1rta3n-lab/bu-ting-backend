CREATE TABLE user_coupon (
    coupon_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    reward_id UUID NOT NULL REFERENCES reward_catalog(reward_id),
    grant_id UUID NOT NULL REFERENCES reward_grant(grant_id),
    coupon_code VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_user_coupon_status CHECK (status IN ('ISSUED', 'USED', 'EXPIRED')),
    CONSTRAINT uk_user_coupon_code UNIQUE (coupon_code),
    CONSTRAINT uk_user_coupon_grant UNIQUE (grant_id)
);

CREATE INDEX idx_user_coupon_user_status ON user_coupon (user_id, status, issued_at DESC);
