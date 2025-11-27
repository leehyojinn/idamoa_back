-- V37: Add missing columns to inquiry_answers table (BaseEntity fields)

-- Add missing columns to inquiry_answers table
ALTER TABLE inquiry_answers
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS metadata JSONB;

-- Add index for soft delete queries
CREATE INDEX IF NOT EXISTS idx_inquiry_answers_is_deleted
    ON inquiry_answers(is_deleted);

-- Add index for deleted_at for cleanup queries
CREATE INDEX IF NOT EXISTS idx_inquiry_answers_deleted_at
    ON inquiry_answers(deleted_at)
    WHERE deleted_at IS NOT NULL;

-- Add comment on columns
COMMENT ON COLUMN inquiry_answers.is_deleted IS 'Soft delete flag';
COMMENT ON COLUMN inquiry_answers.deleted_at IS 'Soft delete timestamp';
COMMENT ON COLUMN inquiry_answers.metadata IS 'Additional metadata in JSON format';