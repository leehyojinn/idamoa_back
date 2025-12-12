--
-- V63: specialty 필터 옵션 통합
--
-- 목적:
--   1. 분할된 서비스 옵션들을 대표 옵션으로 통합
--   2. 기존 인테리어 관련 옵션들 → hospital-interior로 통합
--   3. 새로운 인테리어 전문영역 추가 (아파트, 카페, 미용실)
--

-- ============================================================
-- 1. 통합 매핑 테이블 생성
-- ============================================================

CREATE TABLE IF NOT EXISTS specialty_consolidation_mapping (
    old_code VARCHAR(100),
    new_code VARCHAR(100)
);

TRUNCATE TABLE specialty_consolidation_mapping;

-- 서비스 관련 통합
INSERT INTO specialty_consolidation_mapping VALUES
    ('marketing-online', 'marketing'),
    ('marketing-sns', 'marketing'),
    ('marketing-brand', 'marketing'),
    ('seo', 'marketing'),
    ('sns-marketing', 'marketing'),
    ('video-production', 'marketing'),
    ('photography', 'marketing'),
    ('graphic-design', 'marketing');

INSERT INTO specialty_consolidation_mapping VALUES
    ('web-development', 'homepage'),
    ('web-dev', 'homepage'),
    ('mobile-app', 'homepage'),
    ('system-development', 'homepage');

INSERT INTO specialty_consolidation_mapping VALUES
    ('cleaning-office', 'cleaning'),
    ('cleaning-home', 'cleaning'),
    ('cleaning-move', 'cleaning'),
    ('cleaning-special', 'cleaning'),
    ('pest-control', 'cleaning');

INSERT INTO specialty_consolidation_mapping VALUES
    ('ac-install', 'aircon'),
    ('ac-repair', 'aircon'),
    ('ac-maintenance', 'aircon'),
    ('air-conditioner', 'aircon'),
    ('heating-install', 'aircon');

INSERT INTO specialty_consolidation_mapping VALUES
    ('communication', 'internet'),
    ('locksmith', 'security');

-- 인테리어 관련 → hospital-interior로 통합
INSERT INTO specialty_consolidation_mapping VALUES
    ('residential', 'hospital-interior'),
    ('commercial', 'hospital-interior'),
    ('office', 'hospital-interior'),
    ('medical', 'hospital-interior'),
    ('remodeling', 'hospital-interior'),
    ('new-construction', 'hospital-interior'),
    ('extension', 'hospital-interior'),
    ('new-interior', 'hospital-interior'),
    ('general-interior', 'hospital-interior'),
    ('interior-design', 'hospital-interior'),
    ('interior-construction', 'hospital-interior'),
    ('furniture-custom', 'hospital-interior'),
    ('furniture', 'hospital-interior'),
    ('home-styling', 'hospital-interior'),
    ('wallpaper', 'hospital-interior'),
    ('flooring', 'hospital-interior'),
    ('lighting', 'hospital-interior'),
    ('window', 'hospital-interior'),
    ('window-door', 'hospital-interior'),
    ('painting', 'hospital-interior'),
    ('electric-work', 'hospital-interior'),
    ('electrical', 'hospital-interior'),
    ('plumbing', 'hospital-interior'),
    ('waterproof', 'hospital-interior'),
    ('waterproofing', 'hospital-interior'),
    ('garden', 'hospital-interior'),
    ('moving', 'hospital-interior'),
    ('storage', 'hospital-interior'),
    ('skin-interior', 'hospital-interior'),
    ('dental-interior', 'hospital-interior'),
    ('sterilization', 'hospital-interior'),
    ('consulting', 'hospital-interior'),
    ('accounting', 'hospital-interior'),
    ('legal', 'hospital-interior'),
    ('insurance', 'hospital-interior'),
    ('real-estate', 'hospital-interior');

-- ============================================================
-- 2. 대표 옵션 추가
-- ============================================================

DO $$
DECLARE
    v_specialty_category_id INTEGER;
    v_max_order INTEGER;
BEGIN
    SELECT id INTO v_specialty_category_id
    FROM filter_categories WHERE code = 'specialty' AND is_deleted = false;

    IF v_specialty_category_id IS NULL THEN
        RAISE NOTICE 'specialty 카테고리가 없습니다. 스킵.';
        RETURN;
    END IF;

    SELECT COALESCE(MAX(display_order), 0) INTO v_max_order
    FROM filter_options WHERE category_id = v_specialty_category_id;

    -- 서비스 대표 옵션 추가
    INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
    SELECT gen_random_uuid(), v_specialty_category_id, v.code, v.name, v_max_order + v.ord, true, NOW(), NOW(), false
    FROM (VALUES
        ('homepage', '홈페이지', 1),
        ('aircon', '에어컨', 2),
        ('security', '보안', 3)
    ) AS v(code, name, ord)
    WHERE NOT EXISTS (
        SELECT 1 FROM filter_options
        WHERE category_id = v_specialty_category_id AND code = v.code
    );

    -- 새로운 인테리어 전문영역 추가
    INSERT INTO filter_options (uuid, category_id, code, name, display_order, is_active, created_at, updated_at, is_deleted)
    SELECT gen_random_uuid(), v_specialty_category_id, v.code, v.name, v_max_order + v.ord, true, NOW(), NOW(), false
    FROM (VALUES
        ('apartment-interior', '아파트인테리어', 10),
        ('cafe-interior', '카페인테리어', 11),
        ('salon-interior', '미용실인테리어', 12)
    ) AS v(code, name, ord)
    WHERE NOT EXISTS (
        SELECT 1 FROM filter_options
        WHERE category_id = v_specialty_category_id AND code = v.code
    );

    RAISE NOTICE '대표 옵션 및 신규 인테리어 전문영역 추가 완료';
