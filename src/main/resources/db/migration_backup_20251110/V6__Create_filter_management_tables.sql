-- =============================================================================
-- V6: 필터 관리 시스템 개선 (JSONB → 정규화 테이블)
-- =============================================================================
-- 설명: 관리자가 개별 필터 옵션을 관리할 수 있도록 정규화된 테이블 구조 생성
-- 작성일: 2025-01-10
-- 내용:
--   - filter_categories: 필터 카테고리 정의 (지역, 진료과, 전문영역 등)
--   - filter_options: 필터 옵션 (개별 CRUD 가능, 계층 구조 지원)
--   - filter_option_relations: 옵션 간 관계 (선택사항)
-- =============================================================================

-- =============================================================================
-- 1. 필터 카테고리 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS filter_categories (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 카테고리 기본 정보
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    -- 적용 대상
    entity_type VARCHAR(100) NOT NULL,           -- COMPANY, HOSPITAL, SERVICE

    -- 필터 타입
    filter_type VARCHAR(50) NOT NULL,            -- SINGLE_SELECT, MULTI_SELECT, HIERARCHICAL

    -- 계층 구조 설정
    supports_hierarchy BOOLEAN NOT NULL DEFAULT false,
    max_depth INTEGER NOT NULL DEFAULT 1,        -- 최대 깊이 (1=flat, 2=2단계, 3=3단계 등)

    -- UI 설정
    display_order INTEGER NOT NULL DEFAULT 0,
    icon VARCHAR(100),

    -- 상태
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_required BOOLEAN NOT NULL DEFAULT false,  -- 필수 필터 여부

    -- 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 인덱스
CREATE INDEX idx_filter_categories_code ON filter_categories(code);
CREATE INDEX idx_filter_categories_entity_type ON filter_categories(entity_type);
CREATE INDEX idx_filter_categories_active ON filter_categories(is_active) WHERE is_active = true AND is_deleted = false;
CREATE INDEX idx_filter_categories_display_order ON filter_categories(display_order);

-- =============================================================================
-- 2. 필터 옵션 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS filter_options (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- 카테고리 참조
    category_id BIGINT NOT NULL REFERENCES filter_categories(id) ON DELETE CASCADE,

    -- 옵션 기본 정보
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100),
    description TEXT,

    -- 계층 구조 (Adjacency List Model)
    parent_id BIGINT REFERENCES filter_options(id) ON DELETE CASCADE,
    depth INTEGER NOT NULL DEFAULT 0,            -- 깊이 (0=최상위, 1=2단계, 2=3단계)
    path VARCHAR(500),                           -- 경로 (예: /seoul/gangnam-gu/yeoksam-dong)

    -- 추가 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB,
    /* 예시:
    {
        "latitude": 37.4979,
        "longitude": 127.0276,
        "postal_code_prefix": "06",
        "region_code": "11680"
    }
    */

    -- UI 설정
    display_order INTEGER NOT NULL DEFAULT 0,
    icon VARCHAR(100),
    color VARCHAR(20),

    -- 상태
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_default BOOLEAN NOT NULL DEFAULT false,   -- 기본값 여부

    -- 통계
    usage_count INTEGER NOT NULL DEFAULT 0,      -- 사용 횟수 (캐싱용)

    -- 기본 필드
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft Delete
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- 고유 제약조건
    CONSTRAINT uk_filter_option_category_code UNIQUE(category_id, code),
    CONSTRAINT uk_filter_option_category_parent_name UNIQUE(category_id, parent_id, name)
);

-- 인덱스
CREATE INDEX idx_filter_options_uuid ON filter_options(uuid);
CREATE INDEX idx_filter_options_category_id ON filter_options(category_id);
CREATE INDEX idx_filter_options_parent_id ON filter_options(parent_id);
CREATE INDEX idx_filter_options_code ON filter_options(code);
CREATE INDEX idx_filter_options_depth ON filter_options(depth);
CREATE INDEX idx_filter_options_path ON filter_options(path);
CREATE INDEX idx_filter_options_active ON filter_options(is_active) WHERE is_active = true AND is_deleted = false;
CREATE INDEX idx_filter_options_metadata ON filter_options USING GIN(metadata);

-- 복합 인덱스 (자주 사용되는 쿼리 최적화)
CREATE INDEX idx_filter_options_category_active ON filter_options(category_id, is_active) WHERE is_deleted = false;
CREATE INDEX idx_filter_options_category_parent ON filter_options(category_id, parent_id) WHERE is_deleted = false;
CREATE INDEX idx_filter_options_category_depth ON filter_options(category_id, depth) WHERE is_deleted = false;

