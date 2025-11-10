-- Add is_verified_purchase column to company_reviews table

ALTER TABLE company_reviews
ADD COLUMN IF NOT EXISTS is_verified_purchase BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN company_reviews.is_verified_purchase IS '실제 구매 인증 여부';
