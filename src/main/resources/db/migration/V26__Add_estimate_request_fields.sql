-- ==============================================================================
-- V26__Add_estimate_request_fields.sql
-- 견적 요청 범용 필드 추가 (병원 외 다양한 업종 지원)
-- Date: 2025-11-07
-- Description:
--   - 견적 요청에 범용 필드 추가 (사업장명, 업종, 평수, 연락처 등)
--   - 견적 제안에 업체 정보 필드 추가 (회사소개, 시공이력 등)
--   - 첨부파일 ID 배열 필드 추가
-- ==============================================================================

-- =======================================
-- 1. estimate_requests 테이블에 범용 필드 추가
-- =======================================
ALTER TABLE estimate_requests
    ADD COLUMN IF NOT EXISTS client_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS area_pyeong DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS submission_deadline TIMESTAMP,
    ADD COLUMN IF NOT EXISTS attachment_file_ids bigint[];

-- Add comments for new columns
COMMENT ON COLUMN estimate_requests.client_name IS '사업장명 또는 고객명 (병원, 카페, 사무실 등)';
COMMENT ON COLUMN estimate_requests.business_type IS '업종 분류 (의료, 카페, 사무실, 매장, 주거 등)';
COMMENT ON COLUMN estimate_requests.area_pyeong IS '시공 면적 (평수)';
COMMENT ON COLUMN estimate_requests.contact_name IS '견적 요청자 이름';
COMMENT ON COLUMN estimate_requests.contact_phone IS '견적 요청자 연락처';
COMMENT ON COLUMN estimate_requests.submission_deadline IS '업체 제안서 제출 마감일시';
COMMENT ON COLUMN estimate_requests.attachment_file_ids IS '첨부파일 ID 배열 (files 테이블 참조)';

-- =======================================
-- 2. estimate_proposals 테이블에 업체 정보 필드 추가
-- =======================================
ALTER TABLE estimate_proposals
    ADD COLUMN IF NOT EXISTS company_introduction TEXT,
    ADD COLUMN IF NOT EXISTS construction_history TEXT,
    ADD COLUMN IF NOT EXISTS support_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS attachment_file_ids bigint[];

-- Add comments for new columns
COMMENT ON COLUMN estimate_proposals.company_introduction IS '업체 회사 소개';
COMMENT ON COLUMN estimate_proposals.construction_history IS '업체 시공 이력 설명';
COMMENT ON COLUMN estimate_proposals.support_name IS '제안서 작성 담당자명';
COMMENT ON COLUMN estimate_proposals.attachment_file_ids IS '첨부파일 ID 배열 (files 테이블 참조)';

-- =======================================
-- 3. 인덱스 추가 (성능 최적화)
-- =======================================

-- 업종별 검색을 위한 인덱스
CREATE INDEX IF NOT EXISTS idx_estimate_requests_business_type
    ON estimate_requests(business_type)
    WHERE is_deleted = FALSE;

-- 제안 마감일 기준 정렬/검색을 위한 인덱스
CREATE INDEX IF NOT EXISTS idx_estimate_requests_submission_deadline
    ON estimate_requests(submission_deadline)
    WHERE is_deleted = FALSE AND status = 'PUBLISHED';

-- 지역별 검색을 위한 인덱스 (기존 location 필드 활용)
CREATE INDEX IF NOT EXISTS idx_estimate_requests_location
    ON estimate_requests(location)
    WHERE is_deleted = FALSE AND status = 'PUBLISHED';

-- =======================================
-- 4. 데이터 마이그레이션 (기존 데이터가 있는 경우)
-- =======================================

-- requirements JSONB에서 데이터 추출하여 새 필드로 이관 (있는 경우)
UPDATE estimate_requests
SET
    area_pyeong = CASE
        WHEN requirements->>'area_sqm' IS NOT NULL
        THEN (requirements->>'area_sqm')::numeric / 3.3058
        ELSE NULL
    END
WHERE requirements IS NOT NULL
    AND requirements->>'area_sqm' IS NOT NULL
    AND area_pyeong IS NULL;

-- =======================================
-- 5. 테이블 통계 업데이트
-- =======================================
ANALYZE estimate_requests;
ANALYZE estimate_proposals;

-- =======================================
-- Migration 완료
-- =======================================