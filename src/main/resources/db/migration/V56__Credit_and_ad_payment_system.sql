-- =============================================
-- V56: 크레딧 결제 및 광고 시스템 확장
-- =============================================

-- 1. credits 테이블 컬럼 추가 (Entity와 매핑)
-- =============================================

-- available_credits 컬럼 추가 (balance와 동기화)
ALTER TABLE credits ADD COLUMN IF NOT EXISTS available_credits NUMERIC(12,2) DEFAULT 0;
UPDATE credits SET available_credits = balance WHERE available_credits IS NULL OR available_credits = 0;
ALTER TABLE credits ALTER COLUMN available_credits SET NOT NULL;

-- expiring_credits 컬럼 추가
ALTER TABLE credits ADD COLUMN IF NOT EXISTS expiring_credits NUMERIC(12,2) DEFAULT 0 NOT NULL;

-- expiring_at 컬럼 추가
ALTER TABLE credits ADD COLUMN IF NOT EXISTS expiring_at TIMESTAMP;

-- total_spent 컬럼 추가 (total_used와 동기화)
ALTER TABLE credits ADD COLUMN IF NOT EXISTS total_spent NUMERIC(12,2) DEFAULT 0;
UPDATE credits SET total_spent = total_used WHERE total_spent IS NULL OR total_spent = 0;
ALTER TABLE credits ALTER COLUMN total_spent SET NOT NULL;

-- is_deleted, deleted_at, metadata 추가 (BaseEntity)
ALTER TABLE credits ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE credits ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE credits ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

COMMENT ON COLUMN credits.available_credits IS '사용 가능 크레딧';
COMMENT ON COLUMN credits.expiring_credits IS '곧 만료되는 크레딧';
COMMENT ON COLUMN credits.expiring_at IS '만료 예정일';
COMMENT ON COLUMN credits.total_spent IS '총 사용 크레딧';


-- 2. credit_transactions 테이블 컬럼 추가
-- =============================================

-- entity_type, entity_id 컬럼 추가 (Entity 매핑용)
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50);
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS entity_id BIGINT;

-- reference_type/reference_id가 있으면 entity_type/entity_id로 복사
UPDATE credit_transactions
SET entity_type = reference_type, entity_id = reference_id
WHERE entity_type IS NULL AND reference_type IS NOT NULL;

-- updated_at 추가 (BaseTimeEntity)
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- is_deleted, deleted_at, metadata 추가 (BaseEntity)
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_credit_transactions_entity
ON credit_transactions(entity_type, entity_id);

COMMENT ON COLUMN credit_transactions.entity_type IS '관련 엔티티 타입 (PAYMENT, FILE_DOWNLOAD, AD_CAMPAIGN 등)';
COMMENT ON COLUMN credit_transactions.entity_id IS '관련 엔티티 ID';


-- 3. ad_payments 테이블 생성 (신규)
-- =============================================

