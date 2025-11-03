# Database Tables Guide - HIP Damoa

**총 테이블 수**: 74개 (flyway_schema_history 포함)
**최종 업데이트**: 2025-10-31
**데이터베이스**: hip_damoa_local / hip_damoa_dev / hip_damoa_prod

---

## 📚 목차

1. [사용자 관리 (7개)](#1-사용자-관리-7개)
2. [업체 관리 (6개)](#2-업체-관리-6개)
3. [광고 시스템 (8개)](#3-광고-시스템-8개)
4. [견적/매칭 (8개)](#4-견적매칭-8개)
5. [결제 시스템 (8개)](#5-결제-시스템-8개)
6. [UMS 알림 (6개)](#6-ums-알림-6개)
7. [파일 관리 (4개)](#7-파일-관리-4개)
8. [게시판 (5개)](#8-게시판-5개)
9. [어드민 시스템 (11개)](#9-어드민-시스템-11개)
10. [상담 시스템 (3개)](#10-상담-시스템-3개)
11. [필터 관리 (3개)](#11-필터-관리-3개)
12. [통계/로그 (3개)](#12-통계로그-3개)
13. [시스템 (1개)](#13-시스템-1개)

---

## 1. 사용자 관리 (7개)

### users
**용도**: 사용자 인증 및 기본 정보 관리
**주요 기능**:
- 로그인 인증 (email, password)
- 역할 관리 (USER, COMPANY, DESIGNER, ADMIN, SUPER_ADMIN)
- 인증 상태 (이메일, 전화번호, 신원 인증)
- 로그인 이력 추적
- 계정 잠금 관리

### user_profiles
**용도**: 사용자 프로필 상세 정보
**주요 기능**:
- 이름, 닉네임, 프로필 사진
- 주소 및 위치 정보 (위도/경도)
- SNS 링크 (JSONB)
- 관심사 태그 (배열)

### user_settings
**용도**: 사용자 개인 설정
**주요 기능**:
- 알림 설정 (JSONB)
- 개인화 설정 (JSONB)
- UI/UX 설정 (JSONB)

### user_activity_logs
**용도**: 사용자 활동 로그
**주요 기능**:
- 사용자 행동 추적
- 이벤트 타입별 기록
- IP, User-Agent 추적
- 활동 데이터 (JSONB)

### user_devices
**용도**: 사용자 디바이스 관리
**주요 기능**:
- 푸시 알림 토큰 관리
- 디바이스 타입/OS/브라우저 정보
- 마지막 사용 시간 추적

### social_accounts
**용도**: 소셜 로그인 연동
**주요 기능**:
- OAuth 제공자 (KAKAO, NAVER, GOOGLE, APPLE)
- 소셜 계정 고유 ID
- 프로필 정보 (JSONB)
- Access/Refresh 토큰

### user_points
**용도**: 사용자 포인트 관리
**주요 기능**:
- 보유 포인트, 누적 적립/사용
- 포인트 만료 관리
- 등급별 혜택

---

## 2. 업체 관리 (6개)

### companies
**용도**: 업체 기본 정보 통합 관리
**주요 기능**:
- 사업자 정보 (JSONB)
- 영업시간 (JSONB)
- 서비스 지역 (배열)
- 위치 정보 (위도/경도)
- 평점, 리뷰 통계
- SNS 링크 (JSONB)
- 상세 설명 (HTML/Markdown)

### company_images
**용도**: 업체 복수 이미지 관리 (V2 추가)
**주요 기능**:
- 이미지 타입 (LOGO, COVER, GALLERY, INTERIOR, EXTERIOR, PORTFOLIO)
- 대표 이미지 설정
- 표시 순서 관리

### company_services
**용도**: 업체 제공 서비스
**주요 기능**:
- 서비스명, 설명, 카테고리
- 가격 정보
- 소요 시간
- 서비스 옵션 (JSONB)

### company_portfolios
**용도**: 업체 포트폴리오
**주요 기능**:
- 프로젝트 정보
- 비포/애프터 이미지
- 프로젝트 세부사항 (JSONB)
- 태그 (배열)

### company_reviews
**용도**: 업체 리뷰
**주요 기능**:
- 평점 (1-5)
- 리뷰 내용, 이미지
- 답변 기능
- 좋아요/신고

### company_certifications
**용도**: 업체 인증서/자격증
**주요 기능**:
- 인증서 정보
- 발급 기관, 번호
- 유효 기간
- 인증서 이미지

---

## 3. 광고 시스템 (8개)

### ad_campaigns
**용도**: 광고 캠페인 통합 관리
**주요 기능**:
- 광고 타입 (LISTING, AI_RECOMMENDATION, BANNER, POPUP)
- 예산 관리 (일일/총 예산)
- 30일 환산 가치 (V2)
- 우선순위 점수 (V2)
- 광고 설정 (JSONB)
- 타겟팅 설정 (JSONB)

### ad_payments
**용도**: 광고 결제 이력 (V2 추가)
**주요 기능**:
- 결제 금액, 적용 일수
- 일일 단가 계산
- 30일 환산 가치 자동 계산
- 결제 타입 (INITIAL, ADDITIONAL, RENEWAL)

### ad_daily_snapshots
**용도**: 광고 일별 성과 스냅샷 (V2 추가)
**주요 기능**:
- 일별 30일 환산 가치
- 일별 순위
- 노출수, 클릭수, 전환수
- CTR, CPC, CVR 계산

### ad_creatives
**용도**: 광고 소재 관리
**주요 기능**:
- 제목, 설명, 이미지, 동영상
- 크리에이티브 타입
- 사이즈, 포맷
- A/B 테스트 그룹

### ad_impressions
**용도**: 광고 노출 기록
**주요 기능**:
- 노출 시간, 위치
- 사용자 정보
- 디바이스/브라우저 정보

### ad_clicks
**용도**: 광고 클릭 기록
**주요 기능**:
- 클릭 시간, IP
- 참조 URL
- 전환 여부

### ad_billings
**용도**: 광고 청구 관리
**주요 기능**:
- 청구 기간, 금액
- 과금 방식 (CPM, CPC, CPA)
- 결제 상태

### damoa_picks
**용도**: 다모아 추천 업체 (V2 추가)
**주요 기능**:
- 추천 타입 (SPONSORED, OPERATED, PARTNER)
- 시즌별 관리
- 메인 이미지, 배너 이미지
- 배지 설정
- 노출 기간, 표시 순서

---

## 4. 견적/매칭 (8개)

### estimate_requests
**용도**: 견적 요청
**주요 기능**:
- 요청 정보 (제목, 설명)
- 요구사항 (JSONB)
- 예산 범위
- 희망 일정
- 상태 (DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED)

### estimate_proposals
**용도**: 견적 제안
**주요 기능**:
- 제안 금액
- 제안서 내용
- 일정 계획
- 포트폴리오 링크
- 상태 (SUBMITTED, ACCEPTED, REJECTED)

### estimate_items
**용도**: 견적 항목 상세
**주요 기능**:
- 항목명, 수량, 단가
- 소계 자동 계산
- 항목 설명

### estimate_attachments
**용도**: 견적 첨부파일
**주요 기능**:
- 파일 정보
- 파일 타입
- 다운로드 추적

### estimate_messages
**용도**: 견적 관련 메시지
**주요 기능**:
- 질문/답변
- 협상 내용
- 읽음 상태

### estimate_templates
**용도**: 견적 템플릿
**주요 기능**:
- 재사용 가능한 템플릿
- 업종별 템플릿
- 템플릿 필드 (JSONB)

### matches
**용도**: 매칭 확정
**주요 기능**:
- 견적 요청-제안 매칭
- 계약 조건 (JSONB)
- 진행 상태
- 완료 정보

### match_reviews
**용도**: 매칭 후기
**주요 기능**:
- 양방향 리뷰 (사용자 ↔ 업체)
- 평점, 후기 내용
- 추천 여부

---

## 5. 결제 시스템 (8개)

### payments
**용도**: 결제 내역
**주요 기능**:
- 결제 금액, 수수료
- 결제 수단
- 결제 상태 (PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED)
- 결제 게이트웨이 정보 (JSONB)

### payment_methods
**용도**: 결제 수단 관리
**주요 기능**:
- 결제 타입 (CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, KAKAOPAY, NAVERPAY)
- 카드 정보 (마스킹)
- 기본 결제 수단 설정

### refunds
**용도**: 환불 내역
**주요 기능**:
- 환불 금액
- 환불 사유
- 환불 상태
- 환불 계좌 정보

### invoices
**용도**: 세금계산서/영수증
**주요 기능**:
- 발행 정보
- 발행 타입 (TAX_INVOICE, RECEIPT, CASH_RECEIPT)
- 발행 상태
- 파일 URL

### credits
**용도**: 사용자 크레딧/포인트
**주요 기능**:
- 보유 크레딧
- 만료 예정 크레딧
- 사용 이력

### credit_transactions
**용도**: 크레딧 거래 내역
**주요 기능**:
- 적립/사용/만료
- 거래 금액
- 거래 사유
- 잔액 추적

### coupons
**용도**: 쿠폰 관리
**주요 기능**:
- 쿠폰 코드, 이름
- 할인 타입 (PERCENTAGE, FIXED_AMOUNT)
- 할인 금액/비율
- 사용 조건 (JSONB)
- 유효 기간

### user_coupons
**용도**: 사용자 쿠폰 발급/사용
**주요 기능**:
- 발급 이력
- 사용 여부
- 사용 일시

---

## 6. UMS 알림 (6개)

### notifications
**용도**: 알림 발송 내역
**주요 기능**:
- 알림 채널 (EMAIL, SMS, PUSH, KAKAO)
- 수신자 정보
- 제목, 내용
- 발송 상태
- 읽음 여부

### notification_templates
**용도**: 알림 템플릿
**주요 기능**:
- 템플릿 코드
- 제목/내용 템플릿
- 변수 치환
- 채널별 템플릿

### notification_settings
**용도**: 사용자 알림 설정
**주요 기능**:
- 채널별 수신 동의
- 알림 타입별 설정
- 야간 수신 거부 시간대

### notification_logs
**용도**: 알림 발송 로그
**주요 기능**:
- 발송 시도/성공/실패
- 에러 메시지
- 발송 서비스 응답 (JSONB)

### sms_verifications
**용도**: SMS 인증
**주요 기능**:
- 인증 코드 발송
- 전화번호 인증
- 만료 시간 관리
- 재전송 제한

### email_verifications
**용도**: 이메일 인증
**주요 기능**:
- 인증 코드/토큰 발송
- 이메일 인증
- 만료 시간 관리

---

## 7. 파일 관리 (4개)

### files
**용도**: 파일 메타데이터
**주요 기능**:
- 파일명, 크기, MIME 타입
- S3 경로
- 업로더 정보
- 파일 상태

### file_uploads
**용도**: 파일 업로드 세션
**주요 기능**:
- 청크 업로드 지원
- 업로드 진행 상태
- Presigned URL 관리

### file_attachments
**용도**: 파일 첨부 관계
**주요 기능**:
- Entity와 파일 연결 (Polymorphic)
- 첨부 타입
- 표시 순서

### file_downloads
**용도**: 파일 다운로드 이력
**주요 기능**:
- 다운로드 추적
- 다운로드 수 집계
- IP 기록

---

## 8. 게시판 (5개)

### boards
**용도**: 통합 게시판
**주요 기능**:
- 게시판 타입 (NOTICE, EVENT, FAQ, GALLERY, DOCUMENT)
- 제목, 내용, 이미지
- 조회수, 좋아요
- 타입별 특수 데이터 (JSONB)
- 고정글, 비밀글

### board_comments
**용도**: 게시글 댓글
**주요 기능**:
- 댓글 내용
- 대댓글 (부모 댓글 ID)
- 좋아요, 신고

### board_likes
**용도**: 게시글/댓글 좋아요
**주요 기능**:
- 좋아요 추적
- 중복 방지

### board_categories
**용도**: 게시판 카테고리
**주요 기능**:
- 카테고리명
- 계층 구조
- 표시 순서

### board_attachments
**용도**: 게시글 첨부파일
**주요 기능**:
- 파일 연결
- 다운로드 추적

---

## 9. 어드민 시스템 (11개)

### admin_sessions
**용도**: 어드민 세션 관리 (V4 추가)
**주요 기능**:
- 로그인 세션 추적
- 세션 토큰, 리프레시 토큰
- 디바이스/IP 정보
- 세션 타임아웃 관리
- 강제 로그아웃

### admin_login_history
**용도**: 어드민 로그인 기록 (V4 추가)
**주요 기능**:
- 모든 로그인 시도 기록
- 성공/실패/차단 구분
- IP, User-Agent 추적
- 실패 사유 기록

### admin_ip_whitelist
**용도**: 어드민 IP 접근 제어 (V4 추가)
**주요 기능**:
- IP 화이트리스트/블랙리스트
- CIDR 표기법 지원
- 사용자별/전역 규칙
- 유효 기간 설정

### admin_two_factor_auth
**용도**: 어드민 2단계 인증 (V4 추가)
**주요 기능**:
- TOTP, SMS, Email 지원
- 백업 코드 관리
- 인증 활성화 설정
- 실패 시도 잠금

### admin_password_policies
**용도**: 어드민 비밀번호 정책 (V4 추가)
**주요 기능**:
- 복잡도 규칙
- 비밀번호 만료 (90일)
- 재사용 방지 (5개)
- 계정 잠금 (5회 실패)

### admin_roles
**용도**: 어드민 역할 정의 (V4.1 추가)
**주요 기능**:
- 역할 할당 (SUPER_ADMIN, USER_MANAGER, COMPANY_MANAGER 등)
- 추가 권한 배열
- 주 역할 설정
- 유효 기간

### admin_page_permissions
**용도**: 어드민 페이지 권한 (V4 추가)
**주요 기능**:
- 페이지 코드, URL 패턴
- 계층적 메뉴 구조
- 필요 권한/역할
- 메뉴 표시 설정
- 아이콘, 배지

### admin_role_page_access
**용도**: 역할-페이지 매핑 (V4 추가)
**주요 기능**:
- 역할별 접근 가능 페이지
- 접근 타입 (FULL, READ_ONLY, HIDDEN, CUSTOM)
- 커스텀 권한 (JSONB)

### admin_notifications
**용도**: 어드민 전용 알림 (V4 추가)
**주요 기능**:
- 심각도 (LOW, MEDIUM, HIGH, CRITICAL)
- 역할/사용자 타겟팅
- 관련 엔티티 연결
- 액션 URL
- 읽음 상태

### admin_settings
**용도**: 시스템 설정 & Feature Flags (V4 추가)
**주요 기능**:
- 카테고리별 설정
- 설정 값 타입 (STRING, NUMBER, BOOLEAN, JSON)
- Feature Flag 지원
- 환경별 설정
- 변경 이력

### admin_activity_summary
**용도**: 어드민 활동 통계 (V4 추가)
**주요 기능**:
- 일/주/월별 활동 요약
- 로그인 횟수, 작업 시간
- 엔티티별 생성/수정/삭제 통계
- API 호출/에러 통계
- 성과 점수

---

## 10. 상담 시스템 (3개)

### quick_consultations
**용도**: 빠른상담 요청 (V5 추가)
**주요 기능**:
- 비회원 상담 지원
- 신청자 정보 (이름, 전화, 이메일)
- 상담 내용, 선호 연락 방법/시간
- **3가지 필수 동의** (개인정보, 제3자, 마케팅)
- 동의 IP/약관 버전 (법적 증빙)
- 업체 배정
- 상태 (SUBMITTED → ASSIGNED → IN_PROGRESS → RESPONDED → COMPLETED)

### partnership_inquiries
**용도**: 제휴/광고 문의 (V5 추가)
**주요 기능**:
- 문의 유형 (PARTNERSHIP, ADVERTISING, SPONSORSHIP, BUSINESS, OTHER)
- 회사 정보 (회사명, 담당자, 직책, 연락처)
- 예산 범위, 선호 광고 타입
- 첨부파일 (JSONB)
- 우선순위 (URGENT, HIGH, NORMAL, LOW)
- 관리자 배정, 협상 관리
- 상태 (SUBMITTED → IN_REVIEW → RESPONDED → IN_NEGOTIATION → ACCEPTED/REJECTED)

### consultation_messages
**용도**: 상담 메시지 스레드 (V5 추가)
**주요 기능**:
- Polymorphic 관계 (빠른상담/제휴문의)
- 발신자 유형 (ADMIN, COMPANY, USER, SYSTEM)
- 메시지 타입 (REPLY, NOTE, SYSTEM)
- 읽음 상태
- 첨부파일 (JSONB)

---

## 11. 필터 관리 (3개)

### filter_categories
**용도**: 필터 카테고리 정의 (V6 추가)
**주요 기능**:
- 카테고리 코드/이름 (region, department, specialty 등)
- 필터 타입 (SINGLE_SELECT, MULTI_SELECT, HIERARCHICAL)
- 계층 구조 지원 여부
- 최대 깊이 설정
- 적용 대상 엔티티 (COMPANY, HOSPITAL, SERVICE)

### filter_options
**용도**: 필터 옵션 (개별 CRUD 가능) (V6 추가)
**주요 기능**:
- 옵션 코드/이름 (seoul, gangnam-gu, yeoksam-dong 등)
- 계층 구조 (parent_id, depth, path)
- 메타데이터 (좌표, 지역코드 등 JSONB)
- 관리자가 개별 추가/수정/삭제 가능
- 사용 횟수 추적

### filter_option_relations
**용도**: 필터 옵션 간 관계 (V6 추가)
**주요 기능**:
- 옵션 간 다대다 관계
- 관계 타입 (RELATED_TO, INCLUDES, EQUIVALENT)
- 복잡한 필터 관계 표현

---

## 12. 통계/로그 (3개)

### statistics_daily
**용도**: 일별 통계 집계
**주요 기능**:
- Entity별 통계
- 일별 집계 데이터
- 통계 메트릭 (JSONB)

### analytics_events
**용도**: 분석 이벤트 추적
**주요 기능**:
- 이벤트 타입
- 사용자 행동 분석
- 이벤트 속성 (JSONB)
- 세션 추적

### audit_logs
**용도**: 감사 로그
**주요 기능**:
- 중요 작업 기록
- 변경 전후 데이터
- IP, User-Agent
- 작업 타입, Entity 정보

---

## 13. 시스템 (1개)

### flyway_schema_history
**용도**: Flyway 마이그레이션 이력
**주요 기능**:
- 마이그레이션 버전 추적
- 실행 이력 관리
- 체크섬 검증

---

## 📊 테이블 통계

| 도메인 | 테이블 수 | 비율 |
|--------|-----------|------|
| 사용자 관리 | 7개 | 9.5% |
| **업체 관리** | **6개** | **8.1%** |
| 광고 시스템 | 8개 | 10.8% |
| 견적/매칭 | 8개 | 10.8% |
| 결제 시스템 | 8개 | 10.8% |
| UMS 알림 | 6개 | 8.1% |
| 파일 관리 | 4개 | 5.4% |
| 게시판 | 5개 | 6.8% |
| 어드민 시스템 | 11개 | 14.9% |
| 상담 시스템 | 3개 | 4.1% |
| **필터 관리** | **3개** | **4.1%** |
| 통계/로그 | 3개 | 4.1% |
| 시스템 | 1개 | 1.4% |
| **합계** | **74개** | **100%** |

---

## 🔗 테이블 간 주요 관계

### 사용자 중심
```
users (사용자)
├── 1:1 → user_profiles (프로필)
├── 1:1 → user_settings (설정)
├── 1:N → user_activity_logs (활동 로그)
├── 1:N → user_devices (디바이스)
├── 1:N → social_accounts (소셜 계정)
├── 1:1 → user_points (포인트)
├── 1:N → companies (소유 업체)
├── 1:N → estimate_requests (견적 요청)
├── 1:N → payments (결제)
└── 1:N → quick_consultations (상담 신청)
```

### 업체 중심
```
companies (업체)
├── 1:N → company_images (이미지)
├── 1:N → company_services (서비스)
├── 1:N → company_portfolios (포트폴리오)
├── 1:N → company_reviews (리뷰)
├── 1:N → company_certifications (인증서)
├── 1:N → ad_campaigns (광고)
├── 1:N → damoa_picks (다모아픽)
└── 1:N → estimate_proposals (견적 제안)
```

### 상담 중심
```
quick_consultations (빠른상담)
├── 1:N → consultation_messages (메시지)
├── N:1 → users (신청자)
└── N:1 → companies (배정 업체)

partnership_inquiries (제휴문의)
├── 1:N → consultation_messages (메시지)
├── N:1 → users (문의자)
└── N:1 → users (담당 관리자)
```

### 어드민 중심
```
users (관리자)
├── 1:N → admin_roles (역할)
├── 1:N → admin_sessions (세션)
├── 1:N → admin_login_history (로그인 기록)
└── 1:1 → admin_two_factor_auth (2FA)

admin_page_permissions (페이지)
└── 1:N → admin_role_page_access (역할 매핑)
```

---

## 🎯 공통 패턴

### 모든 테이블 공통
```sql
id BIGSERIAL PRIMARY KEY              -- 내부 참조용
uuid UUID UNIQUE NOT NULL             -- 외부 API용
created_at TIMESTAMP                  -- 생성 시간
updated_at TIMESTAMP                  -- 수정 시간 (트리거 자동)
is_deleted BOOLEAN DEFAULT FALSE      -- Soft Delete
deleted_at TIMESTAMP                  -- 삭제 시간
metadata JSONB DEFAULT '{}'           -- 확장 데이터
```

### 상태 관리 패턴
- **견적**: DRAFT → PUBLISHED → IN_PROGRESS → MATCHED → COMPLETED
- **상담**: SUBMITTED → ASSIGNED → IN_PROGRESS → RESPONDED → COMPLETED
- **제휴**: SUBMITTED → IN_REVIEW → RESPONDED → IN_NEGOTIATION → ACCEPTED/REJECTED
- **결제**: PENDING → COMPLETED / FAILED / CANCELLED / REFUNDED
- **광고**: DRAFT → ACTIVE → PAUSED → COMPLETED → CANCELLED

### JSONB 활용
- **metadata**: 예측 불가능한 확장 데이터
- **settings**: 구조화된 설정 데이터
- **type_data**: 타입별 특수 데이터
- **config**: 설정 정보

### 배열 활용
- **tags[]**: 태그, 키워드
- **categories[]**: 카테고리
- **service_areas[]**: 서비스 지역
- **images[]**: 이미지 URL 목록
- **permissions[]**: 권한 목록

---

## 📝 참고사항

### Redis vs PostgreSQL
- **Redis**: JWT 토큰, OTP, 세션, 캐시, 임시 데이터
- **PostgreSQL**: 영구 저장이 필요한 모든 비즈니스 데이터

### 인덱스 전략
- UUID, 상태, 날짜 컬럼에 인덱스
- JSONB는 GIN 인덱스
- 배열은 GIN 인덱스
- Full-text search 인덱스

### 성능 최적화
- 로그/통계 테이블 파티셔닝 고려
- 읽기 전용 복제본 활용
- 자주 조회되는 데이터 캐싱
- 집계 테이블 활용

---

## 🔄 마이그레이션 이력

| 버전 | 설명 | 테이블 수 | 날짜 |
|------|-----|-----------|------|
| V1 | Initial schema | 55개 | 2025-10-31 |
| V2 | Add company improvements | +4개 | 2025-10-31 |
| V3 | Add comprehensive comments | 0개 | 2025-10-31 |
| V4 | Add admin tables | +10개 | 2025-10-31 |
| V4.1 | Fix admin roles table | +1개 | 2025-10-31 |
| V4.2 | Fix admin active sessions view | 0개 | 2025-10-31 |
| V5 | Add consultation tables | +3개 | 2025-10-31 |
| V6 | Create filter management tables | +3개 | 2025-10-31 |
| V7 | Drop unused tables | -3개 | 2025-10-31 |
| **합계** | - | **73개** | - |

*flyway_schema_history 포함 시 74개*