-- V13: Add soft delete and metadata columns to board_categories
-- Created: 2025-11-12
-- Purpose: 어드민이 카테고리를 삭제할 수 있도록 soft delete 지원 추가

-- Add is_deleted column
ALTER TABLE board_categories
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;

-- Add deleted_at column
ALTER TABLE board_categories
ADD COLUMN deleted_at TIMESTAMP;

-- Add metadata column (JSONB for extended data)
ALTER TABLE board_categories
ADD COLUMN metadata JSONB DEFAULT '{}'::jsonb;

-- Add index for is_deleted for better query performance
CREATE INDEX idx_board_categories_is_deleted ON board_categories(is_deleted);

-- Add comments
COMMENT ON COLUMN board_categories.is_deleted IS 'Soft delete 여부';
COMMENT ON COLUMN board_categories.deleted_at IS 'Soft delete 일시';
COMMENT ON COLUMN board_categories.metadata IS '확장 데이터 (JSONB)';
