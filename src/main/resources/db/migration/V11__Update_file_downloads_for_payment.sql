--
-- Update file_downloads table for payment tracking
-- 파일 다운로드 테이블에 결제 정보 추가
--

ALTER TABLE public.file_downloads
    ADD COLUMN payment_id bigint,
    ADD COLUMN is_free boolean DEFAULT true NOT NULL,
    ADD COLUMN price_paid integer DEFAULT 0 NOT NULL,
    ADD CONSTRAINT ck_price_paid_non_negative CHECK (price_paid >= 0);

--
-- Comments
--

COMMENT ON COLUMN public.file_downloads.payment_id IS '결제 ID (payments 테이블 FK, NULL=무료 다운로드)';
COMMENT ON COLUMN public.file_downloads.is_free IS '무료 다운로드 여부';
COMMENT ON COLUMN public.file_downloads.price_paid IS '실제 지불한 금액 (KRW)';

--
-- Foreign Key
--

ALTER TABLE ONLY public.file_downloads
    ADD CONSTRAINT fk_file_download_payment FOREIGN KEY (payment_id) REFERENCES public.payments(id) ON DELETE SET NULL;

--
-- Indexes
--

CREATE INDEX idx_file_downloads_payment_id ON public.file_downloads USING btree (payment_id) WHERE payment_id IS NOT NULL;
CREATE INDEX idx_file_downloads_is_free ON public.file_downloads USING btree (is_free);
CREATE INDEX idx_file_downloads_file_user ON public.file_downloads USING btree (file_id, user_id);
