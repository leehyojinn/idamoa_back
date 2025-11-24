-- =====================================================
-- V25: 파일 첨부 테이블들에 soft delete 필드 추가
-- =====================================================

-- 1. company_review_images 테이블에 updated_at, is_deleted, deleted_at 추가
ALTER TABLE company_review_images
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 기존 데이터에 updated_at 값 설정 (created_at과 동일하게)
UPDATE company_review_images
SET updated_at = created_at
WHERE updated_at IS NULL;

-- 2. estimate_request_attachments 테이블에 is_deleted, deleted_at 추가
ALTER TABLE estimate_request_attachments
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 3. estimate_proposal_attachments 테이블에 is_deleted, deleted_at 추가
ALTER TABLE estimate_proposal_attachments
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 4. 인덱스 추가 (soft delete 조회 성능 향상)
CREATE INDEX IF NOT EXISTS idx_company_review_images_not_deleted
    ON company_review_images(review_id) WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_estimate_request_attachments_not_deleted
    ON estimate_request_attachments(estimate_request_id) WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_estimate_proposal_attachments_not_deleted
    ON estimate_proposal_attachments(estimate_proposal_id) WHERE is_deleted = FALSE;
