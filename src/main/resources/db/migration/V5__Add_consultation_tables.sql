-- =============================================================================
-- V5: 빠른상담 & 제휴/광고 문의 시스템 추가
-- =============================================================================
-- 설명: 사용자 빠른상담 요청 및 제휴/광고 문의 관리 시스템
-- 작성일: 2025-01-10
-- 내용:
--   - 빠른상담 요청 테이블 (3가지 동의 포함)
--   - 제휴/광고 문의 테이블
--   - 상담 메시지 스레드 테이블
--   - 어드민 페이지 권한 7개 추가
-- =============================================================================

-- =============================================================================
-- 1. 빠른상담 요청 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS quick_consultations (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 요청자 정보 (비회원 가능)
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),

    -- 상담 내용
    subject VARCHAR(200),
    message TEXT NOT NULL,
    preferred_contact_method VARCHAR(20), -- PHONE, EMAIL, KAKAO
    preferred_contact_time VARCHAR(100), -- 선호 연락 시간대

    -- 동의 정보 (비정규화 - 3가지 필수)
    personal_info_consent BOOLEAN NOT NULL DEFAULT false,
    personal_info_consent_at TIMESTAMP WITH TIME ZONE,
    third_party_consent BOOLEAN NOT NULL DEFAULT false,
    third_party_consent_at TIMESTAMP WITH TIME ZONE,
    marketing_consent BOOLEAN NOT NULL DEFAULT false,
    marketing_consent_at TIMESTAMP WITH TIME ZONE,

    -- 법적 증빙 정보
    consent_ip_address INET, -- 동의 시 IP 주소
    consent_version VARCHAR(20), -- 약관 버전 (v1.0, v2.0 등)

    -- 배정 정보
    assigned_company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP WITH TIME ZONE,
    assigned_by BIGINT REFERENCES users(id), -- 배정한 관리자

    -- 상태 관리
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    -- SUBMITTED: 제출됨
    -- ASSIGNED: 업체 배정됨
    -- IN_PROGRESS: 상담 진행중
    -- RESPONDED: 응답 완료
    -- COMPLETED: 완료
    -- CANCELLED: 취소

    -- 응답 정보
    response_message TEXT,
    responded_at TIMESTAMP WITH TIME ZONE,
    responded_by BIGINT REFERENCES users(id),

    -- 완료 정보
    completed_at TIMESTAMP WITH TIME ZONE,
    completion_notes TEXT,
    cancellation_reason TEXT,

    -- IP/접속 정보
    ip_address INET,
    user_agent TEXT,
    referrer VARCHAR(500),

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT REFERENCES users(id)
);

-- 인덱스
CREATE INDEX idx_quick_consultations_uuid ON quick_consultations(uuid);
CREATE INDEX idx_quick_consultations_user_id ON quick_consultations(user_id) WHERE is_deleted = false;
CREATE INDEX idx_quick_consultations_status ON quick_consultations(status) WHERE is_deleted = false;
CREATE INDEX idx_quick_consultations_assigned_company ON quick_consultations(assigned_company_id) WHERE is_deleted = false;
CREATE INDEX idx_quick_consultations_phone ON quick_consultations(phone) WHERE is_deleted = false;
CREATE INDEX idx_quick_consultations_email ON quick_consultations(email) WHERE is_deleted = false;
CREATE INDEX idx_quick_consultations_created_at ON quick_consultations(created_at DESC);
CREATE INDEX idx_quick_consultations_consent ON quick_consultations(personal_info_consent, third_party_consent, marketing_consent) WHERE is_deleted = false;

