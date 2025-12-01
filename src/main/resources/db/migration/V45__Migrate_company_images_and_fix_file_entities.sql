--
-- V45: Company Images 마이그레이션 및 Files entity 설정
--

-- ============================================
-- 1. Company의 thumbnailUrl을 files 테이블에 추가
-- ============================================

INSERT INTO files (
    uuid,
    original_filename,
    stored_filename,
    file_path,
    file_url,
    file_size,
    mime_type,
    file_extension,
    entity_type,
    entity_id,
    is_public,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    gen_random_uuid(),
    SUBSTRING(c.business_info->>'thumbnailUrl' FROM '[^/]+$'),
    SUBSTRING(c.business_info->>'thumbnailUrl' FROM '[^/]+$'),
    c.business_info->>'thumbnailUrl',
    'https://hip-damoa-uploads-local.s3.ap-northeast-2.amazonaws.com/' || (c.business_info->>'thumbnailUrl'),
    0,
    CASE
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.jpg' OR LOWER(c.business_info->>'thumbnailUrl') LIKE '%.jpeg' THEN 'image/jpeg'
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.png' THEN 'image/png'
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.gif' THEN 'image/gif'
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.webp' THEN 'image/webp'
        ELSE 'image/jpeg'
    END,
    CASE
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.jpg' THEN 'jpg'
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.jpeg' THEN 'jpeg'
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.png' THEN 'png'
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.gif' THEN 'gif'
        WHEN LOWER(c.business_info->>'thumbnailUrl') LIKE '%.webp' THEN 'webp'
        ELSE 'jpg'
    END,
    'COMPANY',
    c.id,
    true,
    c.created_at,
    c.updated_at,
    false
FROM companies c
WHERE c.business_info->>'thumbnailUrl' IS NOT NULL
  AND c.business_info->>'thumbnailUrl' != ''
  AND NOT EXISTS (
      SELECT 1 FROM files f
      WHERE f.file_path = c.business_info->>'thumbnailUrl'
  );

-- ============================================
-- 2. company_images 테이블에 추가
-- ============================================

INSERT INTO company_images (
    uuid,
    company_id,
    image_type,
    file_id,
    display_order,
    is_primary,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    gen_random_uuid(),
    c.id,
    'LOGO',
    f.id,
    0,
    true,
    c.created_at,
    c.updated_at,
    false
FROM companies c
JOIN files f ON f.file_path = c.business_info->>'thumbnailUrl'
WHERE c.business_info->>'thumbnailUrl' IS NOT NULL
  AND c.business_info->>'thumbnailUrl' != ''
  AND NOT EXISTS (
      SELECT 1 FROM company_images ci
      WHERE ci.company_id = c.id AND ci.file_id = f.id
  );

-- ============================================
-- 3. Board에 연결된 files의 entity_type, entity_id 업데이트
-- ============================================

UPDATE files f
SET
    entity_type = 'BOARD',
    entity_id = ba.board_id
FROM board_attachments ba
WHERE ba.file_id = f.id
  AND (f.entity_type IS NULL OR f.entity_type = 'TEMP' OR f.entity_id IS NULL);

-- ============================================
-- 4. 시퀀스 업데이트
-- ============================================

SELECT setval('files_id_seq', COALESCE((SELECT MAX(id) FROM files), 1), true);
SELECT setval('company_images_id_seq', COALESCE((SELECT MAX(id) FROM company_images), 1), true);

-- ============================================
-- 5. 통계 출력
-- ============================================

DO $$
DECLARE
    v_company_files INTEGER;
    v_company_images INTEGER;
    v_board_files_updated INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_company_files
    FROM files WHERE entity_type = 'COMPANY';

    SELECT COUNT(*) INTO v_company_images
    FROM company_images WHERE is_deleted = false;

    SELECT COUNT(*) INTO v_board_files_updated
    FROM files WHERE entity_type = 'BOARD' AND entity_id IS NOT NULL;

    RAISE NOTICE 'Migration Complete:';
    RAISE NOTICE '  - Company Files: %', v_company_files;
    RAISE NOTICE '  - Company Images: %', v_company_images;
    RAISE NOTICE '  - Board Files Updated: %', v_board_files_updated;
END $$;
