-- ================================================
-- V79: Specialty 필터 계층 구조 추가
-- ================================================
-- 목적:
-- 1. hospital-interior 옵션 아래에 22개 진료과 자식 옵션 추가
-- 2. 모든 병원인테리어 업체에 22개 진료과 전부 매핑
-- ================================================

-- ============================================
-- 1. hospital-interior 옵션 ID 확인
-- ============================================

DO $$
DECLARE
    v_hospital_interior_id BIGINT;
BEGIN
    SELECT id INTO v_hospital_interior_id
    FROM filter_options
    WHERE category_id = 3
    AND code = 'hospital-interior'
    AND is_deleted = false;

    IF v_hospital_interior_id IS NULL THEN
        RAISE EXCEPTION 'hospital-interior option not found in specialty category';
    END IF;

    RAISE NOTICE 'hospital-interior option ID: %', v_hospital_interior_id;
END $$;

-- ============================================
-- 2. 진료과 자식 옵션 22개 추가 (hospital-interior 하위)
--    코드에 'hi-' prefix 추가하여 중복 방지
-- ============================================

INSERT INTO filter_options (uuid, category_id, parent_id, code, name, short_name, depth, display_order, is_active, created_at, updated_at, is_deleted)
SELECT
    gen_random_uuid(),
    3,
    (SELECT id FROM filter_options WHERE category_id = 3 AND code = 'hospital-interior' AND is_deleted = false),
    v.code,
    v.name,
    v.short_name,
    1,
    v.display_order,
    true,
    NOW(),
    NOW(),
    false
FROM (VALUES
    ('hi-dermatology', '피부과', '피부과', 1),
    ('hi-plastic-surgery', '성형외과', '성형', 2),
    ('hi-orthopedic', '정형외과', '정형', 3),
    ('hi-internal-medicine', '내과', '내과', 4),
    ('hi-dental', '치과', '치과', 5),
    ('hi-ophthalmology', '안과', '안과', 6),
    ('hi-oriental-medicine', '한의원', '한의원', 7),
    ('hi-oriental-hospital', '한방병원', '한방', 8),
    ('hi-obstetrics-gynecology', '산부인과', '산부인과', 9),
    ('hi-urology', '비뇨의학과', '비뇨기', 10),
    ('hi-ent', '이비인후과', '이비인후', 11),
    ('hi-family-medicine', '가정의학과', '가정의학', 12),
    ('hi-rehabilitation-medicine', '재활의학과', '재활', 13),
    ('hi-neurosurgery', '신경외과', '신경외과', 14),
    ('hi-anesthesiology', '마취통증의학과', '마취통증', 15),
    ('hi-psychiatry', '정신건강의학과', '정신건강', 16),
    ('hi-surgery', '외과', '외과', 17),
    ('hi-radiology', '영상의학과', '영상', 18),
    ('hi-pediatrics', '소아청소년과', '소아', 19),
    ('hi-health-checkup-center', '건강검진센터', '검진', 20),
    ('hi-general-hospital', '종합병원', '종합', 21),
    ('hi-all', '전체 진료과', '전체', 99)
) AS v(code, name, short_name, display_order)
WHERE NOT EXISTS (
    SELECT 1 FROM filter_options fo
    WHERE fo.category_id = 3
    AND fo.code = v.code
    AND fo.is_deleted = false
);

-- ============================================
-- 3. 병원인테리어 업체에 hospital-interior 옵션 매핑
-- ============================================

INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT c.id, fo.id, NOW()
FROM companies c
JOIN filter_options fo ON fo.category_id = 3 AND fo.code = 'hospital-interior' AND fo.is_deleted = false
WHERE c.is_deleted = false
AND (
    'interior' = ANY(c.tags) OR
    'hospital_interior' = ANY(c.tags) OR
    '병원인테리어' = ANY(c.tags) OR
    '신규인테리어' = ANY(c.tags)
)
AND NOT EXISTS (
    SELECT 1 FROM company_filter_options cfo
    WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo.id
);

