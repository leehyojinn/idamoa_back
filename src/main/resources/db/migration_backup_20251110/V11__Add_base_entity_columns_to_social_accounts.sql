-- ==============================================================================
-- V11__Add_base_entity_columns_to_social_accounts.sql
-- social_accounts 테이블 스키마를 Entity와 일치시킴
-- - BaseEntity 컬럼 추가 (is_deleted, deleted_at, metadata)
-- - 컬럼명 변경 (provider_id → provider_user_id, expires_at → token_expires_at, raw_data → profile_data)
-- - linked_at 컬럼 추가
-- - provider_image 컬럼 삭제
-- 작성일: 2025-11-05
-- ==============================================================================

-- Part 1: 컬럼명 변경 (Entity와 일치)
-- ============================================

-- 1-1. provider_id → provider_user_id
ALTER TABLE social_accounts
RENAME COLUMN provider_id TO provider_user_id;

COMMENT ON COLUMN social_accounts.provider_user_id IS 'OAuth 제공자의 사용자 ID';

-- 1-2. expires_at → token_expires_at
ALTER TABLE social_accounts
RENAME COLUMN expires_at TO token_expires_at;

COMMENT ON COLUMN social_accounts.token_expires_at IS '액세스 토큰 만료 시간';

-- 1-3. raw_data → profile_data
ALTER TABLE social_accounts
RENAME COLUMN raw_data TO profile_data;

COMMENT ON COLUMN social_accounts.profile_data IS 'OAuth 프로필 데이터 (JSONB)';

-- Part 2: 컬럼 추가
-- ============================================

-- 2-1. linked_at 컬럼 추가 (소셜 계정 연동 시간)
ALTER TABLE social_accounts
ADD COLUMN linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

COMMENT ON COLUMN social_accounts.linked_at IS '소셜 계정 연동 시간';

-- 2-2. is_deleted 컬럼 추가 (Soft delete 패턴)
ALTER TABLE social_accounts
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN social_accounts.is_deleted IS '소프트 삭제 여부';

-- 2-3. deleted_at 컬럼 추가
ALTER TABLE social_accounts
ADD COLUMN deleted_at TIMESTAMP;

COMMENT ON COLUMN social_accounts.deleted_at IS '삭제 시간';

-- 2-4. metadata 컬럼 추가 (BaseEntity 확장 데이터)
ALTER TABLE social_accounts
ADD COLUMN metadata JSONB DEFAULT '{}'::jsonb;

COMMENT ON COLUMN social_accounts.metadata IS '확장 데이터 (JSONB)';

-- Part 3: 컬럼 삭제
-- ============================================

-- 3-1. provider_image 컬럼 삭제 (Entity에 없음, profile_data에 포함)
ALTER TABLE social_accounts
DROP COLUMN IF EXISTS provider_image;

-- Part 4: 인덱스 추가
-- ============================================

-- 4-1. is_deleted 인덱스 (Soft delete 필터링)
CREATE INDEX idx_social_accounts_is_deleted ON social_accounts(is_deleted);

-- 4-2. 복합 인덱스 (provider, provider_user_id로 조회 시 is_deleted 체크)
CREATE INDEX idx_social_accounts_provider_not_deleted
ON social_accounts(provider, provider_user_id)
WHERE is_deleted = FALSE;
