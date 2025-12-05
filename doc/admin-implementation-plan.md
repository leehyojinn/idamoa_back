# DAMOA 관리자 페이지 구현 계획 및 진척도

## 📋 프로젝트 개요

**목적**: ADMIN 역할을 가진 사용자를 위한 관리자 페이지 구현
**시작일**: 2025-11-24
**담당**: Claude AI

---

## 🔐 보안 규칙

### ADMIN 권한 검증 (모든 API 필수)

```java
User admin = userRepository.findByEmail(userDetails.getUsername())
    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

if (!admin.hasRole("ADMIN")) {
    throw new BusinessException(ErrorCode.FORBIDDEN);
}
```

### 컨트롤러 공통 패턴

```java
@Tag(name = "19XX. Admin - Feature Name", description = "기능 설명")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/feature")
public class AdminFeatureController {

    private final FeatureService featureService;
    private final UserRepository userRepository;

    @Operation(summary = "API 요약", description = "상세 설명")
    @GetMapping
    public ApiResponse<ResponseDto> getList(
            @AuthenticationPrincipal UserDetails userDetails) {

        // ADMIN 권한 검증
        User admin = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 비즈니스 로직
        return ApiResponse.success(response);
    }
}
```

---

## 📊 관리자 페이지 주요 기능

### 1. 메인 대시보드
- 전체 유저 수, 활성 유저 수
- 오늘/이번 달 신규 가입자
- 등록된 업체 수, 활성 업체 수
- 견적요청 통계 (상태별)
- 제안서 통계 (선택률)
- 빠른상담 통계
- 플래너 신청 통계
- 문의 통계

### 2. 회원 관리
- 회원 목록 조회 (페이징, 검색, 필터)
- 회원 상세 정보
- 회원 상태 변경 (ACTIVE, INACTIVE, SUSPENDED)
- 회원 역할 관리 (USER, COMPANY, ADMIN 추가/제거)
- 회원 삭제 (Soft Delete)

### 3. 등록 업체 관리 ✅
- 업체 목록 조회
- 업체 상세 정보
- 업체 상태 변경
- 업체 인증 처리
- 업체 삭제

### 4. 견적요청 관리 ✅
- 견적요청 목록 조회
- 견적요청 상세 정보
- 견적요청 상태 변경
- 견적요청 삭제

### 5. 플래너 관리 ✅
- 플래너 신청 목록 조회
- 플래너 신청 상세 정보
- 플래너 신청 상태 변경
- 답변 등록
- 메모 등록
- 담당자 배정
- 플래너 신청 삭제

### 6. 자료실 관리
- 자료 등록 (DOCUMENT 타입 게시판)
- 자료 목록 조회
- 자료 수정
- 자료 삭제

### 7. 공지/이벤트 관리 ✅
- 공지사항 CRUD
- 이벤트 CRUD
- 게시/비게시 관리

### 8. 사진 관리
- 갤러리 등록 (GALLERY 타입 게시판)
- 갤러리 목록 조회
- 갤러리 수정
- 갤러리 삭제

### 9. 제휴/광고 문의 관리
- 문의 목록 조회 (페이징, 필터)
- 문의 상세 정보
- 문의 상태 변경
- 답변 등록
- 문의 삭제

---

## 📁 파일 구조

```
src/main/java/com/hip/damoa/domain/admin/
├── web/
│   ├── AdminDashboardController.java (NEW)
│   ├── AdminUserController.java (NEW)
│   ├── AdminDocumentBoardController.java (NEW)
│   ├── AdminGalleryBoardController.java (NEW)
│   ├── AdminInquiryController.java (NEW)
│   ├── AdminCompanyController.java (EXISTS ✅)
│   ├── AdminEstimateRequestController.java (EXISTS ✅)
│   ├── AdminProposalController.java (EXISTS ✅)
│   ├── AdminNoticeBoardController.java (EXISTS ✅)
│   ├── AdminQuickConsultationController.java (EXISTS ✅)
│   ├── AdminPlannerApplicationController.java (EXISTS ✅)
│   └── dto/
│       ├── DashboardOverviewResponse.java (NEW)
│       ├── UserStatisticsResponse.java (NEW)
│       ├── CompanyStatisticsResponse.java (NEW)
│       ├── EstimateStatisticsResponse.java (NEW)
│       ├── ProposalStatisticsResponse.java (NEW)
│       ├── ConsultationStatisticsResponse.java (NEW)
│       ├── PlannerApplicationStatisticsResponse.java (NEW)
│       ├── InquiryStatisticsResponse.java (NEW)
│       ├── AdminUserListResponse.java (NEW)
│       ├── AdminUserDetailResponse.java (NEW)
│       ├── AdminUserStatusUpdateRequest.java (NEW)
│       ├── AdminUserRoleUpdateRequest.java (NEW)
│       └── ... (기타 DTO)
├── service/
│   ├── AdminDashboardService.java (NEW)
│   ├── AdminUserService.java (NEW)
│   └── AdminInquiryService.java (NEW)
└── model/ (EXISTS - 변경 불필요)

doc/
└── admin-implementation-plan.md (THIS FILE)
```