-- =============================================================================
-- 3. 필터 옵션 간 관계 테이블 (선택사항)
-- =============================================================================
CREATE TABLE IF NOT EXISTS filter_option_relations (
    id BIGSERIAL PRIMARY KEY,

    -- 관계 정보
    source_option_id BIGINT NOT NULL REFERENCES filter_options(id) ON DELETE CASCADE,
    target_option_id BIGINT NOT NULL REFERENCES filter_options(id) ON DELETE CASCADE,
    relation_type VARCHAR(50) NOT NULL,          -- RELATED_TO, INCLUDES, EQUIVALENT

    -- 관계 메타데이터
    metadata JSONB DEFAULT '{}'::JSONB,

    -- 기본 필드
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 고유 제약조건
    CONSTRAINT uk_filter_relation UNIQUE(source_option_id, target_option_id, relation_type)
);

-- 인덱스
CREATE INDEX idx_filter_option_relations_source ON filter_option_relations(source_option_id);
CREATE INDEX idx_filter_option_relations_target ON filter_option_relations(target_option_id);
CREATE INDEX idx_filter_option_relations_type ON filter_option_relations(relation_type);

-- =============================================================================
-- 트리거: updated_at 자동 업데이트
-- =============================================================================
CREATE TRIGGER update_filter_categories_updated_at
    BEFORE UPDATE ON filter_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_filter_options_updated_at
    BEFORE UPDATE ON filter_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- 트리거: path 자동 생성/갱신
-- =============================================================================
CREATE OR REPLACE FUNCTION update_filter_option_path()
RETURNS TRIGGER AS $$
DECLARE
    parent_path VARCHAR(500);
    parent_depth INTEGER;
BEGIN
    IF NEW.parent_id IS NULL THEN
        -- 최상위 옵션
        NEW.path := '/' || NEW.code;
        NEW.depth := 0;
    ELSE
        -- 하위 옵션
        SELECT path, depth INTO parent_path, parent_depth
        FROM filter_options
        WHERE id = NEW.parent_id;

        IF parent_path IS NULL THEN
            RAISE EXCEPTION 'Parent option not found: %', NEW.parent_id;
        END IF;

        NEW.path := parent_path || '/' || NEW.code;
        NEW.depth := parent_depth + 1;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_filter_option_path
    BEFORE INSERT OR UPDATE OF parent_id, code ON filter_options
    FOR EACH ROW EXECUTE FUNCTION update_filter_option_path();

-- =============================================================================
-- 테이블 코멘트
-- =============================================================================
COMMENT ON TABLE filter_categories IS '필터 카테고리 정의 테이블 (지역, 진료과, 전문영역 등)';
COMMENT ON TABLE filter_options IS '필터 옵션 테이블 (개별 CRUD 가능, 계층 구조 지원)';
COMMENT ON TABLE filter_option_relations IS '필터 옵션 간 관계 테이블 (다대다 관계 등)';

-- 주요 컬럼 코멘트
COMMENT ON COLUMN filter_categories.code IS '필터 카테고리 코드 (고유값: region, department, specialty 등)';
COMMENT ON COLUMN filter_categories.name IS '필터 카테고리명 (사용자에게 표시)';
COMMENT ON COLUMN filter_categories.entity_type IS '적용 대상 엔티티 (COMPANY, HOSPITAL, SERVICE 등)';
COMMENT ON COLUMN filter_categories.filter_type IS '필터 선택 방식 (SINGLE_SELECT, MULTI_SELECT, HIERARCHICAL)';
COMMENT ON COLUMN filter_categories.supports_hierarchy IS '계층 구조 지원 여부';
COMMENT ON COLUMN filter_categories.max_depth IS '최대 계층 깊이 (1=flat, 2=2단계, 3=3단계)';

COMMENT ON COLUMN filter_options.category_id IS '필터 카테고리 FK';
COMMENT ON COLUMN filter_options.code IS '옵션 코드 (카테고리 내 고유값)';
COMMENT ON COLUMN filter_options.name IS '옵션명 (사용자에게 표시)';
COMMENT ON COLUMN filter_options.parent_id IS '부모 옵션 ID (계층 구조용, NULL=최상위)';
COMMENT ON COLUMN filter_options.depth IS '계층 깊이 (0=최상위, 1=2단계, 2=3단계)';
COMMENT ON COLUMN filter_options.path IS '전체 경로 (검색 최적화용, 예: /seoul/gangnam-gu/yeoksam-dong)';
COMMENT ON COLUMN filter_options.metadata IS '확장 메타데이터 (좌표, 지역코드 등 JSONB)';
COMMENT ON COLUMN filter_options.usage_count IS '사용 횟수 (인기 옵션 파악용)';

COMMENT ON COLUMN filter_option_relations.relation_type IS '관계 유형 (RELATED_TO, INCLUDES, EQUIVALENT)';

