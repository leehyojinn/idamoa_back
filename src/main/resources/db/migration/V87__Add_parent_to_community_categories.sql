-- 커뮤니티 카테고리 계층 구조 추가
-- parent_id 컬럼 추가 (자기 참조)

ALTER TABLE community_categories
    ADD COLUMN parent_id BIGINT REFERENCES community_categories(id);

-- depth 컬럼 추가 (계층 깊이, 0 = 최상위)
ALTER TABLE community_categories
    ADD COLUMN depth INTEGER DEFAULT 0 NOT NULL;

-- 인덱스 추가
CREATE INDEX idx_community_categories_parent ON community_categories(parent_id);
CREATE INDEX idx_community_categories_depth ON community_categories(depth);

-- 코멘트 추가
COMMENT ON COLUMN community_categories.parent_id IS '부모 카테고리 ID (NULL = 최상위 카테고리)';
COMMENT ON COLUMN community_categories.depth IS '카테고리 깊이 (0 = 최상위, 1 = 하위, ...)';
