-- ==============================================================================
-- V12__Make_password_nullable_for_social_login.sql
-- 소셜 로그인 전용 사용자를 위해 password 컬럼을 nullable로 변경
-- 작성일: 2025-11-05
-- ==============================================================================

-- 소셜 로그인으로만 가입한 사용자는 비밀번호가 없을 수 있음
-- User 엔티티에서도 password는 nullable로 설정되어 있음

ALTER TABLE users
ALTER COLUMN password DROP NOT NULL;

COMMENT ON COLUMN users.password IS '비밀번호 (소셜 로그인 전용 사용자는 NULL 가능)';
