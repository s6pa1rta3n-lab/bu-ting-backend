CREATE TABLE zone_event_settlement_report (
    report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id UUID NOT NULL REFERENCES zone_event_round(round_id) ON DELETE CASCADE,
    summary_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_zone_event_settlement_report_round UNIQUE (round_id)
);

CREATE TABLE zone_event_audit_log (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operator_id UUID,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_zone_event_audit_log_target ON zone_event_audit_log (target_type, target_id);
CREATE INDEX idx_zone_event_audit_log_created ON zone_event_audit_log (created_at DESC);
