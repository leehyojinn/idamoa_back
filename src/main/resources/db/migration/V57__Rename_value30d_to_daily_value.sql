-- =============================================
-- V57: 30일 환산 가치 → 1일 가치로 변경
-- =============================================
-- 더 직관적인 "1일 가치(daily_value)" 기반 우선순위 계산으로 변경
-- 예: 7일에 700원 → dailyValue = 100원
-- 예: 추가 충전 600원 (남은 6일) → 추가 dailyValue = 100원
-- 총 dailyValue = 200원

-- 1. ad_payments 테이블 컬럼 변경
-- =============================================

-- value_30d 컬럼을 daily_value로 변경
-- 기존 daily_rate 컬럼이 이미 1일 가치를 의미하므로 이를 활용
ALTER TABLE ad_payments RENAME COLUMN daily_rate TO daily_value;

-- value_30d 컬럼 삭제 (더 이상 사용 안함)
ALTER TABLE ad_payments DROP COLUMN IF EXISTS value_30d;

COMMENT ON COLUMN ad_payments.daily_value IS '1일 가치 (결제금액 / 적용일수)';


-- 2. ad_campaigns 테이블 컬럼 변경
-- =============================================

-- total_value_30d 컬럼을 total_daily_value로 변경
ALTER TABLE ad_campaigns RENAME COLUMN total_value_30d TO total_daily_value;

COMMENT ON COLUMN ad_campaigns.total_daily_value IS '총 1일 가치 (모든 활성 결제의 dailyValue 합)';


-- 3. 함수 업데이트 (1일 가치 기반)
-- =============================================

-- 캠페인 1일 가치 계산 함수
CREATE OR REPLACE FUNCTION calculate_campaign_daily_value(campaign_id_param BIGINT)
RETURNS NUMERIC AS $$
DECLARE
    total_daily_value NUMERIC(12,4);
    today DATE := CURRENT_DATE;
BEGIN
    -- 현재 유효한 결제들의 dailyValue 합산
    -- 각 결제의 dailyValue = 결제금액 / 적용일수
    SELECT COALESCE(SUM(daily_value), 0) INTO total_daily_value
    FROM ad_payments
    WHERE campaign_id = campaign_id_param
      AND status = 'ACTIVE'
      AND apply_from_date <= today
      AND apply_to_date >= today;

    RETURN ROUND(total_daily_value, 2);
END;
$$ LANGUAGE plpgsql;

-- 기존 30일 함수 삭제
DROP FUNCTION IF EXISTS calculate_campaign_value_30d(BIGINT);

-- 캠페인 우선순위 업데이트 프로시저 (1일 가치 기반)
CREATE OR REPLACE PROCEDURE update_campaign_priorities()
LANGUAGE plpgsql AS $$
DECLARE
    campaign_rec RECORD;
    daily_value NUMERIC(12,2);
    secondary NUMERIC(12,2);
BEGIN
    FOR campaign_rec IN
        SELECT id FROM ad_campaigns
        WHERE status = 'ACTIVE'
          AND is_deleted = FALSE
          AND (end_date IS NULL OR end_date >= CURRENT_DATE)
    LOOP
        -- 1일 가치 계산
        daily_value := calculate_campaign_daily_value(campaign_rec.id);
        -- 2차 점수 계산
        secondary := calculate_campaign_secondary_score(campaign_rec.id);

        UPDATE ad_campaigns
        SET priority_score = daily_value,
            secondary_score = secondary,
            total_daily_value = daily_value,
            last_calculated_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = campaign_rec.id;
    END LOOP;
END;
$$;

COMMENT ON FUNCTION calculate_campaign_daily_value IS '광고 캠페인의 총 1일 가치 계산 (활성 결제들의 dailyValue 합)';
COMMENT ON PROCEDURE update_campaign_priorities IS '모든 활성 캠페인의 우선순위 재계산 (1일 가치 기반)';
