-- V23: Add UNIQUE constraint to prevent duplicate proposals from same company
-- 같은 업체가 같은 견적 요청에 여러 제안을 제출하는 것을 방지

-- 1. 기존 중복 데이터 정리 (가장 최신 것만 남기고 나머지는 WITHDRAWN 처리)
UPDATE estimate_proposals ep1
SET status = 'WITHDRAWN',
    updated_at = CURRENT_TIMESTAMP
WHERE ep1.is_deleted = false
  AND ep1.status != 'WITHDRAWN'
  AND EXISTS (
    SELECT 1
    FROM estimate_proposals ep2
    WHERE ep2.request_id = ep1.request_id
      AND ep2.company_id = ep1.company_id
      AND ep2.is_deleted = false
      AND ep2.status != 'WITHDRAWN'
      AND ep2.created_at > ep1.created_at
  );

-- 2. UNIQUE constraint 추가 (WITHDRAWN 상태가 아닌 제안만 체크)
-- Partial unique index를 사용하여 WITHDRAWN 상태는 제외
CREATE UNIQUE INDEX idx_unique_active_proposal_per_company
ON estimate_proposals (request_id, company_id)
WHERE is_deleted = false AND status != 'WITHDRAWN';
