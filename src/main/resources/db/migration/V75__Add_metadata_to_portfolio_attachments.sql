-- =====================================================
-- V75: portfolio_attachments에 metadata 컬럼 추가
-- BaseEntity에서 상속받는 JSONB 컬럼
-- =====================================================

-- metadata 컬럼 추가
ALTER TABLE portfolio_attachments
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- 코멘트
COMMENT ON COLUMN portfolio_attachments.metadata IS '확장 메타데이터 (JSONB)';
