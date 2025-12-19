-- =====================================================
-- V67: Gallery Promotion (우대 등록) 테이블 생성
-- =====================================================

-- gallery_promotions: 갤러리(포트폴리오) 우대 등록 정보
CREATE TABLE gallery_promotions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    board_id BIGINT NOT NULL REFERENCES boards(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    promotion_type VARCHAR(20) NOT NULL,           -- STANDARD, PREMIUM
    weight INTEGER NOT NULL DEFAULT 1,              -- STANDARD=1, PREMIUM=3
    monthly_price NUMERIC(12,2) NOT NULL,          -- 50000 또는 100000
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    renewal_notified BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, EXPIRED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 갤러리당 1개 활성 우대만 허용 (Partial Unique Index)
CREATE UNIQUE INDEX idx_gallery_promotions_board_active
    ON gallery_promotions(board_id) WHERE status = 'ACTIVE' AND is_deleted = FALSE;

-- 인덱스
CREATE INDEX idx_gallery_promotions_status ON gallery_promotions(status);
CREATE INDEX idx_gallery_promotions_end_date ON gallery_promotions(end_date);
CREATE INDEX idx_gallery_promotions_user_id ON gallery_promotions(user_id);
CREATE INDEX idx_gallery_promotions_uuid ON gallery_promotions(uuid);

COMMENT ON TABLE gallery_promotions IS '갤러리(포트폴리오) 우대 등록';
COMMENT ON COLUMN gallery_promotions.promotion_type IS '우대 타입: STANDARD(일반우대), PREMIUM(강력우대)';
COMMENT ON COLUMN gallery_promotions.weight IS '노출 가중치: STANDARD=1, PREMIUM=3';
COMMENT ON COLUMN gallery_promotions.monthly_price IS '월 결제 금액: STANDARD=50000, PREMIUM=100000';
COMMENT ON COLUMN gallery_promotions.auto_renew IS '자동 갱신 여부';
COMMENT ON COLUMN gallery_promotions.renewal_notified IS '갱신 알림 발송 여부 (3일 전 알림용)';
COMMENT ON COLUMN gallery_promotions.status IS '상태: ACTIVE(활성), EXPIRED(만료), CANCELLED(취소)';

-- gallery_promotion_payments: 우대 등록 결제 내역
CREATE TABLE gallery_promotion_payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    promotion_id BIGINT NOT NULL REFERENCES gallery_promotions(id),
    payment_amount NUMERIC(12,2) NOT NULL,
    payment_date DATE NOT NULL,
    apply_from_date DATE NOT NULL,
    apply_to_date DATE NOT NULL,
    payment_type VARCHAR(20) NOT NULL DEFAULT 'INITIAL',  -- INITIAL, RENEWAL, UPGRADE
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    credit_transaction_id BIGINT REFERENCES credit_transactions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gallery_promotion_payments_promotion ON gallery_promotion_payments(promotion_id);
CREATE INDEX idx_gallery_promotion_payments_uuid ON gallery_promotion_payments(uuid);

COMMENT ON TABLE gallery_promotion_payments IS '갤러리 우대 등록 결제 내역';
COMMENT ON COLUMN gallery_promotion_payments.payment_type IS '결제 유형: INITIAL(최초), RENEWAL(갱신), UPGRADE(업그레이드)';
COMMENT ON COLUMN gallery_promotion_payments.apply_from_date IS '적용 시작일';
COMMENT ON COLUMN gallery_promotion_payments.apply_to_date IS '적용 종료일';
