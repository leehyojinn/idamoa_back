-- V30: Create attachment join tables for EstimateRequest and EstimateProposal
-- Replace array fields (attachment_file_ids) with proper join tables following Company pattern

-- ============================================================================
-- 1. Create estimate_request_attachments join table
-- ============================================================================
CREATE TABLE estimate_request_attachments (
    id BIGSERIAL PRIMARY KEY,
    estimate_request_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    file_type VARCHAR(50),           -- 파일 타입 (DRAWING, PHOTO, DOCUMENT, ESTIMATE, etc.)
    file_description TEXT,           -- 파일 설명
    display_order INT DEFAULT 0,     -- 표시 순서
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_estimate_request_attachments_request
        FOREIGN KEY (estimate_request_id) REFERENCES estimate_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_estimate_request_attachments_file
        FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,

    -- Unique constraint: same file cannot be attached twice to same request
    CONSTRAINT uk_estimate_request_attachments_request_file
        UNIQUE(estimate_request_id, file_id)
);

-- Indexes for performance
CREATE INDEX idx_estimate_request_attachments_request
    ON estimate_request_attachments(estimate_request_id);
CREATE INDEX idx_estimate_request_attachments_file
    ON estimate_request_attachments(file_id);
CREATE INDEX idx_estimate_request_attachments_order
    ON estimate_request_attachments(estimate_request_id, display_order);

COMMENT ON TABLE estimate_request_attachments IS '견적 요청 첨부파일 조인 테이블';
COMMENT ON COLUMN estimate_request_attachments.file_type IS '파일 타입 (DRAWING=도면, PHOTO=사진, DOCUMENT=문서, ESTIMATE=견적서)';
COMMENT ON COLUMN estimate_request_attachments.file_description IS '파일 설명 (선택사항)';
COMMENT ON COLUMN estimate_request_attachments.display_order IS '표시 순서 (0부터 시작)';

-- ============================================================================
-- 2. Create estimate_proposal_attachments join table
-- ============================================================================
CREATE TABLE estimate_proposal_attachments (
    id BIGSERIAL PRIMARY KEY,
    estimate_proposal_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    file_type VARCHAR(50),           -- 파일 타입 (DRAWING, PHOTO, DOCUMENT, ESTIMATE, etc.)
    file_description TEXT,           -- 파일 설명
    display_order INT DEFAULT 0,     -- 표시 순서
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_estimate_proposal_attachments_proposal
        FOREIGN KEY (estimate_proposal_id) REFERENCES estimate_proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_estimate_proposal_attachments_file
        FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,

    -- Unique constraint: same file cannot be attached twice to same proposal
    CONSTRAINT uk_estimate_proposal_attachments_proposal_file
        UNIQUE(estimate_proposal_id, file_id)
);

-- Indexes for performance
CREATE INDEX idx_estimate_proposal_attachments_proposal
    ON estimate_proposal_attachments(estimate_proposal_id);
CREATE INDEX idx_estimate_proposal_attachments_file
    ON estimate_proposal_attachments(file_id);
CREATE INDEX idx_estimate_proposal_attachments_order
    ON estimate_proposal_attachments(estimate_proposal_id, display_order);

COMMENT ON TABLE estimate_proposal_attachments IS '견적 제안 첨부파일 조인 테이블';
COMMENT ON COLUMN estimate_proposal_attachments.file_type IS '파일 타입 (DRAWING=도면, PHOTO=사진, DOCUMENT=문서, ESTIMATE=견적서)';
COMMENT ON COLUMN estimate_proposal_attachments.file_description IS '파일 설명 (선택사항)';
COMMENT ON COLUMN estimate_proposal_attachments.display_order IS '표시 순서 (0부터 시작)';

-- ============================================================================
-- 3. Drop legacy array fields
-- ============================================================================

-- Drop attachment_file_ids from estimate_requests (added in V26)
ALTER TABLE estimate_requests
    DROP COLUMN IF EXISTS attachment_file_ids;

-- Drop attachments and attachment_file_ids from estimate_proposals (added in V28)
ALTER TABLE estimate_proposals
    DROP COLUMN IF EXISTS attachments;

ALTER TABLE estimate_proposals
    DROP COLUMN IF EXISTS attachment_file_ids;

-- ============================================================================
-- Migration Notes:
-- ============================================================================
-- Before: EstimateRequest/Proposal stored file IDs as PostgreSQL arrays
-- After: Proper join tables with metadata (type, description, order)
--
-- Benefits:
-- 1. Follows Company pattern (company_images table)
-- 2. Allows file reuse across multiple requests/proposals
-- 3. Supports metadata (file type, description, display order)
-- 4. Better query performance with proper indexes
-- 5. CASCADE DELETE ensures orphaned attachments are cleaned up
-- 6. Prevents FileCleanupScheduler from deleting attached files
-- ============================================================================
