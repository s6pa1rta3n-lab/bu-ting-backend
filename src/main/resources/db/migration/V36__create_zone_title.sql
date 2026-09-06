CREATE TABLE zone_title_def (
    title_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id VARCHAR(30) NOT NULL,
    tier INTEGER NOT NULL,
    required_success_count INTEGER NOT NULL,
    title_code VARCHAR(100) NOT NULL UNIQUE,
    title_name VARCHAR(100) NOT NULL,
    style VARCHAR(50),
    color VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_zone_title_zone CHECK (zone_id IN (
        'HAEUNDAE_GIJANG', 'SUYEONG_NAMGU', 'CENTRAL_NORTH',
        'OLD_DOWNTOWN', 'YEONGDO', 'WESTERN_BUSAN')),
    CONSTRAINT ck_zone_title_tier CHECK (tier IN (1, 2, 3))
);

CREATE INDEX idx_zone_title_def_zone_tier ON zone_title_def (zone_id, tier);

INSERT INTO zone_title_def (zone_id, tier, required_success_count, title_code, title_name, style, color) VALUES
('HAEUNDAE_GIJANG', 1, 1, 'TITLE_HAEUNDAE_1', '해운대 비기너', 'DEFAULT', '#3B82F6'),
('HAEUNDAE_GIJANG', 2, 5, 'TITLE_HAEUNDAE_2', '기장 파도잡이', 'SILVER', '#06B6D4'),
('HAEUNDAE_GIJANG', 3, 10, 'TITLE_HAEUNDAE_3', '동부산의 제왕', 'GOLD', '#F59E0B'),
('SUYEONG_NAMGU', 1, 1, 'TITLE_SUYEONG_1', '광안리 비기너', 'DEFAULT', '#3B82F6'),
('SUYEONG_NAMGU', 2, 5, 'TITLE_SUYEONG_2', '수영 남구 마스터', 'SILVER', '#06B6D4'),
('SUYEONG_NAMGU', 3, 10, 'TITLE_SUYEONG_3', '남구 오륙도 수호자', 'GOLD', '#F59E0B'),
('CENTRAL_NORTH', 1, 1, 'TITLE_CENTRAL_1', '서면 비기너', 'DEFAULT', '#3B82F6'),
('CENTRAL_NORTH', 2, 5, 'TITLE_CENTRAL_2', '동래 탐험가', 'SILVER', '#06B6D4'),
('CENTRAL_NORTH', 3, 10, 'TITLE_CENTRAL_3', '중북부 지존', 'GOLD', '#F59E0B'),
('OLD_DOWNTOWN', 1, 1, 'TITLE_DOWNTOWN_1', '원도심 비기너', 'DEFAULT', '#3B82F6'),
('OLD_DOWNTOWN', 2, 5, 'TITLE_DOWNTOWN_2', '자갈치 베테랑', 'SILVER', '#06B6D4'),
('OLD_DOWNTOWN', 3, 10, 'TITLE_DOWNTOWN_3', '원도심의 전설', 'GOLD', '#F59E0B'),
('YEONGDO', 1, 1, 'TITLE_YEONGDO_1', '영도 비기너', 'DEFAULT', '#3B82F6'),
('YEONGDO', 2, 5, 'TITLE_YEONGDO_2', '태종대 순례자', 'SILVER', '#06B6D4'),
('YEONGDO', 3, 10, 'TITLE_YEONGDO_3', '절영의 주인', 'GOLD', '#F59E0B'),
('WESTERN_BUSAN', 1, 1, 'TITLE_WESTERN_1', '서부산 비기너', 'DEFAULT', '#3B82F6'),
('WESTERN_BUSAN', 2, 5, 'TITLE_WESTERN_2', '낙동강 탐험가', 'SILVER', '#06B6D4'),
('WESTERN_BUSAN', 3, 10, 'TITLE_WESTERN_3', '서부산의 지배자', 'GOLD', '#F59E0B');

CREATE TABLE user_zone_title (
    user_title_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title_id UUID NOT NULL REFERENCES zone_title_def(title_id),
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE,
    earned_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_zone_title UNIQUE (user_id, title_id)
);

CREATE UNIQUE INDEX uk_user_zone_title_equipped ON user_zone_title (user_id) WHERE is_equipped = TRUE;
CREATE INDEX idx_user_zone_title_user ON user_zone_title (user_id);
