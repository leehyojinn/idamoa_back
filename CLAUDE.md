# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🚨 작업 워크플로우 (Work Workflow)

**중요**: 모든 작업은 반드시 다음 순서를 따릅니다:

1. **작업 시작 전**
   - `doc/work-log.md` 파일을 **반드시** 읽고 현재 상태 확인
   - 진행 중인 작업이 있는지 확인
   - 다음 우선순위 작업 확인

2. **작업 진행 중**
   - `doc/work-log.md`의 "진행 중(In Progress)" 섹션에 작업 내용 추가
   - 작업 시작 시간, 작업 내용 기록
   - 주요 결정사항이나 이슈 발생 시 즉시 기록

3. **작업 완료 후**
   - 작업을 "진행 중"에서 "완료(Completed)" 섹션으로 이동
   - 생성/수정된 파일 목록 기록
   - 주요 변경사항 및 결정사항 기록
   - 다음 작업 항목 업데이트

4. **다음 작업 시작**
   - 1번부터 다시 시작

**작업일지 위치**: `doc/work-log.md`

## ⚠️ 핵심 원칙

### 1. UUID 사용 원칙
- **모든 외부 API 엔드포인트는 UUID를 키로 사용**
- **내부 ID (Long)는 절대 외부에 노출하지 않음**

```java
// ✅ 올바른 방식
@PostMapping("/companies/{companyUuid}/reviews")
public ApiResponse<CompanyReviewResponse> createReview(
    @PathVariable UUID companyUuid,  // ✅ UUID 사용
    @AuthenticationPrincipal UserDetails userDetails,
    @Valid @RequestBody CompanyReviewCreateRequest request) {
    ...
}

// ❌ 잘못된 방식
@PostMapping("/companies/{companyId}/reviews")  // ❌ Long ID 노출
public ApiResponse<CompanyReviewResponse> createReview(
    @PathVariable Long companyId,
    ...
}
```

## Project Overview

Damoa is a Spring Boot web application built with Java 17, using JPA for persistence, PostgreSQL 16 as the database, and Redis 7 for caching. The application implements JWT-based authentication with Spring Security.

## Environment Configuration

All configurations are managed via `.env` file. Copy and customize for your environment:

```bash
# Key configurations in .env:
DB_HOST=localhost
DB_PORT=5432
DB_NAME=damoa
DB_USERNAME=postgres
DB_PASSWORD=my-secret-pw

REDIS_HOST=localhost
REDIS_PORT=6379

JWT_SECRET=your-jwt-secret-here
# ... (see .env for full list)
```

## Commands

### Build and Run

```bash
# Build the project (skips tests)
./gradlew build -x test

# Build with tests
./gradlew build

# Run tests only
./gradlew test

# Clean build
./gradlew clean build
```

### Docker Operations

**Two deployment strategies available:**

#### Option A: Infrastructure Only (Recommended for Development)
Run only PostgreSQL 16 + Redis 7, develop app in IDE:

```bash
# Start infrastructure
docker-compose -f docker-compose.infra.yml up -d

# Stop infrastructure
docker-compose -f docker-compose.infra.yml down

# View logs
docker-compose -f docker-compose.infra.yml logs -f
```

Benefits: Fast restart, easy debugging in IDE, hot reload

#### Option B: Full Stack
Run PostgreSQL 16 + Redis 7 + Application:

```bash
# Start all services
docker-compose up --build

# Stop all services
docker-compose down

# View logs
docker-compose logs -f app
```

Benefits: Complete environment in one command, matches production

### Local Development

**Recommended workflow:**

1. Start infrastructure only:
   ```bash
   docker-compose -f docker-compose.infra.yml up -d
   ```

2. Run application from IDE (IntelliJ/Eclipse) for debugging

3. Or run via Gradle:
   ```bash
   ./gradlew bootRun
   ```

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

## Architecture

### Package Structure

The codebase follows a domain-driven design pattern organized under `com.hipdamoa`:

- **`config/`** - Configuration classes for infrastructure concerns:
  - `persistence/` - JPA configuration
  - `redis/` - Redis configuration
  - `web/` - Security, Swagger, and web MVC configuration

