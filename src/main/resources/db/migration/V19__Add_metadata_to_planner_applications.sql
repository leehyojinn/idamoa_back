-- Add missing metadata column to planner_applications
ALTER TABLE planner_applications
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

COMMENT ON COLUMN planner_applications.metadata IS '추가 메타데이터 (JSON 형식)';
