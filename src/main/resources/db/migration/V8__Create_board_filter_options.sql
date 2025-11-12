--
-- Board Filter Options Join Table
-- Board와 Filter Options간의 다대다 관계를 위한 조인 테이블
--

CREATE TABLE public.board_filter_options (
    id bigint NOT NULL,
    board_id bigint NOT NULL,
    filter_option_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_board_filter UNIQUE(board_id, filter_option_id)
);

--
-- Comments
--

COMMENT ON TABLE public.board_filter_options IS 'Board와 FilterOption 다대다 조인 테이블';
COMMENT ON COLUMN public.board_filter_options.id IS 'Primary key';
COMMENT ON COLUMN public.board_filter_options.board_id IS 'boards 테이블 FK';
COMMENT ON COLUMN public.board_filter_options.filter_option_id IS 'filter_options 테이블 FK';
COMMENT ON COLUMN public.board_filter_options.created_at IS '생성 일시';

--
-- Sequence
--

CREATE SEQUENCE public.board_filter_options_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.board_filter_options_id_seq OWNED BY public.board_filter_options.id;

--
-- Set sequence
--

ALTER TABLE ONLY public.board_filter_options ALTER COLUMN id SET DEFAULT nextval('public.board_filter_options_id_seq'::regclass);

--
-- Primary Key
--

ALTER TABLE ONLY public.board_filter_options
    ADD CONSTRAINT board_filter_options_pkey PRIMARY KEY (id);

--
-- Foreign Keys
--

ALTER TABLE ONLY public.board_filter_options
    ADD CONSTRAINT fk_board_filter_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.board_filter_options
    ADD CONSTRAINT fk_board_filter_option FOREIGN KEY (filter_option_id) REFERENCES public.filter_options(id) ON DELETE RESTRICT;

--
-- Indexes
--

CREATE INDEX idx_board_filter_board_id ON public.board_filter_options USING btree (board_id);
CREATE INDEX idx_board_filter_option_id ON public.board_filter_options USING btree (filter_option_id);
