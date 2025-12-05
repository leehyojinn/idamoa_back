-- =============================================
-- V60: payments 테이블 컬럼을 Payment 엔티티와 정렬
-- =============================================

-- 1. 컬럼 이름 변경
-- =============================================

-- amount → payment_amount
ALTER TABLE payments RENAME COLUMN amount TO payment_amount;

-- tax_amount → fee_amount
ALTER TABLE payments RENAME COLUMN tax_amount TO fee_amount;

-- final_amount → total_amount
ALTER TABLE payments RENAME COLUMN final_amount TO total_amount;

-- pg_tid → transaction_id
ALTER TABLE payments RENAME COLUMN pg_tid TO transaction_id;

-- pg_response → payment_gateway_data
ALTER TABLE payments RENAME COLUMN pg_response TO payment_gateway_data;


-- 2. 누락된 컬럼 추가
-- =============================================

-- entity_type: CREDIT_PURCHASE, AD_CAMPAIGN, SUBSCRIPTION 등
ALTER TABLE payments ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50);

-- entity_id: 연관된 엔티티 ID
ALTER TABLE payments ADD COLUMN IF NOT EXISTS entity_id BIGINT;

-- failed_at: 결제 실패 시간
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failed_at TIMESTAMP;

-- failure_reason: 결제 실패 사유
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failure_reason TEXT;


-- 3. 인덱스 추가
-- =============================================

CREATE INDEX IF NOT EXISTS idx_payments_entity ON payments(entity_type, entity_id);


-- 4. 컬럼 코멘트 추가
-- =============================================

COMMENT ON COLUMN payments.payment_amount IS '결제 금액';
COMMENT ON COLUMN payments.fee_amount IS '수수료';
COMMENT ON COLUMN payments.total_amount IS '총 결제 금액';
COMMENT ON COLUMN payments.transaction_id IS 'PG사 거래 ID';
COMMENT ON COLUMN payments.payment_gateway_data IS 'PG사 응답 데이터 (JSON)';
COMMENT ON COLUMN payments.entity_type IS '연관 엔티티 타입 (CREDIT_PURCHASE, AD_CAMPAIGN, SUBSCRIPTION)';
COMMENT ON COLUMN payments.entity_id IS '연관 엔티티 ID';
COMMENT ON COLUMN payments.failed_at IS '결제 실패 시간';
COMMENT ON COLUMN payments.failure_reason IS '결제 실패 사유';


-- 5. 기존 제약조건 제거 및 새로운 제약조건 추가 (선택사항)
-- =============================================

-- 기존 payment_method 제약조건 제거 후 재생성 (더 많은 결제수단 지원)
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'VIRTUAL_ACCOUNT', 'PHONE',
                               'KAKAO_PAY', 'KAKAOPAY', 'NAVER_PAY', 'TOSS', 'CREDIT'));

