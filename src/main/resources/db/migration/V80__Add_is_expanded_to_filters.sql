-- 필터 카테고리에 is_expanded 컬럼 추가 (기본값: 접힘)
ALTER TABLE filter_categories
ADD COLUMN is_expanded BOOLEAN NOT NULL DEFAULT false;

-- 필터 옵션에 is_expanded 컬럼 추가 (기본값: 접힘)
ALTER TABLE filter_options
ADD COLUMN is_expanded BOOLEAN NOT NULL DEFAULT false;

-- 컬럼 설명 추가
COMMENT ON COLUMN filter_categories.is_expanded IS '아코디언 기본 펼침 여부 (true: 펼침, false: 접힘)';
COMMENT ON COLUMN filter_options.is_expanded IS '자식 옵션이 있을 때 아코디언 기본 펼침 여부 (true: 펼침, false: 접힘)';
