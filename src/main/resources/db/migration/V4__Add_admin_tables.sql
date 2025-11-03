-- =============================================================================
-- V4: 어드민 시스템 테이블 추가
-- =============================================================================
-- 설명: HIP Damoa 어드민 관리를 위한 포괄적인 테이블 구조 추가
-- 작성일: 2025-01-10
-- 내용:
--   - 어드민 세션 관리
--   - 어드민 로그인 기록
--   - IP 화이트리스트
--   - 2FA/MFA 지원
--   - 페이지 권한 관리
--   - 어드민 알림
--   - 시스템 설정
--   - 비밀번호 정책
--   - 활동 요약
-- =============================================================================

-- =============================================================================
-- 0. admin_users: 어드민 사용자 테이블 (가장 먼저 생성 - 다른 테이블이 참조함)
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 사용자 정보
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),

    -- 역할 및 상태
    roles TEXT[] NOT NULL DEFAULT '{}', -- SUPER_ADMIN, ADMIN, OPERATOR, VIEWER
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED

    -- 로그인 추적
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(50),
    login_count BIGINT NOT NULL DEFAULT 0,
    failed_login_count INT NOT NULL DEFAULT 0,

    -- 2FA 설정
    is_2fa_enabled BOOLEAN NOT NULL DEFAULT false,
    two_fa_secret VARCHAR(200),

    -- 메타데이터 (BaseEntity)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}'
);

-- 인덱스
CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_status ON admin_users(status) WHERE is_deleted = false;

-- 코멘트
COMMENT ON TABLE admin_users IS '어드민 사용자 테이블 - 어드민 시스템 사용자 정보';
COMMENT ON COLUMN admin_users.roles IS '역할 배열 (SUPER_ADMIN, ADMIN, OPERATOR, VIEWER)';
COMMENT ON COLUMN admin_users.status IS '상태 (ACTIVE, INACTIVE, SUSPENDED)';

