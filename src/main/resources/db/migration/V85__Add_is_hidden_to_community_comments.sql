-- =====================================================
-- V85: 커뮤니티 댓글에 is_hidden 컬럼 추가
-- 관리자가 댓글을 비공개 처리할 수 있는 기능
-- =====================================================

ALTER TABLE community_comments ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN DEFAULT false NOT NULL;

-- 인덱스 추가 (비공개 댓글 조회용)
CREATE INDEX IF NOT EXISTS idx_community_comments_hidden ON community_comments(is_hidden) WHERE is_hidden = true;
