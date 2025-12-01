--
-- V40: Company 덤프 데이터 마이그레이션
-- 이전 시스템(damoa_company)에서 현재 시스템(companies)으로 데이터 이전
--

-- ============================================
-- 1. 임시 테이블 생성 (덤프 데이터 저장용)
-- ============================================

CREATE TEMPORARY TABLE temp_damoa_company (
    seq INTEGER,
    uuid UUID,
    business_name TEXT,
    introduce_title TEXT,
    introduce_description TEXT,
    location_point TEXT,  -- geometry는 text로 받아서 무시
    tag_skills TEXT[],
    tag_specialties TEXT[],
    create_datetime TIMESTAMPTZ,
    update_datetime TIMESTAMPTZ,
    like_count INTEGER,
    view_count INTEGER,
    location_shortname TEXT[],
    homepage_url TEXT,
    primary_thumbnail_url TEXT,
    business_phone TEXT,
    ceo_name TEXT,
    ceo_phone TEXT,
    recommendation_score INTEGER,
    like_count_offset INTEGER,
    view_count_offset INTEGER,
    owner_user_uuid UUID
);

-- ============================================
-- 2. 덤프 데이터 INSERT (company_dump_file.sql에서 복사)
-- ============================================

INSERT INTO temp_damoa_company (seq, uuid, business_name, introduce_title, introduce_description, location_point, tag_skills, tag_specialties, create_datetime, update_datetime, like_count, view_count, location_shortname, homepage_url, primary_thumbnail_url, business_phone, ceo_name, ceo_phone, recommendation_score, like_count_offset, view_count_offset, owner_user_uuid) VALUES (1975, 'eca244e2-fdb9-4cb7-adae-3815be952cd8', '디자인본어비', '', '자카르타 진출기업 인테리어, 사무공간 인테리어, 병.의원 인테리어 전문기업 전략이 강한기업', '0101000020E610000000000000000000000000000000000000', '{interior}', '{dermatology,plastic_surgery,orthopedic,internal_medicine,dental,ophthalmology,oriental_medicine,oriental_hospital,obstetrics_gynecology,urology,ent,family_medicine,rehabilitation_medicine,neurosurgery,anesthesiology,psychiatry,surgery,radiology,pediatrics,health_checkup_center}', '2025-10-02 05:39:33.713695+00', '2025-11-28 06:10:41.367083+00', 52, 209, '{seoul,daegu}', 'http://dbornabe.com/', 'images/251014/cd8fc596-50bc-45d9-9282-81195291de8c-pre.jpg', '02-404-2415', '박상현', '', 100, 52, 161, NULL);
INSERT INTO temp_damoa_company (seq, uuid, business_name, introduce_title, introduce_description, location_point, tag_skills, tag_specialties, create_datetime, update_datetime, like_count, view_count, location_shortname, homepage_url, primary_thumbnail_url, business_phone, ceo_name, ceo_phone, recommendation_score, like_count_offset, view_count_offset, owner_user_uuid) VALUES (2183, '29c59598-b2f4-4480-871d-2c206e56688a', '진디자인', '', 'J I N . D E S I G N', '0101000020E610000000000000000000000000000000000000', '{signage}', '{}', '2025-10-02 05:40:19.950528+00', '2025-11-14 08:44:40.291733+00', 0, 2, '{chungbuk}', 'https://www.ok114.co.kr/ntnw/search/searchCompanyOfMainPh.crz;jsessionid=BC4774AD77F8C7D911A4DB08D3ADCA79?phone_number=0438556460', 'images/1762495485650_318805778_1848308492186646_4402959847909125825_n.jpg', '043-855-6460', '김미진', '', 0, 0, 0, NULL);

-- TODO: 실제 덤프 파일의 모든 INSERT 문을 여기에 추가해야 합니다.
-- 덤프 파일이 너무 커서 수동으로 복사하거나 스크립트로 처리해야 합니다.

-- ============================================
-- 3. companies 테이블에 데이터 삽입
-- ============================================

