-- =====================================================
-- V84: 커뮤니티 테이블에 metadata 컬럼 추가
-- BaseEntity의 metadata JSONB 필드 지원
-- =====================================================

-- community_categories
ALTER TABLE community_categories ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- community_posts
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- community_comments
ALTER TABLE community_comments ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';
