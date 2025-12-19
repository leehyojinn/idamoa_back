-- =====================================================
-- V70: Gallery Promotion Settings 테이블 생성
-- 관리자가 우대등록 가격/가중치를 동적으로 관리
-- =====================================================

-- 설정 테이블 (Singleton 패턴 - 단일 레코드)
CREATE TABLE gallery_promotion_settings (
    id BIGSERIAL PRIMARY KEY,
    standard_price NUMERIC(12,2) NOT NULL DEFAULT 50000,
    premium_price NUMERIC(12,2) NOT NULL DEFAULT 100000,
    standard_weight INTEGER NOT NULL DEFAULT 1,
    premium_weight INTEGER NOT NULL DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id)
);

-- 초기 설정값 삽입
INSERT INTO gallery_promotion_settings (standard_price, premium_price, standard_weight, premium_weight)
VALUES (50000, 100000, 1, 3);

-- 단일 레코드만 허용하는 제약조건 (id=1만 가능)
ALTER TABLE gallery_promotion_settings ADD CONSTRAINT chk_singleton CHECK (id = 1);

COMMENT ON TABLE gallery_promotion_settings IS '갤러리 우대등록 설정 (Singleton)';
COMMENT ON COLUMN gallery_promotion_settings.standard_price IS '일반우대 월 가격 (원)';
COMMENT ON COLUMN gallery_promotion_settings.premium_price IS '강력우대 월 가격 (원)';
COMMENT ON COLUMN gallery_promotion_settings.standard_weight IS '일반우대 가중치';
COMMENT ON COLUMN gallery_promotion_settings.premium_weight IS '강력우대 가중치';
