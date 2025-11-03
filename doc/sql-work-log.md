# SQL Work Log

## 🗓️ 작업 일자: 2025-10-31

## 📋 작업 요약

Damoa 프로젝트의 데이터베이스 스키마를 처음부터 완전히 재설계했습니다.

### 주요 작업 내용
- 기존 마이그레이션 파일 모두 제거
- V1__Initial_schema.sql 파일 새로 생성
- 55개 테이블 설계 및 구현
- 모든 테이블에 UUID + Soft Delete 패턴 적용
- JSONB/배열 타입을 활용한 유연한 구조 설계

---

## 📊 스키마 구조

### 전체 테이블 수: 55개

| 도메인 | 테이블 수 | 주요 테이블 |
|--------|-----------|-------------|
| 사용자 | 7개 | users, user_profiles, user_settings, user_activity_logs 등 |
| 업체 | 7개 | companies, company_services, company_portfolios, company_reviews 등 |
| 광고 | 5개 | ad_campaigns, ad_creatives, ad_impressions, ad_clicks 등 |
| 견적/매칭 | 8개 | estimate_requests, estimate_proposals, matches 등 |
| UMS | 6개 | notifications, notification_templates, notification_logs 등 |
| 파일 | 4개 | files, file_uploads, file_attachments 등 |
| 결제 | 8개 | payments, payment_methods, refunds, credits 등 |
| 게시판 | 5개 | boards, board_comments, board_likes 등 |
| 필터/검색 | 2개 | filter_templates, saved_searches |
| 통계/로그 | 3개 | statistics_daily, analytics_events, audit_logs |

---

## 🏗️ 테이블 상세 설계

### 1. 사용자 도메인 (7개)

#### 1.1 users
- **역할**: 사용자 인증 및 기본 정보 관리
- **주요 컬럼**:
  - email, password: 로그인 정보
  - role: USER, COMPANY, DESIGNER, ADMIN, SUPER_ADMIN
  - email_verified, phone_verified, identity_verified: 인증 상태
  - last_login_at, login_count: 로그인 추적
  - metadata JSONB: 확장 데이터

#### 1.2 user_profiles
- **역할**: 사용자 프로필 확장 정보
- **주요 컬럼**:
  - name, bio, avatar_url: 기본 프로필
  - address, postal_code, latitude, longitude: 주소 정보
  - social_links JSONB: SNS 링크
  - interests text[]: 관심사 태그

#### 1.3 user_settings
- **역할**: 개인 설정 관리
- **주요 컬럼**:
  - notification_settings JSONB: 알림 설정
  - preferences JSONB: 개인화 설정
  - ui_settings JSONB: UI/UX 설정

### 2. 업체 도메인 (7개)

#### 2.1 companies (통합 테이블)
- **역할**: 업체 정보 통합 관리
- **주요 컬럼**:
  - business_info JSONB: 사업자 정보
  - business_hours JSONB: 영업시간
  - service_areas text[]: 서비스 지역
  - tags text[], keywords text[]: 태그/키워드
  - coordinates JSONB: 위치 정보
  - avg_rating, review_count: 평점/리뷰 통계

**JSONB 활용 예시**:
```json
business_info: {
    "registration_number": "123-45-67890",
    "ceo_name": "홍길동",
    "business_type": "서비스업",
    "business_item": "인테리어"
}

business_hours: {
    "mon": {"open": "09:00", "close": "18:00", "is_closed": false},
    "tue": {"open": "09:00", "close": "18:00", "is_closed": false}
}
```

### 3. 광고 시스템 (5개)

#### 3.1 ad_campaigns (통합 광고)
- **역할**: 모든 광고 타입 통합 관리
- **광고 타입**:
  - LISTING: 상위 노출 광고 (입찰 기반)
  - AI_RECOMMENDATION: AI 추천 가중치
  - BANNER: 배너 광고 (위치, 기간 설정)
  - POPUP: 팝업 광고 (표시 규칙)
- **주요 컬럼**:
  - ad_config JSONB: 타입별 설정
  - targeting JSONB: 타겟팅 설정
  - budget_type, budget_amount: 예산 관리

**광고 타입별 설정 예시**:
```json
// LISTING 타입
ad_config: {
    "bid_amount": 1000,
    "daily_budget": 50000,
    "position_boost": 1
}

// AI_RECOMMENDATION 타입
ad_config: {
    "partnership_level": "GOLD",
    "weight": 1.5,
    "categories": ["인테리어"]
}

// BANNER 타입
ad_config: {
    "position": "MAIN_TOP",
    "size": "728x90",
    "image_url": "...",
    "link_url": "..."
}

// POPUP 타입
ad_config: {
    "trigger": "ON_LOAD",
    "delay": 3000,
    "frequency": "ONCE_PER_DAY",
    "size": "500x600"
}
```

### 4. 견적/매칭 시스템 (8개)

#### 4.1 estimate_requests
- **역할**: 견적 요청 관리
- **주요 컬럼**:
  - requirements JSONB: 요구사항 상세
  - tags text[]: 태그
  - required_skills text[]: 필요 기술
  - status: DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED, CANCELLED

#### 4.2 matches
- **역할**: 매칭 확정 관리
- **주요 컬럼**:
  - contract_terms JSONB: 계약 조건
  - status: CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
- **특징**: 분쟁 관리(match_disputes) 제외 (업체 소개만 담당)

### 5. 게시판 시스템 (5개)

#### 5.1 boards (통합 게시판)
- **역할**: 모든 게시판 타입 통합 관리
- **게시판 타입**:
  - NOTICE: 공지사항
  - EVENT: 이벤트 (시작일/종료일 포함)
  - FAQ: 자주 묻는 질문
  - GALLERY: 갤러리 (이미지 중심)
  - DOCUMENT: 자료실 (파일 다운로드)
- **주요 컬럼**:
  - type_data JSONB: 타입별 특수 데이터
  - tags text[]: 태그

**타입별 데이터 예시**:
```json
// EVENT 타입
type_data: {
    "start_date": "2024-01-01",
    "end_date": "2024-01-31",
    "location": "서울시 강남구",
    "max_participants": 100
}

// GALLERY 타입
type_data: {
    "images": ["url1", "url2"],
    "thumbnail_url": "thumb_url"
}

// DOCUMENT 타입
type_data: {
    "file_id": 123,
    "file_size": 1024000,
    "download_count": 10,
    "price": 0
}
```

