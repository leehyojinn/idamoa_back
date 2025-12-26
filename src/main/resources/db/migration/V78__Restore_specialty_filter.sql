-- ================================================
-- V78: Specialty 필터 카테고리 복구
-- ================================================
-- 목적:
-- 1. V77에서 soft delete된 specialty 카테고리(ID 3) 복구
-- 2. specialty 옵션들 복구
-- 3. V77/V78에서 생성된 hospital-interior 등 새 카테고리 soft delete
-- 4. company_filter_options를 specialty 옵션으로 재매핑
-- 5. companies.filter_option_ids (JSONB) 업데이트
-- ================================================

-- ============================================
-- 1. Specialty 카테고리 복구 (ID 3)
-- ============================================

UPDATE filter_categories
SET is_deleted = false, deleted_at = NULL, updated_at = NOW()
WHERE id = 3 AND code = 'specialty';

-- 혹시 code가 변경되었을 경우 복구
UPDATE filter_categories
SET code = 'specialty', is_deleted = false, deleted_at = NULL, updated_at = NOW()
WHERE id = 3;

-- ============================================
-- 2. Specialty 옵션들 복구
-- ============================================

UPDATE filter_options
SET is_deleted = false, deleted_at = NULL, updated_at = NOW()
WHERE category_id = 3;

-- ============================================
-- 3. V77에서 만든 새 카테고리들 soft delete
-- (hospital-interior, aircon, signage 등 COMPANY 타입 중 ID > 3)
-- ============================================

UPDATE filter_categories
SET is_deleted = true, deleted_at = NOW(), updated_at = NOW()
WHERE entity_type = 'COMPANY'
AND code IN ('hospital-interior', 'aircon', 'signage', 'marketing', 'network',
             'website', 'medical-equipment', 'cleaning', 'bedding', 'card-checker',
             'uniform', 'security', 'furniture')
AND is_deleted = false;

-- 해당 옵션들도 soft delete
UPDATE filter_options
SET is_deleted = true, deleted_at = NOW(), updated_at = NOW()
WHERE category_id IN (
    SELECT id FROM filter_categories
    WHERE code IN ('hospital-interior', 'aircon', 'signage', 'marketing', 'network',
                   'website', 'medical-equipment', 'cleaning', 'bedding', 'card-checker',
                   'uniform', 'security', 'furniture')
)
AND is_deleted = false;

-- ============================================
-- 4. company_filter_options 정리 및 재매핑
-- ============================================

-- 4-1. 삭제된 카테고리 옵션에 연결된 company_filter_options 삭제
DELETE FROM company_filter_options
WHERE filter_option_id IN (
    SELECT fo.id FROM filter_options fo
    WHERE fo.is_deleted = true
);

-- 4-2. 태그 → specialty 옵션 매핑 테이블
CREATE TEMPORARY TABLE tag_specialty_mapping (
    tag_value TEXT,
    specialty_code TEXT
);

INSERT INTO tag_specialty_mapping VALUES
-- 전문영역 태그
('interior', 'hospital-interior'),
('hospital_interior', 'hospital-interior'),
('병원인테리어', 'hospital-interior'),
('신규인테리어', 'hospital-interior'),

-- 기타 전문영역
('aircon', 'aircon'),
('signage', 'signage'),
('marketing', 'marketing'),
('network', 'network'),
('communication', 'internet'),
('internet', 'internet'),
('website', 'homepage'),
('homepage', 'homepage'),
('medical_equipment', 'medical-equipment'),
('medical-equipment', 'medical-equipment'),
('cleaning', 'cleaning'),
('regular_cleaning', 'cleaning'),
('bedding', 'bedding'),
('card_checker', 'card-checker'),
('card-checker', 'card-checker'),
('uniform', 'uniform'),
('security', 'security'),
('purpose_change', 'purpose-change'),
('purpose-change', 'purpose-change'),

-- 아파트/카페/미용실 인테리어
('apartment_interior', 'apartment-interior'),
('apartment-interior', 'apartment-interior'),
('cafe_interior', 'cafe-interior'),
('cafe-interior', 'cafe-interior'),
('salon_interior', 'salon-interior'),
('salon-interior', 'salon-interior');

