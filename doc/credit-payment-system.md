# 크레딧 기반 결제 시스템 구현 문서

## 개요

크레딧 충전형 결제 시스템을 구현하여 자료실 파일 다운로드 및 업체 광고에 사용할 수 있도록 합니다.

- **작업일**: 2024년 12월
- **Flyway 마이그레이션**: V56
- **상태**: 구현 완료 (테스트 필요)

---

## 1. 생성/수정된 파일 목록

### 1.1 Phase 1: 기반 작업

| 파일 경로 | 작업 | 설명 |
|-----------|------|------|
| `domain/payment/model/CreditPackage.java` | 신규 | 충전 패키지 Enum (16개) |
| `domain/payment/repository/CreditRepository.java` | 신규 | 비관적 락 지원 |
| `domain/payment/repository/CreditTransactionRepository.java` | 신규 | 거래 내역 조회 |
| `domain/payment/repository/PaymentRepository.java` | 신규 | 결제 조회 |
| `resources/db/migration/V56__Credit_and_ad_payment_system.sql` | 신규 | DB 마이그레이션 |
| `core/exception/ErrorCode.java` | 수정 | Credit/Ad 에러 코드 추가 |

### 1.2 Phase 2: 크레딧 시스템

| 파일 경로 | 작업 | 설명 |
|-----------|------|------|
| `domain/payment/service/CreditService.java` | 신규 | 충전/사용/환불 비즈니스 로직 |
| `domain/payment/web/CreditController.java` | 신규 | REST API 컨트롤러 |
| `domain/payment/web/dto/CreditPurchaseRequest.java` | 수정 | packageCode 기반으로 변경 |
| `domain/payment/web/dto/CreditPurchaseResponse.java` | 신규 | 충전 응답 DTO |
| `domain/payment/web/dto/CreditPackageResponse.java` | 신규 | 패키지 목록 응답 DTO |
| `domain/payment/web/dto/CreditRefundRequest.java` | 신규 | 환불 요청 DTO |
| `domain/payment/web/dto/CreditRefundResponse.java` | 신규 | 환불 응답 DTO |
| `domain/payment/web/dto/CreditTransactionResponse.java` | 신규 | 거래 내역 응답 DTO |
| `domain/payment/web/dto/CreditBalanceResponse.java` | 기존 | 잔액 조회 응답 |

### 1.3 Phase 3: 광고 시스템

| 파일 경로 | 작업 | 설명 |
|-----------|------|------|
| `domain/ad/model/AdPayment.java` | 신규 | 광고 결제 엔티티 |
| `domain/ad/model/AdCampaign.java` | 수정 | 신규 필드 추가 |
| `domain/ad/repository/AdCampaignRepository.java` | 신규 | 캠페인 조회 |
| `domain/ad/repository/AdPaymentRepository.java` | 신규 | 결제 조회 |
| `domain/ad/service/AdCampaignService.java` | 신규 | 캠페인 비즈니스 로직 |
| `infra/scheduler/AdCampaignScheduler.java` | 신규 | 일일 스케줄러 |
| `domain/ad/web/AdCampaignController.java` | 신규 | REST API 컨트롤러 |
| `domain/ad/web/dto/AdCampaignCreateRequest.java` | 신규 | 캠페인 생성 요청 |
| `domain/ad/web/dto/AdPaymentRequest.java` | 신규 | 추가 결제 요청 |
| `domain/ad/web/dto/AdExtendRequest.java` | 신규 | 연장 요청 |
| `domain/ad/web/dto/AdCampaignResponse.java` | 신규 | 캠페인 응답 |
| `domain/ad/web/dto/AdPaymentResponse.java` | 신규 | 결제 응답 |
| `domain/ad/web/dto/AdCampaignRankingResponse.java` | 신규 | 순위 응답 |

### 1.4 Phase 4: 파일 다운로드

| 파일 경로 | 작업 | 설명 |
|-----------|------|------|
| `domain/file/service/FileDownloadService.java` | 수정 | 크레딧 연동 유료 다운로드 |
| `domain/file/service/AdminFilePricingService.java` | 신규 | 관리자 가격 설정 |
| `domain/file/repository/FileDownloadRepository.java` | 수정 | 구매 확인 쿼리 추가 |
| `domain/file/repository/FilePricingRepository.java` | 수정 | 페이징 쿼리 추가 |
| `domain/file/web/dto/FileDownloadResponse.java` | 신규 | 다운로드 응답 |
| `domain/file/web/dto/FilePurchaseStatusResponse.java` | 신규 | 구매 상태 응답 |
| `domain/file/web/dto/PurchasedFileResponse.java` | 신규 | 구매 파일 목록 응답 |
| `domain/file/web/dto/FilePricingRequest.java` | 신규 | 가격 설정 요청 |
| `domain/file/web/dto/FilePricingResponse.java` | 신규 | 가격 정보 응답 |

---

## 2. API 엔드포인트

