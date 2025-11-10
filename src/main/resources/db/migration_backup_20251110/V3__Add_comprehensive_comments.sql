-- ==============================================================================
-- V3__Add_comprehensive_comments.sql
-- HIP Damoa (Hospital Interior Platform) 데이터베이스 전체 COMMENT 추가
-- 작성일: 2025-10-31
-- 내용: 모든 테이블과 컬럼에 한글 설명 추가
-- ==============================================================================

-- ==============================================================================
-- 1. 사용자 도메인 테이블 COMMENT
-- ==============================================================================

-- users 테이블
COMMENT ON TABLE users IS '사용자 기본 정보 및 인증 관리 테이블';
COMMENT ON COLUMN users.id IS '사용자 고유 ID (Primary Key)';
COMMENT ON COLUMN users.uuid IS '외부 API용 고유 식별자 (UUID)';
COMMENT ON COLUMN users.email IS '사용자 이메일 (로그인 ID)';
COMMENT ON COLUMN users.password IS '암호화된 비밀번호 (bcrypt)';
COMMENT ON COLUMN users.role IS '사용자 역할 (USER:일반, COMPANY:업체, DESIGNER:디자이너, ADMIN:관리자)';
COMMENT ON COLUMN users.status IS '계정 상태 (PENDING:대기, ACTIVE:활성, INACTIVE:비활성, SUSPENDED:정지)';
COMMENT ON COLUMN users.email_verified IS '이메일 인증 여부';
COMMENT ON COLUMN users.email_verified_at IS '이메일 인증 완료 일시';
COMMENT ON COLUMN users.phone_verified IS '휴대폰 인증 여부';
COMMENT ON COLUMN users.phone_verified_at IS '휴대폰 인증 완료 일시';
COMMENT ON COLUMN users.identity_verified IS '본인 인증 여부';
COMMENT ON COLUMN users.identity_verified_at IS '본인 인증 완료 일시';
COMMENT ON COLUMN users.last_login_at IS '마지막 로그인 일시';
COMMENT ON COLUMN users.last_login_ip IS '마지막 로그인 IP 주소';
COMMENT ON COLUMN users.login_count IS '총 로그인 횟수';
COMMENT ON COLUMN users.failed_login_count IS '로그인 실패 횟수';
COMMENT ON COLUMN users.locked_until IS '계정 잠금 해제 시간';
COMMENT ON COLUMN users.terms_agreed_at IS '이용약관 동의 일시';
COMMENT ON COLUMN users.privacy_agreed_at IS '개인정보처리방침 동의 일시';
COMMENT ON COLUMN users.marketing_agreed_at IS '마케팅 수신 동의 일시';
COMMENT ON COLUMN users.created_at IS '계정 생성 일시';
COMMENT ON COLUMN users.updated_at IS '정보 수정 일시';
COMMENT ON COLUMN users.is_deleted IS '삭제 여부 (Soft Delete)';
COMMENT ON COLUMN users.deleted_at IS '삭제 일시';
COMMENT ON COLUMN users.deleted_by IS '삭제한 사용자 ID';
COMMENT ON COLUMN users.metadata IS '확장 데이터 (JSON 형식)';

-- user_profiles 테이블
COMMENT ON TABLE user_profiles IS '사용자 프로필 확장 정보 테이블';
COMMENT ON COLUMN user_profiles.id IS '프로필 고유 ID';
COMMENT ON COLUMN user_profiles.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN user_profiles.user_id IS '사용자 ID (users 테이블 참조)';
COMMENT ON COLUMN user_profiles.name IS '사용자 실명';
COMMENT ON COLUMN user_profiles.nickname IS '닉네임 (고유값)';
COMMENT ON COLUMN user_profiles.bio IS '자기소개';
COMMENT ON COLUMN user_profiles.avatar_url IS '프로필 이미지 URL';
COMMENT ON COLUMN user_profiles.phone IS '연락처';
COMMENT ON COLUMN user_profiles.birth_date IS '생년월일';
COMMENT ON COLUMN user_profiles.gender IS '성별';
COMMENT ON COLUMN user_profiles.address IS '주소';
COMMENT ON COLUMN user_profiles.address_detail IS '상세 주소';
COMMENT ON COLUMN user_profiles.postal_code IS '우편번호';
COMMENT ON COLUMN user_profiles.latitude IS '위도 좌표';
COMMENT ON COLUMN user_profiles.longitude IS '경도 좌표';
COMMENT ON COLUMN user_profiles.social_links IS 'SNS 링크 정보 (JSON: facebook, instagram, blog 등)';
COMMENT ON COLUMN user_profiles.interests IS '관심사 태그 배열';
COMMENT ON COLUMN user_profiles.created_at IS '프로필 생성 일시';
COMMENT ON COLUMN user_profiles.updated_at IS '프로필 수정 일시';
COMMENT ON COLUMN user_profiles.is_deleted IS '삭제 여부';
COMMENT ON COLUMN user_profiles.deleted_at IS '삭제 일시';

-- user_settings 테이블
COMMENT ON TABLE user_settings IS '사용자 개인 설정 테이블';
COMMENT ON COLUMN user_settings.id IS '설정 고유 ID';
COMMENT ON COLUMN user_settings.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN user_settings.user_id IS '사용자 ID';
COMMENT ON COLUMN user_settings.notification_settings IS '알림 설정 (JSON: email, sms, push 등)';
COMMENT ON COLUMN user_settings.preferences IS '개인화 설정 (JSON: language, timezone, theme 등)';
COMMENT ON COLUMN user_settings.ui_settings IS 'UI/UX 설정 (JSON: layout, font_size 등)';
COMMENT ON COLUMN user_settings.created_at IS '설정 생성 일시';
COMMENT ON COLUMN user_settings.updated_at IS '설정 수정 일시';

-- user_activity_logs 테이블
COMMENT ON TABLE user_activity_logs IS '사용자 활동 로그 테이블';
COMMENT ON COLUMN user_activity_logs.id IS '로그 고유 ID';
COMMENT ON COLUMN user_activity_logs.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN user_activity_logs.user_id IS '사용자 ID';
COMMENT ON COLUMN user_activity_logs.activity_type IS '활동 유형 (page_view, click, search, download 등)';
COMMENT ON COLUMN user_activity_logs.entity_type IS '대상 엔티티 타입';
COMMENT ON COLUMN user_activity_logs.entity_id IS '대상 엔티티 ID';
COMMENT ON COLUMN user_activity_logs.action IS '수행한 액션';
COMMENT ON COLUMN user_activity_logs.ip_address IS 'IP 주소';
COMMENT ON COLUMN user_activity_logs.user_agent IS '브라우저/디바이스 정보';
COMMENT ON COLUMN user_activity_logs.referer IS '참조 URL';
COMMENT ON COLUMN user_activity_logs.session_id IS '세션 ID';
COMMENT ON COLUMN user_activity_logs.metadata IS '추가 활동 데이터 (JSON)';
COMMENT ON COLUMN user_activity_logs.created_at IS '활동 발생 일시';

-- user_devices 테이블
COMMENT ON TABLE user_devices IS '사용자 디바이스 및 푸시 토큰 관리 테이블';
COMMENT ON COLUMN user_devices.id IS '디바이스 고유 ID';
COMMENT ON COLUMN user_devices.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN user_devices.user_id IS '사용자 ID';
COMMENT ON COLUMN user_devices.device_id IS '디바이스 고유 식별자';
COMMENT ON COLUMN user_devices.device_type IS '디바이스 유형 (mobile, tablet, desktop)';
COMMENT ON COLUMN user_devices.device_name IS '디바이스 이름';
COMMENT ON COLUMN user_devices.os IS '운영체제 (iOS, Android, Windows 등)';
COMMENT ON COLUMN user_devices.os_version IS '운영체제 버전';
COMMENT ON COLUMN user_devices.app_version IS '앱 버전';
COMMENT ON COLUMN user_devices.push_token IS '푸시 알림 토큰 (FCM/APNS)';
COMMENT ON COLUMN user_devices.push_enabled IS '푸시 알림 활성화 여부';
COMMENT ON COLUMN user_devices.is_active IS '디바이스 활성 상태';
COMMENT ON COLUMN user_devices.last_active_at IS '마지막 활동 시간';
COMMENT ON COLUMN user_devices.created_at IS '디바이스 등록 일시';
COMMENT ON COLUMN user_devices.updated_at IS '정보 수정 일시';

-- social_accounts 테이블
COMMENT ON TABLE social_accounts IS '소셜 로그인 연동 정보 테이블';
COMMENT ON COLUMN social_accounts.id IS '연동 정보 고유 ID';
COMMENT ON COLUMN social_accounts.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN social_accounts.user_id IS '사용자 ID';
COMMENT ON COLUMN social_accounts.provider IS '소셜 제공자 (kakao, naver, google, facebook)';
COMMENT ON COLUMN social_accounts.provider_id IS '제공자측 사용자 ID';
COMMENT ON COLUMN social_accounts.provider_email IS '제공자측 이메일';
COMMENT ON COLUMN social_accounts.provider_name IS '제공자측 이름';
COMMENT ON COLUMN social_accounts.provider_image IS '제공자측 프로필 이미지';
COMMENT ON COLUMN social_accounts.access_token IS '액세스 토큰';
COMMENT ON COLUMN social_accounts.refresh_token IS '리프레시 토큰';
COMMENT ON COLUMN social_accounts.expires_at IS '토큰 만료 시간';
COMMENT ON COLUMN social_accounts.raw_data IS '제공자 원본 데이터 (JSON)';
COMMENT ON COLUMN social_accounts.created_at IS '연동 일시';
COMMENT ON COLUMN social_accounts.updated_at IS '정보 갱신 일시';

-- user_points 테이블
COMMENT ON TABLE user_points IS '사용자 포인트/마일리지 관리 테이블';
COMMENT ON COLUMN user_points.id IS '포인트 정보 고유 ID';
COMMENT ON COLUMN user_points.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN user_points.user_id IS '사용자 ID';
COMMENT ON COLUMN user_points.balance IS '현재 포인트 잔액';
COMMENT ON COLUMN user_points.total_earned IS '총 적립 포인트';
COMMENT ON COLUMN user_points.total_used IS '총 사용 포인트';
COMMENT ON COLUMN user_points.level IS '사용자 레벨';
COMMENT ON COLUMN user_points.experience IS '경험치';
COMMENT ON COLUMN user_points.created_at IS '생성 일시';
COMMENT ON COLUMN user_points.updated_at IS '수정 일시';

-- ==============================================================================
-- 2. 업체 도메인 테이블 COMMENT
-- ==============================================================================

