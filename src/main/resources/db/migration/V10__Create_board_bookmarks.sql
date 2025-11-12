--
-- Board Bookmarks Table
-- 사용자별 게시글 북마크 관리 테이블
--

CREATE TABLE public.board_bookmarks (
    id bigint NOT NULL,
    board_id bigint NOT NULL,
    user_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_board_bookmark UNIQUE(board_id, user_id)
);

--
-- Comments
--

COMMENT ON TABLE public.board_bookmarks IS '게시글 북마크 테이블';
COMMENT ON COLUMN public.board_bookmarks.id IS 'Primary key';
COMMENT ON COLUMN public.board_bookmarks.board_id IS 'boards 테이블 FK';
COMMENT ON COLUMN public.board_bookmarks.user_id IS 'users 테이블 FK';
COMMENT ON COLUMN public.board_bookmarks.created_at IS '북마크 생성 일시';

--
-- Sequence
--

CREATE SEQUENCE public.board_bookmarks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.board_bookmarks_id_seq OWNED BY public.board_bookmarks.id;

--
-- Set sequence
--

ALTER TABLE ONLY public.board_bookmarks ALTER COLUMN id SET DEFAULT nextval('public.board_bookmarks_id_seq'::regclass);

--
-- Primary Key
--

ALTER TABLE ONLY public.board_bookmarks
    ADD CONSTRAINT board_bookmarks_pkey PRIMARY KEY (id);

--
-- Foreign Keys
--

ALTER TABLE ONLY public.board_bookmarks
    ADD CONSTRAINT fk_board_bookmark_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.board_bookmarks
    ADD CONSTRAINT fk_board_bookmark_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Indexes
--

CREATE INDEX idx_board_bookmark_board_id ON public.board_bookmarks USING btree (board_id);
CREATE INDEX idx_board_bookmark_user_id ON public.board_bookmarks USING btree (user_id);
CREATE INDEX idx_board_bookmark_user_board ON public.board_bookmarks USING btree (user_id, board_id);
