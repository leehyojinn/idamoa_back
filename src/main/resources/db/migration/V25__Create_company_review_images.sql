-- V25: Create company_review_images table for managing review images via File IDs
-- This follows the same pattern as company_images (File ID based, not URL based)

-- company_review_images: Join table between reviews and files
CREATE TABLE IF NOT EXISTS company_review_images (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES company_reviews(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Prevent duplicate file assignments to same review
    CONSTRAINT uk_review_file UNIQUE(review_id, file_id)
);

-- Indexes for performance
CREATE INDEX idx_review_images_review_id ON company_review_images(review_id);
CREATE INDEX idx_review_images_file_id ON company_review_images(file_id);

-- Comments for documentation
COMMENT ON TABLE company_review_images IS '리뷰 이미지 관계 테이블 (File ID 기반)';
COMMENT ON COLUMN company_review_images.review_id IS '리뷰 ID (company_reviews FK)';
COMMENT ON COLUMN company_review_images.file_id IS '파일 ID (files FK) - URL이 아닌 File ID로 저장';
COMMENT ON COLUMN company_review_images.display_order IS '이미지 표시 순서';
