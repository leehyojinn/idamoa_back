--
-- V65: business_type 카테고리 삭제 및 specialty 복구
--
-- 목적:
--   1. specialty 카테고리 복구
--   2. specialty 옵션 17개 생성
--   3. company_filter_options를 business_type → specialty로 이전
--   4. business_type 카테고리 삭제
--

-- ============================================================
-- 1. specialty 카테고리 복구/생성
-- ============================================================

INSERT INTO filter_categories (id, uuid, code, name, description, entity_type, filter_type, is_required, display_order, icon, is_active, is_deleted, metadata, created_at, updated_at)
VALUES (
    3,
    '2c624f8f-c999-458a-9ef1-dc0144e79296',
    'specialty',
    '전문영역',
    '인테리어 전문 분야',
    'COMPANY',
    'MULTI_SELECT',
    false,
    3,
    NULL,
    true,
    false,
    '{}',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    code = 'specialty',
    name = '전문영역',
    description = '인테리어 전문 분야',
    entity_type = 'COMPANY',
    filter_type = 'MULTI_SELECT',
    is_active = true,
    is_deleted = false,
    updated_at = NOW();

-- ============================================================
-- 2. specialty 옵션 17개 생성 (없는 경우에만)
-- ============================================================

INSERT INTO filter_options (uuid, category_id, code, name, short_name, display_order, is_active, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), 3, v.code, v.name, v.short_name, v.ord, true, NOW(), NOW(), false
FROM (VALUES
    ('hospital-interior', '병원 인테리어', '병원', 10),
    ('apartment-interior', '아파트인테리어', '아파트', 20),
    ('cafe-interior', '카페인테리어', '카페', 30),
    ('salon-interior', '미용실인테리어', '미용실', 40),
    ('marketing', '마케팅/광고', '마케팅', 50),
    ('cleaning', '청소/방역', '청소', 60),
    ('internet', '인터넷/통신', '통신', 70),
    ('medical-equipment', '의료 장비 설치', '의료장비', 80),
    ('bedding', '침구', '침구', 90),
    ('card-checker', '카드체크기', '카드기', 100),
    ('signage', '간판', '간판', 110),
    ('uniform', '유니폼', '유니폼', 120),
    ('network', '네트워크', '네트워크', 130),
    ('purpose-change', '용도변경', '용도변경', 140),
    ('homepage', '홈페이지', '홈페이지', 150),
    ('aircon', '에어컨', '에어컨', 160),
    ('security', '보안', '보안', 170)
) AS v(code, name, short_name, ord)
WHERE NOT EXISTS (
    SELECT 1 FROM filter_options fo
    WHERE fo.category_id = 3 AND fo.code = v.code
);

-- ============================================================
-- 3. company_filter_options 이전: business_type → specialty
-- ============================================================

-- 매핑 테이블 생성
CREATE TEMP TABLE option_mapping AS
SELECT
    bt.id AS old_option_id,
    sp.id AS new_option_id,
    bt.code
FROM filter_options bt
JOIN filter_options sp ON sp.code = bt.code AND sp.category_id = 3
JOIN filter_categories fc ON fc.id = bt.category_id AND fc.code = 'business_type'
WHERE bt.code IN (
    'marketing', 'homepage', 'aircon', 'signage', 'internet', 'security',
    'network', 'purpose-change', 'bedding', 'cleaning', 'uniform',
    'card-checker', 'medical-equipment', 'hospital-interior',
    'apartment-interior', 'cafe-interior', 'salon-interior'
);

-- 이전할 데이터 저장
CREATE TEMP TABLE cfo_to_migrate AS
SELECT DISTINCT cfo.company_id, om.new_option_id
FROM company_filter_options cfo
JOIN option_mapping om ON om.old_option_id = cfo.filter_option_id
WHERE NOT EXISTS (
    SELECT 1 FROM company_filter_options existing
    WHERE existing.company_id = cfo.company_id
    AND existing.filter_option_id = om.new_option_id
);

-- 새 연결 추가
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT company_id, new_option_id, NOW()
FROM cfo_to_migrate;

-- ============================================================
-- 4. business_type 관련 데이터 삭제
-- ============================================================

-- business_type 옵션에 연결된 company_filter_options 삭제
DELETE FROM company_filter_options
WHERE filter_option_id IN (
    SELECT fo.id FROM filter_options fo
    JOIN filter_categories fc ON fc.id = fo.category_id
    WHERE fc.code = 'business_type'
);

-- board_filter_options에서도 삭제 (있는 경우)
DELETE FROM board_filter_options
WHERE filter_option_id IN (
    SELECT fo.id FROM filter_options fo
    JOIN filter_categories fc ON fc.id = fo.category_id
    WHERE fc.code = 'business_type'
);

-- business_type 옵션 삭제
DELETE FROM filter_options
WHERE category_id IN (
    SELECT id FROM filter_categories WHERE code = 'business_type'
);

-- business_type 카테고리 삭제
DELETE FROM filter_categories WHERE code = 'business_type';

-- 임시 테이블 정리
DROP TABLE IF EXISTS option_mapping;
DROP TABLE IF EXISTS cfo_to_migrate;

-- ============================================================
-- 5. 검증
-- ============================================================

DO $$
DECLARE
    v_specialty_count INTEGER;
    v_business_type_exists BOOLEAN;
    v_cfo_count INTEGER;
BEGIN
    SELECT EXISTS(SELECT 1 FROM filter_categories WHERE code = 'business_type')
    INTO v_business_type_exists;

    SELECT COUNT(*) INTO v_specialty_count
    FROM filter_options WHERE category_id = 3 AND is_active = true AND is_deleted = false;

    SELECT COUNT(*) INTO v_cfo_count
    FROM company_filter_options cfo
    JOIN filter_options fo ON fo.id = cfo.filter_option_id
    WHERE fo.category_id = 3;

    RAISE NOTICE '';
    RAISE NOTICE '=== V65 마이그레이션 결과 ===';
    RAISE NOTICE 'business_type 존재: % (false여야 함)', v_business_type_exists;
    RAISE NOTICE 'specialty 옵션 수: % (17이어야 함)', v_specialty_count;
    RAISE NOTICE 'specialty에 연결된 company 수: %', v_cfo_count;
END $$;
