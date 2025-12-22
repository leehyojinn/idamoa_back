-- =====================================================
-- V72: Gallery → Company Portfolio 마이그레이션
-- =====================================================
-- 갤러리 보드 기능을 업체 포트폴리오로 완전 이관
-- - company_portfolios 테이블 확장
-- - 프로모션 시스템 신규 생성
-- - 필터/북마크/좋아요 테이블 신규 생성
-- - 기존 gallery 테이블 삭제
-- =====================================================

-- =====================================================
-- 1. company_portfolios 테이블 확장
-- =====================================================

-- 갤러리 기능을 위한 새 컬럼 추가
ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS content TEXT;
COMMENT ON COLUMN company_portfolios.content IS '포트폴리오 본문 내용 (HTML/마크다운)';

ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS related_link VARCHAR(500);
COMMENT ON COLUMN company_portfolios.related_link IS '관련 링크 URL';

ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS copyright_owner VARCHAR(200);
COMMENT ON COLUMN company_portfolios.copyright_owner IS '저작권자';

ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS copyright_license VARCHAR(100);
COMMENT ON COLUMN company_portfolios.copyright_license IS '저작권 라이선스 유형';

ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS copyright_attribution VARCHAR(500);
COMMENT ON COLUMN company_portfolios.copyright_attribution IS '저작권 표시 문구';

ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS comment_count INTEGER DEFAULT 0;
COMMENT ON COLUMN company_portfolios.comment_count IS '댓글 수';

ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS bookmark_count INTEGER DEFAULT 0;
COMMENT ON COLUMN company_portfolios.bookmark_count IS '북마크 수';

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_company_portfolios_is_public ON company_portfolios(is_public) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_company_portfolios_created_at ON company_portfolios(created_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_company_portfolios_view_count ON company_portfolios(view_count DESC) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_company_portfolios_like_count ON company_portfolios(like_count DESC) WHERE is_deleted = FALSE;

-- =====================================================
-- 2. portfolio_promotions 테이블 생성
-- =====================================================

CREATE TABLE portfolio_promotions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    portfolio_id BIGINT NOT NULL REFERENCES company_portfolios(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    promotion_type VARCHAR(30) NOT NULL,  -- STANDARD, PREMIUM, etc.
    weight INTEGER NOT NULL DEFAULT 1,
    monthly_price NUMERIC(12,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    auto_renew BOOLEAN DEFAULT FALSE,
    renewal_notified BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, EXPIRED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

COMMENT ON TABLE portfolio_promotions IS '포트폴리오 우대등록';
COMMENT ON COLUMN portfolio_promotions.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN portfolio_promotions.portfolio_id IS '포트폴리오 ID';
COMMENT ON COLUMN portfolio_promotions.user_id IS '등록자 (업체 대표)';
COMMENT ON COLUMN portfolio_promotions.promotion_type IS '프로모션 타입 (STANDARD, PREMIUM 등)';
COMMENT ON COLUMN portfolio_promotions.weight IS '노출 가중치 (배수)';
COMMENT ON COLUMN portfolio_promotions.monthly_price IS '월 가격 (결제 당시)';
COMMENT ON COLUMN portfolio_promotions.start_date IS '시작일';
COMMENT ON COLUMN portfolio_promotions.end_date IS '종료일';
COMMENT ON COLUMN portfolio_promotions.auto_renew IS '자동갱신 여부';
COMMENT ON COLUMN portfolio_promotions.renewal_notified IS '갱신 알림 전송 여부';
COMMENT ON COLUMN portfolio_promotions.status IS '상태 (ACTIVE, EXPIRED, CANCELLED)';

-- 활성 프로모션 유니크 인덱스 (포트폴리오당 하나의 활성 프로모션만 허용)
CREATE UNIQUE INDEX idx_portfolio_promotions_active_unique
    ON portfolio_promotions(portfolio_id)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE;

CREATE INDEX idx_portfolio_promotions_status ON portfolio_promotions(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_portfolio_promotions_end_date ON portfolio_promotions(end_date) WHERE status = 'ACTIVE' AND is_deleted = FALSE;
CREATE INDEX idx_portfolio_promotions_user_id ON portfolio_promotions(user_id) WHERE is_deleted = FALSE;

-- =====================================================
-- 3. portfolio_promotion_type_settings 테이블 생성
-- =====================================================

CREATE TABLE portfolio_promotion_type_settings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    promotion_type VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(50) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    weight INTEGER NOT NULL DEFAULT 1,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES users(id)
);

COMMENT ON TABLE portfolio_promotion_type_settings IS '포트폴리오 프로모션 타입 설정';
COMMENT ON COLUMN portfolio_promotion_type_settings.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN portfolio_promotion_type_settings.promotion_type IS '타입 코드 (STANDARD, PREMIUM 등)';
COMMENT ON COLUMN portfolio_promotion_type_settings.display_name IS '표시명';
COMMENT ON COLUMN portfolio_promotion_type_settings.price IS '월 가격 (원)';
COMMENT ON COLUMN portfolio_promotion_type_settings.weight IS '노출 가중치 (배수)';
COMMENT ON COLUMN portfolio_promotion_type_settings.display_order IS '표시 순서';
COMMENT ON COLUMN portfolio_promotion_type_settings.is_active IS '활성화 여부';
COMMENT ON COLUMN portfolio_promotion_type_settings.description IS '설명';
COMMENT ON COLUMN portfolio_promotion_type_settings.updated_by IS '최종 수정 관리자';

CREATE INDEX idx_portfolio_promotion_type_settings_active ON portfolio_promotion_type_settings(is_active, display_order);

-- 기본 데이터 삽입
INSERT INTO portfolio_promotion_type_settings (promotion_type, display_name, price, weight, display_order, description)
VALUES
    ('STANDARD', '일반우대', 50000, 1, 1, '기본 우대 등록 - 일반 노출'),
    ('PREMIUM', '강력우대', 100000, 3, 2, '프리미엄 우대 등록 - 3배 노출 확률');

-- =====================================================
-- 4. portfolio_promotion_payments 테이블 생성
-- =====================================================

CREATE TABLE portfolio_promotion_payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    promotion_id BIGINT NOT NULL REFERENCES portfolio_promotions(id) ON DELETE CASCADE,
    payment_amount NUMERIC(12,2) NOT NULL,
    payment_date DATE NOT NULL,
    apply_from_date DATE NOT NULL,
    apply_to_date DATE NOT NULL,
    payment_type VARCHAR(20) NOT NULL,  -- INITIAL, RENEWAL, UPGRADE
    status VARCHAR(20) DEFAULT 'COMPLETED' NOT NULL,  -- COMPLETED, REFUNDED, FAILED
    credit_transaction_id BIGINT,  -- 크레딧 결제시 트랜잭션 ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE portfolio_promotion_payments IS '포트폴리오 프로모션 결제 내역';
COMMENT ON COLUMN portfolio_promotion_payments.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN portfolio_promotion_payments.promotion_id IS '프로모션 ID';
COMMENT ON COLUMN portfolio_promotion_payments.payment_amount IS '결제 금액';
COMMENT ON COLUMN portfolio_promotion_payments.payment_date IS '결제일';
COMMENT ON COLUMN portfolio_promotion_payments.apply_from_date IS '적용 시작일';
COMMENT ON COLUMN portfolio_promotion_payments.apply_to_date IS '적용 종료일';
COMMENT ON COLUMN portfolio_promotion_payments.payment_type IS '결제 유형 (INITIAL: 최초, RENEWAL: 갱신, UPGRADE: 업그레이드)';
COMMENT ON COLUMN portfolio_promotion_payments.status IS '결제 상태';
COMMENT ON COLUMN portfolio_promotion_payments.credit_transaction_id IS '크레딧 트랜잭션 ID (크레딧 결제시)';

CREATE INDEX idx_portfolio_promotion_payments_promotion_id ON portfolio_promotion_payments(promotion_id);
CREATE INDEX idx_portfolio_promotion_payments_payment_date ON portfolio_promotion_payments(payment_date DESC);

-- =====================================================
-- 5. portfolio_filter_options 테이블 생성
-- =====================================================

CREATE TABLE portfolio_filter_options (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES company_portfolios(id) ON DELETE CASCADE,
    filter_option_id BIGINT NOT NULL REFERENCES filter_options(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(portfolio_id, filter_option_id)
);

COMMENT ON TABLE portfolio_filter_options IS '포트폴리오-필터 옵션 연결 테이블';
COMMENT ON COLUMN portfolio_filter_options.portfolio_id IS '포트폴리오 ID';
COMMENT ON COLUMN portfolio_filter_options.filter_option_id IS '필터 옵션 ID';

CREATE INDEX idx_portfolio_filter_options_portfolio_id ON portfolio_filter_options(portfolio_id);
CREATE INDEX idx_portfolio_filter_options_filter_option_id ON portfolio_filter_options(filter_option_id);

-- =====================================================
-- 6. portfolio_bookmarks 테이블 생성
-- =====================================================

CREATE TABLE portfolio_bookmarks (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES company_portfolios(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(portfolio_id, user_id)
);

COMMENT ON TABLE portfolio_bookmarks IS '포트폴리오 북마크';
COMMENT ON COLUMN portfolio_bookmarks.portfolio_id IS '포트폴리오 ID';
COMMENT ON COLUMN portfolio_bookmarks.user_id IS '사용자 ID';

CREATE INDEX idx_portfolio_bookmarks_portfolio_id ON portfolio_bookmarks(portfolio_id);
CREATE INDEX idx_portfolio_bookmarks_user_id ON portfolio_bookmarks(user_id);

-- =====================================================
-- 7. portfolio_likes 테이블 생성
-- =====================================================

CREATE TABLE portfolio_likes (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES company_portfolios(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(portfolio_id, user_id)
);

COMMENT ON TABLE portfolio_likes IS '포트폴리오 좋아요';
COMMENT ON COLUMN portfolio_likes.portfolio_id IS '포트폴리오 ID';
COMMENT ON COLUMN portfolio_likes.user_id IS '사용자 ID';

CREATE INDEX idx_portfolio_likes_portfolio_id ON portfolio_likes(portfolio_id);
CREATE INDEX idx_portfolio_likes_user_id ON portfolio_likes(user_id);

-- =====================================================
-- 8. Gallery 관련 테이블 삭제
-- =====================================================

-- 결제 내역 먼저 삭제 (FK 제약)
DROP TABLE IF EXISTS gallery_promotion_payments CASCADE;

-- 프로모션 삭제
DROP TABLE IF EXISTS gallery_promotions CASCADE;

-- 타입 설정 삭제
DROP TABLE IF EXISTS gallery_promotion_type_settings CASCADE;

-- 기존 gallery_promotion_settings도 있다면 삭제 (V70에서 생성됐을 수 있음)
DROP TABLE IF EXISTS gallery_promotion_settings CASCADE;

-- =====================================================
-- 마이그레이션 완료
-- =====================================================
