-- V15: Alter board_attachments table to add missing columns
-- Created: 2025-11-12
-- Purpose: board_attachments 테이블에 BaseEntity 호환 컬럼 추가 (uuid, updated_at, soft delete, metadata, description)

-- ============================================================
-- 기존 테이블에 컬럼 추가
-- ============================================================

-- UUID 컬럼 추가 (BaseEntity)
ALTER TABLE board_attachments
ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid() NOT NULL;

-- updated_at 컬럼 추가 (BaseEntity)
ALTER TABLE board_attachments
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Soft delete 컬럼 추가 (BaseEntity)
ALTER TABLE board_attachments
ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE board_attachments
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 메타데이터 컬럼 추가 (BaseEntity)
ALTER TABLE board_attachments
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

-- 설명 컬럼 추가
ALTER TABLE board_attachments
ADD COLUMN IF NOT EXISTS description TEXT;

-- ============================================================
-- 유니크 제약조건 추가 (UUID)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'board_attachments_uuid_key'
    ) THEN
        ALTER TABLE board_attachments ADD CONSTRAINT board_attachments_uuid_key UNIQUE (uuid);
    END IF;
END $$;

-- ============================================================
-- 인덱스 생성
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_board_attachments_is_deleted ON board_attachments(is_deleted);
CREATE INDEX IF NOT EXISTS idx_board_attachments_board_type ON board_attachments(board_id, attachment_type);

-- ============================================================
-- 기존 데이터 업데이트 (UUID가 NULL인 경우)
-- ============================================================
UPDATE board_attachments
SET uuid = gen_random_uuid()
WHERE uuid IS NULL;

-- ============================================================
-- 코멘트 추가/수정
-- ============================================================
COMMENT ON COLUMN board_attachments.uuid IS '외부 API용 UUID';
COMMENT ON COLUMN board_attachments.updated_at IS '수정 일시';
COMMENT ON COLUMN board_attachments.is_deleted IS 'Soft delete 여부';
COMMENT ON COLUMN board_attachments.deleted_at IS 'Soft delete 일시';
COMMENT ON COLUMN board_attachments.metadata IS '확장 메타데이터 (JSONB)';
COMMENT ON COLUMN board_attachments.description IS '파일 설명';
