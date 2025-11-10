-- =======================================
-- V29: estimate_proposals status constraint 수정
-- 목적: V27에서 생성된 잘못된 constraint 삭제 및 올바른 constraint 추가
-- =======================================

-- 1. V27에서 생성된 잘못된 constraint 삭제
ALTER TABLE estimate_proposals
    DROP CONSTRAINT IF EXISTS chk_estimate_proposal_status;

-- 2. V28에서 생성된 constraint도 삭제 (있다면)
ALTER TABLE estimate_proposals
    DROP CONSTRAINT IF EXISTS chk_proposal_status;

-- 3. 올바른 constraint 추가
ALTER TABLE estimate_proposals
    ADD CONSTRAINT chk_estimate_proposal_status
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'VIEWED', 'SELECTED', 'REJECTED', 'WITHDRAWN'));

-- 4. 기본값 재설정
ALTER TABLE estimate_proposals
    ALTER COLUMN status SET DEFAULT 'SUBMITTED';

-- 5. 코멘트 업데이트
COMMENT ON COLUMN estimate_proposals.status IS '제안 상태 (DRAFT/SUBMITTED/VIEWED/SELECTED/REJECTED/WITHDRAWN)';

-- 6. 통계 업데이트
ANALYZE estimate_proposals;

-- =======================================
-- Migration 완료
-- =======================================
