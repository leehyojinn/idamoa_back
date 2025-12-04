-- V62: Add pg_transaction_id column to payments table
-- 결제 금액 검증 보안 강화를 위한 PG 거래 ID 저장

-- payments 테이블에 pg_transaction_id 컬럼 추가
ALTER TABLE payments ADD COLUMN IF NOT EXISTS pg_transaction_id VARCHAR(200);

-- 인덱스 추가 (PG 거래 조회용)
CREATE INDEX IF NOT EXISTS idx_payments_pg_transaction_id ON payments(pg_transaction_id);

COMMENT ON COLUMN payments.pg_transaction_id IS 'PG사에서 반환한 거래 ID (결제 승인 후 저장)';
