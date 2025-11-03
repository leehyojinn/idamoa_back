-- ==============================================================================
-- V1__Initial_schema.sql
-- HIP Damoa (Hospital Interior Platform) 데이터베이스 초기 스키마
-- 생성일: 2025-10-31
-- 테이블 수: 55개
-- 데이터베이스: hip_damoa_local (local) | hip_damoa_dev (dev) | hip_damoa_prod (prod)
-- ==============================================================================

-- ==============================================================================
-- ENUM 타입 정의
-- ==============================================================================

-- 사용자 역할
CREATE TYPE user_role AS ENUM (
    'USER',           -- 일반 사용자
    'COMPANY',        -- 업체 사용자
    'DESIGNER',       -- 디자이너
    'ADMIN',          -- 관리자
    'SUPER_ADMIN'     -- 최고 관리자
);

-- 사용자 상태
CREATE TYPE user_status AS ENUM (
    'PENDING',        -- 가입 대기
    'ACTIVE',         -- 활성
    'INACTIVE',       -- 비활성
    'SUSPENDED',      -- 정지
    'DELETED'         -- 삭제
);

-- 광고 타입
CREATE TYPE ad_type AS ENUM (
    'LISTING',           -- 상위 노출 광고 (입찰 기반)
    'AI_RECOMMENDATION', -- AI 추천 가중치
    'BANNER',            -- 배너 광고
    'POPUP'              -- 팝업 광고
);

-- 광고 상태
CREATE TYPE ad_status AS ENUM (
    'DRAFT',          -- 초안
    'PENDING',        -- 승인 대기
    'APPROVED',       -- 승인됨
    'ACTIVE',         -- 진행중
    'PAUSED',         -- 일시정지
    'COMPLETED',      -- 완료
    'REJECTED'        -- 거부됨
);

-- 견적 요청 상태
CREATE TYPE estimate_status AS ENUM (
    'DRAFT',          -- 작성중
    'PUBLISHED',      -- 공개됨
    'IN_PROGRESS',    -- 진행중
    'MATCHED',        -- 매칭됨
    'COMPLETED',      -- 완료
    'CANCELLED'       -- 취소됨
);

-- 견적 제안 상태
CREATE TYPE proposal_status AS ENUM (
    'DRAFT',          -- 작성중
    'SUBMITTED',      -- 제출됨
    'ACCEPTED',       -- 수락됨
    'REJECTED',       -- 거절됨
    'WITHDRAWN'       -- 철회됨
);

-- 게시판 타입
CREATE TYPE board_type AS ENUM (
    'NOTICE',         -- 공지사항
    'EVENT',          -- 이벤트
    'FAQ',            -- 자주 묻는 질문
    'GALLERY',        -- 갤러리
    'DOCUMENT'        -- 자료실
);

-- 결제 상태
CREATE TYPE payment_status AS ENUM (
    'PENDING',        -- 대기중
    'PROCESSING',     -- 처리중
    'COMPLETED',      -- 완료
    'FAILED',         -- 실패
    'CANCELLED',      -- 취소
    'REFUNDED',       -- 환불됨
    'PARTIAL_REFUNDED' -- 부분 환불
);

-- 결제 방법
CREATE TYPE payment_method AS ENUM (
    'CARD',           -- 카드
    'BANK_TRANSFER',  -- 계좌이체
    'VIRTUAL_ACCOUNT', -- 가상계좌
    'PHONE',          -- 휴대폰
    'KAKAO_PAY',      -- 카카오페이
    'NAVER_PAY',      -- 네이버페이
    'TOSS',           -- 토스
    'CREDIT'          -- 크레딧
);

-- 알림 채널
CREATE TYPE notification_channel AS ENUM (
    'EMAIL',          -- 이메일
    'SMS',            -- SMS
    'PUSH',           -- 푸시 알림
    'KAKAO',          -- 카카오톡
    'IN_APP'          -- 인앱 알림
);

-- 파일 타입
CREATE TYPE file_type AS ENUM (
    'IMAGE',          -- 이미지
    'VIDEO',          -- 비디오
    'DOCUMENT',       -- 문서
    'AUDIO',          -- 오디오
    'OTHER'           -- 기타
);

-- ==============================================================================
-- UUID 확장 활성화
-- ==============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Full-text search 지원

-- ==============================================================================
-- 공통 함수 정의
-- ==============================================================================

-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================================
-- 1. 사용자 도메인 (7개 테이블)
-- ==============================================================================

-- 1.1 users: 사용자 기본 정보 및 인증
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 로그인 정보
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role user_role DEFAULT 'USER' NOT NULL,
    status user_status DEFAULT 'PENDING' NOT NULL,

    -- 인증 상태
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    phone_verified BOOLEAN DEFAULT FALSE,
    phone_verified_at TIMESTAMP,
    identity_verified BOOLEAN DEFAULT FALSE,
    identity_verified_at TIMESTAMP,

    -- 로그인 추적
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    login_count INT DEFAULT 0,
    failed_login_count INT DEFAULT 0,
    locked_until TIMESTAMP,

    -- 약관 동의
    terms_agreed_at TIMESTAMP,
    privacy_agreed_at TIMESTAMP,
    marketing_agreed_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES users(id),

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE users IS '사용자 기본 정보 및 인증 테이블';
COMMENT ON COLUMN users.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN users.role IS '사용자 역할 (일반/업체/디자이너/관리자)';
COMMENT ON COLUMN users.metadata IS '확장 데이터 (JSON 형식)';

-- 1.2 user_profiles: 사용자 프로필 정보
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 기본 정보
    name VARCHAR(100),
    nickname VARCHAR(50) UNIQUE,
    bio TEXT,
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    birth_date DATE,
    gender VARCHAR(10),

    -- 주소 정보
    address VARCHAR(500),
    address_detail VARCHAR(200),
    postal_code VARCHAR(10),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),

    -- 소셜 링크 및 관심사
    social_links JSONB DEFAULT '{}'::JSONB,
    interests text[] DEFAULT '{}',

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE user_profiles IS '사용자 프로필 확장 정보';
COMMENT ON COLUMN user_profiles.social_links IS '소셜 미디어 링크 {facebook, instagram, blog 등}';
COMMENT ON COLUMN user_profiles.interests IS '관심 분야 태그 배열';

-- 1.3 user_settings: 사용자 설정
CREATE TABLE user_settings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 알림 설정
    notification_settings JSONB DEFAULT '{}'::JSONB,

    -- 개인화 설정
    preferences JSONB DEFAULT '{}'::JSONB,

    -- UI/UX 설정
    ui_settings JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE user_settings IS '사용자 개인 설정';
COMMENT ON COLUMN user_settings.notification_settings IS '알림 설정 {email: true, sms: false, ...}';
COMMENT ON COLUMN user_settings.preferences IS '개인화 설정 {language, timezone, ...}';

-- 1.4 user_activity_logs: 사용자 활동 로그
CREATE TABLE user_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- 활동 정보
    activity_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    action VARCHAR(100),

    -- 추가 정보
    ip_address VARCHAR(45),
    user_agent TEXT,
    referer TEXT,
    session_id VARCHAR(255),

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB,

    -- 시간
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE user_activity_logs IS '사용자 활동 추적 로그';
COMMENT ON COLUMN user_activity_logs.activity_type IS '활동 유형 (page_view, click, search 등)';
COMMENT ON COLUMN user_activity_logs.metadata IS '활동 상세 데이터';

