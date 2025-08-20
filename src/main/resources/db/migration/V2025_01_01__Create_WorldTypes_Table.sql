-- 세계관 정보를 저장하는 테이블 생성
CREATE TABLE world_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    game_settings VARCHAR(1000) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_world_types_code ON world_types(code);
CREATE INDEX idx_world_types_active_sort ON world_types(is_active, sort_order);

-- 기본 데이터 삽입 (기존 enum 데이터)
INSERT INTO world_types (code, display_name, description, game_settings, is_active, sort_order) VALUES
('FANTASY', '판타지', '중세 판타지 - 마법과 모험의 세계', '중세 판타지 세계관에서 펼쳐지는 마법과 모험의 이야기', true, 1),
('SF', 'SF', '미래 SF - 과학기술과 우주탐험', '미래 우주 세계관에서 펼쳐지는 과학기술과 탐험의 이야기', true, 2),
('MODERN', '현대', '현대 도시 - 일상과 미스터리', '현대 도시 세계관에서 펼쳐지는 일상과 미스터리의 이야기', true, 3);

-- updated_at 자동 업데이트를 위한 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- updated_at 자동 업데이트 트리거
CREATE TRIGGER update_world_types_updated_at 
    BEFORE UPDATE ON world_types 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();