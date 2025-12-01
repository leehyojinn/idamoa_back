--
-- V41: Board Category 덤프 데이터 마이그레이션
-- 이전 시스템(damoa_board_category)에서 현재 시스템(board_categories)으로 데이터 이전
--

-- ============================================
-- 1. 임시 테이블 생성 (덤프 데이터 저장용)
-- ============================================

CREATE TEMPORARY TABLE temp_damoa_board_category (
    seq INTEGER,
    category_name TEXT,
    description TEXT,
    sort_order INTEGER,
    is_active BOOLEAN,
    create_datetime TIMESTAMPTZ,
    update_datetime TIMESTAMPTZ
);

-- ============================================
-- 2. 덤프 데이터 INSERT
-- ============================================

INSERT INTO temp_damoa_board_category (seq, category_name, description, sort_order, is_active, create_datetime, update_datetime) VALUES
    (1, 'resource', '자료실', 0, true, '2025-09-07 08:43:23.787983+00', '2025-09-07 08:43:23.787983+00'),
    (2, 'photo', '사진', 0, true, '2025-09-10 15:15:31.190635+00', '2025-09-10 15:15:31.190635+00'),
    (3, 'contact', '제휴문의', 0, true, '2025-10-09 04:19:03.686+00', '2025-10-09 04:19:03.686+00'),
    (4, 'review', '고객리뷰', 0, true, '2025-10-09 04:19:29.309+00', '2025-10-09 04:19:29.309+00'),
    (5, 'notice', '공지사항/이벤트', 0, true, '2025-10-21 01:52:10.21768+00', '2025-10-21 01:52:10.21768+00');

-- ============================================
-- 3. Category Name → Board Type 매핑
-- ============================================

CREATE TEMPORARY TABLE category_type_mapping (
    category_name TEXT PRIMARY KEY,
    board_type TEXT
);

INSERT INTO category_type_mapping (category_name, board_type) VALUES
    ('resource', 'DOCUMENT'),  -- 자료실
    ('photo', 'GALLERY'),      -- 사진
    ('contact', 'FAQ'),        -- 제휴문의 (FAQ로 매핑하거나 별도 처리)
    ('review', 'GALLERY'),     -- 고객리뷰 (GALLERY로 매핑)
    ('notice', 'NOTICE');      -- 공지사항/이벤트

-- ============================================
-- 4. board_categories 테이블에 데이터 삽입
-- ============================================

INSERT INTO board_categories (
    uuid,
    name,
    slug,
    board_type,
    description,
    display_order,
    is_active,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    gen_random_uuid(),
    t.description,  -- description을 name으로 사용 (한글명)
    t.category_name,  -- category_name을 slug로 사용
    m.board_type,
    t.description,
    t.sort_order,
    t.is_active,
    t.create_datetime AT TIME ZONE 'UTC',
    t.update_datetime AT TIME ZONE 'UTC',
    false
FROM temp_damoa_board_category t
JOIN category_type_mapping m ON m.category_name = t.category_name
WHERE NOT EXISTS (
    SELECT 1 FROM board_categories bc WHERE bc.slug = t.category_name
);

-- ============================================
-- 5. 시퀀스 업데이트
-- ============================================

SELECT setval('board_categories_id_seq', COALESCE((SELECT MAX(id) FROM board_categories), 1), true);

-- ============================================
-- 6. 통계 출력
-- ============================================

DO $$
DECLARE
    v_imported INTEGER;
    v_total INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM temp_damoa_board_category;
    SELECT COUNT(*) INTO v_imported FROM board_categories WHERE slug IN (SELECT category_name FROM temp_damoa_board_category);

    RAISE NOTICE 'Board Category Migration Complete: % / % imported', v_imported, v_total;
END $$;

-- 임시 테이블 정리
DROP TABLE IF EXISTS category_type_mapping;
DROP TABLE IF EXISTS temp_damoa_board_category;