---

## 🔧 설계 원칙

### 1. 모든 테이블 공통 구조
```sql
id BIGSERIAL PRIMARY KEY,                           -- 내부 참조용
uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL, -- 외부 API용
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,      -- 트리거로 자동 갱신
is_deleted BOOLEAN DEFAULT FALSE,                   -- Soft Delete
deleted_at TIMESTAMP,
metadata JSONB DEFAULT '{}'::JSONB                  -- 확장 데이터
```

### 2. JSONB 활용 전략
- **metadata**: 예측 불가능한 확장 데이터
- **settings**: 구조화된 설정 데이터
- **type_data**: 타입별 특수 데이터
- **장점**: 스키마 변경 없이 유연한 확장 가능

### 3. 배열 타입 활용
- **tags text[]**: 검색/필터용 태그
- **categories text[]**: 카테고리 분류
- **service_areas text[]**: 서비스 지역
- **images text[]**: 이미지 URL 목록
- **장점**: 다대다 관계 테이블 불필요, GIN 인덱스로 빠른 검색

### 4. 필터 템플릿 시스템
- filter_templates 테이블로 동적 필터 관리
- entity_type별 필터 설정
- filter_config JSONB로 유연한 필터 옵션

---

## 📈 인덱스 전략

### 1. UUID 인덱스
- 모든 테이블의 uuid 컬럼에 인덱스 생성
- 외부 API 조회 최적화

### 2. Full-text Search 인덱스
```sql
CREATE INDEX idx_companies_search ON companies USING GIN(
    to_tsvector('simple', name || ' ' || description || ' ' || array_to_string(tags, ' '))
);
```

### 3. JSONB GIN 인덱스
```sql
CREATE INDEX idx_companies_metadata ON companies USING GIN(metadata);
CREATE INDEX idx_companies_business_info ON companies USING GIN(business_info);
```

### 4. 배열 GIN 인덱스
```sql
CREATE INDEX idx_companies_tags ON companies USING GIN(tags);
CREATE INDEX idx_companies_service_areas ON companies USING GIN(service_areas);
```

### 5. 복합 인덱스
```sql
CREATE INDEX idx_estimate_requests_user_status
  ON estimate_requests(user_id, status, created_at DESC)
  WHERE is_deleted = FALSE;
```

---

## 🔄 Redis vs PostgreSQL 구분

### Redis 사용 (임시/캐시)
- JWT 토큰 (Access/Refresh)
- 이메일/SMS OTP 인증 코드
- 회원가입 임시 데이터
- 세션 데이터
- 실시간 알림
- 속도 제한 카운터
- 임시 파일 업로드 URL

### PostgreSQL 사용 (영구 저장)
- 사용자 정보
- 업체 정보
- 견적/매칭 데이터
- 결제 정보
- 게시판 콘텐츠
- 파일 메타데이터
- 통계/로그

---

## 🚀 확장 계획

### V2: 쇼핑몰 기능 (13개 테이블 추가)
```sql
-- V2__Add_shopping_mall.sql
- products               -- 상품
- product_categories     -- 상품 카테고리
- product_options        -- 상품 옵션
- product_reviews        -- 상품 리뷰
- shopping_carts         -- 장바구니
- orders                 -- 주문
- order_items            -- 주문 항목
- order_deliveries       -- 배송 정보
- user_addresses         -- 배송 주소
- wishlists              -- 찜목록
- inventory              -- 재고 관리
- subscriptions          -- 구독 상품
- subscription_plans     -- 구독 플랜
```

### V3: 추가 기능 고려사항
- 실시간 채팅 시스템
- 화상 상담 예약
- AR/VR 콘텐츠 관리
- 블록체인 기반 계약 관리
- AI 추천 엔진 데이터

---

## 💡 성능 최적화

### 1. 테이블 파티셔닝 고려
- user_activity_logs: 월별 파티션
- analytics_events: 일별 파티션
- notification_logs: 월별 파티션

### 2. 집계 테이블 활용
- statistics_daily: 매일 자정 집계
- company_statistics: 시간별/일별 집계

### 3. 캐싱 전략
- 업체 목록: Redis 캐시 (5분)
- 게시판 목록: Redis 캐시 (1분)
- 사용자 프로필: Redis 캐시 (10분)

---

## 📝 주요 설계 결정 사항

### 1. 테이블 통합 vs 분리
- **companies**: business_hours, service_areas, tags를 하나의 테이블로 통합
  - 이유: 조인 감소, 조회 성능 향상
- **boards**: 모든 게시판 타입 통합, type_data JSONB로 특수 필드 처리
  - 이유: 코드 재사용성, 관리 편의성

### 2. 과도한 정규화 제거
- user_verifications → users 테이블 컬럼으로 통합
- company_employees 테이블 제거 (불필요)
- match_disputes 테이블 제거 (분쟁 관여 안함)

### 3. JSONB 적극 활용
- 스키마 변경 최소화
- 유연한 데이터 구조
- 확장성 확보

### 4. 배열 타입 활용
- 다대다 관계 테이블 대체
- GIN 인덱스로 성능 확보
- 간단한 쿼리로 필터링

---

## ✅ 완료된 작업

1. ✅ V1__Initial_schema.sql 생성 (55개 테이블)
2. ✅ 모든 테이블 COMMENT 추가
3. ✅ 인덱스 전략 구현
4. ✅ 트리거 생성 (updated_at 자동 갱신)
5. ✅ ENUM 타입 정의
6. ✅ 초기 데이터 삽입

---

## 🔜 다음 작업 예정

1. Entity 클래스 생성/수정
2. Repository 인터페이스 생성
3. Service 레이어 구현
4. Controller 구현
5. 테스트 코드 작성

---

## 📌 참고사항

- 모든 날짜/시간은 UTC 기준
- 금액은 DECIMAL(12,2) 타입 사용 (원 단위)
- 파일 경로는 S3 기준
- 검색은 PostgreSQL Full-text search 활용
- 실시간 데이터는 Redis 우선 사용

---

## 🔄 V2 개선사항 (2025-10-31)

### 📋 개선 요약

V2 마이그레이션에서 다음 개선사항들을 적용했습니다:
- 업체 정보 확장 (복수 이미지, 상세 에디터 내용, SNS 링크)
- 광고 시스템 개선 (30일 환산 가치 계산 방식)
- 다모아 Pick 시스템 추가 (시즌별 추천 업체)

