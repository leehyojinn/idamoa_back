-- =====================================================
-- V83: 커뮤니티 테이블 생성
-- DCInside 스타일 커뮤니티 게시판
-- =====================================================

-- 1. 커뮤니티 카테고리
CREATE TABLE community_categories (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(100),

    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,

    -- 카테고리별 설정
    allow_anonymous BOOLEAN DEFAULT false,
    require_login BOOLEAN DEFAULT true,
    allow_attachments BOOLEAN DEFAULT true,
    max_attachments INTEGER DEFAULT 10,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_community_categories_slug ON community_categories(slug);
CREATE INDEX idx_community_categories_order ON community_categories(display_order);
CREATE INDEX idx_community_categories_active ON community_categories(is_active) WHERE is_deleted = false;

-- 2. 커뮤니티 게시글
CREATE TABLE community_posts (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    category_id BIGINT NOT NULL REFERENCES community_categories(id),
    user_id BIGINT NOT NULL REFERENCES users(id),

    title VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(20) DEFAULT 'TEXT',

    -- 익명 정보 (로그인 필수, 닉네임만 숨김)
    is_anonymous BOOLEAN DEFAULT false,

    -- 통계
    view_count INTEGER DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    dislike_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,

    -- 상태
    is_pinned BOOLEAN DEFAULT false,
    is_notice BOOLEAN DEFAULT false,
    is_published BOOLEAN DEFAULT true,

    -- IP 추적 (관리용)
    ip_address VARCHAR(45),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_community_posts_category ON community_posts(category_id);
CREATE INDEX idx_community_posts_user ON community_posts(user_id);
CREATE INDEX idx_community_posts_created ON community_posts(created_at DESC);
CREATE INDEX idx_community_posts_pinned ON community_posts(is_pinned DESC, created_at DESC) WHERE is_deleted = false AND is_published = true;
CREATE INDEX idx_community_posts_notice ON community_posts(is_notice DESC, created_at DESC) WHERE is_deleted = false AND is_published = true;

-- 3. 커뮤니티 첨부파일
CREATE TABLE community_post_attachments (
    id BIGSERIAL PRIMARY KEY,

    post_id BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL,

    attachment_type VARCHAR(20) DEFAULT 'IMAGE',
    display_order INTEGER DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_attachments_post ON community_post_attachments(post_id);

-- 4. 커뮤니티 댓글 (무한 대댓글)
CREATE TABLE community_comments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    post_id BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES community_comments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),

    content TEXT NOT NULL,

    -- 익명 정보 (로그인 필수, 닉네임만 숨김)
    is_anonymous BOOLEAN DEFAULT false,

    -- 통계
    like_count INTEGER DEFAULT 0,
    dislike_count INTEGER DEFAULT 0,

    -- 계층 정보
    depth INTEGER DEFAULT 0,

    ip_address VARCHAR(45),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_community_comments_post ON community_comments(post_id);
CREATE INDEX idx_community_comments_parent ON community_comments(parent_id);
CREATE INDEX idx_community_comments_created ON community_comments(created_at);
CREATE INDEX idx_community_comments_user ON community_comments(user_id);

-- 5. 커뮤니티 좋아요/싫어요
CREATE TABLE community_likes (
    id BIGSERIAL PRIMARY KEY,

    post_id BIGINT REFERENCES community_posts(id) ON DELETE CASCADE,
    comment_id BIGINT REFERENCES community_comments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),

    like_type VARCHAR(10) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_like_type CHECK (like_type IN ('LIKE', 'DISLIKE')),
    CONSTRAINT chk_target CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL) OR
        (post_id IS NULL AND comment_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_community_likes_post_user ON community_likes(post_id, user_id) WHERE post_id IS NOT NULL;
CREATE UNIQUE INDEX idx_community_likes_comment_user ON community_likes(comment_id, user_id) WHERE comment_id IS NOT NULL;
CREATE INDEX idx_community_likes_user ON community_likes(user_id);

-- 기본 카테고리 추가
INSERT INTO community_categories (name, slug, description, display_order, allow_anonymous, require_login, allow_attachments)
VALUES
    ('자유게시판', 'free', '자유롭게 이야기하는 공간입니다', 1, true, true, true),
    ('질문게시판', 'question', '궁금한 것을 질문하세요', 2, false, true, true),
    ('정보공유', 'info', '유용한 정보를 공유하는 공간입니다', 3, false, true, true);
