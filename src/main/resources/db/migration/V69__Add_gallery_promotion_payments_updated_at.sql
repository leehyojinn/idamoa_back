-- =====================================================
-- V69: Gallery Promotion Payments 누락 컬럼 추가
-- BaseTimeEntity에서 상속받는 updated_at 컬럼 추가
-- =====================================================

-- updated_at 컬럼 추가
ALTER TABLE gallery_promotion_payments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
