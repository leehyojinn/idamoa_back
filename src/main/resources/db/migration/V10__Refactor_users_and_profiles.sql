-- ==============================================================================
-- V10__Refactor_users_and_profiles.sql
-- 회원가입 플로우 재설계: users, user_profiles 테이블 리팩토링
-- 작성일: 2025-11-04
-- ==============================================================================

-- ============================================
-- Part 1: users 테이블 수정
-- ============================================

-- 1-1. role을 text[]로 변경 (Entity와 일치)
-- 기존 role ENUM을 text[]로 변환
ALTER TABLE users
ADD COLUMN roles text[];

-- 기존 role 데이터를 roles 배열로 복사
UPDATE users
SET roles = ARRAY[role::text];

-- 기존 role 컬럼 삭제
ALTER TABLE users DROP COLUMN role;

-- roles를 NOT NULL로 설정하고 기본값 지정
ALTER TABLE users
ALTER COLUMN roles SET NOT NULL,
ALTER COLUMN roles SET DEFAULT '{USER}'::text[];

COMMENT ON COLUMN users.roles IS '사용자 역할 배열 (USER, COMPANY, ADMIN 등)';

-- 1-2. 약관 동의 boolean 컬럼 추가
ALTER TABLE users
ADD COLUMN terms_agreed BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN privacy_agreed BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN marketing_agreed BOOLEAN DEFAULT FALSE;

-- 기존 _at 컬럼 값이 있으면 boolean을 true로 설정
UPDATE users
SET terms_agreed = TRUE
WHERE terms_agreed_at IS NOT NULL;

UPDATE users
SET privacy_agreed = TRUE
WHERE privacy_agreed_at IS NOT NULL;

UPDATE users
SET marketing_agreed = TRUE
WHERE marketing_agreed_at IS NOT NULL;

COMMENT ON COLUMN users.terms_agreed IS '이용약관 동의 여부';
COMMENT ON COLUMN users.privacy_agreed IS '개인정보 처리방침 동의 여부';
COMMENT ON COLUMN users.marketing_agreed IS '마케팅 수신 동의 여부';

-- 1-3. 프로필 완료 컬럼 추가
ALTER TABLE users
ADD COLUMN profile_completed BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN profile_completed_at TIMESTAMP;

-- 기존 user_profiles 또는 companies가 있으면 profile_completed = true
UPDATE users u
SET profile_completed = TRUE,
    profile_completed_at = u.created_at
WHERE EXISTS (
    SELECT 1 FROM user_profiles up WHERE up.user_id = u.id
) OR EXISTS (
    SELECT 1 FROM companies c WHERE c.owner_id = u.id
);

COMMENT ON COLUMN users.profile_completed IS '프로필 설정 완료 여부';
COMMENT ON COLUMN users.profile_completed_at IS '프로필 설정 완료 시간';

-- ============================================
-- Part 2: user_profiles 테이블 정리
-- ============================================

-- 불필요한 컬럼 제거 (개인정보 제거)
ALTER TABLE user_profiles
DROP COLUMN IF EXISTS birth_date,
DROP COLUMN IF EXISTS gender,
DROP COLUMN IF EXISTS interests,
DROP COLUMN IF EXISTS address_detail;

-- profile_visibility 컬럼 추가 (Entity에 있는데 DB에 없을 수 있음)
ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS profile_visibility VARCHAR(20) DEFAULT 'PUBLIC' NOT NULL;

COMMENT ON COLUMN user_profiles.profile_visibility IS '프로필 공개 여부 (PUBLIC, PRIVATE, FRIENDS_ONLY)';

-- metadata 컬럼 추가 (BaseEntity 상속으로 필요)
ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

COMMENT ON COLUMN user_profiles.metadata IS '확장 데이터 (JSONB)';

-- profile_type 컬럼 추가 (USER_PROFILE or COMPANY 구분)
ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS profile_type VARCHAR(20) DEFAULT 'USER_PROFILE' NOT NULL;

COMMENT ON COLUMN user_profiles.profile_type IS '프로필 타입 (USER_PROFILE, COMPANY)';

-- ============================================
-- Part 3: 인덱스 추가
-- ============================================

CREATE INDEX IF NOT EXISTS idx_users_roles ON users USING GIN (roles);
CREATE INDEX IF NOT EXISTS idx_users_profile_completed ON users(profile_completed);