-- companies 테이블
COMMENT ON TABLE companies IS '업체 통합 정보 테이블';
COMMENT ON COLUMN companies.id IS '업체 고유 ID';
COMMENT ON COLUMN companies.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN companies.user_id IS '업체 계정 사용자 ID';
COMMENT ON COLUMN companies.name IS '업체명';
COMMENT ON COLUMN companies.description IS '업체 소개';
COMMENT ON COLUMN companies.logo_url IS '업체 로고 이미지 URL';
COMMENT ON COLUMN companies.cover_image_url IS '업체 커버 이미지 URL';
COMMENT ON COLUMN companies.website_url IS '업체 웹사이트 URL';
COMMENT ON COLUMN companies.business_info IS '사업자 정보 (JSON: 사업자번호, 대표자명, 업종 등)';
COMMENT ON COLUMN companies.business_hours IS '영업시간 (JSON: 요일별 open/close 시간)';
COMMENT ON COLUMN companies.service_areas IS '서비스 제공 지역 (배열)';
COMMENT ON COLUMN companies.coordinates IS '업체 위치 좌표 (JSON: lat, lng, address)';
COMMENT ON COLUMN companies.phone IS '대표 전화번호';
COMMENT ON COLUMN companies.email IS '대표 이메일';
COMMENT ON COLUMN companies.fax IS '팩스 번호';
COMMENT ON COLUMN companies.tags IS '업체 태그/해시태그 (배열)';
COMMENT ON COLUMN companies.keywords IS 'SEO 검색 키워드 (배열)';
COMMENT ON COLUMN companies.specialties IS '전문 분야 (배열)';
COMMENT ON COLUMN companies.avg_rating IS '평균 평점 (1-5)';
COMMENT ON COLUMN companies.review_count IS '리뷰 개수';
COMMENT ON COLUMN companies.portfolio_count IS '포트폴리오 개수';
COMMENT ON COLUMN companies.completed_count IS '완료 프로젝트 수';
COMMENT ON COLUMN companies.is_verified IS '인증 업체 여부';
COMMENT ON COLUMN companies.verified_at IS '인증 일시';
COMMENT ON COLUMN companies.is_premium IS '프리미엄 업체 여부';
COMMENT ON COLUMN companies.premium_until IS '프리미엄 만료일';
COMMENT ON COLUMN companies.settings IS '업체 설정 (JSON)';
COMMENT ON COLUMN companies.created_at IS '등록 일시';
COMMENT ON COLUMN companies.updated_at IS '수정 일시';
COMMENT ON COLUMN companies.is_deleted IS '삭제 여부';
COMMENT ON COLUMN companies.deleted_at IS '삭제 일시';
COMMENT ON COLUMN companies.metadata IS '확장 데이터 (JSON)';
COMMENT ON COLUMN companies.detail_content IS 'HTML/Markdown 형식의 상세 소개';
COMMENT ON COLUMN companies.detail_content_format IS '상세 소개 형식 (HTML 또는 MARKDOWN)';
COMMENT ON COLUMN companies.primary_phone IS '대표 전화번호';
COMMENT ON COLUMN companies.secondary_phone IS '보조 전화번호';
COMMENT ON COLUMN companies.emergency_contact IS '긴급 연락처';
COMMENT ON COLUMN companies.kakao_chat_url IS '카카오톡 채팅 상담 URL';
COMMENT ON COLUMN companies.social_links IS 'SNS 링크 (JSON: facebook, instagram, youtube 등)';
COMMENT ON COLUMN companies.business_hours_note IS '영업시간 특이사항/공지';

-- company_services 테이블
COMMENT ON TABLE company_services IS '업체 제공 서비스 테이블';
COMMENT ON COLUMN company_services.id IS '서비스 고유 ID';
COMMENT ON COLUMN company_services.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN company_services.company_id IS '업체 ID';
COMMENT ON COLUMN company_services.name IS '서비스명';
COMMENT ON COLUMN company_services.description IS '서비스 설명';
COMMENT ON COLUMN company_services.category IS '서비스 카테고리';
COMMENT ON COLUMN company_services.price_type IS '가격 유형 (FIXED:고정가, HOURLY:시간당, PROJECT:프로젝트, NEGOTIABLE:협의)';
COMMENT ON COLUMN company_services.min_price IS '최소 가격';
COMMENT ON COLUMN company_services.max_price IS '최대 가격';
COMMENT ON COLUMN company_services.price_unit IS '가격 단위';
COMMENT ON COLUMN company_services.options IS '서비스 옵션 (JSON)';
COMMENT ON COLUMN company_services.images IS '서비스 이미지 URL (배열)';
COMMENT ON COLUMN company_services.is_active IS '활성 상태';
COMMENT ON COLUMN company_services.display_order IS '표시 순서';
COMMENT ON COLUMN company_services.created_at IS '생성 일시';
COMMENT ON COLUMN company_services.updated_at IS '수정 일시';
COMMENT ON COLUMN company_services.is_deleted IS '삭제 여부';
COMMENT ON COLUMN company_services.deleted_at IS '삭제 일시';

-- company_portfolios 테이블
COMMENT ON TABLE company_portfolios IS '업체 포트폴리오 테이블';
COMMENT ON COLUMN company_portfolios.id IS '포트폴리오 고유 ID';
COMMENT ON COLUMN company_portfolios.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN company_portfolios.company_id IS '업체 ID';
COMMENT ON COLUMN company_portfolios.title IS '포트폴리오 제목';
COMMENT ON COLUMN company_portfolios.description IS '포트폴리오 설명';
COMMENT ON COLUMN company_portfolios.category IS '포트폴리오 카테고리';
COMMENT ON COLUMN company_portfolios.project_type IS '프로젝트 유형';
COMMENT ON COLUMN company_portfolios.project_scale IS '프로젝트 규모';
COMMENT ON COLUMN company_portfolios.project_duration IS '프로젝트 기간 (일)';
COMMENT ON COLUMN company_portfolios.project_date IS '프로젝트 완료일';
COMMENT ON COLUMN company_portfolios.budget_range IS '예산 범위';
COMMENT ON COLUMN company_portfolios.actual_cost IS '실제 비용';
COMMENT ON COLUMN company_portfolios.images IS '이미지 URL 목록 (배열)';
COMMENT ON COLUMN company_portfolios.videos IS '동영상 URL 목록 (배열)';
COMMENT ON COLUMN company_portfolios.thumbnail_url IS '썸네일 이미지 URL';
COMMENT ON COLUMN company_portfolios.tags IS '태그 (배열)';
COMMENT ON COLUMN company_portfolios.view_count IS '조회수';
COMMENT ON COLUMN company_portfolios.like_count IS '좋아요 수';
COMMENT ON COLUMN company_portfolios.is_featured IS '대표 포트폴리오 여부';
COMMENT ON COLUMN company_portfolios.is_public IS '공개 여부';
COMMENT ON COLUMN company_portfolios.display_order IS '표시 순서';
COMMENT ON COLUMN company_portfolios.created_at IS '생성 일시';
COMMENT ON COLUMN company_portfolios.updated_at IS '수정 일시';
COMMENT ON COLUMN company_portfolios.is_deleted IS '삭제 여부';
COMMENT ON COLUMN company_portfolios.deleted_at IS '삭제 일시';
COMMENT ON COLUMN company_portfolios.metadata IS '확장 데이터 (JSON)';

-- company_reviews 테이블
COMMENT ON TABLE company_reviews IS '업체 리뷰 및 평점 테이블';
COMMENT ON COLUMN company_reviews.id IS '리뷰 고유 ID';
COMMENT ON COLUMN company_reviews.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN company_reviews.company_id IS '업체 ID';
COMMENT ON COLUMN company_reviews.user_id IS '리뷰 작성자 ID';
COMMENT ON COLUMN company_reviews.match_id IS '매칭 ID (매칭을 통한 리뷰인 경우)';
COMMENT ON COLUMN company_reviews.rating IS '전체 평점 (1-5)';
COMMENT ON COLUMN company_reviews.quality_rating IS '품질 평점 (1-5)';
COMMENT ON COLUMN company_reviews.price_rating IS '가격 평점 (1-5)';
COMMENT ON COLUMN company_reviews.service_rating IS '서비스 평점 (1-5)';
COMMENT ON COLUMN company_reviews.time_rating IS '시간준수 평점 (1-5)';
COMMENT ON COLUMN company_reviews.title IS '리뷰 제목';
COMMENT ON COLUMN company_reviews.content IS '리뷰 내용';
COMMENT ON COLUMN company_reviews.images IS '리뷰 이미지 URL (배열)';
COMMENT ON COLUMN company_reviews.reply IS '업체 답변';
COMMENT ON COLUMN company_reviews.reply_at IS '업체 답변 일시';
COMMENT ON COLUMN company_reviews.is_verified IS '인증된 리뷰 여부';
COMMENT ON COLUMN company_reviews.is_reported IS '신고된 리뷰 여부';
COMMENT ON COLUMN company_reviews.is_hidden IS '숨김 처리 여부';
COMMENT ON COLUMN company_reviews.helpful_count IS '도움됨 수';
COMMENT ON COLUMN company_reviews.created_at IS '작성 일시';
COMMENT ON COLUMN company_reviews.updated_at IS '수정 일시';
COMMENT ON COLUMN company_reviews.is_deleted IS '삭제 여부';
COMMENT ON COLUMN company_reviews.deleted_at IS '삭제 일시';

-- company_certifications 테이블
COMMENT ON TABLE company_certifications IS '업체 자격증 및 인증 테이블';
COMMENT ON COLUMN company_certifications.id IS '인증 정보 고유 ID';
COMMENT ON COLUMN company_certifications.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN company_certifications.company_id IS '업체 ID';
COMMENT ON COLUMN company_certifications.cert_type IS '인증 유형';
COMMENT ON COLUMN company_certifications.cert_name IS '인증/자격증명';
COMMENT ON COLUMN company_certifications.cert_number IS '인증/자격증 번호';
COMMENT ON COLUMN company_certifications.issuer IS '발급 기관';
COMMENT ON COLUMN company_certifications.issue_date IS '발급일';
COMMENT ON COLUMN company_certifications.expiry_date IS '만료일';
COMMENT ON COLUMN company_certifications.is_verified IS '검증 완료 여부';
COMMENT ON COLUMN company_certifications.verified_at IS '검증 일시';
COMMENT ON COLUMN company_certifications.verified_by IS '검증한 관리자 ID';
COMMENT ON COLUMN company_certifications.document_url IS '인증서 문서 URL';
COMMENT ON COLUMN company_certifications.created_at IS '등록 일시';
COMMENT ON COLUMN company_certifications.updated_at IS '수정 일시';
COMMENT ON COLUMN company_certifications.is_deleted IS '삭제 여부';
COMMENT ON COLUMN company_certifications.deleted_at IS '삭제 일시';