-- 인덱스 생성
CREATE INDEX idx_user_activity_logs_user_id ON user_activity_logs(user_id);
CREATE INDEX idx_user_activity_logs_created_at ON user_activity_logs(created_at DESC);
CREATE INDEX idx_user_activity_logs_activity_type ON user_activity_logs(activity_type);

-- 1.5 user_devices: 사용자 디바이스 정보
CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 디바이스 정보
    device_id VARCHAR(255) UNIQUE NOT NULL,
    device_type VARCHAR(50),
    device_name VARCHAR(100),
    os VARCHAR(50),
    os_version VARCHAR(50),
    app_version VARCHAR(50),

    -- 푸시 토큰
    push_token VARCHAR(500),
    push_enabled BOOLEAN DEFAULT TRUE,

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,
    last_active_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE user_devices IS '사용자 디바이스 및 푸시 토큰 관리';

-- 1.6 social_accounts: 소셜 로그인 연동
CREATE TABLE social_accounts (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 소셜 정보
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    provider_name VARCHAR(100),
    provider_image VARCHAR(500),

    -- 토큰
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP,

    -- 추가 데이터
    raw_data JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE(provider, provider_id)
);

COMMENT ON TABLE social_accounts IS '소셜 로그인 연동 정보';

-- 1.7 user_points: 포인트/마일리지
CREATE TABLE user_points (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 포인트 잔액
    balance INT DEFAULT 0 NOT NULL,
    total_earned INT DEFAULT 0 NOT NULL,
    total_used INT DEFAULT 0 NOT NULL,

    -- 등급/레벨
    level INT DEFAULT 1,
    experience INT DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE user_points IS '사용자 포인트/마일리지 관리';

-- ==============================================================================
-- 2. 업체 도메인 (7개 테이블)
-- ==============================================================================

-- 2.1 companies: 업체 정보 (통합)
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 기본 정보
    name VARCHAR(200) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    website_url VARCHAR(500),

    -- 사업자 정보 (JSONB)
    business_info JSONB DEFAULT '{}'::JSONB,
    /* {
        registration_number: '123-45-67890',
        ceo_name: '홍길동',
        business_type: '서비스업',
        business_item: '인테리어',
        establishment_date: '2020-01-01',
        employee_count: 10
    } */

    -- 영업시간 (JSONB)
    business_hours JSONB DEFAULT '{}'::JSONB,
    /* {
        mon: {open: '09:00', close: '18:00', is_closed: false},
        tue: {open: '09:00', close: '18:00', is_closed: false},
        ...
        holiday_closed: true
    } */

    -- 서비스 지역 (배열)
    service_areas text[] DEFAULT '{}',

    -- 좌표 (JSONB)
    coordinates JSONB DEFAULT '{}'::JSONB,
    /* {lat: 37.5665, lng: 126.9780, address: '서울시 중구...'} */

    -- 연락처
    phone VARCHAR(20),
    email VARCHAR(255),
    fax VARCHAR(20),

    -- 태그/키워드 (배열)
    tags text[] DEFAULT '{}',
    keywords text[] DEFAULT '{}',
    specialties text[] DEFAULT '{}',

    -- 통계
    avg_rating DECIMAL(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    portfolio_count INT DEFAULT 0,
    completed_count INT DEFAULT 0,

    -- 인증 상태
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    is_premium BOOLEAN DEFAULT FALSE,
    premium_until TIMESTAMP,

    -- 설정
    settings JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE companies IS '업체 통합 정보';
COMMENT ON COLUMN companies.business_info IS '사업자 정보 JSON';
COMMENT ON COLUMN companies.business_hours IS '영업시간 JSON';
COMMENT ON COLUMN companies.service_areas IS '서비스 제공 지역 배열';
COMMENT ON COLUMN companies.tags IS '업체 태그/해시태그';
COMMENT ON COLUMN companies.keywords IS '검색 키워드';

-- 2.2 company_services: 제공 서비스
CREATE TABLE company_services (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 서비스 정보
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),

    -- 가격 정보
    price_type VARCHAR(50), -- FIXED, HOURLY, PROJECT, NEGOTIABLE
    min_price DECIMAL(12,2),
    max_price DECIMAL(12,2),
    price_unit VARCHAR(50),

    -- 서비스 옵션
    options JSONB DEFAULT '{}'::JSONB,

    -- 이미지
    images text[] DEFAULT '{}',

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE company_services IS '업체 제공 서비스';

-- 2.3 company_portfolios: 포트폴리오
CREATE TABLE company_portfolios (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 포트폴리오 정보
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),

    -- 프로젝트 정보
    project_type VARCHAR(100),
    project_scale VARCHAR(50),
    project_duration INT, -- 일 단위
    project_date DATE,

    -- 비용
    budget_range VARCHAR(50),
    actual_cost DECIMAL(12,2),

    -- 이미지/동영상
    images text[] DEFAULT '{}',
    videos text[] DEFAULT '{}',
    thumbnail_url VARCHAR(500),

    -- 태그
    tags text[] DEFAULT '{}',

    -- 통계
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,

    -- 상태
    is_featured BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE company_portfolios IS '업체 포트폴리오';

-- 2.4 company_reviews: 리뷰/평점
CREATE TABLE company_reviews (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id BIGINT, -- 매칭 ID (있는 경우)

    -- 평점
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),

    -- 세부 평점
    quality_rating INT CHECK (quality_rating >= 1 AND quality_rating <= 5),
    price_rating INT CHECK (price_rating >= 1 AND price_rating <= 5),
    service_rating INT CHECK (service_rating >= 1 AND service_rating <= 5),
    time_rating INT CHECK (time_rating >= 1 AND time_rating <= 5),

    -- 리뷰 내용
    title VARCHAR(200),
    content TEXT NOT NULL,

    -- 이미지
    images text[] DEFAULT '{}',

    -- 업체 답변
    reply TEXT,
    reply_at TIMESTAMP,

    -- 상태
    is_verified BOOLEAN DEFAULT FALSE,
    is_reported BOOLEAN DEFAULT FALSE,
    is_hidden BOOLEAN DEFAULT FALSE,

    -- 추천
    helpful_count INT DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE company_reviews IS '업체 리뷰 및 평점';

-- 2.5 company_certifications: 자격증/인증
CREATE TABLE company_certifications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 인증 정보
    cert_type VARCHAR(100) NOT NULL,
    cert_name VARCHAR(200) NOT NULL,
    cert_number VARCHAR(100),
    issuer VARCHAR(200),

    -- 유효기간
    issue_date DATE,
    expiry_date DATE,

    -- 검증
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    verified_by BIGINT REFERENCES users(id),

    -- 첨부파일
    document_url VARCHAR(500),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE company_certifications IS '업체 자격증 및 인증';

-- 2.6 company_branches: 지점 정보
CREATE TABLE company_branches (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 지점 정보
    name VARCHAR(200) NOT NULL,
    branch_type VARCHAR(50), -- MAIN, SUB, FRANCHISE

    -- 주소
    address VARCHAR(500),
    address_detail VARCHAR(200),
    postal_code VARCHAR(10),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),

    -- 연락처
    phone VARCHAR(20),
    email VARCHAR(255),
    manager_name VARCHAR(100),

    -- 영업시간
    business_hours JSONB DEFAULT '{}'::JSONB,

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE company_branches IS '업체 지점 정보';

-- 2.7 company_statistics: 업체 통계
CREATE TABLE company_statistics (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 집계 기간
    stat_date DATE NOT NULL,
    stat_type VARCHAR(50) DEFAULT 'DAILY', -- DAILY, MONTHLY

    -- 조회 통계
    profile_views INT DEFAULT 0,
    portfolio_views INT DEFAULT 0,
    service_views INT DEFAULT 0,

    -- 활동 통계
    estimate_requests INT DEFAULT 0,
    estimate_proposals INT DEFAULT 0,
    matches INT DEFAULT 0,
    reviews INT DEFAULT 0,

    -- 광고 통계
    ad_impressions INT DEFAULT 0,
    ad_clicks INT DEFAULT 0,

    -- 매출 통계
    revenue DECIMAL(12,2) DEFAULT 0,

    -- 상세 데이터
    daily_data JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE(company_id, stat_date, stat_type)
);

COMMENT ON TABLE company_statistics IS '업체 통계 데이터';

-- ==============================================================================
-- 3. 광고 시스템 (5개 테이블)
-- ==============================================================================

-- 3.1 ad_campaigns: 통합 광고 관리
CREATE TABLE ad_campaigns (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 광고 기본 정보
    name VARCHAR(200) NOT NULL,
    description TEXT,
    ad_type VARCHAR(30) NOT NULL, -- LISTING, AI_RECOMMENDATION, BANNER, POPUP
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, PAUSED, COMPLETED, CANCELLED

    -- 타입별 설정 (JSONB)
    ad_config JSONB DEFAULT '{}'::JSONB,
    /*
    LISTING: {bid_amount: 1000, daily_budget: 50000, position_boost: 1}
    AI_RECOMMENDATION: {partnership_level: 'GOLD', weight: 1.5, categories: ['인테리어']}
    BANNER: {position: 'MAIN_TOP', size: '728x90', image_url: '...', link_url: '...'}
    POPUP: {trigger: 'ON_LOAD', delay: 3000, frequency: 'ONCE_PER_DAY', size: '500x600'}
    */

    -- 타겟팅 설정
    targeting JSONB DEFAULT '{}'::JSONB,
    /* {
        locations: ['서울', '경기'],
        age_range: {min: 25, max: 45},
        gender: 'ALL',
        interests: ['인테리어', '리모델링'],
        devices: ['MOBILE', 'PC']
    } */

    -- 예산 관리
    budget_type VARCHAR(20), -- DAILY, TOTAL, UNLIMITED
    budget_amount DECIMAL(12,2),
    daily_budget DECIMAL(12,2),
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- 가치 및 우선순위
    total_value_30d DECIMAL(12,2) NOT NULL DEFAULT 0,
    priority_score DECIMAL(12,2) NOT NULL DEFAULT 0,
    secondary_score DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- 프리미엄
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    premium_until TIMESTAMP,

    -- 캠페인 기간
    start_date DATE NOT NULL,
    end_date DATE,

    -- 성과 지표
    total_impressions BIGINT NOT NULL DEFAULT 0,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    total_conversions BIGINT NOT NULL DEFAULT 0,

    -- 기본 필드 (BaseEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE ad_campaigns IS '통합 광고 캠페인 관리';
COMMENT ON COLUMN ad_campaigns.ad_type IS '광고 타입 (상위노출/AI추천/배너/팝업)';
COMMENT ON COLUMN ad_campaigns.ad_config IS '광고 타입별 상세 설정';
COMMENT ON COLUMN ad_campaigns.targeting IS '타겟팅 설정';

-- 3.2 ad_creatives: 광고 소재
CREATE TABLE ad_creatives (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,

    -- 소재 정보
    name VARCHAR(200),
    creative_type VARCHAR(50), -- IMAGE, VIDEO, TEXT, HTML
    creative_size VARCHAR(50), -- 728x90, 300x250, etc.
    creative_format VARCHAR(20), -- JPG, PNG, GIF, MP4, etc.

    -- 콘텐츠
    title VARCHAR(200),
    description TEXT,
    call_to_action VARCHAR(100),

    -- 미디어
    image_url VARCHAR(500),
    video_url VARCHAR(500),
    thumbnail_url VARCHAR(500),

    -- 링크
    landing_url VARCHAR(500),
    display_url VARCHAR(500),

    -- A/B 테스트
    is_primary BOOLEAN DEFAULT TRUE,
    weight INT DEFAULT 100,
    ab_test_group VARCHAR(20), -- A, B, C, CONTROL

    -- 상태
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, PAUSED, ARCHIVED

    -- 성과
    impressions BIGINT DEFAULT 0,
    clicks BIGINT DEFAULT 0,
    conversions BIGINT DEFAULT 0,

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,

    -- 기본 필드 (BaseEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE ad_creatives IS '광고 소재 관리';

-- 3.3 ad_impressions: 노출 통계
CREATE TABLE ad_impressions (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    creative_id BIGINT REFERENCES ad_creatives(id) ON DELETE CASCADE,

    -- 노출 정보
    user_id BIGINT REFERENCES users(id),
    session_id VARCHAR(100),
    impression_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 위치/맥락
    placement VARCHAR(100),
    page_url VARCHAR(500),
    referrer_url VARCHAR(500),
    position INT,

    -- 디바이스/환경
    device_type VARCHAR(20),
    browser VARCHAR(50),
    os VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),

    -- 비용
    cost DECIMAL(10,4) DEFAULT 0,

    -- 시간 (BaseTimeEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE ad_impressions IS '광고 노출 로그';

-- 인덱스
CREATE INDEX idx_ad_impressions_campaign_id ON ad_impressions(campaign_id);
CREATE INDEX idx_ad_impressions_created_at ON ad_impressions(created_at DESC);

-- 3.4 ad_clicks: 클릭 통계
CREATE TABLE ad_clicks (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    creative_id BIGINT REFERENCES ad_creatives(id) ON DELETE CASCADE,

    -- 사용자 정보
    user_id BIGINT REFERENCES users(id),
    session_id VARCHAR(100),

    -- 클릭 시간 및 URL
    click_time TIMESTAMP NOT NULL,
    landing_url VARCHAR(500),
    referrer_url VARCHAR(500),

    -- 환경 정보
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    device_type VARCHAR(20),
    browser VARCHAR(50),
    os VARCHAR(50),

    -- 전환 추적
    converted BOOLEAN NOT NULL DEFAULT FALSE,
    conversion_time TIMESTAMP,
    conversion_value DECIMAL(12,2),

    -- 시간 (BaseTimeEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE ad_clicks IS '광고 클릭 로그';

-- 3.5 ad_billings: 광고 청구/정산
CREATE TABLE ad_billings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,

    -- 청구 기간
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    billing_date DATE NOT NULL,

    -- 청구 타입 및 통계
    billing_type VARCHAR(20) NOT NULL, -- CPM, CPC, CPA, FLAT
    impressions BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    conversions BIGINT NOT NULL DEFAULT 0,

    -- 금액
    amount DECIMAL(12,2) NOT NULL,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,

    -- 결제 상태
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, OVERDUE, CANCELLED
    paid_at TIMESTAMP,
    payment_method VARCHAR(50),

    -- 청구서
    invoice_number VARCHAR(100),

    -- 비고
    notes TEXT,

    -- 기본 필드 (BaseEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Soft delete
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE ad_billings IS '광고 청구 및 정산';

-- 3.6 ad_payments: 광고 결제 이력 (V2 추가)
CREATE TABLE ad_payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,

    -- 결제 정보
    payment_amount DECIMAL(12,2) NOT NULL,
    apply_days INT NOT NULL,
    daily_rate DECIMAL(12,2) NOT NULL,
    value_30d DECIMAL(12,2) NOT NULL,

    -- 결제 타입 및 일자
    payment_type VARCHAR(20) NOT NULL, -- INITIAL, ADDITIONAL, RENEWAL
    payment_date DATE NOT NULL,

    -- 적용 기간
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 결제 수단 및 거래
    payment_method VARCHAR(50),
    transaction_id VARCHAR(200),

    -- 상태
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED', -- PENDING, COMPLETED, FAILED, REFUNDED

    -- 비고
    notes TEXT,

    -- 기본 필드 (BaseEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE ad_payments IS '광고 결제 이력';

-- 3.7 ad_daily_snapshots: 광고 일별 성과 스냅샷 (V2 추가)
CREATE TABLE ad_daily_snapshots (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,

    -- 스냅샷 정보
    snapshot_date DATE NOT NULL,
    value_30d DECIMAL(12,2) NOT NULL,
    daily_rank INT,

    -- 성과 지표
    impressions BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    conversions BIGINT NOT NULL DEFAULT 0,
    spent_amount DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- 성과 비율
    ctr DECIMAL(5,2), -- Click Through Rate
    cpc DECIMAL(12,2), -- Cost Per Click
    cvr DECIMAL(5,2), -- Conversion Rate

    -- 시간 (BaseTimeEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 유니크 제약
    CONSTRAINT uk_ad_daily_snapshot UNIQUE (campaign_id, snapshot_date)
);

COMMENT ON TABLE ad_daily_snapshots IS '광고 일별 성과 스냅샷';

-- 3.8 damoa_picks: 다모아 추천 업체 (V2 추가)
CREATE TABLE damoa_picks (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 추천 정보
    pick_type VARCHAR(20) NOT NULL, -- SPONSORED, OPERATED, PARTNER
    season VARCHAR(50),

    -- 콘텐츠
    title VARCHAR(200) NOT NULL,
    description TEXT,
    main_image_url VARCHAR(500),
    banner_image_url VARCHAR(500),
    images TEXT[],

    -- 배지
    badge_text VARCHAR(50),
    badge_color VARCHAR(20),

    -- 기간
    start_date DATE NOT NULL,
    end_date DATE,

    -- 표시 설정
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- 통계
    view_count INT NOT NULL DEFAULT 0,
    click_count INT NOT NULL DEFAULT 0,

    -- 기본 필드 (BaseEntity)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE damoa_picks IS '다모아 추천 업체';

-- ==============================================================================
-- 4. 견적/매칭 시스템 (8개 테이블)
-- ==============================================================================

-- 4.1 estimate_requests: 견적 요청
CREATE TABLE estimate_requests (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 요청 정보
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100),

    -- 요구사항
    requirements JSONB DEFAULT '{}'::JSONB,
    /* {
        space_type: '아파트',
        area: 33,
        budget_range: '1000-2000',
        desired_date: '2024-02-01',
        location: '서울시 강남구'
    } */

    -- 예산
    budget_min DECIMAL(12,2),
    budget_max DECIMAL(12,2),

    -- 일정
    desired_start_date DATE,
    desired_end_date DATE,

    -- 위치
    location VARCHAR(200),
    address VARCHAR(500),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),

    -- 이미지/파일
    images text[] DEFAULT '{}',

    -- 태그
    tags text[] DEFAULT '{}',
    required_skills text[] DEFAULT '{}',

    -- 상태
    status estimate_status DEFAULT 'DRAFT' NOT NULL,
    is_public BOOLEAN DEFAULT TRUE,

    -- 통계
    view_count INT DEFAULT 0,
    proposal_count INT DEFAULT 0,

    -- 만료
    expires_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE estimate_requests IS '견적 요청';

-- 4.2 estimate_proposals: 견적 제안
CREATE TABLE estimate_proposals (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    request_id BIGINT NOT NULL REFERENCES estimate_requests(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 제안 내용
    title VARCHAR(200),
    description TEXT NOT NULL,

    -- 가격 제안
    price DECIMAL(12,2) NOT NULL,
    pricing_details JSONB DEFAULT '{}'::JSONB,
    /* {
        labor_cost: 1000000,
        material_cost: 500000,
        other_cost: 100000,
        discount: 50000
    } */

    -- 일정 제안
    proposed_start_date DATE,
    proposed_end_date DATE,
    timeline JSONB DEFAULT '{}'::JSONB,
    /* {
        phases: [
            {name: '설계', duration: 7},
            {name: '시공', duration: 30}
        ]
    } */

    -- 첨부파일
    attachments text[] DEFAULT '{}',

    -- 상태
    status proposal_status DEFAULT 'DRAFT' NOT NULL,

    -- 선택 정보
    is_selected BOOLEAN DEFAULT FALSE,
    selected_at TIMESTAMP,
    rejection_reason TEXT,

    -- 유효기간
    valid_until DATE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE estimate_proposals IS '견적 제안';

-- 4.3 estimate_items: 견적 항목
CREATE TABLE estimate_items (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    proposal_id BIGINT NOT NULL REFERENCES estimate_proposals(id) ON DELETE CASCADE,

    -- 항목 정보
    item_name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),

    -- 수량/단위
    quantity DECIMAL(10,2) DEFAULT 1,
    unit VARCHAR(50),

    -- 가격
    unit_price DECIMAL(12,2),
    total_price DECIMAL(12,2),

    -- 상세 정보
    item_details JSONB DEFAULT '{}'::JSONB,

    -- 순서
    display_order INT DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE estimate_items IS '견적 상세 항목';

-- 4.4 estimate_attachments: 첨부파일
CREATE TABLE estimate_attachments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    request_id BIGINT REFERENCES estimate_requests(id) ON DELETE CASCADE,
    proposal_id BIGINT REFERENCES estimate_proposals(id) ON DELETE CASCADE,

    -- 파일 정보
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type file_type,
    file_size INT,

    -- 설명
    description VARCHAR(500),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CHECK (
        (request_id IS NOT NULL AND proposal_id IS NULL) OR
        (request_id IS NULL AND proposal_id IS NOT NULL)
    )
);

COMMENT ON TABLE estimate_attachments IS '견적 첨부파일';

-- 4.5 estimate_messages: 견적 메시지
CREATE TABLE estimate_messages (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    request_id BIGINT NOT NULL REFERENCES estimate_requests(id) ON DELETE CASCADE,
    proposal_id BIGINT REFERENCES estimate_proposals(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 메시지 내용
    message TEXT NOT NULL,

    -- 첨부파일
    attachments text[] DEFAULT '{}',

    -- 읽음 상태
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE estimate_messages IS '견적 관련 메시지';

-- 4.6 estimate_templates: 견적 템플릿
CREATE TABLE estimate_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 템플릿 정보
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    description TEXT,

    -- 템플릿 내용
    template_data JSONB DEFAULT '{}'::JSONB,

    -- 사용 횟수
    use_count INT DEFAULT 0,

    -- 공개 여부
    is_public BOOLEAN DEFAULT FALSE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE estimate_templates IS '견적 템플릿';

-- 4.7 matches: 매칭 확정
CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    request_id BIGINT NOT NULL REFERENCES estimate_requests(id),
    proposal_id BIGINT NOT NULL REFERENCES estimate_proposals(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),

    -- 계약 정보
    contract_amount DECIMAL(12,2) NOT NULL,
    contract_terms JSONB DEFAULT '{}'::JSONB,
    /* {
        payment_schedule: '계약시 30%, 중도금 40%, 잔금 30%',
        warranty_period: '1년',
        special_terms: []
    } */

    -- 일정
    start_date DATE,
    end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,

    -- 상태
    status VARCHAR(50) DEFAULT 'CONFIRMED', -- CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED

    -- 완료 정보
    completed_at TIMESTAMP,
    completion_notes TEXT,

    -- 취소 정보
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE matches IS '매칭 확정 정보';

-- 4.8 match_reviews: 매칭 후기
CREATE TABLE match_reviews (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES users(id),
    reviewee_id BIGINT NOT NULL REFERENCES users(id),

    -- 리뷰 타입
    review_type VARCHAR(50), -- USER_TO_COMPANY, COMPANY_TO_USER

    -- 평점
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),

    -- 리뷰 내용
    content TEXT,

    -- 상태
    is_public BOOLEAN DEFAULT TRUE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE match_reviews IS '매칭 후기';

-- ==============================================================================
-- 5. UMS (통합 메시징) (6개 테이블)
-- ==============================================================================

-- 5.1 notifications: 통합 알림
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 알림 정보
    notification_type VARCHAR(100) NOT NULL,
    channel notification_channel NOT NULL,

    -- 내용
    title VARCHAR(200),
    content TEXT NOT NULL,

    -- 링크/액션
    link_url VARCHAR(500),
    action_type VARCHAR(100),
    action_data JSONB DEFAULT '{}'::JSONB,

    -- 발송 정보
    is_sent BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP,
    send_error TEXT,

    -- 수신 확인
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,

    -- 스케줄링
    scheduled_at TIMESTAMP,

    -- 템플릿
    template_id BIGINT,
    template_data JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE notifications IS '통합 알림 관리';

-- 5.2 notification_templates: 알림 템플릿
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 템플릿 정보
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    channel notification_channel NOT NULL,

    -- 템플릿 내용
    title_template VARCHAR(500),
    content_template TEXT NOT NULL,

    -- 변수 정의
    variables JSONB DEFAULT '{}'::JSONB,
    /* {
        user_name: {type: 'string', required: true},
        amount: {type: 'number', required: false}
    } */

    -- 카카오 알림톡
    kakao_template_code VARCHAR(100),

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE notification_templates IS '알림 템플릿';

-- 5.3 notification_settings: 수신 설정
CREATE TABLE notification_settings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 채널별 수신 동의
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT TRUE,
    push_enabled BOOLEAN DEFAULT TRUE,
    kakao_enabled BOOLEAN DEFAULT FALSE,

    -- 알림 타입별 설정
    preferences JSONB DEFAULT '{}'::JSONB,
    /* {
        marketing: {email: true, sms: false, push: true},
        transaction: {email: true, sms: true, push: true},
        system: {email: true, sms: false, push: true}
    } */

    -- 수신 거부 기간
    do_not_disturb_start TIME,
    do_not_disturb_end TIME,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE notification_settings IS '알림 수신 설정';

-- 5.4 notification_logs: 발송 로그
CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT REFERENCES notifications(id) ON DELETE CASCADE,

    -- 발송 정보
    channel notification_channel NOT NULL,
    recipient VARCHAR(255) NOT NULL, -- 이메일, 전화번호 등

    -- 발송 상태
    status VARCHAR(50) NOT NULL, -- PENDING, SENT, FAILED, BOUNCED

    -- 외부 서비스 정보
    provider VARCHAR(100), -- AWS SES, Twilio, FCM 등
    provider_message_id VARCHAR(255),

    -- 응답 데이터
    response_data JSONB DEFAULT '{}'::JSONB,

    -- 에러 정보
    error_code VARCHAR(100),
    error_message TEXT,

    -- 비용
    cost DECIMAL(10,4) DEFAULT 0,

    -- 시간
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE notification_logs IS '알림 발송 로그';

-- 5.5 sms_verifications: SMS 인증
CREATE TABLE sms_verifications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 인증 대상
    phone VARCHAR(20) NOT NULL,
    user_id BIGINT REFERENCES users(id),

    -- 인증 코드
    verification_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50), -- SIGNUP, PASSWORD_RESET, PHONE_CHANGE

    -- 시도 횟수
    attempt_count INT DEFAULT 0,
    max_attempts INT DEFAULT 5,

    -- 상태
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,

    -- 만료
    expires_at TIMESTAMP NOT NULL,

    -- IP 추적
    request_ip VARCHAR(45),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE sms_verifications IS 'SMS 인증 코드 관리';

-- 5.6 email_verifications: 이메일 인증
CREATE TABLE email_verifications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 인증 대상
    email VARCHAR(255) NOT NULL,
    user_id BIGINT REFERENCES users(id),

    -- 인증 토큰
    verification_token VARCHAR(255) UNIQUE NOT NULL,
    purpose VARCHAR(50), -- SIGNUP, PASSWORD_RESET, EMAIL_CHANGE

    -- 상태
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,

    -- 만료
    expires_at TIMESTAMP NOT NULL,

    -- IP 추적
    request_ip VARCHAR(45),
    verified_ip VARCHAR(45),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE email_verifications IS '이메일 인증 토큰 관리';

-- ==============================================================================
-- 6. 파일 관리 (4개 테이블)
-- ==============================================================================

-- 6.1 files: 파일 메타데이터
CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- 파일 정보
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    file_type file_type NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,

    -- 저장 정보
    storage_type VARCHAR(50) DEFAULT 'S3', -- S3, LOCAL
    s3_bucket VARCHAR(100),
    s3_key VARCHAR(500) NOT NULL,
    file_url VARCHAR(1000),

    -- 썸네일
    thumbnail_url VARCHAR(1000),

    -- 메타데이터
    width INT,
    height INT,
    duration INT, -- 비디오/오디오 길이(초)

    -- 추가 정보
    metadata JSONB DEFAULT '{}'::JSONB,

    -- 보안
    is_public BOOLEAN DEFAULT FALSE,
    access_control JSONB DEFAULT '{}'::JSONB,

    -- 통계
    download_count INT DEFAULT 0,
    view_count INT DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE files IS '파일 메타데이터 관리';

-- 6.2 file_uploads: Presigned URL
CREATE TABLE file_uploads (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,

    -- 업로드 정보
    upload_key VARCHAR(500) NOT NULL,
    presigned_url TEXT NOT NULL,

    -- 파일 정보
    file_name VARCHAR(255),
    file_type VARCHAR(100),
    file_size_limit BIGINT,

    -- 상태
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, UPLOADED, EXPIRED, FAILED

    -- 완료 정보
    file_id BIGINT REFERENCES files(id),
    completed_at TIMESTAMP,

    -- 만료
    expires_at TIMESTAMP NOT NULL,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE file_uploads IS 'Presigned URL 관리';

-- 6.3 file_attachments: 파일 연결
CREATE TABLE file_attachments (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,

    -- Polymorphic 연결
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,

    -- 추가 정보
    attachment_type VARCHAR(50), -- MAIN, THUMBNAIL, ATTACHMENT
    display_order INT DEFAULT 0,
    caption VARCHAR(500),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE(file_id, entity_type, entity_id)
);

COMMENT ON TABLE file_attachments IS '파일-엔티티 연결 관리';

-- 6.4 file_downloads: 다운로드 로그
CREATE TABLE file_downloads (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id),

    -- 다운로드 정보
    ip_address VARCHAR(45),
    user_agent TEXT,
    referer TEXT,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE file_downloads IS '파일 다운로드 로그';

-- ==============================================================================
-- 7. 결제/금융 시스템 (8개 테이블)
-- ==============================================================================

-- 7.1 payments: 결제 정보
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- 결제 정보
    payment_type VARCHAR(50), -- ORDER, SUBSCRIPTION, AD, CREDIT
    payment_method payment_method NOT NULL,

    -- 금액
    amount DECIMAL(12,2) NOT NULL,
    tax_amount DECIMAL(12,2) DEFAULT 0,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    final_amount DECIMAL(12,2) NOT NULL,

    -- 상태
    status payment_status DEFAULT 'PENDING' NOT NULL,

    -- PG 정보
    pg_provider VARCHAR(50), -- TOSS, NICEPAY, KCP
    pg_tid VARCHAR(255),
    pg_response JSONB DEFAULT '{}'::JSONB,

    -- 결제 데이터
    payment_data JSONB DEFAULT '{}'::JSONB,
    /* {
        order_id: 123,
        order_name: '인테리어 견적',
        customer_name: '홍길동',
        customer_email: 'test@example.com'
    } */

    -- 결제 완료 정보
    paid_at TIMESTAMP,
    receipt_url VARCHAR(500),

    -- 취소/환불
    is_refundable BOOLEAN DEFAULT TRUE,
    refunded_amount DECIMAL(12,2) DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE payments IS '결제 정보';

-- 7.2 payment_methods: 결제 수단
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 결제 수단 정보
    method_type payment_method NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,

    -- 카드 정보 (암호화)
    card_info JSONB DEFAULT '{}'::JSONB,
    /* 암호화된 데이터:
    {
        card_number_masked: '****-****-****-1234',
        card_company: '삼성카드',
        billing_key: 'encrypted_key'
    } */

    -- 계좌 정보
    bank_info JSONB DEFAULT '{}'::JSONB,

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,
    verified_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE payment_methods IS '저장된 결제 수단';

-- 7.3 refunds: 환불
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- 환불 정보
    refund_amount DECIMAL(12,2) NOT NULL,
    refund_reason VARCHAR(500),
    refund_type VARCHAR(50), -- FULL, PARTIAL

    -- 상태
    status VARCHAR(50) DEFAULT 'REQUESTED', -- REQUESTED, APPROVED, COMPLETED, REJECTED

    -- 처리 정보
    approved_at TIMESTAMP,
    approved_by BIGINT REFERENCES users(id),
    completed_at TIMESTAMP,
    rejection_reason TEXT,

    -- PG 정보
    pg_tid VARCHAR(255),
    pg_response JSONB DEFAULT '{}'::JSONB,

    -- 환불 데이터
    refund_data JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE refunds IS '환불 관리';

-- 7.4 invoices: 세금계산서
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    payment_id BIGINT REFERENCES payments(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT REFERENCES companies(id),

    -- 계산서 정보
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    invoice_type VARCHAR(50), -- TAX, CASH_RECEIPT

    -- 금액
    supply_amount DECIMAL(12,2) NOT NULL,
    tax_amount DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,

    -- 발행 정보
    issue_date DATE NOT NULL,
    due_date DATE,

    -- 공급자 정보
    supplier_info JSONB DEFAULT '{}'::JSONB,

    -- 공급받는자 정보
    buyer_info JSONB DEFAULT '{}'::JSONB,

    -- 품목
    items JSONB DEFAULT '[]'::JSONB,

    -- 상태
    status VARCHAR(50) DEFAULT 'ISSUED', -- ISSUED, SENT, CANCELLED

    -- 파일
    pdf_url VARCHAR(500),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE invoices IS '세금계산서/계산서';

-- 7.5 credits: 크레딧 잔액
CREATE TABLE credits (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 잔액
    balance DECIMAL(12,2) DEFAULT 0 NOT NULL,
    total_earned DECIMAL(12,2) DEFAULT 0,
    total_used DECIMAL(12,2) DEFAULT 0,

    -- 보류 크레딧
    pending_amount DECIMAL(12,2) DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE credits IS '크레딧/포인트 잔액';

-- 7.6 credit_transactions: 크레딧 거래
CREATE TABLE credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    credit_id BIGINT NOT NULL REFERENCES credits(id),

    -- 거래 정보
    transaction_type VARCHAR(50), -- EARN, USE, REFUND, EXPIRE
    amount DECIMAL(12,2) NOT NULL,
    balance_after DECIMAL(12,2) NOT NULL,

    -- 거래 사유
    reason VARCHAR(200),
    reference_type VARCHAR(100),
    reference_id BIGINT,

    -- 만료
    expires_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE credit_transactions IS '크레딧 거래 내역';

-- 7.7 coupons: 쿠폰
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 쿠폰 정보
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    -- 할인 정보
    discount_type VARCHAR(50), -- FIXED, PERCENTAGE
    discount_value DECIMAL(12,2) NOT NULL,
    max_discount_amount DECIMAL(12,2),
    min_purchase_amount DECIMAL(12,2),

    -- 사용 조건
    conditions JSONB DEFAULT '{}'::JSONB,
    /* {
        categories: ['인테리어', '리모델링'],
        user_types: ['NEW', 'VIP'],
        payment_methods: ['CARD']
    } */

    -- 발행 수량
    total_quantity INT,
    used_quantity INT DEFAULT 0,

    -- 유효기간
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE coupons IS '쿠폰 마스터';

-- 7.8 user_coupons: 사용자 쿠폰
CREATE TABLE user_coupons (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    coupon_id BIGINT NOT NULL REFERENCES coupons(id),

    -- 사용 정보
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    payment_id BIGINT REFERENCES payments(id),

    -- 유효기간
    expires_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE(user_id, coupon_id)
);

COMMENT ON TABLE user_coupons IS '사용자별 쿠폰 보유';

-- ==============================================================================
-- 8. 게시판 시스템 (5개 테이블)
-- ==============================================================================

-- 8.1 boards: 통합 게시판
CREATE TABLE boards (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- 게시판 정보
    board_type board_type NOT NULL,
    category_id BIGINT,

    -- 기본 내용
    title VARCHAR(200) NOT NULL,
    content TEXT,

    -- 타입별 특수 데이터
    type_data JSONB DEFAULT '{}'::JSONB,
    /*
    EVENT: {
        start_date: '2024-01-01',
        end_date: '2024-01-31',
        location: '서울시 강남구',
        max_participants: 100,
        current_participants: 50
    }
    GALLERY: {
        images: ['url1', 'url2'],
        thumbnail_url: 'thumb_url'
    }
    DOCUMENT: {
        file_id: 123,
        file_size: 1024000,
        download_count: 10,
        price: 0
    }
    FAQ: {
        category: '결제',
        order: 1
    }
    */

    -- 통계
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,

    -- 상태
    is_pinned BOOLEAN DEFAULT FALSE,
    is_featured BOOLEAN DEFAULT FALSE,
    is_published BOOLEAN DEFAULT TRUE,
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 태그
    tags text[] DEFAULT '{}',

    -- 권한
    is_private BOOLEAN DEFAULT FALSE,
    password VARCHAR(255),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,

    -- 확장 데이터
    metadata JSONB DEFAULT '{}'::JSONB
);

COMMENT ON TABLE boards IS '통합 게시판';
COMMENT ON COLUMN boards.board_type IS '게시판 타입 (공지/이벤트/FAQ/갤러리/자료실)';
COMMENT ON COLUMN boards.type_data IS '타입별 특수 데이터';

-- 8.2 board_comments: 댓글
CREATE TABLE board_comments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    parent_id BIGINT REFERENCES board_comments(id) ON DELETE CASCADE,

    -- 댓글 내용
    content TEXT NOT NULL,

    -- 상태
    is_secret BOOLEAN DEFAULT FALSE,
    is_reported BOOLEAN DEFAULT FALSE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE board_comments IS '게시판 댓글';

-- 8.3 board_likes: 좋아요
CREATE TABLE board_likes (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE(board_id, user_id)
);

COMMENT ON TABLE board_likes IS '게시판 좋아요';

-- 8.4 board_categories: 카테고리
CREATE TABLE board_categories (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 카테고리 정보
    board_type board_type NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100),
    description VARCHAR(500),

    -- 계층 구조
    parent_id BIGINT REFERENCES board_categories(id) ON DELETE CASCADE,
    depth INT DEFAULT 0,
    path VARCHAR(500),

    -- 순서
    display_order INT DEFAULT 0,

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE board_categories IS '게시판 카테고리';

-- 8.5 board_attachments: 첨부파일
CREATE TABLE board_attachments (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES files(id),

    -- 첨부 정보
    attachment_type VARCHAR(50), -- FILE, IMAGE, VIDEO
    display_order INT DEFAULT 0,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE board_attachments IS '게시판 첨부파일';

-- ==============================================================================
-- 9. 필터/검색 시스템 (2개 테이블)
-- ==============================================================================

-- 9.1 filter_templates: 필터 템플릿
CREATE TABLE filter_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 템플릿 정보
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    entity_type VARCHAR(100) NOT NULL, -- COMPANY, ESTIMATE, PRODUCT

    -- 필터 설정
    filter_config JSONB DEFAULT '{}'::JSONB,
    /* {
        filters: [
            {
                key: 'location',
                label: '지역',
                type: 'multi_select',
                options: ['서울', '경기', '인천'],
                required: false
            },
            {
                key: 'price_range',
                label: '가격대',
                type: 'range',
                min: 0,
                max: 10000000,
                step: 100000
            }
        ]
    } */

    -- 정렬 옵션
    sort_options JSONB DEFAULT '{}'::JSONB,

    -- 상태
    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE filter_templates IS '필터 템플릿';

-- 9.2 saved_searches: 저장된 검색
CREATE TABLE saved_searches (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- 검색 정보
    name VARCHAR(200),
    entity_type VARCHAR(100) NOT NULL,

    -- 검색 조건
    search_query TEXT,
    filters JSONB DEFAULT '{}'::JSONB,
    sort_by VARCHAR(100),
    sort_order VARCHAR(10),

    -- 알림 설정
    alert_enabled BOOLEAN DEFAULT FALSE,
    alert_frequency VARCHAR(50), -- INSTANT, DAILY, WEEKLY
    last_alert_at TIMESTAMP,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE saved_searches IS '저장된 검색 조건';

-- ==============================================================================
-- 10. 통계/로그 (3개 테이블)
-- ==============================================================================

-- 10.1 statistics_daily: 일별 통계
CREATE TABLE statistics_daily (
    id BIGSERIAL PRIMARY KEY,
    stat_date DATE NOT NULL,

    -- 사용자 통계
    new_users INT DEFAULT 0,
    active_users INT DEFAULT 0,
    total_users INT DEFAULT 0,

    -- 업체 통계
    new_companies INT DEFAULT 0,
    active_companies INT DEFAULT 0,

    -- 견적 통계
    new_requests INT DEFAULT 0,
    new_proposals INT DEFAULT 0,
    new_matches INT DEFAULT 0,

    -- 결제 통계
    payment_count INT DEFAULT 0,
    payment_amount DECIMAL(12,2) DEFAULT 0,

    -- 광고 통계
    ad_impressions INT DEFAULT 0,
    ad_clicks INT DEFAULT 0,
    ad_revenue DECIMAL(12,2) DEFAULT 0,

    -- 상세 데이터
    detailed_stats JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE(stat_date)
);

COMMENT ON TABLE statistics_daily IS '일별 통계';

-- 10.2 analytics_events: 이벤트 추적
CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    session_id VARCHAR(255),

    -- 이벤트 정보
    event_name VARCHAR(100) NOT NULL,
    event_category VARCHAR(100),
    event_action VARCHAR(100),
    event_label VARCHAR(200),
    event_value DECIMAL(12,2),

    -- 페이지 정보
    page_url VARCHAR(500),
    page_title VARCHAR(200),
    referrer VARCHAR(500),

    -- 디바이스/환경
    device_type VARCHAR(50),
    browser VARCHAR(100),
    os VARCHAR(100),
    ip_address VARCHAR(45),
    country VARCHAR(2),
    region VARCHAR(100),

    -- 이벤트 속성
    properties JSONB DEFAULT '{}'::JSONB,

    -- 시간
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE analytics_events IS '이벤트 추적 로그';

-- 인덱스
CREATE INDEX idx_analytics_events_user_id ON analytics_events(user_id);
CREATE INDEX idx_analytics_events_event_name ON analytics_events(event_name);
CREATE INDEX idx_analytics_events_created_at ON analytics_events(created_at DESC);

-- 10.3 audit_logs: 감사 로그
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),

    -- 감사 정보
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,

    -- 변경 내역
    old_values JSONB,
    new_values JSONB,

    -- 추가 정보
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(255),

    -- 기본 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE audit_logs IS '감사 로그';

-- ==============================================================================
-- 인덱스 생성
-- ==============================================================================

-- users 인덱스
CREATE INDEX idx_users_uuid ON users(uuid);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- companies 인덱스
CREATE INDEX idx_companies_uuid ON companies(uuid);
CREATE INDEX idx_companies_user_id ON companies(user_id);
CREATE INDEX idx_companies_tags ON companies USING GIN(tags);
CREATE INDEX idx_companies_service_areas ON companies USING GIN(service_areas);
CREATE INDEX idx_companies_avg_rating ON companies(avg_rating DESC);
-- Full-text search index on companies (name and description only)
-- Note: tags excluded because array_to_string() is not IMMUTABLE in PostgreSQL
CREATE INDEX idx_companies_search ON companies USING GIN(
    to_tsvector('simple', COALESCE(name, '') || ' ' || COALESCE(description, ''))
);

-- ad_campaigns 인덱스
CREATE INDEX idx_ad_campaigns_uuid ON ad_campaigns(uuid);
CREATE INDEX idx_ad_campaigns_company_id ON ad_campaigns(company_id);
CREATE INDEX idx_ad_campaigns_ad_type ON ad_campaigns(ad_type);
CREATE INDEX idx_ad_campaigns_status ON ad_campaigns(status);
CREATE INDEX idx_ad_campaigns_dates ON ad_campaigns(start_date, end_date);

-- ad_billings 인덱스
CREATE INDEX idx_ad_billings_campaign_id ON ad_billings(campaign_id);
CREATE INDEX idx_ad_billings_billing_date ON ad_billings(billing_date);
CREATE INDEX idx_ad_billings_status ON ad_billings(status);

-- ad_payments 인덱스
CREATE INDEX idx_ad_payments_campaign_id ON ad_payments(campaign_id);
CREATE INDEX idx_ad_payments_payment_date ON ad_payments(payment_date);

-- ad_daily_snapshots 인덱스
CREATE INDEX idx_ad_daily_snapshots_campaign_id ON ad_daily_snapshots(campaign_id);
CREATE INDEX idx_ad_daily_snapshots_date ON ad_daily_snapshots(snapshot_date);

-- damoa_picks 인덱스
CREATE INDEX idx_damoa_picks_company_id ON damoa_picks(company_id);
CREATE INDEX idx_damoa_picks_pick_type ON damoa_picks(pick_type);
CREATE INDEX idx_damoa_picks_season ON damoa_picks(season);
CREATE INDEX idx_damoa_picks_active ON damoa_picks(is_active);

-- estimate_requests 인덱스
CREATE INDEX idx_estimate_requests_uuid ON estimate_requests(uuid);
CREATE INDEX idx_estimate_requests_user_id ON estimate_requests(user_id);
CREATE INDEX idx_estimate_requests_status ON estimate_requests(status);
CREATE INDEX idx_estimate_requests_created_at ON estimate_requests(created_at DESC);

-- estimate_proposals 인덱스
CREATE INDEX idx_estimate_proposals_uuid ON estimate_proposals(uuid);
CREATE INDEX idx_estimate_proposals_request_id ON estimate_proposals(request_id);
CREATE INDEX idx_estimate_proposals_company_id ON estimate_proposals(company_id);
CREATE INDEX idx_estimate_proposals_status ON estimate_proposals(status);

-- matches 인덱스
CREATE INDEX idx_matches_uuid ON matches(uuid);
CREATE INDEX idx_matches_user_id ON matches(user_id);
CREATE INDEX idx_matches_company_id ON matches(company_id);
CREATE INDEX idx_matches_status ON matches(status);

-- notifications 인덱스
CREATE INDEX idx_notifications_uuid ON notifications(uuid);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- files 인덱스
CREATE INDEX idx_files_uuid ON files(uuid);
CREATE INDEX idx_files_user_id ON files(user_id);
CREATE INDEX idx_files_file_type ON files(file_type);

-- payments 인덱스
CREATE INDEX idx_payments_uuid ON payments(uuid);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- boards 인덱스
CREATE INDEX idx_boards_uuid ON boards(uuid);
CREATE INDEX idx_boards_board_type ON boards(board_type);
CREATE INDEX idx_boards_user_id ON boards(user_id);
CREATE INDEX idx_boards_tags ON boards USING GIN(tags);
CREATE INDEX idx_boards_created_at ON boards(created_at DESC);

-- JSONB 인덱스
CREATE INDEX idx_companies_business_info ON companies USING GIN(business_info);
CREATE INDEX idx_companies_metadata ON companies USING GIN(metadata);
CREATE INDEX idx_ad_campaigns_targeting ON ad_campaigns USING GIN(targeting);
CREATE INDEX idx_boards_type_data ON boards USING GIN(type_data);

-- ==============================================================================
-- 트리거 생성
-- ==============================================================================

-- updated_at 자동 갱신 트리거
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_companies_updated_at BEFORE UPDATE ON companies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ad_campaigns_updated_at BEFORE UPDATE ON ad_campaigns
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_estimate_requests_updated_at BEFORE UPDATE ON estimate_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_estimate_proposals_updated_at BEFORE UPDATE ON estimate_proposals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_matches_updated_at BEFORE UPDATE ON matches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_boards_updated_at BEFORE UPDATE ON boards
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==============================================================================
-- 초기 데이터 삽입
-- ==============================================================================

-- 관리자 계정 생성 (비밀번호: admin123!)
INSERT INTO users (email, password, role, status, email_verified, email_verified_at)
VALUES ('admin@damoa.com', '$2a$10$YourHashedPasswordHere', 'ADMIN', 'ACTIVE', TRUE, CURRENT_TIMESTAMP);

-- 필터 템플릿 생성
INSERT INTO filter_templates (name, code, entity_type, filter_config) VALUES
('업체 검색 필터', 'company_search', 'COMPANY', '{
    "filters": [
        {
            "key": "service_areas",
            "label": "서비스 지역",
            "type": "multi_select",
            "options": ["서울", "경기", "인천", "대전", "대구", "부산", "광주", "울산"]
        },
        {
            "key": "avg_rating",
            "label": "평점",
            "type": "min_value",
            "min": 1,
            "max": 5
        },
        {
            "key": "tags",
            "label": "전문분야",
            "type": "multi_select",
            "options": ["주거공간", "상업공간", "사무공간", "리모델링", "신축"]
        },
        {
            "key": "price_range",
            "label": "가격대",
            "type": "range",
            "min": 0,
            "max": 100000000,
            "step": 10000000
        }
    ],
    "sort_options": [
        {"key": "avg_rating", "label": "평점순"},
        {"key": "review_count", "label": "리뷰 많은순"},
        {"key": "created_at", "label": "최신순"}
    ]
}'::JSONB);

-- 게시판 카테고리 생성
INSERT INTO board_categories (board_type, name, slug, display_order) VALUES
('NOTICE', '일반', 'general', 1),
('NOTICE', '업데이트', 'update', 2),
('EVENT', '할인이벤트', 'discount', 1),
('EVENT', '체험이벤트', 'experience', 2),
('FAQ', '회원가입', 'signup', 1),
('FAQ', '견적요청', 'estimate', 2),
('FAQ', '결제', 'payment', 3);

-- ==============================================================================
-- 스키마 버전 정보
-- ==============================================================================

COMMENT ON SCHEMA public IS 'HIP Damoa (Hospital Interior Platform) 데이터베이스 스키마 v1.0.0 - 초기 스키마 (55개 테이블)';

-- ==============================================================================
-- 완료
-- ==============================================================================