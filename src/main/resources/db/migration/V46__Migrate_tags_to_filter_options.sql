--
-- V46: Tags를 Filter Options으로 마이그레이션
--

-- ============================================
-- 1. Region에 누락된 주요 지역 추가
-- ============================================

INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), fc.id, v.code, v.name, v.display_order, true, NOW(), NOW(), false
FROM filter_categories fc
CROSS JOIN (VALUES
    ('seoul', '서울', 1),
    ('gyeonggi', '경기', 2),
    ('incheon', '인천', 3),
    ('busan', '부산', 4),
    ('daegu', '대구', 5),
    ('gwangju', '광주', 6),
    ('daejeon', '대전', 7),
    ('ulsan', '울산', 8)
) AS v(code, name, display_order)
WHERE fc.code = 'region' AND fc.is_deleted = false
AND NOT EXISTS (SELECT 1 FROM filter_options fo WHERE fo.category_id = fc.id AND fo.code = v.code);

-- ============================================
-- 2. Department에 누락된 진료과 추가
-- ============================================

INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), fc.id, v.code, v.name, v.display_order, true, NOW(), NOW(), false
FROM filter_categories fc
CROSS JOIN (VALUES
    ('dermatology', '피부과', 1),
    ('plastic_surgery', '성형외과', 2),
    ('orthopedic', '정형외과', 3),
    ('internal_medicine', '내과', 4),
    ('ophthalmology', '안과', 5),
    ('oriental_medicine', '한의원', 6),
    ('oriental_hospital', '한방병원', 7),
    ('obstetrics_gynecology', '산부인과', 8),
    ('neurosurgery', '신경외과', 9),
    ('health_checkup_center', '건강검진센터', 10),
    ('general_hospital', '종합병원', 11)
) AS v(code, name, display_order)
WHERE fc.code = 'department' AND fc.is_deleted = false
AND NOT EXISTS (SELECT 1 FROM filter_options fo WHERE fo.category_id = fc.id AND fo.code = v.code);

-- ============================================
-- 3. Board 색상에 누락된 옵션 추가
-- ============================================

INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), fc.id, v.code, v.name, v.display_order, true, NOW(), NOW(), false
FROM filter_categories fc
CROSS JOIN (VALUES
    ('red', '레드', 10),
    ('orange', '오렌지', 11),
    ('yellow', '옐로우', 12),
    ('colorful', '컬러풀', 13)
) AS v(code, name, display_order)
WHERE fc.code = 'board_color' AND fc.is_deleted = false
AND NOT EXISTS (SELECT 1 FROM filter_options fo WHERE fo.category_id = fc.id AND fo.code = v.code);

-- ============================================
-- 4. Board 재료에 누락된 옵션 추가
-- ============================================

INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), fc.id, v.code, v.name, v.display_order, true, NOW(), NOW(), false
FROM filter_categories fc
CROSS JOIN (VALUES
    ('exposed', '노출콘크리트', 10),
    ('epoxy', '에폭시', 11),
    ('brick', '벽돌', 12),
    ('pebble', '콩자갈', 13),
    ('cement', '시멘트', 14),
    ('mosaic', '모자이크타일', 15),
    ('barrisol', '바리솔', 16)
) AS v(code, name, display_order)
WHERE fc.code = 'board_material' AND fc.is_deleted = false
AND NOT EXISTS (SELECT 1 FROM filter_options fo WHERE fo.category_id = fc.id AND fo.code = v.code);

-- ============================================
-- 5. Board 공간에 누락된 옵션 추가
-- ============================================

INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), fc.id, v.code, v.name, v.display_order, true, NOW(), NOW(), false
FROM filter_categories fc
CROSS JOIN (VALUES
    ('waiting', '대기실', 1),
    ('counter', '카운터', 5),
    ('skincare', '피부관리실', 11),
    ('exterior', '외부', 12),
    ('surgery', '수술실', 13),
    ('procedure', '시술실', 14),
    ('counseling', '상담실', 15)
) AS v(code, name, display_order)
WHERE fc.code = 'board_space_type' AND fc.is_deleted = false
AND NOT EXISTS (SELECT 1 FROM filter_options fo WHERE fo.category_id = fc.id AND fo.code = v.code);

-- ============================================
-- 6. Board 평수에 누락된 옵션 추가
-- ============================================

INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), fc.id, v.code, v.name, v.display_order, true, NOW(), NOW(), false
FROM filter_categories fc
CROSS JOIN (VALUES
    ('200_under', '200평 이하', 7),
    ('200_over', '200평 이상', 8)
) AS v(code, name, display_order)
WHERE fc.code = 'board_area' AND fc.is_deleted = false
AND NOT EXISTS (SELECT 1 FROM filter_options fo WHERE fo.category_id = fc.id AND fo.code = v.code);

-- ============================================
-- 7. Company 태그 매핑 테이블
-- ============================================

CREATE TEMPORARY TABLE company_tag_mapping (
    tag_value TEXT,
    option_code TEXT,
    category_code TEXT
);