-- company_branches 테이블
COMMENT ON TABLE company_branches IS '업체 지점 정보 테이블';
COMMENT ON COLUMN company_branches.id IS '지점 고유 ID';
COMMENT ON COLUMN company_branches.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN company_branches.company_id IS '업체 ID';
COMMENT ON COLUMN company_branches.name IS '지점명';
COMMENT ON COLUMN company_branches.branch_type IS '지점 유형 (MAIN:본점, SUB:지점, FRANCHISE:가맹점)';
COMMENT ON COLUMN company_branches.address IS '지점 주소';
COMMENT ON COLUMN company_branches.address_detail IS '상세 주소';
COMMENT ON COLUMN company_branches.postal_code IS '우편번호';
COMMENT ON COLUMN company_branches.latitude IS '위도';
COMMENT ON COLUMN company_branches.longitude IS '경도';
COMMENT ON COLUMN company_branches.phone IS '지점 전화번호';
COMMENT ON COLUMN company_branches.email IS '지점 이메일';
COMMENT ON COLUMN company_branches.manager_name IS '지점 담당자명';
COMMENT ON COLUMN company_branches.business_hours IS '영업시간 (JSON)';
COMMENT ON COLUMN company_branches.is_active IS '활성 상태';
COMMENT ON COLUMN company_branches.created_at IS '생성 일시';
COMMENT ON COLUMN company_branches.updated_at IS '수정 일시';
COMMENT ON COLUMN company_branches.is_deleted IS '삭제 여부';
COMMENT ON COLUMN company_branches.deleted_at IS '삭제 일시';

-- company_statistics 테이블
COMMENT ON TABLE company_statistics IS '업체 통계 데이터 테이블';
COMMENT ON COLUMN company_statistics.id IS '통계 고유 ID';
COMMENT ON COLUMN company_statistics.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN company_statistics.company_id IS '업체 ID';
COMMENT ON COLUMN company_statistics.stat_date IS '통계 날짜';
COMMENT ON COLUMN company_statistics.stat_type IS '통계 타입 (DAILY:일별, MONTHLY:월별)';
COMMENT ON COLUMN company_statistics.profile_views IS '프로필 조회수';
COMMENT ON COLUMN company_statistics.portfolio_views IS '포트폴리오 조회수';
COMMENT ON COLUMN company_statistics.service_views IS '서비스 조회수';
COMMENT ON COLUMN company_statistics.estimate_requests IS '견적 요청 수';
COMMENT ON COLUMN company_statistics.estimate_proposals IS '견적 제안 수';
COMMENT ON COLUMN company_statistics.matches IS '매칭 성사 수';
COMMENT ON COLUMN company_statistics.reviews IS '리뷰 수';
COMMENT ON COLUMN company_statistics.ad_impressions IS '광고 노출수';
COMMENT ON COLUMN company_statistics.ad_clicks IS '광고 클릭수';
COMMENT ON COLUMN company_statistics.revenue IS '매출액';
COMMENT ON COLUMN company_statistics.daily_data IS '일별 상세 데이터 (JSON)';
COMMENT ON COLUMN company_statistics.created_at IS '생성 일시';

-- company_images 테이블
COMMENT ON TABLE company_images IS '업체 이미지 관리 테이블 (복수 이미지)';
COMMENT ON COLUMN company_images.id IS '이미지 고유 ID';
COMMENT ON COLUMN company_images.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN company_images.company_id IS '업체 ID';
COMMENT ON COLUMN company_images.image_url IS '이미지 URL';
COMMENT ON COLUMN company_images.image_type IS '이미지 유형 (LOGO, COVER, GALLERY, INTERIOR, EXTERIOR, CERTIFICATE, PORTFOLIO)';
COMMENT ON COLUMN company_images.title IS '이미지 제목';
COMMENT ON COLUMN company_images.description IS '이미지 설명';
COMMENT ON COLUMN company_images.width IS '이미지 너비 (픽셀)';
COMMENT ON COLUMN company_images.height IS '이미지 높이 (픽셀)';
COMMENT ON COLUMN company_images.file_size IS '파일 크기 (바이트)';
COMMENT ON COLUMN company_images.mime_type IS 'MIME 타입';
COMMENT ON COLUMN company_images.display_order IS '표시 순서';
COMMENT ON COLUMN company_images.is_primary IS '대표 이미지 여부';
COMMENT ON COLUMN company_images.is_active IS '활성 상태';
COMMENT ON COLUMN company_images.created_at IS '업로드 일시';
COMMENT ON COLUMN company_images.updated_at IS '수정 일시';
COMMENT ON COLUMN company_images.is_deleted IS '삭제 여부';
COMMENT ON COLUMN company_images.deleted_at IS '삭제 일시';

-- ==============================================================================
-- 3. 광고 시스템 테이블 COMMENT
-- ==============================================================================

-- ad_campaigns 테이블
COMMENT ON TABLE ad_campaigns IS '광고 캠페인 관리 테이블';
COMMENT ON COLUMN ad_campaigns.id IS '캠페인 고유 ID';
COMMENT ON COLUMN ad_campaigns.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN ad_campaigns.company_id IS '광고주 업체 ID';
COMMENT ON COLUMN ad_campaigns.name IS '캠페인명';
COMMENT ON COLUMN ad_campaigns.description IS '캠페인 설명';
COMMENT ON COLUMN ad_campaigns.ad_type IS '광고 타입 (LISTING:상위노출, AI_RECOMMENDATION:AI추천, BANNER:배너, POPUP:팝업)';
COMMENT ON COLUMN ad_campaigns.status IS '캠페인 상태 (DRAFT, ACTIVE, PAUSED, COMPLETED, CANCELLED)';
COMMENT ON COLUMN ad_campaigns.ad_config IS '광고 타입별 상세 설정 (JSONB)';
COMMENT ON COLUMN ad_campaigns.targeting IS '타겟팅 설정 (JSONB: 지역, 나이, 관심사 등)';
COMMENT ON COLUMN ad_campaigns.budget_type IS '예산 타입 (DAILY:일별, TOTAL:전체, UNLIMITED:무제한)';
COMMENT ON COLUMN ad_campaigns.budget_amount IS '예산 금액';
COMMENT ON COLUMN ad_campaigns.daily_budget IS '일일 예산';
COMMENT ON COLUMN ad_campaigns.total_spent IS '총 소진 금액';
COMMENT ON COLUMN ad_campaigns.total_value_30d IS '30일 환산 총 광고 가치';
COMMENT ON COLUMN ad_campaigns.priority_score IS '우선순위 점수 (30일 환산 가치 기반)';
COMMENT ON COLUMN ad_campaigns.secondary_score IS '2차 정렬 점수 (평점, 리뷰 기반)';
COMMENT ON COLUMN ad_campaigns.is_premium IS '프리미엄 광고 여부';
COMMENT ON COLUMN ad_campaigns.premium_until IS '프리미엄 종료일시';
COMMENT ON COLUMN ad_campaigns.start_date IS '캠페인 시작일';
COMMENT ON COLUMN ad_campaigns.end_date IS '캠페인 종료일';
COMMENT ON COLUMN ad_campaigns.total_impressions IS '총 노출수';
COMMENT ON COLUMN ad_campaigns.total_clicks IS '총 클릭수';
COMMENT ON COLUMN ad_campaigns.total_conversions IS '총 전환수';
COMMENT ON COLUMN ad_campaigns.created_at IS '생성 일시';
COMMENT ON COLUMN ad_campaigns.updated_at IS '수정 일시';
COMMENT ON COLUMN ad_campaigns.is_deleted IS '삭제 여부';
COMMENT ON COLUMN ad_campaigns.deleted_at IS '삭제 일시';
COMMENT ON COLUMN ad_campaigns.metadata IS '확장 데이터 (JSONB)';

-- ad_creatives 테이블
COMMENT ON TABLE ad_creatives IS '광고 소재 관리 테이블';
COMMENT ON COLUMN ad_creatives.id IS '소재 고유 ID';
COMMENT ON COLUMN ad_creatives.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN ad_creatives.campaign_id IS '캠페인 ID';
COMMENT ON COLUMN ad_creatives.name IS '소재명';
COMMENT ON COLUMN ad_creatives.creative_type IS '소재 유형 (IMAGE, VIDEO, TEXT, HTML)';
COMMENT ON COLUMN ad_creatives.title IS '광고 제목';
COMMENT ON COLUMN ad_creatives.description IS '광고 설명';
COMMENT ON COLUMN ad_creatives.call_to_action IS '행동 유도 문구';
COMMENT ON COLUMN ad_creatives.image_url IS '이미지 URL';
COMMENT ON COLUMN ad_creatives.video_url IS '동영상 URL';
COMMENT ON COLUMN ad_creatives.thumbnail_url IS '썸네일 URL';
COMMENT ON COLUMN ad_creatives.landing_url IS '랜딩 페이지 URL';
COMMENT ON COLUMN ad_creatives.display_url IS '표시 URL';
COMMENT ON COLUMN ad_creatives.is_primary IS '주 소재 여부';
COMMENT ON COLUMN ad_creatives.weight IS 'A/B 테스트 가중치';
COMMENT ON COLUMN ad_creatives.impressions IS '노출수';
COMMENT ON COLUMN ad_creatives.clicks IS '클릭수';
COMMENT ON COLUMN ad_creatives.is_active IS '활성 상태';
COMMENT ON COLUMN ad_creatives.created_at IS '생성 일시';
COMMENT ON COLUMN ad_creatives.updated_at IS '수정 일시';
COMMENT ON COLUMN ad_creatives.is_deleted IS '삭제 여부';
COMMENT ON COLUMN ad_creatives.deleted_at IS '삭제 일시';
COMMENT ON COLUMN ad_creatives.metadata IS '메타데이터 (JSONB)';

-- ad_impressions 테이블
COMMENT ON TABLE ad_impressions IS '광고 노출 로그 테이블';
COMMENT ON COLUMN ad_impressions.id IS '노출 로그 ID';
COMMENT ON COLUMN ad_impressions.campaign_id IS '캠페인 ID';
COMMENT ON COLUMN ad_impressions.creative_id IS '소재 ID';
COMMENT ON COLUMN ad_impressions.user_id IS '노출 대상 사용자 ID';
COMMENT ON COLUMN ad_impressions.session_id IS '세션 ID';
COMMENT ON COLUMN ad_impressions.placement IS '광고 위치';
COMMENT ON COLUMN ad_impressions.page_url IS '노출 페이지 URL';
COMMENT ON COLUMN ad_impressions.position IS '노출 순위';
COMMENT ON COLUMN ad_impressions.device_type IS '디바이스 유형';
COMMENT ON COLUMN ad_impressions.browser IS '브라우저';
COMMENT ON COLUMN ad_impressions.ip_address IS 'IP 주소';
COMMENT ON COLUMN ad_impressions.cost IS '노출 비용';
COMMENT ON COLUMN ad_impressions.created_at IS '노출 일시';
COMMENT ON COLUMN ad_impressions.updated_at IS '수정 일시';

-- ad_clicks 테이블
COMMENT ON TABLE ad_clicks IS '광고 클릭 로그 테이블';
COMMENT ON COLUMN ad_clicks.id IS '클릭 로그 ID';
COMMENT ON COLUMN ad_clicks.campaign_id IS '캠페인 ID';
COMMENT ON COLUMN ad_clicks.creative_id IS '소재 ID';
COMMENT ON COLUMN ad_clicks.user_id IS '클릭한 사용자 ID';
COMMENT ON COLUMN ad_clicks.session_id IS '세션 ID';
COMMENT ON COLUMN ad_clicks.click_time IS '클릭 시간';
COMMENT ON COLUMN ad_clicks.landing_url IS '랜딩 페이지 URL';
COMMENT ON COLUMN ad_clicks.referrer_url IS '리퍼러 URL';
COMMENT ON COLUMN ad_clicks.ip_address IS 'IP 주소';
COMMENT ON COLUMN ad_clicks.user_agent IS '사용자 에이전트';
COMMENT ON COLUMN ad_clicks.device_type IS '디바이스 유형';
COMMENT ON COLUMN ad_clicks.browser IS '브라우저';
COMMENT ON COLUMN ad_clicks.os IS '운영체제';
COMMENT ON COLUMN ad_clicks.converted IS '전환 여부';
COMMENT ON COLUMN ad_clicks.conversion_time IS '전환 시간';
COMMENT ON COLUMN ad_clicks.conversion_value IS '전환 가치';
COMMENT ON COLUMN ad_clicks.created_at IS '생성 일시';
COMMENT ON COLUMN ad_clicks.updated_at IS '수정 일시';

