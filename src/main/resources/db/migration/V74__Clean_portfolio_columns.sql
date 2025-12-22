-- =====================================================
-- V74: company_portfolios 테이블 정리
-- portfolio_attachments 테이블로 이동된 컬럼 삭제
-- =====================================================

-- 1. images 컬럼 삭제 (portfolio_attachments 테이블의 IMAGE 타입으로 이동됨)
ALTER TABLE company_portfolios DROP COLUMN IF EXISTS images;

-- 2. videos 컬럼 삭제 (portfolio_attachments 테이블의 VIDEO 타입으로 이동됨)
ALTER TABLE company_portfolios DROP COLUMN IF EXISTS videos;

-- 테이블 코멘트 업데이트
COMMENT ON TABLE company_portfolios IS '업체 포트폴리오 - 이미지/비디오는 portfolio_attachments 테이블 참조';