-- =============================================================================
-- 1. 어드민 세션 관리 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_sessions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 어드민 사용자 정보
    admin_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token TEXT NOT NULL, -- 암호화된 세션 토큰
    refresh_token TEXT, -- 리프레시 토큰

    -- 세션 정보
    device_info JSONB DEFAULT '{}', -- 디바이스 정보 (OS, 브라우저, 버전 등)
    ip_address VARCHAR(50) NOT NULL,
    user_agent TEXT,

    -- 세션 시간 관리
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE,

    -- 세션 상태
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, REVOKED

    -- 메타데이터
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_admin_sessions_user_id ON admin_sessions(admin_user_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_admin_sessions_session_token ON admin_sessions(session_token) WHERE status = 'ACTIVE';
CREATE INDEX idx_admin_sessions_status ON admin_sessions(status);
CREATE INDEX idx_admin_sessions_expires_at ON admin_sessions(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_admin_sessions_ip_address ON admin_sessions(ip_address);

-- =============================================================================
-- 2. 어드민 로그인 기록 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_login_history (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 로그인 정보
    admin_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE, -- NULL이면 존재하지 않는 사용자 시도
    email VARCHAR(255) NOT NULL, -- 시도한 이메일
    login_type VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, BLOCKED, LOCKED

    -- 접속 정보
    ip_address VARCHAR(50) NOT NULL,
    user_agent TEXT,
    device_type VARCHAR(20), -- DESKTOP, MOBILE, TABLET
    location JSONB DEFAULT '{}', -- 위치 정보 (country, city, region 등)

    -- 실패 정보
    failure_reason VARCHAR(50), -- INVALID_PASSWORD, USER_NOT_FOUND, ACCOUNT_LOCKED, IP_BLOCKED
    failed_attempts INTEGER DEFAULT 0,

    -- 메타데이터
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_admin_login_history_user_id ON admin_login_history(admin_user_id);
CREATE INDEX idx_admin_login_history_email ON admin_login_history(email);
CREATE INDEX idx_admin_login_history_ip ON admin_login_history(ip_address);
CREATE INDEX idx_admin_login_history_created_at ON admin_login_history(created_at DESC);
CREATE INDEX idx_admin_login_history_login_type ON admin_login_history(login_type);

-- =============================================================================
-- 3. 어드민 IP 화이트리스트 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_ip_whitelist (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- IP 설정
    admin_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE, -- NULL이면 전역 규칙
    ip_address VARCHAR(50) NOT NULL,
    cidr_notation VARCHAR(50), -- CIDR 표기법 (예: 192.168.1.0/24)

    -- 규칙 정보
    description TEXT,
    rule_type VARCHAR(20) NOT NULL DEFAULT 'WHITELIST', -- WHITELIST, BLACKLIST
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- 유효 기간
    valid_from TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,

    -- 생성 정보
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT REFERENCES users(id)
);

-- 인덱스
CREATE INDEX idx_admin_ip_whitelist_user_id ON admin_ip_whitelist(admin_user_id) WHERE is_deleted = false;
CREATE INDEX idx_admin_ip_whitelist_ip ON admin_ip_whitelist(ip_address) WHERE is_deleted = false AND is_active = true;
CREATE INDEX idx_admin_ip_whitelist_cidr ON admin_ip_whitelist(cidr_notation) WHERE is_deleted = false AND is_active = true;

-- =============================================================================
-- 4. 어드민 2FA 인증 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_two_factor_auth (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 사용자 정보
    admin_user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

    -- 2FA 설정
    method VARCHAR(20) NOT NULL, -- TOTP, SMS, EMAIL, APP
    secret_key TEXT, -- 암호화된 시크릿 키 (TOTP용)
    phone_number VARCHAR(20), -- SMS용 전화번호
    email VARCHAR(255), -- 이메일 인증용

    -- 백업 코드
    backup_codes JSONB DEFAULT '[]', -- 암호화된 백업 코드 배열
    backup_codes_generated_at TIMESTAMP WITH TIME ZONE,

    -- 상태
    is_enabled BOOLEAN NOT NULL DEFAULT false,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    verified_at TIMESTAMP WITH TIME ZONE,

    -- 사용 기록
    last_used_at TIMESTAMP WITH TIME ZONE,
    last_used_ip INET,
    failed_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,

    -- 메타데이터
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE UNIQUE INDEX idx_admin_2fa_user_id ON admin_two_factor_auth(admin_user_id);

-- =============================================================================
-- 5. 어드민 페이지 권한 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_page_permissions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 페이지 정보
    page_code VARCHAR(50) UNIQUE NOT NULL, -- 고유 페이지 코드
    page_name VARCHAR(100) NOT NULL, -- 페이지 이름
    page_url VARCHAR(500) NOT NULL, -- 페이지 URL 패턴

    -- 계층 구조
    parent_page_id BIGINT REFERENCES admin_page_permissions(id),
    page_level INTEGER NOT NULL DEFAULT 0,
    page_path VARCHAR(500), -- 전체 경로 (예: /dashboard/users/list)

    -- 권한 설정
    required_permissions TEXT[] DEFAULT '{}', -- 필요한 권한 배열
    required_roles TEXT[] DEFAULT '{}', -- 필요한 역할 배열

    -- UI 설정
    display_order INTEGER NOT NULL DEFAULT 0,
    is_menu_visible BOOLEAN NOT NULL DEFAULT true,
    icon VARCHAR(50), -- 메뉴 아이콘
    badge_text VARCHAR(20), -- NEW, HOT 등 배지

    -- 페이지 타입
    page_type VARCHAR(20) NOT NULL DEFAULT 'PAGE', -- PAGE, API, COMPONENT, WIDGET
    module VARCHAR(50), -- 모듈명 (USER, COMPANY, PAYMENT 등)

    -- 상태
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_public BOOLEAN NOT NULL DEFAULT false, -- 공개 페이지 여부

    -- 메타데이터
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 인덱스
CREATE INDEX idx_admin_page_permissions_parent ON admin_page_permissions(parent_page_id) WHERE is_deleted = false;
CREATE INDEX idx_admin_page_permissions_code ON admin_page_permissions(page_code) WHERE is_deleted = false;
CREATE INDEX idx_admin_page_permissions_url ON admin_page_permissions(page_url) WHERE is_deleted = false;
CREATE INDEX idx_admin_page_permissions_module ON admin_page_permissions(module) WHERE is_deleted = false AND is_active = true;

-- =============================================================================
-- 6. 역할-페이지 접근 매핑 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_role_page_access (
    id BIGSERIAL PRIMARY KEY,

    -- 매핑 정보
    role_name VARCHAR(50) NOT NULL, -- AdminRole enum 값
    page_id BIGINT NOT NULL REFERENCES admin_page_permissions(id) ON DELETE CASCADE,

    -- 접근 타입
    access_type VARCHAR(20) NOT NULL DEFAULT 'FULL', -- FULL, READ_ONLY, HIDDEN, CUSTOM
    custom_permissions JSONB DEFAULT '{}', -- 커스텀 권한 설정

    -- 상태
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- 메타데이터
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),

    -- 유니크 제약
    CONSTRAINT uk_role_page UNIQUE (role_name, page_id)
);

-- 인덱스
CREATE INDEX idx_admin_role_page_access_role ON admin_role_page_access(role_name) WHERE is_active = true;
CREATE INDEX idx_admin_role_page_access_page ON admin_role_page_access(page_id) WHERE is_active = true;

-- =============================================================================
-- 7. 어드민 알림 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_notifications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 대상 사용자 (NULL = broadcast to all admins)
    admin_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,

    -- 알림 정보
    notification_type VARCHAR(50) NOT NULL, -- ALERT, WARNING, INFO, REPORT, etc.
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT

    -- 타겟 설정
    target_roles TEXT[] DEFAULT '{}', -- 대상 역할 배열
    target_users BIGINT[] DEFAULT '{}', -- 대상 사용자 ID 배열
    excluded_users BIGINT[] DEFAULT '{}', -- 제외할 사용자 ID 배열

    -- 관련 엔티티
    entity_type VARCHAR(50), -- USER, COMPANY, PAYMENT, ESTIMATE 등
    entity_id BIGINT,
    entity_uuid UUID,
    related_data JSONB DEFAULT '{}', -- 추가 관련 데이터

    -- 액션 설정
    action_url VARCHAR(500), -- 클릭 시 이동할 URL
    action_type VARCHAR(20), -- LINK, MODAL, API_CALL
    action_params JSONB DEFAULT '{}',
    action_data JSONB,

    -- 상태
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    read_by BIGINT[] DEFAULT '{}', -- 읽은 사용자 ID 배열

    -- 유효 기간
    expires_at TIMESTAMP WITH TIME ZONE,
    is_expired BOOLEAN NOT NULL DEFAULT false,

    -- 메타데이터 (BaseEntity)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}'
);

-- 인덱스
CREATE INDEX idx_admin_notifications_admin_user_id ON admin_notifications(admin_user_id);
CREATE INDEX idx_admin_notifications_type ON admin_notifications(notification_type);
CREATE INDEX idx_admin_notifications_severity ON admin_notifications(severity);
CREATE INDEX idx_admin_notifications_entity ON admin_notifications(entity_type, entity_id);
CREATE INDEX idx_admin_notifications_created_at ON admin_notifications(created_at DESC);
CREATE INDEX idx_admin_notifications_is_read ON admin_notifications(is_read) WHERE is_expired = false;

-- =============================================================================
-- 8. 어드민 시스템 설정 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_settings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 설정 정보
    setting_key VARCHAR(100) UNIQUE NOT NULL, -- 고유 설정 키
    setting_value JSONB NOT NULL, -- 설정 값 (모든 타입 지원)
    setting_type VARCHAR(20) NOT NULL, -- STRING, NUMBER, BOOLEAN, JSON, ARRAY

    -- 카테고리화
    category VARCHAR(50) NOT NULL, -- SYSTEM, SECURITY, UI, NOTIFICATION, PAYMENT 등
    subcategory VARCHAR(50),
    module VARCHAR(50), -- 모듈명

    -- 설정 메타데이터
    description TEXT,
    default_value JSONB,
    validation_rules JSONB DEFAULT '{}', -- 유효성 검사 규칙

    -- 접근 제어
    is_public BOOLEAN NOT NULL DEFAULT false, -- 공개 설정 여부
    is_editable BOOLEAN NOT NULL DEFAULT true, -- 편집 가능 여부
    required_permission VARCHAR(50), -- 편집에 필요한 권한

    -- Feature Flag 지원
    is_feature_flag BOOLEAN NOT NULL DEFAULT false,
    environment VARCHAR(20), -- LOCAL, DEV, STAGING, PROD

    -- 변경 이력
    previous_value JSONB,
    changed_at TIMESTAMP WITH TIME ZONE,
    changed_by BIGINT REFERENCES users(id),
    change_reason TEXT,

    -- 메타데이터
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id)
);

