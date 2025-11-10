-- Add like_count and report_count columns to company_reviews table

ALTER TABLE company_reviews
ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS report_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN company_reviews.like_count IS '좋아요 수';
COMMENT ON COLUMN company_reviews.report_count IS '신고 수';