-- =============================================================================
-- 초기 데이터: 필터 카테고리
-- =============================================================================

-- 지역 필터 카테고리
INSERT INTO filter_categories (code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order)
VALUES
('region', '지역', '서비스 제공 지역', 'COMPANY', 'MULTI_SELECT', true, 3, 1);

-- 진료과 필터 카테고리
INSERT INTO filter_categories (code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order)
VALUES
('department', '진료과', '병원 진료 과목', 'COMPANY', 'MULTI_SELECT', true, 2, 2);

-- 전문영역 필터 카테고리
INSERT INTO filter_categories (code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order)
VALUES
('specialty', '전문영역', '인테리어 전문 분야', 'COMPANY', 'MULTI_SELECT', false, 1, 3);

-- 가격대 필터 카테고리
INSERT INTO filter_categories (code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order)
VALUES
('price_range', '가격대', '프로젝트 예산 범위', 'COMPANY', 'SINGLE_SELECT', false, 1, 4);

-- 평점 필터 카테고리
INSERT INTO filter_categories (code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order)
VALUES
('rating', '평점', '업체 평점', 'COMPANY', 'SINGLE_SELECT', false, 1, 5);

-- =============================================================================
-- 초기 데이터: 지역 필터 옵션 (계층 구조)
-- =============================================================================

-- 서울 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'seoul', '서울', 0, NULL, 1);

-- 서울 > 강남구 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'gangnam-gu', '강남구', 1,
 (SELECT id FROM filter_options WHERE code = 'seoul'), 1);

-- 서울 > 강남구 > 역삼동 (3단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, metadata, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'yeoksam-dong', '역삼동', 2,
 (SELECT id FROM filter_options WHERE code = 'gangnam-gu'),
 '{"latitude": 37.4979, "longitude": 127.0276, "postal_code_prefix": "06"}'::JSONB, 1);

-- 서울 > 강남구 > 삼성동 (3단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, metadata, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'samsung-dong', '삼성동', 2,
 (SELECT id FROM filter_options WHERE code = 'gangnam-gu'),
 '{"latitude": 37.5085, "longitude": 127.0632, "postal_code_prefix": "06"}'::JSONB, 2);

-- 서울 > 서초구 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'seocho-gu', '서초구', 1,
 (SELECT id FROM filter_options WHERE code = 'seoul'), 2);

-- 서울 > 용산구 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'yongsan-gu', '용산구', 1,
 (SELECT id FROM filter_options WHERE code = 'seoul'), 3);

-- 경기 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'gyeonggi', '경기', 0, NULL, 2);

-- 경기 > 성남시 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'seongnam-si', '성남시', 1,
 (SELECT id FROM filter_options WHERE code = 'gyeonggi'), 1);

-- 경기 > 수원시 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'suwon-si', '수원시', 1,
 (SELECT id FROM filter_options WHERE code = 'gyeonggi'), 2);

-- 인천 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'incheon', '인천', 0, NULL, 3);

-- 대전 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'daejeon', '대전', 0, NULL, 4);

-- 대구 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'daegu', '대구', 0, NULL, 5);

-- 부산 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'busan', '부산', 0, NULL, 6);

-- 광주 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'gwangju', '광주', 0, NULL, 7);

-- 울산 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'region'), 'ulsan', '울산', 0, NULL, 8);

-- =============================================================================
-- 초기 데이터: 진료과 필터 옵션 (계층 구조)
-- =============================================================================

-- 내과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'internal-medicine', '내과', 0, NULL, 1);

-- 내과 > 소화기내과 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'gastroenterology', '소화기내과', 1,
 (SELECT id FROM filter_options WHERE category_id = (SELECT id FROM filter_categories WHERE code = 'department') AND code = 'internal-medicine'), 1);

-- 내과 > 순환기내과 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'cardiology', '순환기내과', 1,
 (SELECT id FROM filter_options WHERE category_id = (SELECT id FROM filter_categories WHERE code = 'department') AND code = 'internal-medicine'), 2);

-- 내과 > 호흡기내과 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'pulmonology', '호흡기내과', 1,
 (SELECT id FROM filter_options WHERE category_id = (SELECT id FROM filter_categories WHERE code = 'department') AND code = 'internal-medicine'), 3);

-- 외과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'surgery', '외과', 0, NULL, 2);

-- 외과 > 일반외과 (2단계)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'general-surgery', '일반외과', 1,
 (SELECT id FROM filter_options WHERE category_id = (SELECT id FROM filter_categories WHERE code = 'department') AND code = 'surgery'), 1);

-- 정형외과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'orthopedics', '정형외과', 0, NULL, 3);

