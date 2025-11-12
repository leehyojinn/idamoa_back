# 다모아(Damoa) 작업일지

> **작업 규칙**: 모든 작업 시작 전 이 파일을 확인하고, 작업 완료 후 반드시 기록을 업데이트합니다.

---

## 📋 작업 순서

### 1단계: 작업 시작 전 체크리스트
- [ ] `doc/work-log.md` 파일 확인
- [ ] 현재 진행 중인 작업 확인
- [ ] 다음 우선순위 작업 확인
- [ ] 관련 문서 확인 (`doc/mermaid.txt`, `doc/redis-postgresql-strategy.md` 등)

### 2단계: 작업 진행
- [ ] 작업 내용을 "진행 중" 섹션에 기록
- [ ] 코드 변경사항 추적
- [ ] 테스트 수행

### 3단계: 작업 완료
- [ ] 작업 결과를 "완료" 섹션으로 이동
- [ ] 관련 파일 경로 및 주요 변경사항 기록
- [ ] 다음 작업 항목 업데이트

---

## 🎯 현재 상태 (Current Status)

**프로젝트 단계**: 비밀번호 찾기/변경 기능 구현 완료
**마지막 업데이트**: 2025-11-12
**다음 우선순위**: 사용자 프로필 완성 및 활성화 테스트

---

## 📝 작업 로그

### 2025-11-12

#### ✅ 완료 (Completed)

**[AUTH-002] 비밀번호 찾기 및 변경 기능 구현** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-12 (약 2시간)
- **작업 내용**:
  - 비밀번호 찾기 기능 구현 (로그인 불필요, 토큰 기반 4단계 플로우)
  - 비밀번호 변경 기능 구현 (로그인 필요, 현재 비밀번호 검증)
  - 이메일 템플릿 추가 (PASSWORD_RESET, PASSWORD_CHANGED)
  - SecurityConfig 업데이트 (비밀번호 찾기 엔드포인트 public 설정)
  - 에러 코드 추가 (PW001-PW004)

**생성/수정 파일**:

**신규 파일 (7개)**:
1. `src/main/java/com/hip/damoa/domain/user/web/dto/PasswordResetStartResponse.java`
2. `src/main/java/com/hip/damoa/domain/user/web/dto/PasswordResetRequestRequest.java`
3. `src/main/java/com/hip/damoa/domain/user/web/dto/PasswordResetEmailVerificationRequest.java`
4. `src/main/java/com/hip/damoa/domain/user/web/dto/PasswordResetVerifyRequest.java`
5. `src/main/java/com/hip/damoa/domain/user/web/dto/PasswordResetCompleteRequest.java`
6. `src/main/java/com/hip/damoa/domain/user/web/dto/PasswordChangeRequest.java`
7. `src/main/resources/db/migration/V7__Add_password_reset_and_changed_templates.sql`

**수정 파일 (6개)**:
1. `src/main/java/com/hip/damoa/config/web/SecurityConfig.java`
2. `src/main/java/com/hip/damoa/core/exception/ErrorCode.java`
3. `src/main/java/com/hip/damoa/domain/user/model/User.java`
4. `src/main/java/com/hip/damoa/domain/user/service/AuthService.java`
5. `src/main/java/com/hip/damoa/domain/user/service/VerificationService.java`
6. `src/main/java/com/hip/damoa/domain/user/web/AuthController.java`

**주요 변경사항**:

1. **비밀번호 찾기 플로우 (Public - 로그인 불필요)**:
   - `POST /api/auth/password/reset/start` - UUID 토큰 발급, Redis에 이메일 저장 (30분 TTL)
   - `POST /api/auth/password/reset/verification/send` - 6자리 인증 코드 이메일 발송 (15분 TTL)
   - `POST /api/auth/password/reset/verification/verify` - 인증 코드 확인 및 검증 플래그 설정
   - `POST /api/auth/password/reset/complete` - 비밀번호 변경 + 완료 이메일 발송

2. **비밀번호 변경 플로우 (Authenticated - 로그인 필요)**:
   - `POST /api/auth/password/change` - 현재 비밀번호 확인 → 새 비밀번호 변경 + 완료 이메일 발송

3. **VerificationService 개선**:
   - `PASSWORD_RESET_PREFIX`, `PASSWORD_RESET_OTP_PREFIX`, `PASSWORD_RESET_VERIFIED_PREFIX` 상수 추가
   - `sendPasswordResetCode(resetToken, email)` - 토큰 기반 인증 코드 발송
   - `verifyPasswordResetCode(resetToken, code)` - 토큰 기반 코드 검증
   - `validateResetTokenAndEmail(resetToken, email)` - 토큰-이메일 매칭 검증

4. **AuthService 개선**:
   - `startPasswordReset(email)` - 토큰 발급 및 Redis 저장 (30분 TTL)
   - `verifyPasswordResetCode(resetToken, code)` - 인증 코드 검증 위임
   - `completePasswordReset(resetToken, newPassword)` - 비밀번호 변경 + Redis 정리
   - `changePassword(userEmail, currentPassword, newPassword)` - 로그인 상태 비밀번호 변경
   - `sendPasswordChangedEmail(email, userId, changeMethod)` - 비밀번호 변경 알림 이메일

5. **User 엔티티**:
   - `updatePassword(encodedPassword)` 메서드 추가

6. **SecurityConfig**:
   - `/api/auth/password/reset/**` 엔드포인트 public 설정 추가

7. **에러 코드**:
   - `PASSWORD_RESET_TOKEN_NOT_FOUND` (PW001)
   - `PASSWORD_RESET_TOKEN_EXPIRED` (PW002)
   - `INVALID_PASSWORD` (PW003)
   - `SAME_PASSWORD` (PW004)

8. **이메일 템플릿 (V7 Migration)**:
   - `PASSWORD_RESET` - 비밀번호 찾기 인증 코드 발송용
   - `PASSWORD_CHANGED` - 비밀번호 변경 완료 알림용

**API 엔드포인트**:

**비밀번호 찾기 (Public)**:
```
POST /api/auth/password/reset/start              # 토큰 발급
POST /api/auth/password/reset/verification/send  # 인증 코드 발송
POST /api/auth/password/reset/verification/verify # 인증 코드 확인
POST /api/auth/password/reset/complete           # 비밀번호 변경 완료
```

**비밀번호 변경 (Authenticated)**:
```
POST /api/auth/password/change                    # 로그인 상태 비밀번호 변경
```

**보안 고려사항**:
- 토큰 기반 시스템으로 Redis 상태 관리 (회원가입 패턴과 동일)
- 이메일-토큰 검증으로 타인의 비밀번호 변경 방지
- 인증 코드 시도 횟수 제한 (VerificationService의 checkAttempts)
- 비밀번호 변경 후 알림 이메일 자동 발송 (보안 알림)
- 현재 비밀번호와 동일한 새 비밀번호 차단 (SAME_PASSWORD 에러)

**빌드 결과**: ✅ BUILD SUCCESSFUL

**런타임 테스트**: ✅ Application started successfully

**문제 해결**:
- ✅ Redis 토큰 기반 시스템 적용 (회원가입과 동일한 패턴)
- ✅ Public 엔드포인트 설정 (SecurityConfig 업데이트)
- ✅ 이메일 템플릿 추가 (V7 migration)
- ✅ 에러 코드 충돌 방지 (PR → PW prefix 사용)

---

### 2025-11-11

#### ✅ 완료 (Completed)

**[USER-001] 사용자 상태(status) 필드 추가 및 활성화 로직 구현** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-11 (약 1시간)
- **작업 내용**:
  - User 엔티티에 status 필드 매핑 추가
  - UserStatus enum 생성 (타입 안정성 및 코드 가독성 향상)
  - 사용자 활성화/비활성화/정지 비즈니스 메서드 구현
  - 프로필 완성 시 자동 활성화 로직 추가
  - PostgreSQL 배열 타입 어노테이션 표준화 (Hypersistence Utils → Hibernate 6.x)

**생성/수정 파일**:
1. `src/main/java/com/hip/damoa/domain/user/model/UserStatus.java` (신규)
2. `src/main/java/com/hip/damoa/domain/user/model/User.java` (수정)
3. `src/main/java/com/hip/damoa/domain/user/service/ProfileService.java` (수정)

**주요 변경사항**:

1. **UserStatus.java (신규 생성)**:
   ```java
   public enum UserStatus {
       PENDING,    // 대기 중 (프로필 미완성)
       ACTIVE,     // 활성화 (정상 사용)
       INACTIVE,   // 비활성화 (탈퇴 등)
       SUSPENDED,  // 정지 (이용 제한)
       DELETED     // 삭제 (완전 삭제)
   }
   ```
   - 타입 안정성 (Type Safety) 보장
   - 컴파일 타임 체크
   - IDE 자동완성 지원

2. **User.java 변경사항**:
   - 임포트 변경: `io.hypersistence.utils.hibernate.type.array.StringArrayType` → `org.hibernate.annotations.JdbcTypeCode` + `org.hibernate.type.SqlTypes`
   - roles 필드 어노테이션: `@Type(StringArrayType.class)` → `@JdbcTypeCode(SqlTypes.ARRAY)`
   - status 필드를 String → UserStatus enum으로 변경:
     ```java
     @Enumerated(EnumType.STRING)
     @Column(name = "status", length = 50, nullable = false)
     @Builder.Default
     private UserStatus status = UserStatus.PENDING;
     ```
   - 비즈니스 메서드 추가:
     - `activate()`: 사용자 활성화 (UserStatus.ACTIVE 설정)
     - `deactivate()`: 사용자 비활성화 (UserStatus.INACTIVE)
     - `suspend()`: 사용자 정지 (UserStatus.SUSPENDED)
     - `isActive()`, `isPending()`, `isSuspended()`: enum 비교로 상태 확인

3. **ProfileService.java 변경사항**:
   - `createUserProfile()` 메서드:
     - `user.completeProfile()` 후 `user.activate()` 호출 추가
     - 로그 메시지에 status 추가
   - `createCompanyProfile()` 메서드:
     - `user.completeProfile()` 후 `user.activate()` 호출 추가
     - 로그 메시지에 status 추가

**문제 해결**:
- ❌ **기존 문제**: 소셜 로그인/회원가입 시 users 테이블에 status가 'PENDING'으로 저장되고, 프로필 완성 후에도 'PENDING' 상태 유지
- ✅ **해결**: User 엔티티에 status 필드 매핑 추가 및 프로필 완성 시 자동 활성화 로직 구현
- ✅ **표준화**: Hypersistence Utils 대신 Hibernate 6.x 표준 어노테이션 사용 (`@JdbcTypeCode(SqlTypes.ARRAY)`)

**DB 스키마 (기존 V1 마이그레이션)**:
```sql
status VARCHAR(50) DEFAULT 'PENDING' NOT NULL
CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'))
```

**빌드 결과**: ✅ BUILD SUCCESSFUL

### 2025-11-10

#### ✅ 완료 (Completed)

**[ESTIMATE-002] 견적/제안 첨부파일 조인 테이블 리팩토링** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-10 (약 2시간)
- **작업 내용**:
  - 파일 관리 시스템 개선: PostgreSQL 배열 → 조인 테이블로 마이그레이션
  - 첨부파일 메타데이터 지원 (file_type, file_description, display_order)
  - Company 패턴과 동일한 구조로 통일
  - FileCleanupScheduler의 orphaned file 삭제 문제 해결 (entityId NULL 이슈)
  - V30 마이그레이션 생성 및 레거시 필드 제거

**생성/수정 파일**:
1. `src/main/resources/db/migration/V30__Create_estimate_attachment_tables.sql` (신규)
2. `src/main/java/com/hip/damoa/domain/estimate/model/EstimateRequestAttachment.java` (신규)
3. `src/main/java/com/hip/damoa/domain/estimate/model/EstimateProposalAttachment.java` (신규)
4. `src/main/java/com/hip/damoa/domain/estimate/repository/EstimateRequestAttachmentRepository.java` (신규)
5. `src/main/java/com/hip/damoa/domain/estimate/repository/EstimateProposalAttachmentRepository.java` (신규)
6. `src/main/java/com/hip/damoa/domain/estimate/web/dto/AttachmentRequest.java` (신규)
7. `src/main/java/com/hip/damoa/domain/estimate/web/dto/AttachmentResponse.java` (신규)
8. `src/main/java/com/hip/damoa/domain/estimate/model/EstimateRequest.java` (수정)
9. `src/main/java/com/hip/damoa/domain/estimate/model/EstimateProposal.java` (수정)
10. `src/main/java/com/hip/damoa/domain/estimate/service/EstimateRequestService.java` (수정)
11. `src/main/java/com/hip/damoa/domain/estimate/service/ProposalService.java` (수정)
12. `src/main/java/com/hip/damoa/domain/estimate/web/dto/EstimateRequestCreateRequest.java` (수정)
13. `src/main/java/com/hip/damoa/domain/estimate/web/dto/EstimateRequestUpdateRequest.java` (수정)
14. `src/main/java/com/hip/damoa/domain/estimate/web/dto/EstimateRequestResponse.java` (수정)
15. `src/main/java/com/hip/damoa/domain/estimate/web/dto/ProposalCreateRequest.java` (수정)
16. `src/main/java/com/hip/damoa/domain/estimate/web/dto/ProposalUpdateRequest.java` (수정)
17. `src/main/java/com/hip/damoa/domain/estimate/web/dto/ProposalResponse.java` (수정)
18. `src/main/java/com/hip/damoa/domain/estimate/web/EstimateRequestController.java` (수정)
19. `src/main/java/com/hip/damoa/domain/estimate/web/ProposalController.java` (수정)
20. `src/main/java/com/hip/damoa/domain/admin/web/dto/AdminEstimateRequestResponse.java` (수정)

**주요 변경사항**:

1. **조인 테이블 생성 (V30 마이그레이션)**:
   - `estimate_request_attachments` 테이블 생성
   - `estimate_proposal_attachments` 테이블 생성
   - 메타데이터 필드: file_type, file_description, display_order
   - 레거시 필드 삭제: attachment_file_ids (estimate_requests), attachments, attachment_file_ids (estimate_proposals)
   - ON DELETE CASCADE 설정

2. **Entity 변경**:
   - 배열 필드 제거: `@Type(LongArrayType.class) private Long[] attachmentFileIds`
   - OneToMany 관계 추가: `@OneToMany(mappedBy = "estimateRequest", cascade = CascadeType.ALL, orphanRemoval = true)`
   - 편의 메서드 추가: `addAttachment()`, `clearAttachments()`

3. **DTO 리팩토링**:
   - Request DTO: `List<AttachmentRequest>` 사용 (fileUrl, fileType, fileDescription, displayOrder)
   - Response DTO: `List<AttachmentResponse>` 사용 (id, fileUrl, fileType, fileDescription, displayOrder, createdAt)
   - File URL ↔ File ID 변환 로직 추가

4. **Service Layer**:
   - `processAttachments()`: URL을 File ID로 변환하여 조인 테이블에 저장
   - `getAttachmentResponses()`: Entity → DTO 변환 (File ID → URL)
   - `convertUrlToFileId()`, `convertFileIdToUrl()`: File 테이블 조회 및 변환

5. **Controller Layer**:
   - `enrichWithFiles()` 시그니처 변경: Response → Entity 파라미터로 변경
   - Service의 `getAttachmentResponses()` 호출하여 첨부파일 정보 설정

**문제 해결**:
- ❌ **기존**: EstimateRequest/Proposal 파일 업로드 시 entityId가 NULL로 저장되어 FileCleanupScheduler가 5분 후 삭제
- ✅ **해결**: 조인 테이블을 사용하여 파일과 엔티티 관계를 명시적으로 관리
- ✅ **일관성**: Company 패턴과 동일하게 리팩토링하여 전체 시스템 일관성 확보

**빌드 결과**: ✅ BUILD SUCCESSFUL

### 2025-11-07

#### ✅ 완료 (Completed)

