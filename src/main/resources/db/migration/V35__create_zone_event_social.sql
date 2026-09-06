CREATE TABLE zone_event_like (
    like_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participation_id UUID NOT NULL REFERENCES zone_event_participation(participation_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_zone_event_like UNIQUE (participation_id, user_id)
);

CREATE INDEX idx_zone_event_like_participation ON zone_event_like (participation_id);
CREATE INDEX idx_zone_event_like_user ON zone_event_like (user_id);

CREATE TABLE zone_event_comment (
    comment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participation_id UUID NOT NULL REFERENCES zone_event_participation(participation_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_zone_event_comment_participation ON zone_event_comment (participation_id, created_at);

CREATE TABLE zone_event_report (
    report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participation_id UUID NOT NULL REFERENCES zone_event_participation(participation_id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES users(id),
    reason_code VARCHAR(50) NOT NULL,
    reason_detail TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_zone_event_report UNIQUE (participation_id, reporter_id),
    CONSTRAINT ck_zone_event_report_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_zone_event_report_participation_status ON zone_event_report (participation_id, status);
