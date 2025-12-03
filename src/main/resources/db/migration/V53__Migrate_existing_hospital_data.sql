--
-- V53: 기존 병원 업체 데이터를 새 업종(business_type) 필터로 마이그레이션
--
-- 목표: department 필터를 가진 기존 업체에 business_type='hospital' 자동 추가
-- 기존 데이터는 유지하고, 새 필터만 추가합니다.
--

-- ============================================================
-- 1. 기존 병원 업체에 hospital 필터 자동 매핑
-- ============================================================

-- 기존에 department 필터를 가진 업체들을 병원 업체로 인식
-- → business_type='hospital' 필터 옵션 추가
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'hospital' AND depth = 0
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
JOIN filter_categories fc ON fc.id = fo.category_id
WHERE fc.code = 'department'  -- 기존 진료과 필터를 가진 업체
  AND NOT EXISTS (
      -- 이미 hospital 필터가 있는 경우 제외
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'hospital' AND depth = 0
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'hospital' AND depth = 0
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- ============================================================
-- 2. 진료과별 세부 매핑 (선택사항)
-- ============================================================

-- 피부과 태그가 있는 업체 → clinic_dermatology 추가
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_dermatology' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'dermatology'  -- 기존 board_medical_specialty 또는 department의 피부과
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_dermatology' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_dermatology' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 치과 → clinic_dentistry
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_dentistry' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'dentistry'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_dentistry' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_dentistry' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 성형외과 → clinic_plastic
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_plastic' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'plastic'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_plastic' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_plastic' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 내과 → clinic_internal
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_internal' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'internal'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_internal' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_internal' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 안과 → clinic_ophthalmology
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_ophthalmology' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'ophthalmology'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_ophthalmology' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_ophthalmology' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 이비인후과 → clinic_ent
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_ent' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'ent'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_ent' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_ent' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 정형외과 → clinic_orthopedics
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_orthopedics' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'orthopedics'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_orthopedics' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_orthopedics' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 소아청소년과 → clinic_pediatrics
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_pediatrics' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'pediatrics'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_pediatrics' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_pediatrics' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 산부인과 → clinic_obstetrics
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_obstetrics' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'obstetrics'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_obstetrics' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_obstetrics' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 정신건강의학과 → clinic_psychiatry
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'clinic_psychiatry' AND depth = 2
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'psychiatry'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'clinic_psychiatry' AND depth = 2
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'clinic_psychiatry' AND depth = 2
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- 한의원 → hospital_oriental
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'hospital_oriental' AND depth = 1
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code = 'oriental'
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'hospital_oriental' AND depth = 1
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'hospital_oriental' AND depth = 1
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- ============================================================
-- 3. 병원 필터가 있는 업체에 hospital_clinic (의원급) 중간 계층 추가
-- ============================================================

-- clinic_* 필터를 가진 업체는 hospital_clinic도 가져야 함 (중간 계층)
INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
SELECT DISTINCT
    cfo.company_id,
    (SELECT id FROM filter_options WHERE code = 'hospital_clinic' AND depth = 1
     AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')),
    NOW()
FROM company_filter_options cfo
JOIN filter_options fo ON fo.id = cfo.filter_option_id
WHERE fo.code LIKE 'clinic_%' AND fo.depth = 2  -- clinic_으로 시작하는 depth=2 옵션
  AND fo.category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo2
      WHERE cfo2.company_id = cfo.company_id
        AND cfo2.filter_option_id = (
            SELECT id FROM filter_options WHERE code = 'hospital_clinic' AND depth = 1
            AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
        )
  )
  AND (SELECT id FROM filter_options WHERE code = 'hospital_clinic' AND depth = 1
       AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')) IS NOT NULL;

-- ============================================================
-- 검증 쿼리 (실행 결과 확인용 - 주석 처리)
-- ============================================================

-- 1. 업종 필터가 추가된 업체 수
-- SELECT COUNT(DISTINCT company_id) as hospital_company_count
-- FROM company_filter_options
-- WHERE filter_option_id = (SELECT id FROM filter_options WHERE code = 'hospital' AND depth = 0
--    AND category_id = (SELECT id FROM filter_categories WHERE code = 'business_type'));

-- 2. 업종별 업체 수 분포
-- SELECT fo.code, fo.name, COUNT(DISTINCT cfo.company_id) as company_count
-- FROM filter_options fo
-- LEFT JOIN company_filter_options cfo ON cfo.filter_option_id = fo.id
-- WHERE fo.category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
-- GROUP BY fo.id, fo.code, fo.name
-- ORDER BY fo.depth, fo.display_order;

-- 3. 특정 업체의 업종 필터 조회
-- SELECT c.name, fo.code, fo.name as filter_name, fo.depth, fo.path
-- FROM companies c
-- JOIN company_filter_options cfo ON cfo.company_id = c.id
-- JOIN filter_options fo ON fo.id = cfo.filter_option_id
-- WHERE fo.category_id = (SELECT id FROM filter_categories WHERE code = 'business_type')
-- LIMIT 20;