-- =============================================================================
-- 2. 제휴/광고 문의 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS partnership_inquiries (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 문의자 정보
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL,

    -- 기본 정보
    inquiry_type VARCHAR(50) NOT NULL,
    -- PARTNERSHIP: 제휴 문의
    -- ADVERTISING: 광고 문의
    -- SPONSORSHIP: 후원 문의
    -- BUSINESS: 사업 제안
    -- OTHER: 기타

    -- 회사 정보
    company_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    position VARCHAR(100), -- 직책
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    website_url VARCHAR(500),

    -- 문의 내용
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,

    -- 첨부 파일 (JSONB)
    attachment_files JSONB DEFAULT '[]'::JSONB,
    -- [{"file_id": 123, "file_name": "proposal.pdf", "file_size": 1024000, "file_url": "..."}]

    -- 예산 정보 (광고 문의 시)
    budget_range VARCHAR(50), -- UNDER_1M, 1M_5M, 5M_10M, OVER_10M
    preferred_ad_type VARCHAR(50), -- LISTING, BANNER, POPUP, AI_RECOMMENDATION, DAMOA_PICK
    expected_duration VARCHAR(50), -- 예상 기간 (1개월, 3개월, 6개월, 1년 등)

    -- 배정/처리 정보
    assigned_admin_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP WITH TIME ZONE,

    -- 상태 관리
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    -- SUBMITTED: 제출됨
    -- IN_REVIEW: 검토중
    -- RESPONDED: 응답 완료
    -- IN_NEGOTIATION: 협상중
    -- ACCEPTED: 수락
    -- REJECTED: 거절
    -- COMPLETED: 완료

    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT

    -- 응답 정보
    response_message TEXT,
    responded_at TIMESTAMP WITH TIME ZONE,
    responded_by BIGINT REFERENCES users(id),

    -- 완료 정보
    completed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,

    -- 관리자 전용 메모
    admin_notes TEXT,

    -- IP/접속 정보
    ip_address INET,
    user_agent TEXT,

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT REFERENCES users(id)
);

-- 인덱스
CREATE INDEX idx_partnership_inquiries_uuid ON partnership_inquiries(uuid);
CREATE INDEX idx_partnership_inquiries_user_id ON partnership_inquiries(user_id) WHERE is_deleted = false;
CREATE INDEX idx_partnership_inquiries_company_id ON partnership_inquiries(company_id) WHERE is_deleted = false;
CREATE INDEX idx_partnership_inquiries_inquiry_type ON partnership_inquiries(inquiry_type) WHERE is_deleted = false;
CREATE INDEX idx_partnership_inquiries_status ON partnership_inquiries(status) WHERE is_deleted = false;
CREATE INDEX idx_partnership_inquiries_assigned_admin ON partnership_inquiries(assigned_admin_id) WHERE is_deleted = false;
CREATE INDEX idx_partnership_inquiries_priority ON partnership_inquiries(priority) WHERE is_deleted = false;
CREATE INDEX idx_partnership_inquiries_email ON partnership_inquiries(email) WHERE is_deleted = false;
CREATE INDEX idx_partnership_inquiries_created_at ON partnership_inquiries(created_at DESC);

-- =============================================================================
-- 3. 상담 메시지 스레드 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS consultation_messages (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- Polymorphic 연결 (빠른상담 또는 제휴문의)
    consultation_type VARCHAR(50) NOT NULL, -- QUICK, PARTNERSHIP
    consultation_id BIGINT NOT NULL, -- quick_consultations.id 또는 partnership_inquiries.id

    -- 발신자 정보
    sender_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    sender_type VARCHAR(20) NOT NULL, -- ADMIN, COMPANY, USER, SYSTEM

    -- 메시지 내용
    message TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'REPLY', -- REPLY, NOTE, SYSTEM

    -- 첨부파일 (JSONB)
    attachments JSONB DEFAULT '[]'::JSONB,
    -- [{"file_id": 123, "file_name": "document.pdf", "file_url": "..."}]

    -- 읽음 상태
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    read_by BIGINT REFERENCES users(id),

    -- 기본 필드
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT REFERENCES users(id)
);

-- 인덱스
CREATE INDEX idx_consultation_messages_uuid ON consultation_messages(uuid);
CREATE INDEX idx_consultation_messages_consultation ON consultation_messages(consultation_type, consultation_id) WHERE is_deleted = false;
CREATE INDEX idx_consultation_messages_sender ON consultation_messages(sender_id) WHERE is_deleted = false;
CREATE INDEX idx_consultation_messages_sender_type ON consultation_messages(sender_type) WHERE is_deleted = false;
CREATE INDEX idx_consultation_messages_is_read ON consultation_messages(is_read) WHERE is_deleted = false;
CREATE INDEX idx_consultation_messages_created_at ON consultation_messages(created_at DESC);

