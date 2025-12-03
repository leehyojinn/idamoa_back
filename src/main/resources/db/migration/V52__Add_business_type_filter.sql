--
-- V52: 업종(business_type) 필터 카테고리 및 계층적 옵션 추가
-- 인테리어다모아 통합 플랫폼 - 다중 업종 지원
--
-- 계층 구조 (parent_id 기반):
--   business_type (카테고리)
--     └─ hospital (depth=0, parent_id=NULL)
--         └─ hospital_clinic (depth=1, parent_id=hospital)
--             └─ dermatology (depth=2, parent_id=hospital_clinic)
--

-- ============================================================
-- 1. 업종 분류 카테고리 생성
-- ============================================================
INSERT INTO filter_categories (
    uuid, code, name, description, entity_type,
    filter_type, supports_hierarchy, max_depth,
    display_order, is_active, is_required, metadata
)
VALUES (
    gen_random_uuid(),
    'business_type',
    '업종',
    '인테리어 서비스 업종 분류 (계층 구조 지원)',
    'COMPANY',
    'MULTI_SELECT',
    true,           -- 계층 구조 지원
    3,              -- 최대 4단계 (depth 0, 1, 2, 3)
    1,              -- 최우선 표시
    true,
    true,           -- 필수 선택
    '{}'::jsonb
)
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 2. Level 0 (depth=0): 업종 대분류 (parent_id=NULL)
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    NULL,  -- parent_id = NULL (루트 레벨)
    0,     -- depth = 0
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('hospital', '병원 인테리어', '병원', '병의원 및 의료기관 인테리어', '/hospital', 10),
    ('cafe', '카페 인테리어', '카페', '카페/디저트/베이커리 인테리어', '/cafe', 20),
    ('office', '사무실 인테리어', '사무실', '오피스/코워킹 공간 인테리어', '/office', 30),
    ('residential', '주거 인테리어', '주거', '아파트/빌라/주택 인테리어', '/residential', 40),
    ('education', '교육시설 인테리어', '교육', '학원/교육시설 인테리어', '/education', 50),
    ('commercial', '상업시설 인테리어', '상업', '매장/음식점/리테일 인테리어', '/commercial', 60)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 3. Level 1 (depth=1): 병원 하위 분류
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    (SELECT id FROM filter_options WHERE code = 'hospital' AND depth = 0
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    1,
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('hospital_clinic', '의원급', '의원', '개인 의원 (치과, 피부과, 한의원 등)', '/hospital/clinic', 10),
    ('hospital_general', '병원급', '병원', '종합병원, 전문병원', '/hospital/general', 20),
    ('hospital_oriental', '한방병원', '한방', '한방병원, 한의원', '/hospital/oriental', 30)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 4. Level 2 (depth=2): 의원급 > 진료과
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    (SELECT id FROM filter_options WHERE code = 'hospital_clinic' AND depth = 1
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    2,
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('clinic_dermatology', '피부과', '피부과', '피부과 의원', '/hospital/clinic/dermatology', 10),
    ('clinic_plastic', '성형외과', '성형외과', '성형외과 의원', '/hospital/clinic/plastic', 20),
    ('clinic_dentistry', '치과', '치과', '치과 의원', '/hospital/clinic/dentistry', 30),
    ('clinic_internal', '내과', '내과', '내과 의원', '/hospital/clinic/internal', 40),
    ('clinic_ophthalmology', '안과', '안과', '안과 의원', '/hospital/clinic/ophthalmology', 50),
    ('clinic_ent', '이비인후과', '이비인후과', '이비인후과 의원', '/hospital/clinic/ent', 60),
    ('clinic_orthopedics', '정형외과', '정형외과', '정형외과 의원', '/hospital/clinic/orthopedics', 70),
    ('clinic_pediatrics', '소아청소년과', '소아과', '소아청소년과 의원', '/hospital/clinic/pediatrics', 80),
    ('clinic_obstetrics', '산부인과', '산부인과', '산부인과 의원', '/hospital/clinic/obstetrics', 90),
    ('clinic_psychiatry', '정신건강의학과', '정신과', '정신건강의학과 의원', '/hospital/clinic/psychiatry', 100)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 5. Level 1 (depth=1): 카페 하위 분류
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    (SELECT id FROM filter_options WHERE code = 'cafe' AND depth = 0
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    1,
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('cafe_specialty', '스페셜티 커피', '스페셜티', '스페셜티 커피 전문점', '/cafe/specialty', 10),
    ('cafe_dessert', '디저트 카페', '디저트', '디저트/케이크 카페', '/cafe/dessert', 20),
    ('cafe_bakery', '베이커리 카페', '베이커리', '베이커리/빵집 카페', '/cafe/bakery', 30),
    ('cafe_brunch', '브런치 카페', '브런치', '브런치/샐러드 카페', '/cafe/brunch', 40),
    ('cafe_roastery', '로스터리 카페', '로스터리', '자체 로스팅 카페', '/cafe/roastery', 50)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 6. Level 1 (depth=1): 사무실 하위 분류
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    (SELECT id FROM filter_options WHERE code = 'office' AND depth = 0
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    1,
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('office_corporate', '기업 오피스', '기업', '대기업/중견기업 사무실', '/office/corporate', 10),
    ('office_startup', '스타트업 오피스', '스타트업', '스타트업/벤처 사무실', '/office/startup', 20),
    ('office_coworking', '코워킹 스페이스', '코워킹', '공유 오피스/코워킹', '/office/coworking', 30),
    ('office_soho', '소호 오피스', '소호', '소규모 사무실/홈오피스', '/office/soho', 40)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 7. Level 1 (depth=1): 주거 하위 분류
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    (SELECT id FROM filter_options WHERE code = 'residential' AND depth = 0
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    1,
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('residential_apartment', '아파트', '아파트', '아파트 인테리어', '/residential/apartment', 10),
    ('residential_villa', '빌라/연립', '빌라', '빌라/연립주택 인테리어', '/residential/villa', 20),
    ('residential_house', '단독주택', '주택', '단독주택 인테리어', '/residential/house', 30),
    ('residential_officetel', '오피스텔', '오피스텔', '오피스텔 인테리어', '/residential/officetel', 40)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 8. Level 1 (depth=1): 교육시설 하위 분류
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    (SELECT id FROM filter_options WHERE code = 'education' AND depth = 0
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    1,
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('education_academy', '학원', '학원', '보습/입시/어학 학원', '/education/academy', 10),
    ('education_kindergarten', '유치원/어린이집', '유치원', '유아 교육 시설', '/education/kindergarten', 20),
    ('education_art', '예체능 학원', '예체능', '음악/미술/체육 학원', '/education/art', 30),
    ('education_library', '도서관/스터디카페', '도서관', '도서관/독서실/스터디카페', '/education/library', 40)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 9. Level 1 (depth=1): 상업시설 하위 분류
-- ============================================================
INSERT INTO filter_options (
    uuid, category_id, code, name, short_name, description,
    parent_id, depth, path, display_order, is_active
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM filter_categories WHERE code = 'business_type'),
    v.code, v.name, v.short_name, v.description,
    (SELECT id FROM filter_options WHERE code = 'commercial' AND depth = 0
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    1,
    v.path,
    v.display_order,
    true
FROM (VALUES
    ('commercial_restaurant', '음식점', '음식점', '레스토랑/식당 인테리어', '/commercial/restaurant', 10),
    ('commercial_retail', '매장/리테일', '매장', '판매 매장/리테일 인테리어', '/commercial/retail', 20),
    ('commercial_beauty', '뷰티샵', '뷰티', '미용실/네일샵/에스테틱', '/commercial/beauty', 30),
    ('commercial_fitness', '피트니스/헬스', '피트니스', '헬스장/피트니스/필라테스', '/commercial/fitness', 40),
    ('commercial_pet', '펫샵/동물병원', '펫', '펫샵/애견카페/동물병원', '/commercial/pet', 50)
) AS v(code, name, short_name, description, path, display_order)
ON CONFLICT (category_id, code) DO NOTHING;

-- ============================================================
-- 10. Sequence 업데이트
-- ============================================================
SELECT setval('filter_categories_id_seq', (SELECT COALESCE(MAX(id), 0) FROM filter_categories));
SELECT setval('filter_options_id_seq', (SELECT COALESCE(MAX(id), 0) FROM filter_options));

-- ============================================================
-- 검증 쿼리 (실행 결과 확인용 - 주석 처리)
-- ============================================================
-- SELECT
--     fo.id,
--     fo.code,
--     fo.name,
--     fo.depth,
--     fo.parent_id,
--     p.code as parent_code,
--     fo.path
-- FROM filter_options fo
-- LEFT JOIN filter_options p ON p.id = fo.parent_id
-- WHERE fo.category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
-- ORDER BY fo.path, fo.depth, fo.display_order;
