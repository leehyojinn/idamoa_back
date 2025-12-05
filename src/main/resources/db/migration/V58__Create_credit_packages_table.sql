-- =============================================
-- V58: 크레딧 충전 패키지 테이블 생성
-- =============================================
-- 단위 금액 기반 패키지 (4개)
-- 수량은 사용자가 충전 시 직접 지정 (무제한)

-- 1. credit_packages 테이블 생성
-- =============================================
CREATE TABLE IF NOT EXISTS credit_packages (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,

    -- 패키지 식별 정보
    code VARCHAR(50) NOT NULL UNIQUE,           -- 예: KRW_10000, KRW_30000
    display_name VARCHAR(100) NOT NULL,         -- 예: 1만원권, 3만원권

    -- 금액 정보
    unit_amount INTEGER NOT NULL,               -- 단위 금액 (10000, 30000, 50000, 100000)

    -- 보너스 정보 (3만원 이상만 적용)
    bonus_rate NUMERIC(5,2) NOT NULL DEFAULT 0, -- 보너스율 (%)
    max_bonus INTEGER,                          -- 최대 보너스 한도 (null = 무제한)

    -- 관리 정보
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    description VARCHAR(500),

    -- 공통 필드
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 인덱스 생성
-- =============================================
CREATE INDEX idx_credit_packages_unit_amount ON credit_packages(unit_amount);
CREATE INDEX idx_credit_packages_is_active ON credit_packages(is_active);
CREATE INDEX idx_credit_packages_display_order ON credit_packages(display_order);
CREATE INDEX idx_credit_packages_code ON credit_packages(code);

-- 3. 코멘트 추가
-- =============================================
COMMENT ON TABLE credit_packages IS '크레딧 충전 패키지 (단위 금액 기준, 수량은 충전 시 지정)';
COMMENT ON COLUMN credit_packages.code IS '패키지 코드 (KRW_10000, KRW_30000, KRW_50000, KRW_100000)';
COMMENT ON COLUMN credit_packages.display_name IS '화면 표시명 (1만원권, 3만원권, 5만원권, 10만원권)';
COMMENT ON COLUMN credit_packages.unit_amount IS '단위 금액 (10000, 30000, 50000, 100000)';
COMMENT ON COLUMN credit_packages.bonus_rate IS '보너스율 (%, 3만원 이상만 적용)';
COMMENT ON COLUMN credit_packages.max_bonus IS '최대 보너스 한도 (NULL이면 무제한)';
COMMENT ON COLUMN credit_packages.is_active IS '활성화 여부 (비활성 시 충전 불가)';
COMMENT ON COLUMN credit_packages.display_order IS '표시 순서';

-- 4. 기본 패키지 데이터 삽입 (4개)
-- =============================================
-- 패키지 코드: KRW_{단위금액}
-- 수량은 패키지에 포함되지 않음 - 충전 시 사용자가 지정

-- 1만원권 (보너스 없음 - 3만원 미만)
INSERT INTO credit_packages (code, display_name, unit_amount, bonus_rate, max_bonus, display_order, description)
VALUES ('KRW_10000', '1만원권', 10000, 0, NULL, 1, '1만원 단위 충전권 (보너스 없음)');

-- 3만원권 (보너스 5%)
INSERT INTO credit_packages (code, display_name, unit_amount, bonus_rate, max_bonus, display_order, description)
VALUES ('KRW_30000', '3만원권', 30000, 5.00, NULL, 2, '3만원 단위 충전권 (5% 보너스)');

-- 5만원권 (보너스 7%)
INSERT INTO credit_packages (code, display_name, unit_amount, bonus_rate, max_bonus, display_order, description)
VALUES ('KRW_50000', '5만원권', 50000, 7.00, NULL, 3, '5만원 단위 충전권 (7% 보너스)');

-- 10만원권 (보너스 10%, 최대 50,000원)
INSERT INTO credit_packages (code, display_name, unit_amount, bonus_rate, max_bonus, display_order, description)
VALUES ('KRW_100000', '10만원권', 100000, 10.00, 50000, 4, '10만원 단위 충전권 (10% 보너스, 최대 5만원)');