---

## 🚀 구현 작업 현황

### ✅ Phase 0: 기존 구현 완료
- [x] 업체 관리 API (`AdminCompanyController`)
  - CRUD, 상태 변경, 인증 처리
  - Swagger Tag: "1901. Admin - Company"

- [x] 견적요청 관리 API (`AdminEstimateRequestController`)
  - 목록 조회, 상세, 상태 변경, 삭제
  - Swagger Tag: "1902. Admin - EstimateRequest"

- [x] 제안서 관리 API (`AdminProposalController`)
  - 목록 조회, 삭제
  - Swagger Tag: "1903. Admin - Proposal"

- [x] 공지/이벤트 관리 API (`AdminNoticeBoardController`)
  - 공지사항 CRUD, 이벤트 CRUD
  - Swagger Tag: "1904. Admin - Notice/Event Board"

- [x] 빠른 상담 관리 API (`AdminQuickConsultationController`)
  - 목록 조회, 상태 변경, 답변 등록, 업체 배정
  - Swagger Tag: "1905. Admin - Quick Consultation"

- [x] 플래너 신청 관리 API (`AdminPlannerApplicationController`)
  - 목록 조회, 상태 변경, 답변 등록, 메모, 담당자 배정
  - Swagger Tag: "1912. Admin - Planner Application"

---

### 🚧 Phase 1: 작업 추적 문서 생성
- [x] `doc/admin-implementation-plan.md` 생성 (2025-11-24)

---

### 📅 Phase 2: 대시보드 통계 API (우선순위 1)
**Swagger Tag**: "1900. Admin - Dashboard"

#### 2.1 Response DTO 생성
- [x] `DashboardOverviewResponse.java` - 전체 요약 통계 (2025-11-24)
- [x] `UserStatisticsResponse.java` - 회원 상세 통계 (2025-11-24)
- [x] `CompanyStatisticsResponse.java` - 업체 상세 통계 (2025-11-24)
- [x] `EstimateStatisticsResponse.java` - 견적요청 상세 통계 (2025-11-24)
- [x] `ProposalStatisticsResponse.java` - 제안서 상세 통계 (2025-11-24)
- [x] `ConsultationStatisticsResponse.java` - 빠른상담 상세 통계 (2025-11-24)
- [x] `PlannerApplicationStatisticsResponse.java` - 플래너신청 상세 통계 (2025-11-24)
- [x] `InquiryStatisticsResponse.java` - 문의 상세 통계 (2025-11-24)

#### 2.2 Service 구현
- [x] `AdminDashboardService.java` 생성 (2025-11-24)
  - [x] `getOverview()` - 전체 요약
  - [x] `getUserStatistics()` - 회원 통계
  - [x] `getCompanyStatistics()` - 업체 통계
  - [x] `getEstimateStatistics()` - 견적요청 통계
  - [x] `getProposalStatistics()` - 제안서 통계
  - [x] `getConsultationStatistics()` - 빠른상담 통계
  - [x] `getPlannerApplicationStatistics()` - 플래너신청 통계
  - [x] `getInquiryStatistics()` - 문의 통계

#### 2.3 Repository 메서드 추가
- [x] UserRepository 통계 메서드 (2025-11-24)
  - [x] `countByStatusAndIsDeletedFalse(String)`
  - [x] `countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime)`
  - [x] `countByRolesContainingAndIsDeletedFalse(String)`
  - [x] `countByEmailVerifiedTrueAndIsDeletedFalse()`
  - [x] `countByPhoneVerifiedTrueAndIsDeletedFalse()`
  - [x] `countByIdentityVerifiedTrueAndIsDeletedFalse()`
  - [x] `countByMarketingAgreedTrueAndIsDeletedFalse()`