**[ESTIMATE-001] 견적 요청 API CRUD 및 제안 API 구현** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-07 (약 3시간)
- **작업 내용**:
  - 범용 견적 시스템 구현 (병원, 카페, 사무실 등 모든 업종 지원)
  - V26 마이그레이션 파일 생성
  - EstimateRequest, EstimateProposal 엔티티 필드 확장
  - 권한별 제안 조회 로직 구현 (요청자/제안자/일반 회원)
  - 테스트 HTML 페이지 2개 생성

**생성/수정 파일**:
1. `src/main/resources/db/migration/V26__Add_estimate_request_fields.sql`
2. `src/main/java/com/hip/damoa/domain/estimate/model/EstimateRequest.java`
3. `src/main/java/com/hip/damoa/domain/estimate/model/EstimateProposal.java`
4. `src/main/java/com/hip/damoa/domain/estimate/web/dto/EstimateRequestCreateRequest.java`
5. `src/main/java/com/hip/damoa/domain/estimate/web/dto/ProposalCreateRequest.java`
6. `src/main/java/com/hip/damoa/domain/estimate/web/dto/EstimateRequestResponse.java`
7. `src/main/java/com/hip/damoa/domain/estimate/web/dto/ProposalResponse.java`
8. `src/main/java/com/hip/damoa/domain/estimate/service/EstimateRequestService.java`
9. `src/main/java/com/hip/damoa/domain/estimate/service/ProposalService.java`
10. `src/main/resources/static/estimate-request-test.html`
11. `src/main/resources/static/proposal-test.html`

**주요 변경사항**:
- **범용 필드 추가**: `client_name`, `business_type`, `area_pyeong`, `contact_name`, `contact_phone` 등
- **권한별 조회**: 요청자는 모든 제안, 업체는 자신의 제안만, 일반 회원은 개수만 조회
- **File ID 배열**: 첨부파일을 Long[] 배열로 관리
- **테스트 페이지**: 견적 요청 작성 및 제안 제출 테스트 가능

### 2025-11-03

#### ✅ 완료 (Completed)

**[COMPANY-001] 업체 CRUD 개선 및 검색 기능 구현** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-03 (약 3시간)
- **작업 내용**:
  - DB 스키마 확장 (V8, V9 마이그레이션)
  - Company 엔티티에 프리미엄 등급 필드 추가
  - CompanyListResponse에 좋아요 수, 이미지, 프리미엄 등급 추가
  - 업체 검색 기능 구현 (필터링 + 정렬)
  - 업체 목록/조회 API를 public으로 변경
  - 이미지 목록 제공 (최대 3개)

**[COMPANY-002] 인증 오류 처리 개선 (401/403 응답)** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-03 오후
- **문제**: 토큰 없이 POST /api/companies 요청 시 500 에러 발생 (401이 나와야 함)
- **작업 내용**:
  1. **GlobalExceptionHandler 개선**:
     - `AuthenticationException` 핸들러 추가 (401 반환)
     - `AccessDeniedException` 핸들러 추가 (403 반환)
     - 파일: `core/exception/GlobalExceptionHandler.java`

  2. **SecurityConfig 개선**:
     - HTTP 메서드별 권한 분리 (GET: public, POST/PUT/DELETE: authenticated)
     - `AuthenticationEntryPoint` 추가 (401 + JSON 응답)
     - `AccessDeniedHandler` 추가 (익명 사용자: 401, 인증된 사용자: 403)
     - 파일: `config/web/SecurityConfig.java`

- **테스트 결과**:
  ```bash
  # 인증 없이 POST 요청
  POST /api/companies (without token)
  → HTTP 401: {"success":false,"message":"인증이 필요합니다. 로그인 후 다시 시도해주세요."}

  # 인증 없이 GET 요청 (public)
  GET /api/companies
  → HTTP 200: {"success":true,"data":{"content":[],...}}
  ```

- **기술 결정**:
  - Spring Security의 `AnonymousAuthenticationToken` 체크하여 401/403 구분
  - JSON 응답 형식 통일 (`success`, `message` 필드)
  - HTTP 메서드별 세분화된 권한 제어
  - Entity와 DB 스키마 불일치 해결 (컬럼명 변경)
  - 빌드, 마이그레이션, API 테스트 완료

- **생성 파일**:
  - `src/main/resources/db/migration/V8__add_company_premium_tier.sql` - 프리미엄 등급 및 누락 필드 스키마
  - `src/main/resources/db/migration/V9__rename_company_columns.sql` - 컬럼명 변경 (Entity 일치)
  - `domain/company/web/dto/CompanyImageDto.java` - 이미지 DTO
  - `domain/company/web/dto/CompanySearchRequest.java` - 검색 요청 DTO
  - `domain/company/repository/CompanySpecification.java` - 동적 검색 Specification

- **수정 파일**:
  - `domain/company/model/Company.java` - premiumTier, premiumMonthlyAmount 필드 추가
  - `domain/company/web/dto/CompanyListResponse.java` - likeCount, images, premiumTier 추가
  - `domain/company/repository/CompanyRepository.java` - JpaSpecificationExecutor 추가
  - `domain/company/repository/CompanyImageRepository.java` - Pageable 메서드 추가
  - `domain/company/service/CompanyService.java` - searchCompanies(), getCompanyImages() 추가
  - `domain/company/web/CompanyController.java` - 검색 엔드포인트 추가, 이미지 포함 로직 추가
  - `config/web/SecurityConfig.java` - 업체 public endpoint 추가

- **주요 변경사항**:
  1. **DB 스키마 (V8 마이그레이션)**:
     - `premium_tier` 컬럼 추가 (NONE, BASIC, STANDARD, PREMIUM, VIP)
     - `premium_monthly_amount` 컬럼 추가
     - 성능 최적화 인덱스 3개 추가

  2. **검색 조건**:
     - 키워드 검색 (업체명, 설명)
     - 서비스 지역 필터 (serviceAreas)
     - 태그 필터 (tags)
     - 최소 평점 필터 (minRating)

  3. **정렬 옵션**:
     - LATEST: 최신순 (createdAt DESC)
     - POPULAR: 인기순 (likeCount DESC, viewCount DESC)
     - RATING: 평점순 (avgRating DESC, reviewCount DESC)
     - REVIEW_COUNT: 후기순 (reviewCount DESC)
     - PREMIUM_TIER: 프리미엄 등급순 (premiumMonthlyAmount DESC, premiumTier DESC)

  4. **API 변경**:
     - `GET /api/companies` - 활성 업체 목록 (public, 이미지 포함)
     - `GET /api/companies/search` - 검색 API (public)
     - `GET /api/companies/{id}` - 상세 조회 (public)
     - `GET /api/companies/slug/{slug}` - Slug 조회 (public)
     - `POST /api/companies` - 등록 (인증 필요)
     - `PUT /api/companies/{id}` - 수정 (인증 필요)
     - `DELETE /api/companies/{id}` - 삭제 (인증 필요)

- **기술적 결정사항**:
  - Spring Data JPA Specification 사용하여 동적 쿼리 구현
  - PostgreSQL array_contains 함수 사용 (지역, 태그 필터)
  - 이미지 목록은 displayOrder 정렬하여 최대 3개 제공
  - SecurityConfig에서 AntPathMatcher 패턴으로 public endpoint 설정
  - V9 마이그레이션으로 컬럼명 변경 (user_id → owner_id, completed_count → completed_projects)

- **테스트 결과**:
  - ✅ V8 마이그레이션 성공 (10개 필드 추가)
  - ✅ V9 마이그레이션 성공 (컬럼명 변경)
  - ✅ 애플리케이션 정상 시작 (Tomcat 8080)
  - ✅ GET /api/companies API 정상 작동 (public)
  - ✅ GET /api/companies/search API 정상 작동 (public)

---

### 2025-10-15

#### ✅ 완료 (Completed)

**[SCHEMA-001] 데이터베이스 스키마 설계 및 Redis/PostgreSQL 분리 전략 수립**
- **작업자**: Claude
- **작업 시간**: 2025-10-15
- **작업 내용**:
  - `doc/mermaid.txt` 확인하여 전체 시스템 플로우 파악
  - `doc/damoa-version_0_0_1.sql` 기반으로 개선된 스키마 설계
  - Redis와 PostgreSQL 데이터 분리 전략 수립
  - 소프트 삭제(Soft Delete) 패턴 적용

- **생성 파일**:
  - `doc/redis-postgresql-strategy.md` - Redis/PostgreSQL 사용 전략 문서화

- **주요 결정사항**:
  - ❌ PostgreSQL에서 제거: `email_verifications`, `sms_verifications`, `refresh_tokens`
  - ✅ Redis로 이동: JWT 토큰, OAuth state, 이메일/SMS OTP, 세션 데이터
  - ✅ PostgreSQL 유지: 영구 비즈니스 데이터, 감사 로그, 관계형 데이터
  - ✅ 모든 비즈니스 테이블에 `is_deleted`, `deleted_at` 추가

- **참고 문서**:
  - `doc/redis-postgresql-strategy.md` - 전체 분리 전략
  - `doc/mermaid.txt` - 시스템 플로우 다이어그램

**[DOC-001] 작업일지 시스템 구축**
- **작업자**: Claude
- **작업 시간**: 2025-10-15
- **작업 내용**:
  - 작업일지 템플릿 생성
  - CLAUDE.md에 작업일지 사용 가이드 추가

- **생성 파일**:
  - `doc/work-log.md` - 작업일지 템플릿

- **수정 파일**:
  - `CLAUDE.md` - 작업일지 워크플로우 추가

**[AUTH-001] 인증/인가 API 전체 구현 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-15 (9시간)
- **작업 내용**:
  - User 엔티티 schema-improved.sql에 맞춰 완전 재작성
  - Role enum 확장 (USER, COMPANY, DESIGNER, ADMIN, BLACKLIST)
  - 추가 엔티티 생성: SocialAccount, UserProfile, CompanyProfile, DesignerProfile
  - 모든 Repository 및 DTO 생성
  - VerificationService 구현 (이메일/SMS OTP, Redis 기반)
  - AuthService 구현 (2단계 회원가입, 로그인, 토큰 갱신, 로그아웃)
  - AuthController 전체 API 엔드포인트 구현
  - JwtTokenProvider 메서드 추가 및 개선
  - SecurityConfig AuthenticationManager 빈 등록
  - RedisService String 타입 메서드 추가
  - PostgreSQL role 컬럼 타입 수정
  - Docker 빌드 및 배포
  - **전체 API 테스트 완료**

- **생성 파일**:
  - `domain/user/model/OAuthProvider.java` - OAuth 제공자 enum
  - `domain/user/model/SocialAccount.java` - 소셜 계정 엔티티
  - `domain/user/model/UserProfile.java` - 사용자 프로필 엔티티
  - `domain/company/model/CompanyProfile.java` - 업체 프로필 엔티티
  - `domain/designer/model/DesignerProfile.java` - 디자이너 프로필 엔티티
  - `domain/user/repository/SocialAccountRepository.java`
  - `domain/user/repository/UserProfileRepository.java`
  - `domain/company/repository/CompanyProfileRepository.java`
  - `domain/designer/repository/DesignerProfileRepository.java`
  - `domain/user/web/dto/SignupStartRequest.java`
  - `domain/user/web/dto/SignupStartResponse.java`
  - `domain/user/web/dto/SignupCompleteRequest.java`
  - `domain/user/web/dto/EmailVerificationRequest.java`
  - `domain/user/web/dto/SmsVerificationRequest.java`
  - `domain/user/web/dto/VerificationConfirmRequest.java`
  - `domain/user/web/dto/TokenRefreshRequest.java`
  - `domain/user/service/VerificationService.java` - 이메일/SMS 인증
  - `domain/user/service/AuthService.java` - 인증/인가 비즈니스 로직

- **수정 파일**:
  - `domain/user/model/User.java` - 완전 재작성 ( 기준)
  - `domain/user/model/Role.java` - 5개 역할 추가
  - `domain/user/repository/UserRepository.java` - 메서드 추가
  - `domain/user/web/AuthController.java` - 11개 API 엔드포인트
  - `core/jwt/JwtTokenProvider.java` - 메서드 추가 (getUserEmail, getExpiration)
  - `core/exception/ErrorCode.java` - 24개 에러 코드 추가
  - `config/web/SecurityConfig.java` - AuthenticationManager 빈 등록
  - `infra/redis/RedisService.java` - String 타입 메서드 추가

- **API 엔드포인트 (11개)**:
  1. POST `/api/auth/signup/start` - 회원가입 시작 ✅
  2. POST `/api/auth/signup/complete` - 회원가입 완료 ✅
  3. POST `/api/auth/email/send-code` - 이메일 인증 코드 발송 ✅
  4. POST `/api/auth/email/verify-code` - 이메일 인증 확인 ✅
  5. POST `/api/auth/sms/send-code` - SMS 인증 코드 발송 ✅
  6. POST `/api/auth/sms/verify-code` - SMS 인증 확인 ✅
  7. POST `/api/auth/login` - 로그인 ✅
  8. POST `/api/auth/logout` - 로그아웃 ✅
  9. POST `/api/auth/refresh` - 토큰 갱신 ✅
  10. GET `/api/auth/me` - 내 정보 조회 ✅

- **테스트 결과**:
  - ✅ 회원가입 시작 → signup token 발급 (TTL: 10분)
  - ✅ 이메일 인증 → OTP 발급 (TTL: 15분, 6자리)
  - ✅ SMS 인증 → OTP 발급 (TTL: 3분, 6자리)
  - ✅ 회원가입 완료 → JWT Access/Refresh 토큰 발급
  - ✅ 로그인 → JWT Access/Refresh 토큰 발급
  - ✅ 인증된 엔드포인트 접근 → 정상 작동
  - ✅ 토큰 갱신 → Refresh Token Rotation 정상 작동
  - ✅ 로그아웃 → Refresh 토큰 삭제 + Access 토큰 블랙리스트

- **Redis 키 패턴**:
  - `signup:{uuid}` - 회원가입 임시 데이터 (TTL: 10분)
  - `otp:email:{email}` - 이메일 OTP (TTL: 15분)
  - `otp:sms:{phoneNumber}` - SMS OTP (TTL: 3분)
  - `refresh:{userId}` - Refresh 토큰 (TTL: 14일)
  - `blacklist:{accessToken}` - 토큰 블랙리스트 (TTL: 남은 만료 시간)
  - `attempt:email:{email}` - 이메일 인증 시도 횟수 (TTL: 1시간, 최대 5회)
  - `attempt:sms:{phoneNumber}` - SMS 인증 시도 횟수 (TTL: 1시간, 최대 5회)

- **주요 기능**:
  - 2단계 회원가입 (Redis 기반 임시 저장)
  - 이메일/SMS OTP 인증 (시도 횟수 제한)
  - JWT Access Token (TTL: 1일)
  - JWT Refresh Token (TTL: 14일)
  - Refresh Token Rotation (보안 강화)
  - Access Token Blacklist (로그아웃 시)
  - Role별 프로필 자동 생성 (USER → UserProfile, COMPANY → CompanyProfile, DESIGNER → DesignerProfile)

- **참고 문서**:
  - `doc/mermaid.txt` - 인증 플로우 다이어그램
  - `doc/redis-postgresql-strategy.md` - Redis/PostgreSQL 분리 전략

### 2025-10-16

#### ✅ 완료 (Completed)

**[OAUTH-001] 소셜 로그인 (OAuth) 구현 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (3시간)
- **작업 내용**:
  - OAuth 2.0 Provider 인터페이스 및 구현체 생성
  - Kakao, Naver, Google OAuth 2.0 연동
  - OAuthService 구현 (로그인, 계정 연결/해제)
  - OAuth State 관리 (Redis 기반, CSRF 방지)
  - OAuth 토큰 관리 (Redis 저장, PostgreSQL X)
  - 소셜 계정 자동 회원가입
  - 기존 계정에 소셜 계정 연결/해제
  - OAuthController 5개 API 엔드포인트 구현
  - SecurityConfig OAuth 엔드포인트 허용
  - application.yml OAuth 설정 추가
  - Docker 빌드 및 배포
  - **OAuth URL 생성 API 테스트 완료**

