--
-- V47: 영어 태그를 필터 옵션에 매핑
--

-- ============================================
-- 1. 누락된 필터 옵션 추가 (specialty 카테고리)
-- ============================================

-- specialty 카테고리 ID 조회
DO $$
DECLARE
    v_specialty_category_id INTEGER;
    v_department_category_id INTEGER;
    v_max_order INTEGER;
BEGIN
    SELECT id INTO v_specialty_category_id FROM filter_categories WHERE code = 'specialty';
    SELECT id INTO v_department_category_id FROM filter_categories WHERE code = 'department';
    SELECT COALESCE(MAX(display_order), 0) INTO v_max_order FROM filter_options WHERE category_id = v_specialty_category_id;

    -- 누락된 전문영역 필터 추가
    INSERT INTO filter_options (uuid, category_id, code, name, description, display_order, is_active, created_at, updated_at)
    SELECT gen_random_uuid(), v_specialty_category_id, code, name, description, v_max_order + row_num, true, NOW(), NOW()
    FROM (
        VALUES
            ('bedding', '침구', '침구 판매/설치', 1),
            ('card-checker', '카드체크기', '카드체크기 설치/판매', 2),
            ('signage', '간판', '간판 제작/설치', 3),
            ('uniform', '유니폼', '유니폼 제작/판매', 4),
            ('communication', '통신', '통신/네트워크 설치', 5),
            ('network', '네트워크', '네트워크 구축', 6),
            ('purpose-change', '용도변경', '용도변경 인허가', 7),
            ('dental-interior', '치과인테리어', '치과 전문 인테리어', 8),
            ('skin-interior', '피부과인테리어', '피부과 전문 인테리어', 9),
            ('general-interior', '인테리어', '일반 인테리어', 10),
            ('new-interior', '신규인테리어', '신규 인테리어 시공', 11)
    ) AS t(code, name, description, row_num)
    WHERE NOT EXISTS (
        SELECT 1 FROM filter_options fo
        WHERE fo.category_id = v_specialty_category_id AND fo.code = t.code
    );

    RAISE NOTICE 'Added missing specialty filter options';
END $$;

-- ============================================
-- 2. 영어 태그 → 필터 옵션 매핑 테이블
-- ============================================

CREATE TEMP TABLE english_tag_mapping (
    tag_value VARCHAR(100),
    filter_category_code VARCHAR(50),
    filter_option_code VARCHAR(50)
);

-- 진료과 태그 매핑
INSERT INTO english_tag_mapping VALUES
    ('dental', 'department', 'dentistry'),
    ('dermatology', 'department', 'dermatology'),
    ('plastic_surgery', 'department', 'plastic_surgery'),
    ('orthopedic', 'department', 'orthopedic'),
    ('ophthalmology', 'department', 'ophthalmology'),
    ('oriental_medicine', 'department', 'oriental_medicine'),
    ('oriental_hospital', 'department', 'oriental_hospital'),
    ('ent', 'department', 'ent'),
    ('obstetrics_gynecology', 'department', 'obstetrics_gynecology'),
    ('neurosurgery', 'department', 'neurosurgery'),
    ('psychiatry', 'department', 'psychiatry'),
    ('health_checkup_center', 'department', 'health_checkup_center'),
    ('general_hospital', 'department', 'general_hospital'),
    ('pediatrics', 'department', 'pediatrics'),
    ('internal_medicine', 'department', 'internal_medicine'),
    ('urology', 'department', 'urology'),
    ('family_medicine', 'department', 'family-medicine'),
    ('rehabilitation_medicine', 'department', 'rehabilitation'),
    ('radiology', 'department', 'radiology'),
    ('anesthesiology', 'department', 'anesthesiology'),
    ('surgery', 'department', 'any-department');

-- 전문영역 태그 매핑
INSERT INTO english_tag_mapping VALUES
    ('interior', 'specialty', 'general-interior'),
    ('marketing', 'specialty', 'marketing'),
    ('website', 'specialty', 'web-dev'),
    ('medical_equipment', 'specialty', 'medical-equipment'),
    ('security', 'specialty', 'locksmith'),
    ('regular_cleaning', 'specialty', 'cleaning'),
    ('aircon', 'specialty', 'air-conditioner'),
    ('bedding', 'specialty', 'bedding'),
    ('card_checker', 'specialty', 'card-checker'),
    ('signage', 'specialty', 'signage'),
    ('uniform', 'specialty', 'uniform'),
    ('communication', 'specialty', 'communication'),
    ('network', 'specialty', 'network'),
    ('purpose_change', 'specialty', 'purpose-change');

-- 한글 태그 매핑 (추가)
INSERT INTO english_tag_mapping VALUES
    ('치과인테리어', 'specialty', 'dental-interior'),
    ('피부과인테리어', 'specialty', 'skin-interior'),
    ('병원인테리어', 'specialty', 'hospital-interior'),
    ('인테리어', 'specialty', 'general-interior'),
    ('신규인테리어', 'specialty', 'new-interior'),
    ('리모델링', 'specialty', 'remodeling'),
    ('상업공간', 'specialty', 'commercial');

-- ============================================
-- 3. company_filter_options에 매핑 삽입
-- ============================================

INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    c.id,
    fo.id,
    NOW()
FROM companies c
CROSS JOIN LATERAL unnest(c.tags) AS t(tag_val)
JOIN english_tag_mapping m ON m.tag_value = t.tag_val
JOIN filter_categories fc ON fc.code = m.filter_category_code
JOIN filter_options fo ON fo.category_id = fc.id AND fo.code = m.filter_option_code
WHERE c.tags IS NOT NULL
  AND array_length(c.tags, 1) > 0
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo
      WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo.id
  );

-- ============================================
-- 4. 통계 출력
-- ============================================

DO $$
DECLARE
    v_new_options INTEGER;
    v_new_mappings INTEGER;
    v_companies_with_filters INTEGER;
    v_companies_without_filters INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_new_options
    FROM filter_options fo
    JOIN filter_categories fc ON fo.category_id = fc.id
    WHERE fc.code = 'specialty'
    AND fo.code IN ('bedding', 'card-checker', 'signage', 'uniform', 'communication', 'network', 'purpose-change', 'dental-interior', 'skin-interior', 'general-interior', 'new-interior');

    SELECT COUNT(DISTINCT company_id) INTO v_companies_with_filters
    FROM company_filter_options;

    SELECT COUNT(*) INTO v_companies_without_filters
    FROM companies c
    WHERE NOT EXISTS (
        SELECT 1 FROM company_filter_options cfo WHERE cfo.company_id = c.id
    );

    RAISE NOTICE '=== V47 Migration Complete ===';
    RAISE NOTICE 'New specialty options added: %', v_new_options;
    RAISE NOTICE 'Companies with filters: %', v_companies_with_filters;
    RAISE NOTICE 'Companies without filters: %', v_companies_without_filters;
END $$;

-- 임시 테이블 삭제
DROP TABLE IF EXISTS english_tag_mapping;
