--
-- V66: payment_method 제약조건에 REFUND 추가 및 refunds 테이블 스키마 업데이트
--
-- 목적:
--   1. 크레딧 환불 요청 시 payment_method='REFUND' 허용
--   2. refunds 테이블에 계좌 정보 컬럼 추가
--

-- ============================================================
-- 1. payments 테이블: payment_method 제약조건 업데이트
-- ============================================================
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'VIRTUAL_ACCOUNT', 'PHONE',
                               'KAKAO_PAY', 'KAKAOPAY', 'NAVER_PAY', 'TOSS', 'CREDIT', 'REFUND'));

-- ============================================================
-- 2. refunds 테이블: 계좌 정보 컬럼 추가
-- ============================================================

-- 환불 계좌 정보
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS bank_name VARCHAR(50);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS account_number VARCHAR(50);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS account_holder VARCHAR(100);

-- processed_at 컬럼 (completed_at과 별개로 추가)
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

-- BaseEntity 컬럼들
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- user_id nullable로 변경 (Entity에서는 payment를 통해 user 접근)
ALTER TABLE refunds ALTER COLUMN user_id DROP NOT NULL;

-- ============================================================
-- 3. refund_reason 타입 변경 (VARCHAR(500) → TEXT)
-- ============================================================
ALTER TABLE refunds ALTER COLUMN refund_reason TYPE TEXT;

-- ============================================================
-- 4. 코멘트 추가
-- ============================================================
COMMENT ON COLUMN refunds.bank_name IS '환불 은행명';
COMMENT ON COLUMN refunds.account_number IS '환불 계좌번호';
COMMENT ON COLUMN refunds.account_holder IS '예금주명';
COMMENT ON COLUMN refunds.processed_at IS '처리 완료 일시';
