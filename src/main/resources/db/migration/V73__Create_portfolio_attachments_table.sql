-- =====================================================
-- V73: Portfolio Attachments 중간 테이블 생성
-- Board의 board_attachments와 동일한 패턴
-- =====================================================

-- portfolio_attachments 테이블 생성
CREATE TABLE portfolio_attachments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    portfolio_id BIGINT NOT NULL REFERENCES company_portfolios(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    attachment_type VARCHAR(50) NOT NULL DEFAULT 'IMAGE',  -- IMAGE, VIDEO, THUMBNAIL
    display_order INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_portfolio_attachments_portfolio_id ON portfolio_attachments(portfolio_id);
CREATE INDEX idx_portfolio_attachments_file_id ON portfolio_attachments(file_id);
CREATE INDEX idx_portfolio_attachments_type ON portfolio_attachments(attachment_type);
CREATE INDEX idx_portfolio_attachments_order ON portfolio_attachments(portfolio_id, display_order);
CREATE UNIQUE INDEX idx_portfolio_attachments_unique ON portfolio_attachments(portfolio_id, file_id)
    WHERE is_deleted = FALSE;

-- 코멘트
COMMENT ON TABLE portfolio_attachments IS '포트폴리오 첨부파일 연결 테이블';
COMMENT ON COLUMN portfolio_attachments.portfolio_id IS '포트폴리오 ID (FK)';
COMMENT ON COLUMN portfolio_attachments.file_id IS '파일 ID (FK)';
COMMENT ON COLUMN portfolio_attachments.attachment_type IS '첨부파일 타입: IMAGE, VIDEO, THUMBNAIL';
COMMENT ON COLUMN portfolio_attachments.display_order IS '표시 순서';