-- 인덱스
CREATE UNIQUE INDEX idx_admin_settings_key ON admin_settings(setting_key);
CREATE INDEX idx_admin_settings_category ON admin_settings(category);
CREATE INDEX idx_admin_settings_module ON admin_settings(module);
CREATE INDEX idx_admin_settings_feature_flag ON admin_settings(is_feature_flag) WHERE is_feature_flag = true;

-- =============================================================================
-- 9. 어드민 비밀번호 정책 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_password_policies (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 적용 대상
    admin_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE, -- NULL이면 전역 정책
    policy_name VARCHAR(100) NOT NULL,

    -- 비밀번호 복잡도
    min_length INTEGER NOT NULL DEFAULT 8,
    max_length INTEGER DEFAULT 128,
    require_uppercase BOOLEAN NOT NULL DEFAULT true,
    require_lowercase BOOLEAN NOT NULL DEFAULT true,
    require_numbers BOOLEAN NOT NULL DEFAULT true,
    require_special_chars BOOLEAN NOT NULL DEFAULT true,
    special_chars_set VARCHAR(100) DEFAULT '!@#$%^&*()_+-=[]{}|;:,.<>?',

    -- 비밀번호 생명주기
    max_age_days INTEGER DEFAULT 90, -- 비밀번호 만료 기간
    min_age_days INTEGER DEFAULT 1, -- 최소 사용 기간
    expire_warning_days INTEGER DEFAULT 14, -- 만료 경고 기간

    -- 비밀번호 재사용
    prevent_reuse_count INTEGER DEFAULT 5, -- 재사용 방지 개수
    prevent_common_passwords BOOLEAN NOT NULL DEFAULT true,

    -- 계정 잠금
    failed_attempts_lockout INTEGER DEFAULT 5, -- 실패 시 잠금 횟수
    lockout_duration_minutes INTEGER DEFAULT 30, -- 잠금 시간
    reset_failed_attempts_minutes INTEGER DEFAULT 30, -- 실패 횟수 리셋 시간

    -- 추가 규칙
    prevent_user_info BOOLEAN NOT NULL DEFAULT true, -- 사용자 정보 포함 금지
    require_change_on_first_login BOOLEAN NOT NULL DEFAULT true,

    -- 상태
    is_active BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 0, -- 우선순위 (높을수록 우선)

    -- 메타데이터
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 인덱스
CREATE INDEX idx_admin_password_policies_user ON admin_password_policies(admin_user_id) WHERE is_deleted = false AND is_active = true;
CREATE INDEX idx_admin_password_policies_priority ON admin_password_policies(priority DESC) WHERE is_deleted = false AND is_active = true;

-- =============================================================================
-- 10. 어드민 활동 요약 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_activity_summary (
    id BIGSERIAL PRIMARY KEY,

    -- 요약 정보
    admin_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary_date DATE NOT NULL,
    summary_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY

    -- 활동 통계
    login_count INTEGER NOT NULL DEFAULT 0,
    total_time_minutes INTEGER NOT NULL DEFAULT 0,
    actions_performed JSONB DEFAULT '{}', -- 액션별 카운트

    -- 상세 활동 (JSONB 구조)
    entities_created JSONB DEFAULT '{}', -- {users: 10, companies: 5, ...}
    entities_updated JSONB DEFAULT '{}',
    entities_deleted JSONB DEFAULT '{}',
    entities_viewed JSONB DEFAULT '{}',

    -- API 사용량
    api_calls_count INTEGER NOT NULL DEFAULT 0,
    api_errors_count INTEGER NOT NULL DEFAULT 0,

    -- 페이지 방문
    pages_visited JSONB DEFAULT '{}', -- {page_code: count, ...}
    most_visited_page VARCHAR(100),

    -- 보안 활동
    security_actions JSONB DEFAULT '{}', -- 보안 관련 액션
    failed_actions INTEGER NOT NULL DEFAULT 0,

    -- 성과 지표
    performance_score DECIMAL(5,2), -- 성과 점수 (0-100)
    productivity_index DECIMAL(5,2), -- 생산성 지수

    -- 메타데이터
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 유니크 제약
    CONSTRAINT uk_admin_activity_summary UNIQUE (admin_user_id, summary_date, summary_type)
);

-- 인덱스
CREATE INDEX idx_admin_activity_summary_user ON admin_activity_summary(admin_user_id);
CREATE INDEX idx_admin_activity_summary_date ON admin_activity_summary(summary_date DESC);
CREATE INDEX idx_admin_activity_summary_type ON admin_activity_summary(summary_type);

-- =============================================================================
-- 트리거 함수: updated_at 자동 업데이트
-- =============================================================================
CREATE OR REPLACE FUNCTION update_admin_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER update_admin_sessions_updated_at BEFORE UPDATE ON admin_sessions
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_ip_whitelist_updated_at BEFORE UPDATE ON admin_ip_whitelist
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_two_factor_auth_updated_at BEFORE UPDATE ON admin_two_factor_auth
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_page_permissions_updated_at BEFORE UPDATE ON admin_page_permissions
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_role_page_access_updated_at BEFORE UPDATE ON admin_role_page_access
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_notifications_updated_at BEFORE UPDATE ON admin_notifications
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_settings_updated_at BEFORE UPDATE ON admin_settings
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_password_policies_updated_at BEFORE UPDATE ON admin_password_policies
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

CREATE TRIGGER update_admin_activity_summary_updated_at BEFORE UPDATE ON admin_activity_summary
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

-- =============================================================================
-- admin_roles 테이블 (함수와 뷰에서 참조되므로 먼저 생성)
-- 역할 정의 (Role definitions - types of roles like SUPER_ADMIN, USER_MANAGER)
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_roles (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 역할 정의
    role_code VARCHAR(50) UNIQUE NOT NULL, -- SUPER_ADMIN, USER_MANAGER, COMPANY_MANAGER 등
    role_name VARCHAR(100) NOT NULL,
    description TEXT,
    priority INT NOT NULL, -- Higher number = higher priority

    -- 상태
    is_system_role BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- 메타데이터 (BaseEntity)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}'
);

