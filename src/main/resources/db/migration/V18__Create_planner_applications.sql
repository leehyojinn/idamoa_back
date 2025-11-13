-- 플래너 신청서 테이블
CREATE TABLE planner_applications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id BIGINT REFERENCES users(id),

    -- 신청 정보
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    consultation_method VARCHAR(20) NOT NULL, -- VISIT, PHONE, SNS
    request_types TEXT[] NOT NULL, -- 다중 선택: FULL_CONSULTING, NEW_OPENING, REMODELING, OPERATION_CONSULTING, LEGAL_INQUIRY

    -- 신청자 정보
    applicant_name VARCHAR(100) NOT NULL,
    applicant_phone VARCHAR(20) NOT NULL,
    applicant_email VARCHAR(100) NOT NULL,

    -- 사업장 정보
    business_name VARCHAR(200),
    business_address TEXT,
    business_area_size VARCHAR(50),
    business_type VARCHAR(100),

    -- 첨부파일 (files 테이블 참조)
    attachment_file_ids BIGINT[], -- files 테이블의 id 배열

    -- 상태 관리
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, REJECTED
    admin_response TEXT,
    admin_memo TEXT,
    assigned_admin_id BIGINT REFERENCES users(id),

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES users(id),

    CONSTRAINT uk_planner_applications_uuid UNIQUE (uuid)
);

-- 인덱스 생성
CREATE INDEX idx_planner_applications_user_id ON planner_applications(user_id);
CREATE INDEX idx_planner_applications_status ON planner_applications(status);
CREATE INDEX idx_planner_applications_created_at ON planner_applications(created_at DESC);
CREATE INDEX idx_planner_applications_is_deleted ON planner_applications(is_deleted);

-- 플래너 신청서 희망 일정 테이블
CREATE TABLE planner_preferred_dates (
    id BIGSERIAL PRIMARY KEY,
    planner_application_id BIGINT NOT NULL REFERENCES planner_applications(id) ON DELETE CASCADE,
    priority INTEGER NOT NULL, -- 1, 2, 3 (1순위, 2순위, 3순위)
    preferred_date DATE NOT NULL,
    preferred_time VARCHAR(50) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_planner_preferred_dates_application_priority
        UNIQUE (planner_application_id, priority)
);

-- 인덱스 생성
CREATE INDEX idx_planner_preferred_dates_application_id
    ON planner_preferred_dates(planner_application_id);

-- 코멘트 추가
COMMENT ON TABLE planner_applications IS '플래너 신청서';
COMMENT ON TABLE planner_preferred_dates IS '플래너 신청서 희망 일정';
COMMENT ON COLUMN planner_applications.consultation_method IS '상담 방법: VISIT(방문), PHONE(전화), SNS(SNS)';
COMMENT ON COLUMN planner_applications.request_types IS '요청 내용(다중선택): FULL_CONSULTING(종합컨설팅), NEW_OPENING(신규창업), REMODELING(리모델링), OPERATION_CONSULTING(운영컨설팅), LEGAL_INQUIRY(법률자문)';
COMMENT ON COLUMN planner_applications.status IS '상태: PENDING(대기중), IN_PROGRESS(진행중), COMPLETED(완료), REJECTED(거절)';
COMMENT ON COLUMN planner_applications.attachment_file_ids IS 'files 테이블 id 배열 (전체 100MB 제한)';
