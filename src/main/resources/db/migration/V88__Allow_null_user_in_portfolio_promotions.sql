-- 포트폴리오 프로모션 user_id nullable 허용
-- 관리자가 직접 생성한 프로모션은 user_id가 없을 수 있음

ALTER TABLE portfolio_promotions
    ALTER COLUMN user_id DROP NOT NULL;

COMMENT ON COLUMN portfolio_promotions.user_id IS '결제한 사용자 ID (관리자 생성 시 NULL 가능)';
