CREATE TABLE user_city_grade_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    grade VARCHAR(30) NOT NULL,
    reached_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_user_city_grade CHECK (grade IN ('BEGINNER', 'EXPLORER', 'MASTER', 'TRUE_BUSAN'))
);

CREATE INDEX idx_user_city_grade_history_user ON user_city_grade_history (user_id, reached_at);
