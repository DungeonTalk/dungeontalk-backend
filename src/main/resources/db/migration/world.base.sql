-- 월드(맵, 스테이지 등) 정보를 저장하는 테이블
CREATE TABLE world (
                       world_id INTEGER PRIMARY KEY,
                       world_name VARCHAR(30) NOT NULL,
                       clear_exp INTEGER NOT NULL
);

-- 레벨업에 필요한 경험치를 저장하는 테이블
CREATE TABLE request_exp (
                             level INTEGER PRIMARY KEY,
                             request_total_exp BIGINT NOT NULL, -- 해당 레벨이 되기 위한 총 필요 경험치
                             request_next_level_exp INTEGER NOT NULL -- 다음 레벨로 가기 위한 필요 경험치
);


-----


INSERT INTO request_exp (level, request_total_exp, request_next_level_exp) VALUES
                                                                               (1, 0, 600),
                                                                               (2, 600, 600),
                                                                               (3, 1200, 600),
                                                                               (4, 1800, 600),
                                                                               (5, 2400, 600),
                                                                               (6, 3000, 800),
                                                                               (7, 3800, 800),
                                                                               (8, 4600, 800),
                                                                               (9, 5400, 800),
                                                                               (10, 6200, 800),
                                                                               (11, 7000, 1200),
                                                                               (12, 8200, 1200),
                                                                               (13, 9400, 1200),
                                                                               (14, 10600, 1200),
                                                                               (15, 11800, 1200),
                                                                               (16, 13000, 1500),
                                                                               (17, 14500, 1500),
                                                                               (18, 16000, 1500),
                                                                               (19, 17500, 1500),
                                                                               (20, 19000, 1500),
                                                                               (21, 20500, 1800),
                                                                               (22, 22300, 1800),
                                                                               (23, 24100, 1800),
                                                                               (24, 25900, 1800),
                                                                               (25, 27700, 1800),
                                                                               (26, 29500, 2100),
                                                                               (27, 31600, 2100),
                                                                               (28, 33700, 2100),
                                                                               (29, 35800, 2100),
                                                                               (30, 37900, 0)
    ON CONFLICT (level) DO UPDATE
                               SET request_total_exp = EXCLUDED.request_total_exp,
                               request_next_level_exp = EXCLUDED.request_next_level_exp;



INSERT INTO world (world_id, world_name, clear_exp) VALUES (1, '잊혀진 별의 마지막 노래', 200);
INSERT INTO world (world_id, world_name, clear_exp) VALUES (2, '좀비 아포칼립스', 300);



-- race_stats 테스트 데이터 (풀네임 컬럼 사용)
-- id는 애플리케이션(JPA)에서는 UuidV7Creator.create()로 자동 생성되지만,
-- 직접 SQL로 넣을 때는 아래처럼 명시적으로 지정해야 합니다.

INSERT INTO race_stats (
    id, race,
    health_points, mana_points,
    physical_attack, magic_attack,
    evasion_rate, accuracy, dice_odds
) VALUES
-- 엘프
(
    '018fb1a0-7e2b-7f6a-a1c3-5d2a1e9f3b10',
    '엘프',
    '100 + (willpower * 10)',
    '150 + (wisdom * 12)',
    'strength * 1.2',
    'intelligence * 1.8',
    'dexterity * 1.5',
    '50 + (dexterity * 0.8)',
    'luck * 0.15'
),
-- 인간
(
    '018fb1a0-7e2b-7f6a-a1c3-5d2a1e9f3b11',
    '인간',
    '120 + (willpower * 8)',
    '100 + (wisdom * 10)',
    'strength * 1.5',
    'intelligence * 1.3',
    'dexterity * 1.0',
    '60 + (dexterity * 1.0)',
    'luck * 0.12'
),
-- 드워프
(
    '018fb1a0-7e2b-7f6a-a1c3-5d2a1e9f3b12',
    '드워프',
    '150 + (willpower * 12)',
    '80 + (wisdom * 8)',
    'strength * 1.8',
    'intelligence * 1.0',
    'dexterity * 0.8',
    '70 + (dexterity * 0.6)',
    'luck * 0.10'
),
-- 오크
(
    '018fb1a0-7e2b-7f6a-a1c3-5d2a1e9f3b13',
    '오크',
    '180 + (willpower * 15)',
    '60 + (wisdom * 6)',
    'strength * 2.0',
    'intelligence * 0.8',
    'dexterity * 0.7',
    '55 + (dexterity * 0.5)',
    'luck * 0.08'
);
