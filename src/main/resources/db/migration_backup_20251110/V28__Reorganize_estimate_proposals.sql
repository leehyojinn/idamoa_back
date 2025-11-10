-- =======================================
-- V28: estimate_proposals 테이블 재구성
-- 목적: 제안서를 심플하게 재구성, 회사 정보는 companies 테이블에서 JOIN
-- =======================================

-- 1. 기존 컬럼 이름 변경 (존재하지 않는 컬럼들 추가)
-- =======================================

-- proposal_amount -> price로 변경 (이미 price 컬럼이 있으므로 proposal_amount 삭제)
ALTER TABLE estimate_proposals
    DROP COLUMN IF EXISTS proposal_amount,
    DROP COLUMN IF EXISTS proposal_content,
    DROP COLUMN IF EXISTS estimated_duration_days,
    DROP COLUMN IF EXISTS proposed_start_date,
    DROP COLUMN IF EXISTS proposed_end_date,
    DROP COLUMN IF EXISTS portfolio_links,
    DROP COLUMN IF EXISTS cover_letter,
    DROP COLUMN IF EXISTS viewed_at,
    DROP COLUMN IF EXISTS accepted_at,
    DROP COLUMN IF EXISTS rejected_at,
    DROP COLUMN IF EXISTS rejection_reason;

-- 필요한 컬럼이 없으면 추가
ALTER TABLE estimate_proposals
    ADD COLUMN IF NOT EXISTS title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS description TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS price DECIMAL(12,2) NOT NULL DEFAULT 0;

-- status 컬럼 타입 변경 (ENUM -> VARCHAR)
-- 먼저 기존 인덱스 삭제
DROP INDEX IF EXISTS idx_estimate_proposals_status;

-- status 컬럼 타입 변경
ALTER TABLE estimate_proposals
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- CHECK 제약 조건 추가
ALTER TABLE estimate_proposals
    DROP CONSTRAINT IF EXISTS chk_proposal_status;

ALTER TABLE estimate_proposals
    ADD CONSTRAINT chk_proposal_status
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'VIEWED', 'SELECTED', 'REJECTED', 'WITHDRAWN'));

-- 기본값 설정
ALTER TABLE estimate_proposals
    ALTER COLUMN status SET DEFAULT 'SUBMITTED';

-- 2. V26에서 추가한 불필요한 컬럼들 제거
-- =======================================
ALTER TABLE estimate_proposals
    DROP COLUMN IF EXISTS company_introduction,
    DROP COLUMN IF EXISTS construction_history,
    DROP COLUMN IF EXISTS support_name;

-- attachment_file_ids는 유지 (첨부파일용)

-- 3. 인덱스 재생성
-- =======================================
CREATE INDEX IF NOT EXISTS idx_estimate_proposals_status
    ON estimate_proposals(status);

CREATE INDEX IF NOT EXISTS idx_estimate_proposals_is_selected
    ON estimate_proposals(is_selected);

CREATE INDEX IF NOT EXISTS idx_estimate_proposals_selected_at
    ON estimate_proposals(selected_at)
    WHERE selected_at IS NOT NULL;

-- 4. 코멘트 업데이트
-- =======================================
COMMENT ON COLUMN estimate_proposals.title IS '제안 제목';
COMMENT ON COLUMN estimate_proposals.description IS '제안 설명';
COMMENT ON COLUMN estimate_proposals.price IS '제안 가격';
COMMENT ON COLUMN estimate_proposals.attachments IS '첨부파일 경로 배열';
COMMENT ON COLUMN estimate_proposals.attachment_file_ids IS '첨부파일 ID 배열 (files 테이블 참조)';
COMMENT ON COLUMN estimate_proposals.status IS '제안 상태 (DRAFT/SUBMITTED/VIEWED/SELECTED/REJECTED/WITHDRAWN)';
COMMENT ON COLUMN estimate_proposals.is_selected IS '선택 여부';
COMMENT ON COLUMN estimate_proposals.selected_at IS '선택 일시';

-- 5. 데이터 정리 (기존 데이터가 있는 경우)
-- =======================================
-- title이 비어있으면 기본값 설정
UPDATE estimate_proposals
SET title = '제안서 #' || id
WHERE title IS NULL OR title = '';

-- description이 비어있으면 기본값 설정
UPDATE estimate_proposals
SET description = '제안 내용입니다.'
WHERE description IS NULL OR description = '';

-- 6. NOT NULL 제약조건 추가
-- =======================================
ALTER TABLE estimate_proposals
    ALTER COLUMN title SET NOT NULL,
    ALTER COLUMN description SET NOT NULL,
    ALTER COLUMN price SET NOT NULL;

-- 7. 통계 업데이트
-- =======================================
ANALYZE estimate_proposals;

-- =======================================
-- Migration 완료
-- =======================================