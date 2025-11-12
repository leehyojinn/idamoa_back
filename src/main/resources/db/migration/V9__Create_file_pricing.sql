--
-- File Pricing Table
-- 파일 다운로드 과금 정보 관리 테이블
--

CREATE TABLE public.file_pricing (
    id bigint NOT NULL,
    file_id bigint NOT NULL,
    is_paid boolean DEFAULT false NOT NULL,
    price integer DEFAULT 0 NOT NULL,
    currency character varying(10) DEFAULT 'KRW' NOT NULL,
    download_limit integer,
    is_active boolean DEFAULT true NOT NULL,
    description text,
    metadata jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by bigint,
    updated_by bigint,
    CONSTRAINT ck_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_download_limit_positive CHECK (download_limit IS NULL OR download_limit > 0),
    CONSTRAINT uk_file_pricing_file_id UNIQUE(file_id)
);

--
-- Comments
--

COMMENT ON TABLE public.file_pricing IS '파일 다운로드 과금 정보 테이블';
COMMENT ON COLUMN public.file_pricing.id IS 'Primary key';
COMMENT ON COLUMN public.file_pricing.file_id IS 'files 테이블 FK (1:1 관계)';
COMMENT ON COLUMN public.file_pricing.is_paid IS '유료 파일 여부';
COMMENT ON COLUMN public.file_pricing.price IS '다운로드 가격 (기본: KRW)';
COMMENT ON COLUMN public.file_pricing.currency IS '통화 (KRW, USD 등)';
COMMENT ON COLUMN public.file_pricing.download_limit IS '최대 다운로드 횟수 제한 (NULL=무제한)';
COMMENT ON COLUMN public.file_pricing.is_active IS '과금 활성화 여부';
COMMENT ON COLUMN public.file_pricing.description IS '가격 정책 설명';
COMMENT ON COLUMN public.file_pricing.metadata IS '추가 메타데이터 (할인 정보 등)';

--
-- Sequence
--

CREATE SEQUENCE public.file_pricing_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.file_pricing_id_seq OWNED BY public.file_pricing.id;

--
-- Set sequence
--

ALTER TABLE ONLY public.file_pricing ALTER COLUMN id SET DEFAULT nextval('public.file_pricing_id_seq'::regclass);

--
-- Primary Key
--

ALTER TABLE ONLY public.file_pricing
    ADD CONSTRAINT file_pricing_pkey PRIMARY KEY (id);

--
-- Foreign Keys
--

ALTER TABLE ONLY public.file_pricing
    ADD CONSTRAINT fk_file_pricing_file FOREIGN KEY (file_id) REFERENCES public.files(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.file_pricing
    ADD CONSTRAINT fk_file_pricing_created_by FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.file_pricing
    ADD CONSTRAINT fk_file_pricing_updated_by FOREIGN KEY (updated_by) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Indexes
--

CREATE INDEX idx_file_pricing_file_id ON public.file_pricing USING btree (file_id);
CREATE INDEX idx_file_pricing_is_paid ON public.file_pricing USING btree (is_paid) WHERE is_paid = true;
CREATE INDEX idx_file_pricing_is_active ON public.file_pricing USING btree (is_active) WHERE is_active = true;