### 1. 업체 정보 확장

#### 1.1 companies 테이블 확장
```sql
-- 추가된 컬럼
detail_content TEXT                    -- HTML/Markdown 에디터 내용
detail_content_format VARCHAR(20)      -- HTML 또는 MARKDOWN
primary_phone VARCHAR(20)              -- 대표 전화
secondary_phone VARCHAR(20)            -- 보조 전화
emergency_contact VARCHAR(20)          -- 긴급 연락처
kakao_chat_url VARCHAR(500)           -- 카카오톡 채팅 URL
social_links JSONB                    -- {facebook, instagram, blog, youtube}
business_hours_note TEXT              -- 영업시간 특이사항
```

#### 1.2 company_images 테이블 (신규)
- **역할**: 업체 복수 이미지 관리
- **주요 컬럼**:
  - image_url: 이미지 URL
  - image_type: LOGO, COVER, GALLERY, INTERIOR, EXTERIOR, PORTFOLIO
  - is_primary: 대표 이미지 여부
  - display_order: 표시 순서

### 2. 광고 시스템 개선

#### 2.1 30일 환산 가치 계산 방식

**핵심 계산식**:
```
각 결제의 30일 환산 가치 = (결제 금액 ÷ 적용 일수) × 30
총 광고 가치 = 모든 결제의 30일 환산 가치 합계
```

**예시**:
```
A사: 30일 30만원
→ 30일 환산: (30만원/30일) × 30 = 30만원

B사: 30일 29만원 + 10일 후 1만원 추가
→ 초기: (29만원/30일) × 30 = 29만원
→ 추가: (1만원/20일) × 30 = 1.5만원
→ 총합: 30.5만원

결과: B사가 A사보다 우선순위 높음 (30.5만원 > 30만원)
```

#### 2.2 ad_campaigns 테이블 확장
```sql
-- 추가된 컬럼
total_value_30d DECIMAL(12,2)         -- 30일 환산 총 가치
priority_score DECIMAL(12,2)          -- 우선순위 점수 (= total_value_30d)
secondary_score DECIMAL(12,2)         -- 2차 정렬 점수 (평점×1000 + 리뷰×10)
is_premium BOOLEAN                    -- 프리미엄 광고 여부
premium_until TIMESTAMP                -- 프리미엄 종료일
```

#### 2.3 ad_payments 테이블 (신규)
- **역할**: 광고 결제 이력 및 추가 결제 관리
- **주요 컬럼**:
  - payment_amount: 결제 금액
  - apply_days: 적용 일수
  - daily_rate: 일일 단가 (payment_amount / apply_days)
  - value_30d: 30일 환산 가치 (daily_rate × 30)
  - payment_type: INITIAL(최초), ADDITIONAL(추가), RENEWAL(갱신)

#### 2.4 ad_daily_snapshots 테이블 (신규)
- **역할**: 일별 광고 성과 및 순위 추적
- **주요 컬럼**:
  - snapshot_date: 스냅샷 날짜
  - value_30d: 해당일 30일 환산 가치
  - daily_rank: 해당일 순위
  - impressions, clicks: 성과 지표

### 3. 다모아 Pick 시스템

#### 3.1 damoa_picks 테이블 (신규)
- **역할**: 시즌별 추천 업체 관리
- **Pick 유형**:
  - SPONSORED: 후원 업체
  - OPERATED: 직접 운영 업체
  - PARTNER: 파트너 업체 (렌탈, 침구 등)

- **주요 컬럼**:
  - pick_type: Pick 유형
  - season: 시즌 (2024_SPRING, 2024_SUMMER 등)
  - main_image_url, banner_image_url: 이미지 URL
  - images text[]: 추가 이미지 배열
  - badge_text, badge_color: 배지 표시
  - start_date, end_date: 노출 기간

### 4. 우선순위 정렬 시스템

#### 4.1 노출 순서 (v_company_rankings 뷰)
```
1순위: 다모아 Pick
  - SPONSORED > OPERATED > PARTNER 순
  - 같은 타입 내에서는 display_order 순

2순위: 유료 광고
  - priority_score (30일 환산 가치) 높은 순
  - 동점시 secondary_score (평점/리뷰) 높은 순

3순위: 무료 업체
  - 평점 높은 순
  - 리뷰 많은 순
```

#### 4.2 2차 정렬 점수 계산
```sql
secondary_score =
  (평점 × 1000) +
  (리뷰수 × 10) +
  (포트폴리오수 × 5) +
  완료건수
```

### 5. 주요 함수 및 프로시저

#### 5.1 calculate_total_value_30d(campaign_id)
- 캠페인의 30일 환산 총 가치 계산
- 모든 활성 결제의 value_30d 합계 반환

#### 5.2 calculate_secondary_score(company_id)
- 업체의 2차 정렬 점수 계산
- 평점, 리뷰, 포트폴리오 기반

#### 5.3 process_additional_payment(campaign_id, amount, date)
- 광고 추가 결제 처리
- 남은 일수 기준으로 30일 환산 가치 재계산
- 우선순위 점수 자동 업데이트

#### 5.4 create_ad_campaign_with_payment(company_id, amount, days)
- 광고 캠페인 생성 및 초기 결제 처리
- ad_campaigns와 ad_payments 동시 생성

### 6. 실제 시나리오 예시

#### 시나리오 1: 단순 광고비 비교
```
A사: 30일 30만원 → 우선순위 300,000
B사: 30일 25만원 → 우선순위 250,000
→ A사가 B사보다 우선
```

#### 시나리오 2: 추가 결제 효과
```
C사: 30일 20만원 → 초기 우선순위 200,000
10일 후: +10만원 추가 (남은 20일)
→ 추가분 30일 환산: (10만원/20일)×30 = 15만원
→ 총 우선순위: 350,000 (20만원 + 15만원)
```

#### 시나리오 3: 복수 추가 결제
```
D사:
- 1일: 10만원/30일 → 10만원
- 10일: +5만원/21일 → 7.14만원
- 20일: +3만원/11일 → 8.18만원
→ 총 30일 환산: 25.32만원
```

### 7. 사용 예시

#### 광고 캠페인 생성
```sql
CALL create_ad_campaign_with_payment(
    p_company_id := 1,
    p_payment_amount := 300000,  -- 30만원
    p_duration_days := 30,
    p_ad_type := 'LISTING'
);
```