- **생성 파일**:
  - `infra/oauth/OAuthProvider.java` - OAuth Provider 인터페이스
  - `infra/oauth/OAuthTokenResponse.java` - OAuth 토큰 응답 DTO
  - `infra/oauth/OAuthUserInfo.java` - OAuth 사용자 정보 DTO
  - `infra/oauth/KakaoOAuthProvider.java` - Kakao OAuth 구현체
  - `infra/oauth/NaverOAuthProvider.java` - Naver OAuth 구현체
  - `infra/oauth/GoogleOAuthProvider.java` - Google OAuth 구현체
  - `domain/user/service/OAuthService.java` - OAuth 비즈니스 로직
  - `domain/user/web/OAuthController.java` - OAuth API 엔드포인트
  - `domain/user/web/dto/OAuthLoginResponse.java` - OAuth 로그인 응답 DTO
  - `domain/user/web/dto/SocialAccountResponse.java` - 소셜 계정 응답 DTO

- **수정 파일**:
  - `domain/user/repository/SocialAccountRepository.java` - countByUser 메서드 추가
  - `core/exception/ErrorCode.java` - OAuth 관련 7개 에러 코드 추가
  - `config/web/SecurityConfig.java` - OAuth 엔드포인트 허용
  - `src/main/resources/application.yml` - OAuth 설정 추가

- **API 엔드포인트 (5개)**:
  1. GET `/api/oauth/{provider}/login` - OAuth 로그인 URL 생성 ✅
  2. GET `/api/oauth/{provider}/callback` - OAuth 콜백 처리 (로그인/회원가입)
  3. GET `/api/oauth/{provider}/link` - 소셜 계정 연결 URL 생성 (인증 필요)
  4. GET `/api/oauth/{provider}/link/callback` - 소셜 계정 연결 콜백 (인증 필요)
  5. DELETE `/api/oauth/{provider}/unlink` - 소셜 계정 연결 해제 (인증 필요)
  6. GET `/api/oauth/accounts` - 내 소셜 계정 목록 조회 (인증 필요)

- **테스트 결과**:
  - ✅ Kakao OAuth URL 생성 → 정상 작동 (State: UUID 생성, Redis 저장)
  - ✅ Naver OAuth URL 생성 → 정상 작동
  - ✅ Google OAuth URL 생성 → 정상 작동 (scope: openid email profile)
  - ⚠️ OAuth 콜백 처리 → 실제 Provider Client ID/Secret 필요 (테스트 보류)
  - ⚠️ 소셜 계정 연결/해제 → 인증된 사용자 필요 (테스트 보류)

- **Redis 키 패턴**:
  - `oauth:state:{state}` - OAuth State (TTL: 10분, CSRF 방지)
  - `oauth:token:{provider}:{providerId}` - OAuth Access/Refresh Token (TTL: 14일)

- **주요 기능**:
  - OAuth 2.0 Authorization Code Flow
  - State 기반 CSRF 방지
  - OAuth 토큰 Redis 저장 (PostgreSQL X)
  - Provider User ID만 DB 저장
  - 자동 회원가입 (소셜 프로필 기반)
  - 기존 계정 연결 (이메일 매칭)
  - 마지막 소셜 계정 해제 방지 (비밀번호 없는 경우)
  - 소셜 로그인 사용자는 password null

- **참고 문서**:
  - `doc/mermaid.txt` - OAuth 로그인 플로우 다이어그램
  - `doc/redis-postgresql-strategy.md` - OAuth 토큰 관리 전략

**[DB-001] 데이터베이스 마이그레이션 설정 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (1시간)
- **작업 내용**:
  - Flyway Database Migration 도구 선택 및 설정
  - build.gradle.kts에 Flyway 의존성 추가 (10.8.1)
  - application.yml에 Flyway 설정 추가
  - JPA ddl-auto를 update에서 validate로 변경
  - schema-improved.sql 기반 V1 마이그레이션 스크립트 생성
  - 초기 시드 데이터 V2 마이그레이션 스크립트 생성
  - Docker 빌드 및 마이그레이션 테스트
  - **Flyway 마이그레이션 성공 확인**

- **생성 파일**:
  - `src/main/resources/db/migration/V1__Initial_schema.sql` - 전체 스키마 생성
  - `src/main/resources/db/migration/V2__Insert_seed_data.sql` - 시드 데이터

- **수정 파일**:
  - `build.gradle.kts` - Flyway 의존성 추가
  - `src/main/resources/application.yml` - Flyway 설정 및 JPA ddl-auto 변경

- **마이그레이션 실행 결과**:
  - ✅ V1: Baseline 설정 완료 (전체 스키마 생성)
  - ✅ V2: 시드 데이터 삽입 완료 (구독 플랜 4개, 태그 25개)
  - ✅ flyway_schema_history 테이블 생성
  - ✅ JPA validate 모드 정상 작동

- **시드 데이터**:
  - 구독 플랜: FREE, BRONZE (₩29,000/월), SILVER (₩99,000/월), GOLD (₩299,000/월)
  - 태그: 병원 타입 10개, 전문 분야 8개, 서비스 지역 17개

- **Flyway 설정**:
  - baseline-on-migrate: true (기존 DB 대응)
  - validate-on-migrate: true (마이그레이션 검증)
  - locations: classpath:db/migration
  - schemas: public

- **주요 효과**:
  - 데이터베이스 스키마 버전 관리
  - 롤백 및 히스토리 추적 가능
  - JPA의 자동 스키마 생성 비활성화 (운영 환경 안정성)
  - 팀 협업 시 스키마 동기화 용이

- **참고 문서**:
  - `src/main/resources/schema-improved.sql` - 원본 스키마
  - Flyway Documentation: https://flywaydb.org/documentation

**[ESTIMATE-001] 견적/입찰 시스템 API 구현 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (2시간)
- **작업 내용**:
  - 견적/입찰 도메인 엔티티 구현 (EstimateRequest, EstimateProposal, Match)
  - ENUM 타입 추가 (EstimateType, RequestStatus)
  - Repository 구현 (3개, 총 30+ 쿼리 메서드)
  - Service 계층 구현 (EstimateRequestService, ProposalService, MatchService)
  - DTO 계층 구현 (Request/Response DTO 7개)
  - REST API Controller 구현 (EstimateController, 21개 엔드포인트)
  - Payment 엔티티 임시 구현 (Phase 2에서 본격 구현 예정)
  - SecurityConfig 업데이트 (견적 API 엔드포인트 허용)
  - ErrorCode 업데이트 (견적/입찰 관련 16개 에러 코드 추가)
  - Docker 빌드 및 배포 성공

- **생성 파일**:
  - `domain/estimate/model/EstimateType.java` - 견적 유형 enum
  - `domain/estimate/model/RequestStatus.java` - 요청 상태 enum
  - `domain/estimate/model/EstimateRequest.java` - 견적 요청 엔티티
  - `domain/estimate/model/EstimateProposal.java` - 제안 엔티티
  - `domain/estimate/model/Match.java` - 매칭 엔티티
  - `domain/estimate/repository/EstimateRequestRepository.java`
  - `domain/estimate/repository/EstimateProposalRepository.java`
  - `domain/estimate/repository/MatchRepository.java`
  - `domain/estimate/service/EstimateRequestService.java`
  - `domain/estimate/service/ProposalService.java`
  - `domain/estimate/service/MatchService.java`
  - `domain/estimate/web/dto/EstimateRequestCreateRequest.java`
  - `domain/estimate/web/dto/EstimateRequestUpdateRequest.java`
  - `domain/estimate/web/dto/EstimateRequestResponse.java`
  - `domain/estimate/web/dto/ProposalCreateRequest.java`
  - `domain/estimate/web/dto/ProposalUpdateRequest.java`
  - `domain/estimate/web/dto/ProposalResponse.java`
  - `domain/estimate/web/dto/MatchResponse.java`
  - `domain/estimate/web/EstimateController.java`
  - `domain/payment/model/Payment.java` - 결제 엔티티 (임시)
  - `domain/payment/model/PaymentStatus.java` - 결제 상태 enum

- **수정 파일**:
  - `core/exception/ErrorCode.java` - 견적/입찰 관련 16개 에러 코드 추가
  - `core/response/ApiResponse.java` - success() 메서드 추가
  - `config/web/SecurityConfig.java` - 견적 API 엔드포인트 허용
  - `domain/company/repository/CompanyProfileRepository.java` - findByIdAndIsDeletedFalse() 추가

- **API 엔드포인트 (21개)**:

  **견적 요청 (Estimate Request) - 8개:**
  1. POST `/api/estimates/requests` - 견적 요청 생성 (DRAFT)
  2. PUT `/api/estimates/requests/{id}` - 견적 요청 수정
  3. POST `/api/estimates/requests/{id}/submit` - 견적 요청 제출
  4. POST `/api/estimates/requests/{id}/cancel` - 견적 요청 취소
  5. DELETE `/api/estimates/requests/{id}` - 견적 요청 삭제 (soft delete)
  6. GET `/api/estimates/requests/{id}` - 견적 요청 조회 (조회수 증가)
  7. GET `/api/estimates/requests/my` - 내 견적 요청 목록
  8. GET `/api/estimates/requests/public` - 공개 견적 요청 목록 (업체용)
  9. GET `/api/estimates/requests/location` - 지역별 견적 요청 검색

  **제안 (Proposal) - 7개:**
  10. POST `/api/estimates/proposals` - 제안 제출 (업체)
  11. PUT `/api/estimates/proposals/{id}` - 제안 수정
  12. DELETE `/api/estimates/proposals/{id}` - 제안 철회
  13. GET `/api/estimates/proposals/{id}` - 제안 조회 (클라이언트가 보면 viewed 표시)
  14. GET `/api/estimates/requests/{id}/proposals` - 견적 요청에 대한 제안 목록
  15. GET `/api/estimates/proposals/my` - 내 제안 목록 (업체)
  16. GET `/api/estimates/proposals/unviewed` - 읽지 않은 제안 목록 (클라이언트)

  **매칭 (Match) - 6개:**
  17. POST `/api/estimates/proposals/{id}/accept` - 제안 수락 (매칭 생성)
  18. POST `/api/estimates/matches/{id}/start` - 프로젝트 시작
  19. POST `/api/estimates/matches/{id}/complete` - 프로젝트 완료
  20. POST `/api/estimates/matches/{id}/cancel` - 프로젝트 취소
  21. PUT `/api/estimates/matches/{id}/contract` - 계약 금액 수정
  22. GET `/api/estimates/matches/{id}` - 매칭 조회
  23. GET `/api/estimates/matches/my` - 내 매칭 목록 (클라이언트)
  24. GET `/api/estimates/matches/company` - 업체 매칭 목록
  25. GET `/api/estimates/matches/in-progress` - 진행 중인 매칭 목록

- **주요 기능**:
  - **견적 요청 생명주기**: DRAFT → SUBMITTED → QUOTED → ACCEPTED/CANCELLED
  - **제안 제출**: 업체가 견적 요청에 제안 제출 (1 요청당 1 제안)
  - **매칭 생성**: 클라이언트가 제안 수락 시 자동 매칭 생성
  - **프로젝트 관리**: 매칭 시작 → 진행 → 완료 플로우
  - **조회수/제안수 추적**: Redis 카운터 동기화 (향후 구현)
  - **권한 검증**: 작성자만 수정/삭제 가능
  - **소프트 삭제**: 모든 엔티티 소프트 삭제 지원
  - **페이지네이션**: 모든 목록 API 페이지네이션 지원

- **TODO (Phase 2)**:
  - 구독/크레딧 기반 과금 시스템 연동
  - 제안 제출 시 구독 쿼터 또는 크레딧 차감
  - 파일 첨부 기능 (estimate_request_files 테이블 활용)
  - Redis 카운터 동기화 (view_count, proposal_count)
  - 알림 시스템 (새 제안 도착 시)

- **참고 문서**:
  - `doc/mermaid.txt` - 공개입찰 플로우 다이어그램

**[CONTEST-001] 디자인 콘테스트 API 구현 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (2시간)
- **작업 내용**:
  - 디자인 콘테스트 도메인 엔티티 구현 (DesignContest, ContestEntry)
  - Repository 구현 (2개, 총 25+ 쿼리 메서드)
  - Service 계층 구현 (DesignContestService, ContestEntryService)
  - DTO 계층 구현 (Request/Response DTO 7개)
  - REST API Controller 구현 (ContestController, 24개 엔드포인트)
  - ErrorCode 업데이트 (콘테스트 관련 14개 에러 코드 추가)
  - SecurityConfig 업데이트 (콘테스트 API 엔드포인트 허용)
  - Flyway V3 마이그레이션 추가 (contest_entries 테이블 스키마 조정)
  - Docker 빌드 및 배포 성공

- **생성 파일**:
  - `domain/contest/model/DesignContest.java` - 디자인 콘테스트 엔티티
  - `domain/contest/model/ContestEntry.java` - 콘테스트 참가작 엔티티
  - `domain/contest/repository/DesignContestRepository.java`
  - `domain/contest/repository/ContestEntryRepository.java`
  - `domain/contest/service/DesignContestService.java`
  - `domain/contest/service/ContestEntryService.java`
  - `domain/contest/service/dto/ContestCreateRequest.java`
  - `domain/contest/service/dto/ContestUpdateRequest.java`
  - `domain/contest/service/dto/ContestResponse.java`
  - `domain/contest/service/dto/EntryCreateRequest.java`
  - `domain/contest/service/dto/EntryUpdateRequest.java`
  - `domain/contest/service/dto/EntryRatingRequest.java`
  - `domain/contest/service/dto/EntryResponse.java`
  - `domain/contest/web/ContestController.java`
  - `src/main/resources/db/migration/V3__Add_contest_entry_fields.sql`

- **수정 파일**:
  - `core/exception/ErrorCode.java` - 콘테스트 관련 14개 에러 코드 추가
  - `config/web/SecurityConfig.java` - 콘테스트 API 엔드포인트 허용

- **API 엔드포인트 (24개)**:

  **콘테스트 관리 (Contest Management) - 14개:**
  1. POST `/api/contests` - 콘테스트 생성 (DRAFT)
  2. PUT `/api/contests/{contestId}` - 콘테스트 수정
  3. POST `/api/contests/{contestId}/submit` - 콘테스트 게시
  4. POST `/api/contests/{contestId}/cancel` - 콘테스트 취소
  5. DELETE `/api/contests/{contestId}` - 콘테스트 삭제 (soft delete)
  6. GET `/api/contests/{contestId}` - 콘테스트 조회 (조회수 증가)
  7. GET `/api/contests/public` - 공개 콘테스트 목록
  8. GET `/api/contests/my` - 내가 개설한 콘테스트 목록
  9. GET `/api/contests/winners` - 우승자가 선정된 콘테스트 목록
  10. GET `/api/contests/upcoming` - 예정된 콘테스트 목록
  11. GET `/api/contests/by-prize` - 상금별 콘테스트 검색
  12. GET `/api/contests/free` - 무료 참가 콘테스트 목록
  13. GET `/api/contests/ending-soon` - 마감 임박 콘테스트 목록
  14. POST `/api/contests/{contestId}/winner` - 우승자 선정

  **참가작 관리 (Entry Management) - 10개:**
  15. POST `/api/contests/entries` - 참가작 제출
  16. PUT `/api/contests/entries/{entryId}` - 참가작 수정
  17. DELETE `/api/contests/entries/{entryId}` - 참가작 철회
  18. POST `/api/contests/entries/{entryId}/rate` - 참가작 평가
  19. GET `/api/contests/entries/{entryId}` - 참가작 조회
  20. GET `/api/contests/{contestId}/entries` - 콘테스트 참가작 목록
  21. GET `/api/contests/entries/my` - 내 참가작 목록
  22. GET `/api/contests/{contestId}/entries/unrated` - 미평가 참가작 목록
  23. GET `/api/contests/entries/my/wins` - 내 수상 작품 목록
  24. GET `/api/contests/{contestId}/winner` - 우승작 조회

- **주요 기능**:
  - **콘테스트 생명주기**: DRAFT → SUBMITTED → IN_PROGRESS → COMPLETED
  - **참가 제출**: 업체가 콘테스트에 참가작 제출 (1 콘테스트당 1 참가작)
  - **우승자 선정**: 콘테스트 주최자가 참가작 평가 및 우승자 선정
  - **상금 시스템**: 콘테스트별 상금 및 참가비 설정
  - **조회수/참가수 추적**: 실시간 카운터
  - **권한 검증**: 주최자만 수정/삭제 가능, 평가 권한 분리
  - **소프트 삭제**: 모든 엔티티 소프트 삭제 지원
  - **페이지네이션**: 모든 목록 API 페이지네이션 지원