-- =============================================================================
-- 트리거: updated_at 자동 업데이트
-- =============================================================================
CREATE TRIGGER update_quick_consultations_updated_at BEFORE UPDATE ON quick_consultations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_partnership_inquiries_updated_at BEFORE UPDATE ON partnership_inquiries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- 테이블 코멘트
-- =============================================================================
COMMENT ON TABLE quick_consultations IS '빠른상담 요청 테이블 - 사용자 빠른 상담 신청 및 3가지 동의 관리';
COMMENT ON TABLE partnership_inquiries IS '제휴/광고 문의 테이블 - 제휴, 광고, 후원 등 사업적 문의 관리';
COMMENT ON TABLE consultation_messages IS '상담 메시지 스레드 테이블 - 빠른상담/제휴문의 후속 메시지 관리';

-- 주요 컬럼 코멘트
COMMENT ON COLUMN quick_consultations.user_id IS '요청자 ID (비회원 시 NULL 허용)';
COMMENT ON COLUMN quick_consultations.personal_info_consent IS '개인정보 수집/이용 동의';
COMMENT ON COLUMN quick_consultations.third_party_consent IS '제3자 제공 동의';
COMMENT ON COLUMN quick_consultations.marketing_consent IS '마케팅 수신 동의';
COMMENT ON COLUMN quick_consultations.consent_ip_address IS '동의 시 IP 주소 (법적 증빙)';
COMMENT ON COLUMN quick_consultations.consent_version IS '동의 약관 버전';
COMMENT ON COLUMN quick_consultations.status IS '상담 상태 (SUBMITTED/ASSIGNED/IN_PROGRESS/RESPONDED/COMPLETED/CANCELLED)';

COMMENT ON COLUMN partnership_inquiries.inquiry_type IS '문의 유형 (PARTNERSHIP/ADVERTISING/SPONSORSHIP/BUSINESS/OTHER)';
COMMENT ON COLUMN partnership_inquiries.budget_range IS '예산 범위 (UNDER_1M/1M_5M/5M_10M/OVER_10M)';
COMMENT ON COLUMN partnership_inquiries.preferred_ad_type IS '선호 광고 타입 (LISTING/BANNER/POPUP/AI_RECOMMENDATION/DAMOA_PICK)';
COMMENT ON COLUMN partnership_inquiries.priority IS '우선순위 (LOW/NORMAL/HIGH/URGENT)';
COMMENT ON COLUMN partnership_inquiries.status IS '문의 상태 (SUBMITTED/IN_REVIEW/RESPONDED/IN_NEGOTIATION/ACCEPTED/REJECTED/COMPLETED)';

COMMENT ON COLUMN consultation_messages.consultation_type IS 'Polymorphic 타입 (QUICK: 빠른상담, PARTNERSHIP: 제휴문의)';
COMMENT ON COLUMN consultation_messages.sender_type IS '발신자 유형 (ADMIN/COMPANY/USER/SYSTEM)';
COMMENT ON COLUMN consultation_messages.message_type IS '메시지 유형 (REPLY: 답변, NOTE: 내부메모, SYSTEM: 시스템)';

-- =============================================================================
-- 어드민 페이지 권한 추가 (7개)
-- =============================================================================

-- 상담 관리 메인 메뉴
INSERT INTO admin_page_permissions (
    page_code, page_name, page_url, parent_page_id, page_level, module,
    display_order, icon, page_type, required_permissions, is_public
) VALUES
('CONSULTATION_MANAGEMENT', '상담 관리', '/admin/consultations', NULL, 0, 'CONSULTATION',
 35, 'message-square', 'PAGE', ARRAY['VIEW_CONSULTATIONS'], false);

-- 빠른상담 관련 페이지
INSERT INTO admin_page_permissions (
    page_code, page_name, page_url, parent_page_id, page_level, module,
    display_order, icon, page_type, required_permissions, is_public
) VALUES
('QUICK_CONSULTATION_LIST', '빠른상담 목록', '/admin/consultations/quick',
 (SELECT id FROM admin_page_permissions WHERE page_code = 'CONSULTATION_MANAGEMENT'),
 1, 'CONSULTATION', 36, 'zap', 'PAGE', ARRAY['VIEW_CONSULTATIONS'], false),

('QUICK_CONSULTATION_DETAIL', '빠른상담 상세', '/admin/consultations/quick/*',
 (SELECT id FROM admin_page_permissions WHERE page_code = 'CONSULTATION_MANAGEMENT'),
 1, 'CONSULTATION', 37, 'eye', 'PAGE', ARRAY['VIEW_CONSULTATIONS'], false);

