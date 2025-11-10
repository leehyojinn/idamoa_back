-- ==============================================================================
-- V27__Convert_enum_to_varchar_with_check.sql
-- PostgreSQL ENUM을 VARCHAR + CHECK 제약조건으로 변환
-- Date: 2025-11-07
-- Description:
--   - estimate_status, proposal_status 등 모든 ENUM 타입을 VARCHAR로 변경
--   - CHECK 제약조건으로 허용 값 제한
--   - 개발 및 유지보수 편의성 향상
-- ==============================================================================

-- =======================================
-- 1. estimate_requests 테이블의 status 변경
-- =======================================
-- ENUM 타입을 사용하는 인덱스 삭제 (재생성 예정)
DROP INDEX IF EXISTS idx_estimate_requests_location;
DROP INDEX IF EXISTS idx_estimate_requests_submission_deadline;
DROP INDEX IF EXISTS idx_estimate_requests_status;

-- 기존 CHECK 제약조건 삭제
ALTER TABLE estimate_requests
  DROP CONSTRAINT IF EXISTS estimate_requests_status_check;

-- 컬럼 타입 변경
ALTER TABLE estimate_requests
  ALTER COLUMN status TYPE VARCHAR(20)
  USING status::text;

-- CHECK 제약조건 추가 (특정 값만 허용)
ALTER TABLE estimate_requests
  ADD CONSTRAINT chk_estimate_request_status
  CHECK (status IN ('DRAFT', 'PUBLISHED', 'IN_PROGRESS', 'MATCHED', 'COMPLETED', 'CANCELLED'));

-- 기본값 재설정
ALTER TABLE estimate_requests
  ALTER COLUMN status SET DEFAULT 'DRAFT';

COMMENT ON COLUMN estimate_requests.status IS '견적 요청 상태 (DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED, CANCELLED)';

-- 인덱스 재생성 (VARCHAR로 변경 후)
CREATE INDEX idx_estimate_requests_status ON estimate_requests(status);
CREATE INDEX idx_estimate_requests_location ON estimate_requests(location)
  WHERE is_deleted = false AND status = 'PUBLISHED';
CREATE INDEX idx_estimate_requests_submission_deadline ON estimate_requests(submission_deadline)
  WHERE is_deleted = false AND status = 'PUBLISHED';

-- =======================================
-- 2. estimate_proposals 테이블의 status 변경
-- =======================================
-- 인덱스 삭제
DROP INDEX IF EXISTS idx_estimate_proposals_status;

-- 컬럼 타입 변경
ALTER TABLE estimate_proposals
  ALTER COLUMN status TYPE VARCHAR(20)
  USING status::text;

-- CHECK 제약조건 추가
ALTER TABLE estimate_proposals
  ADD CONSTRAINT chk_estimate_proposal_status
  CHECK (status IN ('PENDING', 'VIEWED', 'ACCEPTED', 'REJECTED', 'CANCELLED'));

-- 기본값 재설정
ALTER TABLE estimate_proposals
  ALTER COLUMN status SET DEFAULT 'PENDING';

COMMENT ON COLUMN estimate_proposals.status IS '제안 상태 (PENDING, VIEWED, ACCEPTED, REJECTED, CANCELLED)';

-- 인덱스 재생성
CREATE INDEX idx_estimate_proposals_status ON estimate_proposals(status);

-- =======================================
-- 3. matches 테이블의 status 변경
-- =======================================
-- 인덱스 삭제
DROP INDEX IF EXISTS idx_matches_status;

-- 컬럼 타입 변경
ALTER TABLE matches
  ALTER COLUMN status TYPE VARCHAR(20)
  USING status::text;

-- CHECK 제약조건 추가
ALTER TABLE matches
  ADD CONSTRAINT chk_match_status
  CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'DISPUTED'));

-- 기본값 재설정
ALTER TABLE matches
  ALTER COLUMN status SET DEFAULT 'ACTIVE';

COMMENT ON COLUMN matches.status IS '매칭 상태 (ACTIVE, COMPLETED, CANCELLED, DISPUTED)';

-- 인덱스 재생성
CREATE INDEX idx_matches_status ON matches(status);

-- =======================================
-- 4. ENUM 타입 삭제 (더 이상 필요 없음)
-- =======================================

-- ENUM 타입 삭제
DO $$
BEGIN
    -- estimate_status 타입 삭제 (CASCADE로 모든 의존성 함께 삭제)
    DROP TYPE IF EXISTS estimate_status CASCADE;
    RAISE NOTICE 'estimate_status type dropped';

    -- proposal_status 타입 삭제
    DROP TYPE IF EXISTS proposal_status CASCADE;
    RAISE NOTICE 'proposal_status type dropped';

    -- match_status 타입 삭제
    DROP TYPE IF EXISTS match_status CASCADE;
    RAISE NOTICE 'match_status type dropped';

EXCEPTION WHEN OTHERS THEN
    -- 타입 삭제 실패 시에도 계속 진행
    RAISE NOTICE 'Some types could not be dropped: %', SQLERRM;
END $$;

-- =======================================
-- 5. 통계 업데이트
-- =======================================
ANALYZE estimate_requests;
ANALYZE estimate_proposals;
ANALYZE matches;

-- =======================================
-- Migration 완료
-- =======================================