-- 플래너 신청서 첨부파일 테이블 생성
CREATE TABLE planner_application_attachments (
    id BIGSERIAL PRIMARY KEY,
    planner_application_id BIGINT NOT NULL REFERENCES planner_applications(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES files(id),
    file_type VARCHAR(50),
    file_description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_planner_application_attachments_application
    ON planner_application_attachments(planner_application_id);
CREATE INDEX idx_planner_application_attachments_file
    ON planner_application_attachments(file_id);
CREATE INDEX idx_planner_application_attachments_order
    ON planner_application_attachments(planner_application_id, display_order);
CREATE INDEX idx_planner_application_attachments_is_deleted
    ON planner_application_attachments(is_deleted);

-- 유니크 제약조건 (soft delete 고려)
CREATE UNIQUE INDEX uk_planner_application_attachments_application_file
    ON planner_application_attachments(planner_application_id, file_id)
    WHERE is_deleted = FALSE;

-- 기존 데이터 마이그레이션 (attachment_file_ids -> 새 테이블)
INSERT INTO planner_application_attachments (planner_application_id, file_id, display_order, created_at, updated_at)
SELECT
    pa.id,
    unnest(pa.attachment_file_ids),
    row_number() OVER (PARTITION BY pa.id ORDER BY unnest(pa.attachment_file_ids)) - 1,
    pa.created_at,
    CURRENT_TIMESTAMP
FROM planner_applications pa
WHERE pa.attachment_file_ids IS NOT NULL
  AND array_length(pa.attachment_file_ids, 1) > 0;

-- 기존 attachment_file_ids 컬럼 삭제
ALTER TABLE planner_applications DROP COLUMN IF EXISTS attachment_file_ids;

-- 코멘트 추가
COMMENT ON TABLE planner_application_attachments IS '플래너 신청서 첨부파일';
COMMENT ON COLUMN planner_application_attachments.file_type IS '파일 타입: DRAWING(도면), PHOTO(사진), DOCUMENT(문서) 등';
COMMENT ON COLUMN planner_application_attachments.file_description IS '파일 설명';
COMMENT ON COLUMN planner_application_attachments.display_order IS '표시 순서';