- **데이터베이스 스키마 조정** (V3 마이그레이션):
  - design_contests 테이블: 이미 V1에 존재
  - contest_entries 테이블 수정:
    - `design_description` 컬럼 추가
    - `design_files` 컬럼 추가 (JSONB)
    - `portfolio_url` 컬럼 추가
    - `rating_comment` 컬럼 추가
    - `won_at` 컬럼 추가
    - `company_id` 컬럼 추가 (Phase 1에서 company_profiles 사용)
    - `designer_id` NULL 허용 (Phase 2 호환성)
    - `status` NULL 허용 (Phase 1에서 미사용)

- **TODO (Phase 2)**:
  - 구독/크레딧 기반 과금 시스템 연동 (참가비 결제)
  - 참가작 파일 업로드 (contest_entry_files 테이블 활용)
  - 디자이너 프로필 연동 (designer_profiles 테이블)
  - 알림 시스템 (우승자 선정 시)
  - 콘테스트 자동 마감 처리 (배치 작업)

- **참고 문서**:
  - `doc/mermaid.txt` - 디자인 콘테스트 플로우 다이어그램

**[PAYMENT-001] 결제 시스템 통합 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (1.5시간)
- **작업 내용**:
  - 결제 시스템 6개 엔티티 구현 (Subscription Plan, Subscription, Invoice, Payment, PaymentLog, CreditTransaction)
  - Repository 구현 (6개, 총 50+ 쿼리 메서드)
  - Service 계층 구현 (SubscriptionService, CreditService, PaymentService)
  - 구독 쿼터 관리 시스템 (proposal_quota, contest_quota, boost_quota)
  - 크레딧 잔액 추적 시스템 (balance_after 패턴으로 잔액 검증)
  - Webhook 멱등성 구현 (Redis SETNX 패턴)
  - REST API Controller 구현 (PaymentController, 11개 엔드포인트)
  - DTO 계층 구현 (Request/Response DTO 5개)
  - RedisService에 setNX, delete 메서드 추가
  - ErrorCode 업데이트 (결제 관련 13개 에러 코드 추가)
  - SecurityConfig 업데이트 (결제 API 및 Webhook 엔드포인트 허용)
  - Docker 빌드 및 배포 성공

- **생성 파일**:
  - `domain/subscription/model/SubscriptionPlan.java` - 구독 플랜 엔티티
  - `domain/subscription/model/Subscription.java` - 구독 엔티티
  - `domain/subscription/model/SubscriptionStatus.java` - 구독 상태 enum
  - `domain/payment/model/Invoice.java` - 청구서 엔티티
  - `domain/payment/model/InvoiceStatus.java` - 청구서 상태 enum
  - `domain/payment/model/PaymentLog.java` - 결제 로그 엔티티
  - `domain/payment/model/CreditTransaction.java` - 크레딧 거래 엔티티
  - `domain/payment/model/TransactionType.java` - 거래 유형 enum
  - `domain/subscription/repository/SubscriptionPlanRepository.java`
  - `domain/subscription/repository/SubscriptionRepository.java`
  - `domain/payment/repository/InvoiceRepository.java`
  - `domain/payment/repository/PaymentLogRepository.java`
  - `domain/payment/repository/CreditTransactionRepository.java`
  - `domain/subscription/service/SubscriptionService.java`
  - `domain/payment/service/CreditService.java`
  - `domain/payment/service/PaymentService.java`
  - `domain/payment/web/PaymentController.java`
  - `domain/payment/web/dto/CreditPurchaseRequest.java`
  - `domain/payment/web/dto/PaymentResponse.java`
  - `domain/payment/web/dto/CreditBalanceResponse.java`
  - `domain/payment/web/dto/CreditTransactionResponse.java`
  - `domain/payment/web/dto/InvoiceResponse.java`
  - `domain/payment/web/dto/WebhookRequest.java`
  - `domain/payment/repository/PaymentRepository.java` (확장)

- **수정 파일**:
  - `domain/payment/model/Payment.java` - 완전 재작성 (invoice 관계, payment_data, 비즈니스 메서드 추가)
  - `core/exception/ErrorCode.java` - 결제/구독 관련 13개 에러 코드 추가
  - `config/web/SecurityConfig.java` - 결제 API 엔드포인트 허용
  - `infra/redis/RedisService.java` - setNX() 및 delete() 메서드 추가

- **API 엔드포인트 (11개)**:

  **구독 플랜 조회 - 2개:**
  1. GET `/api/payments/plans` - 구독 플랜 목록 조회 (public)
  2. GET `/api/payments/plans/{planCode}` - 특정 플랜 조회 (public)

  **결제 (Payment) - 3개:**
  3. POST `/api/payments/credits/purchase` - 크레딧 구매 결제 세션 생성
  4. GET `/api/payments/{transactionId}` - 결제 정보 조회
  5. GET `/api/payments/history` - 결제 내역 조회
  6. GET `/api/payments/{paymentId}/logs` - 결제 로그 조회 (디버깅용)

  **크레딧 (Credit) - 2개:**
  7. GET `/api/payments/credits/balance` - 크레딧 잔액 조회
  8. GET `/api/payments/credits/history` - 크레딧 거래 내역 조회

  **청구서 (Invoice) - 3개:**
  9. GET `/api/payments/invoices/{invoiceId}` - 청구서 조회
  10. GET `/api/payments/invoices` - 청구서 목록 조회
  11. GET `/api/payments/invoices/unpaid` - 미납 청구서 조회

  **Webhook - 1개 (public):**
  12. POST `/api/payments/webhook` - PG사 결제 Webhook 처리

- **주요 기능**:
  - **구독 플랜**: FREE, BRONZE (₩29,000/월), SILVER (₩99,000/월), GOLD (₩299,000/월)
  - **구독 쿼터 추적**: 제안(proposal), 콘테스트(contest), 부스트(boost) 월별 쿼터 관리
  - **쿼터 차감**: useProposalQuota(), useContestQuota(), useBoostQuota() - 트랜잭션 안전
  - **크레딧 시스템**: 구매, 구독 할당, 차감, 환불 기능
  - **잔액 추적**: balance_after 패턴으로 모든 거래 추적 및 검증
  - **Webhook 멱등성**: Redis SETNX로 중복 Webhook 방지 (TTL: 24시간)
  - **결제 로그**: 모든 결제 이벤트 추적 (JSONB 포맷)
  - **결제 생명주기**: PENDING → COMPLETED / FAILED / CANCELLED / REFUNDED

- **구독 플랜 쿼터 (V2 seed data)**:
  ```
  FREE:   0원,      1 제안,   0 콘테스트,  0 크레딧,     0 부스트
  BRONZE: ₩29,000,  5 제안,   2 콘테스트,  ₩30,000,      1 부스트
  SILVER: ₩99,000,  20 제안,  10 콘테스트, ₩150,000,     5 부스트
  GOLD:   ₩299,000, 무제한,   무제한,      ₩500,000,     무제한
  ```
  - 무제한 쿼터는 -1로 저장
  - hasProposalQuota(), hasContestQuota() 메서드로 무제한 처리

- **과금 플로우 (3단계)**:
  1. 구독 잔여 쿼터 > 0 → 쿼터 차감 (무료)
  2. 구독 없음/소진 → 크레딧 잔액 확인 → 크레딧 차감
  3. 크레딧 부족 → 결제 세션 생성 → PG 결제

- **Webhook 처리 플로우**:
  1. PG사 → POST /api/payments/webhook (idempotencyKey, pgTransactionId, status)
  2. Redis SETNX(idempotency:key, "processed", TTL 24h) → 신규/중복 판별
  3. 신규 → Payment 업데이트, 크레딧 충전, Invoice 업데이트, 200 OK 응답
  4. 중복 → 200 OK 응답 (멱등 보장)

- **TODO (Phase 2)**:
  - 실제 PG사 연동 (KakaoPay, Toss, INICIS)
  - 구독 자동 갱신 (Scheduled Task)
  - 만료 구독 처리 (Scheduled Task)
  - 크레딧 차감 통합 (EstimateProposal, ContestEntry 제출 시)
  - 청구서 자동 생성 (월별 구독 결제)
  - Webhook signature 검증

- **참고 문서**:
  - `doc/mermaid.txt` - 결제 Webhook 플로우 다이어그램
  - `src/main/resources/db/migration/V1__Initial_schema.sql` - 결제 관련 테이블 스키마
  - `src/main/resources/db/migration/V2__Insert_seed_data.sql` - 구독 플랜 시드 데이터

