-- =============================================
-- V61: 광고 캠페인 자동 갱신 시스템
-- =============================================

-- 1. ad_campaigns 테이블에 자동 갱신 관련 컬럼 추가
-- =============================================

-- 현재 사이클 누적 결제 금액 (초기 결제 + 추가 결제)
-- 다음 자동 갱신 시 이 금액으로 결제
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS accumulated_payment NUMERIC(12,2) DEFAULT 0;

-- 현재 사이클 시작일 (갱신 시 업데이트)
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS cycle_start_date DATE;

-- 갱신 알림 발송 시간 (3일 전 알림)
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS renewal_notified_at TIMESTAMP;

-- 갱신 알림 발송 여부 (현재 사이클에서 알림 발송했는지)
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS renewal_notified BOOLEAN DEFAULT FALSE;


-- 2. 기존 캠페인 데이터 마이그레이션
-- =============================================

-- cycle_start_date를 start_date로 초기화
UPDATE ad_campaigns
SET cycle_start_date = start_date
WHERE cycle_start_date IS NULL;

-- accumulated_payment 초기화 (기존 total_spent 사용)
UPDATE ad_campaigns
SET accumulated_payment = COALESCE(total_spent, 0)
WHERE accumulated_payment = 0 OR accumulated_payment IS NULL;


-- 3. 컬럼 코멘트 추가
-- =============================================

COMMENT ON COLUMN ad_campaigns.accumulated_payment IS '현재 사이클 누적 결제 금액 (자동 갱신 시 사용)';
COMMENT ON COLUMN ad_campaigns.cycle_start_date IS '현재 사이클 시작일';
COMMENT ON COLUMN ad_campaigns.renewal_notified_at IS '갱신 알림 발송 시간';
COMMENT ON COLUMN ad_campaigns.renewal_notified IS '현재 사이클 갱신 알림 발송 여부';


-- 4. 인덱스 추가
-- =============================================

-- 자동 갱신 대상 조회용 인덱스
CREATE INDEX IF NOT EXISTS idx_ad_campaigns_auto_renew
ON ad_campaigns(auto_renew, status, end_date)
WHERE is_deleted = FALSE;

-- 갱신 알림 대상 조회용 인덱스
CREATE INDEX IF NOT EXISTS idx_ad_campaigns_renewal_notification
ON ad_campaigns(auto_renew, status, end_date, renewal_notified)
WHERE is_deleted = FALSE;
