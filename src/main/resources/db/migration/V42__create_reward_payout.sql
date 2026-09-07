-- Reward payout management for BASE and TOP_LIKE rewards (Issue #244).

CREATE TABLE reward_payout (
    payout_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id UUID REFERENCES zone_event_round(round_id),
    event_id UUID REFERENCES zone_event(event_id),
    participation_id UUID REFERENCES zone_event_participation(participation_id),
    user_id UUID NOT NULL REFERENCES users(id),
    reward_reason VARCHAR(30) NOT NULL,
    rank_n INTEGER,
    like_count_at_close INTEGER,
    status VARCHAR(30) NOT NULL,
    hold_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
    reward_id UUID REFERENCES reward_catalog(reward_id),
    points INTEGER,
    badge_code VARCHAR(100),
    prize_name VARCHAR(255),
    scheduled_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    confirmed_by UUID,
    mailed_at TIMESTAMPTZ,
    information_collected_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    failure_code VARCHAR(100),
    retry_count INTEGER NOT NULL DEFAULT 0,
    note TEXT,
    reference VARCHAR(255),
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reward_payout_participation_reason UNIQUE (participation_id, reward_reason)
);

CREATE INDEX idx_reward_payout_filters ON reward_payout (round_id, event_id, reward_reason, status, hold_status);
CREATE INDEX idx_reward_payout_user ON reward_payout (user_id);
CREATE INDEX idx_reward_payout_scheduled ON reward_payout (scheduled_at);

CREATE TABLE reward_payout_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payout_id UUID NOT NULL REFERENCES reward_payout(payout_id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id UUID,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reward_payout_history_payout ON reward_payout_history (payout_id, created_at DESC);
