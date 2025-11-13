-- V17: IP 주소 컬럼 타입 변경 (inet → varchar)
-- quick_consultations 테이블의 IP 주소 컬럼을 inet에서 varchar로 변경

-- consent_ip_address 컬럼 타입 변경
ALTER TABLE quick_consultations
    ALTER COLUMN consent_ip_address TYPE VARCHAR(50) USING consent_ip_address::text;

-- ip_address 컬럼 타입 변경
ALTER TABLE quick_consultations
    ALTER COLUMN ip_address TYPE VARCHAR(50) USING ip_address::text;

-- 컬럼 코멘트 유지
COMMENT ON COLUMN quick_consultations.consent_ip_address IS '동의 시 IP 주소 (법적 증빙)';
COMMENT ON COLUMN quick_consultations.ip_address IS '요청 IP 주소';