### 2.1 크레딧 API (`/api/credits`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/balance` | 크레딧 잔액 조회 | 필수 |
| GET | `/packages` | 충전 패키지 목록 | 필수 |
| GET | `/transactions` | 거래 내역 조회 (페이징) | 필수 |
| POST | `/purchase` | 크레딧 충전 요청 | 필수 |
| POST | `/purchase/complete` | 충전 완료 콜백 | 필수 |
| POST | `/refund` | 환불 요청 | 필수 |

### 2.2 광고 캠페인 API (`/api/ad-campaigns`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/` | 캠페인 생성 | 필수 |
| POST | `/{uuid}/payments` | 추가 결제 | 필수 |
| POST | `/{uuid}/extend` | 기간 연장 | 필수 |
| GET | `/my` | 내 활성 캠페인 조회 | 필수 |
| GET | `/my/history` | 캠페인 이력 조회 | 필수 |
| GET | `/{uuid}` | 캠페인 상세 조회 | 필수 |
| GET | `/ranking` | 캠페인 순위 조회 | 필수 |
| DELETE | `/{uuid}` | 캠페인 취소 | 필수 |

### 2.3 파일 다운로드 API (FileController에 추가 필요)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/files/{uuid}/download` | 파일 다운로드 (크레딧 차감) | 필수 |
| GET | `/api/files/{uuid}/purchase-status` | 구매 상태 확인 | 필수 |
| GET | `/api/files/my-purchases` | 내가 구매한 파일 목록 | 필수 |

### 2.4 관리자 파일 가격 API (AdminController에 추가 필요)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/admin/files/{uuid}/pricing` | 파일 가격 설정 | 관리자 |
| GET | `/api/admin/files/{uuid}/pricing` | 파일 가격 조회 | 관리자 |
| GET | `/api/admin/files/paid` | 유료 파일 목록 | 관리자 |
| DELETE | `/api/admin/files/{uuid}/pricing` | 가격 비활성화 | 관리자 |

---

## 3. 충전 패키지 정책

### 3.1 패키지 목록

| 패키지 코드 | 결제금액 | 보너스 | 총 크레딧 |
|-------------|----------|--------|-----------|
| PACK_10000_X1 | 10,000원 | 0% | 10,000 |
| PACK_10000_X2 | 20,000원 | 0% | 20,000 |
| PACK_10000_X3 | 30,000원 | 0% | 30,000 |
| PACK_10000_X4 | 40,000원 | 0% | 40,000 |
| PACK_30000_X1 | 30,000원 | 5% (1,500) | 31,500 |
| PACK_30000_X2 | 60,000원 | 6% (3,600) | 63,600 |
| PACK_30000_X3 | 90,000원 | 7% (6,300) | 96,300 |
| PACK_30000_X4 | 120,000원 | 8% (9,600) | 129,600 |
| PACK_50000_X1 | 50,000원 | 6% (3,000) | 53,000 |
| PACK_50000_X2 | 100,000원 | 7% (7,000) | 107,000 |
| PACK_50000_X3 | 150,000원 | 8% (12,000) | 162,000 |
| PACK_50000_X4 | 200,000원 | 10% (20,000) | 220,000 |
| PACK_100000_X1 | 100,000원 | 10% (10,000) | 110,000 |
| PACK_100000_X2 | 200,000원 | 10% (20,000) | 220,000 |
| PACK_100000_X3 | 300,000원 | 10% (30,000) | 330,000 |
| PACK_100000_X4 | 400,000원 | 10% (40,000, 최대) | 440,000 |

### 3.2 환불 정책

- **환불 수수료**: 10% (100원 단위 올림)
- **최소 환불 금액**: 1,000원
- **환불 조건**: 미사용 크레딧만 환불 가능
- **처리 기간**: 영업일 기준 3~5일

---

## 4. 광고 시스템 정책

### 4.1 광고 기간 옵션

| 기간 | 최소 결제금액 | 일당 최소 |
|------|---------------|-----------|
| 7일 | 3,500원 | 500원/일 |
| 14일 | 7,000원 | 500원/일 |
| 30일 | 15,000원 | 500원/일 |

### 4.2 우선순위 계산

```
priority_score = (총 결제금액 / 남은 일수) × 30
```

**예시**:
- A업체: 7일간 3,500원 → 500/7×30 = 2,142점
- B업체: 7일간 7,000원 → 1000/7×30 = 4,285점
- B업체가 상위 노출

**동점 시 secondary_score로 정렬**:
- 평점 × 1000 + 리뷰수 × 10 + 포트폴리오수 × 5 + 선등록 보너스

### 4.3 스케줄러 작업

| 시간 | 작업 |
|------|------|
| 매일 00:00 | 만료된 캠페인 완료 처리, 만료된 결제 소진 처리 |
| 매일 00:05 | 모든 활성 캠페인 우선순위 재계산 |
| 시작 후 1분 | 초기 처리 (누적 만료 처리) |

---

## 5. 남은 작업 (TODO)

### 5.1 컨트롤러 연동 (필수)

- [ ] `FileController`에 파일 다운로드/구매 상태 API 추가
- [ ] `AdminController`에 파일 가격 설정 API 추가
- [ ] Company 목록 조회 시 광고 우선순위 정렬 적용

### 5.2 Company 정렬 수정

