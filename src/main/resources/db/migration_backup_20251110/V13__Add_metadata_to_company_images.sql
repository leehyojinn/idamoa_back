-- V13: Add metadata column to company_images table
--
-- BaseEntity를 상속하는 모든 엔티티는 metadata(JSONB) 컬럼이 필요합니다.
-- company_images 테이블에 metadata 컬럼을 추가합니다.

ALTER TABLE company_images
ADD COLUMN IF NOT EXISTS metadata JSONB;

COMMENT ON COLUMN company_images.metadata IS '확장 메타데이터 (JSONB 형식)';