-- 지역 매핑
INSERT INTO company_tag_mapping VALUES
('seoul', 'seoul', 'region'),
('gyeonggi', 'gyeonggi', 'region'),
('incheon', 'incheon', 'region'),
('busan', 'busan', 'region'),
('daegu', 'daegu', 'region'),
('gwangju', 'gwangju', 'region'),
('daejeon', 'daejeon', 'region'),
('ulsan', 'ulsan', 'region'),
('sejong', 'sejong', 'region'),
('gangwon', 'gangwon', 'region'),
('chungbuk', 'chungbuk', 'region'),
('chungnam', 'chungnam', 'region'),
('jeonbuk', 'jeonbuk', 'region'),
('jeonnam', 'jeonnam', 'region'),
('gyeongbuk', 'gyeongbuk', 'region'),
('gyeongnam', 'gyeongnam', 'region'),
('jeju', 'jeju', 'region');

-- 진료과 매핑
INSERT INTO company_tag_mapping VALUES
('dermatology', 'dermatology', 'department'),
('plastic_surgery', 'plastic_surgery', 'department'),
('dental', 'dentistry', 'department'),
('orthopedic', 'orthopedic', 'department'),
('internal_medicine', 'internal_medicine', 'department'),
('ophthalmology', 'ophthalmology', 'department'),
('ent', 'ent', 'department'),
('oriental_medicine', 'oriental_medicine', 'department'),
('oriental_hospital', 'oriental_hospital', 'department'),
('obstetrics_gynecology', 'obstetrics_gynecology', 'department'),
('urology', 'urology', 'department'),
('family_medicine', 'family-medicine', 'department'),
('rehabilitation_medicine', 'rehabilitation', 'department'),
('neurosurgery', 'neurosurgery', 'department'),
('anesthesiology', 'anesthesiology', 'department'),
('psychiatry', 'psychiatry', 'department'),
('surgery', 'surgery', 'department'),
('radiology', 'radiology', 'department'),
('pediatrics', 'pediatrics', 'department'),
('health_checkup_center', 'health_checkup_center', 'department'),
('general_hospital', 'general_hospital', 'department');

-- 전문영역 매핑 (specialty)
INSERT INTO company_tag_mapping VALUES
('interior', 'hospital-interior', 'specialty'),
('signage', 'interior-construction', 'specialty'),
('aircon', 'air-conditioner', 'specialty'),
('network', 'internet', 'specialty'),
('communication', 'internet', 'specialty'),
('medical_equipment', 'medical-equipment', 'specialty'),
('marketing', 'marketing', 'specialty'),
('website', 'web-development', 'specialty'),
('regular_cleaning', 'cleaning', 'specialty'),
('security', 'locksmith', 'specialty');

-- ============================================
-- 8. Company Filter Options 매핑
-- ============================================

INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT c.id, fo.id, NOW()
FROM companies c
CROSS JOIN LATERAL unnest(c.tags) AS t(tag_val)
JOIN company_tag_mapping m ON m.tag_value = t.tag_val
JOIN filter_categories fc ON fc.code = m.category_code AND fc.is_deleted = false
JOIN filter_options fo ON fo.category_id = fc.id AND fo.code = m.option_code AND fo.is_deleted = false
WHERE c.tags IS NOT NULL AND c.is_deleted = false
AND NOT EXISTS (
    SELECT 1 FROM company_filter_options cfo
    WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo.id
);

-- ============================================
-- 9. Board 태그 매핑 테이블 (한글)
-- ============================================

CREATE TEMPORARY TABLE board_tag_mapping (
    tag_value TEXT,
    option_name TEXT,
    category_code TEXT
);

-- 스타일
INSERT INTO board_tag_mapping VALUES
('모던', '모던', 'board_style'),
('클래식', '클래식', 'board_style'),
('럭셔리', '럭셔리', 'board_style'),
('미니멀', '미니멀', 'board_style'),
('내츄럴', '내츄럴', 'board_style'),
('오리엔탈', '한국적', 'board_style');

-- 색상
INSERT INTO board_tag_mapping VALUES
('그레이', '그레이', 'board_color'),
('화이트', '화이트', 'board_color'),
('베이지', '베이지', 'board_color'),
('블랙', '블랙', 'board_color'),
('브라운', '브라운', 'board_color'),
('레드', '레드', 'board_color'),
('그린', '그린', 'board_color'),
('블루', '블루', 'board_color'),
('엘로우', '옐로우', 'board_color'),
('오렌지', '오렌지', 'board_color'),
('컬러풀', '컬러풀', 'board_color');

-- 재료
INSERT INTO board_tag_mapping VALUES
('도장', '페인트', 'board_material'),
('타일/대리석', '타일', 'board_material'),
('금속', '금속', 'board_material'),
('유리', '유리', 'board_material'),
('노출', '노출콘크리트', 'board_material'),
('에폭시', '에폭시', 'board_material'),
('도배', '벽지', 'board_material'),
('우드', '목재', 'board_material'),
('벽돌', '벽돌', 'board_material'),
('콩자갈', '콩자갈', 'board_material'),
('시멘트', '시멘트', 'board_material'),
('모자이크타일', '모자이크타일', 'board_material'),
('패브릭', '패브릭', 'board_material'),
('바리솔', '바리솔', 'board_material');