- [x] CompanyRepository 통계 메서드 (2025-11-24)
  - [x] `countByStatusAndIsDeletedFalse(String)`
  - [x] `countByVerifiedTrueAndIsDeletedFalse()`
  - [x] `countByPremiumUntilAfterAndIsDeletedFalse(LocalDateTime)`
  - [x] `countByPremiumTierAndIsDeletedFalse(String)`
  - [x] `getAverageRating()`
  - [x] `getTotalReviewCount()`

- [x] EstimateRequestRepository 통계 메서드 (2025-11-24)
  - [x] `countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime)`
  - [x] `countByIsPublicTrueAndIsDeletedFalse()`
  - [x] `getAverageProposalCount()`
  - [x] `getTotalViewCount()`

- [x] EstimateProposalRepository 통계 메서드 (2025-11-24)
  - [x] `countByIsDeletedFalse()`
  - [x] `countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime)`
  - [x] `countByStatusAndIsDeletedFalse(String)`
  - [x] `countByIsSelectedTrueAndIsDeletedFalse()`
  - [x] `getAveragePrice()`

- [x] QuickConsultationRepository 통계 메서드 (2025-11-24)
  - [x] `countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime)`
  - [x] `countByStatusAndIsDeletedFalse(String)` (overload)
  - [x] `countByUserIsNotNullAndIsDeletedFalse()`
  - [x] `countByUserIsNullAndIsDeletedFalse()`
  - [x] `countByAssignedCompanyIsNotNullAndIsDeletedFalse()`

- [x] PlannerApplicationRepository 통계 메서드 (2025-11-24)
  - [x] `countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime)`
  - [x] `countByStatusAndIsDeletedFalse(String)` (overload)

- [x] InquiryRepository 통계 메서드 (2025-11-24)
  - [x] `countByIsDeletedFalse()`
  - [x] `countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime)`
  - [x] `countByStatusAndIsDeletedFalse(String)`
  - [x] `countGroupByInquiryType()`

#### 2.4 Controller 구현
- [x] `AdminDashboardController.java` 생성 (2025-11-24)
  - [x] `GET /api/admin/dashboard/overview` - 전체 요약
  - [x] `GET /api/admin/dashboard/users` - 회원 통계
  - [x] `GET /api/admin/dashboard/companies` - 업체 통계
  - [x] `GET /api/admin/dashboard/estimates` - 견적요청 통계
  - [x] `GET /api/admin/dashboard/proposals` - 제안서 통계
  - [x] `GET /api/admin/dashboard/consultations` - 빠른상담 통계
  - [x] `GET /api/admin/dashboard/planner-applications` - 플래너신청 통계
  - [x] `GET /api/admin/dashboard/inquiries` - 문의 통계

**✅ Phase 2 완료**: 대시보드 통계 API 구현 완료 (2025-11-24)
- 빌드 성공 확인 완료

---

### 📅 Phase 3: 회원 관리 API (우선순위 2)
**Swagger Tag**: "1906. Admin - User"

#### 3.1 DTO 생성
- [ ] `AdminUserListResponse.java`
- [ ] `AdminUserDetailResponse.java`
- [ ] `AdminUserStatusUpdateRequest.java`
- [ ] `AdminUserRoleUpdateRequest.java`

#### 3.2 Service 구현
- [ ] `AdminUserService.java` 생성
  - [ ] `getAllUsers()` - 회원 목록 (페이징, 검색, 필터)
  - [ ] `getUserDetail()` - 회원 상세
  - [ ] `updateUserStatus()` - 상태 변경
  - [ ] `updateUserRoles()` - 역할 추가/제거
  - [ ] `deleteUser()` - 회원 삭제 (soft delete)

#### 3.3 Controller 구현
- [ ] `AdminUserController.java` 생성
  - [ ] `GET /api/admin/users` - 회원 목록
  - [ ] `GET /api/admin/users/{userUuid}` - 회원 상세
  - [ ] `PATCH /api/admin/users/{userUuid}/status` - 상태 변경
  - [ ] `PATCH /api/admin/users/{userUuid}/roles` - 역할 관리
  - [ ] `DELETE /api/admin/users/{userUuid}` - 회원 삭제

---

