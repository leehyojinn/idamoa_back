--
-- PostgreSQL database dump
--
-- Modified version: ENUM types converted to VARCHAR with CHECK constraints
-- This migration converts all PostgreSQL ENUM types to VARCHAR(50) with CHECK constraints
-- for better flexibility and compatibility with JPA/Hibernate.
--
-- Converted ENUM types (10):
--   - ad_status: DRAFT, PENDING, APPROVED, ACTIVE, PAUSED, COMPLETED, REJECTED
--   - ad_type: LISTING, AI_RECOMMENDATION, BANNER, POPUP
--   - board_type: NOTICE, EVENT, FAQ, GALLERY, DOCUMENT
--   - damoa_pick_type: SPONSORED, OPERATED, PARTNER
--   - file_type: IMAGE, VIDEO, DOCUMENT, AUDIO, OTHER
--   - notification_channel: EMAIL, SMS, PUSH, KAKAO, IN_APP
--   - payment_method: CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, PHONE, KAKAO_PAY, NAVER_PAY, TOSS, CREDIT
--   - payment_status: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED, PARTIAL_REFUNDED
--   - user_role: USER, COMPANY, DESIGNER, ADMIN, SUPER_ADMIN
--   - user_status: PENDING, ACTIVE, INACTIVE, SUSPENDED, DELETED
--


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: calculate_secondary_score(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.calculate_secondary_score(p_company_id bigint) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_company RECORD;
    v_score DECIMAL(12,2);
BEGIN
    SELECT
        avg_rating,
        review_count,
        portfolio_count,
        completed_count
    INTO v_company
    FROM companies
    WHERE id = p_company_id;

    -- 평점(0-5) * 1000 + 리뷰수 * 10 + 포트폴리오 * 5 + 완료건수
    v_score :=
        (COALESCE(v_company.avg_rating, 0) * 1000) +
        (COALESCE(v_company.review_count, 0) * 10) +
        (COALESCE(v_company.portfolio_count, 0) * 5) +
        COALESCE(v_company.completed_count, 0);

    RETURN v_score;
END;
$$;


--
-- Name: FUNCTION calculate_secondary_score(p_company_id bigint); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON FUNCTION public.calculate_secondary_score(p_company_id bigint) IS '2차 정렬 점수 계산 (평점, 리뷰 기반)';


--
-- Name: calculate_total_value_30d(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.calculate_total_value_30d(p_campaign_id bigint) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_total_value DECIMAL(12,2) := 0;
BEGIN
    -- 모든 활성 결제의 30일 환산 가치 합계
    SELECT COALESCE(SUM(value_30d), 0)
    INTO v_total_value
    FROM ad_payments
    WHERE campaign_id = p_campaign_id
      AND status = 'ACTIVE';

    RETURN v_total_value;
END;
$$;


--
-- Name: FUNCTION calculate_total_value_30d(p_campaign_id bigint); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON FUNCTION public.calculate_total_value_30d(p_campaign_id bigint) IS '캠페인의 30일 환산 총 가치 계산';


--
-- Name: check_ip_whitelist(bigint, inet); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.check_ip_whitelist(p_user_id bigint, p_ip_address inet) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_allowed BOOLEAN := false;
BEGIN
    -- 전역 화이트리스트 체크
    SELECT EXISTS (
        SELECT 1
        FROM admin_ip_whitelist
        WHERE admin_user_id IS NULL
          AND is_active = true
          AND is_deleted = false
          AND rule_type = 'WHITELIST'
          AND (ip_address = p_ip_address
               OR (cidr_notation IS NOT NULL AND p_ip_address << cidr_notation::inet))
          AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
    ) INTO v_allowed;

    IF v_allowed THEN
        RETURN true;
    END IF;

    -- 사용자별 화이트리스트 체크
    SELECT EXISTS (
        SELECT 1
        FROM admin_ip_whitelist
        WHERE admin_user_id = p_user_id
          AND is_active = true
          AND is_deleted = false
          AND rule_type = 'WHITELIST'
          AND (ip_address = p_ip_address
               OR (cidr_notation IS NOT NULL AND p_ip_address << cidr_notation::inet))
          AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
    ) INTO v_allowed;

    RETURN v_allowed;
END;
$$;


--
-- Name: create_ad_campaign_with_payment(bigint, numeric, integer, public.ad_type); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.create_ad_campaign_with_payment(IN p_company_id bigint, IN p_payment_amount numeric, IN p_duration_days integer DEFAULT 30, IN p_ad_type VARCHAR(50) DEFAULT 'LISTING')
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_campaign_id BIGINT;
    v_daily_rate DECIMAL(12,2);
    v_value_30d DECIMAL(12,2);
BEGIN
    -- 일일 단가 및 30일 환산 가치 계산
    v_daily_rate := p_payment_amount / p_duration_days;
    v_value_30d := v_daily_rate * 30;

    -- 광고 캠페인 생성
    INSERT INTO ad_campaigns (
        company_id,
        ad_type,
        status,
        start_date,
        end_date,
        budget_amount,
        total_value_30d,
        priority_score,
        secondary_score
    ) VALUES (
        p_company_id,
        p_ad_type,
        'ACTIVE',
        CURRENT_DATE,
        CURRENT_DATE + (p_duration_days || ' days')::INTERVAL,
        p_payment_amount,
        v_value_30d,
        v_value_30d,
        calculate_secondary_score(p_company_id)
    ) RETURNING id INTO v_campaign_id;

    -- 초기 결제 이력 추가
    INSERT INTO ad_payments (
        campaign_id,
        payment_amount,
        apply_from_date,
        apply_to_date,
        apply_days,
        daily_rate,
        value_30d,
        payment_type,
        status
    ) VALUES (
        v_campaign_id,
        p_payment_amount,
        CURRENT_DATE,
        CURRENT_DATE + (p_duration_days - 1),
        p_duration_days,
        v_daily_rate,
        v_value_30d,
        'INITIAL',
        'ACTIVE'
    );

    COMMIT;
END;
$$;


--
-- Name: PROCEDURE create_ad_campaign_with_payment(IN p_company_id bigint, IN p_payment_amount numeric, IN p_duration_days integer, IN p_ad_type VARCHAR(50)); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON PROCEDURE public.create_ad_campaign_with_payment(IN p_company_id bigint, IN p_payment_amount numeric, IN p_duration_days integer, IN p_ad_type VARCHAR(50)) IS '광고 캠페인 생성 및 초기 결제 처리';


--
-- Name: get_admin_permissions(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_admin_permissions(p_user_id bigint) RETURNS TABLE(permission_type character varying, permission_value text, source character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
    WITH user_roles AS (
        -- 사용자의 역할 조회
        SELECT ar.role_name
        FROM admin_roles ar
        WHERE ar.user_id = p_user_id
          AND ar.is_active = true
    ),
    role_permissions AS (
        -- 역할에 따른 페이지 권한
        SELECT
            'PAGE' as permission_type,
            app.page_code as permission_value,
            'ROLE' as source
        FROM admin_role_page_access arpa
        JOIN admin_page_permissions app ON app.id = arpa.page_id
        JOIN user_roles ur ON ur.role_name = arpa.role_name
        WHERE arpa.is_active = true
          AND app.is_active = true
          AND app.is_deleted = false
    ),
    direct_permissions AS (
        -- 직접 할당된 권한 (추후 구현)
        SELECT
            'DIRECT' as permission_type,
            '' as permission_value,
            'USER' as source
        WHERE false -- 플레이스홀더
    )
    SELECT * FROM role_permissions
    UNION ALL
    SELECT * FROM direct_permissions
    ORDER BY permission_type, permission_value;
END;
$$;


--
-- Name: process_additional_payment(bigint, numeric, date); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.process_additional_payment(IN p_campaign_id bigint, IN p_payment_amount numeric, IN p_payment_date date DEFAULT CURRENT_DATE)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_campaign_end_date DATE;
    v_remaining_days INT;
    v_daily_rate DECIMAL(12,2);
    v_value_30d DECIMAL(12,2);
    v_company_id BIGINT;
BEGIN
    -- 캠페인 종료일 및 회사 ID 조회
    SELECT
        COALESCE(MAX(ap.apply_to_date), ac.end_date),
        ac.company_id
    INTO v_campaign_end_date, v_company_id
    FROM ad_campaigns ac
    LEFT JOIN ad_payments ap ON ac.id = ap.campaign_id AND ap.status = 'ACTIVE'
    WHERE ac.id = p_campaign_id
    GROUP BY ac.company_id, ac.end_date;

    -- 캠페인이 없는 경우
    IF v_company_id IS NULL THEN
        RAISE EXCEPTION '캠페인을 찾을 수 없습니다 (ID: %)', p_campaign_id;
    END IF;

    -- 종료일이 없는 경우 30일로 설정
    IF v_campaign_end_date IS NULL THEN
        v_campaign_end_date := p_payment_date + INTERVAL '30 days';
    END IF;

    -- 남은 일수 계산
    v_remaining_days := v_campaign_end_date - p_payment_date + 1;

    IF v_remaining_days <= 0 THEN
        RAISE EXCEPTION '캠페인이 이미 종료되었습니다';
    END IF;

    -- 일일 단가 및 30일 환산 가치 계산
    v_daily_rate := p_payment_amount / v_remaining_days;
    v_value_30d := v_daily_rate * 30;

    -- 결제 이력 추가
    INSERT INTO ad_payments (
        campaign_id,
        payment_amount,
        payment_date,
        apply_from_date,
        apply_to_date,
        apply_days,
        daily_rate,
        value_30d,
        payment_type,
        status
    ) VALUES (
        p_campaign_id,
        p_payment_amount,
        CURRENT_TIMESTAMP,
        p_payment_date,
        v_campaign_end_date,
        v_remaining_days,
        v_daily_rate,
        v_value_30d,
        'ADDITIONAL',
        'ACTIVE'
    );

    -- 캠페인 총 가치 및 우선순위 업데이트
    UPDATE ad_campaigns
    SET total_value_30d = calculate_total_value_30d(p_campaign_id),
        priority_score = calculate_total_value_30d(p_campaign_id),
        secondary_score = calculate_secondary_score(v_company_id),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_campaign_id;

    COMMIT;
END;
$$;


--
-- Name: PROCEDURE process_additional_payment(IN p_campaign_id bigint, IN p_payment_amount numeric, IN p_payment_date date); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON PROCEDURE public.process_additional_payment(IN p_campaign_id bigint, IN p_payment_amount numeric, IN p_payment_date date) IS '광고 추가 결제 처리 (30일 환산 가치 재계산)';


--
-- Name: update_admin_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_admin_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: update_filter_option_path(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_filter_option_path() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ad_campaigns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ad_campaigns (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    ad_type character varying(30) NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    ad_config jsonb DEFAULT '{}'::jsonb,
    targeting jsonb DEFAULT '{}'::jsonb,
    budget_type character varying(20),
    budget_amount numeric(12,2),
    daily_budget numeric(12,2),
    total_spent numeric(12,2) DEFAULT 0 NOT NULL,
    total_value_30d numeric(12,2) DEFAULT 0 NOT NULL,
    priority_score numeric(12,2) DEFAULT 0 NOT NULL,
    secondary_score numeric(12,2) DEFAULT 0 NOT NULL,
    is_premium boolean DEFAULT false NOT NULL,
    premium_until timestamp without time zone,
    start_date date NOT NULL,
    end_date date,
    total_impressions bigint DEFAULT 0 NOT NULL,
    total_clicks bigint DEFAULT 0 NOT NULL,
    total_conversions bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE ad_campaigns; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ad_campaigns IS '광고 캠페인 관리 테이블';


--
-- Name: COLUMN ad_campaigns.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.id IS '캠페인 고유 ID';


--
-- Name: COLUMN ad_campaigns.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN ad_campaigns.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.company_id IS '광고주 업체 ID';


--
-- Name: COLUMN ad_campaigns.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.name IS '캠페인명';


--
-- Name: COLUMN ad_campaigns.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.description IS '캠페인 설명';


--
-- Name: COLUMN ad_campaigns.ad_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.ad_type IS '광고 타입 (LISTING:상위노출, AI_RECOMMENDATION:AI추천, BANNER:배너, POPUP:팝업)';


--
-- Name: COLUMN ad_campaigns.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.status IS '캠페인 상태 (DRAFT, ACTIVE, PAUSED, COMPLETED, CANCELLED)';


--
-- Name: COLUMN ad_campaigns.ad_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.ad_config IS '광고 타입별 상세 설정 (JSONB)';


--
-- Name: COLUMN ad_campaigns.targeting; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.targeting IS '타겟팅 설정 (JSONB: 지역, 나이, 관심사 등)';


--
-- Name: COLUMN ad_campaigns.budget_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.budget_type IS '예산 타입 (DAILY:일별, TOTAL:전체, UNLIMITED:무제한)';


--
-- Name: COLUMN ad_campaigns.budget_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.budget_amount IS '예산 금액';


--
-- Name: COLUMN ad_campaigns.daily_budget; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.daily_budget IS '일일 예산';


--
-- Name: COLUMN ad_campaigns.total_spent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.total_spent IS '총 소진 금액';


--
-- Name: COLUMN ad_campaigns.total_value_30d; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.total_value_30d IS '30일 환산 총 광고 가치';


--
-- Name: COLUMN ad_campaigns.priority_score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.priority_score IS '우선순위 점수 (30일 환산 가치 기반)';


--
-- Name: COLUMN ad_campaigns.secondary_score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.secondary_score IS '2차 정렬 점수 (평점, 리뷰 기반)';


--
-- Name: COLUMN ad_campaigns.is_premium; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.is_premium IS '프리미엄 광고 여부';


--
-- Name: COLUMN ad_campaigns.premium_until; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.premium_until IS '프리미엄 종료일시';


--
-- Name: COLUMN ad_campaigns.start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.start_date IS '캠페인 시작일';


--
-- Name: COLUMN ad_campaigns.end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.end_date IS '캠페인 종료일';


--
-- Name: COLUMN ad_campaigns.total_impressions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.total_impressions IS '총 노출수';


--
-- Name: COLUMN ad_campaigns.total_clicks; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.total_clicks IS '총 클릭수';


--
-- Name: COLUMN ad_campaigns.total_conversions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.total_conversions IS '총 전환수';


--
-- Name: COLUMN ad_campaigns.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.created_at IS '생성 일시';


--
-- Name: COLUMN ad_campaigns.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.updated_at IS '수정 일시';


--
-- Name: COLUMN ad_campaigns.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.is_deleted IS '삭제 여부';


--
-- Name: COLUMN ad_campaigns.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.deleted_at IS '삭제 일시';


--
-- Name: COLUMN ad_campaigns.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ad_campaigns.metadata IS '확장 데이터 (JSONB)';


--
-- Name: ad_campaigns_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ad_campaigns_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ad_campaigns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ad_campaigns_id_seq OWNED BY public.ad_campaigns.id;


--
-- Name: admin_activity_summary; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_activity_summary (
    id bigint NOT NULL,
    admin_user_id bigint NOT NULL,
    summary_date date NOT NULL,
    summary_type character varying(20) NOT NULL,
    login_count integer DEFAULT 0 NOT NULL,
    total_time_minutes integer DEFAULT 0 NOT NULL,
    actions_performed jsonb DEFAULT '{}'::jsonb,
    entities_created jsonb DEFAULT '{}'::jsonb,
    entities_updated jsonb DEFAULT '{}'::jsonb,
    entities_deleted jsonb DEFAULT '{}'::jsonb,
    entities_viewed jsonb DEFAULT '{}'::jsonb,
    api_calls_count integer DEFAULT 0 NOT NULL,
    api_errors_count integer DEFAULT 0 NOT NULL,
    pages_visited jsonb DEFAULT '{}'::jsonb,
    most_visited_page character varying(100),
    security_actions jsonb DEFAULT '{}'::jsonb,
    failed_actions integer DEFAULT 0 NOT NULL,
    performance_score numeric(5,2),
    productivity_index numeric(5,2),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE admin_activity_summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_activity_summary IS '어드민 활동 요약 테이블 - 일/주/월별 어드민 활동 통계';


--
-- Name: COLUMN admin_activity_summary.summary_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_activity_summary.summary_type IS '요약 유형 (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)';


--
-- Name: COLUMN admin_activity_summary.performance_score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_activity_summary.performance_score IS '성과 점수 (0-100)';


--
-- Name: admin_activity_summary_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_activity_summary_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_activity_summary_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_activity_summary_id_seq OWNED BY public.admin_activity_summary.id;


--
-- Name: admin_audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_audit_logs (
    id bigint NOT NULL,
    admin_user_id bigint,
    action character varying(50) NOT NULL,
    entity_type character varying(50),
    entity_id bigint,
    description text,
    changes jsonb,
    ip_address character varying(50),
    user_agent character varying(500),
    is_successful boolean DEFAULT true NOT NULL,
    failure_reason text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE admin_audit_logs; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_audit_logs IS '어드민 감사 로그 - 관리자 작업 이력 추적';


--
-- Name: admin_audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_audit_logs_id_seq OWNED BY public.admin_audit_logs.id;


--
-- Name: admin_ip_whitelist; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_ip_whitelist (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    admin_user_id bigint,
    ip_address character varying(50) NOT NULL,
    cidr_notation character varying(50),
    description text,
    rule_type character varying(20) DEFAULT 'WHITELIST'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    valid_from timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp with time zone,
    created_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint
);


--
-- Name: TABLE admin_ip_whitelist; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_ip_whitelist IS '어드민 IP 화이트리스트 테이블 - IP 기반 접근 제어';


--
-- Name: COLUMN admin_ip_whitelist.cidr_notation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_ip_whitelist.cidr_notation IS 'CIDR 표기법 IP 범위 (예: 192.168.1.0/24)';


--
-- Name: COLUMN admin_ip_whitelist.rule_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_ip_whitelist.rule_type IS '규칙 유형 (WHITELIST, BLACKLIST)';


--
-- Name: admin_ip_whitelist_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_ip_whitelist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_ip_whitelist_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_ip_whitelist_id_seq OWNED BY public.admin_ip_whitelist.id;


--
-- Name: admin_login_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_login_history (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    admin_user_id bigint,
    email character varying(255) NOT NULL,
    login_type character varying(20) NOT NULL,
    ip_address character varying(50) NOT NULL,
    user_agent text,
    device_type character varying(20),
    location jsonb DEFAULT '{}'::jsonb,
    failure_reason character varying(50),
    failed_attempts integer DEFAULT 0,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE admin_login_history; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_login_history IS '어드민 로그인 기록 테이블 - 모든 로그인 시도와 결과 기록';


--
-- Name: COLUMN admin_login_history.login_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_login_history.login_type IS '로그인 유형 (SUCCESS, FAILED, BLOCKED, LOCKED)';


--
-- Name: COLUMN admin_login_history.failure_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_login_history.failure_reason IS '실패 사유 (INVALID_PASSWORD, USER_NOT_FOUND, ACCOUNT_LOCKED, IP_BLOCKED)';


--
-- Name: admin_login_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_login_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_login_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_login_history_id_seq OWNED BY public.admin_login_history.id;


--
-- Name: admin_notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_notifications (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    admin_user_id bigint,
    notification_type character varying(50) NOT NULL,
    severity character varying(20) NOT NULL,
    title character varying(500) NOT NULL,
    message text NOT NULL,
    priority character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    target_roles text[] DEFAULT '{}'::text[],
    target_users bigint[] DEFAULT '{}'::bigint[],
    excluded_users bigint[] DEFAULT '{}'::bigint[],
    entity_type character varying(50),
    entity_id bigint,
    entity_uuid uuid,
    related_data jsonb DEFAULT '{}'::jsonb,
    action_url character varying(500),
    action_type character varying(20),
    action_params jsonb DEFAULT '{}'::jsonb,
    action_data jsonb,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    read_by bigint[] DEFAULT '{}'::bigint[],
    expires_at timestamp with time zone,
    is_expired boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE admin_notifications; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_notifications IS '어드민 알림 테이블 - 시스템 알림과 중요 이벤트 관리';


--
-- Name: COLUMN admin_notifications.severity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_notifications.severity IS '알림 심각도 (LOW, MEDIUM, HIGH, CRITICAL)';


--
-- Name: COLUMN admin_notifications.target_roles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_notifications.target_roles IS '알림 대상 역할 배열';


--
-- Name: admin_notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_notifications_id_seq OWNED BY public.admin_notifications.id;


--
-- Name: admin_page_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_page_permissions (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    page_code character varying(50) NOT NULL,
    page_name character varying(100) NOT NULL,
    page_url character varying(500) NOT NULL,
    parent_page_id bigint,
    page_level integer DEFAULT 0 NOT NULL,
    page_path character varying(500),
    required_permissions text[] DEFAULT '{}'::text[],
    required_roles text[] DEFAULT '{}'::text[],
    display_order integer DEFAULT 0 NOT NULL,
    is_menu_visible boolean DEFAULT true NOT NULL,
    icon character varying(50),
    badge_text character varying(20),
    page_type character varying(20) DEFAULT 'PAGE'::character varying NOT NULL,
    module character varying(50),
    is_active boolean DEFAULT true NOT NULL,
    is_public boolean DEFAULT false NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone
);


--
-- Name: TABLE admin_page_permissions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_page_permissions IS '어드민 페이지 권한 테이블 - 페이지별 접근 권한 정의';


--
-- Name: COLUMN admin_page_permissions.required_permissions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_page_permissions.required_permissions IS '접근에 필요한 권한 배열';


--
-- Name: COLUMN admin_page_permissions.page_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_page_permissions.page_type IS '페이지 유형 (PAGE, API, COMPONENT, WIDGET)';


--
-- Name: admin_page_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_page_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_page_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_page_permissions_id_seq OWNED BY public.admin_page_permissions.id;


--
-- Name: admin_password_policies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_password_policies (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    admin_user_id bigint,
    policy_name character varying(100) NOT NULL,
    min_length integer DEFAULT 8 NOT NULL,
    max_length integer DEFAULT 128,
    require_uppercase boolean DEFAULT true NOT NULL,
    require_lowercase boolean DEFAULT true NOT NULL,
    require_numbers boolean DEFAULT true NOT NULL,
    require_special_chars boolean DEFAULT true NOT NULL,
    special_chars_set character varying(100) DEFAULT '!@#$%^&*()_+-=[]{}|;:,.<>?'::character varying,
    max_age_days integer DEFAULT 90,
    min_age_days integer DEFAULT 1,
    expire_warning_days integer DEFAULT 14,
    prevent_reuse_count integer DEFAULT 5,
    prevent_common_passwords boolean DEFAULT true NOT NULL,
    failed_attempts_lockout integer DEFAULT 5,
    lockout_duration_minutes integer DEFAULT 30,
    reset_failed_attempts_minutes integer DEFAULT 30,
    prevent_user_info boolean DEFAULT true NOT NULL,
    require_change_on_first_login boolean DEFAULT true NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by bigint,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone
);


--
-- Name: TABLE admin_password_policies; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_password_policies IS '어드민 비밀번호 정책 테이블 - 비밀번호 복잡도와 생명주기 규칙';


--
-- Name: COLUMN admin_password_policies.prevent_reuse_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_password_policies.prevent_reuse_count IS '재사용 방지할 이전 비밀번호 개수';


--
-- Name: COLUMN admin_password_policies.failed_attempts_lockout; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_password_policies.failed_attempts_lockout IS '계정 잠금까지 허용되는 실패 횟수';


--
-- Name: admin_password_policies_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_password_policies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_password_policies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_password_policies_id_seq OWNED BY public.admin_password_policies.id;


--
-- Name: admin_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_permissions (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    permission_code character varying(100) NOT NULL,
    permission_name character varying(100) NOT NULL,
    resource_type character varying(50) NOT NULL,
    action character varying(20) NOT NULL,
    description text,
    is_system_permission boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE admin_permissions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_permissions IS '어드민 권한 테이블 - 세분화된 권한 관리';


--
-- Name: admin_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_permissions_id_seq OWNED BY public.admin_permissions.id;


--
-- Name: admin_role_page_access; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_role_page_access (
    id bigint NOT NULL,
    role_name character varying(50) NOT NULL,
    page_id bigint NOT NULL,
    access_type character varying(20) DEFAULT 'FULL'::character varying NOT NULL,
    custom_permissions jsonb DEFAULT '{}'::jsonb,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by bigint
);


--
-- Name: TABLE admin_role_page_access; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_role_page_access IS '역할-페이지 접근 매핑 테이블 - 역할별 페이지 접근 권한 설정';


--
-- Name: COLUMN admin_role_page_access.access_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_role_page_access.access_type IS '접근 유형 (FULL, READ_ONLY, HIDDEN, CUSTOM)';


--
-- Name: admin_role_page_access_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_role_page_access_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_role_page_access_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_role_page_access_id_seq OWNED BY public.admin_role_page_access.id;


--
-- Name: admin_role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_role_permissions (
    id bigint NOT NULL,
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    granted_by bigint,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE admin_role_permissions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_role_permissions IS '어드민 역할-권한 매핑 - 역할별 세분화된 권한 할당';


--
-- Name: admin_role_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_role_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_role_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_role_permissions_id_seq OWNED BY public.admin_role_permissions.id;


--
-- Name: admin_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_roles (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    role_code character varying(50) NOT NULL,
    role_name character varying(100) NOT NULL,
    description text,
    priority integer NOT NULL,
    is_system_role boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by bigint,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE admin_roles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_roles IS '어드민 역할 정의 테이블 - 역할 유형과 우선순위 관리';


--
-- Name: COLUMN admin_roles.role_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_roles.role_code IS '역할 코드 (SUPER_ADMIN, USER_MANAGER, COMPANY_MANAGER, etc.)';


--
-- Name: COLUMN admin_roles.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_roles.priority IS '우선순위 - 높은 숫자가 높은 우선순위';


--
-- Name: admin_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_roles_id_seq OWNED BY public.admin_roles.id;


--
-- Name: admin_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_sessions (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    admin_user_id bigint NOT NULL,
    session_token text NOT NULL,
    refresh_token text,
    device_info jsonb DEFAULT '{}'::jsonb,
    ip_address character varying(50) NOT NULL,
    user_agent text,
    expires_at timestamp with time zone NOT NULL,
    last_activity_at timestamp with time zone,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE admin_sessions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_sessions IS '어드민 세션 관리 테이블 - 로그인된 어드민의 세션 정보와 활동 추적';


--
-- Name: COLUMN admin_sessions.session_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_sessions.session_token IS '암호화된 세션 토큰';


--
-- Name: COLUMN admin_sessions.device_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_sessions.device_info IS '접속 디바이스 정보 (OS, 브라우저, 버전 등)';


--
-- Name: COLUMN admin_sessions.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_sessions.status IS '세션 상태 (ACTIVE, EXPIRED, REVOKED)';


--
-- Name: admin_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_sessions_id_seq OWNED BY public.admin_sessions.id;


--
-- Name: admin_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_settings (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    setting_key character varying(100) NOT NULL,
    setting_value jsonb NOT NULL,
    setting_type character varying(20) NOT NULL,
    category character varying(50) NOT NULL,
    subcategory character varying(50),
    module character varying(50),
    description text,
    default_value jsonb,
    validation_rules jsonb DEFAULT '{}'::jsonb,
    is_public boolean DEFAULT false NOT NULL,
    is_editable boolean DEFAULT true NOT NULL,
    required_permission character varying(50),
    is_feature_flag boolean DEFAULT false NOT NULL,
    environment character varying(20),
    previous_value jsonb,
    changed_at timestamp with time zone,
    changed_by bigint,
    change_reason text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by bigint
);


--
-- Name: TABLE admin_settings; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_settings IS '어드민 시스템 설정 테이블 - 시스템 전역 설정과 Feature Flag 관리';


--
-- Name: COLUMN admin_settings.setting_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_settings.setting_type IS '설정 값 유형 (STRING, NUMBER, BOOLEAN, JSON, ARRAY)';


--
-- Name: COLUMN admin_settings.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_settings.category IS '설정 카테고리 (SYSTEM, SECURITY, UI, NOTIFICATION, PAYMENT)';


--
-- Name: admin_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_settings_id_seq OWNED BY public.admin_settings.id;


--
-- Name: admin_two_factor_auth; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_two_factor_auth (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    admin_user_id bigint NOT NULL,
    method character varying(20) NOT NULL,
    secret_key text,
    phone_number character varying(20),
    email character varying(255),
    backup_codes jsonb DEFAULT '[]'::jsonb,
    backup_codes_generated_at timestamp with time zone,
    is_enabled boolean DEFAULT false NOT NULL,
    is_verified boolean DEFAULT false NOT NULL,
    verified_at timestamp with time zone,
    last_used_at timestamp with time zone,
    last_used_ip inet,
    failed_attempts integer DEFAULT 0,
    locked_until timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE admin_two_factor_auth; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_two_factor_auth IS '어드민 2단계 인증 테이블 - TOTP, SMS, 이메일 등 다중 인증 지원';


--
-- Name: COLUMN admin_two_factor_auth.method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_two_factor_auth.method IS '2FA 방법 (TOTP, SMS, EMAIL, APP)';


--
-- Name: COLUMN admin_two_factor_auth.backup_codes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_two_factor_auth.backup_codes IS '암호화된 백업 코드 JSON 배열';


--
-- Name: admin_two_factor_auth_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_two_factor_auth_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_two_factor_auth_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_two_factor_auth_id_seq OWNED BY public.admin_two_factor_auth.id;


--
-- Name: admin_user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_user_roles (
    id bigint NOT NULL,
    admin_user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    granted_by bigint,
    expires_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE admin_user_roles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_user_roles IS '어드민 사용자-역할 매핑 테이블 - 사용자별 역할 할당 관리';


--
-- Name: admin_user_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_user_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_user_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_user_roles_id_seq OWNED BY public.admin_user_roles.id;


--
-- Name: admin_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_users (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(100) NOT NULL,
    password_hash text NOT NULL,
    name character varying(100) NOT NULL,
    phone_number character varying(20),
    roles text[] DEFAULT '{}'::text[] NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    last_login_at timestamp with time zone,
    last_login_ip character varying(50),
    login_count bigint DEFAULT 0 NOT NULL,
    failed_login_count integer DEFAULT 0 NOT NULL,
    is_2fa_enabled boolean DEFAULT false NOT NULL,
    two_fa_secret character varying(200),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by bigint,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE admin_users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.admin_users IS '어드민 사용자 테이블 - 어드민 시스템 사용자 정보';


--
-- Name: COLUMN admin_users.roles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_users.roles IS '역할 배열 (SUPER_ADMIN, ADMIN, OPERATOR, VIEWER)';


--
-- Name: COLUMN admin_users.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.admin_users.status IS '상태 (ACTIVE, INACTIVE, SUSPENDED)';


--
-- Name: admin_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admin_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admin_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admin_users_id_seq OWNED BY public.admin_users.id;


--
-- Name: analytics_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.analytics_events (
    id bigint NOT NULL,
    user_id bigint,
    session_id character varying(255),
    event_name character varying(100) NOT NULL,
    event_category character varying(100),
    event_action character varying(100),
    event_label character varying(200),
    event_value numeric(12,2),
    page_url character varying(500),
    page_title character varying(200),
    referrer character varying(500),
    device_type character varying(50),
    browser character varying(100),
    os character varying(100),
    ip_address character varying(45),
    country character varying(2),
    region character varying(100),
    properties jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE analytics_events; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.analytics_events IS '이벤트 추적 로그 테이블';


--
-- Name: COLUMN analytics_events.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.id IS '이벤트 로그 ID';


--
-- Name: COLUMN analytics_events.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.user_id IS '사용자 ID';


--
-- Name: COLUMN analytics_events.session_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.session_id IS '세션 ID';


--
-- Name: COLUMN analytics_events.event_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.event_name IS '이벤트명';


--
-- Name: COLUMN analytics_events.event_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.event_category IS '이벤트 카테고리';


--
-- Name: COLUMN analytics_events.event_action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.event_action IS '이벤트 액션';


--
-- Name: COLUMN analytics_events.event_label; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.event_label IS '이벤트 라벨';


--
-- Name: COLUMN analytics_events.event_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.event_value IS '이벤트 값';


--
-- Name: COLUMN analytics_events.page_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.page_url IS '페이지 URL';


--
-- Name: COLUMN analytics_events.page_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.page_title IS '페이지 제목';


--
-- Name: COLUMN analytics_events.referrer; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.referrer IS '참조 URL';


--
-- Name: COLUMN analytics_events.device_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.device_type IS '디바이스 유형';


--
-- Name: COLUMN analytics_events.browser; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.browser IS '브라우저';


--
-- Name: COLUMN analytics_events.os; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.os IS '운영체제';


--
-- Name: COLUMN analytics_events.ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.ip_address IS 'IP 주소';


--
-- Name: COLUMN analytics_events.country; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.country IS '국가 코드';


--
-- Name: COLUMN analytics_events.region; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.region IS '지역';


--
-- Name: COLUMN analytics_events.properties; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.properties IS '이벤트 속성 (JSON)';


--
-- Name: COLUMN analytics_events.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.analytics_events.created_at IS '이벤트 발생 일시';


--
-- Name: analytics_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.analytics_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: analytics_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.analytics_events_id_seq OWNED BY public.analytics_events.id;


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    user_id bigint,
    action character varying(100) NOT NULL,
    entity_type character varying(100),
    entity_id bigint,
    old_values jsonb,
    new_values jsonb,
    ip_address character varying(45),
    user_agent text,
    request_id character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE audit_logs; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.audit_logs IS '감사 로그 테이블';


--
-- Name: COLUMN audit_logs.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.id IS '로그 고유 ID';


--
-- Name: COLUMN audit_logs.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.user_id IS '수행자 ID';


--
-- Name: COLUMN audit_logs.action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.action IS '수행 액션';


--
-- Name: COLUMN audit_logs.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.entity_type IS '대상 엔티티 타입';


--
-- Name: COLUMN audit_logs.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.entity_id IS '대상 엔티티 ID';


--
-- Name: COLUMN audit_logs.old_values; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.old_values IS '변경 전 값 (JSON)';


--
-- Name: COLUMN audit_logs.new_values; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.new_values IS '변경 후 값 (JSON)';


--
-- Name: COLUMN audit_logs.ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.ip_address IS 'IP 주소';


--
-- Name: COLUMN audit_logs.user_agent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.user_agent IS '브라우저/클라이언트 정보';


--
-- Name: COLUMN audit_logs.request_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.request_id IS '요청 ID';


--
-- Name: COLUMN audit_logs.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.created_at IS '로그 생성 일시';


--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: board_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.board_attachments (
    id bigint NOT NULL,
    board_id bigint NOT NULL,
    file_id bigint NOT NULL,
    attachment_type character varying(50),
    display_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE board_attachments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.board_attachments IS '게시판 첨부파일 테이블';


--
-- Name: COLUMN board_attachments.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_attachments.id IS '첨부파일 고유 ID';


--
-- Name: COLUMN board_attachments.board_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_attachments.board_id IS '게시글 ID';


--
-- Name: COLUMN board_attachments.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_attachments.file_id IS '파일 ID';


--
-- Name: COLUMN board_attachments.attachment_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_attachments.attachment_type IS '첨부 유형 (FILE, IMAGE, VIDEO)';


--
-- Name: COLUMN board_attachments.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_attachments.display_order IS '표시 순서';


--
-- Name: COLUMN board_attachments.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_attachments.created_at IS '첨부 일시';


--
-- Name: board_attachments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.board_attachments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: board_attachments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.board_attachments_id_seq OWNED BY public.board_attachments.id;


--
-- Name: board_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.board_categories (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    board_type VARCHAR(50) NOT NULL CHECK (board_type IN ('NOTICE', 'EVENT', 'FAQ', 'GALLERY', 'DOCUMENT')),
    name character varying(100) NOT NULL,
    slug character varying(100),
    description character varying(500),
    parent_id bigint,
    depth integer DEFAULT 0,
    path character varying(500),
    display_order integer DEFAULT 0,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE board_categories; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.board_categories IS '게시판 카테고리 테이블';


--
-- Name: COLUMN board_categories.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.id IS '카테고리 고유 ID';


--
-- Name: COLUMN board_categories.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN board_categories.board_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.board_type IS '게시판 유형';


--
-- Name: COLUMN board_categories.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.name IS '카테고리명';


--
-- Name: COLUMN board_categories.slug; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.slug IS 'URL 슬러그';


--
-- Name: COLUMN board_categories.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.description IS '카테고리 설명';


--
-- Name: COLUMN board_categories.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.parent_id IS '부모 카테고리 ID';


--
-- Name: COLUMN board_categories.depth; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.depth IS '카테고리 깊이';


--
-- Name: COLUMN board_categories.path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.path IS '카테고리 경로';


--
-- Name: COLUMN board_categories.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.display_order IS '표시 순서';


--
-- Name: COLUMN board_categories.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.is_active IS '활성 상태';


--
-- Name: COLUMN board_categories.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.created_at IS '생성 일시';


--
-- Name: COLUMN board_categories.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_categories.updated_at IS '수정 일시';


--
-- Name: board_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.board_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: board_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.board_categories_id_seq OWNED BY public.board_categories.id;


--
-- Name: board_comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.board_comments (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    board_id bigint NOT NULL,
    user_id bigint,
    parent_id bigint,
    content text NOT NULL,
    is_secret boolean DEFAULT false,
    is_reported boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone
);


--
-- Name: TABLE board_comments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.board_comments IS '게시판 댓글 테이블';


--
-- Name: COLUMN board_comments.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.id IS '댓글 고유 ID';


--
-- Name: COLUMN board_comments.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN board_comments.board_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.board_id IS '게시글 ID';


--
-- Name: COLUMN board_comments.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.user_id IS '작성자 ID';


--
-- Name: COLUMN board_comments.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.parent_id IS '부모 댓글 ID (대댓글)';


--
-- Name: COLUMN board_comments.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.content IS '댓글 내용';


--
-- Name: COLUMN board_comments.is_secret; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.is_secret IS '비밀 댓글 여부';


--
-- Name: COLUMN board_comments.is_reported; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.is_reported IS '신고된 댓글 여부';


--
-- Name: COLUMN board_comments.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.created_at IS '작성 일시';


--
-- Name: COLUMN board_comments.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.updated_at IS '수정 일시';


--
-- Name: COLUMN board_comments.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.is_deleted IS '삭제 여부';


--
-- Name: COLUMN board_comments.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_comments.deleted_at IS '삭제 일시';


--
-- Name: board_comments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.board_comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: board_comments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.board_comments_id_seq OWNED BY public.board_comments.id;


--
-- Name: board_likes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.board_likes (
    id bigint NOT NULL,
    board_id bigint NOT NULL,
    user_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE board_likes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.board_likes IS '게시판 좋아요 테이블';


--
-- Name: COLUMN board_likes.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_likes.id IS '좋아요 고유 ID';


--
-- Name: COLUMN board_likes.board_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_likes.board_id IS '게시글 ID';


--
-- Name: COLUMN board_likes.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_likes.user_id IS '사용자 ID';


--
-- Name: COLUMN board_likes.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.board_likes.created_at IS '좋아요 일시';


--
-- Name: board_likes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.board_likes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: board_likes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.board_likes_id_seq OWNED BY public.board_likes.id;


--
-- Name: boards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.boards (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint,
    board_type VARCHAR(50) NOT NULL CHECK (board_type IN ('NOTICE', 'EVENT', 'FAQ', 'GALLERY', 'DOCUMENT')),
    category_id bigint,
    title character varying(200) NOT NULL,
    content text,
    type_data jsonb DEFAULT '{}'::jsonb,
    view_count integer DEFAULT 0,
    like_count integer DEFAULT 0,
    comment_count integer DEFAULT 0,
    is_pinned boolean DEFAULT false,
    is_featured boolean DEFAULT false,
    is_published boolean DEFAULT true,
    published_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tags text[] DEFAULT '{}'::text[],
    is_private boolean DEFAULT false,
    password character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint,
    updated_by bigint,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE boards; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.boards IS '통합 게시판 테이블';


--
-- Name: COLUMN boards.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.id IS '게시글 고유 ID';


--
-- Name: COLUMN boards.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN boards.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.user_id IS '작성자 ID';


--
-- Name: COLUMN boards.board_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.board_type IS '게시판 유형 (NOTICE, EVENT, FAQ, GALLERY, DOCUMENT)';


--
-- Name: COLUMN boards.category_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.category_id IS '카테고리 ID';


--
-- Name: COLUMN boards.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.title IS '제목';


--
-- Name: COLUMN boards.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.content IS '내용';


--
-- Name: COLUMN boards.type_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.type_data IS '게시판 타입별 특수 데이터 (JSON)';


--
-- Name: COLUMN boards.view_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.view_count IS '조회수';


--
-- Name: COLUMN boards.like_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.like_count IS '좋아요 수';


--
-- Name: COLUMN boards.comment_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.comment_count IS '댓글 수';


--
-- Name: COLUMN boards.is_pinned; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.is_pinned IS '상단 고정 여부';


--
-- Name: COLUMN boards.is_featured; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.is_featured IS '추천글 여부';


--
-- Name: COLUMN boards.is_published; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.is_published IS '게시 여부';


--
-- Name: COLUMN boards.published_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.published_at IS '게시 일시';


--
-- Name: COLUMN boards.tags; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.tags IS '태그 (배열)';


--
-- Name: COLUMN boards.is_private; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.is_private IS '비공개 여부';


--
-- Name: COLUMN boards.password; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.password IS '비밀글 비밀번호';


--
-- Name: COLUMN boards.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.created_at IS '작성 일시';


--
-- Name: COLUMN boards.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.updated_at IS '수정 일시';


--
-- Name: COLUMN boards.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.created_by IS '작성자 ID';


--
-- Name: COLUMN boards.updated_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.updated_by IS '수정자 ID';


--
-- Name: COLUMN boards.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.is_deleted IS '삭제 여부';


--
-- Name: COLUMN boards.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.deleted_at IS '삭제 일시';


--
-- Name: COLUMN boards.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.boards.metadata IS '확장 데이터 (JSON)';


--
-- Name: boards_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.boards_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: boards_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.boards_id_seq OWNED BY public.boards.id;


--
-- Name: companies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.companies (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    logo_url character varying(500),
    cover_image_url character varying(500),
    website_url character varying(500),
    business_info jsonb DEFAULT '{}'::jsonb,
    business_hours jsonb DEFAULT '{}'::jsonb,
    service_areas text[] DEFAULT '{}'::text[],
    coordinates jsonb DEFAULT '{}'::jsonb,
    phone character varying(20),
    email character varying(255),
    fax character varying(20),
    tags text[] DEFAULT '{}'::text[],
    keywords text[] DEFAULT '{}'::text[],
    specialties text[] DEFAULT '{}'::text[],
    avg_rating numeric(3,2) DEFAULT 0,
    review_count integer DEFAULT 0,
    portfolio_count integer DEFAULT 0,
    completed_projects integer DEFAULT 0,
    is_verified boolean DEFAULT false,
    verified_at timestamp without time zone,
    is_premium boolean DEFAULT false,
    premium_until timestamp without time zone,
    settings jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb,
    detail_content text,
    detail_content_format character varying(20) DEFAULT 'HTML'::character varying,
    primary_phone character varying(20),
    secondary_phone character varying(20),
    emergency_contact character varying(20),
    kakao_chat_url character varying(500),
    social_links jsonb DEFAULT '{}'::jsonb,
    business_hours_note text,
    slug character varying(200),
    address character varying(500),
    postal_code character varying(20),
    latitude numeric(10,7),
    longitude numeric(10,7),
    view_count integer DEFAULT 0 NOT NULL,
    like_count integer DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    featured boolean DEFAULT false NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    premium_tier character varying(20) DEFAULT 'NONE'::character varying NOT NULL,
    premium_monthly_amount numeric(10,2) DEFAULT 0 NOT NULL,
    images bigint[]
);


--
-- Name: TABLE companies; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.companies IS '업체 통합 정보 테이블';


--
-- Name: COLUMN companies.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.id IS '업체 고유 ID';


--
-- Name: COLUMN companies.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN companies.owner_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.owner_id IS '업체 소유자 ID (User FK)';


--
-- Name: COLUMN companies.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.name IS '업체명';


--
-- Name: COLUMN companies.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.description IS '업체 소개';


--
-- Name: COLUMN companies.logo_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.logo_url IS '업체 로고 이미지 URL';


--
-- Name: COLUMN companies.cover_image_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.cover_image_url IS '업체 커버 이미지 URL';


--
-- Name: COLUMN companies.website_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.website_url IS '업체 웹사이트 URL';


--
-- Name: COLUMN companies.business_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.business_info IS '사업자 정보 (JSON: 사업자번호, 대표자명, 업종 등)';


--
-- Name: COLUMN companies.business_hours; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.business_hours IS '영업시간 (JSON: 요일별 open/close 시간)';


--
-- Name: COLUMN companies.service_areas; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.service_areas IS '서비스 제공 지역 (배열)';


--
-- Name: COLUMN companies.coordinates; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.coordinates IS '업체 위치 좌표 (JSON: lat, lng, address)';


--
-- Name: COLUMN companies.phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.phone IS '대표 전화번호';


--
-- Name: COLUMN companies.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.email IS '대표 이메일';


--
-- Name: COLUMN companies.fax; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.fax IS '팩스 번호';


--
-- Name: COLUMN companies.tags; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.tags IS '업체 태그/해시태그 (배열)';


--
-- Name: COLUMN companies.keywords; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.keywords IS 'SEO 검색 키워드 (배열)';


--
-- Name: COLUMN companies.specialties; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.specialties IS '전문 분야 (배열)';


--
-- Name: COLUMN companies.avg_rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.avg_rating IS '평균 평점 (1-5)';


--
-- Name: COLUMN companies.review_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.review_count IS '리뷰 개수';


--
-- Name: COLUMN companies.portfolio_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.portfolio_count IS '포트폴리오 개수';


--
-- Name: COLUMN companies.completed_projects; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.completed_projects IS '완료된 프로젝트 수';


--
-- Name: COLUMN companies.is_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.is_verified IS '인증 업체 여부';


--
-- Name: COLUMN companies.verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.verified_at IS '인증 일시';


--
-- Name: COLUMN companies.is_premium; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.is_premium IS '프리미엄 업체 여부';


--
-- Name: COLUMN companies.premium_until; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.premium_until IS '프리미엄 만료일';


--
-- Name: COLUMN companies.settings; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.settings IS '업체 설정 (JSON)';


--
-- Name: COLUMN companies.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.created_at IS '등록 일시';


--
-- Name: COLUMN companies.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.updated_at IS '수정 일시';


--
-- Name: COLUMN companies.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.is_deleted IS '삭제 여부';


--
-- Name: COLUMN companies.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.deleted_at IS '삭제 일시';


--
-- Name: COLUMN companies.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.metadata IS '확장 데이터 (JSON)';


--
-- Name: COLUMN companies.detail_content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.detail_content IS 'HTML/Markdown 형식의 상세 소개';


--
-- Name: COLUMN companies.detail_content_format; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.detail_content_format IS '상세 소개 형식 (HTML 또는 MARKDOWN)';


--
-- Name: COLUMN companies.primary_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.primary_phone IS '대표 전화번호';


--
-- Name: COLUMN companies.secondary_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.secondary_phone IS '보조 전화번호';


--
-- Name: COLUMN companies.emergency_contact; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.emergency_contact IS '긴급 연락처';


--
-- Name: COLUMN companies.kakao_chat_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.kakao_chat_url IS '카카오톡 채팅 상담 URL';


--
-- Name: COLUMN companies.social_links; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.social_links IS 'SNS 링크 (JSON: facebook, instagram, youtube 등)';


--
-- Name: COLUMN companies.business_hours_note; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.business_hours_note IS '영업시간 특이사항/공지';


--
-- Name: COLUMN companies.slug; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.slug IS 'SEO 친화적인 URL 식별자';


--
-- Name: COLUMN companies.address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.address IS '업체 주소';


--
-- Name: COLUMN companies.postal_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.postal_code IS '우편번호';


--
-- Name: COLUMN companies.latitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.latitude IS '위도';


--
-- Name: COLUMN companies.longitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.longitude IS '경도';


--
-- Name: COLUMN companies.view_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.view_count IS '조회수';


--
-- Name: COLUMN companies.like_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.like_count IS '좋아요 수';


--
-- Name: COLUMN companies.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.status IS '업체 상태 (ACTIVE, INACTIVE, SUSPENDED, PENDING)';


--
-- Name: COLUMN companies.featured; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.featured IS '추천 업체 여부';


--
-- Name: COLUMN companies.verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.verified IS '인증 업체 여부';


--
-- Name: COLUMN companies.premium_tier; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.premium_tier IS '프리미엄 등급 (NONE, BASIC, STANDARD, PREMIUM, VIP)';


--
-- Name: COLUMN companies.premium_monthly_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.premium_monthly_amount IS '월정액 금액 (원)';


--
-- Name: COLUMN companies.images; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.images IS 'Array of File IDs (FK to files.id) for company images';


--
-- Name: companies_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.companies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: companies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.companies_id_seq OWNED BY public.companies.id;


--
-- Name: company_certifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_certifications (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    cert_type character varying(100) NOT NULL,
    cert_name character varying(200) NOT NULL,
    cert_number character varying(100),
    issuer character varying(200),
    issue_date date,
    expiry_date date,
    is_verified boolean DEFAULT false,
    verified_at timestamp without time zone,
    verified_by bigint,
    document_url character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone
);


--
-- Name: TABLE company_certifications; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.company_certifications IS '업체 자격증 및 인증 테이블';


--
-- Name: COLUMN company_certifications.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.id IS '인증 정보 고유 ID';


--
-- Name: COLUMN company_certifications.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN company_certifications.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.company_id IS '업체 ID';


--
-- Name: COLUMN company_certifications.cert_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.cert_type IS '인증 유형';


--
-- Name: COLUMN company_certifications.cert_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.cert_name IS '인증/자격증명';


--
-- Name: COLUMN company_certifications.cert_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.cert_number IS '인증/자격증 번호';


--
-- Name: COLUMN company_certifications.issuer; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.issuer IS '발급 기관';


--
-- Name: COLUMN company_certifications.issue_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.issue_date IS '발급일';


--
-- Name: COLUMN company_certifications.expiry_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.expiry_date IS '만료일';


--
-- Name: COLUMN company_certifications.is_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.is_verified IS '검증 완료 여부';


--
-- Name: COLUMN company_certifications.verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.verified_at IS '검증 일시';


--
-- Name: COLUMN company_certifications.verified_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.verified_by IS '검증한 관리자 ID';


--
-- Name: COLUMN company_certifications.document_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.document_url IS '인증서 문서 URL';


--
-- Name: COLUMN company_certifications.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.created_at IS '등록 일시';


--
-- Name: COLUMN company_certifications.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.updated_at IS '수정 일시';


--
-- Name: COLUMN company_certifications.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.is_deleted IS '삭제 여부';


--
-- Name: COLUMN company_certifications.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_certifications.deleted_at IS '삭제 일시';


--
-- Name: company_certifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_certifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_certifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_certifications_id_seq OWNED BY public.company_certifications.id;


--
-- Name: company_filter_options; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_filter_options (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    filter_option_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE company_filter_options; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.company_filter_options IS '업체-필터 옵션 다대다 조인 테이블';


--
-- Name: COLUMN company_filter_options.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_filter_options.company_id IS '업체 ID (FK to companies)';


--
-- Name: COLUMN company_filter_options.filter_option_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_filter_options.filter_option_id IS '필터 옵션 ID (FK to filter_options)';


--
-- Name: company_filter_options_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_filter_options_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_filter_options_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_filter_options_id_seq OWNED BY public.company_filter_options.id;


--
-- Name: company_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_images (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    image_type character varying(50),
    title character varying(200),
    description text,
    width integer,
    height integer,
    file_size bigint,
    mime_type character varying(100),
    display_order integer DEFAULT 0,
    is_primary boolean DEFAULT false,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb,
    file_id bigint NOT NULL
);


--
-- Name: TABLE company_images; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.company_images IS '업체 이미지 관리 테이블 (복수 이미지)';


--
-- Name: COLUMN company_images.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.id IS '이미지 고유 ID';


--
-- Name: COLUMN company_images.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN company_images.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.company_id IS '업체 ID';


--
-- Name: COLUMN company_images.image_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.image_type IS '이미지 유형 (LOGO, COVER, GALLERY, INTERIOR, EXTERIOR, CERTIFICATE, PORTFOLIO)';


--
-- Name: COLUMN company_images.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.title IS '이미지 제목';


--
-- Name: COLUMN company_images.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.description IS '이미지 설명';


--
-- Name: COLUMN company_images.width; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.width IS '이미지 너비 (픽셀)';


--
-- Name: COLUMN company_images.height; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.height IS '이미지 높이 (픽셀)';


--
-- Name: COLUMN company_images.file_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.file_size IS '파일 크기 (바이트)';


--
-- Name: COLUMN company_images.mime_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.mime_type IS 'MIME 타입';


--
-- Name: COLUMN company_images.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.display_order IS '표시 순서';


--
-- Name: COLUMN company_images.is_primary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.is_primary IS '대표 이미지 여부';


--
-- Name: COLUMN company_images.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.is_active IS '활성 상태';


--
-- Name: COLUMN company_images.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.created_at IS '업로드 일시';


--
-- Name: COLUMN company_images.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.updated_at IS '수정 일시';


--
-- Name: COLUMN company_images.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.is_deleted IS '삭제 여부';


--
-- Name: COLUMN company_images.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.deleted_at IS '삭제 일시';


--
-- Name: COLUMN company_images.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.metadata IS '확장 메타데이터 (JSONB 형식)';


--
-- Name: COLUMN company_images.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_images.file_id IS 'FK to files.id for image file reference';


--
-- Name: company_images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_images_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_images_id_seq OWNED BY public.company_images.id;


--
-- Name: company_likes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_likes (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    user_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb
);


--
-- Name: TABLE company_likes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.company_likes IS '업체 좋아요';


--
-- Name: COLUMN company_likes.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_likes.company_id IS '업체 ID';


--
-- Name: COLUMN company_likes.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_likes.user_id IS '사용자 ID';


--
-- Name: company_likes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_likes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_likes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_likes_id_seq OWNED BY public.company_likes.id;


--
-- Name: company_portfolios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_portfolios (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    category character varying(100),
    project_type character varying(100),
    project_scale character varying(50),
    project_duration integer,
    project_date date,
    budget_range character varying(50),
    actual_cost numeric(12,2),
    images text[] DEFAULT '{}'::text[],
    videos text[] DEFAULT '{}'::text[],
    thumbnail_url character varying(500),
    tags text[] DEFAULT '{}'::text[],
    view_count integer DEFAULT 0,
    like_count integer DEFAULT 0,
    is_featured boolean DEFAULT false,
    is_public boolean DEFAULT true,
    display_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE company_portfolios; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.company_portfolios IS '업체 포트폴리오 테이블';


--
-- Name: COLUMN company_portfolios.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.id IS '포트폴리오 고유 ID';


--
-- Name: COLUMN company_portfolios.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN company_portfolios.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.company_id IS '업체 ID';


--
-- Name: COLUMN company_portfolios.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.title IS '포트폴리오 제목';


--
-- Name: COLUMN company_portfolios.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.description IS '포트폴리오 설명';


--
-- Name: COLUMN company_portfolios.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.category IS '포트폴리오 카테고리';


--
-- Name: COLUMN company_portfolios.project_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.project_type IS '프로젝트 유형';


--
-- Name: COLUMN company_portfolios.project_scale; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.project_scale IS '프로젝트 규모';


--
-- Name: COLUMN company_portfolios.project_duration; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.project_duration IS '프로젝트 기간 (일)';


--
-- Name: COLUMN company_portfolios.project_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.project_date IS '프로젝트 완료일';


--
-- Name: COLUMN company_portfolios.budget_range; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.budget_range IS '예산 범위';


--
-- Name: COLUMN company_portfolios.actual_cost; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.actual_cost IS '실제 비용';


--
-- Name: COLUMN company_portfolios.images; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.images IS '이미지 URL 목록 (배열)';


--
-- Name: COLUMN company_portfolios.videos; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.videos IS '동영상 URL 목록 (배열)';


--
-- Name: COLUMN company_portfolios.thumbnail_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.thumbnail_url IS '썸네일 이미지 URL';


--
-- Name: COLUMN company_portfolios.tags; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.tags IS '태그 (배열)';


--
-- Name: COLUMN company_portfolios.view_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.view_count IS '조회수';


--
-- Name: COLUMN company_portfolios.like_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.like_count IS '좋아요 수';


--
-- Name: COLUMN company_portfolios.is_featured; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.is_featured IS '대표 포트폴리오 여부';


--
-- Name: COLUMN company_portfolios.is_public; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.is_public IS '공개 여부';


--
-- Name: COLUMN company_portfolios.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.display_order IS '표시 순서';


--
-- Name: COLUMN company_portfolios.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.created_at IS '생성 일시';


--
-- Name: COLUMN company_portfolios.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.updated_at IS '수정 일시';


--
-- Name: COLUMN company_portfolios.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.is_deleted IS '삭제 여부';


--
-- Name: COLUMN company_portfolios.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.deleted_at IS '삭제 일시';


--
-- Name: COLUMN company_portfolios.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_portfolios.metadata IS '확장 데이터 (JSON)';


--
-- Name: company_portfolios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_portfolios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_portfolios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_portfolios_id_seq OWNED BY public.company_portfolios.id;


--
-- Name: company_review_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_review_images (
    id bigint NOT NULL,
    review_id bigint NOT NULL,
    file_id bigint NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE company_review_images; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.company_review_images IS '리뷰 이미지 관계 테이블 (File ID 기반)';


--
-- Name: COLUMN company_review_images.review_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_review_images.review_id IS '리뷰 ID (company_reviews FK)';


--
-- Name: COLUMN company_review_images.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_review_images.file_id IS '파일 ID (files FK) - URL이 아닌 File ID로 저장';


--
-- Name: COLUMN company_review_images.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_review_images.display_order IS '이미지 표시 순서';


--
-- Name: company_review_images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_review_images_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_review_images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_review_images_id_seq OWNED BY public.company_review_images.id;


--
-- Name: company_reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_reviews (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    user_id bigint NOT NULL,
    match_id bigint,
    rating numeric(2,1) NOT NULL,
    quality_rating integer,
    price_rating integer,
    service_rating integer,
    time_rating integer,
    title character varying(200),
    content text NOT NULL,
    images bigint[],
    reply text,
    replied_at timestamp without time zone,
    is_verified boolean DEFAULT false,
    is_reported boolean DEFAULT false,
    is_hidden boolean DEFAULT false,
    helpful_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    is_verified_purchase boolean DEFAULT false NOT NULL,
    like_count integer DEFAULT 0 NOT NULL,
    report_count integer DEFAULT 0 NOT NULL,
    metadata jsonb,
    status character varying(20) DEFAULT 'PUBLISHED'::character varying NOT NULL,
    CONSTRAINT company_reviews_price_rating_check CHECK (((price_rating >= 1) AND (price_rating <= 5))),
    CONSTRAINT company_reviews_quality_rating_check CHECK (((quality_rating >= 1) AND (quality_rating <= 5))),
    CONSTRAINT company_reviews_rating_check CHECK (((rating >= (1)::numeric) AND (rating <= (5)::numeric))),
    CONSTRAINT company_reviews_service_rating_check CHECK (((service_rating >= 1) AND (service_rating <= 5))),
    CONSTRAINT company_reviews_time_rating_check CHECK (((time_rating >= 1) AND (time_rating <= 5)))
);


--
-- Name: TABLE company_reviews; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.company_reviews IS '업체 리뷰 및 평점 테이블';


--
-- Name: COLUMN company_reviews.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.id IS '리뷰 고유 ID';


--
-- Name: COLUMN company_reviews.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN company_reviews.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.company_id IS '업체 ID';


--
-- Name: COLUMN company_reviews.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.user_id IS '리뷰 작성자 ID';


--
-- Name: COLUMN company_reviews.match_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.match_id IS '매칭 ID (매칭을 통한 리뷰인 경우)';


--
-- Name: COLUMN company_reviews.rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.rating IS '평점 (1.0 ~ 5.0)';


--
-- Name: COLUMN company_reviews.quality_rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.quality_rating IS '품질 평점 (1-5)';


--
-- Name: COLUMN company_reviews.price_rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.price_rating IS '가격 평점 (1-5)';


--
-- Name: COLUMN company_reviews.service_rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.service_rating IS '서비스 평점 (1-5)';


--
-- Name: COLUMN company_reviews.time_rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.time_rating IS '시간준수 평점 (1-5)';


--
-- Name: COLUMN company_reviews.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.title IS '리뷰 제목';


--
-- Name: COLUMN company_reviews.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.content IS '리뷰 내용';


--
-- Name: COLUMN company_reviews.images; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.images IS 'Array of File IDs (FK to files.id) for review images';


--
-- Name: COLUMN company_reviews.reply; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.reply IS '업체 답변';


--
-- Name: COLUMN company_reviews.replied_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.replied_at IS '업체 답변 작성 시각';


--
-- Name: COLUMN company_reviews.is_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.is_verified IS '인증된 리뷰 여부';


--
-- Name: COLUMN company_reviews.is_reported; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.is_reported IS '신고된 리뷰 여부';


--
-- Name: COLUMN company_reviews.is_hidden; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.is_hidden IS '숨김 처리 여부';


--
-- Name: COLUMN company_reviews.helpful_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.helpful_count IS '도움됨 수';


--
-- Name: COLUMN company_reviews.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.created_at IS '작성 일시';


--
-- Name: COLUMN company_reviews.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.updated_at IS '수정 일시';


--
-- Name: COLUMN company_reviews.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.is_deleted IS 'Soft delete 여부';


--
-- Name: COLUMN company_reviews.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.deleted_at IS 'Soft delete 시각';


--
-- Name: COLUMN company_reviews.is_verified_purchase; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.is_verified_purchase IS '실제 구매 인증 여부';


--
-- Name: COLUMN company_reviews.like_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.like_count IS '좋아요 수';


--
-- Name: COLUMN company_reviews.report_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.report_count IS '신고 수';


--
-- Name: COLUMN company_reviews.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.metadata IS '메타데이터 (JSONB)';


--
-- Name: COLUMN company_reviews.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.company_reviews.status IS '상태 (PUBLISHED, HIDDEN, REPORTED, DELETED)';


--
-- Name: company_reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_reviews_id_seq OWNED BY public.company_reviews.id;


--
-- Name: consultation_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.consultation_messages (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    consultation_type character varying(50) NOT NULL,
    consultation_id bigint NOT NULL,
    sender_id bigint,
    sender_type character varying(20) NOT NULL,
    message text NOT NULL,
    message_type character varying(20) DEFAULT 'REPLY'::character varying NOT NULL,
    attachments jsonb DEFAULT '[]'::jsonb,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    read_by bigint,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint
);


--
-- Name: TABLE consultation_messages; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.consultation_messages IS '상담 메시지 스레드 테이블 - 빠른상담/제휴문의 후속 메시지 관리';


--
-- Name: COLUMN consultation_messages.consultation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultation_messages.consultation_type IS 'Polymorphic 타입 (QUICK: 빠른상담, PARTNERSHIP: 제휴문의)';


--
-- Name: COLUMN consultation_messages.sender_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultation_messages.sender_type IS '발신자 유형 (ADMIN/COMPANY/USER/SYSTEM)';


--
-- Name: COLUMN consultation_messages.message_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultation_messages.message_type IS '메시지 유형 (REPLY: 답변, NOTE: 내부메모, SYSTEM: 시스템)';


--
-- Name: consultation_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.consultation_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: consultation_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.consultation_messages_id_seq OWNED BY public.consultation_messages.id;


--
-- Name: coupons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coupons (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    discount_type character varying(50),
    discount_value numeric(12,2) NOT NULL,
    max_discount_amount numeric(12,2),
    min_purchase_amount numeric(12,2),
    conditions jsonb DEFAULT '{}'::jsonb,
    total_quantity integer,
    used_quantity integer DEFAULT 0,
    valid_from timestamp without time zone,
    valid_to timestamp without time zone,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE coupons; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.coupons IS '쿠폰 마스터 테이블';


--
-- Name: COLUMN coupons.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.id IS '쿠폰 고유 ID';


--
-- Name: COLUMN coupons.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN coupons.code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.code IS '쿠폰 코드 (고유값)';


--
-- Name: COLUMN coupons.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.name IS '쿠폰명';


--
-- Name: COLUMN coupons.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.description IS '쿠폰 설명';


--
-- Name: COLUMN coupons.discount_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.discount_type IS '할인 유형 (FIXED:정액, PERCENTAGE:정률)';


--
-- Name: COLUMN coupons.discount_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.discount_value IS '할인 값';


--
-- Name: COLUMN coupons.max_discount_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.max_discount_amount IS '최대 할인 금액';


--
-- Name: COLUMN coupons.min_purchase_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.min_purchase_amount IS '최소 구매 금액';


--
-- Name: COLUMN coupons.conditions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.conditions IS '사용 조건 (JSON)';


--
-- Name: COLUMN coupons.total_quantity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.total_quantity IS '발행 수량';


--
-- Name: COLUMN coupons.used_quantity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.used_quantity IS '사용된 수량';


--
-- Name: COLUMN coupons.valid_from; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.valid_from IS '유효기간 시작';


--
-- Name: COLUMN coupons.valid_to; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.valid_to IS '유효기간 종료';


--
-- Name: COLUMN coupons.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.is_active IS '활성 상태';


--
-- Name: COLUMN coupons.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.created_at IS '생성 일시';


--
-- Name: COLUMN coupons.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coupons.updated_at IS '수정 일시';


--
-- Name: coupons_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.coupons_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: coupons_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.coupons_id_seq OWNED BY public.coupons.id;


--
-- Name: credit_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credit_transactions (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    credit_id bigint NOT NULL,
    transaction_type character varying(50),
    amount numeric(12,2) NOT NULL,
    balance_after numeric(12,2) NOT NULL,
    reason character varying(200),
    reference_type character varying(100),
    reference_id bigint,
    expires_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE credit_transactions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.credit_transactions IS '크레딧 거래 내역 테이블';


--
-- Name: COLUMN credit_transactions.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.id IS '거래 고유 ID';


--
-- Name: COLUMN credit_transactions.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN credit_transactions.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.user_id IS '사용자 ID';


--
-- Name: COLUMN credit_transactions.credit_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.credit_id IS '크레딧 정보 ID';


--
-- Name: COLUMN credit_transactions.transaction_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.transaction_type IS '거래 유형 (EARN:적립, USE:사용, REFUND:환불, EXPIRE:만료)';


--
-- Name: COLUMN credit_transactions.amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.amount IS '거래 금액';


--
-- Name: COLUMN credit_transactions.balance_after; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.balance_after IS '거래 후 잔액';


--
-- Name: COLUMN credit_transactions.reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.reason IS '거래 사유';


--
-- Name: COLUMN credit_transactions.reference_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.reference_type IS '참조 엔티티 타입';


--
-- Name: COLUMN credit_transactions.reference_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.reference_id IS '참조 엔티티 ID';


--
-- Name: COLUMN credit_transactions.expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.expires_at IS '만료일시';


--
-- Name: COLUMN credit_transactions.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credit_transactions.created_at IS '거래 일시';


--
-- Name: credit_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.credit_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: credit_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.credit_transactions_id_seq OWNED BY public.credit_transactions.id;


--
-- Name: credits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credits (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    balance numeric(12,2) DEFAULT 0 NOT NULL,
    total_earned numeric(12,2) DEFAULT 0,
    total_used numeric(12,2) DEFAULT 0,
    pending_amount numeric(12,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE credits; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.credits IS '크레딧/포인트 잔액 테이블';


--
-- Name: COLUMN credits.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.id IS '크레딧 정보 고유 ID';


--
-- Name: COLUMN credits.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN credits.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.user_id IS '사용자 ID';


--
-- Name: COLUMN credits.balance; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.balance IS '현재 잔액';


--
-- Name: COLUMN credits.total_earned; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.total_earned IS '총 적립액';


--
-- Name: COLUMN credits.total_used; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.total_used IS '총 사용액';


--
-- Name: COLUMN credits.pending_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.pending_amount IS '보류 중인 크레딧';


--
-- Name: COLUMN credits.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.created_at IS '생성 일시';


--
-- Name: COLUMN credits.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.credits.updated_at IS '수정 일시';


--
-- Name: credits_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.credits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: credits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.credits_id_seq OWNED BY public.credits.id;


--
-- Name: damoa_picks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.damoa_picks (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    pick_type character varying(20) NOT NULL,
    season character varying(50),
    title character varying(200) NOT NULL,
    description text,
    main_image_url character varying(500),
    banner_image_url character varying(500),
    images text[],
    badge_text character varying(50),
    badge_color character varying(20),
    start_date date NOT NULL,
    end_date date,
    display_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    view_count integer DEFAULT 0 NOT NULL,
    click_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE damoa_picks; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.damoa_picks IS '다모아 Pick - 시즌별 추천 업체 테이블 (V2 추가)';


--
-- Name: COLUMN damoa_picks.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.id IS 'Pick 고유 ID';


--
-- Name: COLUMN damoa_picks.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN damoa_picks.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.company_id IS '추천 업체 ID';


--
-- Name: COLUMN damoa_picks.pick_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.pick_type IS 'Pick 유형 (SPONSORED:후원, OPERATED:직영, PARTNER:파트너)';


--
-- Name: COLUMN damoa_picks.season; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.season IS '시즌 (예: 2024_SPRING, 2024_SUMMER)';


--
-- Name: COLUMN damoa_picks.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.title IS 'Pick 제목';


--
-- Name: COLUMN damoa_picks.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.description IS 'Pick 설명';


--
-- Name: COLUMN damoa_picks.main_image_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.main_image_url IS '메인 이미지 URL';


--
-- Name: COLUMN damoa_picks.banner_image_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.banner_image_url IS '배너 이미지 URL';


--
-- Name: COLUMN damoa_picks.images; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.images IS '추가 이미지 URL 배열';


--
-- Name: COLUMN damoa_picks.badge_text; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.badge_text IS '배지 텍스트 (예: 다모아 추천, 공식 파트너)';


--
-- Name: COLUMN damoa_picks.badge_color; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.badge_color IS '배지 색상 (HEX 코드)';


--
-- Name: COLUMN damoa_picks.start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.start_date IS '노출 시작일';


--
-- Name: COLUMN damoa_picks.end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.end_date IS '노출 종료일 (NULL이면 무기한)';


--
-- Name: COLUMN damoa_picks.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.display_order IS '표시 순서';


--
-- Name: COLUMN damoa_picks.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.is_active IS '활성 상태';


--
-- Name: COLUMN damoa_picks.view_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.view_count IS '조회수';


--
-- Name: COLUMN damoa_picks.click_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.click_count IS '클릭수';


--
-- Name: COLUMN damoa_picks.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.created_at IS '생성 일시';


--
-- Name: COLUMN damoa_picks.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.updated_at IS '수정 일시';


--
-- Name: COLUMN damoa_picks.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.is_deleted IS '삭제 여부';


--
-- Name: COLUMN damoa_picks.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.deleted_at IS '삭제 일시';


--
-- Name: COLUMN damoa_picks.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.damoa_picks.metadata IS '메타데이터 (JSONB)';


--
-- Name: damoa_picks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.damoa_picks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: damoa_picks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.damoa_picks_id_seq OWNED BY public.damoa_picks.id;


--
-- Name: email_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verifications (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    user_id bigint,
    verification_token character varying(255) NOT NULL,
    purpose character varying(50),
    is_verified boolean DEFAULT false,
    verified_at timestamp without time zone,
    expires_at timestamp without time zone NOT NULL,
    request_ip character varying(45),
    verified_ip character varying(45),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE email_verifications; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.email_verifications IS '이메일 인증 토큰 관리 테이블';


--
-- Name: COLUMN email_verifications.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.id IS '인증 고유 ID';


--
-- Name: COLUMN email_verifications.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN email_verifications.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.email IS '이메일 주소';


--
-- Name: COLUMN email_verifications.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.user_id IS '사용자 ID (선택)';


--
-- Name: COLUMN email_verifications.verification_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.verification_token IS '인증 토큰 (고유값)';


--
-- Name: COLUMN email_verifications.purpose; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.purpose IS '인증 목적 (SIGNUP, PASSWORD_RESET, EMAIL_CHANGE)';


--
-- Name: COLUMN email_verifications.is_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.is_verified IS '인증 완료 여부';


--
-- Name: COLUMN email_verifications.verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.verified_at IS '인증 완료 일시';


--
-- Name: COLUMN email_verifications.expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.expires_at IS '만료 일시';


--
-- Name: COLUMN email_verifications.request_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.request_ip IS '요청 IP 주소';


--
-- Name: COLUMN email_verifications.verified_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.verified_ip IS '인증 완료 IP 주소';


--
-- Name: COLUMN email_verifications.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.email_verifications.created_at IS '생성 일시';


--
-- Name: email_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.email_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: email_verifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.email_verifications_id_seq OWNED BY public.email_verifications.id;


--
-- Name: estimate_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estimate_messages (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    request_id bigint NOT NULL,
    proposal_id bigint,
    sender_id bigint NOT NULL,
    message text NOT NULL,
    attachments text[] DEFAULT '{}'::text[],
    is_read boolean DEFAULT false,
    read_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone
);


--
-- Name: TABLE estimate_messages; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.estimate_messages IS '견적 관련 메시지 테이블';


--
-- Name: COLUMN estimate_messages.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.id IS '메시지 고유 ID';


--
-- Name: COLUMN estimate_messages.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN estimate_messages.request_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.request_id IS '견적 요청 ID';


--
-- Name: COLUMN estimate_messages.proposal_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.proposal_id IS '견적 제안 ID';


--
-- Name: COLUMN estimate_messages.sender_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.sender_id IS '발신자 ID';


--
-- Name: COLUMN estimate_messages.message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.message IS '메시지 내용';


--
-- Name: COLUMN estimate_messages.attachments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.attachments IS '첨부파일 URL (배열)';


--
-- Name: COLUMN estimate_messages.is_read; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.is_read IS '읽음 여부';


--
-- Name: COLUMN estimate_messages.read_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.read_at IS '읽은 일시';


--
-- Name: COLUMN estimate_messages.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.created_at IS '발송 일시';


--
-- Name: COLUMN estimate_messages.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.is_deleted IS '삭제 여부';


--
-- Name: COLUMN estimate_messages.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_messages.deleted_at IS '삭제 일시';


--
-- Name: estimate_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estimate_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estimate_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estimate_messages_id_seq OWNED BY public.estimate_messages.id;


--
-- Name: estimate_proposal_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estimate_proposal_attachments (
    id bigint NOT NULL,
    estimate_proposal_id bigint NOT NULL,
    file_id bigint NOT NULL,
    file_type character varying(50),
    file_description text,
    display_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone
);


--
-- Name: TABLE estimate_proposal_attachments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.estimate_proposal_attachments IS '견적 제안 첨부파일 조인 테이블';


--
-- Name: COLUMN estimate_proposal_attachments.file_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposal_attachments.file_type IS '파일 타입 (DRAWING=도면, PHOTO=사진, DOCUMENT=문서, ESTIMATE=견적서)';


--
-- Name: COLUMN estimate_proposal_attachments.file_description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposal_attachments.file_description IS '파일 설명 (선택사항)';


--
-- Name: COLUMN estimate_proposal_attachments.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposal_attachments.display_order IS '표시 순서 (0부터 시작)';


--
-- Name: estimate_proposal_attachments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estimate_proposal_attachments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estimate_proposal_attachments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estimate_proposal_attachments_id_seq OWNED BY public.estimate_proposal_attachments.id;


--
-- Name: estimate_proposals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estimate_proposals (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    request_id bigint NOT NULL,
    company_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    description text NOT NULL,
    price numeric(12,2),
    pricing_details jsonb DEFAULT '{}'::jsonb,
    timeline jsonb DEFAULT '{}'::jsonb,
    status character varying(20) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    is_selected boolean DEFAULT false,
    selected_at timestamp without time zone,
    valid_until date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb,
    CONSTRAINT chk_estimate_proposal_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'VIEWED'::character varying, 'SELECTED'::character varying, 'REJECTED'::character varying, 'WITHDRAWN'::character varying])::text[])))
);


--
-- Name: TABLE estimate_proposals; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.estimate_proposals IS '견적 제안 테이블';


--
-- Name: COLUMN estimate_proposals.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.id IS '견적 제안 고유 ID';


--
-- Name: COLUMN estimate_proposals.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN estimate_proposals.request_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.request_id IS '견적 요청 ID';


--
-- Name: COLUMN estimate_proposals.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.company_id IS '제안 업체 ID';


--
-- Name: COLUMN estimate_proposals.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.title IS '제안 제목';


--
-- Name: COLUMN estimate_proposals.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.description IS '제안 설명';


--
-- Name: COLUMN estimate_proposals.price; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.price IS '제안 가격';


--
-- Name: COLUMN estimate_proposals.pricing_details; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.pricing_details IS '가격 상세 내역 (JSON)';


--
-- Name: COLUMN estimate_proposals.timeline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.timeline IS '작업 일정 (JSON)';


--
-- Name: COLUMN estimate_proposals.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.status IS '제안 상태 (DRAFT/SUBMITTED/VIEWED/SELECTED/REJECTED/WITHDRAWN)';


--
-- Name: COLUMN estimate_proposals.is_selected; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.is_selected IS '선택 여부';


--
-- Name: COLUMN estimate_proposals.selected_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.selected_at IS '선택 일시';


--
-- Name: COLUMN estimate_proposals.valid_until; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.valid_until IS '제안 유효기간';


--
-- Name: COLUMN estimate_proposals.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.created_at IS '생성 일시';


--
-- Name: COLUMN estimate_proposals.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.updated_at IS '수정 일시';


--
-- Name: COLUMN estimate_proposals.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.is_deleted IS '삭제 여부';


--
-- Name: COLUMN estimate_proposals.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.deleted_at IS '삭제 일시';


--
-- Name: COLUMN estimate_proposals.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_proposals.metadata IS '확장 데이터 (JSON)';


--
-- Name: estimate_proposals_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estimate_proposals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estimate_proposals_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estimate_proposals_id_seq OWNED BY public.estimate_proposals.id;


--
-- Name: estimate_request_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estimate_request_attachments (
    id bigint NOT NULL,
    estimate_request_id bigint NOT NULL,
    file_id bigint NOT NULL,
    file_type character varying(50),
    file_description text,
    display_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone
);


--
-- Name: TABLE estimate_request_attachments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.estimate_request_attachments IS '견적 요청 첨부파일 조인 테이블';


--
-- Name: COLUMN estimate_request_attachments.file_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_request_attachments.file_type IS '파일 타입 (DRAWING=도면, PHOTO=사진, DOCUMENT=문서, ESTIMATE=견적서)';


--
-- Name: COLUMN estimate_request_attachments.file_description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_request_attachments.file_description IS '파일 설명 (선택사항)';


--
-- Name: COLUMN estimate_request_attachments.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_request_attachments.display_order IS '표시 순서 (0부터 시작)';


--
-- Name: estimate_request_attachments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estimate_request_attachments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estimate_request_attachments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estimate_request_attachments_id_seq OWNED BY public.estimate_request_attachments.id;


--
-- Name: estimate_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estimate_requests (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    description text NOT NULL,
    category character varying(100),
    requirements jsonb DEFAULT '{}'::jsonb,
    budget_min numeric(12,2),
    budget_max numeric(12,2),
    desired_start_date date,
    desired_end_date date,
    location character varying(200),
    address character varying(500),
    latitude numeric(10,8),
    longitude numeric(11,8),
    images text[] DEFAULT '{}'::text[],
    tags text[] DEFAULT '{}'::text[],
    required_skills text[] DEFAULT '{}'::text[],
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    is_public boolean DEFAULT true,
    view_count integer DEFAULT 0,
    proposal_count integer DEFAULT 0,
    expires_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb,
    client_name character varying(200),
    business_type character varying(100),
    area_pyeong numeric(10,2),
    contact_name character varying(100),
    contact_phone character varying(20),
    submission_deadline timestamp without time zone,
    CONSTRAINT chk_estimate_request_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'IN_PROGRESS'::character varying, 'MATCHED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: TABLE estimate_requests; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.estimate_requests IS '견적 요청 테이블';


--
-- Name: COLUMN estimate_requests.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.id IS '견적 요청 고유 ID';


--
-- Name: COLUMN estimate_requests.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN estimate_requests.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.user_id IS '요청자 사용자 ID';


--
-- Name: COLUMN estimate_requests.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.title IS '견적 요청 제목';


--
-- Name: COLUMN estimate_requests.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.description IS '견적 요청 설명';


--
-- Name: COLUMN estimate_requests.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.category IS '카테고리';


--
-- Name: COLUMN estimate_requests.requirements; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.requirements IS '요구사항 상세 (JSON)';


--
-- Name: COLUMN estimate_requests.budget_min; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.budget_min IS '예산 최소값';


--
-- Name: COLUMN estimate_requests.budget_max; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.budget_max IS '예산 최대값';


--
-- Name: COLUMN estimate_requests.desired_start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.desired_start_date IS '희망 시작일';


--
-- Name: COLUMN estimate_requests.desired_end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.desired_end_date IS '희망 종료일';


--
-- Name: COLUMN estimate_requests.location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.location IS '작업 위치';


--
-- Name: COLUMN estimate_requests.address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.address IS '상세 주소';


--
-- Name: COLUMN estimate_requests.latitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.latitude IS '위도';


--
-- Name: COLUMN estimate_requests.longitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.longitude IS '경도';


--
-- Name: COLUMN estimate_requests.images; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.images IS '첨부 이미지 URL (배열)';


--
-- Name: COLUMN estimate_requests.tags; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.tags IS '태그 (배열)';


--
-- Name: COLUMN estimate_requests.required_skills; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.required_skills IS '필요 기술/자격 (배열)';


--
-- Name: COLUMN estimate_requests.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.status IS '견적 요청 상태 (DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED, CANCELLED)';


--
-- Name: COLUMN estimate_requests.is_public; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.is_public IS '공개 여부';


--
-- Name: COLUMN estimate_requests.view_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.view_count IS '조회수';


--
-- Name: COLUMN estimate_requests.proposal_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.proposal_count IS '받은 제안 수';


--
-- Name: COLUMN estimate_requests.expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.expires_at IS '만료일시';


--
-- Name: COLUMN estimate_requests.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.created_at IS '생성 일시';


--
-- Name: COLUMN estimate_requests.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.updated_at IS '수정 일시';


--
-- Name: COLUMN estimate_requests.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.is_deleted IS '삭제 여부';


--
-- Name: COLUMN estimate_requests.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.deleted_at IS '삭제 일시';


--
-- Name: COLUMN estimate_requests.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.metadata IS '확장 데이터 (JSON)';


--
-- Name: COLUMN estimate_requests.client_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.client_name IS '사업장명 또는 고객명 (병원, 카페, 사무실 등)';


--
-- Name: COLUMN estimate_requests.business_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.business_type IS '업종 분류 (의료, 카페, 사무실, 매장, 주거 등)';


--
-- Name: COLUMN estimate_requests.area_pyeong; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.area_pyeong IS '시공 면적 (평수)';


--
-- Name: COLUMN estimate_requests.contact_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.contact_name IS '견적 요청자 이름';


--
-- Name: COLUMN estimate_requests.contact_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.contact_phone IS '견적 요청자 연락처';


--
-- Name: COLUMN estimate_requests.submission_deadline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_requests.submission_deadline IS '업체 제안서 제출 마감일시';


--
-- Name: estimate_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estimate_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estimate_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estimate_requests_id_seq OWNED BY public.estimate_requests.id;


--
-- Name: estimate_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estimate_templates (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    category character varying(100),
    description text,
    template_data jsonb DEFAULT '{}'::jsonb,
    use_count integer DEFAULT 0,
    is_public boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone
);


--
-- Name: TABLE estimate_templates; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.estimate_templates IS '견적 템플릿 테이블';


--
-- Name: COLUMN estimate_templates.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.id IS '템플릿 고유 ID';


--
-- Name: COLUMN estimate_templates.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN estimate_templates.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.company_id IS '업체 ID';


--
-- Name: COLUMN estimate_templates.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.name IS '템플릿명';


--
-- Name: COLUMN estimate_templates.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.category IS '템플릿 카테고리';


--
-- Name: COLUMN estimate_templates.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.description IS '템플릿 설명';


--
-- Name: COLUMN estimate_templates.template_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.template_data IS '템플릿 데이터 (JSON)';


--
-- Name: COLUMN estimate_templates.use_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.use_count IS '사용 횟수';


--
-- Name: COLUMN estimate_templates.is_public; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.is_public IS '공개 여부';


--
-- Name: COLUMN estimate_templates.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.created_at IS '생성 일시';


--
-- Name: COLUMN estimate_templates.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.updated_at IS '수정 일시';


--
-- Name: COLUMN estimate_templates.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.is_deleted IS '삭제 여부';


--
-- Name: COLUMN estimate_templates.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.estimate_templates.deleted_at IS '삭제 일시';


--
-- Name: estimate_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estimate_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estimate_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estimate_templates_id_seq OWNED BY public.estimate_templates.id;


--
-- Name: file_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.file_attachments (
    id bigint NOT NULL,
    file_id bigint NOT NULL,
    entity_type character varying(100) NOT NULL,
    entity_id bigint NOT NULL,
    attachment_type character varying(50),
    display_order integer DEFAULT 0,
    caption character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE file_attachments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.file_attachments IS '파일-엔티티 연결 관리 테이블';


--
-- Name: COLUMN file_attachments.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.id IS '연결 고유 ID';


--
-- Name: COLUMN file_attachments.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.file_id IS '파일 ID';


--
-- Name: COLUMN file_attachments.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.entity_type IS '연결 엔티티 타입';


--
-- Name: COLUMN file_attachments.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.entity_id IS '연결 엔티티 ID';


--
-- Name: COLUMN file_attachments.attachment_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.attachment_type IS '첨부 유형 (MAIN, THUMBNAIL, ATTACHMENT)';


--
-- Name: COLUMN file_attachments.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.display_order IS '표시 순서';


--
-- Name: COLUMN file_attachments.caption; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.caption IS '캡션/설명';


--
-- Name: COLUMN file_attachments.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_attachments.created_at IS '연결 일시';


--
-- Name: file_attachments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.file_attachments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: file_attachments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.file_attachments_id_seq OWNED BY public.file_attachments.id;


--
-- Name: file_downloads; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.file_downloads (
    id bigint NOT NULL,
    file_id bigint NOT NULL,
    user_id bigint,
    ip_address character varying(45),
    user_agent text,
    referer text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE file_downloads; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.file_downloads IS '파일 다운로드 로그 테이블';


--
-- Name: COLUMN file_downloads.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_downloads.id IS '다운로드 로그 ID';


--
-- Name: COLUMN file_downloads.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_downloads.file_id IS '파일 ID';


--
-- Name: COLUMN file_downloads.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_downloads.user_id IS '다운로드한 사용자 ID';


--
-- Name: COLUMN file_downloads.ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_downloads.ip_address IS 'IP 주소';


--
-- Name: COLUMN file_downloads.user_agent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_downloads.user_agent IS '브라우저/클라이언트 정보';


--
-- Name: COLUMN file_downloads.referer; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_downloads.referer IS '참조 URL';


--
-- Name: COLUMN file_downloads.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_downloads.created_at IS '다운로드 일시';


--
-- Name: file_downloads_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.file_downloads_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: file_downloads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.file_downloads_id_seq OWNED BY public.file_downloads.id;


--
-- Name: files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.files (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    original_filename character varying(500) NOT NULL,
    stored_filename character varying(500) NOT NULL,
    file_path character varying(1000) NOT NULL,
    file_url character varying(1000) NOT NULL,
    file_size bigint NOT NULL,
    mime_type character varying(100),
    file_extension character varying(20),
    uploader_id bigint,
    category_id bigint,
    entity_type character varying(50),
    entity_id bigint,
    display_order integer,
    is_public boolean DEFAULT false NOT NULL,
    download_count bigint DEFAULT 0 NOT NULL,
    image_metadata jsonb,
    metadata jsonb DEFAULT '{}'::jsonb,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone
);


--
-- Name: TABLE files; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.files IS '파일 관리 (Presigned URL 업로드)';


--
-- Name: COLUMN files.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.files.entity_type IS '연관 엔티티 타입 (USER_PROFILE, COMPANY_IMAGE, PORTFOLIO, etc.)';


--
-- Name: COLUMN files.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.files.entity_id IS '연관 엔티티 ID';


--
-- Name: COLUMN files.image_metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.files.image_metadata IS '이미지 메타데이터 (width, height 등)';


--
-- Name: files_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: files_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.files_id_seq OWNED BY public.files.id;


--
-- Name: filter_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.filter_categories (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    entity_type character varying(100) NOT NULL,
    filter_type character varying(50) NOT NULL,
    supports_hierarchy boolean DEFAULT false NOT NULL,
    max_depth integer DEFAULT 1 NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    icon character varying(100),
    is_active boolean DEFAULT true NOT NULL,
    is_required boolean DEFAULT false NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone
);


--
-- Name: TABLE filter_categories; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.filter_categories IS '필터 카테고리 정의 테이블 (지역, 진료과, 전문영역 등)';


--
-- Name: COLUMN filter_categories.code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_categories.code IS '필터 카테고리 코드 (고유값: region, department, specialty 등)';


--
-- Name: COLUMN filter_categories.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_categories.name IS '필터 카테고리명 (사용자에게 표시)';


--
-- Name: COLUMN filter_categories.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_categories.entity_type IS '적용 대상 엔티티 (COMPANY, HOSPITAL, SERVICE 등)';


--
-- Name: COLUMN filter_categories.filter_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_categories.filter_type IS '필터 선택 방식 (SINGLE_SELECT, MULTI_SELECT, HIERARCHICAL)';


--
-- Name: COLUMN filter_categories.supports_hierarchy; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_categories.supports_hierarchy IS '계층 구조 지원 여부';


--
-- Name: COLUMN filter_categories.max_depth; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_categories.max_depth IS '최대 계층 깊이 (1=flat, 2=2단계, 3=3단계)';


--
-- Name: filter_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.filter_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: filter_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.filter_categories_id_seq OWNED BY public.filter_categories.id;


--
-- Name: filter_option_relations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.filter_option_relations (
    id bigint NOT NULL,
    source_option_id bigint NOT NULL,
    target_option_id bigint NOT NULL,
    relation_type character varying(50) NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE filter_option_relations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.filter_option_relations IS '필터 옵션 간 관계 테이블 (다대다 관계 등)';


--
-- Name: COLUMN filter_option_relations.relation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_option_relations.relation_type IS '관계 유형 (RELATED_TO, INCLUDES, EQUIVALENT)';


--
-- Name: filter_option_relations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.filter_option_relations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: filter_option_relations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.filter_option_relations_id_seq OWNED BY public.filter_option_relations.id;


--
-- Name: filter_options; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.filter_options (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    category_id bigint NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(200) NOT NULL,
    short_name character varying(100),
    description text,
    parent_id bigint,
    depth integer DEFAULT 0 NOT NULL,
    path character varying(500),
    metadata jsonb DEFAULT '{}'::jsonb,
    display_order integer DEFAULT 0 NOT NULL,
    icon character varying(100),
    color character varying(20),
    is_active boolean DEFAULT true NOT NULL,
    is_default boolean DEFAULT false NOT NULL,
    usage_count integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone
);


--
-- Name: TABLE filter_options; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.filter_options IS '필터 옵션 테이블 (개별 CRUD 가능, 계층 구조 지원)';


--
-- Name: COLUMN filter_options.category_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.category_id IS '필터 카테고리 FK';


--
-- Name: COLUMN filter_options.code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.code IS '옵션 코드 (카테고리 내 고유값)';


--
-- Name: COLUMN filter_options.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.name IS '옵션명 (사용자에게 표시)';


--
-- Name: COLUMN filter_options.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.parent_id IS '부모 옵션 ID (계층 구조용, NULL=최상위)';


--
-- Name: COLUMN filter_options.depth; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.depth IS '계층 깊이 (0=최상위, 1=2단계, 2=3단계)';


--
-- Name: COLUMN filter_options.path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.path IS '전체 경로 (검색 최적화용, 예: /seoul/gangnam-gu/yeoksam-dong)';


--
-- Name: COLUMN filter_options.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.metadata IS '확장 메타데이터 (좌표, 지역코드 등 JSONB)';


--
-- Name: COLUMN filter_options.usage_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.filter_options.usage_count IS '사용 횟수 (인기 옵션 파악용)';


--
-- Name: filter_options_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.filter_options_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: filter_options_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.filter_options_id_seq OWNED BY public.filter_options.id;




--
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoices (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    payment_id bigint,
    user_id bigint NOT NULL,
    company_id bigint,
    invoice_number character varying(100) NOT NULL,
    invoice_type character varying(50),
    supply_amount numeric(12,2) NOT NULL,
    tax_amount numeric(12,2) NOT NULL,
    total_amount numeric(12,2) NOT NULL,
    issue_date date NOT NULL,
    due_date date,
    supplier_info jsonb DEFAULT '{}'::jsonb,
    buyer_info jsonb DEFAULT '{}'::jsonb,
    items jsonb DEFAULT '[]'::jsonb,
    status character varying(50) DEFAULT 'ISSUED'::character varying,
    pdf_url character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE invoices; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.invoices IS '세금계산서/계산서 테이블';


--
-- Name: COLUMN invoices.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.id IS '계산서 고유 ID';


--
-- Name: COLUMN invoices.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN invoices.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.payment_id IS '결제 ID';


--
-- Name: COLUMN invoices.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.user_id IS '사용자 ID';


--
-- Name: COLUMN invoices.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.company_id IS '업체 ID';


--
-- Name: COLUMN invoices.invoice_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.invoice_number IS '계산서 번호 (고유값)';


--
-- Name: COLUMN invoices.invoice_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.invoice_type IS '계산서 유형 (TAX:세금계산서, CASH_RECEIPT:현금영수증)';


--
-- Name: COLUMN invoices.supply_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.supply_amount IS '공급가액';


--
-- Name: COLUMN invoices.tax_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.tax_amount IS '세액';


--
-- Name: COLUMN invoices.total_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.total_amount IS '합계 금액';


--
-- Name: COLUMN invoices.issue_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.issue_date IS '발행일';


--
-- Name: COLUMN invoices.due_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.due_date IS '만기일';


--
-- Name: COLUMN invoices.supplier_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.supplier_info IS '공급자 정보 (JSON)';


--
-- Name: COLUMN invoices.buyer_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.buyer_info IS '공급받는자 정보 (JSON)';


--
-- Name: COLUMN invoices.items; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.items IS '품목 리스트 (JSON 배열)';


--
-- Name: COLUMN invoices.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.status IS '계산서 상태 (ISSUED, SENT, CANCELLED)';


--
-- Name: COLUMN invoices.pdf_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.pdf_url IS 'PDF 파일 URL';


--
-- Name: COLUMN invoices.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.created_at IS '생성 일시';


--
-- Name: COLUMN invoices.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.invoices.updated_at IS '수정 일시';


--
-- Name: invoices_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.invoices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: invoices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.invoices_id_seq OWNED BY public.invoices.id;


--
-- Name: match_reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.match_reviews (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    match_id bigint NOT NULL,
    reviewer_id bigint NOT NULL,
    reviewee_id bigint NOT NULL,
    review_type character varying(50),
    rating integer NOT NULL,
    content text,
    is_public boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    CONSTRAINT match_reviews_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


--
-- Name: TABLE match_reviews; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.match_reviews IS '매칭 후기 테이블';


--
-- Name: COLUMN match_reviews.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.id IS '후기 고유 ID';


--
-- Name: COLUMN match_reviews.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN match_reviews.match_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.match_id IS '매칭 ID';


--
-- Name: COLUMN match_reviews.reviewer_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.reviewer_id IS '리뷰 작성자 ID';


--
-- Name: COLUMN match_reviews.reviewee_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.reviewee_id IS '리뷰 대상자 ID';


--
-- Name: COLUMN match_reviews.review_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.review_type IS '리뷰 타입 (USER_TO_COMPANY, COMPANY_TO_USER)';


--
-- Name: COLUMN match_reviews.rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.rating IS '평점 (1-5)';


--
-- Name: COLUMN match_reviews.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.content IS '후기 내용';


--
-- Name: COLUMN match_reviews.is_public; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.is_public IS '공개 여부';


--
-- Name: COLUMN match_reviews.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.created_at IS '작성 일시';


--
-- Name: COLUMN match_reviews.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.updated_at IS '수정 일시';


--
-- Name: COLUMN match_reviews.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.is_deleted IS '삭제 여부';


--
-- Name: COLUMN match_reviews.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.match_reviews.deleted_at IS '삭제 일시';


--
-- Name: match_reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.match_reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: match_reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.match_reviews_id_seq OWNED BY public.match_reviews.id;


--
-- Name: matches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.matches (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    request_id bigint NOT NULL,
    proposal_id bigint NOT NULL,
    user_id bigint NOT NULL,
    company_id bigint NOT NULL,
    contract_amount numeric(12,2) NOT NULL,
    contract_terms jsonb DEFAULT '{}'::jsonb,
    start_date date,
    end_date date,
    actual_start_date date,
    actual_end_date date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    completed_at timestamp without time zone,
    completion_notes text,
    cancelled_at timestamp without time zone,
    cancellation_reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb,
    CONSTRAINT chk_match_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'DISPUTED'::character varying])::text[])))
);


--
-- Name: TABLE matches; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.matches IS '매칭 확정 정보 테이블';


--
-- Name: COLUMN matches.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.id IS '매칭 고유 ID';


--
-- Name: COLUMN matches.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN matches.request_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.request_id IS '견적 요청 ID';


--
-- Name: COLUMN matches.proposal_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.proposal_id IS '선택된 견적 제안 ID';


--
-- Name: COLUMN matches.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.user_id IS '고객 사용자 ID';


--
-- Name: COLUMN matches.company_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.company_id IS '업체 ID';


--
-- Name: COLUMN matches.contract_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.contract_amount IS '계약 금액';


--
-- Name: COLUMN matches.contract_terms; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.contract_terms IS '계약 조건 (JSON)';


--
-- Name: COLUMN matches.start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.start_date IS '계약 시작일';


--
-- Name: COLUMN matches.end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.end_date IS '계약 종료일';


--
-- Name: COLUMN matches.actual_start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.actual_start_date IS '실제 시작일';


--
-- Name: COLUMN matches.actual_end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.actual_end_date IS '실제 종료일';


--
-- Name: COLUMN matches.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.status IS '매칭 상태 (ACTIVE, COMPLETED, CANCELLED, DISPUTED)';


--
-- Name: COLUMN matches.completed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.completed_at IS '완료 일시';


--
-- Name: COLUMN matches.completion_notes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.completion_notes IS '완료 메모';


--
-- Name: COLUMN matches.cancelled_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.cancelled_at IS '취소 일시';


--
-- Name: COLUMN matches.cancellation_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.cancellation_reason IS '취소 사유';


--
-- Name: COLUMN matches.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.created_at IS '매칭 성사 일시';


--
-- Name: COLUMN matches.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.updated_at IS '정보 수정 일시';


--
-- Name: COLUMN matches.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.is_deleted IS '삭제 여부';


--
-- Name: COLUMN matches.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.deleted_at IS '삭제 일시';


--
-- Name: COLUMN matches.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.matches.metadata IS '확장 데이터 (JSON)';


--
-- Name: matches_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.matches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matches_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.matches_id_seq OWNED BY public.matches.id;


--
-- Name: notification_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_logs (
    id bigint NOT NULL,
    notification_id bigint,
    channel VARCHAR(50) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'KAKAO', 'IN_APP')),
    recipient character varying(255) NOT NULL,
    status character varying(50) NOT NULL,
    provider character varying(100),
    provider_message_id character varying(255),
    response_data jsonb DEFAULT '{}'::jsonb,
    error_code character varying(100),
    error_message text,
    cost numeric(10,4) DEFAULT 0,
    sent_at timestamp without time zone,
    delivered_at timestamp without time zone,
    failed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE notification_logs; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.notification_logs IS '알림 발송 로그 테이블';


--
-- Name: COLUMN notification_logs.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.id IS '로그 고유 ID';


--
-- Name: COLUMN notification_logs.notification_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.notification_id IS '알림 ID';


--
-- Name: COLUMN notification_logs.channel; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.channel IS '발송 채널';


--
-- Name: COLUMN notification_logs.recipient; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.recipient IS '수신자 (이메일, 전화번호 등)';


--
-- Name: COLUMN notification_logs.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.status IS '발송 상태 (PENDING, SENT, FAILED, BOUNCED)';


--
-- Name: COLUMN notification_logs.provider; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.provider IS '외부 발송 서비스 (AWS SES, Twilio, FCM 등)';


--
-- Name: COLUMN notification_logs.provider_message_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.provider_message_id IS '외부 서비스 메시지 ID';


--
-- Name: COLUMN notification_logs.response_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.response_data IS '외부 서비스 응답 데이터 (JSON)';


--
-- Name: COLUMN notification_logs.error_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.error_code IS '에러 코드';


--
-- Name: COLUMN notification_logs.error_message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.error_message IS '에러 메시지';


--
-- Name: COLUMN notification_logs.cost; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.cost IS '발송 비용';


--
-- Name: COLUMN notification_logs.sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.sent_at IS '발송 시도 일시';


--
-- Name: COLUMN notification_logs.delivered_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.delivered_at IS '도착 확인 일시';


--
-- Name: COLUMN notification_logs.failed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.failed_at IS '실패 일시';


--
-- Name: COLUMN notification_logs.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_logs.created_at IS '로그 생성 일시';


--
-- Name: notification_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notification_logs_id_seq OWNED BY public.notification_logs.id;


--
-- Name: notification_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_settings (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    email_enabled boolean DEFAULT true,
    sms_enabled boolean DEFAULT true,
    push_enabled boolean DEFAULT true,
    kakao_enabled boolean DEFAULT false,
    preferences jsonb DEFAULT '{}'::jsonb,
    do_not_disturb_start time without time zone,
    do_not_disturb_end time without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE notification_settings; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.notification_settings IS '사용자별 알림 수신 설정 테이블';


--
-- Name: COLUMN notification_settings.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.id IS '설정 고유 ID';


--
-- Name: COLUMN notification_settings.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN notification_settings.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.user_id IS '사용자 ID';


--
-- Name: COLUMN notification_settings.email_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.email_enabled IS '이메일 수신 동의';


--
-- Name: COLUMN notification_settings.sms_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.sms_enabled IS 'SMS 수신 동의';


--
-- Name: COLUMN notification_settings.push_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.push_enabled IS '푸시 알림 수신 동의';


--
-- Name: COLUMN notification_settings.kakao_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.kakao_enabled IS '카카오톡 수신 동의';


--
-- Name: COLUMN notification_settings.preferences; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.preferences IS '알림 타입별 상세 설정 (JSON)';


--
-- Name: COLUMN notification_settings.do_not_disturb_start; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.do_not_disturb_start IS '방해금지 시작 시간';


--
-- Name: COLUMN notification_settings.do_not_disturb_end; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.do_not_disturb_end IS '방해금지 종료 시간';


--
-- Name: COLUMN notification_settings.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.created_at IS '생성 일시';


--
-- Name: COLUMN notification_settings.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_settings.updated_at IS '수정 일시';


--
-- Name: notification_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notification_settings_id_seq OWNED BY public.notification_settings.id;


--
-- Name: notification_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_templates (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(200) NOT NULL,
    code character varying(100) NOT NULL,
    channel VARCHAR(50) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'KAKAO', 'IN_APP')),
    title_template character varying(500),
    content_template text NOT NULL,
    variables jsonb DEFAULT '{}'::jsonb,
    kakao_template_code character varying(100),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE notification_templates; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.notification_templates IS '알림 템플릿 테이블';


--
-- Name: COLUMN notification_templates.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.id IS '템플릿 고유 ID';


--
-- Name: COLUMN notification_templates.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN notification_templates.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.name IS '템플릿명';


--
-- Name: COLUMN notification_templates.code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.code IS '템플릿 코드 (고유값)';


--
-- Name: COLUMN notification_templates.channel; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.channel IS '알림 채널';


--
-- Name: COLUMN notification_templates.title_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.title_template IS '제목 템플릿';


--
-- Name: COLUMN notification_templates.content_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.content_template IS '내용 템플릿';


--
-- Name: COLUMN notification_templates.variables; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.variables IS '템플릿 변수 정의 (JSON)';


--
-- Name: COLUMN notification_templates.kakao_template_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.kakao_template_code IS '카카오 알림톡 템플릿 코드';


--
-- Name: COLUMN notification_templates.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.is_active IS '활성 상태';


--
-- Name: COLUMN notification_templates.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.created_at IS '생성 일시';


--
-- Name: COLUMN notification_templates.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notification_templates.updated_at IS '수정 일시';


--
-- Name: notification_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notification_templates_id_seq OWNED BY public.notification_templates.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    notification_type character varying(100) NOT NULL,
    channel VARCHAR(50) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'KAKAO', 'IN_APP')),
    title character varying(200),
    content text NOT NULL,
    link_url character varying(500),
    action_type character varying(100),
    action_data jsonb DEFAULT '{}'::jsonb,
    is_sent boolean DEFAULT false,
    sent_at timestamp without time zone,
    send_error text,
    is_read boolean DEFAULT false,
    read_at timestamp without time zone,
    scheduled_at timestamp without time zone,
    template_id bigint,
    template_data jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE notifications; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.notifications IS '통합 알림 관리 테이블';


--
-- Name: COLUMN notifications.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.id IS '알림 고유 ID';


--
-- Name: COLUMN notifications.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN notifications.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.user_id IS '수신자 ID';


--
-- Name: COLUMN notifications.notification_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.notification_type IS '알림 유형';


--
-- Name: COLUMN notifications.channel; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.channel IS '알림 채널 (EMAIL, SMS, PUSH, KAKAO, IN_APP)';


--
-- Name: COLUMN notifications.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.title IS '알림 제목';


--
-- Name: COLUMN notifications.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.content IS '알림 내용';


--
-- Name: COLUMN notifications.link_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.link_url IS '연결 URL';


--
-- Name: COLUMN notifications.action_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.action_type IS '액션 타입';


--
-- Name: COLUMN notifications.action_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.action_data IS '액션 데이터 (JSON)';


--
-- Name: COLUMN notifications.is_sent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.is_sent IS '발송 여부';


--
-- Name: COLUMN notifications.sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.sent_at IS '발송 일시';


--
-- Name: COLUMN notifications.send_error; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.send_error IS '발송 오류 메시지';


--
-- Name: COLUMN notifications.is_read; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.is_read IS '읽음 여부';


--
-- Name: COLUMN notifications.read_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.read_at IS '읽은 일시';


--
-- Name: COLUMN notifications.scheduled_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.scheduled_at IS '예약 발송 일시';


--
-- Name: COLUMN notifications.template_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.template_id IS '템플릿 ID';


--
-- Name: COLUMN notifications.template_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.template_data IS '템플릿 변수 데이터 (JSON)';


--
-- Name: COLUMN notifications.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.created_at IS '생성 일시';


--
-- Name: COLUMN notifications.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.notifications.updated_at IS '수정 일시';


--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: partnership_inquiries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.partnership_inquiries (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint,
    company_id bigint,
    inquiry_type character varying(50) NOT NULL,
    company_name character varying(200) NOT NULL,
    contact_person character varying(100) NOT NULL,
    "position" character varying(100),
    phone character varying(20) NOT NULL,
    email character varying(255) NOT NULL,
    website_url character varying(500),
    subject character varying(200) NOT NULL,
    message text NOT NULL,
    attachment_files jsonb DEFAULT '[]'::jsonb,
    budget_range character varying(50),
    preferred_ad_type character varying(50),
    expected_duration character varying(50),
    assigned_admin_id bigint,
    assigned_at timestamp with time zone,
    status character varying(50) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    priority character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    response_message text,
    responded_at timestamp with time zone,
    responded_by bigint,
    completed_at timestamp with time zone,
    rejection_reason text,
    admin_notes text,
    ip_address inet,
    user_agent text,
    metadata jsonb DEFAULT '{}'::jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint
);


--
-- Name: TABLE partnership_inquiries; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.partnership_inquiries IS '제휴/광고 문의 테이블 - 제휴, 광고, 후원 등 사업적 문의 관리';


--
-- Name: COLUMN partnership_inquiries.inquiry_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.partnership_inquiries.inquiry_type IS '문의 유형 (PARTNERSHIP/ADVERTISING/SPONSORSHIP/BUSINESS/OTHER)';


--
-- Name: COLUMN partnership_inquiries.budget_range; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.partnership_inquiries.budget_range IS '예산 범위 (UNDER_1M/1M_5M/5M_10M/OVER_10M)';


--
-- Name: COLUMN partnership_inquiries.preferred_ad_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.partnership_inquiries.preferred_ad_type IS '선호 광고 타입 (LISTING/BANNER/POPUP/AI_RECOMMENDATION/DAMOA_PICK)';


--
-- Name: COLUMN partnership_inquiries.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.partnership_inquiries.status IS '문의 상태 (SUBMITTED/IN_REVIEW/RESPONDED/IN_NEGOTIATION/ACCEPTED/REJECTED/COMPLETED)';


--
-- Name: COLUMN partnership_inquiries.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.partnership_inquiries.priority IS '우선순위 (LOW/NORMAL/HIGH/URGENT)';


--
-- Name: partnership_inquiries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.partnership_inquiries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: partnership_inquiries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.partnership_inquiries_id_seq OWNED BY public.partnership_inquiries.id;


--
-- Name: payment_methods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_methods (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    method_type VARCHAR(50) NOT NULL CHECK (method_type IN ('CARD', 'BANK_TRANSFER', 'VIRTUAL_ACCOUNT', 'PHONE', 'KAKAO_PAY', 'NAVER_PAY', 'TOSS', 'CREDIT')),
    is_default boolean DEFAULT false,
    card_info jsonb DEFAULT '{}'::jsonb,
    bank_info jsonb DEFAULT '{}'::jsonb,
    is_active boolean DEFAULT true,
    verified_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE payment_methods; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.payment_methods IS '저장된 결제 수단 테이블';


--
-- Name: COLUMN payment_methods.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.id IS '결제 수단 고유 ID';


--
-- Name: COLUMN payment_methods.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN payment_methods.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.user_id IS '사용자 ID';


--
-- Name: COLUMN payment_methods.method_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.method_type IS '결제 수단 유형';


--
-- Name: COLUMN payment_methods.is_default; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.is_default IS '기본 결제 수단 여부';


--
-- Name: COLUMN payment_methods.card_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.card_info IS '카드 정보 (암호화된 JSON)';


--
-- Name: COLUMN payment_methods.bank_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.bank_info IS '계좌 정보 (JSON)';


--
-- Name: COLUMN payment_methods.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.is_active IS '활성 상태';


--
-- Name: COLUMN payment_methods.verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.verified_at IS '인증 완료 일시';


--
-- Name: COLUMN payment_methods.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.created_at IS '등록 일시';


--
-- Name: COLUMN payment_methods.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payment_methods.updated_at IS '수정 일시';


--
-- Name: payment_methods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payment_methods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payment_methods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payment_methods_id_seq OWNED BY public.payment_methods.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    payment_type character varying(50),
    payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'VIRTUAL_ACCOUNT', 'PHONE', 'KAKAO_PAY', 'NAVER_PAY', 'TOSS', 'CREDIT')),
    amount numeric(12,2) NOT NULL,
    tax_amount numeric(12,2) DEFAULT 0,
    discount_amount numeric(12,2) DEFAULT 0,
    final_amount numeric(12,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIAL_REFUNDED')),
    pg_provider character varying(50),
    pg_tid character varying(255),
    pg_response jsonb DEFAULT '{}'::jsonb,
    payment_data jsonb DEFAULT '{}'::jsonb,
    paid_at timestamp without time zone,
    receipt_url character varying(500),
    is_refundable boolean DEFAULT true,
    refunded_amount numeric(12,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE payments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.payments IS '결제 정보 테이블';


--
-- Name: COLUMN payments.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.id IS '결제 고유 ID';


--
-- Name: COLUMN payments.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN payments.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.user_id IS '결제자 ID';


--
-- Name: COLUMN payments.payment_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.payment_type IS '결제 유형 (ORDER, SUBSCRIPTION, AD, CREDIT)';


--
-- Name: COLUMN payments.payment_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.payment_method IS '결제 수단';


--
-- Name: COLUMN payments.amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.amount IS '결제 금액';


--
-- Name: COLUMN payments.tax_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.tax_amount IS '세액';


--
-- Name: COLUMN payments.discount_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.discount_amount IS '할인 금액';


--
-- Name: COLUMN payments.final_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.final_amount IS '최종 결제 금액';


--
-- Name: COLUMN payments.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.status IS '결제 상태';


--
-- Name: COLUMN payments.pg_provider; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.pg_provider IS 'PG사 (TOSS, NICEPAY, KCP)';


--
-- Name: COLUMN payments.pg_tid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.pg_tid IS 'PG사 거래 ID';


--
-- Name: COLUMN payments.pg_response; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.pg_response IS 'PG사 응답 데이터 (JSON)';


--
-- Name: COLUMN payments.payment_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.payment_data IS '결제 관련 데이터 (JSON)';


--
-- Name: COLUMN payments.paid_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.paid_at IS '결제 완료 일시';


--
-- Name: COLUMN payments.receipt_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.receipt_url IS '영수증 URL';


--
-- Name: COLUMN payments.is_refundable; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.is_refundable IS '환불 가능 여부';


--
-- Name: COLUMN payments.refunded_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.refunded_amount IS '환불된 금액';


--
-- Name: COLUMN payments.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.created_at IS '생성 일시';


--
-- Name: COLUMN payments.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.updated_at IS '수정 일시';


--
-- Name: COLUMN payments.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.metadata IS '확장 데이터 (JSON)';


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: quick_consultations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quick_consultations (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint,
    name character varying(100) NOT NULL,
    phone character varying(20) NOT NULL,
    email character varying(255),
    subject character varying(200),
    message text NOT NULL,
    preferred_contact_method character varying(20),
    preferred_contact_time character varying(100),
    personal_info_consent boolean DEFAULT false NOT NULL,
    personal_info_consent_at timestamp with time zone,
    third_party_consent boolean DEFAULT false NOT NULL,
    third_party_consent_at timestamp with time zone,
    marketing_consent boolean DEFAULT false NOT NULL,
    marketing_consent_at timestamp with time zone,
    consent_ip_address inet,
    consent_version character varying(20),
    assigned_company_id bigint,
    assigned_at timestamp with time zone,
    assigned_by bigint,
    status character varying(50) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    response_message text,
    responded_at timestamp with time zone,
    responded_by bigint,
    completed_at timestamp with time zone,
    completion_notes text,
    cancellation_reason text,
    ip_address inet,
    user_agent text,
    referrer character varying(500),
    metadata jsonb DEFAULT '{}'::jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint
);


--
-- Name: TABLE quick_consultations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.quick_consultations IS '빠른상담 요청 테이블 - 사용자 빠른 상담 신청 및 3가지 동의 관리';


--
-- Name: COLUMN quick_consultations.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quick_consultations.user_id IS '요청자 ID (비회원 시 NULL 허용)';


--
-- Name: COLUMN quick_consultations.personal_info_consent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quick_consultations.personal_info_consent IS '개인정보 수집/이용 동의';


--
-- Name: COLUMN quick_consultations.third_party_consent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quick_consultations.third_party_consent IS '제3자 제공 동의';


--
-- Name: COLUMN quick_consultations.marketing_consent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quick_consultations.marketing_consent IS '마케팅 수신 동의';


--
-- Name: COLUMN quick_consultations.consent_ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quick_consultations.consent_ip_address IS '동의 시 IP 주소 (법적 증빙)';


--
-- Name: COLUMN quick_consultations.consent_version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quick_consultations.consent_version IS '동의 약관 버전';


--
-- Name: COLUMN quick_consultations.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.quick_consultations.status IS '상담 상태 (SUBMITTED/ASSIGNED/IN_PROGRESS/RESPONDED/COMPLETED/CANCELLED)';


--
-- Name: quick_consultations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quick_consultations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quick_consultations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quick_consultations_id_seq OWNED BY public.quick_consultations.id;


--
-- Name: refunds; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refunds (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    payment_id bigint NOT NULL,
    user_id bigint NOT NULL,
    refund_amount numeric(12,2) NOT NULL,
    refund_reason character varying(500),
    refund_type character varying(50),
    status character varying(50) DEFAULT 'REQUESTED'::character varying,
    approved_at timestamp without time zone,
    approved_by bigint,
    completed_at timestamp without time zone,
    rejection_reason text,
    pg_tid character varying(255),
    pg_response jsonb DEFAULT '{}'::jsonb,
    refund_data jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE refunds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.refunds IS '환불 관리 테이블';


--
-- Name: COLUMN refunds.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.id IS '환불 고유 ID';


--
-- Name: COLUMN refunds.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN refunds.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.payment_id IS '원 결제 ID';


--
-- Name: COLUMN refunds.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.user_id IS '환불 요청자 ID';


--
-- Name: COLUMN refunds.refund_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.refund_amount IS '환불 금액';


--
-- Name: COLUMN refunds.refund_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.refund_reason IS '환불 사유';


--
-- Name: COLUMN refunds.refund_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.refund_type IS '환불 유형 (FULL:전액, PARTIAL:부분)';


--
-- Name: COLUMN refunds.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.status IS '환불 상태 (REQUESTED, APPROVED, COMPLETED, REJECTED)';


--
-- Name: COLUMN refunds.approved_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.approved_at IS '승인 일시';


--
-- Name: COLUMN refunds.approved_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.approved_by IS '승인자 ID';


--
-- Name: COLUMN refunds.completed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.completed_at IS '환불 완료 일시';


--
-- Name: COLUMN refunds.rejection_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.rejection_reason IS '거절 사유';


--
-- Name: COLUMN refunds.pg_tid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.pg_tid IS 'PG사 환불 거래 ID';


--
-- Name: COLUMN refunds.pg_response; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.pg_response IS 'PG사 환불 응답 (JSON)';


--
-- Name: COLUMN refunds.refund_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.refund_data IS '환불 관련 데이터 (JSON)';


--
-- Name: COLUMN refunds.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.created_at IS '요청 일시';


--
-- Name: COLUMN refunds.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refunds.updated_at IS '수정 일시';


--
-- Name: refunds_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refunds_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refunds_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refunds_id_seq OWNED BY public.refunds.id;


--
-- Name: saved_searches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saved_searches (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    name character varying(200),
    entity_type character varying(100) NOT NULL,
    search_query text,
    filters jsonb DEFAULT '{}'::jsonb,
    sort_by character varying(100),
    sort_order character varying(10),
    alert_enabled boolean DEFAULT false,
    alert_frequency character varying(50),
    last_alert_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE saved_searches; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.saved_searches IS '저장된 검색 조건 테이블';


--
-- Name: COLUMN saved_searches.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.id IS '검색 조건 고유 ID';


--
-- Name: COLUMN saved_searches.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN saved_searches.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.user_id IS '사용자 ID';


--
-- Name: COLUMN saved_searches.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.name IS '검색 조건명';


--
-- Name: COLUMN saved_searches.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.entity_type IS '검색 대상 엔티티 타입';


--
-- Name: COLUMN saved_searches.search_query; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.search_query IS '검색어';


--
-- Name: COLUMN saved_searches.filters; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.filters IS '필터 조건 (JSON)';


--
-- Name: COLUMN saved_searches.sort_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.sort_by IS '정렬 필드';


--
-- Name: COLUMN saved_searches.sort_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.sort_order IS '정렬 방향 (ASC, DESC)';


--
-- Name: COLUMN saved_searches.alert_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.alert_enabled IS '알림 설정 여부';


--
-- Name: COLUMN saved_searches.alert_frequency; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.alert_frequency IS '알림 주기 (INSTANT, DAILY, WEEKLY)';


--
-- Name: COLUMN saved_searches.last_alert_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.last_alert_at IS '마지막 알림 발송 일시';


--
-- Name: COLUMN saved_searches.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.created_at IS '생성 일시';


--
-- Name: COLUMN saved_searches.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.saved_searches.updated_at IS '수정 일시';


--
-- Name: saved_searches_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.saved_searches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: saved_searches_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.saved_searches_id_seq OWNED BY public.saved_searches.id;


--
-- Name: sms_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sms_verifications (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    phone character varying(20) NOT NULL,
    user_id bigint,
    verification_code character varying(10) NOT NULL,
    purpose character varying(50),
    attempt_count integer DEFAULT 0,
    max_attempts integer DEFAULT 5,
    is_verified boolean DEFAULT false,
    verified_at timestamp without time zone,
    expires_at timestamp without time zone NOT NULL,
    request_ip character varying(45),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE sms_verifications; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sms_verifications IS 'SMS 인증 코드 관리 테이블';


--
-- Name: COLUMN sms_verifications.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.id IS '인증 고유 ID';


--
-- Name: COLUMN sms_verifications.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN sms_verifications.phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.phone IS '휴대폰 번호';


--
-- Name: COLUMN sms_verifications.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.user_id IS '사용자 ID (선택)';


--
-- Name: COLUMN sms_verifications.verification_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.verification_code IS '인증 코드';


--
-- Name: COLUMN sms_verifications.purpose; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.purpose IS '인증 목적 (SIGNUP, PASSWORD_RESET, PHONE_CHANGE)';


--
-- Name: COLUMN sms_verifications.attempt_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.attempt_count IS '시도 횟수';


--
-- Name: COLUMN sms_verifications.max_attempts; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.max_attempts IS '최대 시도 횟수';


--
-- Name: COLUMN sms_verifications.is_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.is_verified IS '인증 완료 여부';


--
-- Name: COLUMN sms_verifications.verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.verified_at IS '인증 완료 일시';


--
-- Name: COLUMN sms_verifications.expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.expires_at IS '만료 일시';


--
-- Name: COLUMN sms_verifications.request_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.request_ip IS '요청 IP 주소';


--
-- Name: COLUMN sms_verifications.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sms_verifications.created_at IS '생성 일시';


--
-- Name: sms_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sms_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sms_verifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sms_verifications_id_seq OWNED BY public.sms_verifications.id;


--
-- Name: social_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.social_accounts (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    provider character varying(50) NOT NULL,
    provider_user_id character varying(255) NOT NULL,
    provider_email character varying(255),
    provider_name character varying(100),
    access_token text,
    refresh_token text,
    token_expires_at timestamp without time zone,
    profile_data jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    linked_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    metadata jsonb DEFAULT '{}'::jsonb
);


--
-- Name: TABLE social_accounts; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.social_accounts IS '소셜 로그인 연동 정보 테이블';


--
-- Name: COLUMN social_accounts.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.id IS '연동 정보 고유 ID';


--
-- Name: COLUMN social_accounts.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN social_accounts.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.user_id IS '사용자 ID';


--
-- Name: COLUMN social_accounts.provider; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.provider IS '소셜 제공자 (kakao, naver, google, facebook)';


--
-- Name: COLUMN social_accounts.provider_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.provider_user_id IS 'OAuth 제공자의 사용자 ID';


--
-- Name: COLUMN social_accounts.provider_email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.provider_email IS '제공자측 이메일';


--
-- Name: COLUMN social_accounts.provider_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.provider_name IS '제공자측 이름';


--
-- Name: COLUMN social_accounts.access_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.access_token IS '액세스 토큰';


--
-- Name: COLUMN social_accounts.refresh_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.refresh_token IS '리프레시 토큰';


--
-- Name: COLUMN social_accounts.token_expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.token_expires_at IS '액세스 토큰 만료 시간';


--
-- Name: COLUMN social_accounts.profile_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.profile_data IS 'OAuth 프로필 데이터 (JSONB)';


--
-- Name: COLUMN social_accounts.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.created_at IS '연동 일시';


--
-- Name: COLUMN social_accounts.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.updated_at IS '정보 갱신 일시';


--
-- Name: COLUMN social_accounts.linked_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.linked_at IS '소셜 계정 연동 시간';


--
-- Name: COLUMN social_accounts.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.is_deleted IS '소프트 삭제 여부';


--
-- Name: COLUMN social_accounts.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.deleted_at IS '삭제 시간';


--
-- Name: COLUMN social_accounts.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.social_accounts.metadata IS '확장 데이터 (JSONB)';


--
-- Name: social_accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.social_accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: social_accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.social_accounts_id_seq OWNED BY public.social_accounts.id;


--
-- Name: statistics_daily; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.statistics_daily (
    id bigint NOT NULL,
    stat_date date NOT NULL,
    new_users integer DEFAULT 0,
    active_users integer DEFAULT 0,
    total_users integer DEFAULT 0,
    new_companies integer DEFAULT 0,
    active_companies integer DEFAULT 0,
    new_requests integer DEFAULT 0,
    new_proposals integer DEFAULT 0,
    new_matches integer DEFAULT 0,
    payment_count integer DEFAULT 0,
    payment_amount numeric(12,2) DEFAULT 0,
    ad_impressions integer DEFAULT 0,
    ad_clicks integer DEFAULT 0,
    ad_revenue numeric(12,2) DEFAULT 0,
    detailed_stats jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE statistics_daily; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.statistics_daily IS '일별 통계 테이블';


--
-- Name: COLUMN statistics_daily.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.id IS '통계 고유 ID';


--
-- Name: COLUMN statistics_daily.stat_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.stat_date IS '통계 날짜';


--
-- Name: COLUMN statistics_daily.new_users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.new_users IS '신규 가입자 수';


--
-- Name: COLUMN statistics_daily.active_users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.active_users IS '활성 사용자 수';


--
-- Name: COLUMN statistics_daily.total_users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.total_users IS '전체 사용자 수';


--
-- Name: COLUMN statistics_daily.new_companies; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.new_companies IS '신규 업체 수';


--
-- Name: COLUMN statistics_daily.active_companies; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.active_companies IS '활성 업체 수';


--
-- Name: COLUMN statistics_daily.new_requests; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.new_requests IS '신규 견적 요청 수';


--
-- Name: COLUMN statistics_daily.new_proposals; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.new_proposals IS '신규 견적 제안 수';


--
-- Name: COLUMN statistics_daily.new_matches; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.new_matches IS '신규 매칭 수';


--
-- Name: COLUMN statistics_daily.payment_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.payment_count IS '결제 건수';


--
-- Name: COLUMN statistics_daily.payment_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.payment_amount IS '결제 금액';


--
-- Name: COLUMN statistics_daily.ad_impressions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.ad_impressions IS '광고 노출 수';


--
-- Name: COLUMN statistics_daily.ad_clicks; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.ad_clicks IS '광고 클릭 수';


--
-- Name: COLUMN statistics_daily.ad_revenue; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.ad_revenue IS '광고 수익';


--
-- Name: COLUMN statistics_daily.detailed_stats; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.detailed_stats IS '상세 통계 데이터 (JSON)';


--
-- Name: COLUMN statistics_daily.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.statistics_daily.created_at IS '생성 일시';


--
-- Name: statistics_daily_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.statistics_daily_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: statistics_daily_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.statistics_daily_id_seq OWNED BY public.statistics_daily.id;


--
-- Name: user_activity_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_activity_logs (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint,
    activity_type character varying(100) NOT NULL,
    entity_type character varying(100),
    entity_id bigint,
    action character varying(100),
    ip_address character varying(45),
    user_agent text,
    referer text,
    session_id character varying(255),
    metadata jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE user_activity_logs; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_activity_logs IS '사용자 활동 로그 테이블';


--
-- Name: COLUMN user_activity_logs.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.id IS '로그 고유 ID';


--
-- Name: COLUMN user_activity_logs.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN user_activity_logs.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.user_id IS '사용자 ID';


--
-- Name: COLUMN user_activity_logs.activity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.activity_type IS '활동 유형 (page_view, click, search, download 등)';


--
-- Name: COLUMN user_activity_logs.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.entity_type IS '대상 엔티티 타입';


--
-- Name: COLUMN user_activity_logs.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.entity_id IS '대상 엔티티 ID';


--
-- Name: COLUMN user_activity_logs.action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.action IS '수행한 액션';


--
-- Name: COLUMN user_activity_logs.ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.ip_address IS 'IP 주소';


--
-- Name: COLUMN user_activity_logs.user_agent; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.user_agent IS '브라우저/디바이스 정보';


--
-- Name: COLUMN user_activity_logs.referer; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.referer IS '참조 URL';


--
-- Name: COLUMN user_activity_logs.session_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.session_id IS '세션 ID';


--
-- Name: COLUMN user_activity_logs.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.metadata IS '추가 활동 데이터 (JSON)';


--
-- Name: COLUMN user_activity_logs.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_activity_logs.created_at IS '활동 발생 일시';


--
-- Name: user_activity_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_activity_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_activity_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_activity_logs_id_seq OWNED BY public.user_activity_logs.id;


--
-- Name: user_coupons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_coupons (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    coupon_id bigint NOT NULL,
    is_used boolean DEFAULT false,
    used_at timestamp without time zone,
    payment_id bigint,
    expires_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE user_coupons; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_coupons IS '사용자별 쿠폰 보유 테이블';


--
-- Name: COLUMN user_coupons.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.id IS '보유 쿠폰 고유 ID';


--
-- Name: COLUMN user_coupons.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN user_coupons.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.user_id IS '사용자 ID';


--
-- Name: COLUMN user_coupons.coupon_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.coupon_id IS '쿠폰 ID';


--
-- Name: COLUMN user_coupons.is_used; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.is_used IS '사용 여부';


--
-- Name: COLUMN user_coupons.used_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.used_at IS '사용 일시';


--
-- Name: COLUMN user_coupons.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.payment_id IS '사용한 결제 ID';


--
-- Name: COLUMN user_coupons.expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.expires_at IS '만료일시';


--
-- Name: COLUMN user_coupons.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_coupons.created_at IS '발급 일시';


--
-- Name: user_coupons_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_coupons_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_coupons_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_coupons_id_seq OWNED BY public.user_coupons.id;


--
-- Name: user_devices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_devices (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    device_id character varying(255) NOT NULL,
    device_type character varying(50),
    device_name character varying(100),
    os character varying(50),
    os_version character varying(50),
    app_version character varying(50),
    push_token character varying(500),
    push_enabled boolean DEFAULT true,
    is_active boolean DEFAULT true,
    last_active_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE user_devices; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_devices IS '사용자 디바이스 및 푸시 토큰 관리 테이블';


--
-- Name: COLUMN user_devices.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.id IS '디바이스 고유 ID';


--
-- Name: COLUMN user_devices.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN user_devices.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.user_id IS '사용자 ID';


--
-- Name: COLUMN user_devices.device_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.device_id IS '디바이스 고유 식별자';


--
-- Name: COLUMN user_devices.device_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.device_type IS '디바이스 유형 (mobile, tablet, desktop)';


--
-- Name: COLUMN user_devices.device_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.device_name IS '디바이스 이름';


--
-- Name: COLUMN user_devices.os; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.os IS '운영체제 (iOS, Android, Windows 등)';


--
-- Name: COLUMN user_devices.os_version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.os_version IS '운영체제 버전';


--
-- Name: COLUMN user_devices.app_version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.app_version IS '앱 버전';


--
-- Name: COLUMN user_devices.push_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.push_token IS '푸시 알림 토큰 (FCM/APNS)';


--
-- Name: COLUMN user_devices.push_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.push_enabled IS '푸시 알림 활성화 여부';


--
-- Name: COLUMN user_devices.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.is_active IS '디바이스 활성 상태';


--
-- Name: COLUMN user_devices.last_active_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.last_active_at IS '마지막 활동 시간';


--
-- Name: COLUMN user_devices.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.created_at IS '디바이스 등록 일시';


--
-- Name: COLUMN user_devices.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_devices.updated_at IS '정보 수정 일시';


--
-- Name: user_devices_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_devices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_devices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_devices_id_seq OWNED BY public.user_devices.id;


--
-- Name: user_points; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_points (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    balance integer DEFAULT 0 NOT NULL,
    total_earned integer DEFAULT 0 NOT NULL,
    total_used integer DEFAULT 0 NOT NULL,
    level integer DEFAULT 1,
    experience integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE user_points; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_points IS '사용자 포인트/마일리지 관리 테이블';


--
-- Name: COLUMN user_points.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.id IS '포인트 정보 고유 ID';


--
-- Name: COLUMN user_points.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN user_points.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.user_id IS '사용자 ID';


--
-- Name: COLUMN user_points.balance; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.balance IS '현재 포인트 잔액';


--
-- Name: COLUMN user_points.total_earned; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.total_earned IS '총 적립 포인트';


--
-- Name: COLUMN user_points.total_used; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.total_used IS '총 사용 포인트';


--
-- Name: COLUMN user_points.level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.level IS '사용자 레벨';


--
-- Name: COLUMN user_points.experience; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.experience IS '경험치';


--
-- Name: COLUMN user_points.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.created_at IS '생성 일시';


--
-- Name: COLUMN user_points.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_points.updated_at IS '수정 일시';


--
-- Name: user_points_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_points_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_points_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_points_id_seq OWNED BY public.user_points.id;


--
-- Name: user_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_profiles (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    name character varying(100),
    nickname character varying(50),
    bio text,
    avatar_url character varying(500),
    phone character varying(20),
    address character varying(500),
    postal_code character varying(10),
    latitude numeric(10,8),
    longitude numeric(11,8),
    social_links jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    profile_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb,
    profile_type character varying(20) DEFAULT 'USER_PROFILE'::character varying NOT NULL
);


--
-- Name: TABLE user_profiles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_profiles IS '사용자 프로필 확장 정보 테이블';


--
-- Name: COLUMN user_profiles.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.id IS '프로필 고유 ID';


--
-- Name: COLUMN user_profiles.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN user_profiles.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.user_id IS '사용자 ID (users 테이블 참조)';


--
-- Name: COLUMN user_profiles.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.name IS '사용자 실명';


--
-- Name: COLUMN user_profiles.nickname; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.nickname IS '닉네임 (고유값)';


--
-- Name: COLUMN user_profiles.bio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.bio IS '자기소개';


--
-- Name: COLUMN user_profiles.avatar_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.avatar_url IS '프로필 이미지 URL';


--
-- Name: COLUMN user_profiles.phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.phone IS '연락처';


--
-- Name: COLUMN user_profiles.address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.address IS '주소';


--
-- Name: COLUMN user_profiles.postal_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.postal_code IS '우편번호';


--
-- Name: COLUMN user_profiles.latitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.latitude IS '위도 좌표';


--
-- Name: COLUMN user_profiles.longitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.longitude IS '경도 좌표';


--
-- Name: COLUMN user_profiles.social_links; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.social_links IS 'SNS 링크 정보 (JSON: facebook, instagram, blog 등)';


--
-- Name: COLUMN user_profiles.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.created_at IS '프로필 생성 일시';


--
-- Name: COLUMN user_profiles.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.updated_at IS '프로필 수정 일시';


--
-- Name: COLUMN user_profiles.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.is_deleted IS '삭제 여부';


--
-- Name: COLUMN user_profiles.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.deleted_at IS '삭제 일시';


--
-- Name: COLUMN user_profiles.profile_visibility; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_profiles.profile_visibility IS '프로필 공개 여부 (PUBLIC, PRIVATE, FRIENDS_ONLY)';


--
-- Name: user_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_profiles_id_seq OWNED BY public.user_profiles.id;


--
-- Name: user_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_settings (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    notification_settings jsonb DEFAULT '{}'::jsonb,
    preferences jsonb DEFAULT '{}'::jsonb,
    ui_settings jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE user_settings; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_settings IS '사용자 개인 설정 테이블';


--
-- Name: COLUMN user_settings.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.id IS '설정 고유 ID';


--
-- Name: COLUMN user_settings.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.uuid IS '외부 API용 고유 식별자';


--
-- Name: COLUMN user_settings.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.user_id IS '사용자 ID';


--
-- Name: COLUMN user_settings.notification_settings; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.notification_settings IS '알림 설정 (JSON: email, sms, push 등)';


--
-- Name: COLUMN user_settings.preferences; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.preferences IS '개인화 설정 (JSON: language, timezone, theme 등)';


--
-- Name: COLUMN user_settings.ui_settings; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.ui_settings IS 'UI/UX 설정 (JSON: layout, font_size 등)';


--
-- Name: COLUMN user_settings.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.created_at IS '설정 생성 일시';


--
-- Name: COLUMN user_settings.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_settings.updated_at IS '설정 수정 일시';


--
-- Name: user_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_settings_id_seq OWNED BY public.user_settings.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password character varying(255),
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED')),
    email_verified boolean DEFAULT false,
    email_verified_at timestamp without time zone,
    phone_verified boolean DEFAULT false,
    phone_verified_at timestamp without time zone,
    identity_verified boolean DEFAULT false,
    identity_verified_at timestamp without time zone,
    last_login_at timestamp without time zone,
    last_login_ip character varying(45),
    login_count integer DEFAULT 0,
    failed_login_count integer DEFAULT 0,
    locked_until timestamp without time zone,
    terms_agreed_at timestamp without time zone,
    privacy_agreed_at timestamp without time zone,
    marketing_agreed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    deleted_by bigint,
    metadata jsonb DEFAULT '{}'::jsonb,
    roles text[] DEFAULT '{USER}'::text[] NOT NULL,
    terms_agreed boolean DEFAULT false NOT NULL,
    privacy_agreed boolean DEFAULT false NOT NULL,
    marketing_agreed boolean DEFAULT false,
    profile_completed boolean DEFAULT false NOT NULL,
    profile_completed_at timestamp without time zone
);


--
-- Name: TABLE users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.users IS '사용자 기본 정보 및 인증 관리 테이블';


--
-- Name: COLUMN users.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.id IS '사용자 고유 ID (Primary Key)';


--
-- Name: COLUMN users.uuid; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.uuid IS '외부 API용 고유 식별자 (UUID)';


--
-- Name: COLUMN users.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.email IS '사용자 이메일 (로그인 ID)';


--
-- Name: COLUMN users.password; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.password IS '비밀번호 (소셜 로그인 전용 사용자는 NULL 가능)';


--
-- Name: COLUMN users.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.status IS '계정 상태 (PENDING:대기, ACTIVE:활성, INACTIVE:비활성, SUSPENDED:정지)';


--
-- Name: COLUMN users.email_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.email_verified IS '이메일 인증 여부';


--
-- Name: COLUMN users.email_verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.email_verified_at IS '이메일 인증 완료 일시';


--
-- Name: COLUMN users.phone_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.phone_verified IS '휴대폰 인증 여부';


--
-- Name: COLUMN users.phone_verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.phone_verified_at IS '휴대폰 인증 완료 일시';


--
-- Name: COLUMN users.identity_verified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.identity_verified IS '본인 인증 여부';


--
-- Name: COLUMN users.identity_verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.identity_verified_at IS '본인 인증 완료 일시';


--
-- Name: COLUMN users.last_login_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.last_login_at IS '마지막 로그인 일시';


--
-- Name: COLUMN users.last_login_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.last_login_ip IS '마지막 로그인 IP 주소';


--
-- Name: COLUMN users.login_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.login_count IS '총 로그인 횟수';


--
-- Name: COLUMN users.failed_login_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.failed_login_count IS '로그인 실패 횟수';


--
-- Name: COLUMN users.locked_until; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.locked_until IS '계정 잠금 해제 시간';


--
-- Name: COLUMN users.terms_agreed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.terms_agreed_at IS '이용약관 동의 일시';


--
-- Name: COLUMN users.privacy_agreed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.privacy_agreed_at IS '개인정보처리방침 동의 일시';


--
-- Name: COLUMN users.marketing_agreed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.marketing_agreed_at IS '마케팅 수신 동의 일시';


--
-- Name: COLUMN users.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.created_at IS '계정 생성 일시';


--
-- Name: COLUMN users.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.updated_at IS '정보 수정 일시';


--
-- Name: COLUMN users.is_deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.is_deleted IS '삭제 여부 (Soft Delete)';


--
-- Name: COLUMN users.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.deleted_at IS '삭제 일시';


--
-- Name: COLUMN users.deleted_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.deleted_by IS '삭제한 사용자 ID';


--
-- Name: COLUMN users.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.metadata IS '확장 데이터 (JSON 형식)';


--
-- Name: COLUMN users.roles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.roles IS '사용자 역할 배열 (USER, COMPANY, ADMIN 등)';


--
-- Name: COLUMN users.terms_agreed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.terms_agreed IS '이용약관 동의 여부';


--
-- Name: COLUMN users.privacy_agreed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.privacy_agreed IS '개인정보 처리방침 동의 여부';


--
-- Name: COLUMN users.marketing_agreed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.marketing_agreed IS '마케팅 수신 동의 여부';


--
-- Name: COLUMN users.profile_completed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.profile_completed IS '프로필 설정 완료 여부';


--
-- Name: COLUMN users.profile_completed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.profile_completed_at IS '프로필 설정 완료 시간';


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: v_admin_active_sessions; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_admin_active_sessions AS
 SELECT s.id,
    s.uuid,
    s.admin_user_id,
    u.email,
    up.name,
    ar.role_code,
    ar.role_name,
    s.ip_address,
    s.device_info,
    s.last_activity_at,
    s.expires_at,
    (EXTRACT(epoch FROM (CURRENT_TIMESTAMP - s.last_activity_at)) / (60)::numeric) AS idle_minutes,
    (EXTRACT(epoch FROM (s.expires_at - CURRENT_TIMESTAMP)) / (60)::numeric) AS remaining_minutes
   FROM ((((public.admin_sessions s
     JOIN public.users u ON ((u.id = s.admin_user_id)))
     LEFT JOIN public.user_profiles up ON ((up.user_id = s.admin_user_id)))
     LEFT JOIN public.admin_user_roles aur ON (((aur.admin_user_id = s.admin_user_id) AND ((aur.expires_at IS NULL) OR (aur.expires_at > CURRENT_TIMESTAMP)))))
     LEFT JOIN public.admin_roles ar ON (((ar.id = aur.role_id) AND (ar.is_active = true))))
  WHERE (((s.status)::text = 'ACTIVE'::text) AND (s.expires_at > CURRENT_TIMESTAMP))
  ORDER BY ar.priority DESC, s.last_activity_at DESC;


--
-- Name: VIEW v_admin_active_sessions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.v_admin_active_sessions IS '활성 어드민 세션 뷰 - 현재 로그인된 어드민 세션 정보';


--
-- Name: v_admin_dashboard; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_admin_dashboard AS
 SELECT ( SELECT count(*) AS count
           FROM public.admin_sessions
          WHERE ((date(admin_sessions.created_at) = CURRENT_DATE) AND ((admin_sessions.status)::text = 'ACTIVE'::text))) AS active_sessions_today,
    ( SELECT count(*) AS count
           FROM public.admin_login_history
          WHERE ((date(admin_login_history.created_at) = CURRENT_DATE) AND ((admin_login_history.login_type)::text = 'SUCCESS'::text))) AS successful_logins_today,
    ( SELECT count(*) AS count
           FROM public.admin_login_history
          WHERE ((date(admin_login_history.created_at) = CURRENT_DATE) AND ((admin_login_history.login_type)::text = 'FAILED'::text))) AS failed_logins_today,
    ( SELECT count(DISTINCT admin_sessions.admin_user_id) AS count
           FROM public.admin_sessions
          WHERE ((admin_sessions.status)::text = 'ACTIVE'::text)) AS active_admins,
    ( SELECT count(*) AS count
           FROM public.admin_login_history
          WHERE (admin_login_history.created_at > (CURRENT_DATE - '7 days'::interval))) AS logins_last_7days,
    ( SELECT count(*) AS count
           FROM public.admin_two_factor_auth
          WHERE (admin_two_factor_auth.is_enabled = true)) AS two_factor_enabled_count,
    ( SELECT count(*) AS count
           FROM public.admin_ip_whitelist
          WHERE ((admin_ip_whitelist.is_active = true) AND (admin_ip_whitelist.is_deleted = false))) AS active_ip_rules,
    ( SELECT count(*) AS count
           FROM public.admin_notifications
          WHERE ((admin_notifications.is_read = false) AND (admin_notifications.is_expired = false))) AS unread_notifications,
    ( SELECT count(*) AS count
           FROM public.admin_notifications
          WHERE (((admin_notifications.severity)::text = 'CRITICAL'::text) AND (admin_notifications.created_at > (CURRENT_DATE - '1 day'::interval)))) AS critical_alerts_24h,
    ( SELECT count(*) AS count
           FROM public.admin_settings
          WHERE ((admin_settings.category)::text = 'SYSTEM'::text)) AS system_settings_count,
    ( SELECT count(*) AS count
           FROM public.admin_settings
          WHERE (admin_settings.is_feature_flag = true)) AS feature_flags_count,
    CURRENT_TIMESTAMP AS calculated_at;


--
-- Name: v_company_rankings; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_company_rankings AS
 WITH ranked_companies AS (
         SELECT c.id,
            c.uuid,
            c.name,
            c.description,
            c.logo_url,
            c.avg_rating,
            c.review_count,
            c.service_areas,
            c.tags,
            ac.id AS campaign_id,
            ac.total_value_30d,
            ac.priority_score,
            ac.secondary_score,
            ac.is_premium,
            dp.pick_type,
            dp.season,
            dp.badge_text,
            dp.badge_color,
            dp.display_order AS pick_order,
                CASE
                    WHEN (dp.pick_type IS NOT NULL) THEN 0
                    WHEN (ac.priority_score > (0)::numeric) THEN 1
                    ELSE 2
                END AS tier,
                CASE
                    WHEN (dp.pick_type IS NOT NULL) THEN (dp.display_order)::numeric
                    WHEN (ac.priority_score > (0)::numeric) THEN (- ac.priority_score)
                    ELSE ((- c.avg_rating) * (1000)::numeric)
                END AS tier_sort_value
           FROM ((public.companies c
             LEFT JOIN LATERAL ( SELECT ad_campaigns.id,
                    ad_campaigns.uuid,
                    ad_campaigns.company_id,
                    ad_campaigns.name,
                    ad_campaigns.description,
                    ad_campaigns.ad_type,
                    ad_campaigns.status,
                    ad_campaigns.ad_config,
                    ad_campaigns.targeting,
                    ad_campaigns.budget_type,
                    ad_campaigns.budget_amount,
                    ad_campaigns.daily_budget,
                    ad_campaigns.total_spent,
                    ad_campaigns.total_value_30d,
                    ad_campaigns.priority_score,
                    ad_campaigns.secondary_score,
                    ad_campaigns.is_premium,
                    ad_campaigns.premium_until,
                    ad_campaigns.start_date,
                    ad_campaigns.end_date,
                    ad_campaigns.total_impressions,
                    ad_campaigns.total_clicks,
                    ad_campaigns.total_conversions,
                    ad_campaigns.created_at,
                    ad_campaigns.updated_at,
                    ad_campaigns.is_deleted,
                    ad_campaigns.deleted_at,
                    ad_campaigns.metadata
                   FROM public.ad_campaigns
                  WHERE ((ad_campaigns.company_id = c.id) AND ((ad_campaigns.status)::text = 'ACTIVE'::text) AND (ad_campaigns.start_date <= CURRENT_DATE) AND ((ad_campaigns.end_date IS NULL) OR (ad_campaigns.end_date >= CURRENT_DATE)) AND (ad_campaigns.is_deleted = false))
                  ORDER BY ad_campaigns.priority_score DESC
                 LIMIT 1) ac ON (true))
             LEFT JOIN LATERAL ( SELECT damoa_picks.id,
                    damoa_picks.uuid,
                    damoa_picks.company_id,
                    damoa_picks.pick_type,
                    damoa_picks.season,
                    damoa_picks.title,
                    damoa_picks.description,
                    damoa_picks.main_image_url,
                    damoa_picks.banner_image_url,
                    damoa_picks.images,
                    damoa_picks.badge_text,
                    damoa_picks.badge_color,
                    damoa_picks.start_date,
                    damoa_picks.end_date,
                    damoa_picks.display_order,
                    damoa_picks.is_active,
                    damoa_picks.view_count,
                    damoa_picks.click_count,
                    damoa_picks.created_at,
                    damoa_picks.updated_at,
                    damoa_picks.is_deleted,
                    damoa_picks.deleted_at,
                    damoa_picks.metadata
                   FROM public.damoa_picks
                  WHERE ((damoa_picks.company_id = c.id) AND (damoa_picks.is_active = true) AND (damoa_picks.start_date <= CURRENT_DATE) AND ((damoa_picks.end_date IS NULL) OR (damoa_picks.end_date >= CURRENT_DATE)) AND (damoa_picks.is_deleted = false))
                  ORDER BY damoa_picks.display_order
                 LIMIT 1) dp ON (true))
          WHERE (c.is_deleted = false)
        )
 SELECT id,
    uuid,
    name,
    description,
    logo_url,
    avg_rating,
    review_count,
    service_areas,
    tags,
    campaign_id,
    COALESCE(total_value_30d, (0)::numeric) AS total_value_30d,
    COALESCE(priority_score, (0)::numeric) AS priority_score,
    COALESCE(secondary_score, (0)::numeric) AS secondary_score,
    COALESCE(is_premium, false) AS is_premium,
    pick_type,
    season,
    badge_text,
    badge_color,
        CASE tier
            WHEN 0 THEN '다모아 Pick'::text
            WHEN 1 THEN '유료 광고'::text
            ELSE '무료'::text
        END AS tier_name,
    row_number() OVER (ORDER BY tier, tier_sort_value) AS rank
   FROM ranked_companies
  ORDER BY tier, tier_sort_value;


--
-- Name: VIEW v_company_rankings; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.v_company_rankings IS '업체 우선순위 랭킹 뷰 - 다모아 Pick, 유료 광고, 무료 업체 순으로 정렬된 업체 목록';


--
-- Name: v_consultation_dashboard; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_consultation_dashboard AS
 SELECT ( SELECT count(*) AS count
           FROM public.quick_consultations
          WHERE (quick_consultations.is_deleted = false)) AS total_quick_consultations,
    ( SELECT count(*) AS count
           FROM public.quick_consultations
          WHERE (((quick_consultations.status)::text = 'SUBMITTED'::text) AND (quick_consultations.is_deleted = false))) AS pending_quick_consultations,
    ( SELECT count(*) AS count
           FROM public.quick_consultations
          WHERE (((quick_consultations.status)::text = 'COMPLETED'::text) AND (quick_consultations.is_deleted = false))) AS completed_quick_consultations,
    ( SELECT count(*) AS count
           FROM public.quick_consultations
          WHERE ((date(quick_consultations.created_at) = CURRENT_DATE) AND (quick_consultations.is_deleted = false))) AS today_quick_consultations,
    ( SELECT count(*) AS count
           FROM public.partnership_inquiries
          WHERE (partnership_inquiries.is_deleted = false)) AS total_partnership_inquiries,
    ( SELECT count(*) AS count
           FROM public.partnership_inquiries
          WHERE (((partnership_inquiries.status)::text = 'SUBMITTED'::text) AND (partnership_inquiries.is_deleted = false))) AS pending_partnership_inquiries,
    ( SELECT count(*) AS count
           FROM public.partnership_inquiries
          WHERE (((partnership_inquiries.priority)::text = 'URGENT'::text) AND ((partnership_inquiries.status)::text <> ALL ((ARRAY['COMPLETED'::character varying, 'REJECTED'::character varying])::text[])) AND (partnership_inquiries.is_deleted = false))) AS urgent_partnership_inquiries,
    ( SELECT count(*) AS count
           FROM public.partnership_inquiries
          WHERE ((date(partnership_inquiries.created_at) = CURRENT_DATE) AND (partnership_inquiries.is_deleted = false))) AS today_partnership_inquiries,
    ( SELECT count(*) AS count
           FROM public.consultation_messages
          WHERE ((consultation_messages.is_read = false) AND (consultation_messages.is_deleted = false))) AS unread_messages,
    ( SELECT count(*) AS count
           FROM public.quick_consultations
          WHERE ((quick_consultations.marketing_consent = true) AND (quick_consultations.is_deleted = false))) AS marketing_consent_count,
    ( SELECT count(*) AS count
           FROM public.quick_consultations
          WHERE ((quick_consultations.created_at > (CURRENT_DATE - '7 days'::interval)) AND (quick_consultations.is_deleted = false))) AS quick_consultations_last_7days,
    ( SELECT count(*) AS count
           FROM public.partnership_inquiries
          WHERE ((partnership_inquiries.created_at > (CURRENT_DATE - '7 days'::interval)) AND (partnership_inquiries.is_deleted = false))) AS partnership_inquiries_last_7days,
    CURRENT_TIMESTAMP AS calculated_at;


--
-- Name: VIEW v_consultation_dashboard; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.v_consultation_dashboard IS '상담 통계 대시보드 뷰 - 빠른상담 및 제휴문의 현황';


--
-- Name: v_filter_options_tree; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_filter_options_tree AS
 WITH RECURSIVE option_tree AS (
         SELECT fo.id,
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
            (fo.name)::text AS full_name
           FROM (public.filter_options fo
             JOIN public.filter_categories fc ON ((fo.category_id = fc.id)))
          WHERE ((fo.parent_id IS NULL) AND (fo.is_active = true) AND (fo.is_deleted = false))
        UNION ALL
         SELECT fo.id,
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
            (ot.id_path || fo.id),
            ((ot.full_name || ' > '::text) || (fo.name)::text)
           FROM (public.filter_options fo
             JOIN option_tree ot ON ((fo.parent_id = ot.id)))
          WHERE ((fo.is_active = true) AND (fo.is_deleted = false))
        )
 SELECT id,
    uuid,
    category_id,
    category_code,
    category_name,
    code,
    name,
    parent_id,
    depth,
    path,
    metadata,
    display_order,
    is_active,
    id_path,
    full_name
   FROM option_tree
  ORDER BY category_id, path;


--
-- Name: VIEW v_filter_options_tree; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.v_filter_options_tree IS '계층 구조 필터 옵션 뷰 (재귀 CTE)';


--
-- Name: ad_campaigns id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ad_campaigns ALTER COLUMN id SET DEFAULT nextval('public.ad_campaigns_id_seq'::regclass);


--
-- Name: admin_activity_summary id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_activity_summary ALTER COLUMN id SET DEFAULT nextval('public.admin_activity_summary_id_seq'::regclass);


--
-- Name: admin_audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_audit_logs ALTER COLUMN id SET DEFAULT nextval('public.admin_audit_logs_id_seq'::regclass);


--
-- Name: admin_ip_whitelist id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_ip_whitelist ALTER COLUMN id SET DEFAULT nextval('public.admin_ip_whitelist_id_seq'::regclass);


--
-- Name: admin_login_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_login_history ALTER COLUMN id SET DEFAULT nextval('public.admin_login_history_id_seq'::regclass);


--
-- Name: admin_notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_notifications ALTER COLUMN id SET DEFAULT nextval('public.admin_notifications_id_seq'::regclass);


--
-- Name: admin_page_permissions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_page_permissions ALTER COLUMN id SET DEFAULT nextval('public.admin_page_permissions_id_seq'::regclass);


--
-- Name: admin_password_policies id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_password_policies ALTER COLUMN id SET DEFAULT nextval('public.admin_password_policies_id_seq'::regclass);


--
-- Name: admin_permissions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_permissions ALTER COLUMN id SET DEFAULT nextval('public.admin_permissions_id_seq'::regclass);


--
-- Name: admin_role_page_access id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_page_access ALTER COLUMN id SET DEFAULT nextval('public.admin_role_page_access_id_seq'::regclass);


--
-- Name: admin_role_permissions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_permissions ALTER COLUMN id SET DEFAULT nextval('public.admin_role_permissions_id_seq'::regclass);


--
-- Name: admin_roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_roles ALTER COLUMN id SET DEFAULT nextval('public.admin_roles_id_seq'::regclass);


--
-- Name: admin_sessions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_sessions ALTER COLUMN id SET DEFAULT nextval('public.admin_sessions_id_seq'::regclass);


--
-- Name: admin_settings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_settings ALTER COLUMN id SET DEFAULT nextval('public.admin_settings_id_seq'::regclass);


--
-- Name: admin_two_factor_auth id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_two_factor_auth ALTER COLUMN id SET DEFAULT nextval('public.admin_two_factor_auth_id_seq'::regclass);


--
-- Name: admin_user_roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_user_roles ALTER COLUMN id SET DEFAULT nextval('public.admin_user_roles_id_seq'::regclass);


--
-- Name: admin_users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_users ALTER COLUMN id SET DEFAULT nextval('public.admin_users_id_seq'::regclass);


--
-- Name: analytics_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.analytics_events ALTER COLUMN id SET DEFAULT nextval('public.analytics_events_id_seq'::regclass);


--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: board_attachments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_attachments ALTER COLUMN id SET DEFAULT nextval('public.board_attachments_id_seq'::regclass);


--
-- Name: board_categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_categories ALTER COLUMN id SET DEFAULT nextval('public.board_categories_id_seq'::regclass);


--
-- Name: board_comments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_comments ALTER COLUMN id SET DEFAULT nextval('public.board_comments_id_seq'::regclass);


--
-- Name: board_likes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_likes ALTER COLUMN id SET DEFAULT nextval('public.board_likes_id_seq'::regclass);


--
-- Name: boards id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boards ALTER COLUMN id SET DEFAULT nextval('public.boards_id_seq'::regclass);


--
-- Name: companies id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies ALTER COLUMN id SET DEFAULT nextval('public.companies_id_seq'::regclass);


--
-- Name: company_certifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certifications ALTER COLUMN id SET DEFAULT nextval('public.company_certifications_id_seq'::regclass);


--
-- Name: company_filter_options id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_filter_options ALTER COLUMN id SET DEFAULT nextval('public.company_filter_options_id_seq'::regclass);


--
-- Name: company_images id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_images ALTER COLUMN id SET DEFAULT nextval('public.company_images_id_seq'::regclass);


--
-- Name: company_likes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_likes ALTER COLUMN id SET DEFAULT nextval('public.company_likes_id_seq'::regclass);


--
-- Name: company_portfolios id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_portfolios ALTER COLUMN id SET DEFAULT nextval('public.company_portfolios_id_seq'::regclass);


--
-- Name: company_review_images id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_review_images ALTER COLUMN id SET DEFAULT nextval('public.company_review_images_id_seq'::regclass);


--
-- Name: company_reviews id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_reviews ALTER COLUMN id SET DEFAULT nextval('public.company_reviews_id_seq'::regclass);


--
-- Name: consultation_messages id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultation_messages ALTER COLUMN id SET DEFAULT nextval('public.consultation_messages_id_seq'::regclass);


--
-- Name: coupons id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupons ALTER COLUMN id SET DEFAULT nextval('public.coupons_id_seq'::regclass);


--
-- Name: credit_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions ALTER COLUMN id SET DEFAULT nextval('public.credit_transactions_id_seq'::regclass);


--
-- Name: credits id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credits ALTER COLUMN id SET DEFAULT nextval('public.credits_id_seq'::regclass);


--
-- Name: damoa_picks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damoa_picks ALTER COLUMN id SET DEFAULT nextval('public.damoa_picks_id_seq'::regclass);


--
-- Name: email_verifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications ALTER COLUMN id SET DEFAULT nextval('public.email_verifications_id_seq'::regclass);


--
-- Name: estimate_messages id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_messages ALTER COLUMN id SET DEFAULT nextval('public.estimate_messages_id_seq'::regclass);


--
-- Name: estimate_proposal_attachments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposal_attachments ALTER COLUMN id SET DEFAULT nextval('public.estimate_proposal_attachments_id_seq'::regclass);


--
-- Name: estimate_proposals id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposals ALTER COLUMN id SET DEFAULT nextval('public.estimate_proposals_id_seq'::regclass);


--
-- Name: estimate_request_attachments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_request_attachments ALTER COLUMN id SET DEFAULT nextval('public.estimate_request_attachments_id_seq'::regclass);


--
-- Name: estimate_requests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_requests ALTER COLUMN id SET DEFAULT nextval('public.estimate_requests_id_seq'::regclass);


--
-- Name: estimate_templates id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_templates ALTER COLUMN id SET DEFAULT nextval('public.estimate_templates_id_seq'::regclass);


--
-- Name: file_attachments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_attachments ALTER COLUMN id SET DEFAULT nextval('public.file_attachments_id_seq'::regclass);


--
-- Name: file_downloads id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_downloads ALTER COLUMN id SET DEFAULT nextval('public.file_downloads_id_seq'::regclass);


--
-- Name: files id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.files ALTER COLUMN id SET DEFAULT nextval('public.files_id_seq'::regclass);


--
-- Name: filter_categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_categories ALTER COLUMN id SET DEFAULT nextval('public.filter_categories_id_seq'::regclass);


--
-- Name: filter_option_relations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_option_relations ALTER COLUMN id SET DEFAULT nextval('public.filter_option_relations_id_seq'::regclass);


--
-- Name: filter_options id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_options ALTER COLUMN id SET DEFAULT nextval('public.filter_options_id_seq'::regclass);


--
-- Name: invoices id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices ALTER COLUMN id SET DEFAULT nextval('public.invoices_id_seq'::regclass);


--
-- Name: match_reviews id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_reviews ALTER COLUMN id SET DEFAULT nextval('public.match_reviews_id_seq'::regclass);


--
-- Name: matches id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matches ALTER COLUMN id SET DEFAULT nextval('public.matches_id_seq'::regclass);


--
-- Name: notification_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_logs ALTER COLUMN id SET DEFAULT nextval('public.notification_logs_id_seq'::regclass);


--
-- Name: notification_settings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_settings ALTER COLUMN id SET DEFAULT nextval('public.notification_settings_id_seq'::regclass);


--
-- Name: notification_templates id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates ALTER COLUMN id SET DEFAULT nextval('public.notification_templates_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: partnership_inquiries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries ALTER COLUMN id SET DEFAULT nextval('public.partnership_inquiries_id_seq'::regclass);


--
-- Name: payment_methods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_methods ALTER COLUMN id SET DEFAULT nextval('public.payment_methods_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: quick_consultations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations ALTER COLUMN id SET DEFAULT nextval('public.quick_consultations_id_seq'::regclass);


--
-- Name: refunds id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds ALTER COLUMN id SET DEFAULT nextval('public.refunds_id_seq'::regclass);


--
-- Name: saved_searches id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_searches ALTER COLUMN id SET DEFAULT nextval('public.saved_searches_id_seq'::regclass);


--
-- Name: sms_verifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sms_verifications ALTER COLUMN id SET DEFAULT nextval('public.sms_verifications_id_seq'::regclass);


--
-- Name: social_accounts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_accounts ALTER COLUMN id SET DEFAULT nextval('public.social_accounts_id_seq'::regclass);


--
-- Name: statistics_daily id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.statistics_daily ALTER COLUMN id SET DEFAULT nextval('public.statistics_daily_id_seq'::regclass);


--
-- Name: user_activity_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activity_logs ALTER COLUMN id SET DEFAULT nextval('public.user_activity_logs_id_seq'::regclass);


--
-- Name: user_coupons id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_coupons ALTER COLUMN id SET DEFAULT nextval('public.user_coupons_id_seq'::regclass);


--
-- Name: user_devices id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_devices ALTER COLUMN id SET DEFAULT nextval('public.user_devices_id_seq'::regclass);


--
-- Name: user_points id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_points ALTER COLUMN id SET DEFAULT nextval('public.user_points_id_seq'::regclass);


--
-- Name: user_profiles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profiles ALTER COLUMN id SET DEFAULT nextval('public.user_profiles_id_seq'::regclass);


--
-- Name: user_settings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_settings ALTER COLUMN id SET DEFAULT nextval('public.user_settings_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: ad_campaigns ad_campaigns_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ad_campaigns
    ADD CONSTRAINT ad_campaigns_pkey PRIMARY KEY (id);


--
-- Name: ad_campaigns ad_campaigns_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ad_campaigns
    ADD CONSTRAINT ad_campaigns_uuid_key UNIQUE (uuid);


--
-- Name: admin_activity_summary admin_activity_summary_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_activity_summary
    ADD CONSTRAINT admin_activity_summary_pkey PRIMARY KEY (id);


--
-- Name: admin_audit_logs admin_audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_audit_logs
    ADD CONSTRAINT admin_audit_logs_pkey PRIMARY KEY (id);


--
-- Name: admin_ip_whitelist admin_ip_whitelist_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_ip_whitelist
    ADD CONSTRAINT admin_ip_whitelist_pkey PRIMARY KEY (id);


--
-- Name: admin_ip_whitelist admin_ip_whitelist_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_ip_whitelist
    ADD CONSTRAINT admin_ip_whitelist_uuid_key UNIQUE (uuid);


--
-- Name: admin_login_history admin_login_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_login_history
    ADD CONSTRAINT admin_login_history_pkey PRIMARY KEY (id);


--
-- Name: admin_login_history admin_login_history_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_login_history
    ADD CONSTRAINT admin_login_history_uuid_key UNIQUE (uuid);


--
-- Name: admin_notifications admin_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_notifications
    ADD CONSTRAINT admin_notifications_pkey PRIMARY KEY (id);


--
-- Name: admin_notifications admin_notifications_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_notifications
    ADD CONSTRAINT admin_notifications_uuid_key UNIQUE (uuid);


--
-- Name: admin_page_permissions admin_page_permissions_page_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_page_permissions
    ADD CONSTRAINT admin_page_permissions_page_code_key UNIQUE (page_code);


--
-- Name: admin_page_permissions admin_page_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_page_permissions
    ADD CONSTRAINT admin_page_permissions_pkey PRIMARY KEY (id);


--
-- Name: admin_page_permissions admin_page_permissions_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_page_permissions
    ADD CONSTRAINT admin_page_permissions_uuid_key UNIQUE (uuid);


--
-- Name: admin_password_policies admin_password_policies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_password_policies
    ADD CONSTRAINT admin_password_policies_pkey PRIMARY KEY (id);


--
-- Name: admin_password_policies admin_password_policies_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_password_policies
    ADD CONSTRAINT admin_password_policies_uuid_key UNIQUE (uuid);


--
-- Name: admin_permissions admin_permissions_permission_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_permissions
    ADD CONSTRAINT admin_permissions_permission_code_key UNIQUE (permission_code);


--
-- Name: admin_permissions admin_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_permissions
    ADD CONSTRAINT admin_permissions_pkey PRIMARY KEY (id);


--
-- Name: admin_permissions admin_permissions_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_permissions
    ADD CONSTRAINT admin_permissions_uuid_key UNIQUE (uuid);


--
-- Name: admin_role_page_access admin_role_page_access_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_page_access
    ADD CONSTRAINT admin_role_page_access_pkey PRIMARY KEY (id);


--
-- Name: admin_role_permissions admin_role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_permissions
    ADD CONSTRAINT admin_role_permissions_pkey PRIMARY KEY (id);


--
-- Name: admin_roles admin_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_roles
    ADD CONSTRAINT admin_roles_pkey PRIMARY KEY (id);


--
-- Name: admin_roles admin_roles_role_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_roles
    ADD CONSTRAINT admin_roles_role_code_key UNIQUE (role_code);


--
-- Name: admin_roles admin_roles_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_roles
    ADD CONSTRAINT admin_roles_uuid_key UNIQUE (uuid);


--
-- Name: admin_sessions admin_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_sessions
    ADD CONSTRAINT admin_sessions_pkey PRIMARY KEY (id);


--
-- Name: admin_sessions admin_sessions_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_sessions
    ADD CONSTRAINT admin_sessions_uuid_key UNIQUE (uuid);


--
-- Name: admin_settings admin_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_settings
    ADD CONSTRAINT admin_settings_pkey PRIMARY KEY (id);


--
-- Name: admin_settings admin_settings_setting_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_settings
    ADD CONSTRAINT admin_settings_setting_key_key UNIQUE (setting_key);


--
-- Name: admin_settings admin_settings_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_settings
    ADD CONSTRAINT admin_settings_uuid_key UNIQUE (uuid);


--
-- Name: admin_two_factor_auth admin_two_factor_auth_admin_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_two_factor_auth
    ADD CONSTRAINT admin_two_factor_auth_admin_user_id_key UNIQUE (admin_user_id);


--
-- Name: admin_two_factor_auth admin_two_factor_auth_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_two_factor_auth
    ADD CONSTRAINT admin_two_factor_auth_pkey PRIMARY KEY (id);


--
-- Name: admin_two_factor_auth admin_two_factor_auth_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_two_factor_auth
    ADD CONSTRAINT admin_two_factor_auth_uuid_key UNIQUE (uuid);


--
-- Name: admin_user_roles admin_user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_user_roles
    ADD CONSTRAINT admin_user_roles_pkey PRIMARY KEY (id);


--
-- Name: admin_users admin_users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_email_key UNIQUE (email);


--
-- Name: admin_users admin_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_pkey PRIMARY KEY (id);


--
-- Name: admin_users admin_users_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_users
    ADD CONSTRAINT admin_users_uuid_key UNIQUE (uuid);


--
-- Name: analytics_events analytics_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.analytics_events
    ADD CONSTRAINT analytics_events_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: board_attachments board_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_attachments
    ADD CONSTRAINT board_attachments_pkey PRIMARY KEY (id);


--
-- Name: board_categories board_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_categories
    ADD CONSTRAINT board_categories_pkey PRIMARY KEY (id);


--
-- Name: board_categories board_categories_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_categories
    ADD CONSTRAINT board_categories_uuid_key UNIQUE (uuid);


--
-- Name: board_comments board_comments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_comments
    ADD CONSTRAINT board_comments_pkey PRIMARY KEY (id);


--
-- Name: board_comments board_comments_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_comments
    ADD CONSTRAINT board_comments_uuid_key UNIQUE (uuid);


--
-- Name: board_likes board_likes_board_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_likes
    ADD CONSTRAINT board_likes_board_id_user_id_key UNIQUE (board_id, user_id);


--
-- Name: board_likes board_likes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_likes
    ADD CONSTRAINT board_likes_pkey PRIMARY KEY (id);


--
-- Name: boards boards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_pkey PRIMARY KEY (id);


--
-- Name: boards boards_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_uuid_key UNIQUE (uuid);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: companies companies_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_uuid_key UNIQUE (uuid);


--
-- Name: company_certifications company_certifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certifications
    ADD CONSTRAINT company_certifications_pkey PRIMARY KEY (id);


--
-- Name: company_certifications company_certifications_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certifications
    ADD CONSTRAINT company_certifications_uuid_key UNIQUE (uuid);


--
-- Name: company_filter_options company_filter_options_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_filter_options
    ADD CONSTRAINT company_filter_options_pkey PRIMARY KEY (id);


--
-- Name: company_images company_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_images
    ADD CONSTRAINT company_images_pkey PRIMARY KEY (id);


--
-- Name: company_images company_images_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_images
    ADD CONSTRAINT company_images_uuid_key UNIQUE (uuid);


--
-- Name: company_likes company_likes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_likes
    ADD CONSTRAINT company_likes_pkey PRIMARY KEY (id);


--
-- Name: company_portfolios company_portfolios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_portfolios
    ADD CONSTRAINT company_portfolios_pkey PRIMARY KEY (id);


--
-- Name: company_portfolios company_portfolios_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_portfolios
    ADD CONSTRAINT company_portfolios_uuid_key UNIQUE (uuid);


--
-- Name: company_review_images company_review_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_review_images
    ADD CONSTRAINT company_review_images_pkey PRIMARY KEY (id);


--
-- Name: company_reviews company_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_reviews
    ADD CONSTRAINT company_reviews_pkey PRIMARY KEY (id);


--
-- Name: company_reviews company_reviews_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_reviews
    ADD CONSTRAINT company_reviews_uuid_key UNIQUE (uuid);


--
-- Name: consultation_messages consultation_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_pkey PRIMARY KEY (id);


--
-- Name: consultation_messages consultation_messages_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_uuid_key UNIQUE (uuid);


--
-- Name: coupons coupons_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupons
    ADD CONSTRAINT coupons_code_key UNIQUE (code);


--
-- Name: coupons coupons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupons
    ADD CONSTRAINT coupons_pkey PRIMARY KEY (id);


--
-- Name: coupons coupons_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupons
    ADD CONSTRAINT coupons_uuid_key UNIQUE (uuid);


--
-- Name: credit_transactions credit_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions
    ADD CONSTRAINT credit_transactions_pkey PRIMARY KEY (id);


--
-- Name: credit_transactions credit_transactions_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions
    ADD CONSTRAINT credit_transactions_uuid_key UNIQUE (uuid);


--
-- Name: credits credits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credits
    ADD CONSTRAINT credits_pkey PRIMARY KEY (id);


--
-- Name: credits credits_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credits
    ADD CONSTRAINT credits_user_id_key UNIQUE (user_id);


--
-- Name: credits credits_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credits
    ADD CONSTRAINT credits_uuid_key UNIQUE (uuid);


--
-- Name: damoa_picks damoa_picks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damoa_picks
    ADD CONSTRAINT damoa_picks_pkey PRIMARY KEY (id);


--
-- Name: damoa_picks damoa_picks_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damoa_picks
    ADD CONSTRAINT damoa_picks_uuid_key UNIQUE (uuid);


--
-- Name: email_verifications email_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications
    ADD CONSTRAINT email_verifications_pkey PRIMARY KEY (id);


--
-- Name: email_verifications email_verifications_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications
    ADD CONSTRAINT email_verifications_uuid_key UNIQUE (uuid);


--
-- Name: email_verifications email_verifications_verification_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications
    ADD CONSTRAINT email_verifications_verification_token_key UNIQUE (verification_token);


--
-- Name: estimate_messages estimate_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_messages
    ADD CONSTRAINT estimate_messages_pkey PRIMARY KEY (id);


--
-- Name: estimate_messages estimate_messages_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_messages
    ADD CONSTRAINT estimate_messages_uuid_key UNIQUE (uuid);


--
-- Name: estimate_proposal_attachments estimate_proposal_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposal_attachments
    ADD CONSTRAINT estimate_proposal_attachments_pkey PRIMARY KEY (id);


--
-- Name: estimate_proposals estimate_proposals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposals
    ADD CONSTRAINT estimate_proposals_pkey PRIMARY KEY (id);


--
-- Name: estimate_proposals estimate_proposals_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposals
    ADD CONSTRAINT estimate_proposals_uuid_key UNIQUE (uuid);


--
-- Name: estimate_request_attachments estimate_request_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_request_attachments
    ADD CONSTRAINT estimate_request_attachments_pkey PRIMARY KEY (id);


--
-- Name: estimate_requests estimate_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_requests
    ADD CONSTRAINT estimate_requests_pkey PRIMARY KEY (id);


--
-- Name: estimate_requests estimate_requests_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_requests
    ADD CONSTRAINT estimate_requests_uuid_key UNIQUE (uuid);


--
-- Name: estimate_templates estimate_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_templates
    ADD CONSTRAINT estimate_templates_pkey PRIMARY KEY (id);


--
-- Name: estimate_templates estimate_templates_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_templates
    ADD CONSTRAINT estimate_templates_uuid_key UNIQUE (uuid);


--
-- Name: file_attachments file_attachments_file_id_entity_type_entity_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_attachments
    ADD CONSTRAINT file_attachments_file_id_entity_type_entity_id_key UNIQUE (file_id, entity_type, entity_id);


--
-- Name: file_attachments file_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_attachments
    ADD CONSTRAINT file_attachments_pkey PRIMARY KEY (id);


--
-- Name: file_downloads file_downloads_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_downloads
    ADD CONSTRAINT file_downloads_pkey PRIMARY KEY (id);


--
-- Name: files files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_pkey PRIMARY KEY (id);


--
-- Name: files files_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_uuid_key UNIQUE (uuid);


--
-- Name: filter_categories filter_categories_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_categories
    ADD CONSTRAINT filter_categories_code_key UNIQUE (code);


--
-- Name: filter_categories filter_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_categories
    ADD CONSTRAINT filter_categories_pkey PRIMARY KEY (id);


--
-- Name: filter_categories filter_categories_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_categories
    ADD CONSTRAINT filter_categories_uuid_key UNIQUE (uuid);


--
-- Name: filter_option_relations filter_option_relations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_option_relations
    ADD CONSTRAINT filter_option_relations_pkey PRIMARY KEY (id);


--
-- Name: filter_options filter_options_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_options
    ADD CONSTRAINT filter_options_pkey PRIMARY KEY (id);


--
-- Name: filter_options filter_options_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_options
    ADD CONSTRAINT filter_options_uuid_key UNIQUE (uuid);


--
-- Name: invoices invoices_invoice_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_invoice_number_key UNIQUE (invoice_number);


--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);


--
-- Name: invoices invoices_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_uuid_key UNIQUE (uuid);


--
-- Name: match_reviews match_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_reviews
    ADD CONSTRAINT match_reviews_pkey PRIMARY KEY (id);


--
-- Name: match_reviews match_reviews_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_reviews
    ADD CONSTRAINT match_reviews_uuid_key UNIQUE (uuid);


--
-- Name: matches matches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matches
    ADD CONSTRAINT matches_pkey PRIMARY KEY (id);


--
-- Name: matches matches_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matches
    ADD CONSTRAINT matches_uuid_key UNIQUE (uuid);


--
-- Name: notification_logs notification_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_logs
    ADD CONSTRAINT notification_logs_pkey PRIMARY KEY (id);


--
-- Name: notification_settings notification_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_settings
    ADD CONSTRAINT notification_settings_pkey PRIMARY KEY (id);


--
-- Name: notification_settings notification_settings_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_settings
    ADD CONSTRAINT notification_settings_user_id_key UNIQUE (user_id);


--
-- Name: notification_settings notification_settings_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_settings
    ADD CONSTRAINT notification_settings_uuid_key UNIQUE (uuid);


--
-- Name: notification_templates notification_templates_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_code_key UNIQUE (code);


--
-- Name: notification_templates notification_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_pkey PRIMARY KEY (id);


--
-- Name: notification_templates notification_templates_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_uuid_key UNIQUE (uuid);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_uuid_key UNIQUE (uuid);


--
-- Name: partnership_inquiries partnership_inquiries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries
    ADD CONSTRAINT partnership_inquiries_pkey PRIMARY KEY (id);


--
-- Name: partnership_inquiries partnership_inquiries_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries
    ADD CONSTRAINT partnership_inquiries_uuid_key UNIQUE (uuid);


--
-- Name: payment_methods payment_methods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_methods
    ADD CONSTRAINT payment_methods_pkey PRIMARY KEY (id);


--
-- Name: payment_methods payment_methods_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_methods
    ADD CONSTRAINT payment_methods_uuid_key UNIQUE (uuid);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: payments payments_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_uuid_key UNIQUE (uuid);


--
-- Name: quick_consultations quick_consultations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations
    ADD CONSTRAINT quick_consultations_pkey PRIMARY KEY (id);


--
-- Name: quick_consultations quick_consultations_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations
    ADD CONSTRAINT quick_consultations_uuid_key UNIQUE (uuid);


--
-- Name: refunds refunds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT refunds_pkey PRIMARY KEY (id);


--
-- Name: refunds refunds_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT refunds_uuid_key UNIQUE (uuid);


--
-- Name: saved_searches saved_searches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_searches
    ADD CONSTRAINT saved_searches_pkey PRIMARY KEY (id);


--
-- Name: saved_searches saved_searches_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_searches
    ADD CONSTRAINT saved_searches_uuid_key UNIQUE (uuid);


--
-- Name: sms_verifications sms_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sms_verifications
    ADD CONSTRAINT sms_verifications_pkey PRIMARY KEY (id);


--
-- Name: sms_verifications sms_verifications_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sms_verifications
    ADD CONSTRAINT sms_verifications_uuid_key UNIQUE (uuid);


--
-- Name: social_accounts social_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_accounts
    ADD CONSTRAINT social_accounts_pkey PRIMARY KEY (id);


--
-- Name: social_accounts social_accounts_provider_provider_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_accounts
    ADD CONSTRAINT social_accounts_provider_provider_id_key UNIQUE (provider, provider_user_id);


--
-- Name: social_accounts social_accounts_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_accounts
    ADD CONSTRAINT social_accounts_uuid_key UNIQUE (uuid);


--
-- Name: statistics_daily statistics_daily_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.statistics_daily
    ADD CONSTRAINT statistics_daily_pkey PRIMARY KEY (id);


--
-- Name: statistics_daily statistics_daily_stat_date_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.statistics_daily
    ADD CONSTRAINT statistics_daily_stat_date_key UNIQUE (stat_date);


--
-- Name: admin_activity_summary uk_admin_activity_summary; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_activity_summary
    ADD CONSTRAINT uk_admin_activity_summary UNIQUE (admin_user_id, summary_date, summary_type);


--
-- Name: company_filter_options uk_company_filter_option; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_filter_options
    ADD CONSTRAINT uk_company_filter_option UNIQUE (company_id, filter_option_id);


--
-- Name: company_likes uk_company_likes_company_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_likes
    ADD CONSTRAINT uk_company_likes_company_user UNIQUE (company_id, user_id);


--
-- Name: company_likes uk_company_likes_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_likes
    ADD CONSTRAINT uk_company_likes_uuid UNIQUE (uuid);


--
-- Name: estimate_proposal_attachments uk_estimate_proposal_attachments_proposal_file; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposal_attachments
    ADD CONSTRAINT uk_estimate_proposal_attachments_proposal_file UNIQUE (estimate_proposal_id, file_id);


--
-- Name: estimate_request_attachments uk_estimate_request_attachments_request_file; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_request_attachments
    ADD CONSTRAINT uk_estimate_request_attachments_request_file UNIQUE (estimate_request_id, file_id);


--
-- Name: filter_options uk_filter_option_category_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_options
    ADD CONSTRAINT uk_filter_option_category_code UNIQUE (category_id, code);


--
-- Name: filter_options uk_filter_option_category_parent_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_options
    ADD CONSTRAINT uk_filter_option_category_parent_name UNIQUE (category_id, parent_id, name);


--
-- Name: filter_option_relations uk_filter_relation; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_option_relations
    ADD CONSTRAINT uk_filter_relation UNIQUE (source_option_id, target_option_id, relation_type);


--
-- Name: company_review_images uk_review_file; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_review_images
    ADD CONSTRAINT uk_review_file UNIQUE (review_id, file_id);


--
-- Name: admin_role_page_access uk_role_page; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_page_access
    ADD CONSTRAINT uk_role_page UNIQUE (role_name, page_id);


--
-- Name: admin_role_permissions uk_role_permission; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_permissions
    ADD CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id);


--
-- Name: admin_user_roles uk_user_role; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_user_roles
    ADD CONSTRAINT uk_user_role UNIQUE (admin_user_id, role_id);


--
-- Name: user_activity_logs user_activity_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activity_logs
    ADD CONSTRAINT user_activity_logs_pkey PRIMARY KEY (id);


--
-- Name: user_activity_logs user_activity_logs_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activity_logs
    ADD CONSTRAINT user_activity_logs_uuid_key UNIQUE (uuid);


--
-- Name: user_coupons user_coupons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_coupons
    ADD CONSTRAINT user_coupons_pkey PRIMARY KEY (id);


--
-- Name: user_coupons user_coupons_user_id_coupon_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_coupons
    ADD CONSTRAINT user_coupons_user_id_coupon_id_key UNIQUE (user_id, coupon_id);


--
-- Name: user_coupons user_coupons_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_coupons
    ADD CONSTRAINT user_coupons_uuid_key UNIQUE (uuid);


--
-- Name: user_devices user_devices_device_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_devices
    ADD CONSTRAINT user_devices_device_id_key UNIQUE (device_id);


--
-- Name: user_devices user_devices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_devices
    ADD CONSTRAINT user_devices_pkey PRIMARY KEY (id);


--
-- Name: user_devices user_devices_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_devices
    ADD CONSTRAINT user_devices_uuid_key UNIQUE (uuid);


--
-- Name: user_points user_points_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_points
    ADD CONSTRAINT user_points_pkey PRIMARY KEY (id);


--
-- Name: user_points user_points_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_points
    ADD CONSTRAINT user_points_user_id_key UNIQUE (user_id);


--
-- Name: user_points user_points_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_points
    ADD CONSTRAINT user_points_uuid_key UNIQUE (uuid);


--
-- Name: user_profiles user_profiles_nickname_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_nickname_key UNIQUE (nickname);


--
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (id);


--
-- Name: user_profiles user_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_key UNIQUE (user_id);


--
-- Name: user_profiles user_profiles_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_uuid_key UNIQUE (uuid);


--
-- Name: user_settings user_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT user_settings_pkey PRIMARY KEY (id);


--
-- Name: user_settings user_settings_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT user_settings_user_id_key UNIQUE (user_id);


--
-- Name: user_settings user_settings_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT user_settings_uuid_key UNIQUE (uuid);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_uuid_key UNIQUE (uuid);


--
-- Name: idx_ad_campaigns_ad_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ad_campaigns_ad_type ON public.ad_campaigns USING btree (ad_type);


--
-- Name: idx_ad_campaigns_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ad_campaigns_company_id ON public.ad_campaigns USING btree (company_id);


--
-- Name: idx_ad_campaigns_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ad_campaigns_dates ON public.ad_campaigns USING btree (start_date, end_date);


--
-- Name: idx_ad_campaigns_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ad_campaigns_status ON public.ad_campaigns USING btree (status);


--
-- Name: idx_ad_campaigns_targeting; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ad_campaigns_targeting ON public.ad_campaigns USING gin (targeting);


--
-- Name: idx_ad_campaigns_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ad_campaigns_uuid ON public.ad_campaigns USING btree (uuid);


--
-- Name: idx_admin_2fa_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_admin_2fa_user_id ON public.admin_two_factor_auth USING btree (admin_user_id);


--
-- Name: idx_admin_activity_summary_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_activity_summary_date ON public.admin_activity_summary USING btree (summary_date DESC);


--
-- Name: idx_admin_activity_summary_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_activity_summary_type ON public.admin_activity_summary USING btree (summary_type);


--
-- Name: idx_admin_activity_summary_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_activity_summary_user ON public.admin_activity_summary USING btree (admin_user_id);


--
-- Name: idx_admin_audit_logs_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_audit_logs_action ON public.admin_audit_logs USING btree (action);


--
-- Name: idx_admin_audit_logs_admin_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_audit_logs_admin_user_id ON public.admin_audit_logs USING btree (admin_user_id);


--
-- Name: idx_admin_audit_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_audit_logs_created_at ON public.admin_audit_logs USING btree (created_at DESC);


--
-- Name: idx_admin_audit_logs_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_audit_logs_entity ON public.admin_audit_logs USING btree (entity_type, entity_id);


--
-- Name: idx_admin_ip_whitelist_cidr; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_ip_whitelist_cidr ON public.admin_ip_whitelist USING btree (cidr_notation) WHERE ((is_deleted = false) AND (is_active = true));


--
-- Name: idx_admin_ip_whitelist_ip; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_ip_whitelist_ip ON public.admin_ip_whitelist USING btree (ip_address) WHERE ((is_deleted = false) AND (is_active = true));


--
-- Name: idx_admin_ip_whitelist_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_ip_whitelist_user_id ON public.admin_ip_whitelist USING btree (admin_user_id) WHERE (is_deleted = false);


--
-- Name: idx_admin_login_history_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_login_history_created_at ON public.admin_login_history USING btree (created_at DESC);


--
-- Name: idx_admin_login_history_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_login_history_email ON public.admin_login_history USING btree (email);


--
-- Name: idx_admin_login_history_ip; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_login_history_ip ON public.admin_login_history USING btree (ip_address);


--
-- Name: idx_admin_login_history_login_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_login_history_login_type ON public.admin_login_history USING btree (login_type);


--
-- Name: idx_admin_login_history_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_login_history_user_id ON public.admin_login_history USING btree (admin_user_id);


--
-- Name: idx_admin_notifications_admin_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_notifications_admin_user_id ON public.admin_notifications USING btree (admin_user_id);


--
-- Name: idx_admin_notifications_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_notifications_created_at ON public.admin_notifications USING btree (created_at DESC);


--
-- Name: idx_admin_notifications_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_notifications_entity ON public.admin_notifications USING btree (entity_type, entity_id);


--
-- Name: idx_admin_notifications_is_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_notifications_is_read ON public.admin_notifications USING btree (is_read) WHERE (is_expired = false);


--
-- Name: idx_admin_notifications_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_notifications_severity ON public.admin_notifications USING btree (severity);


--
-- Name: idx_admin_notifications_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_notifications_type ON public.admin_notifications USING btree (notification_type);


--
-- Name: idx_admin_page_permissions_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_page_permissions_code ON public.admin_page_permissions USING btree (page_code) WHERE (is_deleted = false);


--
-- Name: idx_admin_page_permissions_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_page_permissions_module ON public.admin_page_permissions USING btree (module) WHERE ((is_deleted = false) AND (is_active = true));


--
-- Name: idx_admin_page_permissions_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_page_permissions_parent ON public.admin_page_permissions USING btree (parent_page_id) WHERE (is_deleted = false);


--
-- Name: idx_admin_page_permissions_url; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_page_permissions_url ON public.admin_page_permissions USING btree (page_url) WHERE (is_deleted = false);


--
-- Name: idx_admin_password_policies_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_password_policies_priority ON public.admin_password_policies USING btree (priority DESC) WHERE ((is_deleted = false) AND (is_active = true));


--
-- Name: idx_admin_password_policies_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_password_policies_user ON public.admin_password_policies USING btree (admin_user_id) WHERE ((is_deleted = false) AND (is_active = true));


--
-- Name: idx_admin_permissions_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_permissions_code ON public.admin_permissions USING btree (permission_code);


--
-- Name: idx_admin_permissions_resource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_permissions_resource ON public.admin_permissions USING btree (resource_type);


--
-- Name: idx_admin_role_page_access_page; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_role_page_access_page ON public.admin_role_page_access USING btree (page_id) WHERE (is_active = true);


--
-- Name: idx_admin_role_page_access_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_role_page_access_role ON public.admin_role_page_access USING btree (role_name) WHERE (is_active = true);


--
-- Name: idx_admin_role_permissions_permission_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_role_permissions_permission_id ON public.admin_role_permissions USING btree (permission_id);


--
-- Name: idx_admin_role_permissions_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_role_permissions_role_id ON public.admin_role_permissions USING btree (role_id);


--
-- Name: idx_admin_roles_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_roles_code ON public.admin_roles USING btree (role_code) WHERE (is_active = true);


--
-- Name: idx_admin_roles_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_roles_priority ON public.admin_roles USING btree (priority) WHERE (is_active = true);


--
-- Name: idx_admin_sessions_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_sessions_expires_at ON public.admin_sessions USING btree (expires_at) WHERE ((status)::text = 'ACTIVE'::text);


--
-- Name: idx_admin_sessions_ip_address; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_sessions_ip_address ON public.admin_sessions USING btree (ip_address);


--
-- Name: idx_admin_sessions_session_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_sessions_session_token ON public.admin_sessions USING btree (session_token) WHERE ((status)::text = 'ACTIVE'::text);


--
-- Name: idx_admin_sessions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_sessions_status ON public.admin_sessions USING btree (status);


--
-- Name: idx_admin_sessions_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_sessions_user_id ON public.admin_sessions USING btree (admin_user_id) WHERE ((status)::text = 'ACTIVE'::text);


--
-- Name: idx_admin_settings_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_settings_category ON public.admin_settings USING btree (category);


--
-- Name: idx_admin_settings_feature_flag; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_settings_feature_flag ON public.admin_settings USING btree (is_feature_flag) WHERE (is_feature_flag = true);


--
-- Name: idx_admin_settings_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_admin_settings_key ON public.admin_settings USING btree (setting_key);


--
-- Name: idx_admin_settings_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_settings_module ON public.admin_settings USING btree (module);


--
-- Name: idx_admin_user_roles_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_user_roles_expires_at ON public.admin_user_roles USING btree (expires_at);


--
-- Name: idx_admin_user_roles_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_user_roles_role_id ON public.admin_user_roles USING btree (role_id);


--
-- Name: idx_admin_user_roles_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_user_roles_user_id ON public.admin_user_roles USING btree (admin_user_id);


--
-- Name: idx_admin_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_users_email ON public.admin_users USING btree (email);


--
-- Name: idx_admin_users_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_users_status ON public.admin_users USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_analytics_events_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_events_created_at ON public.analytics_events USING btree (created_at DESC);


--
-- Name: idx_analytics_events_event_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_events_event_name ON public.analytics_events USING btree (event_name);


--
-- Name: idx_analytics_events_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_events_user_id ON public.analytics_events USING btree (user_id);


--
-- Name: idx_boards_board_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boards_board_type ON public.boards USING btree (board_type);


--
-- Name: idx_boards_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boards_created_at ON public.boards USING btree (created_at DESC);


--
-- Name: idx_boards_tags; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boards_tags ON public.boards USING gin (tags);


--
-- Name: idx_boards_type_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boards_type_data ON public.boards USING gin (type_data);


--
-- Name: idx_boards_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boards_user_id ON public.boards USING btree (user_id);


--
-- Name: idx_boards_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boards_uuid ON public.boards USING btree (uuid);


--
-- Name: idx_companies_avg_rating; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_avg_rating ON public.companies USING btree (avg_rating DESC);


--
-- Name: idx_companies_business_info; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_business_info ON public.companies USING gin (business_info);


--
-- Name: idx_companies_images_gin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_images_gin ON public.companies USING gin (images);


--
-- Name: idx_companies_like_count; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_like_count ON public.companies USING btree (like_count DESC);


--
-- Name: idx_companies_metadata; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_metadata ON public.companies USING gin (metadata);


--
-- Name: idx_companies_owner_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_owner_id ON public.companies USING btree (owner_id);


--
-- Name: idx_companies_premium_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_premium_active ON public.companies USING btree (premium_tier, status, is_deleted) WHERE (((status)::text = 'ACTIVE'::text) AND (is_deleted = false));


--
-- Name: idx_companies_premium_monthly_amount; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_premium_monthly_amount ON public.companies USING btree (premium_monthly_amount DESC);


--
-- Name: idx_companies_premium_tier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_premium_tier ON public.companies USING btree (premium_tier);


--
-- Name: idx_companies_search; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_search ON public.companies USING gin (to_tsvector('simple'::regconfig, (((COALESCE(name, ''::character varying))::text || ' '::text) || COALESCE(description, ''::text))));


--
-- Name: idx_companies_service_areas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_service_areas ON public.companies USING gin (service_areas);


--
-- Name: idx_companies_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_companies_slug ON public.companies USING btree (slug) WHERE (slug IS NOT NULL);


--
-- Name: idx_companies_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_status ON public.companies USING btree (status);


--
-- Name: idx_companies_tags; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_tags ON public.companies USING gin (tags);


--
-- Name: idx_companies_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_uuid ON public.companies USING btree (uuid);


--
-- Name: idx_companies_view_count; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_view_count ON public.companies USING btree (view_count DESC);


--
-- Name: idx_company_filter_options_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_filter_options_company_id ON public.company_filter_options USING btree (company_id);


--
-- Name: idx_company_filter_options_filter_option_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_filter_options_filter_option_id ON public.company_filter_options USING btree (filter_option_id);


--
-- Name: idx_company_images_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_images_company_id ON public.company_images USING btree (company_id);


--
-- Name: idx_company_images_file_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_images_file_id ON public.company_images USING btree (file_id);


--
-- Name: idx_company_images_is_primary; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_images_is_primary ON public.company_images USING btree (is_primary) WHERE (is_primary = true);


--
-- Name: idx_company_images_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_images_type ON public.company_images USING btree (image_type);


--
-- Name: idx_company_likes_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_likes_company_id ON public.company_likes USING btree (company_id);


--
-- Name: idx_company_likes_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_likes_user_id ON public.company_likes USING btree (user_id);


--
-- Name: idx_company_reviews_images_gin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_company_reviews_images_gin ON public.company_reviews USING gin (images);


--
-- Name: idx_consultation_messages_consultation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultation_messages_consultation ON public.consultation_messages USING btree (consultation_type, consultation_id) WHERE (is_deleted = false);


--
-- Name: idx_consultation_messages_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultation_messages_created_at ON public.consultation_messages USING btree (created_at DESC);


--
-- Name: idx_consultation_messages_is_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultation_messages_is_read ON public.consultation_messages USING btree (is_read) WHERE (is_deleted = false);


--
-- Name: idx_consultation_messages_sender; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultation_messages_sender ON public.consultation_messages USING btree (sender_id) WHERE (is_deleted = false);


--
-- Name: idx_consultation_messages_sender_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultation_messages_sender_type ON public.consultation_messages USING btree (sender_type) WHERE (is_deleted = false);


--
-- Name: idx_consultation_messages_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultation_messages_uuid ON public.consultation_messages USING btree (uuid);


--
-- Name: idx_damoa_picks_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_damoa_picks_active ON public.damoa_picks USING btree (is_active);


--
-- Name: idx_damoa_picks_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_damoa_picks_company_id ON public.damoa_picks USING btree (company_id);


--
-- Name: idx_damoa_picks_display_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_damoa_picks_display_order ON public.damoa_picks USING btree (display_order, created_at);


--
-- Name: idx_damoa_picks_pick_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_damoa_picks_pick_type ON public.damoa_picks USING btree (pick_type);


--
-- Name: idx_damoa_picks_season; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_damoa_picks_season ON public.damoa_picks USING btree (season);


--
-- Name: idx_estimate_proposal_attachments_file; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposal_attachments_file ON public.estimate_proposal_attachments USING btree (file_id);


--
-- Name: idx_estimate_proposal_attachments_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposal_attachments_order ON public.estimate_proposal_attachments USING btree (estimate_proposal_id, display_order);


--
-- Name: idx_estimate_proposal_attachments_proposal; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposal_attachments_proposal ON public.estimate_proposal_attachments USING btree (estimate_proposal_id);


--
-- Name: idx_estimate_proposals_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposals_company_id ON public.estimate_proposals USING btree (company_id);


--
-- Name: idx_estimate_proposals_is_selected; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposals_is_selected ON public.estimate_proposals USING btree (is_selected);


--
-- Name: idx_estimate_proposals_request_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposals_request_id ON public.estimate_proposals USING btree (request_id);


--
-- Name: idx_estimate_proposals_selected_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposals_selected_at ON public.estimate_proposals USING btree (selected_at) WHERE (selected_at IS NOT NULL);


--
-- Name: idx_estimate_proposals_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposals_status ON public.estimate_proposals USING btree (status);


--
-- Name: idx_estimate_proposals_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_proposals_uuid ON public.estimate_proposals USING btree (uuid);


--
-- Name: idx_estimate_request_attachments_file; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_request_attachments_file ON public.estimate_request_attachments USING btree (file_id);


--
-- Name: idx_estimate_request_attachments_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_request_attachments_order ON public.estimate_request_attachments USING btree (estimate_request_id, display_order);


--
-- Name: idx_estimate_request_attachments_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_request_attachments_request ON public.estimate_request_attachments USING btree (estimate_request_id);


--
-- Name: idx_estimate_requests_business_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_requests_business_type ON public.estimate_requests USING btree (business_type) WHERE (is_deleted = false);


--
-- Name: idx_estimate_requests_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_requests_created_at ON public.estimate_requests USING btree (created_at DESC);


--
-- Name: idx_estimate_requests_location; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_requests_location ON public.estimate_requests USING btree (location) WHERE ((is_deleted = false) AND ((status)::text = 'PUBLISHED'::text));


--
-- Name: idx_estimate_requests_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_requests_status ON public.estimate_requests USING btree (status);


--
-- Name: idx_estimate_requests_submission_deadline; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_requests_submission_deadline ON public.estimate_requests USING btree (submission_deadline) WHERE ((is_deleted = false) AND ((status)::text = 'PUBLISHED'::text));


--
-- Name: idx_estimate_requests_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_requests_user_id ON public.estimate_requests USING btree (user_id);


--
-- Name: idx_estimate_requests_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estimate_requests_uuid ON public.estimate_requests USING btree (uuid);


--
-- Name: idx_files_category_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_files_category_id ON public.files USING btree (category_id);


--
-- Name: idx_files_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_files_created_at ON public.files USING btree (created_at);


--
-- Name: idx_files_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_files_entity ON public.files USING btree (entity_type, entity_id);


--
-- Name: idx_files_is_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_files_is_deleted ON public.files USING btree (is_deleted);


--
-- Name: idx_files_uploader_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_files_uploader_id ON public.files USING btree (uploader_id);


--
-- Name: idx_filter_categories_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_categories_active ON public.filter_categories USING btree (is_active) WHERE ((is_active = true) AND (is_deleted = false));


--
-- Name: idx_filter_categories_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_categories_code ON public.filter_categories USING btree (code);


--
-- Name: idx_filter_categories_display_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_categories_display_order ON public.filter_categories USING btree (display_order);


--
-- Name: idx_filter_categories_entity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_categories_entity_type ON public.filter_categories USING btree (entity_type);


--
-- Name: idx_filter_option_relations_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_option_relations_source ON public.filter_option_relations USING btree (source_option_id);


--
-- Name: idx_filter_option_relations_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_option_relations_target ON public.filter_option_relations USING btree (target_option_id);


--
-- Name: idx_filter_option_relations_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_option_relations_type ON public.filter_option_relations USING btree (relation_type);


--
-- Name: idx_filter_options_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_active ON public.filter_options USING btree (is_active) WHERE ((is_active = true) AND (is_deleted = false));


--
-- Name: idx_filter_options_category_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_category_active ON public.filter_options USING btree (category_id, is_active) WHERE (is_deleted = false);


--
-- Name: idx_filter_options_category_depth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_category_depth ON public.filter_options USING btree (category_id, depth) WHERE (is_deleted = false);


--
-- Name: idx_filter_options_category_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_category_id ON public.filter_options USING btree (category_id);


--
-- Name: idx_filter_options_category_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_category_parent ON public.filter_options USING btree (category_id, parent_id) WHERE (is_deleted = false);


--
-- Name: idx_filter_options_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_code ON public.filter_options USING btree (code);


--
-- Name: idx_filter_options_depth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_depth ON public.filter_options USING btree (depth);


--
-- Name: idx_filter_options_metadata; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_metadata ON public.filter_options USING gin (metadata);


--
-- Name: idx_filter_options_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_parent_id ON public.filter_options USING btree (parent_id);


--
-- Name: idx_filter_options_path; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_path ON public.filter_options USING btree (path);


--
-- Name: idx_filter_options_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_filter_options_uuid ON public.filter_options USING btree (uuid);


--
-- Name: idx_matches_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matches_company_id ON public.matches USING btree (company_id);


--
-- Name: idx_matches_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matches_status ON public.matches USING btree (status);


--
-- Name: idx_matches_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matches_user_id ON public.matches USING btree (user_id);


--
-- Name: idx_matches_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_matches_uuid ON public.matches USING btree (uuid);


--
-- Name: idx_notifications_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_created_at ON public.notifications USING btree (created_at DESC);


--
-- Name: idx_notifications_is_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_is_read ON public.notifications USING btree (is_read);


--
-- Name: idx_notifications_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user_id ON public.notifications USING btree (user_id);


--
-- Name: idx_notifications_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_uuid ON public.notifications USING btree (uuid);


--
-- Name: idx_partnership_inquiries_assigned_admin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_assigned_admin ON public.partnership_inquiries USING btree (assigned_admin_id) WHERE (is_deleted = false);


--
-- Name: idx_partnership_inquiries_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_company_id ON public.partnership_inquiries USING btree (company_id) WHERE (is_deleted = false);


--
-- Name: idx_partnership_inquiries_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_created_at ON public.partnership_inquiries USING btree (created_at DESC);


--
-- Name: idx_partnership_inquiries_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_email ON public.partnership_inquiries USING btree (email) WHERE (is_deleted = false);


--
-- Name: idx_partnership_inquiries_inquiry_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_inquiry_type ON public.partnership_inquiries USING btree (inquiry_type) WHERE (is_deleted = false);


--
-- Name: idx_partnership_inquiries_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_priority ON public.partnership_inquiries USING btree (priority) WHERE (is_deleted = false);


--
-- Name: idx_partnership_inquiries_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_status ON public.partnership_inquiries USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_partnership_inquiries_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_user_id ON public.partnership_inquiries USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_partnership_inquiries_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partnership_inquiries_uuid ON public.partnership_inquiries USING btree (uuid);


--
-- Name: idx_payments_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_created_at ON public.payments USING btree (created_at DESC);


--
-- Name: idx_payments_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_status ON public.payments USING btree (status);


--
-- Name: idx_payments_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_user_id ON public.payments USING btree (user_id);


--
-- Name: idx_payments_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_uuid ON public.payments USING btree (uuid);


--
-- Name: idx_quick_consultations_assigned_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_assigned_company ON public.quick_consultations USING btree (assigned_company_id) WHERE (is_deleted = false);


--
-- Name: idx_quick_consultations_consent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_consent ON public.quick_consultations USING btree (personal_info_consent, third_party_consent, marketing_consent) WHERE (is_deleted = false);


--
-- Name: idx_quick_consultations_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_created_at ON public.quick_consultations USING btree (created_at DESC);


--
-- Name: idx_quick_consultations_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_email ON public.quick_consultations USING btree (email) WHERE (is_deleted = false);


--
-- Name: idx_quick_consultations_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_phone ON public.quick_consultations USING btree (phone) WHERE (is_deleted = false);


--
-- Name: idx_quick_consultations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_status ON public.quick_consultations USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_quick_consultations_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_user_id ON public.quick_consultations USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_quick_consultations_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quick_consultations_uuid ON public.quick_consultations USING btree (uuid);


--
-- Name: idx_review_images_file_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_images_file_id ON public.company_review_images USING btree (file_id);


--
-- Name: idx_review_images_review_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_review_images_review_id ON public.company_review_images USING btree (review_id);


--
-- Name: idx_social_accounts_is_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_social_accounts_is_deleted ON public.social_accounts USING btree (is_deleted);


--
-- Name: idx_social_accounts_provider_not_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_social_accounts_provider_not_deleted ON public.social_accounts USING btree (provider, provider_user_id) WHERE (is_deleted = false);


--
-- Name: idx_user_activity_logs_activity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_activity_logs_activity_type ON public.user_activity_logs USING btree (activity_type);


--
-- Name: idx_user_activity_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_activity_logs_created_at ON public.user_activity_logs USING btree (created_at DESC);


--
-- Name: idx_user_activity_logs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_activity_logs_user_id ON public.user_activity_logs USING btree (user_id);


--
-- Name: idx_users_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_created_at ON public.users USING btree (created_at DESC);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_profile_completed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_profile_completed ON public.users USING btree (profile_completed);


--
-- Name: idx_users_roles; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_roles ON public.users USING gin (roles);


--
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- Name: idx_users_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_uuid ON public.users USING btree (uuid);


--
-- Name: filter_options set_filter_option_path; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER set_filter_option_path BEFORE INSERT OR UPDATE OF parent_id, code ON public.filter_options FOR EACH ROW EXECUTE FUNCTION public.update_filter_option_path();


--
-- Name: ad_campaigns update_ad_campaigns_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_ad_campaigns_updated_at BEFORE UPDATE ON public.ad_campaigns FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: admin_activity_summary update_admin_activity_summary_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_activity_summary_updated_at BEFORE UPDATE ON public.admin_activity_summary FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_ip_whitelist update_admin_ip_whitelist_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_ip_whitelist_updated_at BEFORE UPDATE ON public.admin_ip_whitelist FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_notifications update_admin_notifications_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_notifications_updated_at BEFORE UPDATE ON public.admin_notifications FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_page_permissions update_admin_page_permissions_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_page_permissions_updated_at BEFORE UPDATE ON public.admin_page_permissions FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_password_policies update_admin_password_policies_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_password_policies_updated_at BEFORE UPDATE ON public.admin_password_policies FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_permissions update_admin_permissions_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_permissions_updated_at BEFORE UPDATE ON public.admin_permissions FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_role_page_access update_admin_role_page_access_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_role_page_access_updated_at BEFORE UPDATE ON public.admin_role_page_access FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_role_permissions update_admin_role_permissions_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_role_permissions_updated_at BEFORE UPDATE ON public.admin_role_permissions FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_roles update_admin_roles_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_roles_updated_at BEFORE UPDATE ON public.admin_roles FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_sessions update_admin_sessions_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_sessions_updated_at BEFORE UPDATE ON public.admin_sessions FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_settings update_admin_settings_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_settings_updated_at BEFORE UPDATE ON public.admin_settings FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_two_factor_auth update_admin_two_factor_auth_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_two_factor_auth_updated_at BEFORE UPDATE ON public.admin_two_factor_auth FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: admin_user_roles update_admin_user_roles_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_admin_user_roles_updated_at BEFORE UPDATE ON public.admin_user_roles FOR EACH ROW EXECUTE FUNCTION public.update_admin_updated_at_column();


--
-- Name: boards update_boards_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_boards_updated_at BEFORE UPDATE ON public.boards FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: companies update_companies_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_companies_updated_at BEFORE UPDATE ON public.companies FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: company_images update_company_images_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_company_images_updated_at BEFORE UPDATE ON public.company_images FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: damoa_picks update_damoa_picks_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_damoa_picks_updated_at BEFORE UPDATE ON public.damoa_picks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: estimate_proposals update_estimate_proposals_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_estimate_proposals_updated_at BEFORE UPDATE ON public.estimate_proposals FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: estimate_requests update_estimate_requests_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_estimate_requests_updated_at BEFORE UPDATE ON public.estimate_requests FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: filter_categories update_filter_categories_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_filter_categories_updated_at BEFORE UPDATE ON public.filter_categories FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: filter_options update_filter_options_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_filter_options_updated_at BEFORE UPDATE ON public.filter_options FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: matches update_matches_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_matches_updated_at BEFORE UPDATE ON public.matches FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: partnership_inquiries update_partnership_inquiries_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_partnership_inquiries_updated_at BEFORE UPDATE ON public.partnership_inquiries FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: payments update_payments_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON public.payments FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: quick_consultations update_quick_consultations_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_quick_consultations_updated_at BEFORE UPDATE ON public.quick_consultations FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: user_profiles update_user_profiles_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON public.user_profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: user_settings update_user_settings_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON public.user_settings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: ad_campaigns ad_campaigns_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ad_campaigns
    ADD CONSTRAINT ad_campaigns_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: admin_activity_summary admin_activity_summary_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_activity_summary
    ADD CONSTRAINT admin_activity_summary_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: admin_audit_logs admin_audit_logs_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_audit_logs
    ADD CONSTRAINT admin_audit_logs_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id);


--
-- Name: admin_ip_whitelist admin_ip_whitelist_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_ip_whitelist
    ADD CONSTRAINT admin_ip_whitelist_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: admin_ip_whitelist admin_ip_whitelist_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_ip_whitelist
    ADD CONSTRAINT admin_ip_whitelist_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: admin_ip_whitelist admin_ip_whitelist_deleted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_ip_whitelist
    ADD CONSTRAINT admin_ip_whitelist_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users(id);


--
-- Name: admin_login_history admin_login_history_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_login_history
    ADD CONSTRAINT admin_login_history_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: admin_notifications admin_notifications_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_notifications
    ADD CONSTRAINT admin_notifications_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: admin_notifications admin_notifications_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_notifications
    ADD CONSTRAINT admin_notifications_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: admin_page_permissions admin_page_permissions_parent_page_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_page_permissions
    ADD CONSTRAINT admin_page_permissions_parent_page_id_fkey FOREIGN KEY (parent_page_id) REFERENCES public.admin_page_permissions(id);


--
-- Name: admin_password_policies admin_password_policies_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_password_policies
    ADD CONSTRAINT admin_password_policies_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: admin_password_policies admin_password_policies_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_password_policies
    ADD CONSTRAINT admin_password_policies_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: admin_password_policies admin_password_policies_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_password_policies
    ADD CONSTRAINT admin_password_policies_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);


--
-- Name: admin_role_page_access admin_role_page_access_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_page_access
    ADD CONSTRAINT admin_role_page_access_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: admin_role_page_access admin_role_page_access_page_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_page_access
    ADD CONSTRAINT admin_role_page_access_page_id_fkey FOREIGN KEY (page_id) REFERENCES public.admin_page_permissions(id) ON DELETE CASCADE;


--
-- Name: admin_role_page_access admin_role_page_access_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_page_access
    ADD CONSTRAINT admin_role_page_access_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);


--
-- Name: admin_role_permissions admin_role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_permissions
    ADD CONSTRAINT admin_role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.admin_permissions(id) ON DELETE CASCADE;


--
-- Name: admin_role_permissions admin_role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_role_permissions
    ADD CONSTRAINT admin_role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.admin_roles(id) ON DELETE CASCADE;


--
-- Name: admin_roles admin_roles_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_roles
    ADD CONSTRAINT admin_roles_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: admin_roles admin_roles_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_roles
    ADD CONSTRAINT admin_roles_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);


--
-- Name: admin_sessions admin_sessions_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_sessions
    ADD CONSTRAINT admin_sessions_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: admin_settings admin_settings_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_settings
    ADD CONSTRAINT admin_settings_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.users(id);


--
-- Name: admin_settings admin_settings_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_settings
    ADD CONSTRAINT admin_settings_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: admin_settings admin_settings_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_settings
    ADD CONSTRAINT admin_settings_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);


--
-- Name: admin_two_factor_auth admin_two_factor_auth_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_two_factor_auth
    ADD CONSTRAINT admin_two_factor_auth_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: admin_user_roles admin_user_roles_admin_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_user_roles
    ADD CONSTRAINT admin_user_roles_admin_user_id_fkey FOREIGN KEY (admin_user_id) REFERENCES public.admin_users(id) ON DELETE CASCADE;


--
-- Name: admin_user_roles admin_user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_user_roles
    ADD CONSTRAINT admin_user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.admin_roles(id) ON DELETE CASCADE;


--
-- Name: analytics_events analytics_events_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.analytics_events
    ADD CONSTRAINT analytics_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: board_attachments board_attachments_board_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_attachments
    ADD CONSTRAINT board_attachments_board_id_fkey FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;


--
-- Name: board_categories board_categories_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_categories
    ADD CONSTRAINT board_categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.board_categories(id) ON DELETE CASCADE;


--
-- Name: board_comments board_comments_board_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_comments
    ADD CONSTRAINT board_comments_board_id_fkey FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;


--
-- Name: board_comments board_comments_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_comments
    ADD CONSTRAINT board_comments_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.board_comments(id) ON DELETE CASCADE;


--
-- Name: board_comments board_comments_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_comments
    ADD CONSTRAINT board_comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: board_likes board_likes_board_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_likes
    ADD CONSTRAINT board_likes_board_id_fkey FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;


--
-- Name: board_likes board_likes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.board_likes
    ADD CONSTRAINT board_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: boards boards_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: boards boards_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);


--
-- Name: boards boards_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: companies companies_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: company_certifications company_certifications_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certifications
    ADD CONSTRAINT company_certifications_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: company_certifications company_certifications_verified_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_certifications
    ADD CONSTRAINT company_certifications_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(id);


--
-- Name: company_filter_options company_filter_options_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_filter_options
    ADD CONSTRAINT company_filter_options_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: company_filter_options company_filter_options_filter_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_filter_options
    ADD CONSTRAINT company_filter_options_filter_option_id_fkey FOREIGN KEY (filter_option_id) REFERENCES public.filter_options(id) ON DELETE CASCADE;


--
-- Name: company_images company_images_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_images
    ADD CONSTRAINT company_images_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: company_portfolios company_portfolios_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_portfolios
    ADD CONSTRAINT company_portfolios_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: company_review_images company_review_images_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_review_images
    ADD CONSTRAINT company_review_images_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.files(id) ON DELETE CASCADE;


--
-- Name: company_review_images company_review_images_review_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_review_images
    ADD CONSTRAINT company_review_images_review_id_fkey FOREIGN KEY (review_id) REFERENCES public.company_reviews(id) ON DELETE CASCADE;


--
-- Name: company_reviews company_reviews_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_reviews
    ADD CONSTRAINT company_reviews_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: company_reviews company_reviews_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_reviews
    ADD CONSTRAINT company_reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: consultation_messages consultation_messages_deleted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users(id);


--
-- Name: consultation_messages consultation_messages_read_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_read_by_fkey FOREIGN KEY (read_by) REFERENCES public.users(id);


--
-- Name: consultation_messages consultation_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: credit_transactions credit_transactions_credit_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions
    ADD CONSTRAINT credit_transactions_credit_id_fkey FOREIGN KEY (credit_id) REFERENCES public.credits(id);


--
-- Name: credit_transactions credit_transactions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions
    ADD CONSTRAINT credit_transactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: credits credits_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credits
    ADD CONSTRAINT credits_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: damoa_picks damoa_picks_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damoa_picks
    ADD CONSTRAINT damoa_picks_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: email_verifications email_verifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications
    ADD CONSTRAINT email_verifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: estimate_messages estimate_messages_proposal_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_messages
    ADD CONSTRAINT estimate_messages_proposal_id_fkey FOREIGN KEY (proposal_id) REFERENCES public.estimate_proposals(id) ON DELETE CASCADE;


--
-- Name: estimate_messages estimate_messages_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_messages
    ADD CONSTRAINT estimate_messages_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.estimate_requests(id) ON DELETE CASCADE;


--
-- Name: estimate_messages estimate_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_messages
    ADD CONSTRAINT estimate_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: estimate_proposals estimate_proposals_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposals
    ADD CONSTRAINT estimate_proposals_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: estimate_proposals estimate_proposals_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposals
    ADD CONSTRAINT estimate_proposals_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.estimate_requests(id) ON DELETE CASCADE;


--
-- Name: estimate_requests estimate_requests_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_requests
    ADD CONSTRAINT estimate_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: estimate_templates estimate_templates_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_templates
    ADD CONSTRAINT estimate_templates_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: file_downloads file_downloads_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_downloads
    ADD CONSTRAINT file_downloads_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: files files_uploader_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_uploader_id_fkey FOREIGN KEY (uploader_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: filter_option_relations filter_option_relations_source_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_option_relations
    ADD CONSTRAINT filter_option_relations_source_option_id_fkey FOREIGN KEY (source_option_id) REFERENCES public.filter_options(id) ON DELETE CASCADE;


--
-- Name: filter_option_relations filter_option_relations_target_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_option_relations
    ADD CONSTRAINT filter_option_relations_target_option_id_fkey FOREIGN KEY (target_option_id) REFERENCES public.filter_options(id) ON DELETE CASCADE;


--
-- Name: filter_options filter_options_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_options
    ADD CONSTRAINT filter_options_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.filter_categories(id) ON DELETE CASCADE;


--
-- Name: filter_options filter_options_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.filter_options
    ADD CONSTRAINT filter_options_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.filter_options(id) ON DELETE CASCADE;


--
-- Name: company_likes fk_company_likes_company; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_likes
    ADD CONSTRAINT fk_company_likes_company FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: company_likes fk_company_likes_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_likes
    ADD CONSTRAINT fk_company_likes_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: estimate_proposal_attachments fk_estimate_proposal_attachments_file; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposal_attachments
    ADD CONSTRAINT fk_estimate_proposal_attachments_file FOREIGN KEY (file_id) REFERENCES public.files(id) ON DELETE CASCADE;


--
-- Name: estimate_proposal_attachments fk_estimate_proposal_attachments_proposal; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_proposal_attachments
    ADD CONSTRAINT fk_estimate_proposal_attachments_proposal FOREIGN KEY (estimate_proposal_id) REFERENCES public.estimate_proposals(id) ON DELETE CASCADE;


--
-- Name: estimate_request_attachments fk_estimate_request_attachments_file; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_request_attachments
    ADD CONSTRAINT fk_estimate_request_attachments_file FOREIGN KEY (file_id) REFERENCES public.files(id) ON DELETE CASCADE;


--
-- Name: estimate_request_attachments fk_estimate_request_attachments_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estimate_request_attachments
    ADD CONSTRAINT fk_estimate_request_attachments_request FOREIGN KEY (estimate_request_id) REFERENCES public.estimate_requests(id) ON DELETE CASCADE;


--
-- Name: invoices invoices_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: invoices invoices_payment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payments(id);


--
-- Name: invoices invoices_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: match_reviews match_reviews_match_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_reviews
    ADD CONSTRAINT match_reviews_match_id_fkey FOREIGN KEY (match_id) REFERENCES public.matches(id) ON DELETE CASCADE;


--
-- Name: match_reviews match_reviews_reviewee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_reviews
    ADD CONSTRAINT match_reviews_reviewee_id_fkey FOREIGN KEY (reviewee_id) REFERENCES public.users(id);


--
-- Name: match_reviews match_reviews_reviewer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_reviews
    ADD CONSTRAINT match_reviews_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES public.users(id);


--
-- Name: matches matches_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matches
    ADD CONSTRAINT matches_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: matches matches_proposal_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matches
    ADD CONSTRAINT matches_proposal_id_fkey FOREIGN KEY (proposal_id) REFERENCES public.estimate_proposals(id);


--
-- Name: matches matches_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matches
    ADD CONSTRAINT matches_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.estimate_requests(id);


--
-- Name: matches matches_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.matches
    ADD CONSTRAINT matches_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: notification_logs notification_logs_notification_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_logs
    ADD CONSTRAINT notification_logs_notification_id_fkey FOREIGN KEY (notification_id) REFERENCES public.notifications(id) ON DELETE CASCADE;


--
-- Name: notification_settings notification_settings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_settings
    ADD CONSTRAINT notification_settings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: partnership_inquiries partnership_inquiries_assigned_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries
    ADD CONSTRAINT partnership_inquiries_assigned_admin_id_fkey FOREIGN KEY (assigned_admin_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: partnership_inquiries partnership_inquiries_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries
    ADD CONSTRAINT partnership_inquiries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE SET NULL;


--
-- Name: partnership_inquiries partnership_inquiries_deleted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries
    ADD CONSTRAINT partnership_inquiries_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users(id);


--
-- Name: partnership_inquiries partnership_inquiries_responded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries
    ADD CONSTRAINT partnership_inquiries_responded_by_fkey FOREIGN KEY (responded_by) REFERENCES public.users(id);


--
-- Name: partnership_inquiries partnership_inquiries_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partnership_inquiries
    ADD CONSTRAINT partnership_inquiries_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: payment_methods payment_methods_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_methods
    ADD CONSTRAINT payment_methods_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: payments payments_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: quick_consultations quick_consultations_assigned_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations
    ADD CONSTRAINT quick_consultations_assigned_by_fkey FOREIGN KEY (assigned_by) REFERENCES public.users(id);


--
-- Name: quick_consultations quick_consultations_assigned_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations
    ADD CONSTRAINT quick_consultations_assigned_company_id_fkey FOREIGN KEY (assigned_company_id) REFERENCES public.companies(id) ON DELETE SET NULL;


--
-- Name: quick_consultations quick_consultations_deleted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations
    ADD CONSTRAINT quick_consultations_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users(id);


--
-- Name: quick_consultations quick_consultations_responded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations
    ADD CONSTRAINT quick_consultations_responded_by_fkey FOREIGN KEY (responded_by) REFERENCES public.users(id);


--
-- Name: quick_consultations quick_consultations_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quick_consultations
    ADD CONSTRAINT quick_consultations_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: refunds refunds_approved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT refunds_approved_by_fkey FOREIGN KEY (approved_by) REFERENCES public.users(id);


--
-- Name: refunds refunds_payment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT refunds_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payments(id);


--
-- Name: refunds refunds_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT refunds_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: saved_searches saved_searches_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_searches
    ADD CONSTRAINT saved_searches_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: sms_verifications sms_verifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sms_verifications
    ADD CONSTRAINT sms_verifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: social_accounts social_accounts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_accounts
    ADD CONSTRAINT social_accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_activity_logs user_activity_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activity_logs
    ADD CONSTRAINT user_activity_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: user_coupons user_coupons_coupon_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_coupons
    ADD CONSTRAINT user_coupons_coupon_id_fkey FOREIGN KEY (coupon_id) REFERENCES public.coupons(id);


--
-- Name: user_coupons user_coupons_payment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_coupons
    ADD CONSTRAINT user_coupons_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payments(id);


--
-- Name: user_coupons user_coupons_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_coupons
    ADD CONSTRAINT user_coupons_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_devices user_devices_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_devices
    ADD CONSTRAINT user_devices_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_points user_points_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_points
    ADD CONSTRAINT user_points_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_profiles user_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_settings user_settings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT user_settings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: users users_deleted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--


-- ============================================================================
-- Filter 템플릿 데이터 (Template Data)
-- ============================================================================

--
-- Data for Name: filter_categories
--

COPY public.filter_categories (id, uuid, code, name, description, entity_type, filter_type, supports_hierarchy, max_depth, display_order, icon, is_active, is_required, metadata, created_at, updated_at, is_deleted, deleted_at) FROM stdin;
1	afd17dd3-c6f3-44b4-8a09-bc63bbc0b8f6	region	지역	서비스 제공 지역	COMPANY	MULTI_SELECT	t	3	1	\N	t	f	{}	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
2	d0b93095-d2e4-4e39-9333-9c8a300d45ad	department	진료과	병원 진료 과목	COMPANY	MULTI_SELECT	t	2	2	\N	t	f	{}	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
3	2c624f8f-c999-458a-9ef1-dc0144e79296	specialty	전문영역	인테리어 전문 분야	COMPANY	MULTI_SELECT	f	1	3	\N	t	f	{}	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
4	9a98e72b-a4a6-45ce-a0ef-58a57cef5f2c	price_range	가격대	프로젝트 예산 범위	COMPANY	SINGLE_SELECT	f	1	4	\N	t	f	{}	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
5	c62422c3-aa82-4cea-954f-9e30ffe13ed5	rating	평점	업체 평점	COMPANY	SINGLE_SELECT	f	1	5	\N	t	f	{}	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
6	27b466ec-2f2b-4da5-a698-45cfa1f39368	project_size	작업평수	프로젝트 규모 (평수)	COMPANY	SINGLE_SELECT	f	1	6	\N	t	f	{}	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
7	57a20a7b-c9da-4583-b392-4bef3a1afd86	project_size_range	작업 평수	프로젝트 시공 가능 면적	COMPANY	MULTI_SELECT	f	1	6	\N	t	f	{}	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
\.

--
-- Data for Name: filter_options
--

COPY public.filter_options (id, uuid, category_id, code, name, short_name, description, parent_id, depth, path, metadata, display_order, icon, color, is_active, is_default, usage_count, created_at, updated_at, is_deleted, deleted_at) FROM stdin;
26	818684fd-fae1-46d9-9568-c41deb6e41c6	2	ent	이비인후과	\N	\N	\N	0	/ent	{}	7	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
27	c4355ec9-ca30-4a3f-a595-0b20c71be4d4	2	dentistry	치과	\N	\N	\N	0	/dentistry	{}	8	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
28	39c50210-2476-4a01-b36f-6e33fe6c2769	3	residential	주거공간	\N	\N	\N	0	/residential	{}	1	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
29	acae0636-1845-439f-acfc-2fc1348ccc84	3	commercial	상업공간	\N	\N	\N	0	/commercial	{}	2	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
30	ad966114-4f27-404a-89d0-236923bc6647	3	office	사무공간	\N	\N	\N	0	/office	{}	3	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
31	9bb40a27-d913-4821-8686-1a96ca6052e6	3	medical	의료공간	\N	\N	\N	0	/medical	{}	4	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
32	3d469da6-87de-4098-8290-7193f5b24730	3	remodeling	리모델링	\N	\N	\N	0	/remodeling	{}	5	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
33	5edcbab1-d2c3-4d58-9c50-d84c13359810	3	new-construction	신축	\N	\N	\N	0	/new-construction	{}	6	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
34	97340cc6-b9eb-45f0-8f61-2804acf416ce	3	extension	증축	\N	\N	\N	0	/extension	{}	7	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
35	869b2ba0-6ba9-4dc0-a58e-8e08e1ef6dae	4	under-5m	500만원 이하	\N	\N	\N	0	/under-5m	{}	1	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
36	a2a80622-7863-4a6d-b730-661c02b467b9	4	5m-10m	500만원 ~ 1,000만원	\N	\N	\N	0	/5m-10m	{}	2	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
37	5c3e9f64-435b-4bb9-9775-d3505e3435de	4	10m-30m	1,000만원 ~ 3,000만원	\N	\N	\N	0	/10m-30m	{}	3	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
38	78e4b341-dc92-4b5d-ae56-d00727292f18	4	30m-50m	3,000만원 ~ 5,000만원	\N	\N	\N	0	/30m-50m	{}	4	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
39	6793ce0c-c594-4f3b-aaac-4095e642712c	4	over-50m	5,000만원 이상	\N	\N	\N	0	/over-50m	{}	5	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
40	032c80a3-73d2-4a10-823d-6eb655b8b958	5	rating-5	5점	\N	\N	\N	0	/rating-5	{}	1	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
41	da1f9d71-23fd-4942-b690-62a0b743d305	5	rating-4-plus	4점 이상	\N	\N	\N	0	/rating-4-plus	{}	2	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
42	7e86a1b2-62e9-4ef0-bfa4-eb3563b02320	5	rating-3-plus	3점 이상	\N	\N	\N	0	/rating-3-plus	{}	3	\N	\N	t	f	0	2025-11-03 05:37:05.986119+00	2025-11-03 05:37:05.986119+00	f	\N
43	a9ae6450-eea3-4a5e-866b-e8de0b9325b9	6	under-10	10평 이하	\N	\N	\N	0	/under-10	{}	1	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
44	11a02fb3-cf1f-43d9-a225-ab6b6badaf60	6	10-20	10평 ~ 20평	\N	\N	\N	0	/10-20	{}	2	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
45	79a45154-62f9-4849-8e94-252d535eb32a	6	20-30	20평 ~ 30평	\N	\N	\N	0	/20-30	{}	3	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
46	64ca400a-cd07-4bc0-80a4-fe4a366ebeee	6	30-50	30평 ~ 50평	\N	\N	\N	0	/30-50	{}	4	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
47	8cc8d133-d122-4f24-9a74-376d781cf91f	6	over-50	50평 이상	\N	\N	\N	0	/over-50	{}	5	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
48	9cb3ca35-da3c-4600-a93b-dbb0659ab773	6	any-size	평수 무관	\N	\N	\N	0	/any-size	{}	6	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
49	3dcf8bf8-8330-48d9-bb74-fb3b339e23f6	3	interior-design	인테리어 디자인	\N	\N	\N	0	/interior-design	{}	10	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
50	9d700c8b-8464-4922-9d75-7e467165e192	3	interior-construction	인테리어 시공	\N	\N	\N	0	/interior-construction	{}	11	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
51	5664931e-cfa7-4151-a1f3-4de6ee1292e8	3	furniture-custom	가구 제작	\N	\N	\N	0	/furniture-custom	{}	12	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
52	861ee08d-1332-4733-83f3-fced960bcee6	3	marketing-online	온라인 마케팅	\N	\N	\N	0	/marketing-online	{}	20	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
53	e87ecb48-ba5d-4069-981a-67fdd7162375	3	marketing-sns	SNS 마케팅	\N	\N	\N	0	/marketing-sns	{}	21	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
54	42279ff8-cba3-424a-bd20-4cbe141ad7a7	3	marketing-brand	브랜딩	\N	\N	\N	0	/marketing-brand	{}	22	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
55	c57da9bd-c226-4538-976f-36831f2c9614	3	web-development	홈페이지 제작	\N	\N	\N	0	/web-development	{}	30	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
56	d4b30096-5640-4851-8a0b-7937b13a025e	3	mobile-app	모바일 앱 개발	\N	\N	\N	0	/mobile-app	{}	31	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
57	d830bf80-22ff-46ca-b620-177e7160b003	3	system-development	시스템 개발	\N	\N	\N	0	/system-development	{}	32	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
58	a10445a0-c149-45a2-bed3-25ce115cb358	3	cleaning-office	사무실 청소	\N	\N	\N	0	/cleaning-office	{}	40	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
59	380cc95b-a730-488d-b3d4-aaf11274fc92	3	cleaning-home	가정 청소	\N	\N	\N	0	/cleaning-home	{}	41	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
60	619dd687-2ac5-4d3d-babc-537adc2711a9	3	cleaning-move	이사 청소	\N	\N	\N	0	/cleaning-move	{}	42	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
61	ce098da6-dc61-4e67-bf49-0096376cc7b0	3	cleaning-special	특수 청소	\N	\N	\N	0	/cleaning-special	{}	43	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
62	077737a1-e4f2-4bb9-82ff-14868488ad5a	3	ac-install	에어컨 설치	\N	\N	\N	0	/ac-install	{}	50	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
63	1335eb04-fc0c-40a6-8911-c7967f57b956	3	ac-repair	에어컨 수리	\N	\N	\N	0	/ac-repair	{}	51	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
64	bf85f0f2-9933-476b-beda-ef7044fe8963	3	ac-maintenance	에어컨 관리	\N	\N	\N	0	/ac-maintenance	{}	52	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
65	a7e29aa3-aeb8-4d9b-8f22-beea7ae2106d	3	heating-install	난방 설치	\N	\N	\N	0	/heating-install	{}	53	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
66	1d41aa6f-6bc6-4c55-9119-8d5e548e17c0	3	electric-work	전기 공사	\N	\N	\N	0	/electric-work	{}	60	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
67	2ec2b82e-94f7-4bea-a7e2-008b6553c4ee	3	plumbing	배관 공사	\N	\N	\N	0	/plumbing	{}	61	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
68	31d2d3bb-9d94-4734-a41d-3c368c4eb3d7	3	waterproof	방수 공사	\N	\N	\N	0	/waterproof	{}	62	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
69	3cc36113-4bb0-4fa6-8612-4fb1cd97ff60	3	moving	이사	\N	\N	\N	0	/moving	{}	70	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
70	3630b6b1-12a2-4fa4-8bc2-85143a631bf4	3	painting	도배/페인팅	\N	\N	\N	0	/painting	{}	71	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
71	f6cd4aad-f7a4-4b2a-9e34-57529e85b399	3	window-door	창호/샷시	\N	\N	\N	0	/window-door	{}	72	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
72	c565cece-7675-453b-a9cd-8f15400382d9	3	flooring	바닥재 시공	\N	\N	\N	0	/flooring	{}	73	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
73	dee48051-f243-4a6e-8120-3dd5943ba182	3	garden	조경/정원	\N	\N	\N	0	/garden	{}	74	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
74	312b9895-b1bf-4228-8728-1f8d4467c89a	3	pest-control	방역/해충 방제	\N	\N	\N	0	/pest-control	{}	75	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
75	252b4f67-2c1c-4e06-8ede-90b35b35e74d	2	neurology	신경과	\N	\N	\N	0	/neurology	{}	9	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
76	60db9e4e-f47a-4c3b-96a0-204c21c59a91	2	psychiatry	정신과	\N	\N	\N	0	/psychiatry	{}	10	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
77	1f837565-7f02-4aa9-9396-3ef6907d8038	2	obgyn	산부인과	\N	\N	\N	0	/obgyn	{}	11	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
78	cac456c8-1a06-4036-800a-0ed2023a5c65	2	pediatrics	소아과	\N	\N	\N	0	/pediatrics	{}	12	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
79	5cee356f-ad1f-46b7-a65a-3b28d217985a	2	urology	비뇨기과	\N	\N	\N	0	/urology	{}	13	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
80	901c7364-cc39-4900-8a97-3783cd8abb31	2	family-medicine	가정의학과	\N	\N	\N	0	/family-medicine	{}	14	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
81	af762cc9-1365-46ea-859c-aafc7604faa0	2	rehabilitation	재활의학과	\N	\N	\N	0	/rehabilitation	{}	15	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
82	d5d489b9-3cb9-4b8a-ba1c-906c6e466ba9	2	radiology	영상의학과	\N	\N	\N	0	/radiology	{}	16	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
83	985e6582-a4af-44e3-af39-52b455c883cd	2	anesthesiology	마취통증의학과	\N	\N	\N	0	/anesthesiology	{}	17	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
84	9e1f6c3f-bb20-4b88-bf90-94b358cbea1c	2	emergency	응급의학과	\N	\N	\N	0	/emergency	{}	18	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
85	493d9ffd-d19d-4772-a8ed-f0ef77295d29	2	lab-medicine	진단검사의학과	\N	\N	\N	0	/lab-medicine	{}	19	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
86	7d1845b6-8cb6-4d74-bff4-8e99a049c98f	2	any-department	진료과 무관	\N	\N	\N	0	/any-department	{}	99	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
87	47dfca48-4350-451c-bdb2-0975208ebb7c	1	sejong	세종	\N	\N	\N	0	/sejong	{}	9	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
88	38977b20-068d-4737-a7e8-483ef7922355	1	gangwon	강원	\N	\N	\N	0	/gangwon	{}	10	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
89	ead3cebc-37a2-42d2-ba5e-a2cb8fb1165e	1	chungbuk	충북	\N	\N	\N	0	/chungbuk	{}	11	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
90	a1bdfeff-9fde-4517-9e93-5479f5e285f9	1	chungnam	충남	\N	\N	\N	0	/chungnam	{}	12	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
91	9103a3ba-dfb7-4acf-8e31-95bc3d7b45d3	1	jeonbuk	전북	\N	\N	\N	0	/jeonbuk	{}	13	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
92	3711c945-dac8-4212-8096-343e14be00c3	1	jeonnam	전남	\N	\N	\N	0	/jeonnam	{}	14	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
93	091cacfa-3c73-4da9-8eaf-de5fcf0433ee	1	gyeongbuk	경북	\N	\N	\N	0	/gyeongbuk	{}	15	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
94	6d22c36b-9edb-41a1-8bc3-44d9e8a9c89a	1	gyeongnam	경남	\N	\N	\N	0	/gyeongnam	{}	16	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
95	3144fe2d-edde-4df6-92aa-24dbf047894e	1	jeju	제주	\N	\N	\N	0	/jeju	{}	17	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
96	829cd480-2316-4f61-9d68-fb6614db4e82	1	all-region	전국	\N	\N	\N	0	/all-region	{}	99	\N	\N	t	f	0	2025-11-05 04:48:20.891546+00	2025-11-05 04:48:20.891546+00	f	\N
97	79667ad5-72d3-4c15-a5a7-b4ddf28db9c4	7	all-sizes	전체 가능	\N	\N	\N	0	/all-sizes	{}	1	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
98	16fd1fe3-f012-4358-b070-c3dea6c9fcc1	7	under-10pyeong	10평 이하	\N	\N	\N	0	/under-10pyeong	{}	2	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
99	ddc353e5-ed69-4f4c-84c1-63b449e2e970	7	10-30pyeong	10평 ~ 30평	\N	\N	\N	0	/10-30pyeong	{}	3	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
100	247fb256-66e6-42ae-88eb-937424769034	7	30-50pyeong	30평 ~ 50평	\N	\N	\N	0	/30-50pyeong	{}	4	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
101	d82bb7de-96bf-4702-9ab9-69859e1ef9bc	7	50-100pyeong	50평 ~ 100평	\N	\N	\N	0	/50-100pyeong	{}	5	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
102	24719292-c4ad-4f45-aa20-3009a17f26d7	7	over-100pyeong	100평 이상	\N	\N	\N	0	/over-100pyeong	{}	6	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
103	8387fe70-a5e3-4daf-815f-4064c3968bd2	7	no-limit	평수 무관	\N	\N	\N	0	/no-limit	{}	7	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
105	52ba24fa-0f8f-4037-89b7-08cc61bdcb60	3	home-styling	홈스타일링	\N	\N	\N	0	/home-styling	{}	9	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
106	8ebf5806-60c6-415b-b6b5-654f67998639	3	furniture	가구/목공	\N	\N	\N	0	/furniture	{}	10	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
108	68b317d0-2cd4-4709-a364-f5f94cd94315	3	wallpaper	도배/벽지	\N	\N	\N	0	/wallpaper	{}	12	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
110	5e326037-f26c-4835-b42f-3116bb3a600b	3	lighting	조명 설치	\N	\N	\N	0	/lighting	{}	14	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
111	19c50566-d82f-4974-b883-06bc694ccb93	3	window	창호/유리	\N	\N	\N	0	/window	{}	15	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
112	93e53cce-aa80-4b7f-ba46-9df94d01844e	3	marketing	마케팅/광고	\N	\N	\N	0	/marketing	{}	20	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
113	87ce3d43-3b20-4999-aa5e-06090fdee4a4	3	web-dev	홈페이지 제작	\N	\N	\N	0	/web-dev	{}	21	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
114	93d112fb-7d4e-4e1d-bc93-20afc98020af	3	seo	SEO/검색최적화	\N	\N	\N	0	/seo	{}	22	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
115	3fe7f62e-1a58-41d4-ac0d-1be4831a20c8	3	sns-marketing	SNS 마케팅	\N	\N	\N	0	/sns-marketing	{}	23	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
116	800f06e7-49d2-4d74-8107-d430b5354e56	3	video-production	영상 제작	\N	\N	\N	0	/video-production	{}	24	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
117	ef134b04-09c0-4ede-a148-e30be5b2c775	3	photography	사진 촬영	\N	\N	\N	0	/photography	{}	25	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
118	fed70b38-c208-4fda-afd2-9086a129475a	3	graphic-design	그래픽 디자인	\N	\N	\N	0	/graphic-design	{}	26	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
119	1917123d-20fa-4cbb-ba48-b39ce972234b	3	cleaning	청소/방역	\N	\N	\N	0	/cleaning	{}	30	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
120	cd8ca867-6b7d-42e7-b446-5eb860dc5e4a	3	air-conditioner	에어컨 설치/수리	\N	\N	\N	0	/air-conditioner	{}	31	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
121	d6674065-e71c-40fb-80b1-b0d7bdf0a149	3	internet	인터넷/통신	\N	\N	\N	0	/internet	{}	32	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
122	8931310f-af44-4361-9912-3ba8de823959	3	electrical	전기 공사/수리	\N	\N	\N	0	/electrical	{}	33	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
124	af89ac62-99f8-443c-8c0d-9ddf65d2414d	3	waterproofing	방수 공사	\N	\N	\N	0	/waterproofing	{}	35	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
125	a77a3b7f-0753-4f8d-9f66-b289665acae4	3	locksmith	자물쇠/보안	\N	\N	\N	0	/locksmith	{}	36	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
127	6f46ebb1-8f0c-4739-84c6-0f0481f77954	3	storage	창고/보관	\N	\N	\N	0	/storage	{}	38	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
128	f2cf790f-4b7b-4d05-90ad-2b0baa434c7f	3	hospital-interior	병원 인테리어	\N	\N	\N	0	/hospital-interior	{}	40	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
129	0dbc2f56-292a-439c-9c53-9570570235de	3	medical-equipment	의료 장비 설치	\N	\N	\N	0	/medical-equipment	{}	41	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
130	99ac0f3b-75d4-45e5-88a6-d7274c0273b7	3	sterilization	멸균/소독	\N	\N	\N	0	/sterilization	{}	42	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
131	3742d665-3b4d-44bc-a8a5-bfb22bb861e8	3	consulting	컨설팅	\N	\N	\N	0	/consulting	{}	50	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
132	54e5d6c4-1b46-4674-a526-800d6f142d6e	3	accounting	회계/세무	\N	\N	\N	0	/accounting	{}	51	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
133	c5cc282a-d100-42a3-9f31-bdbac696a26e	3	legal	법무/법률	\N	\N	\N	0	/legal	{}	52	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
134	a4e5fa31-4211-4b9f-9f14-9899a4f408b7	3	insurance	보험	\N	\N	\N	0	/insurance	{}	53	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
135	23135c27-3971-4e50-b7ef-95c03031c8d4	3	real-estate	부동산	\N	\N	\N	0	/real-estate	{}	54	\N	\N	t	f	0	2025-11-06 08:05:30.76898+00	2025-11-06 08:05:30.76898+00	f	\N
\.


--
-- Data for Name: company_filter_options; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.company_filter_options (id, company_id, filter_option_id, created_at) FROM stdin;
1	10	1	2025-11-06 08:43:52.424467+00
2	10	2	2025-11-06 08:43:52.424467+00
3	10	4	2025-11-06 08:43:52.424467+00
4	10	7	2025-11-06 08:43:52.424467+00
5	10	8	2025-11-06 08:43:52.424467+00
6	10	11	2025-11-06 08:43:52.424467+00
7	10	12	2025-11-06 08:43:52.424467+00
8	10	16	2025-11-06 08:43:52.424467+00
9	10	18	2025-11-06 08:43:52.424467+00
10	10	19	2025-11-06 08:43:52.424467+00
11	10	20	2025-11-06 08:43:52.424467+00
12	10	21	2025-11-06 08:43:52.424467+00
13	10	23	2025-11-06 08:43:52.424467+00
14	10	25	2025-11-06 08:43:52.424467+00
15	10	28	2025-11-06 08:43:52.424467+00
16	10	29	2025-11-06 08:43:52.424467+00
17	10	31	2025-11-06 08:43:52.424467+00
18	10	32	2025-11-06 08:43:52.424467+00
19	10	34	2025-11-06 08:43:52.424467+00
20	10	105	2025-11-06 08:43:52.424467+00
21	11	3	2025-11-10 00:19:36.873874+00
22	11	21	2025-11-10 00:19:36.873874+00
23	11	28	2025-11-10 00:19:36.873874+00
24	11	57	2025-11-10 00:19:36.873874+00
--

SELECT pg_catalog.setval('public.company_filter_options_id_seq', 24, true);


--
-- Name: filter_categories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.filter_categories_id_seq', 7, true);


--
-- Name: filter_option_relations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.filter_option_relations_id_seq', 1, false);


--
-- Name: filter_options_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.filter_options_id_seq', 135, true);


--
-- PostgreSQL database dump complete
--

