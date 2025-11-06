-- Fix company_reviews table schema to match CompanyReview entity

-- 1. Rename reply_at to replied_at
ALTER TABLE company_reviews RENAME COLUMN reply_at TO replied_at;

-- 2. Change rating from INT to DECIMAL(2,1)
ALTER TABLE company_reviews ALTER COLUMN rating TYPE DECIMAL(2,1);

-- 3. Add missing BaseEntity columns
ALTER TABLE company_reviews
ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS metadata JSONB;

-- 4. Add like_count and report_count
ALTER TABLE company_reviews
ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS report_count INTEGER NOT NULL DEFAULT 0;

-- 5. Add status column (replacing is_verified, is_reported, is_hidden)
ALTER TABLE company_reviews
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';

-- 6. Migrate existing data to status column
UPDATE company_reviews
SET status = CASE
    WHEN is_hidden = TRUE THEN 'HIDDEN'
    WHEN is_reported = TRUE THEN 'REPORTED'
    ELSE 'PUBLISHED'
END
WHERE status = 'PUBLISHED';

-- 7. Comments
COMMENT ON COLUMN company_reviews.replied_at IS '업체 답변 작성 시각';
COMMENT ON COLUMN company_reviews.rating IS '평점 (1.0 ~ 5.0)';
COMMENT ON COLUMN company_reviews.is_deleted IS 'Soft delete 여부';
COMMENT ON COLUMN company_reviews.deleted_at IS 'Soft delete 시각';
COMMENT ON COLUMN company_reviews.metadata IS '메타데이터 (JSONB)';
COMMENT ON COLUMN company_reviews.like_count IS '좋아요 수';
COMMENT ON COLUMN company_reviews.report_count IS '신고 수';
COMMENT ON COLUMN company_reviews.status IS '상태 (PUBLISHED, HIDDEN, REPORTED, DELETED)';