-- 제휴/광고 문의 관련 페이지
INSERT INTO admin_page_permissions (
    page_code, page_name, page_url, parent_page_id, page_level, module,
    display_order, icon, page_type, required_permissions, is_public
) VALUES
('PARTNERSHIP_INQUIRY_LIST', '제휴/광고 문의 목록', '/admin/consultations/partnership',
 (SELECT id FROM admin_page_permissions WHERE page_code = 'CONSULTATION_MANAGEMENT'),
 1, 'CONSULTATION', 38, 'handshake', 'PAGE', ARRAY['VIEW_INQUIRIES'], false),

('PARTNERSHIP_INQUIRY_DETAIL', '제휴/광고 상세', '/admin/consultations/partnership/*',
 (SELECT id FROM admin_page_permissions WHERE page_code = 'CONSULTATION_MANAGEMENT'),
 1, 'CONSULTATION', 39, 'file-text', 'PAGE', ARRAY['VIEW_INQUIRIES'], false);

-- 상담 통계 및 동의 관리
INSERT INTO admin_page_permissions (
    page_code, page_name, page_url, parent_page_id, page_level, module,
    display_order, icon, page_type, required_permissions, is_public
) VALUES
('CONSULTATION_STATS', '상담 통계', '/admin/consultations/stats',
 (SELECT id FROM admin_page_permissions WHERE page_code = 'CONSULTATION_MANAGEMENT'),
 1, 'CONSULTATION', 40, 'bar-chart', 'PAGE', ARRAY['VIEW_ANALYTICS'], false),

('CONSENT_MANAGEMENT', '동의 기록 관리', '/admin/consultations/consents',
 (SELECT id FROM admin_page_permissions WHERE page_code = 'CONSULTATION_MANAGEMENT'),
 1, 'CONSULTATION', 41, 'shield', 'PAGE', ARRAY['VIEW_CONSENTS'], false);

-- =============================================================================
-- 어드민 역할별 페이지 접근 권한 설정
-- =============================================================================

-- SUPER_ADMIN: 모든 상담 관리 페이지 접근
INSERT INTO admin_role_page_access (role_name, page_id, access_type, created_by)
SELECT 'SUPER_ADMIN', id, 'FULL', 1
FROM admin_page_permissions
WHERE module = 'CONSULTATION'
  AND is_deleted = false
ON CONFLICT (role_name, page_id) DO NOTHING;

-- CONSULTATION_MANAGER: 상담 관리 전담 역할 (새 역할)
INSERT INTO admin_role_page_access (role_name, page_id, access_type, created_by)
SELECT 'CONSULTATION_MANAGER', id, 'FULL', 1
FROM admin_page_permissions
WHERE module IN ('CONSULTATION', 'DASHBOARD')
  AND is_deleted = false
ON CONFLICT (role_name, page_id) DO NOTHING;

-- CONTENT_MANAGER: 상담 읽기 전용
INSERT INTO admin_role_page_access (role_name, page_id, access_type, created_by)
SELECT 'CONTENT_MANAGER', id, 'READ_ONLY', 1
FROM admin_page_permissions
WHERE module = 'CONSULTATION'
  AND is_deleted = false
ON CONFLICT (role_name, page_id) DO NOTHING;

-- USER_MANAGER: 빠른상담 관련 페이지만 접근
INSERT INTO admin_role_page_access (role_name, page_id, access_type, created_by)
SELECT 'USER_MANAGER', id, 'FULL', 1
FROM admin_page_permissions
WHERE page_code IN ('CONSULTATION_MANAGEMENT', 'QUICK_CONSULTATION_LIST', 'QUICK_CONSULTATION_DETAIL', 'CONSULTATION_STATS')
  AND is_deleted = false
ON CONFLICT (role_name, page_id) DO NOTHING;

-- COMPANY_MANAGER: 제휴/광고 문의 관련 페이지만 접근
INSERT INTO admin_role_page_access (role_name, page_id, access_type, created_by)
SELECT 'COMPANY_MANAGER', id, 'FULL', 1
FROM admin_page_permissions
WHERE page_code IN ('CONSULTATION_MANAGEMENT', 'PARTNERSHIP_INQUIRY_LIST', 'PARTNERSHIP_INQUIRY_DETAIL', 'CONSULTATION_STATS')
  AND is_deleted = false