-- 인덱스
CREATE INDEX idx_admin_roles_code ON admin_roles(role_code) WHERE is_active = true;
CREATE INDEX idx_admin_roles_priority ON admin_roles(priority) WHERE is_active = true;

-- 트리거
CREATE TRIGGER update_admin_roles_updated_at BEFORE UPDATE ON admin_roles
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

-- 코멘트
COMMENT ON TABLE admin_roles IS '어드민 역할 정의 테이블 - 역할 유형과 우선순위 관리';
COMMENT ON COLUMN admin_roles.role_code IS '역할 코드 (SUPER_ADMIN, USER_MANAGER, COMPANY_MANAGER, etc.)';
COMMENT ON COLUMN admin_roles.priority IS '우선순위 - 높은 숫자가 높은 우선순위';

-- 초기 역할 데이터
INSERT INTO admin_roles (role_code, role_name, description, priority, is_system_role, is_active) VALUES
('SUPER_ADMIN', '슈퍼 관리자', '모든 시스템 권한을 가진 최고 관리자', 100, true, true),
('USER_MANAGER', '사용자 관리자', '사용자 및 회원 관리 권한', 80, true, true),
('COMPANY_MANAGER', '업체 관리자', '업체 및 업체 정보 관리 권한', 70, true, true),
('CONTENT_MANAGER', '콘텐츠 관리자', '콘텐츠 및 게시물 관리 권한', 60, true, true),
('PLANNER_MANAGER', '플래너 관리자', '플래너 및 플래닝 관리 권한', 60, true, true),
('ESTIMATE_MANAGER', '견적 관리자', '견적 및 입찰 관리 권한', 60, true, true),
('PAYMENT_MANAGER', '결제 관리자', '결제 및 정산 관리 권한', 60, true, true),
('AUDIT_VIEWER', '감사 조회자', '감사 로그 조회 권한 (읽기 전용)', 40, true, true);

-- =============================================================================
-- admin_user_roles 테이블 - 사용자-역할 매핑
-- User-to-role assignments (which users have which roles)
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_user_roles (
    id BIGSERIAL PRIMARY KEY,

    -- 사용자-역할 매핑
    admin_user_id BIGINT NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES admin_roles(id) ON DELETE CASCADE,

    -- 부여 정보
    granted_by BIGINT, -- Admin user ID who granted this role
    expires_at TIMESTAMP WITH TIME ZONE,

    -- 시간 (BaseTimeEntity)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 유니크 제약
    CONSTRAINT uk_user_role UNIQUE (admin_user_id, role_id)
);