INSERT INTO companies (
    uuid,
    name,
    slug,
    description,
    detail_content,
    detail_content_format,
    business_info,
    tags,
    primary_phone,
    secondary_phone,
    website_url,
    view_count,
    like_count,
    status,
    featured,
    verified,
    premium_tier,
    avg_rating,
    review_count,
    portfolio_count,
    completed_projects,
    premium_monthly_amount,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    t.uuid,
    t.business_name,
    LOWER(REPLACE(t.business_name, ' ', '-')) || '-' || t.seq,  -- slug 생성
    NULLIF(t.introduce_description, ''),
    NULL,  -- detail_content
    'HTML',
    jsonb_build_object(
        'ceoName', NULLIF(t.ceo_name, ''),
        'thumbnailUrl', NULLIF(t.primary_thumbnail_url, ''),
        'introduceTitle', NULLIF(t.introduce_title, '')
    ),
    CASE
        WHEN t.tag_skills IS NOT NULL AND t.tag_specialties IS NOT NULL
            THEN t.tag_skills || t.tag_specialties
        WHEN t.tag_skills IS NOT NULL
            THEN t.tag_skills
        WHEN t.tag_specialties IS NOT NULL
            THEN t.tag_specialties
        ELSE NULL
    END,
    NULLIF(t.business_phone, ''),
    NULLIF(t.ceo_phone, ''),
    NULLIF(t.homepage_url, ''),
    COALESCE(t.view_count, 0) + COALESCE(t.view_count_offset, 0),
    COALESCE(t.like_count, 0) + COALESCE(t.like_count_offset, 0),
    'ACTIVE',
    CASE WHEN t.recommendation_score >= 50 THEN true ELSE false END,
    false,
    'NONE',
    0,
    0,
    0,
    0,
    0,
    t.create_datetime AT TIME ZONE 'UTC',
    t.update_datetime AT TIME ZONE 'UTC',
    false
FROM temp_damoa_company t
WHERE NOT EXISTS (
    SELECT 1 FROM companies c WHERE c.uuid = t.uuid
);

-- ============================================
-- 4. company_filter_options 테이블에 지역 필터 추가
-- location_shortname 배열의 각 값을 filter_option으로 매핑
-- ============================================

-- 먼저 지역 필터 옵션들이 있는지 확인하고 없으면 생성
-- (filter_options 테이블에 REGION 카테고리의 옵션들이 필요)

-- 지역 매핑 테이블 생성
CREATE TEMPORARY TABLE region_mapping (
    code TEXT PRIMARY KEY,
    name TEXT
);

INSERT INTO region_mapping (code, name) VALUES
    ('seoul', '서울'),
    ('gyeonggi', '경기'),
    ('incheon', '인천'),
    ('busan', '부산'),
    ('daegu', '대구'),
    ('gwangju', '광주'),
    ('daejeon', '대전'),
    ('sejong', '세종'),
    ('gangwon', '강원'),
    ('chungbuk', '충북'),
    ('chungnam', '충남'),
    ('jeonbuk', '전북'),
    ('jeonnam', '전남'),
    ('gyeongbuk', '경북'),
    ('gyeongnam', '경남'),
    ('jeju', '제주');

-- company_filter_options에 지역 필터 추가
-- (filter_options 테이블에 해당 지역 코드가 있어야 함)
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    c.id,
    fo.id,
    CURRENT_TIMESTAMP
FROM temp_damoa_company t
CROSS JOIN LATERAL unnest(t.location_shortname) AS region_code
JOIN companies c ON c.uuid = t.uuid
JOIN filter_options fo ON fo.code = region_code AND fo.is_deleted = false
WHERE NOT EXISTS (
    SELECT 1 FROM company_filter_options cfo
    WHERE cfo.company_id = c.id AND cfo.filter_option_id = fo.id
);

-- ============================================
-- 5. 시퀀스 업데이트
-- ============================================

SELECT setval('companies_id_seq', COALESCE((SELECT MAX(id) FROM companies), 1), true);
SELECT setval('company_filter_options_id_seq', COALESCE((SELECT MAX(id) FROM company_filter_options), 1), true);

-- ============================================
-- 6. 통계 출력
-- ============================================

DO $$
DECLARE
    v_imported INTEGER;
    v_total INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM temp_damoa_company;
    SELECT COUNT(*) INTO v_imported FROM companies WHERE uuid IN (SELECT uuid FROM temp_damoa_company);

    RAISE NOTICE 'Company Migration Complete: % / % imported', v_imported, v_total;
END $$;

-- 임시 테이블 정리
DROP TABLE IF EXISTS region_mapping;
DROP TABLE IF EXISTS temp_damoa_company;
