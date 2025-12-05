-- =============================================
-- V59: credit_packages 테이블에 metadata 컬럼 추가
-- =============================================
-- BaseEntity에서 상속받는 metadata 컬럼 추가

ALTER TABLE credit_packages
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

COMMENT ON COLUMN credit_packages.metadata IS '확장 데이터 (JSON)';