-- 인덱스
CREATE INDEX idx_admin_user_roles_user_id ON admin_user_roles(admin_user_id);
CREATE INDEX idx_admin_user_roles_role_id ON admin_user_roles(role_id);
CREATE INDEX idx_admin_user_roles_expires_at ON admin_user_roles(expires_at);

-- 트리거
CREATE TRIGGER update_admin_user_roles_updated_at BEFORE UPDATE ON admin_user_roles
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

-- 코멘트
COMMENT ON TABLE admin_user_roles IS '어드민 사용자-역할 매핑 테이블 - 사용자별 역할 할당 관리';

-- =============================================================================
-- 10. admin_audit_logs: 어드민 감사 로그
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,

    -- 감사 정보
    admin_user_id BIGINT REFERENCES users(id),
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, APPROVE, REJECT, LOGIN, LOGOUT, etc.

    -- 대상 엔티티
    entity_type VARCHAR(50),
    entity_id BIGINT,
    description TEXT,

    -- 변경 내역
    changes JSONB, -- Before/After values

    -- 요청 정보
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),

    -- 결과
    is_successful BOOLEAN NOT NULL DEFAULT true,
    failure_reason TEXT,

    -- 시간 (BaseTimeEntity)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_admin_audit_logs_admin_user_id ON admin_audit_logs(admin_user_id);
CREATE INDEX idx_admin_audit_logs_action ON admin_audit_logs(action);
CREATE INDEX idx_admin_audit_logs_entity ON admin_audit_logs(entity_type, entity_id);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs(created_at DESC);

-- 코멘트
COMMENT ON TABLE admin_audit_logs IS '어드민 감사 로그 - 관리자 작업 이력 추적';

-- =============================================================================
-- 11. admin_permissions: 어드민 권한 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_permissions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 권한 정보
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL, -- USER, COMPANY, PAYMENT, AD, ESTIMATE, etc.
    action VARCHAR(20) NOT NULL, -- CREATE, READ, UPDATE, DELETE, APPROVE, etc.
    description TEXT,

    -- 시스템 권한 여부
    is_system_permission BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- 메타데이터 (BaseEntity)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}'
);

-- 인덱스
CREATE INDEX idx_admin_permissions_code ON admin_permissions(permission_code);
CREATE INDEX idx_admin_permissions_resource ON admin_permissions(resource_type);

-- 트리거
CREATE TRIGGER update_admin_permissions_updated_at BEFORE UPDATE ON admin_permissions
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

-- 코멘트
COMMENT ON TABLE admin_permissions IS '어드민 권한 테이블 - 세분화된 권한 관리';