#### 추가 결제 처리
```sql
CALL process_additional_payment(
    p_campaign_id := 1,
    p_payment_amount := 50000,   -- 5만원 추가
    p_payment_date := CURRENT_DATE + 10
);
```

#### 다모아 Pick 등록
```sql
INSERT INTO damoa_picks (
    company_id, pick_type, season, title,
    main_image_url, badge_text, badge_color,
    start_date, end_date, display_order
) VALUES (
    1, 'SPONSORED', '2024_WINTER', '겨울 추천 업체',
    'https://example.com/main.jpg', '다모아 추천', '#FF6B6B',
    '2024-12-01', '2025-02-28', 1
);
```

#### 업체 랭킹 조회
```sql
SELECT
    rank as "순위",
    name as "업체명",
    tier_name as "구분",
    total_value_30d as "30일환산가치",
    avg_rating as "평점"
FROM v_company_rankings
LIMIT 20;
```

### 8. 성능 최적화

#### 8.1 추가된 인덱스
- idx_company_images_company_id: 업체별 이미지 조회
- idx_ad_payments_campaign_id: 캠페인별 결제 이력 조회
- idx_damoa_picks_active: 활성 Pick 빠른 조회

#### 8.2 뷰(View) 활용
- v_company_rankings: 복잡한 우선순위 계산 쿼리 단순화
- 실시간 랭킹 조회 성능 향상

### 9. 마이그레이션 정보

#### V2 마이그레이션 파일
- 파일명: V2__Add_company_improvements.sql
- 테이블 추가: 4개 (company_images, ad_payments, ad_daily_snapshots, damoa_picks)
- 컬럼 추가: companies 8개, ad_campaigns 5개
- 함수/프로시저: 4개
- 뷰: 1개 (v_company_rankings)

---

## 🔄 V3 데이터베이스 코멘트 추가 (2025-10-31)

### 📋 작업 요약

모든 데이터베이스 테이블과 컬럼에 한글 코멘트를 추가하여 개발자들이 스키마를 쉽게 이해할 수 있도록 개선했습니다.

### 주요 작업 내용

#### 코멘트 추가 범위
```sql
-- 테이블 코멘트
COMMENT ON TABLE users IS '사용자 기본 정보 및 인증 관리 테이블';
COMMENT ON TABLE companies IS '업체 기본 정보 및 사업자 정보 관리 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN users.email IS '사용자 이메일 (로그인 ID)';
COMMENT ON COLUMN users.role IS '사용자 역할 (USER/COMPANY/DESIGNER/ADMIN)';
```

#### 작업 결과
- **전체 테이블**: 60개
- **코멘트 추가 컬럼**: 900+ 개
- **도메인별 구성**:
  - 사용자 관리 (7개 테이블)
  - 업체 관리 (8개 테이블)
  - 광고 시스템 (8개 테이블)
  - 다모아픽 (1개 테이블)
  - 견적/매칭 (8개 테이블)
  - 알림 시스템 (6개 테이블)
  - 파일 관리 (4개 테이블)
  - 결제 시스템 (8개 테이블)
  - 게시판 (5개 테이블)
  - 필터/검색 (2개 테이블)
  - 통계/로그 (3개 테이블)

### 마이그레이션 정보
- 파일명: V3__Add_comprehensive_comments.sql
- 실행일: 2025-10-31
- 상태: ✅ 완료

---

## 🔄 V4 어드민 시스템 추가 (2025-10-31)

### 📋 작업 요약

포괄적인 어드민 관리 시스템을 구축하여 보안, 권한 관리, 모니터링 기능을 제공합니다.

### 1. 생성된 테이블 (11개)

#### 1.1 인증 & 보안 (5개)

##### admin_sessions
- **용도**: 어드민 세션 관리
- **주요 기능**:
  - 로그인 세션 추적
  - 세션 타임아웃 관리
  - 디바이스/IP 정보 기록
  - 동시 로그인 제한
  - 강제 로그아웃

##### admin_login_history
- **용도**: 어드민 로그인 기록
- **주요 기능**:
  - 모든 로그인 시도 기록
  - 성공/실패 구분
  - 의심스러운 활동 감지
  - IP/디바이스 추적
  - 로그인 통계

##### admin_ip_whitelist
- **용도**: IP 접근 제어
- **주요 기능**:
  - IP 화이트리스트/블랙리스트
  - CIDR 표기법 지원
  - 사용자별/전역 규칙
  - 유효 기간 설정

##### admin_two_factor_auth
- **용도**: 2단계 인증 (2FA/MFA)
- **주요 기능**:
  - TOTP, SMS, Email 지원
  - 백업 코드 관리
  - 인증 방법별 활성화 설정
  - 실패 시도 잠금

##### admin_password_policies
- **용도**: 비밀번호 정책 관리
- **주요 기능**:
  - 복잡도 규칙 (대소문자, 숫자, 특수문자)
  - 비밀번호 만료 (기본 90일)
  - 재사용 방지 (최근 5개)
  - 실패 시 계정 잠금 (5회)

#### 1.2 권한 관리 (3개)

##### admin_roles
- **용도**: 어드민 역할 정의
- **주요 역할**:
  - SUPER_ADMIN: 최고 관리자
  - USER_MANAGER: 사용자 관리자
  - COMPANY_MANAGER: 업체 관리자
  - CONTENT_MANAGER: 콘텐츠 관리자
  - PLANNER_MANAGER: 기획자
  - ESTIMATE_MANAGER: 견적 관리자
  - PAYMENT_MANAGER: 결제 관리자
  - AUDIT_VIEWER: 감사 뷰어
  - CONSULTATION_MANAGER: 상담 관리자

##### admin_page_permissions
- **용도**: 페이지/메뉴 권한 정의
- **주요 기능**:
  - 페이지별 접근 권한 설정
  - 계층적 메뉴 구조
  - URL 패턴 매칭
  - 모듈별 그룹화
  - 아이콘, 배지 설정

##### admin_role_page_access
- **용도**: 역할-페이지 매핑
- **주요 기능**:
  - 역할별 접근 가능 페이지 설정
  - 접근 유형 (FULL, READ_ONLY, HIDDEN, CUSTOM)
  - 동적 권한 관리

#### 1.3 운영 관리 (3개)

##### admin_notifications
- **용도**: 어드민 전용 알림
- **주요 기능**:
  - 심각도별 알림 (LOW, MEDIUM, HIGH, CRITICAL)
  - 역할/사용자별 타겟팅
  - 시스템 이벤트 알림
  - 읽음 상태 추적
  - 액션 URL 설정

