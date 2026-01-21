-- portfolio_consultations 테이블에 answered_by_id 컬럼 추가 (답변한 사람)
ALTER TABLE portfolio_consultations
ADD COLUMN answered_by_id BIGINT REFERENCES users(id);

CREATE INDEX idx_portfolio_consultations_answered_by ON portfolio_consultations(answered_by_id);

COMMENT ON COLUMN portfolio_consultations.answered_by_id IS '답변한 사람 (업체 소유자)';