### 📅 Phase 4: 제휴/광고 문의 관리 API (우선순위 3)
**Swagger Tag**: "1909. Admin - Inquiry"

#### 4.1 DTO 생성
- [ ] `AdminInquiryListResponse.java`
- [ ] `AdminInquiryDetailResponse.java`
- [ ] `AdminInquiryStatusUpdateRequest.java`
- [ ] `AdminInquiryResponseRequest.java`

#### 4.2 Service 구현
- [ ] `AdminInquiryService.java` 생성
  - [ ] `getAllInquiries()` - 문의 목록 (페이징, 필터)
  - [ ] `getInquiryDetail()` - 문의 상세
  - [ ] `updateInquiryStatus()` - 상태 변경
  - [ ] `addInquiryResponse()` - 답변 등록
  - [ ] `deleteInquiry()` - 문의 삭제

#### 4.3 Controller 구현
- [ ] `AdminInquiryController.java` 생성
  - [ ] `GET /api/admin/inquiries` - 문의 목록
  - [ ] `GET /api/admin/inquiries/{inquiryUuid}` - 문의 상세
  - [ ] `PATCH /api/admin/inquiries/{inquiryUuid}/status` - 상태 변경
  - [ ] `POST /api/admin/inquiries/{inquiryUuid}/response` - 답변 등록
  - [ ] `DELETE /api/admin/inquiries/{inquiryUuid}` - 문의 삭제

---

### 📅 Phase 5: 자료실 관리 API (우선순위 4)
**Swagger Tag**: "1907. Admin - Document Board"

#### 5.1 Controller 구현
- [ ] `AdminDocumentBoardController.java` 생성
  - [ ] `POST /api/admin/boards/document` - 자료 등록
  - [ ] `GET /api/admin/boards/document` - 자료 목록
  - [ ] `PUT /api/admin/boards/document/{uuid}` - 자료 수정
  - [ ] `DELETE /api/admin/boards/document/{uuid}` - 자료 삭제

**참고**: Board 엔티티와 Service는 이미 존재하므로 Controller만 추가

---

### 📅 Phase 6: 사진 관리 API (우선순위 5)
**Swagger Tag**: "1908. Admin - Gallery Board"

#### 6.1 Controller 구현
- [ ] `AdminGalleryBoardController.java` 생성
  - [ ] `POST /api/admin/boards/gallery` - 갤러리 등록
  - [ ] `GET /api/admin/boards/gallery` - 갤러리 목록
  - [ ] `PUT /api/admin/boards/gallery/{uuid}` - 갤러리 수정
  - [ ] `DELETE /api/admin/boards/gallery/{uuid}` - 갤러리 삭제

**참고**: Board 엔티티와 Service는 이미 존재하므로 Controller만 추가

---

## 📊 대시보드 통계 상세 정의

### Overview (전체 요약)
```json
{
  "totalUsers": 1234,
  "activeUsers": 1100,
  "newUsersToday": 15,
  "newUsersThisMonth": 234,
  "totalCompanies": 567,
  "activeCompanies": 456,
  "totalEstimateRequests": 890,
  "estimateRequestsInProgress": 123,
  "totalProposals": 2345,
  "proposalSelectionRate": 15.6,
  "pendingConsultations": 45,
  "pendingPlannerApplications": 23,
  "totalInquiries": 678
}
```

### User Statistics
```json
{
  "total": 1234,
  "newToday": 15,
  "newThisMonth": 234,
  "byStatus": {
    "active": 1100,
    "pending": 34,
    "inactive": 80,
    "suspended": 20
  },
  "byRole": {
    "user": 800,
    "company": 400,
    "admin": 5
  },
  "verification": {
    "emailVerified": 1000,
    "phoneVerified": 800,
    "identityVerified": 600
  },
  "marketingAgreed": 700
}
```

### Company Statistics
```json
{
  "total": 567,
  "newToday": 3,
  "newThisMonth": 45,
  "byStatus": {
    "active": 456,
    "pending": 23,
    "inactive": 78,
    "suspended": 10
  },
  "verified": 345,
  "premium": 123,
  "byPremiumTier": {
    "none": 400,
    "basic": 50,
    "standard": 60,
    "premium": 40,
    "vip": 17
  },
  "averageRating": 4.3,
  "totalReviews": 3456
}
```

