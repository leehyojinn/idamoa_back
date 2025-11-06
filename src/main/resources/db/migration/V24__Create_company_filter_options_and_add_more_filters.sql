-- =============================================================================
-- V24: 업체-필터 다대다 관계 테이블 생성 및 필터 옵션 추가
-- =============================================================================
-- 설명: 업체 등록 시 필터 선택 기능 추가
-- 작성일: 2025-11-06
-- 내용:
--   - company_filter_options: 업체와 필터 옵션 다대다 조인 테이블
--   - project_size_range: 작업 평수 필터 카테고리 추가
--   - specialty: 더 많은 업종 추가 (인테리어, 마케팅, 홈페이지, 청소 등)
-- =============================================================================

-- =============================================================================
-- 1. 업체-필터 옵션 다대다 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS company_filter_options (
    id BIGSERIAL PRIMARY KEY,

    -- 업체와 필터 옵션 관계
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    filter_option_id BIGINT NOT NULL REFERENCES filter_options(id) ON DELETE CASCADE,

    -- 기본 필드
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 고유 제약조건 (동일한 업체-필터 옵션 조합은 하나만 존재)
    CONSTRAINT uk_company_filter_option UNIQUE(company_id, filter_option_id)
);

-- 인덱스
CREATE INDEX idx_company_filter_options_company_id ON company_filter_options(company_id);
CREATE INDEX idx_company_filter_options_filter_option_id ON company_filter_options(filter_option_id);

COMMENT ON TABLE company_filter_options IS '업체-필터 옵션 다대다 조인 테이블';
COMMENT ON COLUMN company_filter_options.company_id IS '업체 ID (FK to companies)';
COMMENT ON COLUMN company_filter_options.filter_option_id IS '필터 옵션 ID (FK to filter_options)';

-- =============================================================================
-- 2. 작업 평수 필터 카테고리 추가
-- =============================================================================
INSERT INTO filter_categories (code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order)
VALUES
('project_size_range', '작업 평수', '프로젝트 시공 가능 면적', 'COMPANY', 'MULTI_SELECT', false, 1, 6)
ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- 3. 작업 평수 필터 옵션
-- =============================================================================
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'project_size_range'), 'all-sizes', '전체 가능', 0, NULL, 1),
((SELECT id FROM filter_categories WHERE code = 'project_size_range'), 'under-10pyeong', '10평 이하', 0, NULL, 2),
((SELECT id FROM filter_categories WHERE code = 'project_size_range'), '10-30pyeong', '10평 ~ 30평', 0, NULL, 3),
((SELECT id FROM filter_categories WHERE code = 'project_size_range'), '30-50pyeong', '30평 ~ 50평', 0, NULL, 4),
((SELECT id FROM filter_categories WHERE code = 'project_size_range'), '50-100pyeong', '50평 ~ 100평', 0, NULL, 5),
((SELECT id FROM filter_categories WHERE code = 'project_size_range'), 'over-100pyeong', '100평 이상', 0, NULL, 6),
((SELECT id FROM filter_categories WHERE code = 'project_size_range'), 'no-limit', '평수 무관', 0, NULL, 7)
ON CONFLICT (category_id, code) DO NOTHING;

-- =============================================================================
-- 4. specialty 카테고리에 더 많은 업종 추가
-- =============================================================================

-- 인테리어/시공 관련
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'interior-design', '인테리어', 0, NULL, 8),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'home-styling', '홈스타일링', 0, NULL, 9),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'furniture', '가구/목공', 0, NULL, 10),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'flooring', '바닥재 시공', 0, NULL, 11),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'wallpaper', '도배/벽지', 0, NULL, 12),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'painting', '페인트/도장', 0, NULL, 13),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'lighting', '조명 설치', 0, NULL, 14),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'window', '창호/유리', 0, NULL, 15)
ON CONFLICT (category_id, code) DO NOTHING;

-- IT/디지털 마케팅 관련
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'marketing', '마케팅/광고', 0, NULL, 20),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'web-dev', '홈페이지 제작', 0, NULL, 21),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'seo', 'SEO/검색최적화', 0, NULL, 22),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'sns-marketing', 'SNS 마케팅', 0, NULL, 23),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'video-production', '영상 제작', 0, NULL, 24),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'photography', '사진 촬영', 0, NULL, 25),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'graphic-design', '그래픽 디자인', 0, NULL, 26)
ON CONFLICT (category_id, code) DO NOTHING;

-- 설비/유지보수 관련
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'cleaning', '청소/방역', 0, NULL, 30),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'air-conditioner', '에어컨 설치/수리', 0, NULL, 31),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'internet', '인터넷/통신', 0, NULL, 32),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'electrical', '전기 공사/수리', 0, NULL, 33),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'plumbing', '배관/수도', 0, NULL, 34),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'waterproofing', '방수 공사', 0, NULL, 35),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'locksmith', '자물쇠/보안', 0, NULL, 36),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'moving', '이사/운송', 0, NULL, 37),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'storage', '창고/보관', 0, NULL, 38)
ON CONFLICT (category_id, code) DO NOTHING;

-- 의료/병원 관련 (기존 department와 별도)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'hospital-interior', '병원 인테리어', 0, NULL, 40),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'medical-equipment', '의료 장비 설치', 0, NULL, 41),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'sterilization', '멸균/소독', 0, NULL, 42)
ON CONFLICT (category_id, code) DO NOTHING;

-- 기타 전문 서비스
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'consulting', '컨설팅', 0, NULL, 50),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'accounting', '회계/세무', 0, NULL, 51),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'legal', '법무/법률', 0, NULL, 52),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'insurance', '보험', 0, NULL, 53),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'real-estate', '부동산', 0, NULL, 54)
ON CONFLICT (category_id, code) DO NOTHING;

-- =============================================================================
-- 완료 메시지
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'V24 마이그레이션 완료: 업체-필터 관계 및 필터 옵션 확장';
    RAISE NOTICE '- 생성된 테이블: 1개 (company_filter_options)';
    RAISE NOTICE '- 추가된 필터 카테고리: 1개 (작업 평수)';
    RAISE NOTICE '- 추가된 specialty 옵션: 30+ 개';
    RAISE NOTICE '- 추가된 project_size_range 옵션: 7개';
    RAISE NOTICE '=============================================================================';
END $$;
