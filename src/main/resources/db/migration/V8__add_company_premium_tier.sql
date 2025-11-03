-- V8: Company 테이블에 프리미엄 등급 및 누락된 필드 추가
-- 프리미엄 업체 우선 노출 및 Entity와 DB 스키마 일치

-- 1. 누락된 기본 필드 추가
ALTER TABLE companies
ADD COLUMN IF NOT EXISTS slug VARCHAR(200),
ADD COLUMN IF NOT EXISTS address VARCHAR(500),
ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 7),
ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 7),
ADD COLUMN IF NOT EXISTS view_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS like_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
ADD COLUMN IF NOT EXISTS featured BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE NOT NULL;

-- 2. 프리미엄 등급 컬럼 추가
ALTER TABLE companies
ADD COLUMN IF NOT EXISTS premium_tier VARCHAR(20) DEFAULT 'NONE' NOT NULL,
ADD COLUMN IF NOT EXISTS premium_monthly_amount DECIMAL(10, 2) DEFAULT 0 NOT NULL;

-- 3. slug unique 제약조건 추가
CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_slug ON companies(slug) WHERE slug IS NOT NULL;

-- 4. 인덱스 추가 (프리미엄 등급으로 정렬 조회 최적화)
CREATE INDEX IF NOT EXISTS idx_companies_premium_tier ON companies(premium_tier);

-- 5. 인덱스 추가 (월정액 내림차순으로 정렬 조회 최적화)
CREATE INDEX IF NOT EXISTS idx_companies_premium_monthly_amount ON companies(premium_monthly_amount DESC);

-- 6. 인덱스 추가 (상태별 조회 최적화)
CREATE INDEX IF NOT EXISTS idx_companies_status ON companies(status);

-- 7. 복합 인덱스 추가 (프리미엄 업체 + 활성 상태 + 삭제 여부)
CREATE INDEX IF NOT EXISTS idx_companies_premium_active ON companies(premium_tier, status, is_deleted)
WHERE status = 'ACTIVE' AND is_deleted = false;

-- 8. 인덱스 추가 (좋아요 수 정렬)
CREATE INDEX IF NOT EXISTS idx_companies_like_count ON companies(like_count DESC);

-- 9. 인덱스 추가 (조회수 정렬)
CREATE INDEX IF NOT EXISTS idx_companies_view_count ON companies(view_count DESC);

-- 10. 컬럼 코멘트 추가
COMMENT ON COLUMN companies.slug IS 'SEO 친화적인 URL 식별자';
COMMENT ON COLUMN companies.address IS '업체 주소';
COMMENT ON COLUMN companies.postal_code IS '우편번호';
COMMENT ON COLUMN companies.latitude IS '위도';
COMMENT ON COLUMN companies.longitude IS '경도';
COMMENT ON COLUMN companies.view_count IS '조회수';
COMMENT ON COLUMN companies.like_count IS '좋아요 수';
COMMENT ON COLUMN companies.status IS '업체 상태 (ACTIVE, INACTIVE, SUSPENDED, PENDING)';
COMMENT ON COLUMN companies.featured IS '추천 업체 여부';
COMMENT ON COLUMN companies.verified IS '인증 업체 여부';
COMMENT ON COLUMN companies.premium_tier IS '프리미엄 등급 (NONE, BASIC, STANDARD, PREMIUM, VIP)';
COMMENT ON COLUMN companies.premium_monthly_amount IS '월정액 금액 (원)';