##### admin_settings
- **용도**: 시스템 설정 & Feature Flags
- **주요 기능**:
  - 카테고리별 설정 관리
  - Feature Flag 지원
  - 설정 값 타입 (STRING, NUMBER, BOOLEAN, JSON)
  - 환경별 설정 (LOCAL, DEV, PROD)
  - 변경 이력 추적

##### admin_activity_summary
- **용도**: 어드민 활동 통계
- **주요 기능**:
  - 일/주/월별 활동 요약
  - 로그인 횟수, 작업 시간
  - 엔티티별 생성/수정/삭제 통계
  - API 호출 및 에러 통계
  - 성과 점수 계산

### 2. 생성된 함수 (2개)

#### get_admin_permissions(user_id)
- 사용자의 모든 권한 조회
- 역할 기반 + 직접 할당 권한 통합

#### check_ip_whitelist(user_id, ip_address)
- IP 화이트리스트 검증
- 전역 규칙 + 사용자별 규칙 통합

### 3. 생성된 뷰 (2개)

#### v_admin_dashboard
- 어드민 대시보드 통계
- 활성 세션, 로그인 통계, 보안 지표

#### v_admin_active_sessions
- 활성 어드민 세션 모니터링
- 세션 시간, 남은 시간 계산

### 4. 초기 데이터

#### 어드민 페이지 (25개)
- 대시보드 (2개)
- 사용자 관리 (4개)
- 업체 관리 (3개)
- 견적 관리 (3개)
- 결제 관리 (3개)
- 광고 관리 (3개)
- 콘텐츠 관리 (3개)
- 시스템 관리 (4개)

#### 시스템 설정 (7개)
- 시스템 점검 모드
- 세션 타임아웃 (3600초)
- 최대 로그인 시도 (5회)
- 페이지당 항목 수 (20개)
- 이메일/SMS 알림 활성화

#### 비밀번호 정책 (1개)
- 최소 10자, 대소문자/숫자/특수문자 필수
- 90일 만료, 5개 재사용 금지

### 5. 마이그레이션 정보

#### V4 시리즈
- V4__Add_admin_tables.sql
- V4_1__Fix_admin_roles_table.sql
- V4_2__Fix_admin_active_sessions_view.sql

#### 실행일
2025-10-31

#### 상태
✅ 완료

---

## 🔄 V5 빠른상담 & 제휴문의 시스템 추가 (2025-10-31)

### 📋 작업 요약

사용자 빠른 상담 신청과 제휴/광고 문의를 관리하는 시스템을 구축했습니다. 개인정보보호법 준수를 위한 3가지 동의 관리 기능을 포함합니다.

### 1. 생성된 테이블 (3개)

#### 1.1 quick_consultations - 빠른상담 요청
- **용도**: 웹사이트 방문자의 빠른 상담 신청 관리
- **주요 기능**:
  ```
  ✓ 비회원 상담 지원 (user_id NULL 허용)
  ✓ 신청자 정보: 이름, 전화번호, 이메일
  ✓ 상담 내용 및 선호 연락 방법/시간대
  ✓ 3가지 필수 동의 (비정규화):
    - personal_info_consent: 개인정보 수집/이용 동의
    - third_party_consent: 제3자 제공 동의
    - marketing_consent: 마케팅 수신 동의
  ✓ 동의 시 IP 주소 및 약관 버전 기록 (법적 증빙)
  ✓ 관리자의 업체 배정
  ✓ 상태 추적: SUBMITTED → ASSIGNED → IN_PROGRESS → RESPONDED → COMPLETED
  ✓ 응답 및 완료 정보 관리
  ```

- **상태 종류**:
  - SUBMITTED: 제출됨 (대기중)
  - ASSIGNED: 업체 배정됨
  - IN_PROGRESS: 상담 진행중
  - RESPONDED: 응답 완료
  - COMPLETED: 완료
  - CANCELLED: 취소

- **법적 준수**:
  - consent_ip_address: 동의 시 IP 기록
  - consent_version: 약관 버전 기록
  - *_consent_at: 동의 시점 타임스탬프

#### 1.2 partnership_inquiries - 제휴/광고 문의
- **용도**: 업체/파트너사의 사업적 문의 관리
- **주요 기능**:
  ```
  ✓ 문의 유형 구분:
    - PARTNERSHIP: 제휴 문의
    - ADVERTISING: 광고 문의
    - SPONSORSHIP: 후원 문의
    - BUSINESS: 사업 제안
    - OTHER: 기타
  ✓ 회사 정보: 회사명, 담당자, 직책, 연락처, 웹사이트
  ✓ 광고 관련 정보:
    - budget_range: UNDER_1M, 1M_5M, 5M_10M, OVER_10M
    - preferred_ad_type: LISTING, BANNER, POPUP, AI_RECOMMENDATION, DAMOA_PICK
    - expected_duration: 예상 광고 기간
  ✓ 우선순위 관리: URGENT, HIGH, NORMAL, LOW
  ✓ 첨부파일 지원 (JSONB)
  ✓ 관리자 배정 및 협상 관리
  ✓ 관리자 전용 내부 메모
  ```

- **상태 종류**:
  - SUBMITTED: 제출됨
  - IN_REVIEW: 검토중
  - RESPONDED: 응답 완료
  - IN_NEGOTIATION: 협상중
  - ACCEPTED: 수락
  - REJECTED: 거절
  - COMPLETED: 완료

#### 1.3 consultation_messages - 상담 메시지 스레드
- **용도**: 빠른상담/제휴문의 후속 대화 관리
- **주요 기능**:
  ```
  ✓ Polymorphic 관계:
    - consultation_type: QUICK (빠른상담) / PARTNERSHIP (제휴문의)
    - consultation_id: 해당 상담/문의 ID
  ✓ 발신자 유형:
    - ADMIN: 관리자
    - COMPANY: 업체
    - USER: 사용자
    - SYSTEM: 시스템 자동 메시지
  ✓ 메시지 타입:
    - REPLY: 답변
    - NOTE: 내부 메모 (관리자 전용)
    - SYSTEM: 시스템 알림
  ✓ 읽음 상태 추적
  ✓ 첨부파일 지원 (JSONB)
  ```

### 2. 어드민 페이지 추가 (7개)