-- ad_billings 테이블
COMMENT ON TABLE ad_billings IS '광고 청구 및 정산 테이블';
COMMENT ON COLUMN ad_billings.id IS '청구 고유 ID';
COMMENT ON COLUMN ad_billings.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN ad_billings.campaign_id IS '캠페인 ID';
COMMENT ON COLUMN ad_billings.billing_period_start IS '청구 기간 시작일';
COMMENT ON COLUMN ad_billings.billing_period_end IS '청구 기간 종료일';
COMMENT ON COLUMN ad_billings.billing_date IS '청구 일자';
COMMENT ON COLUMN ad_billings.billing_type IS '청구 타입 (CPM, CPC, CPA, FLAT)';
COMMENT ON COLUMN ad_billings.impressions IS '노출 수';
COMMENT ON COLUMN ad_billings.clicks IS '클릭 수';
COMMENT ON COLUMN ad_billings.conversions IS '전환 수';
COMMENT ON COLUMN ad_billings.amount IS '청구 금액';
COMMENT ON COLUMN ad_billings.tax_amount IS '세액';
COMMENT ON COLUMN ad_billings.total_amount IS '총 금액 (청구액 + 세액)';
COMMENT ON COLUMN ad_billings.status IS '결제 상태 (PENDING, PAID, OVERDUE, CANCELLED)';
COMMENT ON COLUMN ad_billings.paid_at IS '결제 완료 일시';
COMMENT ON COLUMN ad_billings.payment_method IS '결제 수단';
COMMENT ON COLUMN ad_billings.invoice_number IS '청구서 번호';
COMMENT ON COLUMN ad_billings.notes IS '비고';
COMMENT ON COLUMN ad_billings.created_at IS '생성 일시';
COMMENT ON COLUMN ad_billings.updated_at IS '수정 일시';
COMMENT ON COLUMN ad_billings.is_deleted IS '삭제 여부';
COMMENT ON COLUMN ad_billings.deleted_at IS '삭제 일시';
COMMENT ON COLUMN ad_billings.metadata IS '메타데이터 (JSONB)';

-- ad_payments 테이블
COMMENT ON TABLE ad_payments IS '광고 결제 이력 테이블 (V2 추가)';
COMMENT ON COLUMN ad_payments.id IS '결제 고유 ID';
COMMENT ON COLUMN ad_payments.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN ad_payments.campaign_id IS '캠페인 ID';
COMMENT ON COLUMN ad_payments.payment_amount IS '결제 금액';
COMMENT ON COLUMN ad_payments.apply_days IS '적용 일수';
COMMENT ON COLUMN ad_payments.daily_rate IS '일일 단가 (payment_amount / apply_days)';
COMMENT ON COLUMN ad_payments.value_30d IS '30일 환산 가치 (daily_rate * 30)';
COMMENT ON COLUMN ad_payments.payment_type IS '결제 유형 (INITIAL:최초, ADDITIONAL:추가, RENEWAL:갱신)';
COMMENT ON COLUMN ad_payments.payment_date IS '결제 일시';
COMMENT ON COLUMN ad_payments.start_date IS '적용 시작일';
COMMENT ON COLUMN ad_payments.end_date IS '적용 종료일';
COMMENT ON COLUMN ad_payments.payment_method IS '결제 수단';
COMMENT ON COLUMN ad_payments.transaction_id IS 'PG사 거래 ID';
COMMENT ON COLUMN ad_payments.status IS '결제 상태 (PENDING, COMPLETED, FAILED, REFUNDED)';
COMMENT ON COLUMN ad_payments.notes IS '비고';
COMMENT ON COLUMN ad_payments.created_at IS '생성 일시';
COMMENT ON COLUMN ad_payments.updated_at IS '수정 일시';
COMMENT ON COLUMN ad_payments.is_deleted IS '삭제 여부';
COMMENT ON COLUMN ad_payments.deleted_at IS '삭제 일시';
COMMENT ON COLUMN ad_payments.metadata IS '메타데이터 (JSONB)';

-- ad_daily_snapshots 테이블
COMMENT ON TABLE ad_daily_snapshots IS '광고 일별 성과 스냅샷 테이블 (V2 추가)';
COMMENT ON COLUMN ad_daily_snapshots.id IS '스냅샷 고유 ID';
COMMENT ON COLUMN ad_daily_snapshots.campaign_id IS '캠페인 ID';
COMMENT ON COLUMN ad_daily_snapshots.snapshot_date IS '스냅샷 날짜';
COMMENT ON COLUMN ad_daily_snapshots.value_30d IS '해당일 30일 환산 가치';
COMMENT ON COLUMN ad_daily_snapshots.daily_rank IS '해당일 광고 순위';
COMMENT ON COLUMN ad_daily_snapshots.impressions IS '일 노출수';
COMMENT ON COLUMN ad_daily_snapshots.clicks IS '일 클릭수';
COMMENT ON COLUMN ad_daily_snapshots.conversions IS '일 전환수';
COMMENT ON COLUMN ad_daily_snapshots.spent_amount IS '일 소진 금액';
COMMENT ON COLUMN ad_daily_snapshots.ctr IS '클릭률 (Click Through Rate)';
COMMENT ON COLUMN ad_daily_snapshots.cpc IS '클릭당 비용 (Cost Per Click)';
COMMENT ON COLUMN ad_daily_snapshots.cvr IS '전환율 (Conversion Rate)';
COMMENT ON COLUMN ad_daily_snapshots.created_at IS '생성 일시';
COMMENT ON COLUMN ad_daily_snapshots.updated_at IS '수정 일시';

-- ==============================================================================
-- 4. 다모아 Pick 시스템 테이블 COMMENT
-- ==============================================================================

-- damoa_picks 테이블
COMMENT ON TABLE damoa_picks IS '다모아 Pick - 시즌별 추천 업체 테이블 (V2 추가)';
COMMENT ON COLUMN damoa_picks.id IS 'Pick 고유 ID';
COMMENT ON COLUMN damoa_picks.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN damoa_picks.company_id IS '추천 업체 ID';
COMMENT ON COLUMN damoa_picks.pick_type IS 'Pick 유형 (SPONSORED:후원, OPERATED:직영, PARTNER:파트너)';
COMMENT ON COLUMN damoa_picks.season IS '시즌 (예: 2024_SPRING, 2024_SUMMER)';
COMMENT ON COLUMN damoa_picks.title IS 'Pick 제목';
COMMENT ON COLUMN damoa_picks.description IS 'Pick 설명';
COMMENT ON COLUMN damoa_picks.main_image_url IS '메인 이미지 URL';
COMMENT ON COLUMN damoa_picks.banner_image_url IS '배너 이미지 URL';
COMMENT ON COLUMN damoa_picks.images IS '추가 이미지 URL 배열';
COMMENT ON COLUMN damoa_picks.badge_text IS '배지 텍스트 (예: 다모아 추천, 공식 파트너)';
COMMENT ON COLUMN damoa_picks.badge_color IS '배지 색상 (HEX 코드)';
COMMENT ON COLUMN damoa_picks.start_date IS '노출 시작일';
COMMENT ON COLUMN damoa_picks.end_date IS '노출 종료일 (NULL이면 무기한)';
COMMENT ON COLUMN damoa_picks.display_order IS '표시 순서';
COMMENT ON COLUMN damoa_picks.is_active IS '활성 상태';
COMMENT ON COLUMN damoa_picks.view_count IS '조회수';
COMMENT ON COLUMN damoa_picks.click_count IS '클릭수';
COMMENT ON COLUMN damoa_picks.created_at IS '생성 일시';
COMMENT ON COLUMN damoa_picks.updated_at IS '수정 일시';
COMMENT ON COLUMN damoa_picks.is_deleted IS '삭제 여부';
COMMENT ON COLUMN damoa_picks.deleted_at IS '삭제 일시';
COMMENT ON COLUMN damoa_picks.metadata IS '메타데이터 (JSONB)';

-- ==============================================================================
-- 5. 견적/매칭 시스템 테이블 COMMENT
-- ==============================================================================

-- estimate_requests 테이블
COMMENT ON TABLE estimate_requests IS '견적 요청 테이블';
COMMENT ON COLUMN estimate_requests.id IS '견적 요청 고유 ID';
COMMENT ON COLUMN estimate_requests.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN estimate_requests.user_id IS '요청자 사용자 ID';
COMMENT ON COLUMN estimate_requests.title IS '견적 요청 제목';
COMMENT ON COLUMN estimate_requests.description IS '견적 요청 설명';
COMMENT ON COLUMN estimate_requests.category IS '카테고리';
COMMENT ON COLUMN estimate_requests.requirements IS '요구사항 상세 (JSON)';
COMMENT ON COLUMN estimate_requests.budget_min IS '예산 최소값';
COMMENT ON COLUMN estimate_requests.budget_max IS '예산 최대값';
COMMENT ON COLUMN estimate_requests.desired_start_date IS '희망 시작일';
COMMENT ON COLUMN estimate_requests.desired_end_date IS '희망 종료일';
COMMENT ON COLUMN estimate_requests.location IS '작업 위치';
COMMENT ON COLUMN estimate_requests.address IS '상세 주소';
COMMENT ON COLUMN estimate_requests.latitude IS '위도';
COMMENT ON COLUMN estimate_requests.longitude IS '경도';
COMMENT ON COLUMN estimate_requests.images IS '첨부 이미지 URL (배열)';
COMMENT ON COLUMN estimate_requests.tags IS '태그 (배열)';
COMMENT ON COLUMN estimate_requests.required_skills IS '필요 기술/자격 (배열)';
COMMENT ON COLUMN estimate_requests.status IS '견적 요청 상태';
COMMENT ON COLUMN estimate_requests.is_public IS '공개 여부';
COMMENT ON COLUMN estimate_requests.view_count IS '조회수';
COMMENT ON COLUMN estimate_requests.proposal_count IS '받은 제안 수';
COMMENT ON COLUMN estimate_requests.expires_at IS '만료일시';
COMMENT ON COLUMN estimate_requests.created_at IS '생성 일시';
COMMENT ON COLUMN estimate_requests.updated_at IS '수정 일시';
COMMENT ON COLUMN estimate_requests.is_deleted IS '삭제 여부';
COMMENT ON COLUMN estimate_requests.deleted_at IS '삭제 일시';
COMMENT ON COLUMN estimate_requests.metadata IS '확장 데이터 (JSON)';