CREATE TABLE IF NOT EXISTS ad_payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id),

    -- 결제 정보
    payment_amount NUMERIC(12,2) NOT NULL,
    payment_date DATE NOT NULL,

    -- 적용 기간
    apply_from_date DATE NOT NULL,
    apply_to_date DATE NOT NULL,
    apply_days INTEGER NOT NULL,

    -- 계산된 값
    daily_rate NUMERIC(12,4),          -- 일당 금액
    value_30d NUMERIC(12,2),           -- 30일 환산 가치

    -- 상태
    payment_type VARCHAR(20) NOT NULL DEFAULT 'INITIAL',  -- INITIAL, ADDITIONAL
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',         -- ACTIVE, CONSUMED, REFUNDED

    -- 관련 결제
    credit_transaction_id BIGINT REFERENCES credit_transactions(id),

    -- 감사 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}'
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_ad_payments_campaign_id ON ad_payments(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ad_payments_status ON ad_payments(status);
CREATE INDEX IF NOT EXISTS idx_ad_payments_apply_dates ON ad_payments(apply_from_date, apply_to_date);

COMMENT ON TABLE ad_payments IS '광고 캠페인 결제 내역';
COMMENT ON COLUMN ad_payments.payment_amount IS '결제 금액 (크레딧)';
COMMENT ON COLUMN ad_payments.payment_date IS '결제 일자';
COMMENT ON COLUMN ad_payments.apply_from_date IS '적용 시작일';
COMMENT ON COLUMN ad_payments.apply_to_date IS '적용 종료일';
COMMENT ON COLUMN ad_payments.apply_days IS '적용 일수';
COMMENT ON COLUMN ad_payments.daily_rate IS '일당 금액';
COMMENT ON COLUMN ad_payments.value_30d IS '30일 환산 가치';
COMMENT ON COLUMN ad_payments.payment_type IS '결제 유형: INITIAL(최초), ADDITIONAL(추가)';
COMMENT ON COLUMN ad_payments.status IS '상태: ACTIVE(활성), CONSUMED(소진), REFUNDED(환불)';


-- 4. ad_campaigns 테이블 수정
-- =============================================

-- 광고 기간 옵션 컬럼 추가
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS duration_days INTEGER DEFAULT 7;
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS min_daily_amount NUMERIC(12,2) DEFAULT 500;
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN DEFAULT FALSE;
ALTER TABLE ad_campaigns ADD COLUMN IF NOT EXISTS last_calculated_at TIMESTAMP;

COMMENT ON COLUMN ad_campaigns.duration_days IS '광고 기간 (7, 14, 30일)';
COMMENT ON COLUMN ad_campaigns.min_daily_amount IS '최소 일당 금액';
COMMENT ON COLUMN ad_campaigns.auto_renew IS '자동 갱신 여부';
COMMENT ON COLUMN ad_campaigns.last_calculated_at IS '마지막 우선순위 계산 시간';


-- 5. payments 테이블 수정
-- =============================================

-- payments 테이블에 is_deleted, metadata 추가 (없는 경우)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';


-- 6. 우선순위 계산 함수 업데이트
-- =============================================

-- 광고 캠페인 30일 환산 가치 계산 함수
CREATE OR REPLACE FUNCTION calculate_campaign_value_30d(campaign_id_param BIGINT)
RETURNS NUMERIC AS $$
DECLARE
    total_value NUMERIC(12,2);
    remaining_days INTEGER;
    campaign_end DATE;
    today DATE := CURRENT_DATE;
BEGIN
    -- 캠페인 종료일 조회
    SELECT end_date INTO campaign_end
    FROM ad_campaigns
    WHERE id = campaign_id_param;

    -- 남은 일수 계산 (최소 1일)
    remaining_days := GREATEST(1, campaign_end - today + 1);

    -- 활성 결제의 총 가치 계산
    SELECT COALESCE(SUM(payment_amount), 0) INTO total_value
    FROM ad_payments
    WHERE campaign_id = campaign_id_param
      AND status = 'ACTIVE'
      AND apply_to_date >= today;

    -- 30일 환산 가치 = (총 금액 / 남은 일수) * 30
    RETURN ROUND((total_value / remaining_days) * 30, 2);
END;
$$ LANGUAGE plpgsql;

-- 2차 점수 계산 함수 (동점 시 선등록자 우선)
CREATE OR REPLACE FUNCTION calculate_campaign_secondary_score(campaign_id_param BIGINT)
RETURNS NUMERIC AS $$
DECLARE
    campaign_record RECORD;
    company_record RECORD;
    score NUMERIC(12,2) := 0;
BEGIN
    -- 캠페인 정보 조회
    SELECT * INTO campaign_record
    FROM ad_campaigns
    WHERE id = campaign_id_param;

    -- 회사 정보 조회
    SELECT * INTO company_record
    FROM companies
    WHERE id = campaign_record.company_id;

    -- 점수 계산: 평점 * 1000 + 리뷰수 * 10 + 포트폴리오수 * 5
    score := COALESCE(company_record.avg_rating, 0) * 1000
           + COALESCE(company_record.review_count, 0) * 10
           + COALESCE(company_record.portfolio_count, 0) * 5;

    -- 선등록 보너스: 오래된 캠페인일수록 높은 점수
    -- (최대 타임스탬프 - 생성 시간) / 1000000 으로 정규화
    score := score + (EXTRACT(EPOCH FROM (TIMESTAMP '2100-01-01' - campaign_record.created_at)) / 1000000);

    RETURN ROUND(score, 2);
END;
$$ LANGUAGE plpgsql;

-- 캠페인 우선순위 업데이트 프로시저
CREATE OR REPLACE PROCEDURE update_campaign_priorities()
LANGUAGE plpgsql AS $$
DECLARE
    campaign_rec RECORD;
BEGIN
    FOR campaign_rec IN
        SELECT id FROM ad_campaigns
        WHERE status = 'ACTIVE'
          AND is_deleted = FALSE
          AND (end_date IS NULL OR end_date >= CURRENT_DATE)
    LOOP
        UPDATE ad_campaigns
        SET priority_score = calculate_campaign_value_30d(campaign_rec.id),
            secondary_score = calculate_campaign_secondary_score(campaign_rec.id),
            total_value_30d = calculate_campaign_value_30d(campaign_rec.id),
            last_calculated_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = campaign_rec.id;
    END LOOP;
END;
$$;

COMMENT ON FUNCTION calculate_campaign_value_30d IS '광고 캠페인의 30일 환산 가치 계산';
COMMENT ON FUNCTION calculate_campaign_secondary_score IS '광고 캠페인의 2차 점수 계산 (동점 처리용)';
COMMENT ON PROCEDURE update_campaign_priorities IS '모든 활성 캠페인의 우선순위 재계산';
