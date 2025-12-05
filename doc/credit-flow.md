# 크레딧 결제 플로우 가이드

> 크레딧 충전, 사용, 환불에 대한 상세 플로우 문서

---

## 목차

1. [크레딧 충전 플로우](#1-크레딧-충전-플로우)
2. [크레딧 사용 플로우 - 광고 캠페인](#2-크레딧-사용-플로우---광고-캠페인)
3. [크레딧 사용 플로우 - 파일 다운로드](#3-크레딧-사용-플로우---파일-다운로드)
4. [크레딧 환불 플로우](#4-크레딧-환불-플로우)
5. [API 엔드포인트 정리](#5-api-엔드포인트-정리)
6. [충전 패키지 정책](#6-충전-패키지-정책)
7. [관리자 패키지 관리](#7-관리자-패키지-관리)

---

## 1. 크레딧 충전 플로우

### 1.1 전체 플로우 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           크레딧 충전 플로우                                   │
└─────────────────────────────────────────────────────────────────────────────┘

  [사용자]                    [서버]                      [PG사]
     │                          │                          │
     │  1. 패키지 목록 조회       │                          │
     │  GET /api/credits/packages                          │
     │─────────────────────────▶│                          │
     │                          │                          │
     │     패키지 목록 반환       │                          │
     │◀─────────────────────────│                          │
     │                          │                          │
     │  2. 충전 요청             │                          │
     │  POST /api/credits/purchase                         │
     │  { "packageCode": "KRW_30000", "quantity": 2 }     │
     │─────────────────────────▶│                          │
     │                          │                          │
     │                          │  Payment PENDING 생성    │
     │                          │  Redis 세션 저장 (30분)   │
     │                          │  금액/보너스 동적 계산    │
     │                          │                          │
     │                          │  3. PG 결제 준비 요청     │
     │                          │─────────────────────────▶│
     │                          │                          │
     │                          │     paymentUrl 반환      │
     │                          │◀─────────────────────────│
     │                          │                          │
     │     paymentUrl 반환       │                          │
     │◀─────────────────────────│                          │
     │                          │                          │
     │  4. 결제 페이지로 이동                                │
     │─────────────────────────────────────────────────────▶│
     │                          │                          │
     │     카드정보 입력/결제 승인                            │
     │◀─────────────────────────────────────────────────────│
     │                          │                          │
     │  5. 충전 완료 처리        │                          │
     │  POST /api/credits/purchase/complete                │
     │  { "orderId": "...", "pgToken": "..." }             │
     │─────────────────────────▶│                          │
     │                          │                          │
     │                          │  6. PG 결제 확정 요청     │
     │                          │─────────────────────────▶│
     │                          │                          │
     │                          │     결제 확정 완료        │
     │                          │◀─────────────────────────│
     │                          │                          │
     │                          │  Payment → COMPLETED     │
     │                          │  Credit 잔액 증가        │
     │                          │  Transaction 기록        │
     │                          │                          │
     │     충전 완료!            │                          │
     │◀─────────────────────────│                          │
     │                          │                          │
```

### 1.2 단계별 상세 설명

#### Step 1: 충전 패키지 조회

```http
GET /api/credits/packages
Authorization: Bearer {token}
```

**응답:**
```json
{
  "success": true,
  "data": [
    {
      "uuid": "...",
      "code": "KRW_10000",
      "displayName": "1만원권",
      "unitAmount": 10000,
      "bonusRate": 0,
      "maxBonus": null,
      "bonusEligible": false,
      "description": "1만원 단위 충전권 (보너스 없음)"
    },
    {
      "uuid": "...",
      "code": "KRW_30000",
      "displayName": "3만원권",
      "unitAmount": 30000,
      "bonusRate": 5,
      "maxBonus": null,
      "bonusEligible": true,
      "description": "3만원 단위 충전권 (5% 보너스)"
    },
    {
      "uuid": "...",
      "code": "KRW_50000",
      "displayName": "5만원권",
      "unitAmount": 50000,
      "bonusRate": 7,
      "maxBonus": null,
      "bonusEligible": true,
      "description": "5만원 단위 충전권 (7% 보너스)"
    },
    {
      "uuid": "...",
      "code": "KRW_100000",
      "displayName": "10만원권",
      "unitAmount": 100000,
      "bonusRate": 10,
      "maxBonus": 50000,
      "bonusEligible": true,
      "description": "10만원 단위 충전권 (10% 보너스, 최대 5만원)"
    }
  ]
}
```

#### Step 2: 충전 요청

사용자가 패키지와 수량을 선택하여 충전 요청:

```http
POST /api/credits/purchase
Authorization: Bearer {token}
Content-Type: application/json

{
  "packageCode": "KRW_30000",
  "quantity": 2,
  "paymentMethod": "TOSS"
}
```

**서버 처리:**
1. 패키지 검증 (DB에서 조회)
2. 수량 × 단위금액으로 결제금액 계산
3. 보너스 동적 계산 (bonusRate 적용)
4. Payment 레코드 생성 (상태: PENDING)
5. Redis에 결제 세션 저장 (TTL: 30분)
6. PG사에 결제 준비 요청

**계산 예시 (3만원권 2개):**
- 결제금액: 30,000 × 2 = 60,000원
- 기본 크레딧: 60,000
- 보너스 (5%): 60,000 × 0.05 = 3,000
- 총 크레딧: 63,000

**응답:**
```json
{
  "success": true,
  "data": {
    "paymentUuid": "550e8400-e29b-41d4-a716-446655440000",
    "orderId": "CR_123_KRW_30000_X2_1701676800000",
    "paymentUrl": "https://pay.toss.im/...",
    "paymentAmount": 60000,
    "totalCredits": 63000,
    "bonusCredits": 3000,
    "packageDisplayName": "3만원권 x 2"
  }
}
```

#### Step 3-4: PG 결제 진행

사용자가 `paymentUrl`로 이동하여 결제 진행:
- 카드 정보 입력
- 결제 승인
- 성공 시 `successUrl`로 리다이렉트 (pgToken 포함)

#### Step 5-6: 충전 완료 처리

```http
POST /api/credits/purchase/complete
Authorization: Bearer {token}
Content-Type: application/json

{
  "orderId": "CR_123_KRW_30000_X2_1701676800000",
  "pgToken": "pg_token_from_redirect"
}
```

**서버 처리:**
1. Redis 세션 검증 (세션에 저장된 totalCredits 사용)
2. PG 결제 확정 (confirm)
3. Payment 상태 → COMPLETED
4. Credit 잔액 증가 (+63,000)
5. CreditTransaction 기록 생성

**응답:**
```json
{
  "success": true,
  "data": {
    "message": "크레딧 충전이 완료되었습니다",
    "chargedCredits": 63000,
    "newBalance": 63000
  }
}
```

---

## 2. 크레딧 사용 플로우 - 광고 캠페인

### 2.1 광고 캠페인 생성 플로우

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        광고 캠페인 생성 플로우                                 │
└─────────────────────────────────────────────────────────────────────────────┘

  [업체 사용자]                        [서버]
       │                                │
       │  1. 캠페인 생성 요청             │
       │  POST /api/ad-campaigns        │
       │  {                             │
       │    "companyUuid": "...",       │
       │    "durationDays": 7,          │
       │    "paymentAmount": 3500       │  ← 최소 500원/일 × 7일
       │  }                             │
       │─────────────────────────────▶ │
       │                                │
       │                                │  [검증]
       │                                │  - 업체 소유자 확인
       │                                │  - 기존 활성 캠페인 확인
       │                                │  - 최소 금액 검증 (500원/일)
       │                                │  - 크레딧 잔액 확인
       │                                │
       │                                │  [처리]
       │                                │  - 크레딧 차감 (-3,500)
       │                                │  - AdCampaign 생성 (ACTIVE)
       │                                │  - AdPayment 생성
       │                                │  - 우선순위 점수 계산
       │                                │
       │     캠페인 생성 완료             │
       │◀─────────────────────────────  │
       │                                │
```

### 2.2 광고 우선순위 계산

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          1일 가치(dailyValue) 기반 우선순위                    │
└─────────────────────────────────────────────────────────────────────────────┘

계산 공식:
  dailyValue = 결제금액 / 적용일수
  priorityScore = Σ(활성 결제들의 dailyValue)

예시 1: 기본 캠페인
  - 7일에 700원 충전
  - dailyValue = 700 / 7 = 100원/일
  - priorityScore = 100

예시 2: 추가 충전
  - 1일차: 7일에 700원 → dailyValue = 100원
  - 2일차: 600원 추가 (남은 6일) → 추가 dailyValue = 100원
  - 총 priorityScore = 200원

우선순위 비교:
  ┌─────────┬─────────┬─────────┬─────────────┬──────────┐
  │  업체   │  기간   │  금액   │ 1일 가치    │ 우선순위  │
  ├─────────┼─────────┼─────────┼─────────────┼──────────┤
  │   A     │  7일    │  700원  │  100원/일   │   3위    │
  │   B     │  7일    │ 1,400원 │  200원/일   │   1위    │
  │   C     │  30일   │ 3,000원 │  100원/일   │   3위    │
  │   D     │  7일    │ 1,300원 │  200원/일   │   1위    │
  └─────────┴─────────┴─────────┴─────────────┴──────────┘

  * D는 700원(7일) + 600원(6일 추가) = 100 + 100 = 200원/일
```

---

## 3. 크레딧 사용 플로우 - 파일 다운로드

### 3.1 유료 파일 다운로드 플로우

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         유료 파일 다운로드 플로우                              │
└─────────────────────────────────────────────────────────────────────────────┘

  [사용자]                             [서버]
       │                                │
       │  1. 구매 여부 확인              │
       │  GET /api/files/{uuid}/purchase-status
       │─────────────────────────────▶ │
       │                                │
       │     { "purchased": false,      │
       │       "price": 5000 }          │
       │◀─────────────────────────────  │
       │                                │
       │  2. 파일 다운로드 요청          │
       │  POST /api/files/{uuid}/download
       │─────────────────────────────▶ │
       │                                │
       │                                │  [검증]
       │                                │  - 파일 존재 확인
       │                                │  - 유료 파일인지 확인
       │                                │  - 기존 구매 이력 확인
       │                                │
       │                                │  [미구매 시]
       │                                │  - 크레딧 잔액 확인
       │                                │  - 크레딧 차감 (-5,000)
       │                                │  - FileDownload 기록
       │                                │  - CreditTransaction 기록
       │                                │
       │                                │  [이미 구매한 경우]
       │                                │  - 무료 재다운로드
       │                                │
       │                                │  [Presigned URL 생성]
       │                                │  - S3 다운로드 URL (15분 유효)
       │                                │
       │     다운로드 URL 반환           │
       │◀─────────────────────────────  │
       │                                │
       │  3. 파일 다운로드              │
       │─────────────────────────────▶ [S3]
       │                                │
```

---

## 4. 크레딧 환불 플로우

### 4.1 환불 정책

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              환불 정책                                       │
└─────────────────────────────────────────────────────────────────────────────┘

1. 환불 수수료: 10% (100원 단위 올림)
   - 10,000원 환불 → 수수료 1,000원 → 실환불 9,000원
   - 15,000원 환불 → 수수료 1,500원 → 실환불 13,500원

2. 최소 환불 금액: 1,000원

3. 환불 조건:
   - 미사용 크레딧만 환불 가능
   - 보너스 크레딧은 환불 불가
   - 이미 사용한 크레딧은 환불 불가

4. 처리 기간: 영업일 기준 3~5일
```

---

## 5. API 엔드포인트 정리

### 5.1 크레딧 관리 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/credits/balance` | 잔액 조회 |
| GET | `/api/credits/packages` | 충전 패키지 목록 (4개) |
| GET | `/api/credits/transactions` | 거래 내역 조회 |
| POST | `/api/credits/purchase` | 충전 요청 |
| POST | `/api/credits/purchase/complete` | 충전 완료 |
| POST | `/api/credits/refund` | 환불 요청 |

### 5.2 광고 캠페인 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/ad-campaigns` | 캠페인 생성 |
| GET | `/api/ad-campaigns/my` | 내 활성 캠페인 |
| GET | `/api/ad-campaigns/history` | 캠페인 이력 |
| GET | `/api/ad-campaigns/{uuid}` | 캠페인 상세 |
| POST | `/api/ad-campaigns/{uuid}/payments` | 추가 결제 |
| DELETE | `/api/ad-campaigns/{uuid}` | 캠페인 취소 |

### 5.3 파일 다운로드 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/files/{uuid}/purchase-status` | 구매 여부 확인 |
| POST | `/api/files/{uuid}/download` | 파일 다운로드 (구매) |
| GET | `/api/files/my-purchases` | 내가 구매한 파일 |

---

## 6. 충전 패키지 정책

### 6.1 패키지 구조

패키지는 **4개**만 존재하며, 수량은 사용자가 충전 시 직접 지정합니다.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           패키지 구조 (단위 금액 기반)                          │
└─────────────────────────────────────────────────────────────────────────────┘

패키지 4개:
  ┌─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
  │  패키지코드  │  표시명     │  단위금액   │  보너스율   │  최대보너스  │
  ├─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
  │ KRW_10000   │  1만원권    │  10,000원   │    0%       │     -       │
  │ KRW_30000   │  3만원권    │  30,000원   │    5%       │     -       │
  │ KRW_50000   │  5만원권    │  50,000원   │    7%       │     -       │
  │ KRW_100000  │ 10만원권    │ 100,000원   │   10%       │  50,000원   │
  └─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘

수량은 사용자가 직접 지정 (1개 ~ 무제한)

예: "3만원권 10개 충전" 요청
  - packageCode: "KRW_30000"
  - quantity: 10
```

### 6.2 동적 금액 계산

충전 시 수량에 따라 금액과 보너스가 동적으로 계산됩니다:

```
계산 공식:
  결제금액 = 단위금액 × 수량
  기본크레딧 = 결제금액
  보너스크레딧 = 기본크레딧 × (보너스율 / 100)

  maxBonus가 설정된 경우:
    보너스크레딧 = min(보너스크레딧, maxBonus)

  총크레딧 = 기본크레딧 + 보너스크레딧
```

**계산 예시:**

| 패키지 | 수량 | 결제금액 | 기본크레딧 | 보너스율 | 보너스 | 총 크레딧 |
|--------|------|----------|-----------|----------|--------|-----------|
| 1만원권 | 1 | 10,000 | 10,000 | 0% | 0 | 10,000 |
| 1만원권 | 10 | 100,000 | 100,000 | 0% | 0 | 100,000 |
| 3만원권 | 1 | 30,000 | 30,000 | 5% | 1,500 | 31,500 |
| 3만원권 | 5 | 150,000 | 150,000 | 5% | 7,500 | 157,500 |
| 5만원권 | 2 | 100,000 | 100,000 | 7% | 7,000 | 107,000 |
| 10만원권 | 1 | 100,000 | 100,000 | 10% | 10,000 | 110,000 |
| 10만원권 | 10 | 1,000,000 | 1,000,000 | 10% | **50,000** | 1,050,000 |

* 10만원권 10개: 보너스 100,000원이지만 maxBonus(50,000)로 제한됨

### 6.3 보너스 규칙

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              보너스 규칙                                     │
└─────────────────────────────────────────────────────────────────────────────┘

1. 1만원권: 보너스 없음 (0%)
   - 3만원 미만은 보너스 적용 불가

2. 3만원권: 5%
   - 예: 3만원 × 10개 = 300,000원 → 보너스 15,000원

3. 5만원권: 7%
   - 예: 5만원 × 5개 = 250,000원 → 보너스 17,500원

4. 10만원권: 10% (최대 50,000원)
   - 예: 10만원 × 3개 = 300,000원 → 보너스 30,000원
   - 예: 10만원 × 10개 = 1,000,000원 → 보너스 50,000원 (최대)
```

---

## 7. 관리자 패키지 관리

### 7.1 관리자 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/credit-packages` | 패키지 목록 조회 |
| GET | `/api/admin/credit-packages/{uuid}` | 패키지 상세 조회 |
| GET | `/api/admin/credit-packages/active` | 활성 패키지 목록 |
| GET | `/api/admin/credit-packages/unit-amounts` | 허용 단위 금액 목록 |
| POST | `/api/admin/credit-packages` | 패키지 생성 |
| PUT | `/api/admin/credit-packages/{uuid}` | 패키지 수정 |
| PATCH | `/api/admin/credit-packages/{uuid}/toggle-active` | 활성화 토글 |
| PATCH | `/api/admin/credit-packages/{uuid}/display-order` | 순서 변경 |
| PATCH | `/api/admin/credit-packages/bonus-rate` | 단위금액별 보너스율 변경 |
| DELETE | `/api/admin/credit-packages/{uuid}` | 패키지 삭제 |

### 7.2 패키지 수정 (보너스율 변경)

```http
PUT /api/admin/credit-packages/{uuid}
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "displayName": "3만원권",
  "bonusRate": 6.00,
  "maxBonus": null,
  "description": "3만원 단위 충전권 (6% 보너스)"
}
```

### 7.3 관리 포인트

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        관리자 패키지 관리 포인트                               │
└─────────────────────────────────────────────────────────────────────────────┘

1. 패키지는 기본 4개 (마이그레이션으로 생성됨)
   - KRW_10000, KRW_30000, KRW_50000, KRW_100000

2. 관리자가 변경 가능한 항목:
   - 보너스율 (bonusRate)
   - 최대 보너스 한도 (maxBonus)
   - 설명 (description)
   - 표시 이름 (displayName)
   - 활성화 여부 (isActive)
   - 표시 순서 (displayOrder)

3. 변경 불가 항목:
   - 패키지 코드 (code)
   - 단위 금액 (unitAmount)

4. 비활성화된 패키지:
   - 사용자에게 노출되지 않음
   - 해당 패키지로 충전 불가

* 수량은 패키지에 포함되지 않음 - 사용자가 충전 시 직접 지정
```

---

## 관련 파일

### 크레딧 관련
- **엔티티**: `domain/payment/model/Credit.java`, `CreditTransaction.java`, `CreditPackage.java`
- **Repository**: `domain/payment/repository/CreditRepository.java`, `CreditPackageRepository.java`
- **서비스**: `domain/payment/service/CreditService.java`, `AdminCreditPackageService.java`
- **컨트롤러**: `domain/payment/web/CreditController.java`, `AdminCreditPackageController.java`

### 광고 관련
- **엔티티**: `domain/ad/model/AdCampaign.java`, `AdPayment.java`
- **서비스**: `domain/ad/service/AdCampaignService.java`
- **컨트롤러**: `domain/ad/web/AdCampaignController.java`

### 파일 다운로드 관련
- **서비스**: `domain/file/service/FileDownloadService.java`, `AdminFilePricingService.java`
- **컨트롤러**: `domain/file/web/FileController.java`, `AdminFileController.java`

### DB 마이그레이션
- `db/migration/V56__Credit_and_ad_payment_system.sql`
- `db/migration/V57__Rename_value30d_to_daily_value.sql`
- `db/migration/V58__Create_credit_packages_table.sql`

---

*마지막 업데이트: 2024-12-04*