`CompanyRepository` 또는 `CompanyService`에서 업체 목록 조회 시 광고 캠페인 우선순위 반영:

```java
// CompanyRepository에 추가 필요
@Query("""
    SELECT c FROM Company c
    LEFT JOIN AdCampaign ac ON ac.company = c
        AND ac.status = 'ACTIVE'
        AND ac.adType = 'LISTING'
        AND (ac.endDate IS NULL OR ac.endDate >= CURRENT_DATE)
    WHERE c.isDeleted = false AND c.status = 'ACTIVE'
    ORDER BY
        CASE WHEN ac.id IS NOT NULL THEN 0 ELSE 1 END,
        COALESCE(ac.priorityScore, 0) DESC,
        COALESCE(ac.secondaryScore, 0) DESC,
        c.createdAt DESC
    """)
Page<Company> findAllWithAdPriority(Pageable pageable);
```

### 5.3 테스트 작성

- [ ] CreditService 단위 테스트
- [ ] AdCampaignService 단위 테스트
- [ ] FileDownloadService 유료 다운로드 테스트
- [ ] 우선순위 계산 로직 테스트
- [ ] API 통합 테스트

### 5.4 프론트엔드 연동

- [ ] 크레딧 충전 페이지
- [ ] 크레딧 잔액/내역 표시
- [ ] 광고 캠페인 생성/관리 페이지
- [ ] 파일 다운로드 시 크레딧 차감 확인 UI
- [ ] 구매한 파일 목록 페이지

### 5.5 PG 연동 확인

- [ ] TossPayments 결제 플로우 테스트
- [ ] KakaoPay 결제 플로우 테스트
- [ ] 결제 완료 콜백 처리 확인

### 5.6 보안 검토

- [ ] 크레딧 동시성 처리 (비관적 락) 확인
- [ ] 결제 세션 Redis TTL 확인
- [ ] 환불 요청 검증 로직 확인

---

## 6. DB 스키마 변경 사항 (V56)

### 6.1 credits 테이블

```sql
-- 추가된 컬럼
available_credits NUMERIC(12,2)  -- 사용 가능 크레딧
expiring_credits NUMERIC(12,2)   -- 곧 만료되는 크레딧
expiring_at TIMESTAMP            -- 만료 예정일
total_spent NUMERIC(12,2)        -- 총 사용 크레딧
is_deleted, deleted_at, metadata -- BaseEntity 필드
```

### 6.2 credit_transactions 테이블

```sql
-- 추가된 컬럼
entity_type VARCHAR(50)  -- PAYMENT, FILE_DOWNLOAD, AD_CAMPAIGN, REFUND
entity_id BIGINT         -- 관련 엔티티 ID
updated_at, is_deleted, deleted_at, metadata
```

### 6.3 ad_payments 테이블 (신규)

```sql
CREATE TABLE ad_payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id),
    payment_amount NUMERIC(12,2) NOT NULL,
    payment_date DATE NOT NULL,
    apply_from_date DATE NOT NULL,
    apply_to_date DATE NOT NULL,
    apply_days INTEGER NOT NULL,
    daily_rate NUMERIC(12,4),
    value_30d NUMERIC(12,2),
    payment_type VARCHAR(20) NOT NULL,  -- INITIAL, ADDITIONAL
    status VARCHAR(20) NOT NULL,        -- ACTIVE, CONSUMED, REFUNDED
    credit_transaction_id BIGINT REFERENCES credit_transactions(id),
    created_at, updated_at, is_deleted, deleted_at, metadata
);
```

### 6.4 ad_campaigns 테이블

```sql
-- 추가된 컬럼
duration_days INTEGER DEFAULT 7         -- 광고 기간 (7, 14, 30일)
min_daily_amount NUMERIC(12,2) DEFAULT 500  -- 최소 일당 금액
auto_renew BOOLEAN DEFAULT FALSE        -- 자동 갱신 여부
last_calculated_at TIMESTAMP            -- 마지막 우선순위 계산 시간
```

---

## 7. 참고 사항

### 7.1 트랜잭션 타입 상수

```java
public static final String TX_EARN = "EARN";      // 적립
public static final String TX_SPEND = "SPEND";    // 사용
public static final String TX_EXPIRE = "EXPIRE";  // 만료
public static final String TX_REFUND = "REFUND";  // 환불
```

### 7.2 엔티티 타입 상수

```java
public static final String ENTITY_PAYMENT = "PAYMENT";
public static final String ENTITY_FILE_DOWNLOAD = "FILE_DOWNLOAD";
public static final String ENTITY_AD_CAMPAIGN = "AD_CAMPAIGN";
public static final String ENTITY_REFUND = "REFUND";
```

### 7.3 광고 캠페인 상태

- `DRAFT`: 초안
- `ACTIVE`: 활성
- `PAUSED`: 일시정지
- `COMPLETED`: 완료 (만료)
- `CANCELLED`: 취소

### 7.4 광고 결제 상태

- `ACTIVE`: 활성 (적용 중)
- `CONSUMED`: 소진 (기간 만료)
- `REFUNDED`: 환불됨

---

## 8. 문의

구현 관련 질문이나 이슈는 개발팀에 문의하세요.