-- =============================================================================
-- 12. admin_role_permissions: 어드민 역할-권한 매핑 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_role_permissions (
    id BIGSERIAL PRIMARY KEY,

    -- 역할-권한 매핑
    role_id BIGINT NOT NULL REFERENCES admin_roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES admin_permissions(id) ON DELETE CASCADE,

    -- 부여 정보
    granted_by BIGINT, -- Admin user ID who granted this permission

    -- 시간 (BaseTimeEntity)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 유니크 제약
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- 인덱스
CREATE INDEX idx_admin_role_permissions_role_id ON admin_role_permissions(role_id);
CREATE INDEX idx_admin_role_permissions_permission_id ON admin_role_permissions(permission_id);

-- 트리거
CREATE TRIGGER update_admin_role_permissions_updated_at BEFORE UPDATE ON admin_role_permissions
    FOR EACH ROW EXECUTE FUNCTION update_admin_updated_at_column();

-- 코멘트
COMMENT ON TABLE admin_role_permissions IS '어드민 역할-권한 매핑 - 역할별 세분화된 권한 할당';

-- =============================================================================
-- 함수: 어드민 권한 조회
-- =============================================================================
CREATE OR REPLACE FUNCTION get_admin_permissions(p_user_id BIGINT)
RETURNS TABLE (
    permission_type VARCHAR,
    permission_value TEXT,
    source VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    WITH user_roles AS (
        -- 사용자의 역할 조회
        SELECT ar.role_name
        FROM admin_roles ar
        WHERE ar.user_id = p_user_id
          AND ar.is_active = true
    ),
    role_permissions AS (
        -- 역할에 따른 페이지 권한
        SELECT
            'PAGE' as permission_type,
            app.page_code as permission_value,
            'ROLE' as source
        FROM admin_role_page_access arpa
        JOIN admin_page_permissions app ON app.id = arpa.page_id
        JOIN user_roles ur ON ur.role_name = arpa.role_name
        WHERE arpa.is_active = true
          AND app.is_active = true
          AND app.is_deleted = false
    ),
    direct_permissions AS (
        -- 직접 할당된 권한 (추후 구현)
        SELECT
            'DIRECT' as permission_type,
            '' as permission_value,
            'USER' as source
        WHERE false -- 플레이스홀더
    )
    SELECT * FROM role_permissions
    UNION ALL
    SELECT * FROM direct_permissions
    ORDER BY permission_type, permission_value;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 함수: IP 화이트리스트 체크
-- =============================================================================
CREATE OR REPLACE FUNCTION check_ip_whitelist(p_user_id BIGINT, p_ip_address INET)
RETURNS BOOLEAN AS $$
DECLARE
    v_allowed BOOLEAN := false;
BEGIN
    -- 전역 화이트리스트 체크
    SELECT EXISTS (
        SELECT 1
        FROM admin_ip_whitelist
        WHERE admin_user_id IS NULL
          AND is_active = true
          AND is_deleted = false
          AND rule_type = 'WHITELIST'
          AND (ip_address = p_ip_address
               OR (cidr_notation IS NOT NULL AND p_ip_address << cidr_notation::inet))
          AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
    ) INTO v_allowed;

    IF v_allowed THEN
        RETURN true;
    END IF;

    -- 사용자별 화이트리스트 체크
    SELECT EXISTS (
        SELECT 1
        FROM admin_ip_whitelist
        WHERE admin_user_id = p_user_id
          AND is_active = true
          AND is_deleted = false
          AND rule_type = 'WHITELIST'
          AND (ip_address = p_ip_address
               OR (cidr_notation IS NOT NULL AND p_ip_address << cidr_notation::inet))
          AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
    ) INTO v_allowed;

    RETURN v_allowed;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 뷰: 어드민 대시보드 통계
-- =============================================================================
CREATE OR REPLACE VIEW v_admin_dashboard AS
SELECT
    -- 오늘 통계
    (SELECT COUNT(*) FROM admin_sessions WHERE DATE(created_at) = CURRENT_DATE AND status = 'ACTIVE') as active_sessions_today,
    (SELECT COUNT(*) FROM admin_login_history WHERE DATE(created_at) = CURRENT_DATE AND login_type = 'SUCCESS') as successful_logins_today,
    (SELECT COUNT(*) FROM admin_login_history WHERE DATE(created_at) = CURRENT_DATE AND login_type = 'FAILED') as failed_logins_today,

    -- 활성 어드민 수
    (SELECT COUNT(DISTINCT admin_user_id) FROM admin_sessions WHERE status = 'ACTIVE') as active_admins,

    -- 최근 7일 통계
    (SELECT COUNT(*) FROM admin_login_history WHERE created_at > CURRENT_DATE - INTERVAL '7 days') as logins_last_7days,

    -- 보안 통계
    (SELECT COUNT(*) FROM admin_two_factor_auth WHERE is_enabled = true) as two_factor_enabled_count,
    (SELECT COUNT(*) FROM admin_ip_whitelist WHERE is_active = true AND is_deleted = false) as active_ip_rules,

    -- 알림 통계
    (SELECT COUNT(*) FROM admin_notifications WHERE is_read = false AND is_expired = false) as unread_notifications,
    (SELECT COUNT(*) FROM admin_notifications WHERE severity = 'CRITICAL' AND created_at > CURRENT_DATE - INTERVAL '1 day') as critical_alerts_24h,

    -- 시스템 설정
    (SELECT COUNT(*) FROM admin_settings WHERE category = 'SYSTEM') as system_settings_count,
    (SELECT COUNT(*) FROM admin_settings WHERE is_feature_flag = true) as feature_flags_count,

    -- 업데이트 시간
    CURRENT_TIMESTAMP as calculated_at;

-- =============================================================================
-- 뷰: 활성 어드민 세션
-- =============================================================================
CREATE OR REPLACE VIEW v_admin_active_sessions AS
SELECT
    s.id,
    s.uuid,
    s.admin_user_id,
    u.email,
    ar.role_code,
    ar.role_name,
    s.ip_address,
    s.device_info,
    s.last_activity_at,
    s.expires_at,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - s.last_activity_at))/60 as idle_minutes,
    EXTRACT(EPOCH FROM (s.expires_at - CURRENT_TIMESTAMP))/60 as remaining_minutes
FROM admin_sessions s
JOIN users u ON u.id = s.admin_user_id
LEFT JOIN admin_user_roles aur ON aur.admin_user_id = s.admin_user_id
    AND (aur.expires_at IS NULL OR aur.expires_at > CURRENT_TIMESTAMP)
LEFT JOIN admin_roles ar ON ar.id = aur.role_id AND ar.is_active = true
WHERE s.status = 'ACTIVE'
  AND s.expires_at > CURRENT_TIMESTAMP
ORDER BY ar.priority DESC, s.last_activity_at DESC;

-- =============================================================================
-- 테이블 코멘트
-- =============================================================================
COMMENT ON TABLE admin_sessions IS '어드민 세션 관리 테이블 - 로그인된 어드민의 세션 정보와 활동 추적';
COMMENT ON TABLE admin_login_history IS '어드민 로그인 기록 테이블 - 모든 로그인 시도와 결과 기록';
COMMENT ON TABLE admin_ip_whitelist IS '어드민 IP 화이트리스트 테이블 - IP 기반 접근 제어';
COMMENT ON TABLE admin_two_factor_auth IS '어드민 2단계 인증 테이블 - TOTP, SMS, 이메일 등 다중 인증 지원';
COMMENT ON TABLE admin_page_permissions IS '어드민 페이지 권한 테이블 - 페이지별 접근 권한 정의';
COMMENT ON TABLE admin_role_page_access IS '역할-페이지 접근 매핑 테이블 - 역할별 페이지 접근 권한 설정';
COMMENT ON TABLE admin_notifications IS '어드민 알림 테이블 - 시스템 알림과 중요 이벤트 관리';
COMMENT ON TABLE admin_settings IS '어드민 시스템 설정 테이블 - 시스템 전역 설정과 Feature Flag 관리';
COMMENT ON TABLE admin_password_policies IS '어드민 비밀번호 정책 테이블 - 비밀번호 복잡도와 생명주기 규칙';
COMMENT ON TABLE admin_activity_summary IS '어드민 활동 요약 테이블 - 일/주/월별 어드민 활동 통계';