-- ============================================
-- 4. 병원인테리어 업체에 22개 진료과 자식옵션 전부 매핑
-- ============================================

INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT c.id, fo_child.id, NOW()
FROM companies c
-- hospital-interior 옵션
JOIN filter_options fo_hi ON fo_hi.category_id = 3
    AND fo_hi.code = 'hospital-interior'
    AND fo_hi.is_deleted = false
-- 22개 자식 옵션 (CROSS JOIN으로 모든 자식 옵션과 조합)
CROSS JOIN filter_options fo_child
WHERE c.is_deleted = false
-- fo_child는 hospital-interior의 자식이어야 함
AND fo_child.parent_id = fo_hi.id
AND fo_child.is_deleted = false
-- 병원인테리어 업체만 (hospital-interior 옵션이 있는 업체)
AND EXISTS (
    SELECT 1 FROM company_filter_options cfo
    WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo_hi.id
)
-- 중복 방지
AND NOT EXISTS (
    SELECT 1 FROM company_filter_options cfo
    WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo_child.id
);

-- ============================================
-- 5. companies.filter_option_ids (JSONB) 업데이트
-- ============================================

UPDATE companies c SET filter_option_ids = (
    SELECT COALESCE(jsonb_agg(cfo.filter_option_id ORDER BY cfo.filter_option_id), '[]'::jsonb)
    FROM company_filter_options cfo
    WHERE cfo.company_id = c.id
)
WHERE is_deleted = false;

-- ============================================
-- 6. 시퀀스 업데이트
-- ============================================

SELECT setval('filter_options_id_seq', COALESCE((SELECT MAX(id) FROM filter_options), 1), true);
SELECT setval('company_filter_options_id_seq', COALESCE((SELECT MAX(id) FROM company_filter_options), 1), true);

-- ============================================
-- 7. 결과 검증
-- ============================================

DO $$
DECLARE
    v_hospital_interior_id BIGINT;
    v_child_count INTEGER;
    v_hi_companies INTEGER;
    v_total_mappings INTEGER;
    v_expected_mappings INTEGER;
BEGIN
    SELECT id INTO v_hospital_interior_id
    FROM filter_options WHERE category_id = 3 AND code = 'hospital-interior' AND is_deleted = false;

    -- 진료과 자식 옵션 수
    SELECT COUNT(*) INTO v_child_count
    FROM filter_options WHERE parent_id = v_hospital_interior_id AND is_deleted = false;

    -- 병원인테리어 업체 수
    SELECT COUNT(DISTINCT company_id) INTO v_hi_companies
    FROM company_filter_options WHERE filter_option_id = v_hospital_interior_id;

    -- 총 진료과 매핑 수
    SELECT COUNT(*) INTO v_total_mappings
    FROM company_filter_options cfo
    JOIN filter_options fo ON cfo.filter_option_id = fo.id
    WHERE fo.parent_id = v_hospital_interior_id;

    -- 예상 매핑 수 (업체 수 × 자식 옵션 수)
    v_expected_mappings := v_hi_companies * v_child_count;

    RAISE NOTICE '';
    RAISE NOTICE '=== V79 Specialty 계층 구조 결과 ===';
    RAISE NOTICE 'hospital-interior 옵션 ID: %', v_hospital_interior_id;
    RAISE NOTICE '진료과 자식 옵션 수: %', v_child_count;
    RAISE NOTICE '병원인테리어 업체 수: %', v_hi_companies;
    RAISE NOTICE '총 진료과 매핑 수: %', v_total_mappings;
    RAISE NOTICE '예상 매핑 수 (업체×진료과): %', v_expected_mappings;

    IF v_total_mappings = v_expected_mappings THEN
        RAISE NOTICE '모든 병원인테리어 업체에 22개 진료과가 전부 매핑됨';
    ELSE
        RAISE WARNING '매핑 불일치: 예상 % / 실제 %', v_expected_mappings, v_total_mappings;
    END IF;
END $$;