### EstimateRequest Statistics
```json
{
  "total": 890,
  "newToday": 12,
  "newThisMonth": 123,
  "byStatus": {
    "draft": 45,
    "published": 234,
    "inProgress": 123,
    "matched": 345,
    "completed": 120,
    "cancelled": 23
  },
  "public": 670,
  "averageProposalsPerRequest": 2.6,
  "totalViews": 45678
}
```

### Proposal Statistics
```json
{
  "total": 2345,
  "newToday": 34,
  "newThisMonth": 456,
  "byStatus": {
    "submitted": 567,
    "viewed": 890,
    "selected": 234,
    "rejected": 600,
    "withdrawn": 54
  },
  "selectionRate": 15.6,
  "averagePrice": 3450000
}
```

### QuickConsultation Statistics
```json
{
  "total": 678,
  "newToday": 8,
  "newThisMonth": 89,
  "byStatus": {
    "submitted": 45,
    "inProgress": 123,
    "completed": 456,
    "cancelled": 54
  },
  "memberVsNonMember": {
    "member": 456,
    "nonMember": 222
  },
  "assigned": 567
}
```

### PlannerApplication Statistics
```json
{
  "total": 234,
  "newToday": 3,
  "newThisMonth": 34,
  "byStatus": {
    "pending": 23,
    "inProgress": 67,
    "completed": 123,
    "rejected": 21
  }
}
```

### Inquiry Statistics
```json
{
  "total": 456,
  "newToday": 5,
  "newThisMonth": 56,
  "byStatus": {
    "pending": 34,
    "inProgress": 123,
    "completed": 278,
    "cancelled": 21
  },
  "byType": {
    "partnership": 234,
    "advertising": 222
  }
}
```

---

## 🔄 Google Analytics 통합 (향후 고려사항)

### 현재 상태
- Google Analytics 직접 통합 없음
- 자체 DB 통계만 제공

### 향후 통합 옵션
1. **프론트엔드 통합** (권장)
   - React/Vue에서 GA4 SDK 사용
   - 클라이언트 측에서 이벤트 추적

2. **서버 사이드 통합**
   - Google Analytics Reporting API v4 사용
   - 필요 라이브러리: `google-analytics-data`
   - OAuth 2.0 인증 필요

### 제안
- Phase 1-6에서는 자체 DB 통계만 제공
- 향후 필요 시 GA API 통합 추가

---

## 📝 작업 기록

### 2025-11-24
- [x] 프로젝트 계획서 작성
- [x] 파일 구조 정의
- [x] 통계 항목 상세 정의
- [x] Phase 2 완료: 대시보드 통계 API 구현 완료
  - [x] 8개 Response DTO 생성
  - [x] AdminDashboardService 생성 (8개 메서드)
  - [x] 7개 Repository에 통계 메서드 추가
  - [x] AdminDashboardController 생성 (8개 엔드포인트)
  - [x] 빌드 성공 확인

---

## 📌 참고 사항

### 기존 Admin 엔드포인트 URL 패턴
- 업체: `/api/admin/companies`
- 견적요청: `/api/admin/estimate-requests`
- 제안서: `/api/admin/proposals`
- 공지/이벤트: `/api/admin/boards/notice`, `/api/admin/boards/event`
- 빠른상담: `/api/admin/consultations`
- 플래너: `/api/admin/planner-applications`

### Swagger Tag 넘버링
- 1900: Dashboard
- 1901: Company
- 1902: EstimateRequest
- 1903: Proposal
- 1904: Notice/Event Board
- 1905: Quick Consultation
- 1906: User (신규)
- 1907: Document Board (신규)
- 1908: Gallery Board (신규)
- 1909: Inquiry (신규)
- 1912: Planner Application

---

## ✅ 체크리스트

매 API 구현 시 확인사항:
- [ ] ADMIN 권한 검증 포함
- [ ] `@SecurityRequirement(name = "bearerAuth")` 추가
- [ ] Swagger `@Tag`, `@Operation` 작성
- [ ] 로그 기록 (`log.info`, `log.warn`)
- [ ] DTO validation (`@Valid`)
- [ ] Soft delete 고려 (`isDeleted = false`)
- [ ] 페이징 처리 (`@PageableDefault`)
- [ ] UUID 기반 식별자 사용
- [ ] ApiResponse 래퍼 사용
- [ ] 에러 처리 (BusinessException)

---

**최종 수정일**: 2025-11-24
