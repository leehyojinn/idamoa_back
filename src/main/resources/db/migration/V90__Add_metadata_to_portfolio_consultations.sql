-- portfolio_consultations 테이블에 metadata 컬럼 추가 (BaseEntity 필드)
ALTER TABLE portfolio_consultations
ADD COLUMN metadata JSONB DEFAULT '{}';

COMMENT ON COLUMN portfolio_consultations.metadata IS 'JSONB 확장 데이터';
