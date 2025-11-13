-- V16: Add password and terms_of_service_consent columns to quick_consultations table
-- Author: Claude Code
-- Date: 2025-11-13
-- Description: Add password field for non-member verification and terms of service consent

-- Add password column for non-member consultation verification (4-digit plain text)
ALTER TABLE quick_consultations
    ADD COLUMN IF NOT EXISTS password VARCHAR(4);

-- Add terms of service consent fields
ALTER TABLE quick_consultations
    ADD COLUMN IF NOT EXISTS terms_of_service_consent BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS terms_of_service_consent_at TIMESTAMP WITH TIME ZONE;

-- Add comment for password column
COMMENT ON COLUMN quick_consultations.password IS '비회원 상담 조회용 4자리 비밀번호 (평문 저장)';
COMMENT ON COLUMN quick_consultations.terms_of_service_consent IS '이용약관 동의 여부';
COMMENT ON COLUMN quick_consultations.terms_of_service_consent_at IS '이용약관 동의 시각';