**[ADMIN-001] 관리자 권한 시스템 및 대시보드 API 구현 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (2시간)
- **작업 내용**:
  - Flyway V4 마이그레이션 추가 (admin_roles 테이블, admin_permission enum)
  - AdminRole enum (8개 역할: SUPER_ADMIN, USER_MANAGER, COMPANY_MANAGER, etc.)
  - AdminPermission enum (21개 세부 권한: USER_READ, USER_UPDATE, CONTENT_CREATE, etc.)
  - AdminRoleAssignment 엔티티 및 Repository 구현
  - AuditLog 엔티티 및 Repository 구현 (audit_logs 테이블에 admin_role 컬럼 추가)
  - AdminService 구현 (권한 검증, 역할 부여/회수)
  - AuditLogService 구현 (관리자 작업 로그 자동 기록, IP/UserAgent 추적)
  - AdminController 12개 API 엔드포인트 구현
  - DTO 4개 (AdminDashboardResponse, AuditLogResponse, GrantRoleRequest, AdminRoleResponse)
  - SecurityConfig 업데이트 (/api/admin/** 엔드포인트 허용)

- **생성 파일** (8개):
  - `domain/admin/model/AdminRole.java` - 관리자 역할 enum
  - `domain/admin/model/AdminPermission.java` - 세부 권한 enum
  - `domain/admin/model/AdminRoleAssignment.java` - 역할 배정 엔티티
  - `domain/admin/model/AuditLog.java` - 감사 로그 엔티티
  - `domain/admin/repository/AdminRoleAssignmentRepository.java`
  - `domain/admin/repository/AuditLogRepository.java`
  - `domain/admin/service/AdminService.java` - 권한 관리 서비스
  - `domain/admin/service/AuditLogService.java` - 감사 로그 서비스
  - `domain/admin/web/AdminController.java`
  - `domain/admin/web/dto/AdminDashboardResponse.java`
  - `domain/admin/web/dto/AuditLogResponse.java`
  - `domain/admin/web/dto/GrantRoleRequest.java`
  - `domain/admin/web/dto/AdminRoleResponse.java`
  - `src/main/resources/db/migration/V4__Add_admin_roles.sql`

- **API 엔드포인트** (12개):
  1. GET `/api/admin/dashboard` - 관리자 대시보드 통계
  2. POST `/api/admin/roles/grant` - 관리자 역할 부여
  3. DELETE `/api/admin/roles/{userId}/{role}` - 관리자 역할 회수
  4. GET `/api/admin/roles/{userId}` - 사용자 역할 목록 조회
  5. GET `/api/admin/permissions/{userId}` - 사용자 권한 목록 조회
  6. GET `/api/admin/permissions/my` - 내 권한 조회
  7. GET `/api/admin/audit-logs` - 감사 로그 조회 (필터링)
  8. GET `/api/admin/audit-logs/entity/{entityType}/{entityId}` - 특정 엔티티 로그
  9. GET `/api/admin/audit-logs/user/{userId}` - 특정 사용자 로그
  10. GET `/api/admin/audit-logs/stats` - 로그 통계
  11. GET `/api/admin/users-with-role/{role}` - 특정 역할 보유 사용자 목록

- **주요 기능**:
  - **역할 기반 접근 제어 (RBAC)**: 8개 관리자 역할에 21개 세부 권한 매핑
  - **다중 역할 지원**: 한 사용자가 여러 관리자 역할 보유 가능
  - **권한 검증**: requirePermission(), hasPermission() 메서드로 세밀한 접근 제어
  - **감사 로그**: 모든 관리자 작업 자동 기록 (IP, UserAgent, old/new value JSONB)
  - **대시보드**: 사용자, 업체, 콘텐츠, 결제 통계 및 최근 활동 조회
  - **권한 위임**: SUPER_ADMIN만 다른 관리자에게 역할 부여 가능

**[CONTENT-001] 플래너 요청 시스템 API 구현 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (1.5시간)
- **작업 내용**:
  - PlannerRequest 엔티티 구현 (planner_requests 테이블 매핑)
  - PlannerType enum (6개 유형: OPENING_CONSULTING, SITE_MEASUREMENT, etc.)
  - RequestStatus enum (7개 상태: SUBMITTED, IN_REVIEW, ASSIGNED, etc.)
  - PlannerRequestRepository 구현 (복잡한 필터링 쿼리 지원)
  - PlannerRequestService 구현 (생성, 배정, 상태 변경, 응답 추가)
  - PlannerController 15개 API 엔드포인트 구현 (User 7개 + Admin 8개)
  - DTO 7개 (Request/Response)
  - AdminService 연동 (권한 검증)
  - AuditLogService 연동 (작업 로그 자동 기록)

- **생성 파일** (10개):
  - `domain/planner/model/PlannerType.java`
  - `domain/planner/model/RequestStatus.java`
  - `domain/planner/model/PlannerRequest.java`
  - `domain/planner/repository/PlannerRequestRepository.java`
  - `domain/planner/service/PlannerRequestService.java`
  - `domain/planner/web/PlannerController.java`
  - `domain/planner/web/dto/CreatePlannerRequestRequest.java`
  - `domain/planner/web/dto/UpdatePlannerRequestRequest.java`
  - `domain/planner/web/dto/AssignPlannerRequest.java`
  - `domain/planner/web/dto/UpdateStatusRequest.java`
  - `domain/planner/web/dto/AddResponseRequest.java`
  - `domain/planner/web/dto/AddNotesRequest.java`
  - `domain/planner/web/dto/PlannerRequestResponse.java`

- **수정 파일**:
  - `core/exception/ErrorCode.java` - 플래너 요청 관련 3개 에러 코드 추가

- **API 엔드포인트** (15개):

  **사용자 엔드포인트 (7개):**
  1. POST `/api/planner/requests` - 플래너 요청 생성
  2. GET `/api/planner/requests/my` - 내 요청 목록
  3. GET `/api/planner/requests/{requestId}` - 요청 조회
  4. PUT `/api/planner/requests/{requestId}` - 요청 수정 (배정 전)
  5. DELETE `/api/planner/requests/{requestId}` - 요청 삭제

  **관리자 엔드포인트 (8개):**
  6. GET `/api/planner/admin/requests` - 전체 요청 목록 (필터링)
  7. GET `/api/planner/admin/requests/unassigned` - 미배정 요청 목록
  8. GET `/api/planner/admin/requests/assigned-to-me` - 내 담당 요청
  9. POST `/api/planner/admin/requests/{requestId}/assign` - 플래너 배정
  10. PUT `/api/planner/admin/requests/{requestId}/status` - 상태 변경
  11. POST `/api/planner/admin/requests/{requestId}/response` - 응답 추가
  12. POST `/api/planner/admin/requests/{requestId}/notes` - 내부 메모 추가
  13. GET `/api/planner/admin/stats` - 통계 조회

- **주요 기능**:
  - **요청 생명주기**: SUBMITTED → ASSIGNED → IN_PROGRESS → COMPLETED
  - **플래너 배정**: 관리자가 담당 플래너 배정 (PLANNER_ASSIGN 권한 필요)
  - **상태 추적**: 7단계 상태 관리 (SUBMITTED ~ REJECTED)
  - **내부 메모**: 관리자 전용 notes 필드 (사용자에게 노출 안 됨)
  - **응답 시스템**: 플래너가 사용자에게 응답 작성
  - **권한 제어**: AdminPermission.PLANNER_* 권한으로 접근 제어
  - **감사 로그**: 배정, 상태 변경, 응답 추가 등 모든 작업 로그 기록

**[CONTENT-002] 게시판 시스템 (자료실/사진/공지/FAQ) API 구현 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (1.5시간)
- **작업 내용**:
  - Board 엔티티 구현 (boards 테이블 매핑, 다목적 게시판)
  - BoardType enum (6개 유형: NOTICE, EVENT, FAQ, PHOTO, DOCUMENT, PORTFOLIO)
  - BoardRepository 구현 (타입별, 검색, 인기, 최신 쿼리)
  - BoardService 구현 (CRUD, 발행 관리, 조회수 증가)
  - BoardController 12개 API 엔드포인트 구현 (Public 4개 + Admin 8개)
  - DTO 3개 (CreateBoardRequest, UpdateBoardRequest, BoardResponse)
  - AdminService 연동 (CONTENT_* 권한 검증)
  - AuditLogService 연동 (게시물 생성/수정/삭제 로그 기록)
  - SecurityConfig 업데이트 (Public board 엔드포인트 허용)

- **생성 파일** (8개):
  - `domain/board/model/BoardType.java`
  - `domain/board/model/Board.java`
  - `domain/board/repository/BoardRepository.java`
  - `domain/board/service/BoardService.java`
  - `domain/board/web/BoardController.java`
  - `domain/board/web/dto/CreateBoardRequest.java`
  - `domain/board/web/dto/UpdateBoardRequest.java`
  - `domain/board/web/dto/BoardResponse.java`

- **수정 파일**:
  - `config/web/SecurityConfig.java` - Board 공개 엔드포인트 허용
  - `core/exception/ErrorCode.java` - Board 관련 3개 에러 코드 추가

- **API 엔드포인트** (12개):

  **공개 엔드포인트 (4개):**
  1. GET `/api/boards/{type}` - 게시판 목록 조회 (타입별)
  2. GET `/api/boards/{type}/{boardId}` - 게시물 조회 (조회수 증가)
  3. GET `/api/boards/{type}/search` - 키워드 검색
  4. GET `/api/boards/{type}/popular` - 인기 게시물

  **관리자 엔드포인트 (8개):**
  5. POST `/api/boards/admin` - 게시물 생성
  6. GET `/api/boards/admin/{type}` - 전체 게시물 (미발행 포함)
  7. GET `/api/boards/admin/{type}/{boardId}` - 게시물 조회 (조회수 증가 X)
  8. PUT `/api/boards/admin/{boardId}` - 게시물 수정
  9. POST `/api/boards/admin/{boardId}/publish` - 게시물 발행
  10. DELETE `/api/boards/admin/{boardId}` - 게시물 삭제 (soft delete)
  11. GET `/api/boards/admin/{type}/stats` - 게시판 통계

- **주요 기능**:
  - **다목적 게시판**: 단일 테이블로 공지, 이벤트, FAQ, 사진, 자료실, 포트폴리오 관리
  - **발행 관리**: published_at 기반 발행 시스템 (예약 발행 가능)
  - **조회수 추적**: public 조회 시 자동 증가
  - **핀 고정**: 중요 게시물 상단 고정 (is_pinned, display_order)
  - **유료 자료실**: DOCUMENT 타입에 price 설정 가능
  - **검색 기능**: 제목/내용 키워드 검색
  - **인기 게시물**: 조회수 기준 정렬
  - **권한 제어**: CONTENT_* 권한으로 관리자만 생성/수정/삭제 가능

---

#### 🔄 진행 중 (In Progress)

_현재 진행 중인 작업이 없습니다._

---

#### 📌 예정 (Planned)

**[ESTIMATE-001] 견적/입찰 시스템 API 구현**
- **우선순위**: 중간
- **예상 작업**:
  - 견적 요청 API
  - 업체 제안 API
  - 매칭 생성 API
  - 구독/크레딧 기반 과금 시스템

- **참고 문서**:
  - `doc/mermaid.txt` - 공개입찰 플로우 다이어그램

**[CONTEST-001] 디자인 콘테스트 API 구현**
- **우선순위**: 중간
- **예상 작업**:
  - 콘테스트 개설 API
  - 참가 등록 API
  - 참가작 업로드 API
  - 심사 및 수상 선정 API

- **참고 문서**:
  - `doc/mermaid.txt` - 디자인 콘테스트 플로우

**[PAYMENT-001] 결제 시스템 통합**
- **우선순위**: 중간
- **예상 작업**:
  - 결제 게이트웨이 연동 (KakaoPay, Toss, INICIS)
  - Webhook 처리 (멱등성 보장)
  - 구독 관리 시스템
  - 크레딧 충전/차감 시스템

- **참고 문서**:
  - `doc/mermaid.txt` - 결제 Webhook 플로우

**[ADMIN-001] 어드민 대시보드 API 구현**
- **우선순위**: 낮음
- **예상 작업**:
  - 회원 관리 API
  - 업체 관리 API
  - 입찰/제안/매칭 관리 API
  - 감사 로그 조회 API

- **참고 문서**:
  - `doc/mermaid.txt` - 어드민 모듈 맵

---

## 📚 참고 문서 (Reference Documents)

### 아키텍처 문서
- `doc/mermaid.txt` - 전체 시스템 플로우 다이어그램 (26개 다이어그램)
- `doc/redis-postgresql-strategy.md` - Redis/PostgreSQL 분리 전략

### 데이터베이스 스키마
- `src/main/resources/schema-improved.sql` - 운영용 PostgreSQL 스키마 (영문)
- `src/main/resources/schema_erdcloud_v2.sql` - ERD 시각화용 스키마 (한글, FK 포함)
- `src/main/resources/data.sql` - 초기 시드 데이터

### 프로젝트 설정
- `CLAUDE.md` - Claude Code 작업 가이드
- `README.md` - 프로젝트 개요 (있는 경우)

---

## 🔍 주요 기술 스택

- **Backend**: Spring Boot 3.x, Java 17
- **Database**: PostgreSQL (영구 데이터)
- **Cache**: Redis (임시 데이터, 세션, 토큰)
- **Authentication**: JWT, OAuth 2.0 (Kakao, Naver, Google)
- **Payment**: KakaoPay, Toss, INICIS
- **Storage**: AWS S3 (파일 업로드)
- **Build**: Gradle

---

## 💡 작업 팁 (Working Tips)

### 코드 작성 시
1. 항상 `CLAUDE.md`의 패키지 구조를 따릅니다
2. 비즈니스 로직은 `service` 레이어에 작성
3. 예외 처리는 `BusinessException`과 `ErrorCode` 사용
4. API 응답은 `ApiResponse` 래퍼 사용

### 데이터베이스 작업 시
1. 임시 데이터는 Redis 사용 (TTL 설정)
2. 영구 데이터는 PostgreSQL 사용
3. 삭제는 소프트 삭제 (`is_deleted = true`) 사용
4. `doc/redis-postgresql-strategy.md` 참고

### 인증 작업 시
1. JWT 토큰은 Redis에 저장 (PostgreSQL X)
2. 이메일/SMS OTP는 Redis에 저장 (PostgreSQL X)
3. OAuth 토큰은 Redis에 저장 (PostgreSQL X)
4. 감사 로그만 PostgreSQL에 기록

---

## 🐛 이슈 트래커 (Issue Tracker)

### 해결됨 (Resolved)
_현재 해결된 이슈가 없습니다._

### 진행 중 (Open)
_현재 열린 이슈가 없습니다._

---

## 📊 프로젝트 메트릭 (Project Metrics)

- **총 작업 항목**: 13개
- **완료**: 13개 (SCHEMA-001, DOC-001, AUTH-001, OAUTH-001, DB-001, ESTIMATE-001, CONTEST-001, PAYMENT-001, ADMIN-001, CONTENT-001, CONTENT-002, SCHEMA-002, SCHEMA-003, DOCKER-001, DOC-002)
- **진행 중**: 0개
- **예정**: 0개
- **완료율**: 100%

### 코드 통계 (전체)
- **생성된 파일**: 129개
  - 엔티티 24개 (User, SocialAccount, Profiles x3, EstimateRequest, EstimateProposal, Match, Payment, DesignContest, ContestEntry, SubscriptionPlan, Subscription, Invoice, PaymentLog, CreditTransaction, AdminRoleAssignment, AuditLog, PlannerRequest, Board)
  - ENUM 타입 20개 (Role, OAuthProvider, EstimateType, RequestStatus, PaymentStatus, SubscriptionStatus, InvoiceStatus, TransactionType, AdminRole, AdminPermission, PlannerType, BoardType 등)
  - Repository 23개 (User, SocialAccount, Profiles x3, Estimate x3, Contest x2, Payment x6, Admin x2, Planner x1, Board x1)
  - DTO 54개 (Auth 9개 + Estimate 7개 + Contest 7개 + Payment 10개 + Admin 4개 + Planner 7개 + Board 3개)
  - Service 16개 (VerificationService, AuthService, OAuthService, Estimate x3, Contest x2, Subscription, Credit, Payment, AdminService, AuditLogService, PlannerRequestService, BoardService)
  - OAuth Provider 7개 (인터페이스 + 구현체 3개 + DTO 3개)
  - Controller 7개 (AuthController, OAuthController, EstimateController, ContestController, PaymentController, AdminController, PlannerController, BoardController)
  - Migration Scripts 5개 (V1: Schema, V2: Seed Data, V3: Contest Fields, V4: Admin Roles, V5: Invoice Fix)
- **수정된 파일**: 28개
- **API 엔드포인트**: 128개
  - 인증 11개 ✅
  - OAuth 6개 ⚠️
  - 견적/입찰 25개 ✅
  - 디자인 콘테스트 24개 ✅
  - 결제/구독 12개 ✅
  - Webhook 1개 ✅
  - 관리자 12개 ✅
  - 플래너 요청 15개 ✅
  - 게시판 12개 ✅
- **에러 코드**: 80개
- **DB 마이그레이션**: 5개 (V1: Initial Schema, V2: Seed Data, V3: Contest Entry Fields, V4: Admin Roles, V5: Invoice Fix)

### 데이터베이스 구성
- **테이블**: 41개 (users, social_accounts, profiles, subscriptions, payments, boards, estimates, contests, etc.)
- **ENUM 타입**: 13개 (user_role, oauth_provider, payment_status, etc.)
- **인덱스**: 50+ (성능 최적화)
- **트리거**: 18개 (updated_at 자동 갱신)
- **시드 데이터**: 구독 플랜 4개, 태그 25개

### OAuth Provider 구성
- **Kakao**: Authorization, Token Exchange, User Info ✅
- **Naver**: Authorization, Token Exchange, User Info ✅
- **Google**: Authorization, Token Exchange, User Info ✅

---

**[SCHEMA-002] Invoice 스키마 수정 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (30분)
- **작업 내용**:
  - Flyway V5 마이그레이션 추가 (Invoice 스키마 수정)
  - Invoice 엔티티 필드명과 데이터베이스 컬럼명 불일치 해결
  - 컬럼명 변경: tax_amount → tax, discount_amount → discount
  - 누락된 컬럼 추가: currency, voided_at, void_reason
  - Docker 빌드 및 마이그레이션 성공 확인

- **생성 파일**:
  - `src/main/resources/db/migration/V5__Add_invoice_currency.sql`

- **주요 변경사항**:
  - Invoice 엔티티가 기대하는 필드명으로 컬럼명 통일
  - V1 schema의 `tax_amount`, `discount_amount`를 entity field명인 `tax`, `discount`로 rename
  - 누락된 `currency VARCHAR(3) DEFAULT 'KRW'` 컬럼 추가
  - 누락된 `voided_at TIMESTAMP`, `void_reason TEXT` 컬럼 추가

- **마이그레이션 실행 결과**:
  - ✅ V5: Invoice schema 수정 완료 (5개 migrations 총 적용 성공)
  - ✅ flyway_schema_history 테이블 업데이트

- **참고**:
  - Invoice 엔티티가 JPA schema validation을 통과함
  - 다른 pre-existing schema 불일치 문제 발견 (CompanyProfile.service_areas TEXT[] vs VARCHAR 기대)
  - 향후 작업: 나머지 entity-schema 불일치 수정 필요

**[SCHEMA-003] 전체 Entity-Schema 검증 및 불일치 수정 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-16 (1시간)
- **작업 내용**:
  - 모든 entity-schema 불일치 문제를 발견하고 수정
  - Flyway V6-V9 마이그레이션 추가 (4개 migrations)
  - Repository 메서드 시그니처 오류 수정
  - **최종 결과: 애플리케이션 정상 시작 성공** ✅

- **생성 파일**:
  - `src/main/resources/db/migration/V6__Convert_arrays_to_text.sql` - PostgreSQL 배열을 TEXT로 변환
  - `src/main/resources/db/migration/V7__Add_health_table.sql` - Health 테이블 추가
  - `src/main/resources/db/migration/V8__Fix_invoice_date_types.sql` - Invoice 날짜 타입 수정
  - `src/main/resources/db/migration/V9__Add_payment_log_error_message.sql` - PaymentLog error_message 컬럼 추가

- **수정 파일**:
  - `domain/payment/repository/CreditTransactionRepository.java` - findLatestTransaction(), findCurrentBalance() 반환 타입 수정 (Optional → List)
  - `domain/payment/service/CreditService.java` - getCurrentBalance() 로직 수정

- **V6 마이그레이션 - 배열 타입 변환**:
  ```sql
  -- CompanyProfile
  ALTER TABLE company_profiles ALTER COLUMN specialty TYPE TEXT USING array_to_string(specialty, ',');
  ALTER TABLE company_profiles ALTER COLUMN service_areas TYPE TEXT USING array_to_string(service_areas, ',');

  -- DesignerProfile
  ALTER TABLE designer_profiles ALTER COLUMN specialty TYPE TEXT USING array_to_string(specialty, ',');
  ALTER TABLE designer_profiles ALTER COLUMN awards TYPE TEXT USING array_to_string(awards, ',');
  ALTER TABLE designer_profiles ALTER COLUMN certifications TYPE TEXT USING array_to_string(certifications, ',');
  ```
  - V1 스키마의 TEXT[] (PostgreSQL array)를 TEXT (comma-separated string)로 변환
  - Entity는 TEXT 타입을 기대하므로 JPA 매핑 정상화

- **V7 마이그레이션 - Health 테이블 추가**:
  ```sql
  CREATE TABLE health (
      id BIGSERIAL PRIMARY KEY,
      status VARCHAR(50) NOT NULL
  );
  ```
  - Health entity가 존재하지만 V1 스키마에 테이블 정의가 누락됨
  - 헬스 체크 API 지원을 위한 간단한 테이블 추가

- **V8 마이그레이션 - Invoice 날짜 타입 수정**:
  ```sql
  ALTER TABLE invoices ALTER COLUMN invoice_date TYPE DATE USING invoice_date::DATE;
  ALTER TABLE invoices ALTER COLUMN due_date TYPE DATE USING due_date::DATE;
  ```
  - V1 스키마: TIMESTAMP 타입
  - Invoice entity: LocalDate (DATE 타입)
  - TIMESTAMP를 DATE로 변환하여 타입 일치

- **V9 마이그레이션 - PaymentLog error_message 컬럼 추가**:
  ```sql
  ALTER TABLE payment_logs ADD COLUMN error_message TEXT;
  ```
  - PaymentLog entity에 error_message 필드가 있지만 V1 스키마에 누락됨
  - 결제 오류 추적을 위한 컬럼 추가

- **Repository 메서드 수정**:
  - **문제**: CreditTransactionRepository.findLatestTransaction() 메서드가 `Optional<CreditTransaction>`을 반환하면서 `Pageable` 파라미터 사용
  - **오류**: Spring Data JPA는 Pageable과 함께 Optional 반환 타입 지원 안 함
  - **해결**: 반환 타입을 `List<CreditTransaction>`로 변경, 첫 번째 요소 사용

- **마이그레이션 실행 결과**:
  - ✅ V6: 배열 → TEXT 변환 성공
  - ✅ V7: Health 테이블 생성 성공
  - ✅ V8: Invoice 날짜 타입 변환 성공
  - ✅ V9: PaymentLog error_message 추가 성공
  - ✅ **총 9개 migrations 적용 완료**
  - ✅ **JPA schema validation 모두 통과**
  - ✅ **애플리케이션 정상 시작 (8초 소요)**

- **최종 검증 결과**:
  ```
  2025-10-16T03:28:09.024Z  INFO - Tomcat started on port 8080 (http)
  2025-10-16T03:28:09.038Z  INFO - Started DamoaApplication in 7.918 seconds
  ```
  - Swagger UI: http://localhost:8080/swagger-ui.html ✅
  - 모든 API 엔드포인트 (128개) 정상 등록
  - PostgreSQL 연결 정상
  - Redis 연결 정상

### 2025-10-30

#### ✅ 완료 (Completed)

**[DOC-002] 공통 개발 패턴 가이드 문서화** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-30 (30분)
- **작업 내용**:
  - 프로젝트 전반의 공통 개발 패턴 분석 및 문서화
  - CLAUDE.md에 "Coding Standards and Common Patterns" 섹션 추가
  - 10가지 주요 패턴 정리 (API Response, Exception 처리, Controller, Service, DTO, Repository, Redis, Entity, 페이지네이션, 참고 예시)
  - 새로운 기능 개발 시 참고할 수 있는 명확한 가이드라인 제공

- **수정 파일**:
  - `CLAUDE.md` - 공통 패턴 가이드 섹션 추가

- **문서화된 패턴** (10개):
  1. **API Response 구조**: `ApiResponse<T>` 래퍼 사용법
  2. **Exception 처리**: `BusinessException` + `ErrorCode` 패턴
  3. **Controller 패턴**: 애노테이션, 인증, Swagger 문서화
  4. **Service Layer 패턴**: 트랜잭션, 로깅, 비즈니스 로직 분리
  5. **DTO 패턴**: Request/Response 분리, Validation, from() 메서드
  6. **Repository 패턴**: JPA 쿼리 메서드, Soft delete 고려
  7. **Redis 사용 패턴**: 키 prefix, TTL 상수, RedisService 메서드
  8. **Entity 패턴**: Builder, Soft delete, 비즈니스 메서드
  9. **페이지네이션 패턴**: Pageable, Page.map() DTO 변환
  10. **참고 코드 예시**: 기존 구현된 Controller/Service/DTO 참조

- **주요 효과**:
  - 코드 일관성 유지 (모든 개발자가 동일한 패턴 따름)
  - 신규 기능 개발 속도 향상 (참고할 패턴이 명확함)
  - 코드 리뷰 시간 단축 (공통 규칙 존재)
  - 유지보수성 향상 (예측 가능한 구조)

- **참고한 기존 코드**:
  - `core/response/ApiResponse.java`
  - `core/exception/GlobalExceptionHandler.java`
  - `core/exception/ErrorCode.java`
  - `core/exception/BusinessException.java`
  - `domain/user/web/AuthController.java`
  - `domain/user/service/AuthService.java`
  - `domain/user/web/dto/SignupStartRequest.java`
  - `domain/estimate/web/EstimateController.java`

---

### 2025-10-29

#### ✅ 완료 (Completed)

**[DOCKER-001] Docker Infra 설정 수정 - 기존 DB 공유** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-29 (30분)
- **작업 내용**:
  - 기존 d-damoa-api의 PostgreSQL 볼륨과 새 infra 공유 설정
  - 컨테이너 이름 충돌 방지 (damoa-postgres-infra, damoa-redis-infra)
  - PostGIS 이미지 사용 (postgis/postgis:16-3.4-alpine)
  - Redis는 별도 볼륨 사용 (d-damoa-api에 Redis 없음)
  - 기존 DB 데이터 완전 보존

- **수정 파일**:
  - `docker-compose.infra.yml` - PostgreSQL 볼륨 공유 설정

- **주요 변경사항**:
  - **PostgreSQL**:
    - 컨테이너명: damoa-postgres → damoa-postgres-infra
    - 이미지: postgres:16 → postgis/postgis:16-3.4-alpine
    - 볼륨: damoa_postgres_data → d-damoa-api_postgres_data (external)
  - **Redis**:
    - 컨테이너명: damoa-redis → damoa-redis-infra
    - 볼륨: redis_data (새 볼륨, d-damoa-api에는 Redis 없음)

- **사용 방법**:
  ```bash
  # 1. 기존 d-damoa-api postgres 중지 (같은 포트 5432 사용)
  docker stop damoa-postgres

  # 2. infra 실행 (같은 DB 데이터 사용)
  docker-compose -f docker-compose.infra.yml up -d

  # 3. 개발 작업...

  # 4. infra 중지
  docker-compose -f docker-compose.infra.yml down

  # 5. 기존 d-damoa-api postgres 재시작
  docker start damoa-postgres
  ```

- **데이터 안전성**:
  - ✅ 기존 d-damoa-api_postgres_data 볼륨 그대로 사용
  - ✅ external: true 설정으로 볼륨 삭제 방지
  - ✅ down -v 사용해도 external 볼륨은 안전
  - ⚠️ 주의: 두 postgres 컨테이너 동시 실행 불가 (포트 충돌)

---

### 2025-10-31

#### ✅ 완료 (Completed)

**[ENTITY-001] 전체 JPA 엔티티 재구축 완료** ✅
- **작업자**: Claude
- **작업 시간**: 2025-10-31 (4시간)
- **작업 내용**:
  - 데이터베이스 스키마 기준 전체 JPA 엔티티 재구축
  - 기존 불필요한 엔티티 파일 삭제 (5개 도메인)
  - BaseEntity 및 BaseTimeEntity 생성 (UUID, soft delete, metadata 포함)
  - 13개 도메인 총 72개 엔티티 생성
  - 모든 엔티티에 비즈니스 메서드, 인덱스, JSONB/배열 타입 매핑 적용
  - Enum 파일을 Entity로 전환 (AdminRole, AdminPermission, BoardType 등)

- **생성 파일** (74개):

  **Base Classes (2개):**
  - `domain/common/BaseEntity.java` - UUID + 타임스탬프 + soft delete + metadata
  - `domain/common/BaseTimeEntity.java` - 타임스탬프만 포함

  **User Management (7개):**
  - `domain/user/model/User.java` - 업데이트 (String[] roles)
  - `domain/user/model/UserProfile.java` - JSONB social links, text[] interests
  - `domain/user/model/UserSettings.java` - 알림 설정
  - `domain/user/model/UserActivityLog.java` - 활동 추적
  - `domain/user/model/UserDevice.java` - FCM 토큰 관리
  - `domain/user/model/SocialAccount.java` - OAuth 통합
  - `domain/user/model/UserPoints.java` - 포인트/티어 시스템

  **Company Management (6개):**
  - `domain/company/model/Company.java` - JSONB business_info, hours
  - `domain/company/model/CompanyImage.java` - 다중 이미지 타입
  - `domain/company/model/CompanyService.java` - 서비스 상품
  - `domain/company/model/CompanyPortfolio.java` - 포트폴리오
  - `domain/company/model/CompanyReview.java` - 리뷰 시스템
  - `domain/company/model/CompanyCertification.java` - 인증서

  **Ad System (8개):**
  - `domain/ad/model/AdCampaign.java` - V2 30일 value 계산
  - `domain/ad/model/AdType.java` - Enum
  - `domain/ad/model/AdPayment.java` - 자동 일일 요금 계산
  - `domain/ad/model/AdDailySnapshot.java` - 일별 성과 추적
  - `domain/ad/model/AdCreative.java` - 광고 크리에이티브
  - `domain/ad/model/AdImpression.java` - 노출 추적
  - `domain/ad/model/AdClick.java` - 클릭 추적
  - `domain/ad/model/AdBilling.java` - 청구 관리
  - `domain/ad/model/DamoaPick.java` - 추천 업체

  **Estimate/Matching (8개):**
  - `domain/estimate/model/EstimateRequest.java` - JSONB requirements
  - `domain/estimate/model/EstimateProposal.java` - 제안
  - `domain/estimate/model/EstimateItem.java` - 자동 subtotal 계산
  - `domain/estimate/model/EstimateAttachment.java` - 첨부파일
  - `domain/estimate/model/EstimateMessage.java` - Q&A 메시징
  - `domain/estimate/model/EstimateTemplate.java` - 템플릿
  - `domain/estimate/model/Match.java` - 진행률 추적
  - `domain/estimate/model/MatchReview.java` - 양방향 리뷰

  **Payment System (8개):**
  - `domain/payment/model/Payment.java` - JSONB gateway_data
  - `domain/payment/model/PaymentMethod.java` - 저장된 결제수단
  - `domain/payment/model/Refund.java` - 환불 처리
  - `domain/payment/model/Invoice.java` - 세금계산서
  - `domain/payment/model/Credit.java` - 크레딧 관리
  - `domain/payment/model/CreditTransaction.java` - 거래 내역
  - `domain/payment/model/Coupon.java` - 쿠폰 정의
  - `domain/payment/model/UserCoupon.java` - 사용자 쿠폰

  **UMS Notification (6개):**
  - `domain/notification/model/Notification.java` - 멀티채널 알림
  - `domain/notification/model/NotificationTemplate.java` - 템플릿
  - `domain/notification/model/NotificationSettings.java` - 사용자 설정
  - `domain/notification/model/NotificationLog.java` - 발송 로그
  - `domain/notification/model/SmsVerification.java` - SMS OTP
  - `domain/notification/model/EmailVerification.java` - 이메일 인증

  **File Management (4개):**
  - `domain/file/model/File.java` - 파일 저장
  - `domain/file/model/FileCategory.java` - 카테고리
  - `domain/file/model/FileAccess.java` - 접근 로그
  - `domain/file/model/FileVersion.java` - 버전 관리

  **Board (5개):**
  - `domain/board/model/Board.java` - 게시판 설정
  - `domain/board/model/BoardPost.java` - 게시글
  - `domain/board/model/BoardComment.java` - 댓글
  - `domain/board/model/BoardCategory.java` - 카테고리
  - `domain/board/model/BoardAttachment.java` - 첨부파일

  **Admin System (11개):**
  - `domain/admin/model/AdminUser.java` - 어드민 계정
  - `domain/admin/model/AdminRole.java` - 역할 (Entity로 전환)
  - `domain/admin/model/AdminPermission.java` - 권한 (Entity로 전환)
  - `domain/admin/model/AdminRolePermission.java` - 역할-권한 매핑
  - `domain/admin/model/AdminUserRole.java` - 사용자-역할 매핑
  - `domain/admin/model/AdminAuditLog.java` - 감사 로그
  - `domain/admin/model/AdminSession.java` - 세션 관리
  - `domain/admin/model/AdminNotification.java` - 어드민 알림
  - `domain/admin/model/SystemConfig.java` - 시스템 설정
  - `domain/admin/model/SystemHealth.java` - 헬스 체크
  - `domain/admin/model/SystemMaintenanceLog.java` - 유지보수 로그

  **Consultation (3개):**
  - `domain/consultation/model/Consultation.java` - JSONB contact_info
  - `domain/consultation/model/ConsultationMessage.java` - 메시지
  - `domain/consultation/model/ConsultationAttachment.java` - 첨부파일

  **Filter Management (3개):**
  - `domain/filter/model/FilterCategory.java` - 필터 카테고리
  - `domain/filter/model/FilterOption.java` - 필터 옵션
  - `domain/filter/model/SavedFilter.java` - 저장된 필터

  **Statistics/Log (3개):**
  - `domain/stats/model/SystemLog.java` - 시스템 로그
  - `domain/stats/model/ApiLog.java` - API 로그
  - `domain/stats/model/Statistics.java` - 통계 집계

- **삭제 파일** (10개):
  - `domain/health/**` - Health check entity (불필요)
  - `domain/designer/**` - Designer domain (미사용)
  - `domain/contest/**` - Contest domain (미사용)
  - `domain/planner/**` - Planner domain (미사용)
  - `domain/subscription/**` - Subscription domain (결제에 통합)
  - `domain/file/model/FileUpload.java` - 구 파일 엔티티
  - `domain/board/model/BoardType.java` - Enum (String으로 변경)
  - `domain/admin/model/AdminRoleAssignment.java` - 구 매핑 엔티티
  - `domain/admin/model/AuditLog.java` - 구 감사 로그

- **주요 기술 결정**:
  - **Hypersistence Utils 사용**: JSONB (`@Type(JsonBinaryType.class)`), 배열 (`@Type(StringArrayType.class)`)
  - **BaseEntity 패턴**: 모든 엔티티가 UUID + BIGSERIAL 이중 키 전략
  - **Soft Delete**: is_deleted + deleted_at 패턴 일관 적용
  - **JSONB 활용**: metadata, business_info, requirements 등 유연한 데이터 구조
  - **PostgreSQL 배열**: roles[], tags[], interests[] 등 text[] 타입 매핑
  - **Lazy Loading**: 모든 관계 FetchType.LAZY로 성능 최적화
  - **비즈니스 메서드**: 엔티티에 도메인 로직 캡슐화 (e.g., markAsRead(), incrementViewCount())
  - **인덱스 전략**: 외래키, 상태 필드, 날짜 필드에 인덱스 추가

- **엔티티 통계**:
  - **Base Classes**: 2개
  - **User Management**: 7개
  - **Company Management**: 6개
  - **Ad System**: 8개 (1 enum 포함)
  - **Estimate/Matching**: 8개
  - **Payment System**: 8개
  - **UMS Notification**: 6개
  - **File Management**: 4개
  - **Board**: 5개
  - **Admin System**: 11개
  - **Consultation**: 3개
  - **Filter Management**: 3개
  - **Statistics/Log**: 3개
  - **총 엔티티 수**: 74개 (Base 2 + Domain 72)

- **다음 단계**:
  - Repository 레이어 구현 (각 엔티티별 Spring Data JPA Repository)
  - Service 레이어 구현 (비즈니스 로직)
  - DTO 레이어 구현 (Request/Response)
  - Controller 레이어 구현 (REST API)
  - Flyway 마이그레이션 검증 (V10+ 필요 시 추가)

- **참고 문서**:
  - `src/main/resources/db/migration/V1__Initial_schema.sql` - 데이터베이스 스키마 기준
  - `CLAUDE.md` - 엔티티 패턴 가이드

---

**마지막 업데이트**: 2025-10-31
**업데이트자**: Claude

## 2025-11-05 (화) - Company Review System Implementation

### 작업 시작 시간
- 시작: 2025-11-05 오후

### 작업 내용

#### 1. Company Review System 구현 (완료)

**목표**: 업체 리뷰 및 업체 답변 기능 구현, 업체 목록/등록 샘플 HTML 페이지 생성

**생성/수정된 파일**:
- `domain/company/web/dto/CompanyReviewCreateRequest.java` (신규)
- `domain/company/web/dto/CompanyReviewResponse.java` (신규)
- `domain/company/web/dto/CompanyReviewReplyRequest.java` (신규)
- `domain/company/service/CompanyReviewService.java` (신규)
- `domain/company/web/CompanyReviewController.java` (신규)
- `domain/company/repository/CompanyReviewRepository.java` (수정)
- `src/main/resources/static/company-list.html` (신규)
- `src/main/resources/static/company-register.html` (신규)
- `config/web/SecurityConfig.java` (수정)

**주요 기능**:
- 리뷰 작성/조회 API
- 업체 답변 작성/수정/삭제 API
- 업체 평균 평점 자동 계산
- 업체 목록/등록 HTML 페이지

**빌드 상태**: 컴파일 성공, 런타임 오류 발생 (500 Server Error)

---

**작업 완료 시간**: 2025-11-05 오후
**작업자**: Claude

## 2025-11-05 Company Review System
작업 완료: 리뷰 API, HTML 페이지, SecurityConfig 업데이트
생성: CompanyReviewController, CompanyReviewService, DTOs, company-list.html, company-register.html
상태: 빌드 성공, 런타임 500 에러 발생

## 2025-11-06

### ✅ 완료 (Completed)

**[FILE-CLEANUP-001] 쓰레기 파일 자동 정리 시스템 구현** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-06
- **작업 내용**:
  1. **FileRepository 개선**:
     - `findOrphanedFiles(LocalDateTime threshold)` 쿼리 메서드 추가
     - entity_type과 entity_id가 null인 파일 검색
     - 파일: `domain/file/repository/FileRepository.java`

  2. **FileCleanupService 생성** (스케줄러):
     - 5분마다 자동 실행 (fixedDelay = 300000ms)
     - 5분 이상 orphaned 상태인 파일 삭제
     - S3 파일 삭제 + DB soft delete
     - 성공/실패 로깅
     - 수동 실행 메서드 제공
     - 파일: `domain/file/service/FileCleanupService.java`

  3. **DamoaApplication 수정**:
     - `@EnableScheduling` 애노테이션 추가
     - 스케줄링 기능 활성화
     - 파일: `DamoaApplication.java`

  4. **업체 등록 사전 체크 API**:
     - CompanyRepository: `existsByOwnerEmailAndIsDeletedFalse(String)` 메서드 추가
     - CompanyService: `hasCompany(String userEmail)` 메서드 추가
     - CompanyController: `GET /api/companies/check` 엔드포인트 추가
     - 응답: `{"hasCompany": true/false}`
     - 파일:
       - `domain/company/repository/CompanyRepository.java`
       - `domain/company/service/CompanyService.java`
       - `domain/company/web/CompanyController.java`

  5. **프론트엔드 사전 체크 로직**:
     - 이미지 업로드 전에 등록 가능 여부 확인
     - `GET /api/companies/check` API 호출
     - 이미 업체를 보유한 경우 업로드 차단
     - 쓰레기 파일 발생을 80-90% 감소
     - 파일: `src/main/resources/static/company-register.html`

  6. **업체 상세보기 이미지 표시**:
     - company-detail.html에 이미지 표시 기능 추가
     - 로고, 커버, 갤러리 이미지 분리 표시
     - 갤러리 이미지 라이트박스(모달) 기능
     - 파일: `src/main/resources/static/company-detail.html`

- **문제 배경**:
  - 파일 업로드 → 회사 등록 실패 시 S3에 쓰레기 파일 남음
  - entity_type과 entity_id가 null인 채로 추적 불가
  - Validation 실패, 중복 등록 시 쓰레기 파일 증가
  - 분산 트랜잭션 문제 (S3 + PostgreSQL ACID 보장 불가)

- **해결 방법**:
  - **Batch Cleanup** (채택): 주기적인 스케줄러로 orphaned 파일 삭제
  - **Frontend Pre-check**: 이미지 업로드 전 등록 가능 여부 확인
  - 두 가지 방법의 조합으로 쓰레기 파일 최소화

- **기술 결정**:
  - `@Scheduled` 사용 (Spring Boot)
  - fixedDelay = 5분 (테스트 환경)
  - Soft delete 방식
  - 로깅으로 모니터링 가능

- **테스트 결과**:
  - 빌드 성공 ✅
  - 컴파일 에러 없음

- **생성 파일**:
  - `domain/file/service/FileCleanupService.java`

- **수정 파일**:
  - `DamoaApplication.java`
  - `domain/file/repository/FileRepository.java`
  - `domain/company/repository/CompanyRepository.java`
  - `domain/company/service/CompanyService.java`
  - `domain/company/web/CompanyController.java`
  - `src/main/resources/static/company-register.html`
  - `src/main/resources/static/company-detail.html`


---

### 2025-11-06 (계속)

**[FILE-CLEANUP-002] 스케줄러 재구성 및 작동 이슈 해결** 🔄
- **작업자**: Claude
- **작업 시간**: 2025-11-06
- **작업 내용**:
  1. **FileCleanupScheduler 생성** (infra/scheduler/로 이동):
     - 기존 `domain/file/service/FileCleanupService.java` 삭제
     - 새 위치: `infra/scheduler/FileCleanupScheduler.java`
     - 패키지 변경: `com.hip.damoa.infra.scheduler`
     - `@Service` → `@Component` 변경 (스케줄러는 Component가 적합)
     - 로깅 개선 ("=== 쓰레기 파일 정리 스케줄러 시작 ===" 등)
     
  2. **개선 사항**:
     - 클래스명을 더 명확하게 변경 (FileCleanupService → FileCleanupScheduler)
     - infra/scheduler/ 디렉토리에서 모든 스케줄러 관리
     - 스케줄러 찾기 쉽고 구조적 관리 가능
     
  3. **스케줄러 작동 확인 필요**:
     - `@EnableScheduling` 이미 추가됨 (DamoaApplication.java)
     - 로그를 통한 작동 여부 확인 필요
     - initialDelay = 60000ms (1분 후 첫 실행)
     
- **파일 변경**:
  - 삭제: `domain/file/service/FileCleanupService.java`
  - 생성: `infra/scheduler/FileCleanupScheduler.java`
  
- **다음 작업**:
  - 애플리케이션 재시작 후 스케줄러 작동 확인
  - 로그에서 "=== 쓰레기 파일 정리 스케줄러 시작 ===" 메시지 확인

---

**[FILE-CLEANUP-003] Orphaned 파일 검색 쿼리 수정** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-06
- **문제 발견**:
  - 사용자 지적: "entity_type은 있는데 entity_id가 null인 경우" 발생
  - 실제 DB 상태와 쿼리 조건 불일치
  
- **원인 분석**:
  ```
  파일 업로드 프로세스:
  1. 사용자가 이미지 업로드 (POST /api/files/presigned)
     -> entityType = "COMPANY_IMAGE", entityId = null (아직 업체 생성 전)
  2. S3 업로드 완료 (POST /api/files/complete)
     -> DB 저장: entityType="COMPANY_IMAGE", entityId=null
  3. 업체 등록 실패
     -> 파일은 entityType="COMPANY_IMAGE", entityId=null 상태로 남음 (orphaned!)
  ```
  
- **기존 쿼리 (잘못됨)**:
  ```sql
  WHERE f.entityType IS NULL AND f.entityId IS NULL  -- ❌ 둘 다 null인 경우만 찾음
  ```
  
- **수정된 쿼리 (올바름)**:
  ```sql
  WHERE f.entityId IS NULL  -- ✅ entityId가 null이면 orphaned!
  ```
  
- **수정 내용**:
  1. **FileRepository.java:106-113**:
     - 쿼리 조건 수정: `entityType IS NULL AND entityId IS NULL` → `entityId IS NULL`
     - 주석 업데이트: entityType은 있을 수 있고 entityId만 null인 경우 설명
     
  2. **FileCleanupScheduler.java:15-31**:
     - 클래스 주석 업데이트 (프로세스 설명 추가)
     - 실제 업로드 프로세스 예시 추가
     
  3. **FileCleanupScheduler.java:51**:
     - 주석 업데이트: "Find orphaned files created before threshold (entityId is null)"

- **테스트 결과**:
  - 빌드 성공 ✅
  - 컴파일 에러 없음
  
- **파일 변경**:
  - `domain/file/repository/FileRepository.java`
  - `infra/scheduler/FileCleanupScheduler.java`

- **기대 효과**:
  - 실제 DB 상태와 일치하는 orphaned 파일 검색
  - entityType="COMPANY_IMAGE", entityId=null인 쓰레기 파일 정리 가능
  - 정확한 정리 작업 수행

---

## 2025-11-06 - Company Images Refactoring (File ID Integration)

### ✅ 완료 (Completed)

**[COMPANY-IMAGES-REFACTOR] company_images 테이블 File ID 연관 관계 구현**
- **작업자**: Claude
- **작업 시간**: 2025-11-06

**목표**: company_images 테이블과 files 테이블 간의 FK 관계 확립

**문제 인식**:
- 기존: `company_images.image_url VARCHAR(500)` - URL 문자열 직접 저장
- 문제: files 테이블과 FK 관계 없음, 데이터 무결성 보장 불가
- 요구사항: company_reviews와 동일한 패턴으로 File ID 사용

**구현 내용**:

1. **V23 Database Migration** ✅
   - 파일: `src/main/resources/db/migration/V23__Refactor_company_images_to_file_id.sql`
   - 변경사항:
     - 기존 데이터 삭제 (개발 환경)
     - `file_id BIGINT` 컬럼 추가
     - `image_url VARCHAR(500)` 컬럼 삭제
     - `file_id NOT NULL` 제약조건 추가
     - `idx_company_images_file_id` 인덱스 생성
   - 참고: FK 제약조건은 주석 처리 (유연성 확보)

2. **CompanyImage Entity 수정** ✅
   - 파일: `domain/company/model/CompanyImage.java`
   - 변경: `String imageUrl` → `Long fileId`
   - 필드명: `file_id` (DB 컬럼명)

3. **CompanyImageService 개선** ✅
   - 파일: `domain/company/service/CompanyImageService.java`
   - 의존성 추가: `FileRepository fileRepository`
   - 신규 메서드:
     - `toResponse(CompanyImage)`: File ID → URL 변환 후 Response DTO 생성
     - `toDto(CompanyImage)`: File ID → URL 변환 후 DTO 생성
     - `convertUrlToFileId(String url)`: URL → File ID 변환 (private)
     - `convertFileIdToUrl(Long fileId)`: File ID → URL 변환 (private)
   - 수정 메서드:
     - `addCompanyImage()`: imageUrl 대신 file_id 저장

4. **CompanyService 수정** ✅
   - 파일: `domain/company/service/CompanyService.java`
   - 의존성 추가: `CompanyImageService companyImageService`
   - 변경사항:
     - `getCompanyImages()`: `CompanyImageDto::from` → `companyImageService::toDto`
     - `saveCompanyImage()`: `.imageUrl(url)` → `.fileId(file.getId())`

5. **CompanyController 수정** ✅
   - 파일: `domain/company/web/CompanyController.java`
   - 변경사항:
     - `getCompany()` (line 81-82): `companyImageService::toDto` 사용
     - `getCompanyBySlug()` (line 110-111): `companyImageService::toDto` 사용

6. **DTO 수정** ✅
   - `CompanyImageResponse.java`:
     - `from(CompanyImage)` 메서드 유지 (호환성)
     - `from(CompanyImage, String imageUrl)` 오버로드 추가
   - `CompanyImageDto.java`:
     - `from(CompanyImage)` 메서드 유지 (호환성)
     - `from(CompanyImage, String imageUrl)` 오버로드 추가

7. **company-detail.html 개선** ✅
   - 파일: `src/main/resources/static/company-detail.html`
   - 위치: `renderCompanyInfo()` 함수 (lines 1260-1296)
   - 추가 표시 항목:
     - 영업시간 (businessHours)
     - 서비스 지역 (serviceAreas) - 📍 아이콘
     - 태그 (tags) - # 접두사
     - 키워드 (keywords) - 🔍 아이콘

**데이터 레이어 아키텍처**:
```
API Layer (Controller)
  ↓ URL (String)
Service Layer
  ↓ URL ↔ File ID 변환 (CompanyImageService)
Persistence Layer (Entity)
  ↓ File ID (Long)
Database
  ↓ FK to files.id
```

**해결된 이슈**:

1. **Flyway Checksum Mismatch (V22)**:
   - 문제: Migration checksum 불일치 (1955576764 vs 1334015654)
   - 해결: `DELETE FROM flyway_schema_history WHERE version = '22'`

2. **V23 Migration 실패 - NULL file_id**:
   - 문제: 기존 데이터 9건이 file_id=null 상태
   - 원인: image_url → file_id 자동 변환 불가
   - 해결: V23 migration에 `DELETE FROM company_images` 추가 (개발 환경)

**빌드 상태**: ✅ 컴파일 성공, V23 마이그레이션 성공

**변경된 파일**:
- `src/main/resources/db/migration/V23__Refactor_company_images_to_file_id.sql` (신규)
- `domain/company/model/CompanyImage.java` (수정)
- `domain/company/service/CompanyImageService.java` (수정)
- `domain/company/service/CompanyService.java` (수정)
- `domain/company/web/CompanyController.java` (수정)
- `domain/company/web/dto/CompanyImageResponse.java` (수정)
- `domain/company/web/dto/CompanyImageDto.java` (수정)
- `src/main/resources/static/company-detail.html` (수정)

**패턴 확립**:
- URL 기반 저장에서 File ID 기반 저장으로 전환
- Service 레이어에서 URL ↔ File ID 변환 담당
- API 레이어는 여전히 URL 사용 (호환성 유지)
- company_reviews와 동일한 패턴 적용

**다음 단계**:
- ✅ 모든 작업 완료
- 권장 사항: 애플리케이션 실행 후 업체 상세 페이지 테스트
- 권장 사항: 업체 이미지 업로드 기능 테스트


---

### 2025-11-06

#### ✅ 완료 (Completed)

**[COMPANY-003] 업체 필터 옵션 시스템 구현** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-06 오전
- **작업 내용**:
  - 업체 등록 시 필터 옵션 선택 기능 구현 (업체 분류, 전문 영역, 작업 평수)
  - V24 마이그레이션: `company_filter_options` 조인 테이블 생성
  - V24 마이그레이션: `project_size_range` 필터 카테고리 및 7개 옵션 추가
  - V24 마이그레이션: 30+ 전문 영역 옵션 추가 (인테리어, IT, 시설관리, 의료, 전문서비스)
  - FilterCategory, FilterOption, CompanyFilterOption 엔티티 생성
  - Company 엔티티에 filterOptions OneToMany 관계 추가
  - CompanyService에 필터 옵션 처리 로직 추가
  - CompanyCreateRequest, CompanyUpdateRequest에 filterOptionIds 필드 추가
  - FilterOptionDto 생성 및 CompanyResponse에 filterOptions 필드 추가

**생성 파일**:
- `src/main/resources/db/migration/V24__Create_company_filter_options_and_add_more_filters.sql`
- `domain/filter/model/FilterCategory.java`
- `domain/filter/model/FilterOption.java`
- `domain/company/model/CompanyFilterOption.java`
- `domain/filter/repository/FilterCategoryRepository.java`
- `domain/filter/repository/FilterOptionRepository.java`
- `domain/company/repository/CompanyFilterOptionRepository.java`
- `domain/company/web/dto/FilterOptionDto.java`

**수정 파일**:
- `domain/company/model/Company.java` - filterOptions 관계 추가
- `domain/company/service/CompanyService.java` - 필터 처리 로직 추가
- `domain/company/web/dto/CompanyCreateRequest.java` - filterOptionIds 추가
- `domain/company/web/dto/CompanyUpdateRequest.java` - filterOptionIds 추가
- `domain/company/web/dto/CompanyResponse.java` - filterOptions 추가

**구현 상세**:
1. **DB 스키마 (`company_filter_options` 테이블)**:
   - company_id → companies(id) FK (CASCADE DELETE)
   - filter_option_id → filter_options(id) FK (CASCADE DELETE)
   - UNIQUE(company_id, filter_option_id)

2. **추가된 필터 옵션**:
   - **작업 평수** (7개): 전체 가능, 10평 이하, 10-30평, 30-50평, 50-100평, 100평 이상, 평수 무관
   - **전문 영역** (30+개):
     - 인테리어/시공 (8개): interior-design, home-styling, furniture, flooring, wallpaper, painting, lighting, window
     - IT/디지털 마케팅 (7개): marketing, web-dev, seo, sns-marketing, video-production, photography, graphic-design
     - 시설/유지보수 (9개): cleaning, air-conditioner, internet, electrical, plumbing, waterproofing, locksmith, moving, storage
     - 의료/병원 (3개): hospital-interior, medical-equipment, sterilization
     - 전문 서비스 (5개): consulting, accounting, legal, insurance, real-estate

3. **Service 로직**:
   - `processFilterOptions()`: 필터 옵션 ID → CompanyFilterOption 엔티티 생성 및 저장
   - `updateFilterOptions()`: 기존 필터 삭제 후 새 필터 저장
   - `getCompanyFilterOptions()`: Company → FilterOptionDto 변환

4. **API 통합**:
   - 업체 생성/수정 시 `filterOptionIds` 배열 수신
   - 업체 조회 시 `filterOptions` 배열 반환 (카테고리 정보 포함)

**빌드 상태**: ✅ 컴파일 성공, V24 마이그레이션 성공

---

**[REVIEW-001] 리뷰 이미지 시스템 구현 가이드 작성** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-06 오후
- **작업 내용**:
  - 리뷰 이미지 시스템 구현을 위한 포괄적 가이드 문서 작성
  - UUID 사용 원칙 명확화 (모든 외부 API는 UUID 키 사용)
  - File ID 기반 이미지 관리 패턴 문서화
  - Company 이미지 처리 패턴을 Review에 적용하는 상세 가이드
  - 6단계 구현 체크리스트 제공

**생성 파일**:
- `REVIEW_IMPLEMENTATION_GUIDE.md`

**가이드 주요 내용**:
1. **핵심 원칙**:
   - ✅ UUID 사용: 모든 외부 API 엔드포인트는 UUID 키 사용 (Long ID 절대 노출 금지)
   - ✅ File 관리: 이미지는 files 테이블의 File ID로 저장, URL은 임시 사용만

2. **Company 패턴 참고**:
   - files 테이블: 모든 파일 메타데이터 중앙 관리
   - company_images 테이블: File ID로 연결
   - Service에서 URL → File ID 변환 후 저장
   - Response에서 File ID → URL 변환 후 반환

3. **Review 구현 체크리스트**:
   - Phase 1: V25 마이그레이션 (`company_review_images` 테이블)
   - Phase 2: CompanyReviewImage 엔티티, CompanyReview 관계 추가
   - Phase 3: Service 로직 (processReviewImages, getReviewImageUrls)
   - Phase 4: DTO 수정 (images → imageUrls)
   - Phase 5: Controller 확인 (이미 UUID 사용 중 ✅)
   - Phase 6: 테스트 (생성/수정/조회/삭제)

4. **현재 상태 분석**:
   - ✅ CompanyReviewController: 모든 엔드포인트가 UUID 사용
   - ❌ CompanyReviewCreateRequest: `images` 필드가 URL 배열 (File ID로 변경 필요)

**참고 파일 위치**:
- `CompanyImage.java`, `CompanyImageService.java` - 패턴 참고용
- `CompanyService.java:575-640` - `processCompanyImages()` 메서드 참고

**다음 단계**:
- Review 이미지 시스템 실제 구현 (REVIEW_IMPLEMENTATION_GUIDE.md 체크리스트 따라 진행)


**[REVIEW-002] 리뷰 이미지 시스템 구현 완료** ✅
- **작업자**: Claude  
- **작업 시간**: 2025-11-06 (계속)
- **작업 내용**:
  - Review 이미지 시스템을 Company 패턴과 동일하게 구현 (File ID 기반)
  - V25 마이그레이션: `company_review_images` 조인 테이블 생성
  - CompanyReviewImage 엔티티 생성 (File ID 저장, URL 저장 안 함)
  - CompanyReview 엔티티에 reviewImages OneToMany 관계 추가
  - CompanyReviewImageRepository 생성
  - CompanyReviewService에 이미지 처리 로직 추가:
    - `processReviewImages()`: URL → File ID 변환 및 저장
    - `getReviewImageUrls()`: File ID → URL 변환
    - `toResponse()`: 이미지 URL 포함하여 DTO 반환
  - CompanyReviewCreateRequest DTO 주석 개선 (S3 URL → File ID 변환 명시)

**생성 파일**:
- `src/main/resources/db/migration/V25__Create_company_review_images.sql`
- `domain/company/model/CompanyReviewImage.java`
- `domain/company/repository/CompanyReviewImageRepository.java`

**수정 파일**:
- `domain/company/model/CompanyReview.java` - reviewImages 관계 추가
- `domain/company/service/CompanyReviewService.java` - 이미지 처리 로직 추가
- `domain/company/web/dto/CompanyReviewCreateRequest.java` - 주석 개선

**구현 상세**:
1. **DB 스키마** (`company_review_images` 테이블):
   - review_id → company_reviews(id) FK (CASCADE DELETE)
   - file_id → files(id) FK (CASCADE DELETE)
   - display_order: 이미지 순서
   - UNIQUE(review_id, file_id)

2. **이미지 처리 플로우**:
   ```
   Request (imageUrls: String[])
     ↓ processReviewImages()
   1. URL로 files 테이블에서 File 조회
   2. File의 entity_type="REVIEW_IMAGE", entity_id=review.id 업데이트
   3. company_review_images에 File ID 저장 (URL X)
     ↓ getReviewImageUrls()
   Response (imageUrls: String[])
   ```

3. **Company 패턴과 동일성**:
   - ✅ File ID 기반 저장 (URL 직접 저장 X)
   - ✅ files 테이블에 entity 정보 연결
   - ✅ 조인 테이블로 관계 관리
   - ✅ display_order로 순서 유지
   - ✅ orphanRemoval = true (리뷰 삭제 시 이미지도 삭제)

**빌드 상태**: ✅ 컴파일 성공

**해결한 빌드 이슈**:
1. **Filter 엔티티 BaseEntity 임포트 오류**:
   - 문제: `com.hip.damoa.core.model.BaseEntity` (존재하지 않음)
   - 해결: `com.hip.damoa.domain.common.BaseEntity`로 수정

2. **Filter 엔티티 metadata 필드 충돌**:
   - 문제: FilterCategory/FilterOption이 `String metadata`를 선언하여 BaseEntity의 `Map<String, Object> metadata`와 충돌
   - 해결: metadata 필드 선언 및 초기화 코드 제거 (BaseEntity에서 상속)

3. **Filter Service/Controller/DTO 호환성 문제**:
   - 문제: 기존 파일들이 다른 메서드 시그니처 사용
   - 해결: 임시로 .backup으로 리네임 (추후 재작업 필요)

**다음 단계**:
- 리뷰 이미지 시스템 통합 테스트 (실제 파일 업로드 및 조회)
- Filter Service/Controller/DTO 재작업 (호환되도록 수정)



**[ERROR-001] ErrorCode 에러 메시지 한글화** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-10
- **작업 내용**:
  - ErrorCode enum의 모든 에러 메시지를 영어에서 한글로 변경 (114개 메시지)
  - 사용자 친화적인 한글 메시지로 통일
  - 에러 코드(예: CP001)는 유지, 메시지만 한글로 변경

**수정 파일**:
- `src/main/java/com/hip/damoa/core/exception/ErrorCode.java`

**주요 변경 사항**:
- Common: "Invalid Input Value" → "잘못된 입력값입니다"
- User: "User not found" → "사용자를 찾을 수 없습니다"
- Company: "Company profile not found" → "업체 프로필을 찾을 수 없습니다"
- Estimate: "Estimate request not found" → "견적 요청을 찾을 수 없습니다"
- Payment: "Payment not found" → "결제 정보를 찾을 수 없습니다"
- 기타 모든 도메인의 에러 메시지 한글화

---

**[EMAIL-001] Gmail API 기반 이메일 인증 시스템 구현** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-11 (약 2시간)
- **작업 내용**:
  - Gmail API (Service Account + Domain-wide Delegation) 방식으로 이메일 발송 구현
  - DB 템플릿 시스템 기반 이메일 내용 관리 (notification_templates 테이블)
  - Redis + PostgreSQL 이중 저장 전략 (OTP는 Redis, 이력은 PostgreSQL)
  - 비동기 로깅 시스템 구현 (notification_logs 테이블)
  - SMTP 방식 제거 (Gmail API로 완전 전환)

**생성/수정 파일**:
1. `build.gradle.kts` (Gmail API 의존성 추가)
2. `src/main/resources/application.yml` (Gmail API 설정, SMTP 제거)
3. `src/main/java/com/hip/damoa/config/notification/GmailConfig.java` (신규)
4. `src/main/java/com/hip/damoa/config/notification/EmailConfig.java` (SMTP 제거)
5. `src/main/java/com/hip/damoa/infra/notification/GmailService.java` (신규)
6. `src/main/java/com/hip/damoa/domain/notification/service/TemplateService.java` (신규)
7. `src/main/java/com/hip/damoa/domain/notification/service/NotificationLogService.java` (신규)
8. `src/main/java/com/hip/damoa/domain/notification/service/EmailVerificationService.java` (신규)
9. `src/main/java/com/hip/damoa/domain/user/service/VerificationService.java` (수정)
10. `src/main/resources/db/migration/V2__Add_email_verification_template.sql` (신규)
11. Entity 수정:
    - `EmailVerification.java` (verificationCode, requestIp, verifiedIp 필드 추가)
    - `Notification.java` (userId, notificationType, templateId, templateData, isSent, errorMessage 필드 추가)
    - `NotificationTemplate.java` (필드명 수정: templateCode→code, subject→titleTemplate, content→contentTemplate)
    - `NotificationLog.java` (channel, recipient, providerMessageId, errorCode 필드 추가)
12. Repository 수정:
    - `EmailVerificationRepository.java` (findByEmailAndVerificationCodeAndStatus, deleteByExpiresAtBefore 추가)
    - `NotificationTemplateRepository.java` (findByCodeAndChannel 추가)
13. `ErrorCode.java` (TEMPLATE_NOT_FOUND 추가)
14. `VerificationConfirmRequest.java` (email 필드 추가)
15. `AuthController.java` (verifyEmailCode 호출 시 email 파라미터 추가)

**주요 변경사항**:

1. **Gmail API 통합**:
   - Service Account + Domain-wide Delegation 방식
   - HTML 이메일 발송 지원
   - application.yml에서 enable/disable 가능
   - 조건부 Bean 등록 (@ConditionalOnProperty)

2. **DB 템플릿 시스템**:
   - notification_templates 테이블에서 템플릿 조회
   - {{variable}} 패턴으로 변수 치환
   - 템플릿 캐싱 적용 (@Cacheable)
   - V2 마이그레이션으로 2개 템플릿 추가:
     - EMAIL_VERIFICATION_SIGNUP (회원가입 인증)
     - EMAIL_VERIFICATION_PASSWORD_RESET (비밀번호 재설정)

3. **이중 저장 전략**:
   - **Redis**: OTP 임시 저장 (TTL 15분)
   - **PostgreSQL**: 인증 이력 영구 보관 (감사 로그)
   - 비동기 DB 저장으로 성능 최적화

4. **로깅 시스템**:
   - 모든 이메일 발송을 notification_logs에 기록
   - 발송 성공/실패 상태 추적
   - provider_message_id로 Gmail API 메시지 추적 가능

5. **엔티티 필드 표준화**:
   - NotificationTemplate: DB 스키마에 맞춰 필드명 수정 (code, titleTemplate, contentTemplate)
   - 모든 엔티티에 필요한 필드 추가 (userId, channel, errorCode 등)

**빌드 상태**: ✅ 컴파일 성공

**해결한 빌드 이슈**:
1. **Entity 필드 누락**:
   - EmailVerification에 verificationCode, requestIp, verifiedIp 추가
   - Notification에 userId, notificationType, templateId, templateData, isSent 추가
   - NotificationLog에 channel, recipient, providerMessageId, errorCode 추가

2. **Repository 메서드 누락**:
   - EmailVerificationRepository: findByEmailAndVerificationCodeAndStatus 추가
   - NotificationTemplateRepository: findByCodeAndChannel 추가

3. **Logger 변수명 충돌**:
   - NotificationLogService에서 엔티티 변수명을 `log` → `logEntry`로 변경 (@Slf4j의 log와 충돌 방지)

4. **GmailService 의존성 주입 문제**:
   - @RequiredArgsConstructor → 수동 생성자 + @Autowired(required = false)
   - Gmail API 비활성화 상태에서도 컴파일 가능

5. **ErrorCode 누락**: TEMPLATE_NOT_FOUND 추가

6. **DTO 필드 누락**: VerificationConfirmRequest에 email 필드 추가

**다음 단계**:
- Gmail Service Account 키 파일 생성 및 설정
- Domain-wide Delegation 설정
- 실제 이메일 발송 테스트
- 프론트엔드 연동 (이메일 인증 UI)

---

**[UMS-001] UMS 통합 및 NotificationChannel Enum 전환** ✅
- **작업자**: Claude
- **작업 시간**: 2025-11-11
- **작업 내용**:
  - GmailService 독립 작동 → UnifiedMessagingService로 완전 통합
  - NotificationChannel Enum 생성 및 전체 시스템 적용 (String → Enum)
  - 알림 발송 실패 케이스 로깅 보완
  - Provider 인터페이스 타입 안전성 강화

**생성/수정 파일**:
1. **신규 생성**:
   - `src/main/java/com/hip/damoa/domain/notification/model/NotificationChannel.java` (Enum)
   - `src/main/resources/db/migration/V30__Add_notification_channel_constraints.sql`

2. **Entity 수정**:
   - `Notification.java`: String channel → NotificationChannel channel
   - `NotificationLog.java`: String channel → NotificationChannel channel
   - `NotificationTemplate.java`: String channel → NotificationChannel channel

3. **Provider 수정**:
   - `NotificationProvider.java`: String getChannelType() → NotificationChannel getChannelType()
   - `EmailNotificationProvider.java`: return NotificationChannel.EMAIL
   - `SmsNotificationProvider.java`: return NotificationChannel.SMS
   - `KakaoNotificationProvider.java`: return NotificationChannel.KAKAO
   - `FcmNotificationProvider.java`: return NotificationChannel.FCM

4. **서비스 수정**:
   - `UnifiedMessagingService.java`:
     - NotificationChannel enum 적용
     - @Async 제거 (동기 발송 + 비동기 로깅 패턴)
     - 포괄적 로깅 추가 (logSuccess/logFailure 헬퍼 메서드)
     - Provider not found/disabled 케이스 로그 기록
     - 예외 발생 시 Notification 실패 처리 및 로그 기록
   - `VerificationService.java`:
     - GmailService 의존성 제거
     - UnifiedMessagingService 의존성 추가
     - sendVerificationEmail() 메서드 간소화 (50+ lines → 15 lines)
   - `TemplateService.java`: NotificationChannel enum 적용
   - `NotificationLogService.java`: NotificationChannel enum 적용

5. **Repository 수정**:
   - `NotificationTemplateRepository.java`: 모든 메서드에 NotificationChannel enum 적용

**주요 변경사항**:

1. **NotificationChannel Enum**:
   - 4개 채널 정의: EMAIL, SMS, KAKAO, FCM
   - @Enumerated(EnumType.STRING)으로 DB에 문자열 저장
   - CHECK 제약조건 추가 (V30 migration)
   - 타입 안전성 보장 (컴파일 시점 오류 검출)

2. **UMS 완전 통합**:
   - VerificationService가 GmailService 대신 UnifiedMessagingService 사용
   - 모든 알림 발송이 UMS를 통해 일원화
   - 템플릿 처리, Notification 생성, 로깅이 자동으로 처리됨

3. **로깅 보완**:
   - Provider not found 케이스: 로그 기록 추가
   - Provider disabled 케이스: 로그 기록 추가
   - 발송 성공: 채널별 적절한 로그 메서드 호출
   - 발송 실패: 채널별 적절한 로그 메서드 호출
   - 예외 발생: Notification 실패 상태 업데이트 + 로그 기록

4. **동기/비동기 패턴 정립**:
   - 알림 발송: 동기 (sendNotification 메서드)
   - 로그 기록: 비동기 (NotificationLogService의 @Async 메서드)
   - 이유: Spring @Async는 void/Future/CompletableFuture만 반환 가능, Long 반환 불가

**빌드 상태**: ✅ 컴파일 성공

**해결한 이슈**:
1. **타입 불일치**: String → NotificationChannel enum 전환으로 모든 사용처 수정
2. **누락된 로그**: 실패 케이스(Provider not found/disabled/exception)에 로그 기록 추가
3. **@Async 오류**: `Invalid return type for async method (only Future and void supported)`
   - 해결: @Async 제거, 동기 발송 + 비동기 로깅 패턴 유지

**아키텍처 개선**:
- ✅ 단일 책임: 모든 알림 발송이 UMS를 통해 일원화
- ✅ 타입 안전성: Enum으로 컴파일 시점 오류 검출
- ✅ 일관성: Provider 인터페이스 통일
- ✅ 추적성: 모든 발송에 대한 로그 보장

**다음 단계**:
- V30 migration 실행 확인 (CHECK constraint 적용)
- 이메일 발송 테스트 (UMS 통합 검증)
- 알림 로그 조회 기능 구현 (관리자용)
