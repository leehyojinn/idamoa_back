-- =====================================================================
-- V77: 필터 옵션 JSONB 배열로 마이그레이션
--
-- 목적: 조인 테이블(company_filter_options, board_filter_options, portfolio_filter_options)을
--       JSONB 배열로 변경하여 INSERT/DELETE 부하를 줄이고 성능 개선
-- =====================================================================

-- 0. JSONB 배열에서 ID 포함 여부 확인하는 함수 생성
-- JPA Criteria API에서 사용하기 위한 헬퍼 함수
CREATE OR REPLACE FUNCTION jsonb_contains_id(jsonb_array JSONB, id_value BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    IF jsonb_array IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN jsonb_array @> to_jsonb(id_value);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 1. 각 테이블에 JSONB 컬럼 추가
ALTER TABLE companies ADD COLUMN IF NOT EXISTS filter_option_ids JSONB DEFAULT '[]'::jsonb;
ALTER TABLE boards ADD COLUMN IF NOT EXISTS filter_option_ids JSONB DEFAULT '[]'::jsonb;
ALTER TABLE company_portfolios ADD COLUMN IF NOT EXISTS filter_option_ids JSONB DEFAULT '[]'::jsonb;

-- 2. 기존 데이터 마이그레이션

-- 2-1. Company 필터 옵션 마이그레이션
UPDATE companies c SET filter_option_ids = (
    SELECT COALESCE(jsonb_agg(cfo.filter_option_id ORDER BY cfo.filter_option_id), '[]'::jsonb)
    FROM company_filter_options cfo
    WHERE cfo.company_id = c.id
)
WHERE EXISTS (SELECT 1 FROM company_filter_options cfo WHERE cfo.company_id = c.id);

-- 2-2. Board 필터 옵션 마이그레이션
UPDATE boards b SET filter_option_ids = (
    SELECT COALESCE(jsonb_agg(bfo.filter_option_id ORDER BY bfo.filter_option_id), '[]'::jsonb)
    FROM board_filter_options bfo
    WHERE bfo.board_id = b.id
)
WHERE EXISTS (SELECT 1 FROM board_filter_options bfo WHERE bfo.board_id = b.id);

-- 2-3. Portfolio 필터 옵션 마이그레이션 (portfolio_filter_options 테이블이 있는 경우)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'portfolio_filter_options') THEN
        EXECUTE '
            UPDATE company_portfolios p SET filter_option_ids = (
                SELECT COALESCE(jsonb_agg(pfo.filter_option_id ORDER BY pfo.filter_option_id), ''[]''::jsonb)
                FROM portfolio_filter_options pfo
                WHERE pfo.portfolio_id = p.id
            )
            WHERE EXISTS (SELECT 1 FROM portfolio_filter_options pfo WHERE pfo.portfolio_id = p.id)
        ';
    END IF;
END $$;

-- 3. GIN 인덱스 추가 (JSONB 배열 검색 최적화)
CREATE INDEX IF NOT EXISTS idx_companies_filter_option_ids ON companies USING GIN (filter_option_ids jsonb_path_ops);
CREATE INDEX IF NOT EXISTS idx_boards_filter_option_ids ON boards USING GIN (filter_option_ids jsonb_path_ops);
CREATE INDEX IF NOT EXISTS idx_portfolios_filter_option_ids ON company_portfolios USING GIN (filter_option_ids jsonb_path_ops);

-- 4. 조인 테이블은 유지 (롤백 대비)
-- 안정화 후 별도 마이그레이션에서 삭제 예정
-- DROP TABLE company_filter_options;
-- DROP TABLE board_filter_options;
-- DROP TABLE portfolio_filter_options;

-- 5. 마이그레이션 결과 로그
DO $$
DECLARE
    company_count INTEGER;
    board_count INTEGER;
    portfolio_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO company_count FROM companies WHERE filter_option_ids != '[]'::jsonb;
    SELECT COUNT(*) INTO board_count FROM boards WHERE filter_option_ids != '[]'::jsonb;
    SELECT COUNT(*) INTO portfolio_count FROM company_portfolios WHERE filter_option_ids != '[]'::jsonb;

    RAISE NOTICE 'Filter options migration completed:';
    RAISE NOTICE '  - Companies with filters: %', company_count;
    RAISE NOTICE '  - Boards with filters: %', board_count;
    RAISE NOTICE '  - Portfolios with filters: %', portfolio_count;
END $$;