-- 공간
INSERT INTO board_tag_mapping VALUES
('대기실', '대기실', 'board_space_type'),
('대기공간', '대기실', 'board_space_type'),
('중간대기실', '대기실', 'board_space_type'),
('진료실', '진료실', 'board_space_type'),
('수술실', '수술실', 'board_space_type'),
('시술실', '시술실', 'board_space_type'),
('치료공간', '처치실', 'board_space_type'),
('상담실', '상담실', 'board_space_type'),
('상담공간', '상담실', 'board_space_type'),
('접수', '접수/대기실', 'board_space_type'),
('카운터', '카운터', 'board_space_type'),
('복도', '복도', 'board_space_type'),
('출입구', '출입구', 'board_space_type'),
('화장실', '화장실', 'board_space_type'),
('입원/회복실', '회복실', 'board_space_type'),
('피부관리실', '피부관리실', 'board_space_type'),
('메이크업실', '피부관리실', 'board_space_type'),
('외부', '외부', 'board_space_type');

-- 평수
INSERT INTO board_tag_mapping VALUES
('50평이하', '30평~50평', 'board_area'),
('40평', '30평~50평', 'board_area'),
('50평', '30평~50평', 'board_area'),
('50py', '30평~50평', 'board_area'),
('50평전후', '50평~100평', 'board_area'),
('100평이하', '50평~100평', 'board_area'),
('100평', '50평~100평', 'board_area'),
('100평전후', '100평 이상', 'board_area'),
('150평전후', '100평 이상', 'board_area'),
('200평이하', '200평 이하', 'board_area'),
('200평이상', '200평 이상', 'board_area');

-- 진료과목 (한글)
INSERT INTO board_tag_mapping VALUES
('피부과', '피부과', 'board_medical_specialty'),
('성형외과', '성형외과', 'board_medical_specialty'),
('치과', '치과', 'board_medical_specialty'),
('정형외과', '정형외과', 'board_medical_specialty'),
('내과', '내과', 'board_medical_specialty'),
('안과', '안과', 'board_medical_specialty'),
('이비인후과', '이비인후과', 'board_medical_specialty'),
('한의원', '한의원', 'board_medical_specialty'),
('한방병원', '한의원', 'board_medical_specialty'),
('산부인과', '산부인과', 'board_medical_specialty'),
('비뇨기과', '외과', 'board_medical_specialty'),
('가정의학과', '내과', 'board_medical_specialty'),
('재활의학과', '정형외과', 'board_medical_specialty'),
('신경외과', '외과', 'board_medical_specialty'),
('정신건강의학과', '정신건강의학과', 'board_medical_specialty'),
('정신과', '정신건강의학과', 'board_medical_specialty'),
('외과', '외과', 'board_medical_specialty'),
('소아과', '소아청소년과', 'board_medical_specialty'),
('종합병원', '내과', 'board_medical_specialty');

-- 이미지 유형
INSERT INTO board_tag_mapping VALUES
('실사', '실사', 'board_image_type'),
('3D', '3D 렌더링', 'board_image_type');

-- ============================================
-- 10. Board Filter Options 매핑
-- ============================================

INSERT INTO board_filter_options (board_id, filter_option_id, created_at)
SELECT DISTINCT b.id, fo.id, NOW()
FROM boards b
CROSS JOIN LATERAL unnest(b.tags) AS t(tag_val)
JOIN board_tag_mapping m ON m.tag_value = t.tag_val
JOIN filter_categories fc ON fc.code = m.category_code AND fc.is_deleted = false
JOIN filter_options fo ON fo.category_id = fc.id AND fo.name = m.option_name AND fo.is_deleted = false
WHERE b.tags IS NOT NULL
AND b.board_type IN ('GALLERY', 'DOCUMENT')
AND b.is_deleted = false
AND NOT EXISTS (
    SELECT 1 FROM board_filter_options bfo
    WHERE bfo.board_id = b.id AND bfo.filter_option_id = fo.id
);

-- ============================================
-- 11. 시퀀스 업데이트
-- ============================================

SELECT setval('filter_options_id_seq', COALESCE((SELECT MAX(id) FROM filter_options), 1), true);
SELECT setval('company_filter_options_id_seq', COALESCE((SELECT MAX(id) FROM company_filter_options), 1), true);
SELECT setval('board_filter_options_id_seq', COALESCE((SELECT MAX(id) FROM board_filter_options), 1), true);

-- ============================================
-- 12. 통계 출력
-- ============================================

DO $$
DECLARE
    v_company_filter_count INTEGER;
    v_board_filter_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_company_filter_count FROM company_filter_options;
    SELECT COUNT(*) INTO v_board_filter_count FROM board_filter_options;

    RAISE NOTICE 'Tag Migration Complete:';
    RAISE NOTICE '  - Company Filter Mappings: %', v_company_filter_count;
    RAISE NOTICE '  - Board Filter Mappings: %', v_board_filter_count;
END $$;

-- 임시 테이블 정리
DROP TABLE IF EXISTS company_tag_mapping;
DROP TABLE IF EXISTS board_tag_mapping;
