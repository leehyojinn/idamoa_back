-- =====================================================
-- V68: Gallery Promotions 누락 컬럼 추가
-- BaseEntity에서 상속받는 deleted_at, metadata 컬럼 추가
-- =====================================================

-- deleted_at 컬럼 추가
ALTER TABLE gallery_promotions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- metadata JSONB 컬럼 추가
ALTER TABLE gallery_promotions ADD COLUMN IF NOT EXISTS metadata JSONB;