-- 성형외과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'plastic-surgery', '성형외과', 0, NULL, 4);

-- 피부과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'dermatology', '피부과', 0, NULL, 5);

-- 안과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'ophthalmology', '안과', 0, NULL, 6);

-- 이비인후과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'ent', '이비인후과', 0, NULL, 7);

-- 치과 (최상위)
INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'department'), 'dentistry', '치과', 0, NULL, 8);

-- =============================================================================
-- 초기 데이터: 전문영역 필터 옵션 (평면 구조)
-- =============================================================================

INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'residential', '주거공간', 0, NULL, 1),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'commercial', '상업공간', 0, NULL, 2),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'office', '사무공간', 0, NULL, 3),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'medical', '의료공간', 0, NULL, 4),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'remodeling', '리모델링', 0, NULL, 5),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'new-construction', '신축', 0, NULL, 6),
((SELECT id FROM filter_categories WHERE code = 'specialty'), 'extension', '증축', 0, NULL, 7);

-- =============================================================================
-- 초기 데이터: 가격대 필터 옵션 (평면 구조)
-- =============================================================================

INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'price_range'), 'under-5m', '500만원 이하', 0, NULL, 1),
((SELECT id FROM filter_categories WHERE code = 'price_range'), '5m-10m', '500만원 ~ 1,000만원', 0, NULL, 2),
((SELECT id FROM filter_categories WHERE code = 'price_range'), '10m-30m', '1,000만원 ~ 3,000만원', 0, NULL, 3),
((SELECT id FROM filter_categories WHERE code = 'price_range'), '30m-50m', '3,000만원 ~ 5,000만원', 0, NULL, 4),
((SELECT id FROM filter_categories WHERE code = 'price_range'), 'over-50m', '5,000만원 이상', 0, NULL, 5);

-- =============================================================================
-- 초기 데이터: 평점 필터 옵션 (평면 구조)
-- =============================================================================

INSERT INTO filter_options (category_id, code, name, depth, parent_id, display_order)
VALUES
((SELECT id FROM filter_categories WHERE code = 'rating'), 'rating-5', '5점', 0, NULL, 1),
((SELECT id FROM filter_categories WHERE code = 'rating'), 'rating-4-plus', '4점 이상', 0, NULL, 2),
((SELECT id FROM filter_categories WHERE code = 'rating'), 'rating-3-plus', '3점 이상', 0, NULL, 3);

-- =============================================================================
-- 뷰: 계층 구조 필터 옵션 (재귀 CTE)
-- =============================================================================
CREATE OR REPLACE VIEW v_filter_options_tree AS
WITH RECURSIVE option_tree AS (
    -- 최상위 노드
    SELECT
        fo.id,
        fo.uuid,
        fo.category_id,
        fc.code AS category_code,
        fc.name AS category_name,
        fo.code,
        fo.name,
        fo.parent_id,
        fo.depth,
        fo.path,
        fo.metadata,
        fo.display_order,
        fo.is_active,
        ARRAY[fo.id] AS id_path,
        fo.name::TEXT AS full_name
    FROM filter_options fo
    INNER JOIN filter_categories fc ON fo.category_id = fc.id
    WHERE fo.parent_id IS NULL
      AND fo.is_active = true
      AND fo.is_deleted = false

    UNION ALL

    -- 하위 노드
    SELECT
        fo.id,
        fo.uuid,
        fo.category_id,
        ot.category_code,
        ot.category_name,
        fo.code,
        fo.name,
        fo.parent_id,
        fo.depth,
        fo.path,
        fo.metadata,
        fo.display_order,
        fo.is_active,
        ot.id_path || fo.id,
        ot.full_name || ' > ' || fo.name
    FROM filter_options fo
    INNER JOIN option_tree ot ON fo.parent_id = ot.id
    WHERE fo.is_active = true
      AND fo.is_deleted = false
)
SELECT * FROM option_tree
ORDER BY category_id, path;

COMMENT ON VIEW v_filter_options_tree IS '계층 구조 필터 옵션 뷰 (재귀 CTE)';

-- =============================================================================
-- 완료 메시지
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'V6 마이그레이션 완료: 필터 관리 시스템 개선';
    RAISE NOTICE '- 생성된 테이블: 3개 (filter_categories, filter_options, filter_option_relations)';
    RAISE NOTICE '- 생성된 뷰: 1개 (v_filter_options_tree)';
    RAISE NOTICE '- 생성된 트리거: 3개';
    RAISE NOTICE '- 초기 카테고리: 5개 (지역, 진료과, 전문영역, 가격대, 평점)';
    RAISE NOTICE '- 초기 옵션: 50+ 개';
    RAISE NOTICE '=============================================================================';
END $$;