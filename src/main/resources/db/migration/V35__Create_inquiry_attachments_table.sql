-- 일반 문의 첨부파일 테이블 생성
CREATE TABLE IF NOT EXISTS inquiry_attachments (
    id BIGSERIAL PRIMARY KEY,
    inquiry_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    file_type VARCHAR(50),
    file_description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_inquiry_attachments_inquiry FOREIGN KEY (inquiry_id)
        REFERENCES inquiries(id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_attachments_file FOREIGN KEY (file_id)
        REFERENCES files(id) ON DELETE RESTRICT
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_inquiry_attachments_inquiry ON inquiry_attachments(inquiry_id);
CREATE INDEX IF NOT EXISTS idx_inquiry_attachments_file ON inquiry_attachments(file_id);
CREATE INDEX IF NOT EXISTS idx_inquiry_attachments_order ON inquiry_attachments(inquiry_id, display_order);

-- 코멘트 추가
COMMENT ON TABLE inquiry_attachments IS '일반 문의 첨부파일';
COMMENT ON COLUMN inquiry_attachments.id IS '첨부파일 ID';
COMMENT ON COLUMN inquiry_attachments.inquiry_id IS '문의 ID';
COMMENT ON COLUMN inquiry_attachments.file_id IS '파일 ID';
COMMENT ON COLUMN inquiry_attachments.file_type IS '파일 타입 (SCREENSHOT, DOCUMENT, LOG, etc.)';
COMMENT ON COLUMN inquiry_attachments.file_description IS '파일 설명';
COMMENT ON COLUMN inquiry_attachments.display_order IS '표시 순서';
COMMENT ON COLUMN inquiry_attachments.created_at IS '생성일시';
COMMENT ON COLUMN inquiry_attachments.updated_at IS '수정일시';
COMMENT ON COLUMN inquiry_attachments.is_deleted IS '삭제 여부';
COMMENT ON COLUMN inquiry_attachments.deleted_at IS '삭제일시';