# 파일 유료 다운로드 전체 플로우

> 자료실 파일 업로드부터 유료 다운로드까지의 전체 프로세스

---

## 목차

1. [파일 업로드 (Presigned URL 방식)](#1-파일-업로드-presigned-url-방식)
2. [자료실 글 작성 (유료 설정)](#2-자료실-글-작성-유료-설정)
3. [다른 사용자가 파일 조회](#3-다른-사용자가-파일-조회)
4. [다운로드 전 구매 상태 확인](#4-다운로드-전-구매-상태-확인-선택)
5. [파일 다운로드 (크레딧 자동 차감)](#5-파일-다운로드-크레딧-자동-차감)
6. [구매 내역 조회](#6-구매-내역-조회)
7. [DB 테이블 관계](#7-db-테이블-관계)
8. [관리자 기능](#8-관리자-기능)

---

## 1. 파일 업로드 (Presigned URL 방식)

### Step 1: Presigned URL 요청

```http
POST /api/files/presigned
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "filename": "설계도면.pdf",
  "mimeType": "application/pdf",
  "fileSize": 1024000,
  "entityType": "BOARD_DOCUMENT",
  "entityId": null
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "presignedUrl": "https://s3.ap-northeast-2.amazonaws.com/bucket/...",
    "uploadId": "upload-uuid-123",
    "fileKey": "documents/2025/12/file-uuid.pdf",
    "expiresIn": 3600
  }
}
```

### Step 2: S3 직접 업로드

```http
PUT {presignedUrl}
Content-Type: application/pdf

[파일 바이너리 데이터]
```

> **장점**: 파일이 백엔드 서버를 거치지 않아 빠르고 서버 부하가 적음

### Step 3: 업로드 완료 알림

```http
POST /api/files/complete
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "uploadId": "upload-uuid-123",
  "fileKey": "documents/2025/12/file-uuid.pdf"
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "uuid": "file-uuid-456",
    "fileUrl": "https://cdn.example.com/documents/...",
    "originalFilename": "설계도면.pdf",
    "fileSize": 1024000,
    "mimeType": "application/pdf"
  }
}
```

> ⚠️ **중요**: 응답의 `uuid`를 저장해두세요. 자료실 글 작성 시 필요합니다.

---

## 2. 자료실 글 작성 (유료 설정)

### 방식 A: 파일별 개별 가격 설정 (권장)

```http
POST /api/boards/document
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "50평 치과 인테리어 설계도면",
  "content": "대기실, 진료실, 상담실 포함된 설계도면입니다.",
  "files": [
    {"uuid": "file-uuid-456", "isPaid": true, "price": 5000},
    {"uuid": "file-uuid-789", "isPaid": true, "price": 3000},
    {"uuid": "file-uuid-abc", "isPaid": false}
  ],
  "thumbnailUuid": "thumbnail-uuid-123",
  "filterOptionIds": [1, 5, 12],
  "tags": ["치과", "50평", "모던"],
  "isPublished": true,
  "isPrivate": false
}
```

### 방식 B: 일괄 가격 설정 (레거시)

```http
POST /api/boards/document
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "50평 치과 인테리어 설계도면",
  "content": "대기실, 진료실, 상담실 포함된 설계도면입니다.",
  "fileUuids": [
    "file-uuid-456",
    "file-uuid-789"
  ],
  "thumbnailUuid": "thumbnail-uuid-123",
  "isPaid": true,
  "price": 5000,
  "filterOptionIds": [1, 5, 12],
  "tags": ["치과", "50평", "모던"],
  "isPublished": true,
  "isPrivate": false
}
```

### 서버 내부 처리 (DocumentBoardService)

```java
// 1. Board 생성
Board board = boardService.createBoard(...);

// 2. BoardAttachment 생성 (파일 연결)
processDocumentFiles(board, fileUuids, thumbnailUuid);

// 3. FilePricing 생성 (각 파일에 가격 설정)
processFilePricing(fileUuids, isPaid, price, userId);
```

**FilePricing 레코드 생성:**
```sql
INSERT INTO file_pricing (file_id, is_paid, price, is_active, created_by)
VALUES (123, true, 5000, true, 1);
```

**응답:**
```json
{
  "success": true,
  "data": {
    "uuid": "board-uuid-abc",
    "title": "50평 치과 인테리어 설계도면",
    "isPaid": true,
    "price": 5000,
    "files": [
      {
        "uuid": "file-uuid-456",
        "fileName": "설계도면.pdf",
        "fileSize": 1024000
      }
    ]
  }
}
```

---

## 3. 다른 사용자가 파일 조회

```http
GET /api/boards/document/{boardUuid}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "uuid": "board-uuid-abc",
    "title": "50평 치과 인테리어 설계도면",
    "content": "대기실, 진료실, 상담실 포함된 설계도면입니다.",
    "isPaid": true,
    "price": 5000,
    "viewCount": 150,
    "downloadCount": 23,
    "files": [
      {
        "uuid": "file-uuid-456",
        "fileName": "설계도면.pdf",
        "fileSize": 1024000,
        "mimeType": "application/pdf"
      }
    ],
    "author": {
      "name": "홍길동",
      "profileImage": "https://..."
    },
    "isBookmarked": false,
    "hasDownloaded": false
  }
}
```

---

## 4. 다운로드 전 구매 상태 확인 (선택)

> 다운로드 버튼 클릭 전 미리 구매 필요 여부를 확인할 수 있습니다.

```http
GET /api/files/{fileUuid}/purchase-status
Authorization: Bearer {accessToken}
```

**응답 (미구매 상태):**
```json
{
  "success": true,
  "data": {
    "fileUuid": "file-uuid-456",
    "isPaid": true,
    "price": 5000,
    "hasPurchased": false,
    "canDownload": true
  }
}
```

**응답 필드 설명:**

| 필드 | 설명 |
|------|------|
| `isPaid` | 유료 파일 여부 |
| `price` | 가격 (크레딧) |
| `hasPurchased` | 이미 구매했는지 여부 |
| `canDownload` | 다운로드 가능 여부 (구매함 또는 크레딧 충분) |

---

## 5. 파일 다운로드 (크레딧 자동 차감)

```http
POST /api/files/{fileUuid}/download
Authorization: Bearer {accessToken}
```

### 서버 내부 처리 (FileDownloadService)

```
┌─────────────────────────────────────────────────────────────┐
│  1. 파일 조회 (File)                                         │
│  2. 사용자 조회 (User)                                       │
│  3. 가격 정보 조회 (FilePricing)                              │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Case A: 무료 파일 (pricing == null || price == 0)       │ │
│  │  → FileDownload 기록 (is_free = true)                  │ │
│  │  → Presigned URL 반환                                  │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Case B: 유료 파일 - 이미 구매함                           │ │
│  │  → FileDownload 이력 확인 (is_free = false 존재)        │ │
│  │  → 무료 재다운로드 (FileDownload 기록, is_free = true)   │ │
│  │  → Presigned URL 반환                                  │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Case C: 유료 파일 - 첫 구매                              │ │
│  │  1. 다운로드 제한 확인                                   │ │
│  │     → 초과 시 DOWNLOAD_LIMIT_EXCEEDED 에러              │ │
│  │  2. 크레딧 잔액 확인                                     │ │
│  │     → 부족 시 INSUFFICIENT_CREDITS 에러                 │ │
│  │  3. 크레딧 차감 (CreditService.spendCredits)            │ │
│  │  4. CreditTransaction 기록                             │ │
│  │  5. FileDownload 기록 (is_free = false, price_paid)    │ │
│  │  6. Presigned URL 반환                                 │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 응답 (첫 구매)

```json
{
  "success": true,
  "data": {
    "fileUuid": "file-uuid-456",
    "fileName": "설계도면.pdf",
    "downloadUrl": "https://s3...?X-Amz-Signature=...",
    "price": 5000
  }
}
```

### 응답 (재다운로드)

```json
{
  "success": true,
  "data": {
    "fileUuid": "file-uuid-456",
    "fileName": "설계도면.pdf",
    "downloadUrl": "https://s3...?X-Amz-Signature=...",
    "price": 0
  }
}
```

### 에러 응답

**크레딧 부족:**
```json
{
  "success": false,
  "errorCode": "CR003",
  "message": "크레딧이 부족합니다"
}
```

**다운로드 제한 초과:**
```json
{
  "success": false,
  "errorCode": "FD002",
  "message": "다운로드 제한을 초과했습니다"
}
```

---

## 6. 구매 내역 조회

```http
GET /api/files/my-purchases?page=0&size=20
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "fileUuid": "file-uuid-456",
        "fileName": "설계도면.pdf",
        "fileSize": 1024000,
        "mimeType": "application/pdf",
        "pricePaid": 5000,
        "purchasedAt": "2025-12-05T10:30:00"
      },
      {
        "fileUuid": "file-uuid-789",
        "fileName": "견적서.xlsx",
        "fileSize": 52400,
        "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "pricePaid": 3000,
        "purchasedAt": "2025-12-04T15:20:00"
      }
    ],
    "totalElements": 15,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

---

## 7. DB 테이블 관계

```
┌──────────────────┐
│      boards      │
│    (자료실 글)    │
│ - uuid           │
│ - title          │
│ - type_data      │  ← isPaid, price 저장 (JSON)
│   (jsonb)        │
└────────┬─────────┘
         │
         │ 1:N
         ▼
┌──────────────────┐
│ board_attachments│
│   (파일 연결)     │
│ - board_id       │
│ - file_id        │
│ - attachment_type│
└────────┬─────────┘
         │
         │ N:1
         ▼
┌──────────────────┐
│      files       │
│   (파일 정보)     │
│ - uuid           │
│ - original_name  │
│ - file_url       │
│ - file_size      │
└────────┬─────────┘
         │
         │ 1:1
         ▼
┌──────────────────┐
│   file_pricing   │
│   (가격 설정)     │
│ - file_id        │
│ - is_paid        │
│ - price          │
│ - download_limit │
│ - is_active      │
└────────┬─────────┘
         │
         │ 1:N
         ▼
┌──────────────────┐     ┌──────────────────┐
│  file_downloads  │────▶│     credits      │
│  (다운로드 이력)   │     │  (크레딧 잔액)    │
│ - file_id        │     │ - user_id        │
│ - user_id        │     │ - balance        │
│ - is_free        │     │ - total_spent    │
│ - price_paid     │     └──────────────────┘
│ - ip_address     │              │
└──────────────────┘              │
                                  │ 1:N
                                  ▼
                    ┌────────────────────────┐
                    │  credit_transactions   │
                    │   (크레딧 사용 내역)     │
                    │ - credit_id            │
                    │ - type: SPEND          │
                    │ - amount: -5000        │
                    │ - entity_type:         │
                    │   FILE_DOWNLOAD        │
                    │ - entity_id: {file_id} │
                    │ - description          │
                    └────────────────────────┘
```

---

## 8. 관리자 기능

### 파일 가격 설정

```http
POST /api/admin/files/{fileUuid}/pricing
Authorization: Bearer {adminAccessToken}
Content-Type: application/json

{
  "isPaid": true,
  "price": 5000,
  "downloadLimit": 100,
  "description": "프리미엄 설계도면"
}
```

### 파일 가격 정보 조회

```http
GET /api/admin/files/{fileUuid}/pricing
Authorization: Bearer {adminAccessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "fileUuid": "file-uuid-456",
    "fileName": "설계도면.pdf",
    "fileSize": 1024000,
    "contentType": "application/pdf",
    "isPaid": true,
    "price": 5000,
    "downloadLimit": 100,
    "isActive": true,
    "description": "프리미엄 설계도면",
    "totalDownloads": 45,
    "totalRevenue": 225000
  }
}
```

### 유료 파일 목록 조회

```http
GET /api/admin/files/paid?page=0&size=20
Authorization: Bearer {adminAccessToken}
```

### 무료 전환

```http
POST /api/admin/files/{fileUuid}/pricing/free
Authorization: Bearer {adminAccessToken}
```

### 가격 비활성화

```http
DELETE /api/admin/files/{fileUuid}/pricing
Authorization: Bearer {adminAccessToken}
```

---

## 9. 핵심 포인트 요약

| 상황 | 결과 | 크레딧 차감 |
|------|------|------------|
| 무료 파일 다운로드 | 즉시 다운로드 | ❌ |
| 유료 파일 첫 구매 | 크레딧 차감 후 다운로드 | ✅ |
| 유료 파일 재다운로드 | 무료 다운로드 (이미 구매) | ❌ |
| 크레딧 부족 | `INSUFFICIENT_CREDITS` 에러 | - |
| 다운로드 제한 초과 | `DOWNLOAD_LIMIT_EXCEEDED` 에러 | - |

---

## 10. 프론트엔드 구현 가이드

### JavaScript 예시

```javascript
// 1. 구매 상태 확인
async function checkPurchaseStatus(fileUuid) {
  const response = await fetch(`/api/files/${fileUuid}/purchase-status`, {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
  return response.json();
}

// 2. 파일 다운로드
async function downloadFile(fileUuid) {
  // 구매 상태 확인
  const status = await checkPurchaseStatus(fileUuid);

  if (status.data.isPaid && !status.data.hasPurchased) {
    // 유료 파일 - 구매 확인
    const confirmed = confirm(
      `이 파일은 ${status.data.price.toLocaleString()}원입니다. 구매하시겠습니까?`
    );
    if (!confirmed) return;
  }

  // 다운로드 요청
  const response = await fetch(`/api/files/${fileUuid}/download`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });

  const result = await response.json();

  if (result.success) {
    // 다운로드 URL로 이동
    window.location.href = result.data.downloadUrl;
  } else {
    alert(result.message);
  }
}
```

---

## 관련 문서

- [크레딧 결제 시스템](./credit-payment-system.md)
- [광고 캠페인 시스템](./ad-campaign-system.md)
- [Redis-PostgreSQL 전략](./redis-postgresql-strategy.md)