-- 4-3. 태그 기반으로 company_filter_options 매핑
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT c.id, fo.id, NOW()
FROM companies c
CROSS JOIN LATERAL unnest(c.tags) AS t(tag_val)
JOIN tag_specialty_mapping m ON m.tag_value = t.tag_val
JOIN filter_options fo ON fo.category_id = 3 AND fo.code = m.specialty_code AND fo.is_deleted = false
WHERE c.is_deleted = false
AND NOT EXISTS (
    SELECT 1 FROM company_filter_options cfo
    WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo.id
);

-- 4-4. interior 태그가 있지만 specialty 매핑이 없는 회사에 hospital-interior 추가
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT c.id, fo.id, NOW()
FROM companies c
JOIN filter_options fo ON fo.category_id = 3 AND fo.code = 'hospital-interior' AND fo.is_deleted = false
WHERE c.is_deleted = false
AND ('interior' = ANY(c.tags) OR 'hospital_interior' = ANY(c.tags))
AND NOT EXISTS (
    SELECT 1 FROM company_filter_options cfo
    JOIN filter_options fo2 ON cfo.filter_option_id = fo2.id
    WHERE cfo.company_id = c.id AND fo2.category_id = 3
);

-- 임시 테이블 정리
DROP TABLE IF EXISTS tag_specialty_mapping;

-- ============================================
-- 5. companies.filter_option_ids (JSONB) 업데이트
-- ============================================

-- 기존 데이터 클리어 후 재생성
UPDATE companies c SET filter_option_ids = (
    SELECT COALESCE(jsonb_agg(cfo.filter_option_id ORDER BY cfo.filter_option_id), '[]'::jsonb)
    FROM company_filter_options cfo
    WHERE cfo.company_id = c.id
)
WHERE is_deleted = false;

-- filter_option_ids가 NULL인 경우 빈 배열로
UPDATE companies
SET filter_option_ids = '[]'::jsonb
WHERE filter_option_ids IS NULL;

-- ============================================
-- 6. 시퀀스 업데이트
-- ============================================

SELECT setval('company_filter_options_id_seq', COALESCE((SELECT MAX(id) FROM company_filter_options), 1), true);

-- ============================================
-- 7. 결과 검증
-- ============================================

DO $$
DECLARE
    v_specialty_active BOOLEAN;
    v_specialty_options INTEGER;
    v_mapped_companies INTEGER;
    v_total_mappings INTEGER;
BEGIN
    -- specialty 카테고리 활성 상태 확인
    SELECT NOT is_deleted INTO v_specialty_active
    FROM filter_categories WHERE id = 3;

    -- specialty 옵션 수 확인
    SELECT COUNT(*) INTO v_specialty_options
    FROM filter_options WHERE category_id = 3 AND is_deleted = false;

    -- 매핑된 회사 수
    SELECT COUNT(DISTINCT company_id) INTO v_mapped_companies
    FROM company_filter_options cfo
    JOIN filter_options fo ON cfo.filter_option_id = fo.id
    WHERE fo.category_id = 3;

    -- 총 매핑 수
    SELECT COUNT(*) INTO v_total_mappings
    FROM company_filter_options cfo
    JOIN filter_options fo ON cfo.filter_option_id = fo.id
    WHERE fo.category_id = 3;

    RAISE NOTICE '';
    RAISE NOTICE '=== V78 Specialty 복구 결과 ===';
    RAISE NOTICE 'specialty 카테고리 활성: % (true여야 함)', v_specialty_active;
    RAISE NOTICE 'specialty 옵션 수: % (17이어야 함)', v_specialty_options;
    RAISE NOTICE '매핑된 회사 수: %', v_mapped_companies;
    RAISE NOTICE '총 매핑 수: %', v_total_mappings;
END $$;

-- 카테고리별 매핑 통계
DO $$
DECLARE
    rec RECORD;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE 'Specialty 옵션별 매핑 현황:';
    FOR rec IN
        SELECT fo.name, COUNT(cfo.id) as count
        FROM filter_options fo
        LEFT JOIN company_filter_options cfo ON cfo.filter_option_id = fo.id
        WHERE fo.category_id = 3 AND fo.is_deleted = false
        GROUP BY fo.id, fo.name, fo.display_order
        ORDER BY fo.display_order
    LOOP
        RAISE NOTICE '  - %: %', rec.name, rec.count;
    END LOOP;
END $$;
