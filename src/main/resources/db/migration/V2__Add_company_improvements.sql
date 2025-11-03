-- ==============================================================================
-- V2__Add_company_improvements.sql
-- HIP Damoa (Hospital Interior Platform) 데이터베이스 개선사항 적용
-- 작성일: 2025-10-31
-- 내용: 업체 정보 확장, 광고 시스템 개선, 다모아 Pick 추가
-- 데이터베이스: hip_damoa_local (local) | hip_damoa_dev (dev) | hip_damoa_prod (prod)
-- ==============================================================================

-- ==============================================================================
-- 1. 업체 정보 확장
-- ==============================================================================

-- 1.1 companies 테이블 컬럼 추가
ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS detail_content TEXT,                    -- HTML/Markdown 에디터 내용
  ADD COLUMN IF NOT EXISTS detail_content_format VARCHAR(20) DEFAULT 'HTML', -- HTML, MARKDOWN
  ADD COLUMN IF NOT EXISTS primary_phone VARCHAR(20),              -- 대표 전화
  ADD COLUMN IF NOT EXISTS secondary_phone VARCHAR(20),            -- 보조 전화
  ADD COLUMN IF NOT EXISTS emergency_contact VARCHAR(20),          -- 긴급 연락처
  ADD COLUMN IF NOT EXISTS kakao_chat_url VARCHAR(500),           -- 카카오톡 채팅 URL
  ADD COLUMN IF NOT EXISTS social_links JSONB DEFAULT '{}'::JSONB, -- SNS 링크
  ADD COLUMN IF NOT EXISTS business_hours_note TEXT;              -- 영업시간 비고

COMMENT ON COLUMN companies.detail_content IS '업체 상세 소개 (HTML/Markdown 에디터 작성)';
COMMENT ON COLUMN companies.detail_content_format IS '상세 소개 형식 (HTML 또는 MARKDOWN)';
COMMENT ON COLUMN companies.primary_phone IS '대표 전화번호';
COMMENT ON COLUMN companies.secondary_phone IS '보조 전화번호';
COMMENT ON COLUMN companies.emergency_contact IS '긴급 연락처';
COMMENT ON COLUMN companies.kakao_chat_url IS '카카오톡 채팅 URL';
COMMENT ON COLUMN companies.social_links IS 'SNS 링크 {facebook, instagram, blog, youtube, naver_place 등}';
COMMENT ON COLUMN companies.business_hours_note IS '영업시간 특이사항';

-- 1.2 company_images 테이블 생성 (복수 이미지 관리)
CREATE TABLE IF NOT EXISTS company_images (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 이미지 정보
    image_url VARCHAR(500) NOT NULL,
    image_type VARCHAR(50), -- LOGO, COVER, GALLERY, INTERIOR, EXTERIOR, CERTIFICATE, PORTFOLIO
    title VARCHAR(200),
    description TEXT,

    -- 이미지 메타데이터
    width INT,
    height INT,
    file_size BIGINT,
    mime_type VARCHAR(100),

    -- 순서 및 상태
    display_order INT DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE, -- 대표 이미지
    is_active BOOLEAN DEFAULT TRUE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE company_images IS '업체 이미지 관리 (복수 이미지 지원)';
COMMENT ON COLUMN company_images.image_type IS '이미지 유형 (LOGO, COVER, GALLERY, INTERIOR 등)';
COMMENT ON COLUMN company_images.is_primary IS '대표 이미지 여부';

-- 인덱스 생성
CREATE INDEX idx_company_images_company_id ON company_images(company_id);
CREATE INDEX idx_company_images_type ON company_images(image_type);
CREATE INDEX idx_company_images_is_primary ON company_images(is_primary) WHERE is_primary = TRUE;

-- 트리거 추가
CREATE TRIGGER update_company_images_updated_at BEFORE UPDATE ON company_images
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==============================================================================
-- 2. 광고 시스템 개선 (30일 환산 가치 방식)
-- ==============================================================================

-- 2.1 ad_campaigns 테이블 컬럼 추가
ALTER TABLE ad_campaigns
  ADD COLUMN IF NOT EXISTS total_value_30d DECIMAL(12,2) DEFAULT 0,    -- 30일 환산 총 가치
  ADD COLUMN IF NOT EXISTS secondary_score DECIMAL(12,2) DEFAULT 0,    -- 2차 정렬 점수 (평점/리뷰)
  ADD COLUMN IF NOT EXISTS is_premium BOOLEAN DEFAULT FALSE,           -- 프리미엄 광고 여부
  ADD COLUMN IF NOT EXISTS premium_until TIMESTAMP;                    -- 프리미엄 종료일

-- priority_score 이미 존재하는 경우 타입 변경
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ad_campaigns'
                   AND column_name = 'priority_score') THEN
        ALTER TABLE ad_campaigns ADD COLUMN priority_score DECIMAL(12,2) DEFAULT 0;
    END IF;
