-- =============================================================================
-- V4.1: 어드민 역할별 페이지 접근 권한 초기 데이터
-- =============================================================================
-- admin_roles 테이블은 V4에서 이미 생성됨
-- 여기서는 역할별 페이지 접근 권한 매핑 데이터만 추가합니다.

-- =============================================================================
-- 초기 데이터: 역할별 페이지 접근 권한
-- =============================================================================
INSERT INTO admin_role_page_access (role_name, page_id, access_type)
SELECT 'SUPER_ADMIN', id, 'FULL'
FROM admin_page_permissions
WHERE is_deleted = false
ON CONFLICT DO NOTHING;

-- USER_MANAGER 역할 권한
INSERT INTO admin_role_page_access (role_name, page_id, access_type)
SELECT 'USER_MANAGER', id, 'FULL'
FROM admin_page_permissions
WHERE module IN ('USER', 'DASHBOARD')
  AND is_deleted = false
ON CONFLICT DO NOTHING;

-- COMPANY_MANAGER 역할 권한
INSERT INTO admin_role_page_access (role_name, page_id, access_type)
SELECT 'COMPANY_MANAGER', id, 'FULL'
FROM admin_page_permissions
WHERE module IN ('COMPANY', 'DASHBOARD')
  AND is_deleted = false
ON CONFLICT DO NOTHING;

-- CONTENT_MANAGER 역할 권한
INSERT INTO admin_role_page_access (role_name, page_id, access_type)
SELECT 'CONTENT_MANAGER', id, 'FULL'
FROM admin_page_permissions
WHERE module IN ('CONTENT', 'DASHBOARD')
  AND is_deleted = false
ON CONFLICT DO NOTHING;

-- ESTIMATE_MANAGER 역할 권한
INSERT INTO admin_role_page_access (role_name, page_id, access_type)
SELECT 'ESTIMATE_MANAGER', id, 'FULL'
FROM admin_page_permissions
WHERE module IN ('ESTIMATE', 'DASHBOARD')
  AND is_deleted = false
ON CONFLICT DO NOTHING;

-- PAYMENT_MANAGER 역할 권한
INSERT INTO admin_role_page_access (role_name, page_id, access_type)
SELECT 'PAYMENT_MANAGER', id, 'FULL'
FROM admin_page_permissions
WHERE module IN ('PAYMENT', 'DASHBOARD')
  AND is_deleted = false
ON CONFLICT DO NOTHING;

-- AUDIT_VIEWER 역할 권한 (읽기 전용)
INSERT INTO admin_role_page_access (role_name, page_id, access_type)
SELECT 'AUDIT_VIEWER', id, 'READ_ONLY'
FROM admin_page_permissions
WHERE page_code IN ('AUDIT_LOGS', 'DASHBOARD')
  AND is_deleted = false
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 완료 메시지
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'V4.1 마이그레이션 완료: 역할별 페이지 접근 권한 초기 데이터';
    RAISE NOTICE '- 추가된 역할별 페이지 권한 매핑 (SUPER_ADMIN, USER_MANAGER, etc.)';
    RAISE NOTICE '- admin_roles 테이블은 V4에서 이미 생성됨';
    RAISE NOTICE '=============================================================================';
END $$;