-- estimate_proposals 테이블
COMMENT ON TABLE estimate_proposals IS '견적 제안 테이블';
COMMENT ON COLUMN estimate_proposals.id IS '견적 제안 고유 ID';
COMMENT ON COLUMN estimate_proposals.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN estimate_proposals.request_id IS '견적 요청 ID';
COMMENT ON COLUMN estimate_proposals.company_id IS '제안 업체 ID';
COMMENT ON COLUMN estimate_proposals.title IS '제안 제목';
COMMENT ON COLUMN estimate_proposals.description IS '제안 설명';
COMMENT ON COLUMN estimate_proposals.price IS '제안 가격';
COMMENT ON COLUMN estimate_proposals.pricing_details IS '가격 상세 내역 (JSON)';
COMMENT ON COLUMN estimate_proposals.proposed_start_date IS '제안 시작일';
COMMENT ON COLUMN estimate_proposals.proposed_end_date IS '제안 종료일';
COMMENT ON COLUMN estimate_proposals.timeline IS '작업 일정 (JSON)';
COMMENT ON COLUMN estimate_proposals.attachments IS '첨부파일 URL (배열)';
COMMENT ON COLUMN estimate_proposals.status IS '제안 상태';
COMMENT ON COLUMN estimate_proposals.is_selected IS '선택 여부';
COMMENT ON COLUMN estimate_proposals.selected_at IS '선택 일시';
COMMENT ON COLUMN estimate_proposals.rejection_reason IS '거절 사유';
COMMENT ON COLUMN estimate_proposals.valid_until IS '제안 유효기간';
COMMENT ON COLUMN estimate_proposals.created_at IS '생성 일시';
COMMENT ON COLUMN estimate_proposals.updated_at IS '수정 일시';
COMMENT ON COLUMN estimate_proposals.is_deleted IS '삭제 여부';
COMMENT ON COLUMN estimate_proposals.deleted_at IS '삭제 일시';
COMMENT ON COLUMN estimate_proposals.metadata IS '확장 데이터 (JSON)';

-- estimate_items 테이블
COMMENT ON TABLE estimate_items IS '견적 상세 항목 테이블';
COMMENT ON COLUMN estimate_items.id IS '항목 고유 ID';
COMMENT ON COLUMN estimate_items.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN estimate_items.proposal_id IS '견적 제안 ID';
COMMENT ON COLUMN estimate_items.item_name IS '항목명';
COMMENT ON COLUMN estimate_items.description IS '항목 설명';
COMMENT ON COLUMN estimate_items.category IS '항목 카테고리';
COMMENT ON COLUMN estimate_items.quantity IS '수량';
COMMENT ON COLUMN estimate_items.unit IS '단위';
COMMENT ON COLUMN estimate_items.unit_price IS '단가';
COMMENT ON COLUMN estimate_items.total_price IS '총액';
COMMENT ON COLUMN estimate_items.item_details IS '항목 상세 정보 (JSON)';
COMMENT ON COLUMN estimate_items.display_order IS '표시 순서';
COMMENT ON COLUMN estimate_items.created_at IS '생성 일시';
COMMENT ON COLUMN estimate_items.updated_at IS '수정 일시';

-- estimate_attachments 테이블
COMMENT ON TABLE estimate_attachments IS '견적 첨부파일 테이블';
COMMENT ON COLUMN estimate_attachments.id IS '첨부파일 고유 ID';
COMMENT ON COLUMN estimate_attachments.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN estimate_attachments.request_id IS '견적 요청 ID';
COMMENT ON COLUMN estimate_attachments.proposal_id IS '견적 제안 ID';
COMMENT ON COLUMN estimate_attachments.file_name IS '파일명';
COMMENT ON COLUMN estimate_attachments.file_url IS '파일 URL';
COMMENT ON COLUMN estimate_attachments.file_type IS '파일 유형';
COMMENT ON COLUMN estimate_attachments.file_size IS '파일 크기';
COMMENT ON COLUMN estimate_attachments.description IS '파일 설명';
COMMENT ON COLUMN estimate_attachments.created_at IS '업로드 일시';

-- estimate_messages 테이블
COMMENT ON TABLE estimate_messages IS '견적 관련 메시지 테이블';
COMMENT ON COLUMN estimate_messages.id IS '메시지 고유 ID';
COMMENT ON COLUMN estimate_messages.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN estimate_messages.request_id IS '견적 요청 ID';
COMMENT ON COLUMN estimate_messages.proposal_id IS '견적 제안 ID';
COMMENT ON COLUMN estimate_messages.sender_id IS '발신자 ID';
COMMENT ON COLUMN estimate_messages.message IS '메시지 내용';
COMMENT ON COLUMN estimate_messages.attachments IS '첨부파일 URL (배열)';
COMMENT ON COLUMN estimate_messages.is_read IS '읽음 여부';
COMMENT ON COLUMN estimate_messages.read_at IS '읽은 일시';
COMMENT ON COLUMN estimate_messages.created_at IS '발송 일시';
COMMENT ON COLUMN estimate_messages.is_deleted IS '삭제 여부';
COMMENT ON COLUMN estimate_messages.deleted_at IS '삭제 일시';

-- estimate_templates 테이블
COMMENT ON TABLE estimate_templates IS '견적 템플릿 테이블';
COMMENT ON COLUMN estimate_templates.id IS '템플릿 고유 ID';
COMMENT ON COLUMN estimate_templates.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN estimate_templates.company_id IS '업체 ID';
COMMENT ON COLUMN estimate_templates.name IS '템플릿명';
COMMENT ON COLUMN estimate_templates.category IS '템플릿 카테고리';
COMMENT ON COLUMN estimate_templates.description IS '템플릿 설명';
COMMENT ON COLUMN estimate_templates.template_data IS '템플릿 데이터 (JSON)';
COMMENT ON COLUMN estimate_templates.use_count IS '사용 횟수';
COMMENT ON COLUMN estimate_templates.is_public IS '공개 여부';
COMMENT ON COLUMN estimate_templates.created_at IS '생성 일시';
COMMENT ON COLUMN estimate_templates.updated_at IS '수정 일시';
COMMENT ON COLUMN estimate_templates.is_deleted IS '삭제 여부';
COMMENT ON COLUMN estimate_templates.deleted_at IS '삭제 일시';

-- matches 테이블
COMMENT ON TABLE matches IS '매칭 확정 정보 테이블';
COMMENT ON COLUMN matches.id IS '매칭 고유 ID';
COMMENT ON COLUMN matches.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN matches.request_id IS '견적 요청 ID';
COMMENT ON COLUMN matches.proposal_id IS '선택된 견적 제안 ID';
COMMENT ON COLUMN matches.user_id IS '고객 사용자 ID';
COMMENT ON COLUMN matches.company_id IS '업체 ID';
COMMENT ON COLUMN matches.contract_amount IS '계약 금액';
COMMENT ON COLUMN matches.contract_terms IS '계약 조건 (JSON)';
COMMENT ON COLUMN matches.start_date IS '계약 시작일';
COMMENT ON COLUMN matches.end_date IS '계약 종료일';
COMMENT ON COLUMN matches.actual_start_date IS '실제 시작일';
COMMENT ON COLUMN matches.actual_end_date IS '실제 종료일';
COMMENT ON COLUMN matches.status IS '매칭 상태 (CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED)';
COMMENT ON COLUMN matches.completed_at IS '완료 일시';
COMMENT ON COLUMN matches.completion_notes IS '완료 메모';
COMMENT ON COLUMN matches.cancelled_at IS '취소 일시';
COMMENT ON COLUMN matches.cancellation_reason IS '취소 사유';
COMMENT ON COLUMN matches.created_at IS '매칭 성사 일시';
COMMENT ON COLUMN matches.updated_at IS '정보 수정 일시';
COMMENT ON COLUMN matches.is_deleted IS '삭제 여부';
COMMENT ON COLUMN matches.deleted_at IS '삭제 일시';
COMMENT ON COLUMN matches.metadata IS '확장 데이터 (JSON)';

-- match_reviews 테이블
COMMENT ON TABLE match_reviews IS '매칭 후기 테이블';
COMMENT ON COLUMN match_reviews.id IS '후기 고유 ID';
COMMENT ON COLUMN match_reviews.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN match_reviews.match_id IS '매칭 ID';
COMMENT ON COLUMN match_reviews.reviewer_id IS '리뷰 작성자 ID';
COMMENT ON COLUMN match_reviews.reviewee_id IS '리뷰 대상자 ID';
COMMENT ON COLUMN match_reviews.review_type IS '리뷰 타입 (USER_TO_COMPANY, COMPANY_TO_USER)';
COMMENT ON COLUMN match_reviews.rating IS '평점 (1-5)';
COMMENT ON COLUMN match_reviews.content IS '후기 내용';
COMMENT ON COLUMN match_reviews.is_public IS '공개 여부';
COMMENT ON COLUMN match_reviews.created_at IS '작성 일시';
COMMENT ON COLUMN match_reviews.updated_at IS '수정 일시';
COMMENT ON COLUMN match_reviews.is_deleted IS '삭제 여부';
COMMENT ON COLUMN match_reviews.deleted_at IS '삭제 일시';

-- ==============================================================================
-- 6. 알림 시스템 테이블 COMMENT
-- ==============================================================================

-- notifications 테이블
COMMENT ON TABLE notifications IS '통합 알림 관리 테이블';
COMMENT ON COLUMN notifications.id IS '알림 고유 ID';
COMMENT ON COLUMN notifications.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN notifications.user_id IS '수신자 ID';
COMMENT ON COLUMN notifications.notification_type IS '알림 유형';
COMMENT ON COLUMN notifications.channel IS '알림 채널 (EMAIL, SMS, PUSH, KAKAO, IN_APP)';
COMMENT ON COLUMN notifications.title IS '알림 제목';
COMMENT ON COLUMN notifications.content IS '알림 내용';
COMMENT ON COLUMN notifications.link_url IS '연결 URL';
COMMENT ON COLUMN notifications.action_type IS '액션 타입';
COMMENT ON COLUMN notifications.action_data IS '액션 데이터 (JSON)';
COMMENT ON COLUMN notifications.is_sent IS '발송 여부';
COMMENT ON COLUMN notifications.sent_at IS '발송 일시';
COMMENT ON COLUMN notifications.send_error IS '발송 오류 메시지';
COMMENT ON COLUMN notifications.is_read IS '읽음 여부';
COMMENT ON COLUMN notifications.read_at IS '읽은 일시';
COMMENT ON COLUMN notifications.scheduled_at IS '예약 발송 일시';
COMMENT ON COLUMN notifications.template_id IS '템플릿 ID';
COMMENT ON COLUMN notifications.template_data IS '템플릿 변수 데이터 (JSON)';
COMMENT ON COLUMN notifications.created_at IS '생성 일시';
COMMENT ON COLUMN notifications.updated_at IS '수정 일시';