- **`core/`** - Cross-cutting concerns and shared infrastructure:
  - `exception/` - Global exception handling with `GlobalExceptionHandler`, custom `BusinessException`, and `ErrorCode` enum
  - `jwt/` - JWT authentication infrastructure (`JwtTokenProvider`, `JwtAuthenticationFilter`, `TokenInfo`)
  - `response/` - Standardized API response wrapper (`ApiResponse`)

- **`domain/`** - Business domains, each containing:
  - `model/` - JPA entities
  - `repository/` - Spring Data JPA repositories
  - `service/` - Business logic layer
  - `web/` - REST controllers and DTOs

- **`infra/`** - External infrastructure services:
  - `redis/` - Redis service abstraction (`RedisService`)

### Authentication Flow

1. **Two-Phase Signup**:
   - `POST /api/auth/signup/start` - Validates email uniqueness, stores request in Redis with UUID token (10 min TTL)
   - `POST /api/auth/signup/complete` - Retrieves cached data from Redis, creates user in PostgreSQL, deletes Redis entry

2. **Login**:
   - `POST /api/auth/login` - Authenticates via Spring Security's `AuthenticationManager`
   - Returns JWT access token (1 day) and refresh token (7 days) via `JwtTokenProvider`

3. **Request Authorization**:
   - `JwtAuthenticationFilter` intercepts requests, extracts JWT from Authorization header
   - Validates token and sets `Authentication` in SecurityContext
   - Protected endpoints require valid JWT (except health, auth/login, auth/signup/*, Swagger)

### Security Configuration

- Located in `config/web/SecurityConfig.java`
- Stateless session management (JWT-based)
- Public endpoints: `/api/health/**`, `/api/auth/login`, `/api/auth/signup/**`, Swagger UI
- All other endpoints require authentication
- Password encoding uses `DelegatingPasswordEncoder` (default bcrypt)

### Database Configuration

- **PostgreSQL 16** connection configured via `application.yml` + `.env`
- Environment variables:
  - `DB_HOST` (default: localhost)
  - `DB_PORT` (default: 5432)
  - `DB_NAME` (default: damoa)
  - `DB_USERNAME` (default: postgres)
  - `DB_PASSWORD` (default: my-secret-pw)
- **Flyway Database Migration**: Schema versioning enabled (V1-V9 migrations)
- JPA `ddl-auto: validate` - validates schema against entities (no auto-modification)
- SQL logging enabled via `show-sql: true`

### Redis Configuration

- **Redis 7** used for temporary data storage (signup tokens with TTL)
- Environment variables:
  - `REDIS_HOST` (default: localhost)
  - `REDIS_PORT` (default: 6379)
- Persistence: AOF (Append-Only File) enabled in Docker

**⚠️ Redis vs PostgreSQL 분리 전략**:
- **Redis 사용**: JWT 토큰, OAuth state, 이메일/SMS OTP, 세션, 캐시, 속도 제한
- **PostgreSQL 사용**: 영구 비즈니스 데이터, 감사 로그, 관계형 데이터
- 자세한 내용은 `doc/redis-postgresql-strategy.md` 참고

### JWT Secret

JWT secret is configured in `application.yml` under `jwt.secret`. This is base64-encoded and used for signing tokens. In production, use environment variables or secure secret management.

### User Model

`User` entity (`domain/user/model/User.java`) implements Spring Security's `UserDetails`:
- Uses email as username
- Supports multiple roles via `@ElementCollection`
- Default role: `Role.USER`

### Error Handling

Global exception handling via `GlobalExceptionHandler`:
- `BusinessException` - Custom business logic exceptions mapped to error codes
- `MethodArgumentNotValidException` - Validation errors (returns first error message)
- Generic `Exception` - Catches all unhandled exceptions, returns 500

---

## 💻 Coding Standards and Common Patterns

**⚠️ 중요**: 새로운 기능을 구현하거나 코드를 작성할 때는 반드시 아래의 공통 패턴을 참고하여 일관성을 유지하세요.

### 1. API Response 구조

모든 API는 `ApiResponse<T>` 래퍼를 사용하여 응답합니다.

**위치**: `core/response/ApiResponse.java`

**사용 예시**:
```java
// 데이터와 함께 성공 응답
return ApiResponse.ok(data);
// 또는
return ApiResponse.success(data);

// 데이터 없이 성공 응답
return ApiResponse.ok();
// 또는
return ApiResponse.success();

// 에러 응답 (일반적으로 사용하지 않음, GlobalExceptionHandler가 자동 처리)
return ApiResponse.error("에러 메시지");
```

**Response 형식**:
```json
{
  "success": true,
  "data": { ... },
  "errorCode": null,
  "message": null
}
```

### 2. Exception 처리

비즈니스 로직 오류는 `BusinessException`을 던지고, `ErrorCode` enum을 사용합니다.

**위치**:
- `core/exception/BusinessException.java`
- `core/exception/ErrorCode.java`
- `core/exception/GlobalExceptionHandler.java`

**사용 예시**:
```java
// Service 레이어에서
if (user == null) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}

// ErrorCode enum에 새 에러 추가
CUSTOM_ERROR(HttpStatus.BAD_REQUEST, "CE001", "Custom error message")
```

**자동 변환**: `GlobalExceptionHandler`가 `BusinessException`을 자동으로 `ApiResponse.error()`로 변환합니다.

### 3. Controller 패턴

**필수 애노테이션**:
```java
@Tag(name = "Domain", description = "도메인 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/domain")
public class DomainController {

    private final DomainService domainService;

    @Operation(summary = "리소스 생성", description = "새로운 리소스를 생성합니다")
    @PostMapping
    public ApiResponse<ResourceResponse> createResource(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ResourceCreateRequest request) {

        // Service 호출 시 userDetails.getUsername() (이메일) 전달
        Resource resource = domainService.createResource(
            userDetails.getUsername(), request);

        return ApiResponse.success(ResourceResponse.from(resource));
    }
}
```

**주요 규칙**:
- `@Tag`, `@Operation` 사용 (Swagger 문서화)
- `@Valid` 사용하여 DTO validation
- `@AuthenticationPrincipal UserDetails` 사용하여 인증된 사용자 정보 받기
- Service 레이어에 `userDetails.getUsername()` (이메일) 전달
- `ApiResponse.ok()` 또는 `ApiResponse.success()` 사용
- 생성 API는 `@ResponseStatus(HttpStatus.CREATED)` 추가 권장

### 4. Service Layer 패턴

**필수 애노테이션 및 구조**:
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainService {

    private final DomainRepository domainRepository;
    private final RedisService redisService;

    @Transactional(readOnly = true)
    public Resource getResource(Long id) {
        return domainRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public Resource createResource(String userEmail, ResourceCreateRequest request) {
        // 로깅
        log.info("리소스 생성 시작: userEmail={}", userEmail);

        // 비즈니스 로직
        Resource resource = Resource.builder()
            .name(request.getName())
            .build();

        resource = domainRepository.save(resource);

        log.info("리소스 생성 완료: id={}", resource.getId());
        return resource;
    }
}
```

**주요 규칙**:
- `@Service`, `@RequiredArgsConstructor`, `@Slf4j` 사용
- `@Transactional` 사용
  - 읽기 전용: `@Transactional(readOnly = true)`
  - 쓰기: `@Transactional`
- `BusinessException` 던지기
- 주요 작업 전후로 로그 기록 (`log.info`, `log.error`)

### 5. DTO 패턴

**Request DTO**:
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCreateRequest {

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$",
             message = "올바른 전화번호 형식이 아닙니다")
    private String phoneNumber;
}
```

**Response DTO**:
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

    private Long id;
    private String name;
    private LocalDateTime createdAt;

    // Entity → DTO 변환 정적 메서드
    public static ResourceResponse from(Resource resource) {
        return ResourceResponse.builder()
            .id(resource.getId())
            .name(resource.getName())
            .createdAt(resource.getCreatedAt())
            .build();
    }
}
```

**주요 규칙**:
- Request/Response DTO 분리
- `@Getter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` 사용
- Jakarta Validation 사용 (`@NotBlank`, `@Email`, `@NotNull`, `@Pattern`, `@Min`, `@Max` 등)
- Response DTO에 `from(Entity)` 정적 메서드 제공

### 6. Repository 패턴

```java
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // 쿼리 메서드 네이밍 규칙
    Optional<Resource> findByIdAndIsDeletedFalse(Long id);

    List<Resource> findByUserEmailAndIsDeletedFalse(String userEmail);

    Page<Resource> findByIsDeletedFalse(Pageable pageable);

    boolean existsByEmail(String email);

    long countByStatusAndIsDeletedFalse(Status status);
}
```

**주요 규칙**:
- `JpaRepository<Entity, ID>` 상속
- Spring Data JPA 쿼리 메서드 네이밍 규칙 따름
- Soft delete를 고려하여 `IsDeletedFalse` 조건 추가
- 복잡한 쿼리는 `@Query` 사용

### 7. Redis 사용 패턴

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisService redisService;

    // Redis 키 prefix 상수
    private static final String SIGNUP_PREFIX = "signup:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    // TTL 상수
    private static final Duration SIGNUP_TTL = Duration.ofMinutes(10);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    public void saveToken(Long userId, String token) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        redisService.setValues(redisKey, token, REFRESH_TOKEN_TTL);
    }

    public String getToken(Long userId) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        return redisService.getValues(redisKey);
    }

    public void deleteToken(Long userId) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        redisService.deleteValues(redisKey);
    }
}
```

**주요 규칙**:
- Redis 키 prefix 상수 정의 (예: `"signup:"`, `"refresh:"`, `"otp:email:"`)
- TTL 상수 정의 (`Duration.ofMinutes()`, `Duration.ofDays()`)
- `RedisService` 메서드 사용:
  - `setValues(key, value, duration)` - 값 저장
  - `getValues(key)` - 값 조회
  - `deleteValues(key)` - 값 삭제
  - `setNX(key, value, duration)` - 멱등성 보장

### 8. Entity 패턴

```java
@Entity
@Table(name = "resources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Soft delete
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Resource(String name) {
        this.name = name;
    }

    // 비즈니스 메서드
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateName(String name) {
        this.name = name;
    }
}
```

**주요 규칙**:
- `@Entity`, `@Table`, `@Getter` 사용
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` - JPA 요구사항
- `@Builder` 패턴 사용
- Soft delete 필드 (`isDeleted`, `deletedAt`) 포함
- Setter 대신 비즈니스 메서드 제공

### 9. 페이지네이션 패턴

```java
@GetMapping
public ApiResponse<Page<ResourceResponse>> getResources(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {

    Page<Resource> resources = resourceService.getResources(pageable);

    // Page<Entity> → Page<DTO> 변환
    Page<ResourceResponse> response = resources.map(ResourceResponse::from);

    return ApiResponse.success(response);
}
```

**주요 규칙**:
- `@PageableDefault` 사용 (기본값 설정)
- Service에서 `Page<Entity>` 반환
- Controller에서 `Page.map()` 사용하여 DTO 변환

### 10. 참고할 코드 예시

새로운 기능 구현 시 아래 코드를 참고하세요:

**Controller 예시**:
- `domain/user/web/AuthController.java` - 인증 API
- `domain/estimate/web/EstimateController.java` - 견적/입찰 API
- `domain/payment/web/PaymentController.java` - 결제 API

**Service 예시**:
- `domain/user/service/AuthService.java` - 인증 비즈니스 로직
- `domain/estimate/service/EstimateRequestService.java` - 견적 요청 비즈니스 로직

**DTO 예시**:
- `domain/user/web/dto/SignupStartRequest.java` - Request DTO with validation
- `domain/estimate/web/dto/EstimateRequestResponse.java` - Response DTO with from()

---

## 📚 Important Documents

### 필수 참고 문서 (Must Read)

1. **`doc/work-log.md`** ⭐⭐⭐
   - 작업일지 및 프로젝트 진행 상황
   - 모든 작업 시작 전 반드시 확인
   - 완료된 작업, 진행 중인 작업, 예정된 작업 기록

2. **`doc/mermaid.txt`** ⭐⭐⭐
   - 전체 시스템 플로우 다이어그램 (26개)
   - 인증, 회원가입, 입찰, 콘테스트, 결제 등 모든 주요 플로우
   - API 구현 시 반드시 참고

3. **`doc/redis-postgresql-strategy.md`** ⭐⭐⭐
   - Redis와 PostgreSQL 데이터 분리 전략
   - 어떤 데이터를 어디에 저장할지 명확한 가이드
   - 캐싱, 세션, 토큰 관리 전략