-- 컬럼 코멘트는 너무 많아서 주요 컬럼만
COMMENT ON COLUMN admin_sessions.session_token IS '암호화된 세션 토큰';
COMMENT ON COLUMN admin_sessions.device_info IS '접속 디바이스 정보 (OS, 브라우저, 버전 등)';
COMMENT ON COLUMN admin_sessions.status IS '세션 상태 (ACTIVE, EXPIRED, REVOKED)';

COMMENT ON COLUMN admin_login_history.login_type IS '로그인 유형 (SUCCESS, FAILED, BLOCKED, LOCKED)';
COMMENT ON COLUMN admin_login_history.failure_reason IS '실패 사유 (INVALID_PASSWORD, USER_NOT_FOUND, ACCOUNT_LOCKED, IP_BLOCKED)';

COMMENT ON COLUMN admin_ip_whitelist.cidr_notation IS 'CIDR 표기법 IP 범위 (예: 192.168.1.0/24)';
COMMENT ON COLUMN admin_ip_whitelist.rule_type IS '규칙 유형 (WHITELIST, BLACKLIST)';

COMMENT ON COLUMN admin_two_factor_auth.method IS '2FA 방법 (TOTP, SMS, EMAIL, APP)';
COMMENT ON COLUMN admin_two_factor_auth.backup_codes IS '암호화된 백업 코드 JSON 배열';

COMMENT ON COLUMN admin_page_permissions.page_type IS '페이지 유형 (PAGE, API, COMPONENT, WIDGET)';
COMMENT ON COLUMN admin_page_permissions.required_permissions IS '접근에 필요한 권한 배열';

COMMENT ON COLUMN admin_role_page_access.access_type IS '접근 유형 (FULL, READ_ONLY, HIDDEN, CUSTOM)';

COMMENT ON COLUMN admin_notifications.severity IS '알림 심각도 (LOW, MEDIUM, HIGH, CRITICAL)';
COMMENT ON COLUMN admin_notifications.target_roles IS '알림 대상 역할 배열';

COMMENT ON COLUMN admin_settings.setting_type IS '설정 값 유형 (STRING, NUMBER, BOOLEAN, JSON, ARRAY)';
COMMENT ON COLUMN admin_settings.category IS '설정 카테고리 (SYSTEM, SECURITY, UI, NOTIFICATION, PAYMENT)';

COMMENT ON COLUMN admin_password_policies.prevent_reuse_count IS '재사용 방지할 이전 비밀번호 개수';
COMMENT ON COLUMN admin_password_policies.failed_attempts_lockout IS '계정 잠금까지 허용되는 실패 횟수';

COMMENT ON COLUMN admin_activity_summary.summary_type IS '요약 유형 (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)';
COMMENT ON COLUMN admin_activity_summary.performance_score IS '성과 점수 (0-100)';

-- =============================================================================
-- 초기 데이터: 기본 페이지 권한 설정
-- =============================================================================
INSERT INTO admin_page_permissions (page_code, page_name, page_url, parent_page_id, page_level, module, display_order, icon, page_type, required_permissions, is_public)
VALUES
-- 대시보드
('DASHBOARD', '대시보드', '/admin/dashboard', NULL, 0, 'DASHBOARD', 1, 'dashboard', 'PAGE', ARRAY['VIEW_DASHBOARD'], false),
('DASHBOARD_ANALYTICS', '통계 대시보드', '/admin/dashboard/analytics', 1, 1, 'DASHBOARD', 2, 'chart', 'PAGE', ARRAY['VIEW_ANALYTICS'], false),

-- 사용자 관리
('USER_MANAGEMENT', '사용자 관리', '/admin/users', NULL, 0, 'USER', 10, 'users', 'PAGE', ARRAY['VIEW_USERS'], false),
('USER_LIST', '사용자 목록', '/admin/users/list', 3, 1, 'USER', 11, 'list', 'PAGE', ARRAY['VIEW_USERS'], false),
('USER_CREATE', '사용자 생성', '/admin/users/create', 3, 1, 'USER', 12, 'plus', 'PAGE', ARRAY['CREATE_USER'], false),
('USER_EDIT', '사용자 수정', '/admin/users/edit/*', 3, 1, 'USER', 13, 'edit', 'PAGE', ARRAY['EDIT_USER'], false),

-- 업체 관리
('COMPANY_MANAGEMENT', '업체 관리', '/admin/companies', NULL, 0, 'COMPANY', 20, 'building', 'PAGE', ARRAY['VIEW_COMPANIES'], false),
('COMPANY_LIST', '업체 목록', '/admin/companies/list', 7, 1, 'COMPANY', 21, 'list', 'PAGE', ARRAY['VIEW_COMPANIES'], false),
('COMPANY_APPROVAL', '업체 승인', '/admin/companies/approval', 7, 1, 'COMPANY', 22, 'check', 'PAGE', ARRAY['APPROVE_COMPANY'], false),

-- 견적/입찰 관리
('ESTIMATE_MANAGEMENT', '견적 관리', '/admin/estimates', NULL, 0, 'ESTIMATE', 30, 'document', 'PAGE', ARRAY['VIEW_ESTIMATES'], false),
('ESTIMATE_LIST', '견적 목록', '/admin/estimates/list', 10, 1, 'ESTIMATE', 31, 'list', 'PAGE', ARRAY['VIEW_ESTIMATES'], false),
('CONTEST_MANAGEMENT', '콘테스트 관리', '/admin/contests', 10, 1, 'ESTIMATE', 32, 'trophy', 'PAGE', ARRAY['MANAGE_CONTESTS'], false),