-- notification_templates 테이블
COMMENT ON TABLE notification_templates IS '알림 템플릿 테이블';
COMMENT ON COLUMN notification_templates.id IS '템플릿 고유 ID';
COMMENT ON COLUMN notification_templates.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN notification_templates.name IS '템플릿명';
COMMENT ON COLUMN notification_templates.code IS '템플릿 코드 (고유값)';
COMMENT ON COLUMN notification_templates.channel IS '알림 채널';
COMMENT ON COLUMN notification_templates.title_template IS '제목 템플릿';
COMMENT ON COLUMN notification_templates.content_template IS '내용 템플릿';
COMMENT ON COLUMN notification_templates.variables IS '템플릿 변수 정의 (JSON)';
COMMENT ON COLUMN notification_templates.kakao_template_code IS '카카오 알림톡 템플릿 코드';
COMMENT ON COLUMN notification_templates.is_active IS '활성 상태';
COMMENT ON COLUMN notification_templates.created_at IS '생성 일시';
COMMENT ON COLUMN notification_templates.updated_at IS '수정 일시';

-- notification_settings 테이블
COMMENT ON TABLE notification_settings IS '사용자별 알림 수신 설정 테이블';
COMMENT ON COLUMN notification_settings.id IS '설정 고유 ID';
COMMENT ON COLUMN notification_settings.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN notification_settings.user_id IS '사용자 ID';
COMMENT ON COLUMN notification_settings.email_enabled IS '이메일 수신 동의';
COMMENT ON COLUMN notification_settings.sms_enabled IS 'SMS 수신 동의';
COMMENT ON COLUMN notification_settings.push_enabled IS '푸시 알림 수신 동의';
COMMENT ON COLUMN notification_settings.kakao_enabled IS '카카오톡 수신 동의';
COMMENT ON COLUMN notification_settings.preferences IS '알림 타입별 상세 설정 (JSON)';
COMMENT ON COLUMN notification_settings.do_not_disturb_start IS '방해금지 시작 시간';
COMMENT ON COLUMN notification_settings.do_not_disturb_end IS '방해금지 종료 시간';
COMMENT ON COLUMN notification_settings.created_at IS '생성 일시';
COMMENT ON COLUMN notification_settings.updated_at IS '수정 일시';

-- notification_logs 테이블
COMMENT ON TABLE notification_logs IS '알림 발송 로그 테이블';
COMMENT ON COLUMN notification_logs.id IS '로그 고유 ID';
COMMENT ON COLUMN notification_logs.notification_id IS '알림 ID';
COMMENT ON COLUMN notification_logs.channel IS '발송 채널';
COMMENT ON COLUMN notification_logs.recipient IS '수신자 (이메일, 전화번호 등)';
COMMENT ON COLUMN notification_logs.status IS '발송 상태 (PENDING, SENT, FAILED, BOUNCED)';
COMMENT ON COLUMN notification_logs.provider IS '외부 발송 서비스 (AWS SES, Twilio, FCM 등)';
COMMENT ON COLUMN notification_logs.provider_message_id IS '외부 서비스 메시지 ID';
COMMENT ON COLUMN notification_logs.response_data IS '외부 서비스 응답 데이터 (JSON)';
COMMENT ON COLUMN notification_logs.error_code IS '에러 코드';
COMMENT ON COLUMN notification_logs.error_message IS '에러 메시지';
COMMENT ON COLUMN notification_logs.cost IS '발송 비용';
COMMENT ON COLUMN notification_logs.sent_at IS '발송 시도 일시';
COMMENT ON COLUMN notification_logs.delivered_at IS '도착 확인 일시';
COMMENT ON COLUMN notification_logs.failed_at IS '실패 일시';
COMMENT ON COLUMN notification_logs.created_at IS '로그 생성 일시';

-- sms_verifications 테이블
COMMENT ON TABLE sms_verifications IS 'SMS 인증 코드 관리 테이블';
COMMENT ON COLUMN sms_verifications.id IS '인증 고유 ID';
COMMENT ON COLUMN sms_verifications.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN sms_verifications.phone IS '휴대폰 번호';
COMMENT ON COLUMN sms_verifications.user_id IS '사용자 ID (선택)';
COMMENT ON COLUMN sms_verifications.verification_code IS '인증 코드';
COMMENT ON COLUMN sms_verifications.purpose IS '인증 목적 (SIGNUP, PASSWORD_RESET, PHONE_CHANGE)';
COMMENT ON COLUMN sms_verifications.attempt_count IS '시도 횟수';
COMMENT ON COLUMN sms_verifications.max_attempts IS '최대 시도 횟수';
COMMENT ON COLUMN sms_verifications.is_verified IS '인증 완료 여부';
COMMENT ON COLUMN sms_verifications.verified_at IS '인증 완료 일시';
COMMENT ON COLUMN sms_verifications.expires_at IS '만료 일시';
COMMENT ON COLUMN sms_verifications.request_ip IS '요청 IP 주소';
COMMENT ON COLUMN sms_verifications.created_at IS '생성 일시';

-- email_verifications 테이블
COMMENT ON TABLE email_verifications IS '이메일 인증 토큰 관리 테이블';
COMMENT ON COLUMN email_verifications.id IS '인증 고유 ID';
COMMENT ON COLUMN email_verifications.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN email_verifications.email IS '이메일 주소';
COMMENT ON COLUMN email_verifications.user_id IS '사용자 ID (선택)';
COMMENT ON COLUMN email_verifications.verification_token IS '인증 토큰 (고유값)';
COMMENT ON COLUMN email_verifications.purpose IS '인증 목적 (SIGNUP, PASSWORD_RESET, EMAIL_CHANGE)';
COMMENT ON COLUMN email_verifications.is_verified IS '인증 완료 여부';
COMMENT ON COLUMN email_verifications.verified_at IS '인증 완료 일시';
COMMENT ON COLUMN email_verifications.expires_at IS '만료 일시';
COMMENT ON COLUMN email_verifications.request_ip IS '요청 IP 주소';
COMMENT ON COLUMN email_verifications.verified_ip IS '인증 완료 IP 주소';
COMMENT ON COLUMN email_verifications.created_at IS '생성 일시';

-- ==============================================================================
-- 7. 파일 관리 테이블 COMMENT
-- ==============================================================================

-- files 테이블
COMMENT ON TABLE files IS '파일 메타데이터 관리 테이블';
COMMENT ON COLUMN files.id IS '파일 고유 ID';
COMMENT ON COLUMN files.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN files.user_id IS '업로드한 사용자 ID';
COMMENT ON COLUMN files.file_name IS '저장된 파일명';
COMMENT ON COLUMN files.original_name IS '원본 파일명';
COMMENT ON COLUMN files.file_type IS '파일 유형 (IMAGE, VIDEO, DOCUMENT, AUDIO, OTHER)';
COMMENT ON COLUMN files.mime_type IS 'MIME 타입';
COMMENT ON COLUMN files.file_size IS '파일 크기 (바이트)';
COMMENT ON COLUMN files.storage_type IS '저장 유형 (S3, LOCAL)';
COMMENT ON COLUMN files.s3_bucket IS 'S3 버킷명';
COMMENT ON COLUMN files.s3_key IS 'S3 키';
COMMENT ON COLUMN files.file_url IS '파일 접근 URL';
COMMENT ON COLUMN files.thumbnail_url IS '썸네일 URL';
COMMENT ON COLUMN files.width IS '이미지/비디오 너비';
COMMENT ON COLUMN files.height IS '이미지/비디오 높이';
COMMENT ON COLUMN files.duration IS '비디오/오디오 길이 (초)';
COMMENT ON COLUMN files.metadata IS '파일 메타데이터 (JSON)';
COMMENT ON COLUMN files.is_public IS '공개 여부';
COMMENT ON COLUMN files.access_control IS '접근 제어 설정 (JSON)';
COMMENT ON COLUMN files.download_count IS '다운로드 횟수';
COMMENT ON COLUMN files.view_count IS '조회 횟수';
COMMENT ON COLUMN files.created_at IS '업로드 일시';
COMMENT ON COLUMN files.updated_at IS '수정 일시';
COMMENT ON COLUMN files.is_deleted IS '삭제 여부';
COMMENT ON COLUMN files.deleted_at IS '삭제 일시';

-- file_uploads 테이블
COMMENT ON TABLE file_uploads IS 'Presigned URL 관리 테이블';
COMMENT ON COLUMN file_uploads.id IS '업로드 고유 ID';
COMMENT ON COLUMN file_uploads.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN file_uploads.user_id IS '사용자 ID';
COMMENT ON COLUMN file_uploads.upload_key IS '업로드 키';
COMMENT ON COLUMN file_uploads.presigned_url IS 'Presigned URL';
COMMENT ON COLUMN file_uploads.file_name IS '파일명';
COMMENT ON COLUMN file_uploads.file_type IS '파일 유형';
COMMENT ON COLUMN file_uploads.file_size_limit IS '파일 크기 제한';
COMMENT ON COLUMN file_uploads.status IS '업로드 상태 (PENDING, UPLOADED, EXPIRED, FAILED)';
COMMENT ON COLUMN file_uploads.file_id IS '완료된 파일 ID';
COMMENT ON COLUMN file_uploads.completed_at IS '업로드 완료 일시';
COMMENT ON COLUMN file_uploads.expires_at IS 'URL 만료 일시';
COMMENT ON COLUMN file_uploads.created_at IS '생성 일시';

-- file_attachments 테이블
COMMENT ON TABLE file_attachments IS '파일-엔티티 연결 관리 테이블';
COMMENT ON COLUMN file_attachments.id IS '연결 고유 ID';
COMMENT ON COLUMN file_attachments.file_id IS '파일 ID';
COMMENT ON COLUMN file_attachments.entity_type IS '연결 엔티티 타입';
COMMENT ON COLUMN file_attachments.entity_id IS '연결 엔티티 ID';
COMMENT ON COLUMN file_attachments.attachment_type IS '첨부 유형 (MAIN, THUMBNAIL, ATTACHMENT)';
COMMENT ON COLUMN file_attachments.display_order IS '표시 순서';
COMMENT ON COLUMN file_attachments.caption IS '캡션/설명';
COMMENT ON COLUMN file_attachments.created_at IS '연결 일시';

-- file_downloads 테이블
COMMENT ON TABLE file_downloads IS '파일 다운로드 로그 테이블';
COMMENT ON COLUMN file_downloads.id IS '다운로드 로그 ID';
COMMENT ON COLUMN file_downloads.file_id IS '파일 ID';
COMMENT ON COLUMN file_downloads.user_id IS '다운로드한 사용자 ID';
COMMENT ON COLUMN file_downloads.ip_address IS 'IP 주소';
COMMENT ON COLUMN file_downloads.user_agent IS '브라우저/클라이언트 정보';
COMMENT ON COLUMN file_downloads.referer IS '참조 URL';
COMMENT ON COLUMN file_downloads.created_at IS '다운로드 일시';

-- ==============================================================================
-- 8. 결제 시스템 테이블 COMMENT
-- ==============================================================================

