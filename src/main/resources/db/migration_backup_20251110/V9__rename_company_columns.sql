-- V9: Company 테이블 컬럼명 변경 (Entity와 일치)

-- 1. user_id → owner_id 변경
ALTER TABLE companies RENAME COLUMN user_id TO owner_id;

-- 2. completed_count → completed_projects 변경
ALTER TABLE companies RENAME COLUMN completed_count TO completed_projects;

-- 3. 외래 키 제약조건 이름 변경
ALTER TABLE companies DROP CONSTRAINT IF EXISTS companies_user_id_fkey;
ALTER TABLE companies ADD CONSTRAINT companies_owner_id_fkey
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;

-- 4. 기존 인덱스 재생성
DROP INDEX IF EXISTS idx_companies_user_id;
CREATE INDEX IF NOT EXISTS idx_companies_owner_id ON companies(owner_id);

-- 5. 컬럼 코멘트 추가
COMMENT ON COLUMN companies.owner_id IS '업체 소유자 ID (User FK)';
COMMENT ON COLUMN companies.completed_projects IS '완료된 프로젝트 수';
