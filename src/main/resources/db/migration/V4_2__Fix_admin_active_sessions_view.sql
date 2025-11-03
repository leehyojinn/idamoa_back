-- =============================================================================
-- V4.2: v_admin_active_sessions 뷰 수정
-- =============================================================================
-- users 테이블에 name 컬럼이 없어서 user_profiles와 조인하도록 수정

-- 기존 뷰 삭제
DROP VIEW IF EXISTS v_admin_active_sessions;

-- 뷰 재생성
CREATE OR REPLACE VIEW v_admin_active_sessions AS
SELECT
    s.id,
    s.uuid,
    s.admin_user_id,
    u.email,
    up.name,
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
LEFT JOIN user_profiles up ON up.user_id = s.admin_user_id
LEFT JOIN admin_user_roles aur ON aur.admin_user_id = s.admin_user_id
    AND (aur.expires_at IS NULL OR aur.expires_at > CURRENT_TIMESTAMP)
LEFT JOIN admin_roles ar ON ar.id = aur.role_id AND ar.is_active = true
WHERE s.status = 'ACTIVE'
  AND s.expires_at > CURRENT_TIMESTAMP
ORDER BY ar.priority DESC, s.last_activity_at DESC;

-- 코멘트 추가
COMMENT ON VIEW v_admin_active_sessions IS '활성 어드민 세션 뷰 - 현재 로그인된 어드민 세션 정보';

-- =============================================================================
-- 완료 메시지
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'V4.2 마이그레이션 완료: v_admin_active_sessions 뷰 수정';
    RAISE NOTICE '- user_profiles 테이블과 조인하여 name 필드 조회';
    RAISE NOTICE '=============================================================================';
END $$;