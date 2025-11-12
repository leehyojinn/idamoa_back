--
-- Board Filter Categories and Options Initial Data
-- 게시판용 필터 카테고리 및 옵션 초기 데이터
--

-- ============================================================
-- Sync sequences first to prevent ID conflicts
-- ============================================================
SELECT setval('filter_categories_id_seq', COALESCE((SELECT MAX(id) FROM filter_categories), 0) + 1, false);
SELECT setval('filter_options_id_seq', COALESCE((SELECT MAX(id) FROM filter_options), 0) + 1, false);

-- ============================================================
-- Filter Categories (GALLERY용 7종 + DOCUMENT용 1종)
-- ============================================================

-- 1. 평수 (GALLERY, DOCUMENT 공통)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_area', '평수', '공간 평수 필터 (병의원)', 'BOARD', 'SINGLE_SELECT', false, 1, 1, true, false, '{"appliesTo": ["GALLERY", "DOCUMENT"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- 2. 진료과목 (GALLERY, DOCUMENT 공통)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_medical_specialty', '진료과목', '병의원 진료과목', 'BOARD', 'MULTI_SELECT', false, 1, 2, true, false, '{"appliesTo": ["GALLERY", "DOCUMENT"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- 3. 공간별 (GALLERY 전용)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_space_type', '공간별', '병의원 공간 타입', 'BOARD', 'MULTI_SELECT', false, 1, 3, true, false, '{"appliesTo": ["GALLERY"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- 4. 스타일 (GALLERY 전용)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_style', '스타일', '인테리어 스타일', 'BOARD', 'MULTI_SELECT', false, 1, 4, true, false, '{"appliesTo": ["GALLERY"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- 5. 컬러 (GALLERY 전용)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_color', '컬러', '주요 컬러 테마', 'BOARD', 'MULTI_SELECT', false, 1, 5, true, false, '{"appliesTo": ["GALLERY"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- 6. 자재 (GALLERY 전용)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_material', '자재', '사용 자재', 'BOARD', 'MULTI_SELECT', false, 1, 6, true, false, '{"appliesTo": ["GALLERY"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- 7. 이미지 유형 (GALLERY 전용)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_image_type', '이미지 유형', '3D 렌더링 또는 실사', 'BOARD', 'SINGLE_SELECT', false, 1, 7, true, false, '{"appliesTo": ["GALLERY"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- 8. 문서 분류 (DOCUMENT 전용)
INSERT INTO public.filter_categories (uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, is_active, is_required, metadata)
VALUES (gen_random_uuid(), 'board_document_category', '문서 분류', '자료실 문서 카테고리', 'BOARD', 'MULTI_SELECT', false, 1, 8, true, false, '{"appliesTo": ["DOCUMENT"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- Filter Options for each category
-- ============================================================

-- 1. 평수 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_area'), '10_under', '10평 미만', '10평↓', '10평 미만', 0, 1, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_area'), '10_20', '10평~20평', '10-20평', '10평 이상 20평 미만', 0, 2, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_area'), '20_30', '20평~30평', '20-30평', '20평 이상 30평 미만', 0, 3, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_area'), '30_50', '30평~50평', '30-50평', '30평 이상 50평 미만', 0, 4, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_area'), '50_100', '50평~100평', '50-100평', '50평 이상 100평 미만', 0, 5, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_area'), '100_over', '100평 이상', '100평↑', '100평 이상', 0, 6, true)
ON CONFLICT (category_id, code) DO NOTHING;

-- 2. 진료과목 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'internal', '내과', '내과', '내과', 0, 1, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'surgery', '외과', '외과', '외과', 0, 2, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'pediatrics', '소아청소년과', '소아과', '소아청소년과', 0, 3, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'obstetrics', '산부인과', '산부인과', '산부인과', 0, 4, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'dermatology', '피부과', '피부과', '피부과', 0, 5, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'ophthalmology', '안과', '안과', '안과', 0, 6, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'ent', '이비인후과', '이비인후과', '이비인후과', 0, 7, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'orthopedics', '정형외과', '정형외과', '정형외과', 0, 8, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'dentistry', '치과', '치과', '치과', 0, 9, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'oriental', '한의원', '한의원', '한의원', 0, 10, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'plastic', '성형외과', '성형외과', '성형외과', 0, 11, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_medical_specialty'), 'psychiatry', '정신건강의학과', '정신과', '정신건강의학과', 0, 12, true)
ON CONFLICT (category_id, code) DO NOTHING;

-- 3. 공간별 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'reception', '접수/대기실', '접수대기', '접수 및 대기 공간', 0, 1, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'consultation', '진료실', '진료실', '진료 공간', 0, 2, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'treatment', '처치실', '처치실', '처치 공간', 0, 3, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'examination', '검사실', '검사실', '검사 공간', 0, 4, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'recovery', '회복실', '회복실', '회복 공간', 0, 5, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'pharmacy', '약국', '약국', '약국 공간', 0, 6, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'office', '원장실', '원장실', '원장실', 0, 7, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'restroom', '화장실', '화장실', '화장실', 0, 8, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'corridor', '복도', '복도', '복도 공간', 0, 9, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_space_type'), 'entrance', '출입구', '출입구', '출입구 및 현관', 0, 10, true)
ON CONFLICT (category_id, code) DO NOTHING;

-- 4. 스타일 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'modern', '모던', '모던', '현대적이고 세련된 스타일', 0, 1, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'classic', '클래식', '클래식', '전통적이고 고풍스러운 스타일', 0, 2, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'minimal', '미니멀', '미니멀', '간결하고 단순한 스타일', 0, 3, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'luxury', '럭셔리', '럭셔리', '고급스러운 스타일', 0, 4, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'natural', '내츄럴', '내츄럴', '자연친화적인 스타일', 0, 5, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'industrial', '인더스트리얼', '인더스트리얼', '산업적이고 빈티지한 스타일', 0, 6, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'scandinavian', '스칸디나비안', '북유럽', '북유럽 스타일', 0, 7, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_style'), 'korean', '한국적', '한국적', '한국 전통 요소가 가미된 스타일', 0, 8, true)
ON CONFLICT (category_id, code) DO NOTHING;

-- 5. 컬러 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active, color)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'white', '화이트', '화이트', '흰색 계열', 0, 1, true, '#FFFFFF'),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'beige', '베이지', '베이지', '베이지 계열', 0, 2, true, '#F5F5DC'),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'gray', '그레이', '그레이', '회색 계열', 0, 3, true, '#808080'),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'brown', '브라운', '브라운', '갈색 계열', 0, 4, true, '#8B4513'),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'blue', '블루', '블루', '파란색 계열', 0, 5, true, '#4169E1'),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'green', '그린', '그린', '녹색 계열', 0, 6, true, '#228B22'),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'black', '블랙', '블랙', '검정 계열', 0, 7, true, '#000000'),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_color'), 'multicolor', '멀티컬러', '다색', '여러 색상 조합', 0, 8, true, NULL)
ON CONFLICT (category_id, code) DO NOTHING;

-- 6. 자재 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'wood', '목재', '목재', '원목, 합판 등', 0, 1, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'metal', '금속', '금속', '철, 스테인리스, 알루미늄 등', 0, 2, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'glass', '유리', '유리', '강화유리, 아크릴 등', 0, 3, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'stone', '석재', '석재', '대리석, 화강석 등', 0, 4, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'tile', '타일', '타일', '세라믹, 포세린 등', 0, 5, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'wallpaper', '벽지', '벽지', '실크벽지, 합지벽지 등', 0, 6, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'paint', '페인트', '페인트', '수성, 유성 페인트', 0, 7, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'fabric', '패브릭', '패브릭', '천, 가죽 등', 0, 8, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_material'), 'plastic', '플라스틱', '플라스틱', '아크릴, PVC 등', 0, 9, true)
ON CONFLICT (category_id, code) DO NOTHING;

-- 7. 이미지 유형 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_image_type'), '3d', '3D 렌더링', '3D', '3D 모델링 렌더링 이미지', 0, 1, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_image_type'), 'real', '실사', '실사', '실제 촬영 사진', 0, 2, true)
ON CONFLICT (category_id, code) DO NOTHING;

-- 8. 문서 분류 옵션
INSERT INTO public.filter_options (uuid, category_id, code, name, short_name, description, depth, display_order, is_active)
VALUES
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'design', '설계도면', '설계도면', '평면도, 입면도 등', 0, 1, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'construction', '시공사례', '시공사례', '시공 관련 문서', 0, 2, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'catalog', '제품카탈로그', '카탈로그', '제품 및 자재 카탈로그', 0, 3, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'manual', '시공매뉴얼', '매뉴얼', '시공 및 설치 매뉴얼', 0, 4, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'specification', '사양서', '사양서', '제품/자재 사양서', 0, 5, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'contract', '계약서 양식', '계약서', '계약 관련 양식', 0, 6, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'guideline', '가이드라인', '가이드', '디자인/시공 가이드', 0, 7, true),
    (gen_random_uuid(), (SELECT id FROM filter_categories WHERE code = 'board_document_category'), 'regulation', '법규/규정', '법규', '건축 관련 법규 및 규정', 0, 8, true)
ON CONFLICT (category_id, code) DO NOTHING;


-- ============================================================
-- Update sequence values to ensure no conflicts
-- ============================================================

SELECT setval('filter_categories_id_seq', (SELECT MAX(id) FROM filter_categories));
SELECT setval('filter_options_id_seq', (SELECT MAX(id) FROM filter_options));
