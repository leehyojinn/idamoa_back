--
-- V54: 회사 지역 필터 옵션 재매핑
-- V46에서 일부 지역만 매핑되어서 누락된 회사-지역 연결 복구
--

-- ============================================
-- 1. 지역 태그 매핑 테이블 (영어 태그 → 한글 필터명)
-- ============================================

CREATE TEMP TABLE region_tag_mapping (
    tag_value VARCHAR(50),
    option_name VARCHAR(50)
);

-- 모든 17개 지역 매핑 (태그값 → 필터 옵션 name)
INSERT INTO region_tag_mapping VALUES
    ('seoul', '서울'),
    ('gyeonggi', '경기'),
    ('incheon', '인천'),
    ('busan', '부산'),
    ('daegu', '대구'),
    ('gwangju', '광주'),
    ('daejeon', '대전'),
    ('ulsan', '울산'),
    ('sejong', '세종'),
    ('gangwon', '강원'),
    ('chungbuk', '충북'),
    ('chungnam', '충남'),
    ('jeonbuk', '전북'),
    ('jeonnam', '전남'),
    ('gyeongbuk', '경북'),
    ('gyeongnam', '경남'),
    ('jeju', '제주');

-- ============================================
-- 2. 기존 매핑 상태 확인
-- ============================================

DO $$
DECLARE
    v_existing_mappings INTEGER;
    v_companies_with_region_tags INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_existing_mappings
    FROM company_filter_options cfo
    JOIN filter_options fo ON cfo.filter_option_id = fo.id
    JOIN filter_categories fc ON fo.category_id = fc.id
    WHERE fc.code = 'region';

    SELECT COUNT(DISTINCT c.id) INTO v_companies_with_region_tags
    FROM companies c
    CROSS JOIN LATERAL unnest(c.tags) AS t(tag_val)
    WHERE t.tag_val IN ('seoul', 'gyeonggi', 'incheon', 'busan', 'daegu', 'gwangju', 'daejeon', 'ulsan',
                        'sejong', 'gangwon', 'chungbuk', 'chungnam', 'jeonbuk', 'jeonnam', 'gyeongbuk', 'gyeongnam', 'jeju')
    AND c.is_deleted = false;

    RAISE NOTICE '=== Before Migration ===';
    RAISE NOTICE 'Existing region mappings: %', v_existing_mappings;
    RAISE NOTICE 'Companies with region tags: %', v_companies_with_region_tags;
END $$;

-- ============================================
-- 3. 회사-지역 필터 매핑 삽입 (name으로 조인)
-- ============================================

INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    c.id,
    fo.id,
    NOW()
FROM companies c
CROSS JOIN LATERAL unnest(c.tags) AS t(tag_val)
JOIN region_tag_mapping m ON m.tag_value = t.tag_val
JOIN filter_categories fc ON fc.code = 'region' AND fc.is_deleted = false
JOIN filter_options fo ON fo.category_id = fc.id AND fo.name = m.option_name AND fo.is_deleted = false
WHERE c.tags IS NOT NULL
  AND array_length(c.tags, 1) > 0
  AND c.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo
      WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo.id
  );

-- ============================================
-- 4. 시퀀스 업데이트
-- ============================================

SELECT setval('company_filter_options_id_seq', COALESCE((SELECT MAX(id) FROM company_filter_options), 1), true);

-- ============================================
-- 5. 결과 확인
-- ============================================

DO $$
DECLARE
    v_total_mappings INTEGER;
    v_region_mappings INTEGER;
    v_companies_mapped INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total_mappings FROM company_filter_options;

    SELECT COUNT(*) INTO v_region_mappings
    FROM company_filter_options cfo
    JOIN filter_options fo ON cfo.filter_option_id = fo.id
    JOIN filter_categories fc ON fo.category_id = fc.id
    WHERE fc.code = 'region';

    SELECT COUNT(DISTINCT cfo.company_id) INTO v_companies_mapped
    FROM company_filter_options cfo
    JOIN filter_options fo ON cfo.filter_option_id = fo.id
    JOIN filter_categories fc ON fo.category_id = fc.id
    WHERE fc.code = 'region';

    RAISE NOTICE '=== After Migration ===';
    RAISE NOTICE 'Total company-filter mappings: %', v_total_mappings;
    RAISE NOTICE 'Region filter mappings: %', v_region_mappings;
    RAISE NOTICE 'Companies with region filters: %', v_companies_mapped;
END $$;

-- 임시 테이블 삭제
DROP TABLE IF EXISTS region_tag_mapping;