END $$;

COMMENT ON COLUMN ad_campaigns.total_value_30d IS '30일 환산 총 광고 가치 (원)';
COMMENT ON COLUMN ad_campaigns.priority_score IS '우선순위 점수 (= total_value_30d)';
COMMENT ON COLUMN ad_campaigns.secondary_score IS '2차 정렬 점수 (평점*1000 + 리뷰수*10)';
COMMENT ON COLUMN ad_campaigns.is_premium IS '프리미엄 광고 여부';

-- 2.2 ad_payments 테이블 생성 (광고 결제 이력)
CREATE TABLE IF NOT EXISTS ad_payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,

    -- 결제 정보
    payment_amount DECIMAL(12,2) NOT NULL,           -- 결제 금액
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 결제일시

    -- 적용 기간
    apply_from_date DATE NOT NULL,                   -- 적용 시작일
    apply_to_date DATE NOT NULL,                     -- 적용 종료일
    apply_days INT NOT NULL,                         -- 적용 일수

    -- 계산값
    daily_rate DECIMAL(12,2) NOT NULL,               -- 일일 단가 (payment_amount / apply_days)
    value_30d DECIMAL(12,2) NOT NULL,                -- 30일 환산 가치 (daily_rate * 30)

    -- 결제 정보
    payment_type VARCHAR(50),                        -- INITIAL, ADDITIONAL, RENEWAL
    payment_method payment_method,                   -- 결제 수단
    transaction_id VARCHAR(255),                     -- 거래 ID

    -- 상태
    status VARCHAR(50) DEFAULT 'ACTIVE',             -- ACTIVE, EXPIRED, REFUNDED, CANCELLED

    -- 환불 정보
    refund_amount DECIMAL(12,2) DEFAULT 0,
    refund_date TIMESTAMP,
    refund_reason TEXT,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id)
);

COMMENT ON TABLE ad_payments IS '광고 결제 이력 - 추가 결제 관리';
COMMENT ON COLUMN ad_payments.value_30d IS '30일 환산 가치 = (결제금액/적용일수) * 30';
COMMENT ON COLUMN ad_payments.payment_type IS '결제 유형 (INITIAL:최초, ADDITIONAL:추가, RENEWAL:갱신)';

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_ad_payments_campaign_id ON ad_payments(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ad_payments_status ON ad_payments(status);
CREATE INDEX IF NOT EXISTS idx_ad_payments_apply_dates ON ad_payments(start_date, end_date);

-- 2.3 ad_daily_snapshots 테이블 생성 (일별 광고 스냅샷)
CREATE TABLE IF NOT EXISTS ad_daily_snapshots (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,

    -- 해당일 광고비 정보
    daily_budget DECIMAL(12,2),                      -- 해당일 적용 광고비
    accumulated_budget DECIMAL(12,2),                -- 누적 광고비
    value_30d DECIMAL(12,2),                         -- 해당일 30일 환산 가치

    -- 우선순위 정보
    priority_score DECIMAL(12,2),                    -- 해당일 우선순위 점수
    secondary_score DECIMAL(12,2),                   -- 해당일 2차 점수
    daily_rank INT,                                  -- 해당일 순위

    -- 성과 지표
    impressions INT DEFAULT 0,                       -- 노출수
    clicks INT DEFAULT 0,                            -- 클릭수
    conversions INT DEFAULT 0,                       -- 전환수

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(campaign_id, snapshot_date)
);

COMMENT ON TABLE ad_daily_snapshots IS '일별 광고 스냅샷 - 일별 순위 및 성과 추적';

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_ad_daily_snapshots_campaign_date ON ad_daily_snapshots(campaign_id, snapshot_date DESC);
CREATE INDEX IF NOT EXISTS idx_ad_daily_snapshots_date ON ad_daily_snapshots(snapshot_date DESC);

-- ==============================================================================
-- 3. 다모아 Pick 시스템
-- ==============================================================================

-- 3.1 Pick 유형 ENUM 생성
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'damoa_pick_type') THEN
        CREATE TYPE damoa_pick_type AS ENUM (
            'SPONSORED',      -- 후원 업체
            'OPERATED',       -- 직접 운영 업체
            'PARTNER'         -- 파트너 업체 (렌탈, 침구 등)
        );
    END IF;
