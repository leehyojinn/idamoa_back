--
-- V42: Board 덤프 데이터 마이그레이션
-- 이전 시스템(damoa_board)에서 현재 시스템(boards)으로 데이터 이전
--

-- ============================================
-- 1. 임시 테이블 생성 (덤프 데이터 저장용)
-- ============================================

CREATE TEMPORARY TABLE temp_damoa_board (
    seq INTEGER,
    uuid UUID,
    title TEXT,
    content TEXT,
    tags TEXT[],
    metadata JSON,
    like_count INTEGER,
    view_count INTEGER,
    create_datetime TIMESTAMPTZ,
    update_datetime TIMESTAMPTZ,
    delete_datetime TIMESTAMPTZ,
    category_name TEXT,
    writer_uuid UUID,
    group_id TEXT,
    primary_thumbnail_url TEXT,
    start_datetime TIMESTAMPTZ,
    end_datetime TIMESTAMPTZ
);

-- ============================================
-- 2. 덤프 데이터 INSERT (board_dump_file.sql에서 복사)
-- ============================================

-- TODO: 실제 덤프 파일의 모든 INSERT 문을 여기에 추가해야 합니다.
-- 예시 데이터:
INSERT INTO temp_damoa_board (seq, uuid, title, content, tags, metadata, like_count, view_count, create_datetime, update_datetime, delete_datetime, category_name, writer_uuid, group_id, primary_thumbnail_url, start_datetime, end_datetime) VALUES
(47, '9ad52d6c-41f1-4e89-8045-ad91c4a22e45', '미소퀸성형외과', '', '{100평,소파,미소퀸성형외과,와플레이스,실사,100평이하,피부과,성형외과,대기실,모던,클래식,럭셔리,그레이,레드,그린,도장,도배,타일/대리석}', '{"link":"https://waplace.kr/bbs/board.php?bo_table=portfolio&sca=HOSPITAL&page=5#group_8-1","copyright":"waplace","thumbnail_url":"images/250922/51c79aa6-4e8e-4eb4-a86e-91f5fb3a2616-pre.jpg"}', 0, 3, '2025-09-21 18:02:45.256253+00', '2025-09-28 18:50:14.512911+00', NULL, 'photo', '97957035-5cba-47eb-987b-593bf1bcf45f', NULL, '', NULL, NULL);

-- ============================================
-- 3. Category Name → Board Type 매핑 테이블
-- ============================================

CREATE TEMPORARY TABLE category_type_mapping (
    category_name TEXT PRIMARY KEY,
    board_type TEXT
);

INSERT INTO category_type_mapping (category_name, board_type) VALUES
    ('resource', 'DOCUMENT'),
    ('photo', 'GALLERY'),
    ('contact', 'FAQ'),
    ('review', 'GALLERY'),
    ('notice', 'NOTICE');

-- ============================================
-- 4. boards 테이블에 데이터 삽입
-- ============================================

INSERT INTO boards (
    uuid,
    user_id,
    board_type,
    category_id,
    title,
    content,
    type_data,
    view_count,
    like_count,
    comment_count,
    is_pinned,
    is_featured,
    is_published,
    published_at,
    tags,
    is_private,
    event_status,
    created_at,
    updated_at,
    is_deleted,
    deleted_at
)
SELECT
    t.uuid,
    NULL,  -- user_id는 NULL (User 데이터 없음)
    COALESCE(m.board_type, 'GALLERY'),  -- 기본값 GALLERY
    bc.id,  -- category_id
    t.title,
    t.content,
    -- type_data: metadata + thumbnail + event dates를 JSONB로 병합
    COALESCE(t.metadata::jsonb, '{}'::jsonb) ||
        CASE WHEN t.primary_thumbnail_url IS NOT NULL AND t.primary_thumbnail_url != ''
             THEN jsonb_build_object('thumbnailUrl', t.primary_thumbnail_url)
             ELSE '{}'::jsonb
        END ||
        CASE WHEN t.start_datetime IS NOT NULL
             THEN jsonb_build_object('startDateTime', t.start_datetime)
             ELSE '{}'::jsonb
        END ||
        CASE WHEN t.end_datetime IS NOT NULL
             THEN jsonb_build_object('endDateTime', t.end_datetime)
             ELSE '{}'::jsonb
        END,
    COALESCE(t.view_count, 0),
    COALESCE(t.like_count, 0),
    0,  -- comment_count
    false,  -- is_pinned
    false,  -- is_featured
    true,   -- is_published
    t.create_datetime AT TIME ZONE 'UTC',  -- published_at
    t.tags,
    false,  -- is_private
    CASE WHEN m.board_type = 'EVENT' THEN 'ACTIVE' ELSE NULL END,  -- event_status
    t.create_datetime AT TIME ZONE 'UTC',
    t.update_datetime AT TIME ZONE 'UTC',
    CASE WHEN t.delete_datetime IS NOT NULL THEN true ELSE false END,
    t.delete_datetime AT TIME ZONE 'UTC'
FROM temp_damoa_board t
LEFT JOIN category_type_mapping m ON m.category_name = t.category_name
LEFT JOIN board_categories bc ON bc.slug = t.category_name
WHERE NOT EXISTS (
    SELECT 1 FROM boards b WHERE b.uuid = t.uuid
);

-- ============================================
-- 5. 시퀀스 업데이트
-- ============================================

SELECT setval('boards_id_seq', COALESCE((SELECT MAX(id) FROM boards), 1), true);

-- ============================================
-- 6. 통계 출력
-- ============================================

DO $$
DECLARE
    v_imported INTEGER;
    v_total INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM temp_damoa_board;
    SELECT COUNT(*) INTO v_imported FROM boards WHERE uuid IN (SELECT uuid FROM temp_damoa_board);

    RAISE NOTICE 'Board Migration Complete: % / % imported', v_imported, v_total;
END $$;

-- 임시 테이블 정리
DROP TABLE IF EXISTS category_type_mapping;
DROP TABLE IF EXISTS temp_damoa_board;