-- payments 테이블
COMMENT ON TABLE payments IS '결제 정보 테이블';
COMMENT ON COLUMN payments.id IS '결제 고유 ID';
COMMENT ON COLUMN payments.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN payments.user_id IS '결제자 ID';
COMMENT ON COLUMN payments.payment_type IS '결제 유형 (ORDER, SUBSCRIPTION, AD, CREDIT)';
COMMENT ON COLUMN payments.payment_method IS '결제 수단';
COMMENT ON COLUMN payments.amount IS '결제 금액';
COMMENT ON COLUMN payments.tax_amount IS '세액';
COMMENT ON COLUMN payments.discount_amount IS '할인 금액';
COMMENT ON COLUMN payments.final_amount IS '최종 결제 금액';
COMMENT ON COLUMN payments.status IS '결제 상태';
COMMENT ON COLUMN payments.pg_provider IS 'PG사 (TOSS, NICEPAY, KCP)';
COMMENT ON COLUMN payments.pg_tid IS 'PG사 거래 ID';
COMMENT ON COLUMN payments.pg_response IS 'PG사 응답 데이터 (JSON)';
COMMENT ON COLUMN payments.payment_data IS '결제 관련 데이터 (JSON)';
COMMENT ON COLUMN payments.paid_at IS '결제 완료 일시';
COMMENT ON COLUMN payments.receipt_url IS '영수증 URL';
COMMENT ON COLUMN payments.is_refundable IS '환불 가능 여부';
COMMENT ON COLUMN payments.refunded_amount IS '환불된 금액';
COMMENT ON COLUMN payments.created_at IS '생성 일시';
COMMENT ON COLUMN payments.updated_at IS '수정 일시';
COMMENT ON COLUMN payments.metadata IS '확장 데이터 (JSON)';

-- payment_methods 테이블
COMMENT ON TABLE payment_methods IS '저장된 결제 수단 테이블';
COMMENT ON COLUMN payment_methods.id IS '결제 수단 고유 ID';
COMMENT ON COLUMN payment_methods.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN payment_methods.user_id IS '사용자 ID';
COMMENT ON COLUMN payment_methods.method_type IS '결제 수단 유형';
COMMENT ON COLUMN payment_methods.is_default IS '기본 결제 수단 여부';
COMMENT ON COLUMN payment_methods.card_info IS '카드 정보 (암호화된 JSON)';
COMMENT ON COLUMN payment_methods.bank_info IS '계좌 정보 (JSON)';
COMMENT ON COLUMN payment_methods.is_active IS '활성 상태';
COMMENT ON COLUMN payment_methods.verified_at IS '인증 완료 일시';
COMMENT ON COLUMN payment_methods.created_at IS '등록 일시';
COMMENT ON COLUMN payment_methods.updated_at IS '수정 일시';

-- refunds 테이블
COMMENT ON TABLE refunds IS '환불 관리 테이블';
COMMENT ON COLUMN refunds.id IS '환불 고유 ID';
COMMENT ON COLUMN refunds.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN refunds.payment_id IS '원 결제 ID';
COMMENT ON COLUMN refunds.user_id IS '환불 요청자 ID';
COMMENT ON COLUMN refunds.refund_amount IS '환불 금액';
COMMENT ON COLUMN refunds.refund_reason IS '환불 사유';
COMMENT ON COLUMN refunds.refund_type IS '환불 유형 (FULL:전액, PARTIAL:부분)';
COMMENT ON COLUMN refunds.status IS '환불 상태 (REQUESTED, APPROVED, COMPLETED, REJECTED)';
COMMENT ON COLUMN refunds.approved_at IS '승인 일시';
COMMENT ON COLUMN refunds.approved_by IS '승인자 ID';
COMMENT ON COLUMN refunds.completed_at IS '환불 완료 일시';
COMMENT ON COLUMN refunds.rejection_reason IS '거절 사유';
COMMENT ON COLUMN refunds.pg_tid IS 'PG사 환불 거래 ID';
COMMENT ON COLUMN refunds.pg_response IS 'PG사 환불 응답 (JSON)';
COMMENT ON COLUMN refunds.refund_data IS '환불 관련 데이터 (JSON)';
COMMENT ON COLUMN refunds.created_at IS '요청 일시';
COMMENT ON COLUMN refunds.updated_at IS '수정 일시';

-- invoices 테이블
COMMENT ON TABLE invoices IS '세금계산서/계산서 테이블';
COMMENT ON COLUMN invoices.id IS '계산서 고유 ID';
COMMENT ON COLUMN invoices.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN invoices.payment_id IS '결제 ID';
COMMENT ON COLUMN invoices.user_id IS '사용자 ID';
COMMENT ON COLUMN invoices.company_id IS '업체 ID';
COMMENT ON COLUMN invoices.invoice_number IS '계산서 번호 (고유값)';
COMMENT ON COLUMN invoices.invoice_type IS '계산서 유형 (TAX:세금계산서, CASH_RECEIPT:현금영수증)';
COMMENT ON COLUMN invoices.supply_amount IS '공급가액';
COMMENT ON COLUMN invoices.tax_amount IS '세액';
COMMENT ON COLUMN invoices.total_amount IS '합계 금액';
COMMENT ON COLUMN invoices.issue_date IS '발행일';
COMMENT ON COLUMN invoices.due_date IS '만기일';
COMMENT ON COLUMN invoices.supplier_info IS '공급자 정보 (JSON)';
COMMENT ON COLUMN invoices.buyer_info IS '공급받는자 정보 (JSON)';
COMMENT ON COLUMN invoices.items IS '품목 리스트 (JSON 배열)';
COMMENT ON COLUMN invoices.status IS '계산서 상태 (ISSUED, SENT, CANCELLED)';
COMMENT ON COLUMN invoices.pdf_url IS 'PDF 파일 URL';
COMMENT ON COLUMN invoices.created_at IS '생성 일시';
COMMENT ON COLUMN invoices.updated_at IS '수정 일시';

-- credits 테이블
COMMENT ON TABLE credits IS '크레딧/포인트 잔액 테이블';
COMMENT ON COLUMN credits.id IS '크레딧 정보 고유 ID';
COMMENT ON COLUMN credits.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN credits.user_id IS '사용자 ID';
COMMENT ON COLUMN credits.balance IS '현재 잔액';
COMMENT ON COLUMN credits.total_earned IS '총 적립액';
COMMENT ON COLUMN credits.total_used IS '총 사용액';
COMMENT ON COLUMN credits.pending_amount IS '보류 중인 크레딧';
COMMENT ON COLUMN credits.created_at IS '생성 일시';
COMMENT ON COLUMN credits.updated_at IS '수정 일시';

-- credit_transactions 테이블
COMMENT ON TABLE credit_transactions IS '크레딧 거래 내역 테이블';
COMMENT ON COLUMN credit_transactions.id IS '거래 고유 ID';
COMMENT ON COLUMN credit_transactions.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN credit_transactions.user_id IS '사용자 ID';
COMMENT ON COLUMN credit_transactions.credit_id IS '크레딧 정보 ID';
COMMENT ON COLUMN credit_transactions.transaction_type IS '거래 유형 (EARN:적립, USE:사용, REFUND:환불, EXPIRE:만료)';
COMMENT ON COLUMN credit_transactions.amount IS '거래 금액';
COMMENT ON COLUMN credit_transactions.balance_after IS '거래 후 잔액';
COMMENT ON COLUMN credit_transactions.reason IS '거래 사유';
COMMENT ON COLUMN credit_transactions.reference_type IS '참조 엔티티 타입';
COMMENT ON COLUMN credit_transactions.reference_id IS '참조 엔티티 ID';
COMMENT ON COLUMN credit_transactions.expires_at IS '만료일시';
COMMENT ON COLUMN credit_transactions.created_at IS '거래 일시';

-- coupons 테이블
COMMENT ON TABLE coupons IS '쿠폰 마스터 테이블';
COMMENT ON COLUMN coupons.id IS '쿠폰 고유 ID';
COMMENT ON COLUMN coupons.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN coupons.code IS '쿠폰 코드 (고유값)';
COMMENT ON COLUMN coupons.name IS '쿠폰명';
COMMENT ON COLUMN coupons.description IS '쿠폰 설명';
COMMENT ON COLUMN coupons.discount_type IS '할인 유형 (FIXED:정액, PERCENTAGE:정률)';
COMMENT ON COLUMN coupons.discount_value IS '할인 값';
COMMENT ON COLUMN coupons.max_discount_amount IS '최대 할인 금액';
COMMENT ON COLUMN coupons.min_purchase_amount IS '최소 구매 금액';
COMMENT ON COLUMN coupons.conditions IS '사용 조건 (JSON)';
COMMENT ON COLUMN coupons.total_quantity IS '발행 수량';
COMMENT ON COLUMN coupons.used_quantity IS '사용된 수량';
COMMENT ON COLUMN coupons.valid_from IS '유효기간 시작';
COMMENT ON COLUMN coupons.valid_to IS '유효기간 종료';
COMMENT ON COLUMN coupons.is_active IS '활성 상태';
COMMENT ON COLUMN coupons.created_at IS '생성 일시';
COMMENT ON COLUMN coupons.updated_at IS '수정 일시';

-- user_coupons 테이블
COMMENT ON TABLE user_coupons IS '사용자별 쿠폰 보유 테이블';
COMMENT ON COLUMN user_coupons.id IS '보유 쿠폰 고유 ID';
COMMENT ON COLUMN user_coupons.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN user_coupons.user_id IS '사용자 ID';
COMMENT ON COLUMN user_coupons.coupon_id IS '쿠폰 ID';
COMMENT ON COLUMN user_coupons.is_used IS '사용 여부';
COMMENT ON COLUMN user_coupons.used_at IS '사용 일시';
COMMENT ON COLUMN user_coupons.payment_id IS '사용한 결제 ID';
COMMENT ON COLUMN user_coupons.expires_at IS '만료일시';
COMMENT ON COLUMN user_coupons.created_at IS '발급 일시';

-- ==============================================================================
-- 9. 게시판 시스템 테이블 COMMENT
-- ==============================================================================

-- boards 테이블
COMMENT ON TABLE boards IS '통합 게시판 테이블';
COMMENT ON COLUMN boards.id IS '게시글 고유 ID';
COMMENT ON COLUMN boards.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN boards.user_id IS '작성자 ID';
COMMENT ON COLUMN boards.board_type IS '게시판 유형 (NOTICE, EVENT, FAQ, GALLERY, DOCUMENT)';
COMMENT ON COLUMN boards.category_id IS '카테고리 ID';
COMMENT ON COLUMN boards.title IS '제목';
COMMENT ON COLUMN boards.content IS '내용';
COMMENT ON COLUMN boards.type_data IS '게시판 타입별 특수 데이터 (JSON)';
COMMENT ON COLUMN boards.view_count IS '조회수';
COMMENT ON COLUMN boards.like_count IS '좋아요 수';
COMMENT ON COLUMN boards.comment_count IS '댓글 수';
COMMENT ON COLUMN boards.is_pinned IS '상단 고정 여부';
COMMENT ON COLUMN boards.is_featured IS '추천글 여부';
COMMENT ON COLUMN boards.is_published IS '게시 여부';
COMMENT ON COLUMN boards.published_at IS '게시 일시';
COMMENT ON COLUMN boards.tags IS '태그 (배열)';
COMMENT ON COLUMN boards.is_private IS '비공개 여부';
COMMENT ON COLUMN boards.password IS '비밀글 비밀번호';
COMMENT ON COLUMN boards.created_at IS '작성 일시';
COMMENT ON COLUMN boards.updated_at IS '수정 일시';
COMMENT ON COLUMN boards.created_by IS '작성자 ID';
COMMENT ON COLUMN boards.updated_by IS '수정자 ID';
COMMENT ON COLUMN boards.is_deleted IS '삭제 여부';
COMMENT ON COLUMN boards.deleted_at IS '삭제 일시';
COMMENT ON COLUMN boards.metadata IS '확장 데이터 (JSON)';