#### 상담 관리 모듈 (CONSULTATION)

| 페이지 코드 | 페이지 이름 | URL | 설명 |
|------------|-----------|-----|------|
| CONSULTATION_MANAGEMENT | 상담 관리 | /admin/consultations | 메인 메뉴 |
| QUICK_CONSULTATION_LIST | 빠른상담 목록 | /admin/consultations/quick | 모든 빠른상담 조회/관리 |
| QUICK_CONSULTATION_DETAIL | 빠른상담 상세 | /admin/consultations/quick/* | 개별 상담 상세/응답 |
| PARTNERSHIP_INQUIRY_LIST | 제휴/광고 문의 목록 | /admin/consultations/partnership | 제휴 문의 조회/관리 |
| PARTNERSHIP_INQUIRY_DETAIL | 제휴/광고 상세 | /admin/consultations/partnership/* | 개별 문의 협상 관리 |
| CONSULTATION_STATS | 상담 통계 | /admin/consultations/stats | 통계 대시보드 |
| CONSENT_MANAGEMENT | 동의 기록 관리 | /admin/consultations/consents | 법적 동의 기록 조회/감사 |

### 3. 관리자 역할별 권한

#### SUPER_ADMIN
- 모든 상담 기능 전체 접근

#### CONSULTATION_MANAGER (신규 역할)
- 상담 관리 모듈 전체 접근
- 대시보드 접근

#### USER_MANAGER
- 빠른상담 관련 페이지만 접근
- 상담 통계 조회

#### COMPANY_MANAGER
- 제휴/광고 문의 관련 페이지만 접근
- 상담 통계 조회

#### CONTENT_MANAGER
- 모든 상담 읽기 전용 (통계/분석용)

### 4. 생성된 뷰 (1개)

#### v_consultation_dashboard
- **용도**: 상담 통계 대시보드
- **제공 지표**:
  ```
  - 전체/대기/완료 빠른상담 수
  - 오늘 접수된 빠른상담 수
  - 전체/대기/긴급 제휴문의 수
  - 오늘 접수된 제휴문의 수
  - 읽지 않은 메시지 수
  - 마케팅 동의 사용자 수
  - 최근 7일간 통계
  ```

### 5. 시스템 설정 추가 (5개)

```sql
consultation.auto_assign_enabled: 빠른상담 자동 배정 (기본: false)
consultation.response_sla_hours: 상담 응답 SLA (기본: 24시간)
consultation.notification_enabled: 상담 알림 활성화 (기본: true)
partnership.review_sla_days: 제휴문의 검토 SLA (기본: 3일)
consent.retention_years: 동의 기록 보관 기간 (기본: 3년)
```

### 6. 테이블 간 관계

```
quick_consultations (빠른상담)
├── 1:N → consultation_messages (대화 기록)
├── N:1 → users (신청자 - 비회원 시 NULL)
├── N:1 → companies (배정된 업체)
└── N:1 → users (배정/응답 관리자)

partnership_inquiries (제휴문의)
├── 1:N → consultation_messages (대화 기록)
├── N:1 → users (문의자)
├── N:1 → companies (문의 업체)
└── N:1 → users (담당 관리자)

consultation_messages (메시지)
├── Polymorphic → quick_consultations OR partnership_inquiries
└── N:1 → users (발신자, 읽은 사람)
```

### 7. 주요 특징

#### 법적 준수
- ✓ 개인정보보호법 준수 (3가지 동의 명확히 구분)
- ✓ 동의 시점 IP 주소 및 약관 버전 기록
- ✓ GDPR 준수 가능

#### 비회원 지원
- ✓ user_id NULL 허용으로 비회원 상담 가능
- ✓ 이름, 전화번호만으로 간편 신청

#### 효율적 관리
- ✓ 상태별 진행 관리
- ✓ 우선순위 설정 (긴급/높음/보통/낮음)
- ✓ 메시지 스레드로 실시간 소통
- ✓ 읽음 상태 추적

#### 통계 및 분석
- ✓ 대시보드 뷰로 실시간 현황 파악
- ✓ IP 추적으로 지역별 분석
- ✓ 응답률/완료율 모니터링
- ✓ 마케팅 동의 사용자 집계

### 8. 마이그레이션 정보

- 파일명: V5__Add_consultation_tables.sql
- 실행일: 2025-10-31
- 상태: ✅ 완료

### 9. 설계 결정 사항

#### 동의 정보 비정규화
- **결정**: consultation_consent_records 테이블 제거, quick_consultations에 통합
- **이유**:
  - 과도한 정규화 방지
  - 조회 성능 향상 (JOIN 불필요)
  - 동의 정보는 상담당 1회만 존재
  - IP/버전 정보로 법적 증빙 충분

#### Polymorphic 관계
- **결정**: consultation_messages를 빠른상담/제휴문의 모두 사용
- **이유**:
  - 중복 테이블 방지
  - 통일된 메시지 인터페이스
  - consultation_type으로 명확한 구분

---

## 🔄 V6 필터 관리 시스템 추가 (2025-10-31)

### 📋 작업 요약

JSONB 기반 filter_templates 테이블을 정규화된 필터 관리 시스템으로 개선했습니다. 관리자가 개별 필터 옵션을 쉽게 추가/수정/삭제할 수 있고, 계층적 구조(서울 > 강남구 > 역삼동)를 지원합니다.

### 1. 문제점 및 개선 사유

#### 기존 filter_templates 문제점
```sql
-- 기존: JSONB로 모든 옵션 저장
filter_config: {
  "region": ["서울", "부산", "대구", "인천"],
  "department": ["내과", "외과", "소아과"],
  "specialty": ["피부과", "정형외과"]
}
```

**문제점**:
- ❌ 개별 옵션 추가/수정/삭제 불가 (전체 JSONB를 교체해야 함)
- ❌ 계층 구조 표현 불가 (서울 > 강남구 > 역삼동)
- ❌ 옵션별 메타데이터 관리 어려움 (좌표, 지역코드 등)
- ❌ 어드민 UI 구현 복잡
- ❌ 검색/필터 성능 저하
- ❌ 사용 횟수 추적 불가

#### 개선 목표
- ✅ 관리자가 개별 옵션을 쉽게 CRUD
- ✅ 계층 구조 지원 (최대 3단계)
- ✅ 옵션별 메타데이터 관리 (JSONB)
- ✅ 자동 경로 생성 (트리거)
- ✅ 사용 횟수 추적

### 2. 생성된 테이블 (3개)

#### 2.1 filter_categories - 필터 카테고리 정의
- **용도**: 필터 종류 정의 및 설정
- **주요 기능**:
  ```
  ✓ 카테고리 코드/이름 (region, department, specialty, price_range, rating)
  ✓ 필터 타입:
    - SINGLE_SELECT: 단일 선택 (예: 평점)
    - MULTI_SELECT: 복수 선택 (예: 전문영역)
    - HIERARCHICAL: 계층적 선택 (예: 지역)
  ✓ 계층 구조 지원 여부
  ✓ 최대 깊이 설정 (0~3)
  ✓ 적용 대상 엔티티 (COMPANY, HOSPITAL, SERVICE)
  ✓ 표시 순서 관리
  ```

- **초기 카테고리 (5개)**:
  | 코드 | 이름 | 타입 | 계층 지원 | 최대 깊이 | 적용 대상 |
  |------|------|------|----------|---------|---------|
  | region | 지역 | HIERARCHICAL | O | 3 | ALL |
  | department | 진료과 | MULTI_SELECT | X | 0 | HOSPITAL |
  | specialty | 전문영역 | MULTI_SELECT | X | 0 | COMPANY |
  | price_range | 가격대 | SINGLE_SELECT | X | 0 | SERVICE |
  | rating | 평점 | SINGLE_SELECT | X | 0 | ALL |

#### 2.2 filter_options - 필터 옵션 (개별 CRUD)
- **용도**: 개별 필터 옵션 관리
- **주요 기능**:
  ```
  ✓ 옵션 코드/이름 (seoul, gangnam-gu, yeoksam-dong 등)
  ✓ 계층 구조:
    - parent_id: 부모 옵션 ID (NULL이면 최상위)
    - depth: 깊이 (0=최상위, 1=2단계, 2=3단계)
    - path: 자동 생성 경로 (/seoul/gangnam-gu/yeoksam-dong)
  ✓ 메타데이터 (JSONB):
    - 좌표 (latitude, longitude)
    - 지역 코드 (region_code)
    - 인구수, 특성 등 확장 데이터
  ✓ 표시 순서 관리
  ✓ 사용 횟수 추적 (usage_count)
  ✓ 설명 필드
  ```

- **초기 옵션 (42개)**:
  - 지역: 8개 (서울, 경기, 인천, 부산, 대구, 대전, 광주, 울산)
  - 서울 하위: 5개 (강남구, 서초구, 송파구, 강동구, 관악구)
  - 강남구 하위: 3개 (역삼동, 삼성동, 청담동)
  - 진료과: 10개 (내과, 외과, 정형외과, 성형외과, 피부과, 안과, 치과, 한의과, 소아청소년과, 재활의학과)
  - 전문영역: 16개 (병원 인테리어 설계, 의료기기 배치, 위생 설비, 병실 디자인, 대기실 설계 등)

#### 2.3 filter_option_relations - 필터 옵션 간 관계
- **용도**: 복잡한 다대다 관계 표현
- **주요 기능**:
  ```
  ✓ 옵션 간 관계 정의:
    - RELATED_TO: 관련 있음 (예: "강남구" ↔ "고급 인테리어")
    - INCLUDES: 포함 관계 (예: "병원 설계" includes "의료기기 배치")
    - EQUIVALENT: 동등 관계 (예: "신사동" ≈ "강남 중심")
  ✓ 가중치 설정 (1.0~5.0)
  ✓ 관계 설명
  ✓ 양방향 관계 지원
  ```

### 3. 자동 경로 생성 트리거

#### update_filter_option_path() 함수
```sql
-- 부모 옵션이 없으면: path = /code
-- 부모 옵션이 있으면: path = parent_path/code

예시:
서울 (parent_id=NULL): /seoul
강남구 (parent_id=서울): /seoul/gangnam-gu
역삼동 (parent_id=강남구): /seoul/gangnam-gu/yeoksam-dong
```

**작동 방식**:
1. INSERT/UPDATE 시 자동 실행
2. 부모 옵션의 path를 조회
3. 현재 옵션의 code를 추가하여 새 path 생성
4. depth도 자동 계산 (parent_depth + 1)

### 4. 계층 구조 조회를 위한 재귀 CTE 뷰

#### v_filter_option_tree 뷰
```sql
-- 재귀 CTE로 전체 트리 구조 조회
SELECT
  id, category_id, code, name, depth, path,
  parent_id, full_name
FROM filter_options
START WITH parent_id IS NULL
CONNECT BY parent_id = PRIOR id;
```

**사용 예시**:
```sql
-- 특정 카테고리의 전체 트리 조회
SELECT * FROM v_filter_option_tree
WHERE category_id = (SELECT id FROM filter_categories WHERE code = 'region')
ORDER BY path;

결과:
/seoul → 서울
/seoul/gangnam-gu → 서울 > 강남구
/seoul/gangnam-gu/yeoksam-dong → 서울 > 강남구 > 역삼동
```

### 5. 인덱스 전략

```sql
-- 기본 조회 성능
CREATE INDEX idx_filter_options_category_id ON filter_options(category_id);
CREATE INDEX idx_filter_options_parent_id ON filter_options(parent_id);

-- 경로 기반 검색 (LIKE '/seoul%')
CREATE INDEX idx_filter_options_path ON filter_options(path);

-- 코드 검색
CREATE INDEX idx_filter_options_code ON filter_options(code);

-- 메타데이터 검색 (JSONB)
CREATE INDEX idx_filter_options_metadata ON filter_options USING GIN(metadata);
```

### 6. 실제 사용 시나리오

#### 시나리오 1: 관리자가 "세종시" 추가
```sql
-- 기존: filter_templates의 JSONB 전체를 수정해야 함
UPDATE filter_templates SET filter_config = ... (전체 수정)

-- 개선: 단일 INSERT로 해결
INSERT INTO filter_options (category_id, code, name, display_order)
VALUES (
  (SELECT id FROM filter_categories WHERE code = 'region'),
  'sejong', '세종', 9
);
-- path가 자동으로 '/sejong'로 생성됨
```

#### 시나리오 2: 계층 구조 조회 (서울 > 강남구 하위 동 목록)
```sql
-- 재귀 쿼리로 쉽게 조회
SELECT * FROM filter_options
WHERE path LIKE '/seoul/gangnam-gu/%'
  AND depth = 2
ORDER BY display_order;
```

#### 시나리오 3: 메타데이터 활용 (좌표 기반 지역 검색)
```sql
-- GIN 인덱스로 빠른 JSONB 검색
SELECT * FROM filter_options
WHERE metadata @> '{"latitude": 37.5}'::jsonb
  AND category_id = (SELECT id FROM filter_categories WHERE code = 'region');
```

### 7. 마이그레이션 영향

#### 삭제 예정
- filter_templates 테이블 (V7에서 삭제)

#### 코드 영향
- FilterService 구현 필요
- Admin UI: 트리 구조 UI 구현
- API: 계층 구조 응답 포맷 설계

### 8. 마이그레이션 정보

- 파일명: V6__Create_filter_management_tables.sql
- 테이블 추가: 3개
- 초기 데이터: 5개 카테고리, 42개 옵션
- 함수: 1개 (update_filter_option_path)
- 뷰: 1개 (v_filter_option_tree)
- 실행일: 2025-10-31
- 상태: ✅ 완료

---

## 🔄 V7 불필요한 테이블 삭제 (2025-10-31)

### 📋 작업 요약

더 이상 필요하지 않은 3개 테이블을 삭제하여 스키마를 정리했습니다.

### 1. 삭제된 테이블 (3개)

#### 1.1 filter_templates
- **삭제 사유**: V6에서 filter_categories, filter_options로 완전히 대체됨
- **설명**: JSONB 방식에서 정규화 테이블로 전환 완료

#### 1.2 company_branches
- **삭제 사유**: 업체 지점 관리 기능 불필요
- **설명**: 단일 업체 정보만 companies 테이블에서 관리

#### 1.3 company_statistics
- **삭제 사유**: 시간별 통계 테이블 불필요
- **설명**: 필요 시 statistics_daily 테이블로 대체 가능

### 2. 영향 분석

#### 데이터 손실
- ✅ filter_templates: V6 이전에 데이터 없음 (초기 마이그레이션 단계)
- ✅ company_branches: 사용하지 않음
- ✅ company_statistics: 사용하지 않음

#### CASCADE 영향
```sql
DROP TABLE IF EXISTS filter_templates CASCADE;
DROP TABLE IF EXISTS company_branches CASCADE;
DROP TABLE IF EXISTS company_statistics CASCADE;
```
- 연관 뷰, 인덱스, 제약조건 자동 삭제
- 다른 테이블에 영향 없음 (외래 키 없음)

### 3. 테이블 수 변화

| 마이그레이션 | 작업 | 테이블 수 |
|-------------|------|----------|
| V5 완료 후 | - | 73개 |
| V6 | +3개 | 76개 |
| V7 | -3개 | 73개 |

### 4. 마이그레이션 정보

- 파일명: V7__Drop_unused_tables.sql
- 테이블 삭제: 3개
- 실행일: 2025-10-31
- 상태: ✅ 완료

---

## 📊 전체 데이터베이스 현황 (2025-10-31 기준)

### 총 테이블 수: 74개

| 마이그레이션 | 테이블 수 | 주요 내용 |
|-------------|-----------|-----------|
| V1 | 55개 | 초기 스키마 (사용자, 업체, 견적, 결제, 게시판 등) |
| V2 | +4개 (59개) | 광고 개선, 다모아픽 (company_images, ad_payments, ad_daily_snapshots, damoa_picks) |
| V3 | 0개 (59개) | 코멘트 추가 (900+ 컬럼) |
| V4 | +11개 (70개) | 어드민 시스템 (세션, 권한, 설정, 통계 등) |
| V5 | +3개 (73개) | 상담 시스템 (quick_consultations, partnership_inquiries, consultation_messages) |
| V6 | +3개 (76개) | 필터 관리 시스템 (filter_categories, filter_options, filter_option_relations) |
| V7 | -3개 (73개) | 불필요 테이블 삭제 (filter_templates, company_branches, company_statistics) |
| **합계** | **73개** | - |

*참고: flyway_schema_history 테이블 포함 시 74개*

### 도메인별 테이블 분포

| 도메인 | 테이블 수 | 비율 |
|--------|-----------|------|
| 사용자 관리 | 7개 | 9.5% |
| 업체 관리 | 6개 | 8.1% |
| 광고 시스템 | 8개 | 10.8% |
| 견적/매칭 | 8개 | 10.8% |
| 결제 시스템 | 8개 | 10.8% |
| UMS (알림) | 6개 | 8.1% |
| 파일 관리 | 4개 | 5.4% |
| 게시판 | 5개 | 6.8% |
| 어드민 시스템 | 11개 | 14.9% |
| 상담 시스템 | 3개 | 4.1% |
| 필터 관리 | 3개 | 4.1% |
| 통계/로그 | 3개 | 4.1% |
| 시스템 | 1개 | 1.4% |

---

## ✅ 완료된 모든 작업

1. ✅ V1__Initial_schema.sql (55개 테이블)
2. ✅ V2__Add_company_improvements.sql (4개 테이블 추가)
3. ✅ V3__Add_comprehensive_comments.sql (900+ 컬럼 코멘트)
4. ✅ V4__Add_admin_tables.sql (10개 테이블)
5. ✅ V4_1__Fix_admin_roles_table.sql (admin_roles 추가)
6. ✅ V4_2__Fix_admin_active_sessions_view.sql (뷰 수정)
7. ✅ V5__Add_consultation_tables.sql (3개 테이블 추가)
8. ✅ V6__Create_filter_management_tables.sql (3개 테이블 추가)
9. ✅ V7__Drop_unused_tables.sql (3개 테이블 삭제)

---

## 🔜 향후 작업 고려사항

### 추가 기능
- [ ] 실시간 채팅 시스템
- [ ] 화상 상담 예약 시스템
- [ ] AR/VR 콘텐츠 관리
- [ ] AI 추천 엔진 데이터 구조
- [ ] 블록체인 기반 계약 관리

### 성능 최적화
- [ ] 파티셔닝 전략 수립 (로그, 통계 테이블)
- [ ] 읽기 전용 복제본 구성
- [ ] 쿼리 성능 모니터링
- [ ] 인덱스 최적화

### 보안 강화
- [ ] 데이터 암호화 (민감 정보)
- [ ] 접근 로그 강화
- [ ] GDPR 완전 준수 검증
- [ ] 백업 및 복구 전략