END $$;

-- ============================================================
-- 3. company_filter_options 통합 (DELETE + INSERT 방식)
-- ============================================================

DO $$
DECLARE
    v_specialty_category_id INTEGER;
    v_inserted_count INTEGER := 0;
    v_deleted_count INTEGER := 0;
BEGIN
    SELECT id INTO v_specialty_category_id
    FROM filter_categories WHERE code = 'specialty' AND is_deleted = false;

    IF v_specialty_category_id IS NULL THEN
        RAISE NOTICE 'specialty 카테고리가 없습니다. 스킵.';
        RETURN;
    END IF;

    RAISE NOTICE 'specialty_category_id: %', v_specialty_category_id;

    -- ============================================================
    -- 3-1. 통합 대상 company 목록을 임시 테이블에 저장
    -- ============================================================

    CREATE TEMP TABLE companies_to_migrate AS
    SELECT DISTINCT cfo.company_id, m.new_code
    FROM company_filter_options cfo
    JOIN filter_options fo ON fo.id = cfo.filter_option_id
    JOIN specialty_consolidation_mapping m ON m.old_code = fo.code
    WHERE fo.category_id = v_specialty_category_id;

    RAISE NOTICE '통합 대상 company-option 쌍: %건', (SELECT COUNT(*) FROM companies_to_migrate);

    -- ============================================================
    -- 3-2. 기존 분할 옵션 연결 삭제
    -- ============================================================

    DELETE FROM company_filter_options
    WHERE filter_option_id IN (
        SELECT fo.id
        FROM filter_options fo
        JOIN specialty_consolidation_mapping m ON m.old_code = fo.code
        WHERE fo.category_id = v_specialty_category_id
    );

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RAISE NOTICE '삭제된 기존 연결: %건', v_deleted_count;

    -- ============================================================
    -- 3-3. 대표 옵션으로 새로 INSERT (중복 무시)
    -- ============================================================

    INSERT INTO company_filter_options (company_id, filter_option_id, created_at)
    SELECT DISTINCT
        ctm.company_id,
        fo_new.id,
        NOW()
    FROM companies_to_migrate ctm
    JOIN filter_options fo_new ON fo_new.code = ctm.new_code
        AND fo_new.category_id = v_specialty_category_id
    WHERE NOT EXISTS (
        SELECT 1 FROM company_filter_options cfo
        WHERE cfo.company_id = ctm.company_id
          AND cfo.filter_option_id = fo_new.id
    );

    GET DIAGNOSTICS v_inserted_count = ROW_COUNT;
    RAISE NOTICE '새로 추가된 연결: %건', v_inserted_count;

    -- 임시 테이블 삭제
    DROP TABLE IF EXISTS companies_to_migrate;

END $$;

-- ============================================================
-- 4. 통합된 분할 옵션 비활성화
-- ============================================================

DO $$
DECLARE
    v_specialty_category_id INTEGER;
    v_deactivated_count INTEGER;
BEGIN
    SELECT id INTO v_specialty_category_id
    FROM filter_categories WHERE code = 'specialty' AND is_deleted = false;

    IF v_specialty_category_id IS NULL THEN
        RETURN;
    END IF;

    UPDATE filter_options
    SET is_active = false, updated_at = NOW()
    WHERE category_id = v_specialty_category_id
      AND code IN (SELECT old_code FROM specialty_consolidation_mapping);

    GET DIAGNOSTICS v_deactivated_count = ROW_COUNT;
    RAISE NOTICE '비활성화된 분할 옵션: %건', v_deactivated_count;
END $$;

-- ============================================================
-- 5. 검증
-- ============================================================

DO $$
DECLARE
    v_specialty_category_id INTEGER;
    rec RECORD;
BEGIN
    SELECT id INTO v_specialty_category_id
    FROM filter_categories WHERE code = 'specialty' AND is_deleted = false;

    IF v_specialty_category_id IS NULL THEN
        RETURN;
    END IF;

    RAISE NOTICE '';
    RAISE NOTICE '=== 활성 specialty 옵션 목록 ===';
    FOR rec IN
        SELECT fo.code, fo.name, COUNT(DISTINCT cfo.company_id) as cnt
        FROM filter_options fo
        LEFT JOIN company_filter_options cfo ON cfo.filter_option_id = fo.id
        WHERE fo.category_id = v_specialty_category_id
          AND fo.is_active = true
          AND fo.is_deleted = false
        GROUP BY fo.code, fo.name, fo.display_order
        ORDER BY cnt DESC, fo.display_order
    LOOP
        RAISE NOTICE '  %: % (%건)', rec.code, rec.name, rec.cnt;
    END LOOP;
END $$;

-- ============================================================
-- 6. 정리
-- ============================================================

DROP TABLE IF EXISTS specialty_consolidation_mapping;