END $$;

-- 3.2 damoa_picks 테이블 생성
CREATE TABLE IF NOT EXISTS damoa_picks (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- Pick 정보
    pick_type VARCHAR(50) NOT NULL,                  -- SPONSORED, OPERATED, PARTNER
    season VARCHAR(50) NOT NULL,                     -- 2024_SPRING, 2024_SUMMER, 2024_FALL, 2024_WINTER
    title VARCHAR(200),                              -- Pick 제목
    description TEXT,                                 -- Pick 설명

    -- 이미지
    main_image_url VARCHAR(500),                     -- 메인 이미지
    banner_image_url VARCHAR(500),                   -- 배너 이미지
    thumbnail_url VARCHAR(500),                      -- 썸네일
    images text[] DEFAULT '{}',                      -- 추가 이미지 배열

    -- 표시 정보
    badge_text VARCHAR(50),                          -- 배지 텍스트 (예: "다모아 추천", "공식 파트너")
    badge_color VARCHAR(20),                         -- 배지 색상 (HEX 코드)
    highlight_text TEXT,                             -- 강조 문구

    -- 링크
    landing_url VARCHAR(500),                        -- 클릭 시 이동할 URL

    -- 순서 및 기간
    display_order INT DEFAULT 0,                     -- 표시 순서
    start_date DATE NOT NULL,                        -- 시작일
    end_date DATE,                                    -- 종료일 (NULL이면 무기한)

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,                  -- 활성 상태

    -- 통계
    view_count INT DEFAULT 0,                        -- 조회수
    click_count INT DEFAULT 0,                       -- 클릭수

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE damoa_picks IS '다모아 Pick - 시즌별 추천 업체';
COMMENT ON COLUMN damoa_picks.pick_type IS 'Pick 유형 (SPONSORED:후원, OPERATED:직영, PARTNER:파트너)';
COMMENT ON COLUMN damoa_picks.season IS '시즌 (YYYY_SEASON 형식)';
COMMENT ON COLUMN damoa_picks.images IS '추가 이미지 URL 배열';
COMMENT ON COLUMN damoa_picks.badge_text IS '배지에 표시될 텍스트';
COMMENT ON COLUMN damoa_picks.badge_color IS '배지 색상 (예: #FF6B6B)';

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_damoa_picks_company_id ON damoa_picks(company_id);
CREATE INDEX IF NOT EXISTS idx_damoa_picks_pick_type ON damoa_picks(pick_type);
CREATE INDEX IF NOT EXISTS idx_damoa_picks_season ON damoa_picks(season);
CREATE INDEX IF NOT EXISTS idx_damoa_picks_active ON damoa_picks(is_active, start_date, end_date)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_damoa_picks_display_order ON damoa_picks(display_order, created_at);

-- 트리거 추가
CREATE TRIGGER update_damoa_picks_updated_at BEFORE UPDATE ON damoa_picks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==============================================================================
-- 4. 계산 함수 및 프로시저
-- ==============================================================================

-- 4.1 30일 환산 총 가치 계산 함수
CREATE OR REPLACE FUNCTION calculate_total_value_30d(p_campaign_id BIGINT)
RETURNS DECIMAL AS $$
DECLARE
    v_total_value DECIMAL(12,2) := 0;
BEGIN
    -- 모든 활성 결제의 30일 환산 가치 합계
    SELECT COALESCE(SUM(value_30d), 0)
    INTO v_total_value
    FROM ad_payments
    WHERE campaign_id = p_campaign_id
      AND status = 'ACTIVE';

    RETURN v_total_value;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_total_value_30d IS '캠페인의 30일 환산 총 가치 계산';

-- 4.2 2차 정렬 점수 계산 함수 (평점, 리뷰 등)
CREATE OR REPLACE FUNCTION calculate_secondary_score(p_company_id BIGINT)
RETURNS DECIMAL AS $$
DECLARE
    v_company RECORD;
    v_score DECIMAL(12,2);
BEGIN
    SELECT
        avg_rating,
        review_count,
        portfolio_count,
        completed_count
    INTO v_company
    FROM companies
    WHERE id = p_company_id;

    -- 평점(0-5) * 1000 + 리뷰수 * 10 + 포트폴리오 * 5 + 완료건수
    v_score :=
        (COALESCE(v_company.avg_rating, 0) * 1000) +
        (COALESCE(v_company.review_count, 0) * 10) +
        (COALESCE(v_company.portfolio_count, 0) * 5) +
        COALESCE(v_company.completed_count, 0);

    RETURN v_score;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_secondary_score IS '2차 정렬 점수 계산 (평점, 리뷰 기반)';

-- 4.3 추가 결제 처리 프로시저
CREATE OR REPLACE PROCEDURE process_additional_payment(
    p_campaign_id BIGINT,
    p_payment_amount DECIMAL(12,2),
    p_payment_date DATE DEFAULT CURRENT_DATE
) AS $$
DECLARE
    v_campaign_end_date DATE;
    v_remaining_days INT;
    v_daily_rate DECIMAL(12,2);
    v_value_30d DECIMAL(12,2);
    v_company_id BIGINT;
BEGIN
    -- 캠페인 종료일 및 회사 ID 조회
    SELECT
        COALESCE(MAX(ap.apply_to_date), ac.end_date),
        ac.company_id
    INTO v_campaign_end_date, v_company_id
    FROM ad_campaigns ac
    LEFT JOIN ad_payments ap ON ac.id = ap.campaign_id AND ap.status = 'ACTIVE'
    WHERE ac.id = p_campaign_id
    GROUP BY ac.company_id, ac.end_date;

    -- 캠페인이 없는 경우
    IF v_company_id IS NULL THEN
        RAISE EXCEPTION '캠페인을 찾을 수 없습니다 (ID: %)', p_campaign_id;
    END IF;

    -- 종료일이 없는 경우 30일로 설정
    IF v_campaign_end_date IS NULL THEN
        v_campaign_end_date := p_payment_date + INTERVAL '30 days';
    END IF;

    -- 남은 일수 계산
    v_remaining_days := v_campaign_end_date - p_payment_date + 1;

    IF v_remaining_days <= 0 THEN
        RAISE EXCEPTION '캠페인이 이미 종료되었습니다';
    END IF;

    -- 일일 단가 및 30일 환산 가치 계산
    v_daily_rate := p_payment_amount / v_remaining_days;
    v_value_30d := v_daily_rate * 30;

    -- 결제 이력 추가
    INSERT INTO ad_payments (
        campaign_id,
        payment_amount,
        payment_date,
        apply_from_date,
        apply_to_date,
        apply_days,
        daily_rate,
        value_30d,
        payment_type,
        status
    ) VALUES (
        p_campaign_id,
        p_payment_amount,
        CURRENT_TIMESTAMP,
        p_payment_date,
        v_campaign_end_date,
        v_remaining_days,
        v_daily_rate,
        v_value_30d,
        'ADDITIONAL',
        'ACTIVE'
    );

    -- 캠페인 총 가치 및 우선순위 업데이트
    UPDATE ad_campaigns
    SET total_value_30d = calculate_total_value_30d(p_campaign_id),
        priority_score = calculate_total_value_30d(p_campaign_id),
        secondary_score = calculate_secondary_score(v_company_id),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_campaign_id;

    COMMIT;
END;
$$ LANGUAGE plpgsql;

COMMENT ON PROCEDURE process_additional_payment IS '광고 추가 결제 처리 (30일 환산 가치 재계산)';

-- 4.4 초기 광고 캠페인 생성 프로시저
CREATE OR REPLACE PROCEDURE create_ad_campaign_with_payment(
    p_company_id BIGINT,
    p_payment_amount DECIMAL(12,2),
    p_duration_days INT DEFAULT 30,
    p_ad_type ad_type DEFAULT 'LISTING'
) AS $$
DECLARE
    v_campaign_id BIGINT;
    v_daily_rate DECIMAL(12,2);
    v_value_30d DECIMAL(12,2);
BEGIN
    -- 일일 단가 및 30일 환산 가치 계산
    v_daily_rate := p_payment_amount / p_duration_days;
    v_value_30d := v_daily_rate * 30;

    -- 광고 캠페인 생성
    INSERT INTO ad_campaigns (
        company_id,
        ad_type,
        status,
        start_date,
        end_date,
        budget_amount,
        total_value_30d,
        priority_score,
        secondary_score
    ) VALUES (
        p_company_id,
        p_ad_type,
        'ACTIVE',
        CURRENT_DATE,
        CURRENT_DATE + (p_duration_days || ' days')::INTERVAL,
        p_payment_amount,
        v_value_30d,
        v_value_30d,
        calculate_secondary_score(p_company_id)
    ) RETURNING id INTO v_campaign_id;

    -- 초기 결제 이력 추가
    INSERT INTO ad_payments (
        campaign_id,
        payment_amount,
        apply_from_date,
        apply_to_date,
        apply_days,
        daily_rate,
        value_30d,
        payment_type,
        status
    ) VALUES (
        v_campaign_id,
        p_payment_amount,
        CURRENT_DATE,
        CURRENT_DATE + (p_duration_days - 1),
        p_duration_days,
        v_daily_rate,
        v_value_30d,
        'INITIAL',
        'ACTIVE'
    );

    COMMIT;
END;
$$ LANGUAGE plpgsql;

COMMENT ON PROCEDURE create_ad_campaign_with_payment IS '광고 캠페인 생성 및 초기 결제 처리';

-- ==============================================================================
-- 5. 뷰(View) 생성 - 광고 우선순위 조회
-- ==============================================================================

CREATE OR REPLACE VIEW v_company_rankings AS
WITH ranked_companies AS (
    SELECT
        c.id,
        c.uuid,
        c.name,
        c.description,
        c.logo_url,
        c.avg_rating,
        c.review_count,
        c.service_areas,
        c.tags,

        -- 광고 정보
        ac.id as campaign_id,
        ac.total_value_30d,
        ac.priority_score,
        ac.secondary_score,
        ac.is_premium,

        -- 다모아 Pick 정보
        dp.pick_type,
        dp.season,
        dp.badge_text,
        dp.badge_color,
        dp.display_order as pick_order,

        -- 티어 구분
        CASE
            WHEN dp.pick_type IS NOT NULL THEN 0  -- 다모아 Pick
            WHEN ac.priority_score > 0 THEN 1     -- 유료 광고
            ELSE 2                                 -- 무료
        END as tier,

        -- 티어별 정렬 값
        CASE
            WHEN dp.pick_type IS NOT NULL THEN dp.display_order
            WHEN ac.priority_score > 0 THEN -ac.priority_score  -- 음수로 변환 (큰 값이 우선)
            ELSE -c.avg_rating * 1000                            -- 음수로 변환
        END as tier_sort_value

    FROM companies c

    -- 활성 광고 캠페인 조인
    LEFT JOIN LATERAL (
        SELECT *
        FROM ad_campaigns
        WHERE company_id = c.id
          AND status = 'ACTIVE'
          AND start_date <= CURRENT_DATE
          AND (end_date IS NULL OR end_date >= CURRENT_DATE)
          AND is_deleted = FALSE
        ORDER BY priority_score DESC
        LIMIT 1
    ) ac ON TRUE

    -- 활성 다모아 Pick 조인
    LEFT JOIN LATERAL (
        SELECT *
        FROM damoa_picks
        WHERE company_id = c.id
          AND is_active = TRUE
          AND start_date <= CURRENT_DATE
          AND (end_date IS NULL OR end_date >= CURRENT_DATE)
          AND is_deleted = FALSE
        ORDER BY display_order
        LIMIT 1
    ) dp ON TRUE

    WHERE c.is_deleted = FALSE
)
SELECT
    id,
    uuid,
    name,
    description,
    logo_url,
    avg_rating,
    review_count,
    service_areas,
    tags,

    -- 광고 정보
    campaign_id,
    COALESCE(total_value_30d, 0) as total_value_30d,
    COALESCE(priority_score, 0) as priority_score,
    COALESCE(secondary_score, 0) as secondary_score,
    COALESCE(is_premium, FALSE) as is_premium,

    -- 다모아 Pick 정보
    pick_type,
    season,
    badge_text,
    badge_color,

    -- 랭킹 정보
    CASE tier
        WHEN 0 THEN '다모아 Pick'
        WHEN 1 THEN '유료 광고'
        ELSE '무료'
    END as tier_name,

    ROW_NUMBER() OVER (ORDER BY tier, tier_sort_value) as rank

FROM ranked_companies
ORDER BY tier, tier_sort_value;

COMMENT ON VIEW v_company_rankings IS '업체 우선순위 랭킹 뷰 (Pick > 유료광고 > 무료)';

-- ==============================================================================
-- 6. 샘플 데이터 및 사용 예시
-- ==============================================================================

-- 샘플 1: 광고 캠페인 생성 (30일 30만원)
/*
CALL create_ad_campaign_with_payment(
    p_company_id := 1,
    p_payment_amount := 300000,
    p_duration_days := 30,
    p_ad_type := 'LISTING'
);
*/

-- 샘플 2: 추가 결제 처리 (10일 후 5만원 추가)
/*
CALL process_additional_payment(
    p_campaign_id := 1,
    p_payment_amount := 50000,
    p_payment_date := CURRENT_DATE + 10
);
*/

-- 샘플 3: 다모아 Pick 등록
/*
INSERT INTO damoa_picks (
    company_id, pick_type, season, title, description,
    main_image_url, banner_image_url, badge_text, badge_color,
    start_date, end_date, display_order
) VALUES (
    1, 'SPONSORED', '2024_WINTER', '겨울 추천 업체', '다모아가 추천하는 겨울 인테리어 전문 업체',
    'https://example.com/main.jpg', 'https://example.com/banner.jpg',
    '다모아 추천', '#FF6B6B',
    '2024-12-01', '2025-02-28', 1
);
*/

-- 샘플 4: 업체 랭킹 조회
/*
SELECT
    rank as "순위",
    name as "업체명",
    tier_name as "구분",
    total_value_30d as "30일환산가치",
    priority_score as "우선순위점수",
    avg_rating as "평점",
    review_count as "리뷰수"
FROM v_company_rankings
LIMIT 20;
*/

-- ==============================================================================
-- 계산 예시 설명
-- ==============================================================================
/*
예시 1: A사 30일 30만원
  - 30일 환산 가치: (30만원 ÷ 30일) × 30 = 30만원
  - 우선순위 점수: 300,000

예시 2: B사 30일 29만원 + 10일 후 1만원 추가
  - 초기: (29만원 ÷ 30일) × 30 = 29만원
  - 추가: (1만원 ÷ 20일) × 30 = 1.5만원
  - 총 30일 환산 가치: 30.5만원
  - 우선순위 점수: 305,000

→ B사가 A사보다 높은 우선순위 (305,000 > 300,000)

동점일 경우: secondary_score (평점×1000 + 리뷰수×10) 순으로 정렬
*/

-- ==============================================================================
-- 완료
-- ==============================================================================