ON CONFLICT (role_name, page_id) DO NOTHING;

-- =============================================================================
-- 뷰: 상담 통계 대시보드
-- =============================================================================
CREATE OR REPLACE VIEW v_consultation_dashboard AS
SELECT
    -- 빠른상담 통계
    (SELECT COUNT(*) FROM quick_consultations WHERE is_deleted = false) as total_quick_consultations,
    (SELECT COUNT(*) FROM quick_consultations WHERE status = 'SUBMITTED' AND is_deleted = false) as pending_quick_consultations,
    (SELECT COUNT(*) FROM quick_consultations WHERE status = 'COMPLETED' AND is_deleted = false) as completed_quick_consultations,
    (SELECT COUNT(*) FROM quick_consultations WHERE DATE(created_at) = CURRENT_DATE AND is_deleted = false) as today_quick_consultations,

    -- 제휴/광고 문의 통계
    (SELECT COUNT(*) FROM partnership_inquiries WHERE is_deleted = false) as total_partnership_inquiries,
    (SELECT COUNT(*) FROM partnership_inquiries WHERE status = 'SUBMITTED' AND is_deleted = false) as pending_partnership_inquiries,
    (SELECT COUNT(*) FROM partnership_inquiries WHERE priority = 'URGENT' AND status NOT IN ('COMPLETED', 'REJECTED') AND is_deleted = false) as urgent_partnership_inquiries,
    (SELECT COUNT(*) FROM partnership_inquiries WHERE DATE(created_at) = CURRENT_DATE AND is_deleted = false) as today_partnership_inquiries,

    -- 메시지 통계
    (SELECT COUNT(*) FROM consultation_messages WHERE is_read = false AND is_deleted = false) as unread_messages,

    -- 동의 통계
    (SELECT COUNT(*) FROM quick_consultations WHERE marketing_consent = true AND is_deleted = false) as marketing_consent_count,

    -- 최근 7일 통계
    (SELECT COUNT(*) FROM quick_consultations WHERE created_at > CURRENT_DATE - INTERVAL '7 days' AND is_deleted = false) as quick_consultations_last_7days,
    (SELECT COUNT(*) FROM partnership_inquiries WHERE created_at > CURRENT_DATE - INTERVAL '7 days' AND is_deleted = false) as partnership_inquiries_last_7days,

    -- 업데이트 시간
    CURRENT_TIMESTAMP as calculated_at;

COMMENT ON VIEW v_consultation_dashboard IS '상담 통계 대시보드 뷰 - 빠른상담 및 제휴문의 현황';

-- =============================================================================
-- 초기 데이터: 시스템 설정
-- =============================================================================
INSERT INTO admin_settings (setting_key, setting_value, setting_type, category, description, is_public, is_editable, created_by)
VALUES
('consultation.auto_assign_enabled', 'false', 'BOOLEAN', 'CONSULTATION', '빠른상담 자동 배정 활성화', false, true, 1),
('consultation.response_sla_hours', '24', 'NUMBER', 'CONSULTATION', '상담 응답 SLA (시간)', false, true, 1),
('consultation.notification_enabled', 'true', 'BOOLEAN', 'CONSULTATION', '상담 알림 활성화', false, true, 1),
('partnership.review_sla_days', '3', 'NUMBER', 'CONSULTATION', '제휴문의 검토 SLA (일)', false, true, 1),
('consent.retention_years', '3', 'NUMBER', 'CONSULTATION', '동의 기록 보관 기간 (년)', false, true, 1)
ON CONFLICT (setting_key) DO NOTHING;

-- =============================================================================
-- 완료 메시지
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'V5 마이그레이션 완료: 빠른상담 & 제휴/광고 문의 시스템';
    RAISE NOTICE '- 생성된 테이블: 3개 (quick_consultations, partnership_inquiries, consultation_messages)';
    RAISE NOTICE '- 생성된 뷰: 1개 (v_consultation_dashboard)';
    RAISE NOTICE '- 생성된 트리거: 2개';
    RAISE NOTICE '- 추가된 어드민 페이지: 7개';
    RAISE NOTICE '- 역할별 페이지 접근 권한 설정 완료';
    RAISE NOTICE '- 초기 시스템 설정: 5개';
    RAISE NOTICE '=============================================================================';
END $$;