-- board_comments 테이블
COMMENT ON TABLE board_comments IS '게시판 댓글 테이블';
COMMENT ON COLUMN board_comments.id IS '댓글 고유 ID';
COMMENT ON COLUMN board_comments.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN board_comments.board_id IS '게시글 ID';
COMMENT ON COLUMN board_comments.user_id IS '작성자 ID';
COMMENT ON COLUMN board_comments.parent_id IS '부모 댓글 ID (대댓글)';
COMMENT ON COLUMN board_comments.content IS '댓글 내용';
COMMENT ON COLUMN board_comments.is_secret IS '비밀 댓글 여부';
COMMENT ON COLUMN board_comments.is_reported IS '신고된 댓글 여부';
COMMENT ON COLUMN board_comments.created_at IS '작성 일시';
COMMENT ON COLUMN board_comments.updated_at IS '수정 일시';
COMMENT ON COLUMN board_comments.is_deleted IS '삭제 여부';
COMMENT ON COLUMN board_comments.deleted_at IS '삭제 일시';

-- board_likes 테이블
COMMENT ON TABLE board_likes IS '게시판 좋아요 테이블';
COMMENT ON COLUMN board_likes.id IS '좋아요 고유 ID';
COMMENT ON COLUMN board_likes.board_id IS '게시글 ID';
COMMENT ON COLUMN board_likes.user_id IS '사용자 ID';
COMMENT ON COLUMN board_likes.created_at IS '좋아요 일시';

-- board_categories 테이블
COMMENT ON TABLE board_categories IS '게시판 카테고리 테이블';
COMMENT ON COLUMN board_categories.id IS '카테고리 고유 ID';
COMMENT ON COLUMN board_categories.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN board_categories.board_type IS '게시판 유형';
COMMENT ON COLUMN board_categories.name IS '카테고리명';
COMMENT ON COLUMN board_categories.slug IS 'URL 슬러그';
COMMENT ON COLUMN board_categories.description IS '카테고리 설명';
COMMENT ON COLUMN board_categories.parent_id IS '부모 카테고리 ID';
COMMENT ON COLUMN board_categories.depth IS '카테고리 깊이';
COMMENT ON COLUMN board_categories.path IS '카테고리 경로';
COMMENT ON COLUMN board_categories.display_order IS '표시 순서';
COMMENT ON COLUMN board_categories.is_active IS '활성 상태';
COMMENT ON COLUMN board_categories.created_at IS '생성 일시';
COMMENT ON COLUMN board_categories.updated_at IS '수정 일시';

-- board_attachments 테이블
COMMENT ON TABLE board_attachments IS '게시판 첨부파일 테이블';
COMMENT ON COLUMN board_attachments.id IS '첨부파일 고유 ID';
COMMENT ON COLUMN board_attachments.board_id IS '게시글 ID';
COMMENT ON COLUMN board_attachments.file_id IS '파일 ID';
COMMENT ON COLUMN board_attachments.attachment_type IS '첨부 유형 (FILE, IMAGE, VIDEO)';
COMMENT ON COLUMN board_attachments.display_order IS '표시 순서';
COMMENT ON COLUMN board_attachments.created_at IS '첨부 일시';

-- ==============================================================================
-- 10. 필터/검색 시스템 테이블 COMMENT
-- ==============================================================================

-- filter_templates 테이블
COMMENT ON TABLE filter_templates IS '필터 템플릿 테이블';
COMMENT ON COLUMN filter_templates.id IS '템플릿 고유 ID';
COMMENT ON COLUMN filter_templates.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN filter_templates.name IS '템플릿명';
COMMENT ON COLUMN filter_templates.code IS '템플릿 코드 (고유값)';
COMMENT ON COLUMN filter_templates.entity_type IS '대상 엔티티 타입 (COMPANY, ESTIMATE, PRODUCT)';
COMMENT ON COLUMN filter_templates.filter_config IS '필터 설정 (JSON)';
COMMENT ON COLUMN filter_templates.sort_options IS '정렬 옵션 (JSON)';
COMMENT ON COLUMN filter_templates.is_active IS '활성 상태';
COMMENT ON COLUMN filter_templates.is_default IS '기본 템플릿 여부';
COMMENT ON COLUMN filter_templates.created_at IS '생성 일시';
COMMENT ON COLUMN filter_templates.updated_at IS '수정 일시';

-- saved_searches 테이블
COMMENT ON TABLE saved_searches IS '저장된 검색 조건 테이블';
COMMENT ON COLUMN saved_searches.id IS '검색 조건 고유 ID';
COMMENT ON COLUMN saved_searches.uuid IS '외부 API용 고유 식별자';
COMMENT ON COLUMN saved_searches.user_id IS '사용자 ID';
COMMENT ON COLUMN saved_searches.name IS '검색 조건명';
COMMENT ON COLUMN saved_searches.entity_type IS '검색 대상 엔티티 타입';
COMMENT ON COLUMN saved_searches.search_query IS '검색어';
COMMENT ON COLUMN saved_searches.filters IS '필터 조건 (JSON)';
COMMENT ON COLUMN saved_searches.sort_by IS '정렬 필드';
COMMENT ON COLUMN saved_searches.sort_order IS '정렬 방향 (ASC, DESC)';
COMMENT ON COLUMN saved_searches.alert_enabled IS '알림 설정 여부';
COMMENT ON COLUMN saved_searches.alert_frequency IS '알림 주기 (INSTANT, DAILY, WEEKLY)';
COMMENT ON COLUMN saved_searches.last_alert_at IS '마지막 알림 발송 일시';
COMMENT ON COLUMN saved_searches.created_at IS '생성 일시';
COMMENT ON COLUMN saved_searches.updated_at IS '수정 일시';

-- ==============================================================================
-- 11. 통계/로그 테이블 COMMENT
-- ==============================================================================

-- statistics_daily 테이블
COMMENT ON TABLE statistics_daily IS '일별 통계 테이블';
COMMENT ON COLUMN statistics_daily.id IS '통계 고유 ID';
COMMENT ON COLUMN statistics_daily.stat_date IS '통계 날짜';
COMMENT ON COLUMN statistics_daily.new_users IS '신규 가입자 수';
COMMENT ON COLUMN statistics_daily.active_users IS '활성 사용자 수';
COMMENT ON COLUMN statistics_daily.total_users IS '전체 사용자 수';
COMMENT ON COLUMN statistics_daily.new_companies IS '신규 업체 수';
COMMENT ON COLUMN statistics_daily.active_companies IS '활성 업체 수';
COMMENT ON COLUMN statistics_daily.new_requests IS '신규 견적 요청 수';
COMMENT ON COLUMN statistics_daily.new_proposals IS '신규 견적 제안 수';
COMMENT ON COLUMN statistics_daily.new_matches IS '신규 매칭 수';
COMMENT ON COLUMN statistics_daily.payment_count IS '결제 건수';
COMMENT ON COLUMN statistics_daily.payment_amount IS '결제 금액';
COMMENT ON COLUMN statistics_daily.ad_impressions IS '광고 노출 수';
COMMENT ON COLUMN statistics_daily.ad_clicks IS '광고 클릭 수';
COMMENT ON COLUMN statistics_daily.ad_revenue IS '광고 수익';
COMMENT ON COLUMN statistics_daily.detailed_stats IS '상세 통계 데이터 (JSON)';
COMMENT ON COLUMN statistics_daily.created_at IS '생성 일시';

-- analytics_events 테이블
COMMENT ON TABLE analytics_events IS '이벤트 추적 로그 테이블';
COMMENT ON COLUMN analytics_events.id IS '이벤트 로그 ID';
COMMENT ON COLUMN analytics_events.user_id IS '사용자 ID';
COMMENT ON COLUMN analytics_events.session_id IS '세션 ID';
COMMENT ON COLUMN analytics_events.event_name IS '이벤트명';
COMMENT ON COLUMN analytics_events.event_category IS '이벤트 카테고리';
COMMENT ON COLUMN analytics_events.event_action IS '이벤트 액션';
COMMENT ON COLUMN analytics_events.event_label IS '이벤트 라벨';
COMMENT ON COLUMN analytics_events.event_value IS '이벤트 값';
COMMENT ON COLUMN analytics_events.page_url IS '페이지 URL';
COMMENT ON COLUMN analytics_events.page_title IS '페이지 제목';
COMMENT ON COLUMN analytics_events.referrer IS '참조 URL';
COMMENT ON COLUMN analytics_events.device_type IS '디바이스 유형';
COMMENT ON COLUMN analytics_events.browser IS '브라우저';
COMMENT ON COLUMN analytics_events.os IS '운영체제';
COMMENT ON COLUMN analytics_events.ip_address IS 'IP 주소';
COMMENT ON COLUMN analytics_events.country IS '국가 코드';
COMMENT ON COLUMN analytics_events.region IS '지역';
COMMENT ON COLUMN analytics_events.properties IS '이벤트 속성 (JSON)';
COMMENT ON COLUMN analytics_events.created_at IS '이벤트 발생 일시';

-- audit_logs 테이블
COMMENT ON TABLE audit_logs IS '감사 로그 테이블';
COMMENT ON COLUMN audit_logs.id IS '로그 고유 ID';
COMMENT ON COLUMN audit_logs.user_id IS '수행자 ID';
COMMENT ON COLUMN audit_logs.action IS '수행 액션';
COMMENT ON COLUMN audit_logs.entity_type IS '대상 엔티티 타입';
COMMENT ON COLUMN audit_logs.entity_id IS '대상 엔티티 ID';
COMMENT ON COLUMN audit_logs.old_values IS '변경 전 값 (JSON)';
COMMENT ON COLUMN audit_logs.new_values IS '변경 후 값 (JSON)';
COMMENT ON COLUMN audit_logs.ip_address IS 'IP 주소';
COMMENT ON COLUMN audit_logs.user_agent IS '브라우저/클라이언트 정보';
COMMENT ON COLUMN audit_logs.request_id IS '요청 ID';
COMMENT ON COLUMN audit_logs.created_at IS '로그 생성 일시';

-- ==============================================================================
-- 12. 뷰(View) COMMENT
-- ==============================================================================

COMMENT ON VIEW v_company_rankings IS '업체 우선순위 랭킹 뷰 - 다모아 Pick, 유료 광고, 무료 업체 순으로 정렬된 업체 목록';

-- ==============================================================================
-- 완료
-- ==============================================================================