-- 결제 관리
('PAYMENT_MANAGEMENT', '결제 관리', '/admin/payments', NULL, 0, 'PAYMENT', 40, 'credit-card', 'PAGE', ARRAY['VIEW_PAYMENTS'], false),
('PAYMENT_LIST', '결제 목록', '/admin/payments/list', 13, 1, 'PAYMENT', 41, 'list', 'PAGE', ARRAY['VIEW_PAYMENTS'], false),
('REFUND_MANAGEMENT', '환불 관리', '/admin/payments/refunds', 13, 1, 'PAYMENT', 42, 'undo', 'PAGE', ARRAY['MANAGE_REFUNDS'], false),

-- 광고 관리
('AD_MANAGEMENT', '광고 관리', '/admin/ads', NULL, 0, 'ADVERTISEMENT', 50, 'megaphone', 'PAGE', ARRAY['VIEW_ADS'], false),
('AD_CAMPAIGNS', '캠페인 관리', '/admin/ads/campaigns', 16, 1, 'ADVERTISEMENT', 51, 'campaign', 'PAGE', ARRAY['MANAGE_CAMPAIGNS'], false),
('DAMOA_PICKS', '다모아픽 관리', '/admin/ads/picks', 16, 1, 'ADVERTISEMENT', 52, 'star', 'PAGE', ARRAY['MANAGE_PICKS'], false),

-- 콘텐츠 관리
('CONTENT_MANAGEMENT', '콘텐츠 관리', '/admin/contents', NULL, 0, 'CONTENT', 60, 'file-text', 'PAGE', ARRAY['VIEW_CONTENTS'], false),
('BOARD_MANAGEMENT', '게시판 관리', '/admin/contents/boards', 19, 1, 'CONTENT', 61, 'clipboard', 'PAGE', ARRAY['MANAGE_BOARDS'], false),
('FAQ_MANAGEMENT', 'FAQ 관리', '/admin/contents/faq', 19, 1, 'CONTENT', 62, 'help-circle', 'PAGE', ARRAY['MANAGE_FAQ'], false),

-- 시스템 관리
('SYSTEM_MANAGEMENT', '시스템 관리', '/admin/system', NULL, 0, 'SYSTEM', 100, 'settings', 'PAGE', ARRAY['VIEW_SYSTEM'], false),
('SYSTEM_SETTINGS', '시스템 설정', '/admin/system/settings', 22, 1, 'SYSTEM', 101, 'sliders', 'PAGE', ARRAY['MANAGE_SETTINGS'], false),
('AUDIT_LOGS', '감사 로그', '/admin/system/audit', 22, 1, 'SYSTEM', 102, 'shield', 'PAGE', ARRAY['VIEW_AUDIT_LOG'], false),
('ADMIN_USERS', '어드민 관리', '/admin/system/admins', 22, 1, 'SYSTEM', 103, 'user-check', 'PAGE', ARRAY['MANAGE_ADMINS'], false);

-- =============================================================================
-- 초기 데이터: 시스템 설정
-- =============================================================================
INSERT INTO admin_settings (setting_key, setting_value, setting_type, category, description, is_public, is_editable)
VALUES
('system.maintenance_mode', 'false', 'BOOLEAN', 'SYSTEM', '시스템 점검 모드', false, true),
('system.max_upload_size', '10485760', 'NUMBER', 'SYSTEM', '최대 업로드 파일 크기 (bytes)', false, true),
('security.session_timeout', '3600', 'NUMBER', 'SECURITY', '세션 타임아웃 (초)', false, true),
('security.max_login_attempts', '5', 'NUMBER', 'SECURITY', '최대 로그인 시도 횟수', false, true),
('ui.items_per_page', '20', 'NUMBER', 'UI', '페이지당 항목 수', false, true),
('notification.email_enabled', 'true', 'BOOLEAN', 'NOTIFICATION', '이메일 알림 활성화', false, true),
('notification.sms_enabled', 'true', 'BOOLEAN', 'NOTIFICATION', 'SMS 알림 활성화', false, true);

-- =============================================================================
-- 초기 데이터: 기본 비밀번호 정책
-- =============================================================================
INSERT INTO admin_password_policies (
    policy_name, min_length, require_uppercase, require_lowercase,
    require_numbers, require_special_chars, max_age_days,
    prevent_reuse_count, failed_attempts_lockout, created_by
)
VALUES (
    '기본 어드민 비밀번호 정책',
    10, -- 최소 10자
    true, -- 대문자 필수
    true, -- 소문자 필수
    true, -- 숫자 필수
    true, -- 특수문자 필수
    90, -- 90일마다 변경
    5, -- 최근 5개 비밀번호 재사용 금지
    5, -- 5회 실패시 잠금
    1 -- system user
);

-- =============================================================================
-- 완료 메시지
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'V4 마이그레이션 완료: 어드민 시스템 테이블 생성';
    RAISE NOTICE '- 생성된 테이블: 10개';
    RAISE NOTICE '- 생성된 함수: 2개';
    RAISE NOTICE '- 생성된 뷰: 2개';
    RAISE NOTICE '- 생성된 트리거: 9개';
    RAISE NOTICE '- 초기 데이터: 페이지 권한 25개, 시스템 설정 7개, 비밀번호 정책 1개';
    RAISE NOTICE '=============================================================================';
END $$;