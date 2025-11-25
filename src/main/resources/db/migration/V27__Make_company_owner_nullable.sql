-- 관리자가 소유자 없이 업체를 등록할 수 있도록 owner_id를 nullable로 변경

ALTER TABLE companies
ALTER COLUMN owner_id DROP NOT NULL;

COMMENT ON COLUMN companies.owner_id IS '업체 소유자 ID (NULL인 경우 관리자가 생성한 업체)';
