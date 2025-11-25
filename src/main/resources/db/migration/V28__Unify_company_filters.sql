-- ================================================
-- V28: 업체 서비스 지역 필터로 통합
-- ================================================
-- 목적:
-- 1. service_areas 배열을 company_filter_options로 통합
-- 2. tags는 text[] 배열로 유지 (자유로운 태그 입력 위해)
-- 3. 데이터 일관성 확보 및 검색 성능 개선
-- ================================================

-- 1. 필터 카테고리에 REGION 추가 (없으면)
INSERT INTO filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, created_at, updated_at)
SELECT gen_random_uuid(), 'REGION', '지역', '서비스 제공 지역', 'COMPANY', 'MULTI_SELECT', false, 1, 10, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM filter_categories WHERE code = 'REGION');

-- 2. 주요 지역을 filter_options에 추가
WITH regions AS (
    SELECT DISTINCT unnest(service_areas) AS region_name
    FROM companies
    WHERE service_areas IS NOT NULL
),
category AS (
    SELECT id FROM filter_categories WHERE code = 'REGION' LIMIT 1
)
INSERT INTO filter_options (uuid, category_id, code, name, description, is_active, display_order, created_at, updated_at)
SELECT
    gen_random_uuid(),
    category.id,
    UPPER(REPLACE(REPLACE(region_name, ' ', '_'), '-', '_')),
    region_name,
    region_name || ' 지역',
    true,
    ROW_NUMBER() OVER (ORDER BY region_name) * 10,
    NOW(),
    NOW()
FROM regions, category
WHERE region_name IS NOT NULL
  AND region_name != ''
  AND NOT EXISTS (
    SELECT 1 FROM filter_options fo
    WHERE fo.category_id = category.id
    AND fo.name = region_name
  );

-- 3. 기존 service_areas 데이터를 company_filter_options로 마이그레이션
WITH region_mappings AS (
    SELECT
        c.id AS company_id,
        fo.id AS filter_option_id
    FROM companies c
    CROSS JOIN LATERAL unnest(c.service_areas) AS area_name
    JOIN filter_options fo ON fo.name = area_name
    JOIN filter_categories fc ON fc.id = fo.category_id AND fc.code = 'REGION'
    WHERE c.service_areas IS NOT NULL
)
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT
    company_id,
    filter_option_id,
    NOW()
FROM region_mappings
WHERE NOT EXISTS (
    SELECT 1 FROM company_filter_options cfo
    WHERE cfo.company_id = region_mappings.company_id
    AND cfo.filter_option_id = region_mappings.filter_option_id
);

-- 4. 마이그레이션 검증 (선택적)
DO $$
DECLARE
    v_company_count INTEGER;
    v_migrated_regions INTEGER;
BEGIN
    -- 지역 데이터 검증
    SELECT COUNT(DISTINCT c.id) INTO v_company_count
    FROM companies c
    WHERE c.service_areas IS NOT NULL AND array_length(c.service_areas, 1) > 0;

    SELECT COUNT(DISTINCT cfo.company_id) INTO v_migrated_regions
    FROM company_filter_options cfo
    JOIN filter_options fo ON fo.id = cfo.filter_option_id
    JOIN filter_categories fc ON fc.id = fo.category_id
    WHERE fc.code = 'REGION';

    RAISE NOTICE 'Migration completed. Companies with regions: %, Migrated: %', v_company_count, v_migrated_regions;
END $$;

-- 5. 인덱스 추가 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_filter_categories_code ON filter_categories(code);
CREATE INDEX IF NOT EXISTS idx_filter_options_category_name ON filter_options(category_id, name);

-- 6. keywords 컬럼 제거 (통합 검색으로 대체)
ALTER TABLE companies DROP COLUMN IF EXISTS keywords;

-- 7. 주의: service_areas 컬럼 삭제는 데이터 마이그레이션 확인 후 실행
-- ALTER TABLE companies DROP COLUMN service_areas;

COMMENT ON COLUMN companies.service_areas IS 'DEPRECATED: Use company_filter_options with REGION category';
-- tags는 text[]로 유지 (사용자 자유 입력용)