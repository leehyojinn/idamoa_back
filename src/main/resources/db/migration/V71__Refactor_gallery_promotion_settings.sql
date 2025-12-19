-- V71: 갤러리 우대등록 설정 테이블 리팩토링
-- 기존: Singleton 테이블 (하드코딩된 컬럼) → 새: 타입별 row 관리 (확장 가능)

-- 1. 기존 테이블 삭제
DROP TABLE IF EXISTS gallery_promotion_settings;

-- 2. 새 테이블 생성 (BaseTimeEntity 구조: id, uuid, created_at, updated_at)
CREATE TABLE gallery_promotion_type_settings (
    -- BaseTimeEntity 필드
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 비즈니스 필드
    promotion_type VARCHAR(30) NOT NULL UNIQUE,     -- STANDARD, PREMIUM, SUPER_PREMIUM 등
    display_name VARCHAR(50) NOT NULL,              -- 표시명: "일반우대", "강력우대"
    price NUMERIC(12,2) NOT NULL,                   -- 월 가격
    weight INTEGER NOT NULL DEFAULT 1,              -- 가중치 (노출 확률)
    display_order INTEGER NOT NULL DEFAULT 0,       -- 표시 순서
    is_active BOOLEAN NOT NULL DEFAULT TRUE,        -- 활성화 여부
    description VARCHAR(200),                       -- 설명

    -- 관리자 참조
    updated_by BIGINT REFERENCES users(id)
);

-- 3. 초기 데이터 삽입 (기존 STANDARD, PREMIUM)
INSERT INTO gallery_promotion_type_settings
    (promotion_type, display_name, price, weight, display_order, description)
VALUES
    ('STANDARD', '일반우대', 50000, 1, 1, '기본 우대 등록'),
    ('PREMIUM', '강력우대', 100000, 3, 2, '3배 노출 확률');

-- 4. 인덱스 생성
CREATE INDEX idx_promotion_type_settings_active ON gallery_promotion_type_settings(is_active);
CREATE INDEX idx_promotion_type_settings_type ON gallery_promotion_type_settings(promotion_type);
CREATE INDEX idx_promotion_type_settings_order ON gallery_promotion_type_settings(display_order);

-- 5. 코멘트
COMMENT ON TABLE gallery_promotion_type_settings IS '갤러리 우대등록 타입별 설정 (확장 가능)';
COMMENT ON COLUMN gallery_promotion_type_settings.promotion_type IS '우대 타입 코드 (STANDARD, PREMIUM 등)';
COMMENT ON COLUMN gallery_promotion_type_settings.display_name IS 'UI 표시명';
COMMENT ON COLUMN gallery_promotion_type_settings.price IS '월 가격 (원)';
COMMENT ON COLUMN gallery_promotion_type_settings.weight IS '노출 가중치 (랜덤 선택 시 확률 배수)';
COMMENT ON COLUMN gallery_promotion_type_settings.display_order IS 'UI 정렬 순서';
COMMENT ON COLUMN gallery_promotion_type_settings.is_active IS '활성화 여부 (FALSE면 신규 등록 불가)';
