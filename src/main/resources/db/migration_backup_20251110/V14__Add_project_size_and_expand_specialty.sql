-- =============================================================================
-- V14: 작업평수 카테고리 추가 및 전문영역 확장
-- =============================================================================
-- 설명: 업체 등록 시 선택할 수 있는 추가 필터 옵션 제공
-- 작성일: 2025-11-05
-- 내용:
--   - 작업평수 카테고리 추가
--   - 전문영역(specialty) 확장: 인테리어, 마케팅, 홈페이지, 청소, 에어컨 등
-- =============================================================================

-- =============================================================================
-- 1. 작업평수 카테고리 추가
-- =============================================================================
INSERT INTO filter_categories (code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order)
VALUES
('project_size', '작업평수', '프로젝트 규모 (평수)', 'COMPANY', 'SINGLE_SELECT', false, 1, 6)
ON CONFLICT (code) DO NOTHING;

-- 작업평수 옵션
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'project_size'), 'under-10', '10평 이하', 0, NULL, 1),
((SELECT id FROM filter_categories WHERE code = 'project_size'), '10-20', '10평 ~ 20평', 0, NULL, 2),
((SELECT id FROM filter_categories WHERE code = 'project_size'), '20-30', '20평 ~ 30평', 0, NULL, 3),
((SELECT id FROM filter_categories WHERE code = 'project_size'), '30-50', '30평 ~ 50평', 0, NULL, 4),
((SELECT id FROM filter_categories WHERE code = 'project_size'), 'over-50', '50평 이상', 0, NULL, 5),
((SELECT id FROM filter_categories WHERE code = 'project_size'), 'any-size', '평수 무관', 0, NULL, 6)
ON CONFLICT (category_id, code) DO NOTHING;

-- =============================================================================
-- 2. 전문영역(specialty) 확장
-- =============================================================================

-- 기존 인테리어 관련 옵션 유지하면서 새 카테고리 추가
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
-- 인테리어 세부 분야
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'interior-design', '인테리어 디자인', 0, NULL, 10),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'interior-construction', '인테리어 시공', 0, NULL, 11),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'furniture-custom', '가구 제작', 0, NULL, 12),

-- 마케팅
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'marketing-online', '온라인 마케팅', 0, NULL, 20),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'marketing-sns', 'SNS 마케팅', 0, NULL, 21),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'marketing-brand', '브랜딩', 0, NULL, 22),

-- 웹/IT
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'web-development', '홈페이지 제작', 0, NULL, 30),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'mobile-app', '모바일 앱 개발', 0, NULL, 31),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'system-development', '시스템 개발', 0, NULL, 32),

-- 청소/관리
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'cleaning-office', '사무실 청소', 0, NULL, 40),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'cleaning-home', '가정 청소', 0, NULL, 41),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'cleaning-move', '이사 청소', 0, NULL, 42),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'cleaning-special', '특수 청소', 0, NULL, 43),

-- 냉난방/설비
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'ac-install', '에어컨 설치', 0, NULL, 50),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'ac-repair', '에어컨 수리', 0, NULL, 51),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'ac-maintenance', '에어컨 관리', 0, NULL, 52),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'heating-install', '난방 설치', 0, NULL, 53),

-- 전기/설비
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'electric-work', '전기 공사', 0, NULL, 60),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'plumbing', '배관 공사', 0, NULL, 61),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'waterproof', '방수 공사', 0, NULL, 62),

-- 기타 서비스
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'moving', '이사', 0, NULL, 70),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'painting', '도배/페인팅', 0, NULL, 71),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'window-door', '창호/샷시', 0, NULL, 72),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'flooring', '바닥재 시공', 0, NULL, 73),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'garden', '조경/정원', 0, NULL, 74),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'pest-control', '방역/해충 방제', 0, NULL, 75)
ON CONFLICT (category_id, code) DO NOTHING;

-- =============================================================================
-- 3. 기존 department(진료과) 카테고리 확장
-- =============================================================================

-- 추가 진료과 옵션
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'neurology', '신경과', 0, NULL, 9),
((SELECT id FROM filter_categories WHERE code = 'department'), 'psychiatry', '정신과', 0, NULL, 10),
((SELECT id FROM filter_categories WHERE code = 'department'), 'obgyn', '산부인과', 0, NULL, 11),
((SELECT id FROM filter_categories WHERE code = 'department'), 'pediatrics', '소아과', 0, NULL, 12),
((SELECT id FROM filter_categories WHERE code = 'department'), 'urology', '비뇨기과', 0, NULL, 13),
((SELECT id FROM filter_categories WHERE code = 'department'), 'family-medicine', '가정의학과', 0, NULL, 14),
((SELECT id FROM filter_categories WHERE code = 'department'), 'rehabilitation', '재활의학과', 0, NULL, 15),
((SELECT id FROM filter_categories WHERE code = 'department'), 'radiology', '영상의학과', 0, NULL, 16),
((SELECT id FROM filter_categories WHERE code = 'department'), 'anesthesiology', '마취통증의학과', 0, NULL, 17),
((SELECT id FROM filter_categories WHERE code = 'department'), 'emergency', '응급의학과', 0, NULL, 18),
((SELECT id FROM filter_categories WHERE code = 'department'), 'lab-medicine', '진단검사의학과', 0, NULL, 19),
((SELECT id FROM filter_categories WHERE code = 'department'), 'any-department', '진료과 무관', 0, NULL, 99)
ON CONFLICT (category_id, code) DO NOTHING;

-- =============================================================================
-- 4. region(지역) 카테고리 확장
-- =============================================================================

-- 추가 광역시/도
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'sejong', '세종', 0, NULL, 9),
((SELECT id FROM filter_categories WHERE code = 'region'), 'gangwon', '강원', 0, NULL, 10),
((SELECT id FROM filter_categories WHERE code = 'region'), 'chungbuk', '충북', 0, NULL, 11),
((SELECT id FROM filter_categories WHERE code = 'region'), 'chungnam', '충남', 0, NULL, 12),
((SELECT id FROM filter_categories WHERE code = 'region'), 'jeonbuk', '전북', 0, NULL, 13),
((SELECT id FROM filter_categories WHERE code = 'region'), 'jeonnam', '전남', 0, NULL, 14),
((SELECT id FROM filter_categories WHERE code = 'region'), 'gyeongbuk', '경북', 0, NULL, 15),
((SELECT id FROM filter_categories WHERE code = 'region'), 'gyeongnam', '경남', 0, NULL, 16),
((SELECT id FROM filter_categories WHERE code = 'region'), 'jeju', '제주', 0, NULL, 17),
((SELECT id FROM filter_categories WHERE code = 'region'), 'all-region', '전국', 0, NULL, 99)
ON CONFLICT (category_id, code) DO NOTHING;

-- =============================================================================
-- 완료 메시지
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'V14 마이그레이션 완료: 작업평수 및 전문영역 확장';
    RAISE NOTICE '- 추가 카테고리: 1개 (작업평수)';
    RAISE NOTICE '- 추가 옵션: 60+ 개';
    RAISE NOTICE '  * 작업평수: 6개';
    RAISE NOTICE '  * 전문영역: 30+ 개 (인테리어, 마케팅, 웹/IT, 청소, 냉난방, 전기 등)';
    RAISE NOTICE '  * 진료과: 12개';
    RAISE NOTICE '  * 지역: 10개';
    RAISE NOTICE '=============================================================================';
END $$;
