# Redis와 PostgreSQL 데이터 저장 전략

## 기본 원칙

### Redis 사용 목적
- **임시 데이터** (TTL이 있는 데이터)
- **세션 관리** (빠른 조회가 필요한 인증 데이터)
- **캐싱** (자주 조회되는 데이터)
- **속도 제한** (Rate Limiting)
- **실시간 데이터** (임시 상태, 카운터)

### PostgreSQL 사용 목적
- **영구 저장** (비즈니스 데이터)
- **감사 추적** (Audit Log)
- **관계형 데이터** (복잡한 JOIN이 필요한 데이터)
- **트랜잭션** (ACID 보장이 필요한 데이터)

---

## 📌 Redis에서 관리할 데이터

### 1. **인증 관련 (Authentication)**

#### 1.1 JWT Refresh Token
```
키 형식: refresh_token:{user_id}:{device_id}
값: {token_string}
TTL: 14일 (604800초)

예시:
refresh_token:user123:browser_abc -> "eyJhbGciOiJIUzI1NiIs..."
```

**이유:**
- 빠른 조회 속도 필요 (매 요청마다 검증)
- 자동 만료 (TTL)
- 로그아웃 시 즉시 삭제 가능
- RDB에 저장할 필요 없음 (만료되면 자동 삭제)

#### 1.2 JWT Blacklist (로그아웃된 Access Token)
```
키 형식: blacklist:access:{jti}
값: revoked_at timestamp
TTL: Access Token 잔여 만료 시간

예시:
blacklist:access:abc123 -> "2025-10-15T10:30:00Z"
```

**이유:**
- Access Token 만료 전까지만 유지하면 됨
- 빠른 조회 속도 (매 요청마다 체크)
- TTL 자동 관리

#### 1.3 소셜 로그인 임시 State
```
키 형식: oauth:state:{state_token}
값: {user_id, provider, redirect_url}
TTL: 10분 (600초)

예시:
oauth:state:xyz789 -> {"user_id":"user123","provider":"kakao","redirect_url":"/dashboard"}
```

**이유:**
- OAuth 플로우는 짧은 시간 내에 완료
- CSRF 방지를 위한 임시 토큰
- 사용 후 즉시 삭제

### 2. **이메일/SMS 인증**

#### 2.1 이메일 인증 토큰
```
키 형식: email_verify:{token}
값: {user_id, email, created_at}
TTL: 15분 (900초)

예시:
email_verify:abc123def -> {"user_id":"user123","email":"test@example.com","created_at":"2025-10-15T10:00:00Z"}
```

**이유:**
- 짧은 유효기간 (15분)
- 사용 후 삭제 (일회성)
- RDB에 저장할 필요 없음

#### 2.2 SMS 인증 코드
```
키 형식: sms_otp:{phone_number}
값: {code, attempt_count, created_at}
TTL: 3분 (180초)

예시:
sms_otp:01012345678 -> {"code":"123456","attempt_count":1,"created_at":"2025-10-15T10:00:00Z"}
```

**이유:**
- 매우 짧은 유효기간 (3분)
- 시도 횟수 제한 (5회)
- 사용 후 삭제

#### 2.3 이메일/SMS 발송 속도 제한
```
키 형식: rate_limit:email:{email}
값: send_count
TTL: 1시간 (3600초)

예시:
rate_limit:email:test@example.com -> 3
```

**이유:**
- 시간당 발송 횟수 제한
- 자동 리셋 (TTL)

### 3. **회원가입 2단계 프로세스**

#### 3.1 회원가입 임시 데이터
```
키 형식: signup:temp:{signup_token}
값: {email, password_hash, name, phone, agree_terms, agree_privacy}
TTL: 10분 (600초)

예시:
signup:temp:token123 -> {"email":"user@example.com","password":"$2a$10$...","name":"홍길동",...}
```

**이유:**
- 이메일/SMS 인증 전까지 임시 저장
- 인증 완료 후 PostgreSQL로 이동
- 미완료 가입 자동 삭제 (TTL)

### 4. **세션 관리**

#### 4.1 사용자 활성 세션
```
키 형식: session:user:{user_id}
값: {last_activity, ip_address, user_agent}
TTL: 1시간 (3600초, 활동 시 갱신)

예시:
session:user:user123 -> {"last_activity":"2025-10-15T10:30:00Z","ip":"192.168.1.1",...}
```

**이유:**
- 빠른 세션 체크
- 비활동 자동 로그아웃
- 동시 접속 제어 가능

### 5. **캐싱**

#### 5.1 업체 프로필 캐시
```
키 형식: cache:company:{company_id}
값: {전체 업체 프로필 JSON}
TTL: 1시간 (3600초)

예시:
cache:company:comp123 -> {"company_name":"다모아인테리어","rating":4.8,...}
```

**이유:**
- 자주 조회되는 데이터
- 업데이트 시 캐시 무효화
- DB 부하 감소

#### 5.2 인기 게시글 캐시
```
키 형식: cache:board:popular
값: [board_id 배열]
TTL: 5분 (300초)

예시:
cache:board:popular -> [123, 456, 789]
```

**이유:**
- 실시간성이 필요하지 않음
- 빠른 조회
- 주기적 갱신

### 6. **실시간 카운터**

#### 6.1 조회수 임시 카운터
```
키 형식: counter:view:{entity_type}:{entity_id}
값: view_count
TTL: 없음 (주기적으로 PostgreSQL에 동기화)

예시:
counter:view:board:123 -> 50
```

**이유:**
- 매번 DB 업데이트 방지
- 배치로 주기적 동기화 (5분마다)
- 성능 향상

#### 6.2 좋아요 임시 카운터
```
키 형식: counter:like:{entity_type}:{entity_id}
값: like_count

예시:
counter:like:board:123 -> 25
```

### 7. **Rate Limiting (속도 제한)**

#### 7.1 API 요청 제한
```
키 형식: rate_limit:api:{user_id}
값: request_count
TTL: 1분 (60초)

예시:
rate_limit:api:user123 -> 50
```

**이유:**
- DDoS 방지
- 공정한 리소스 사용
- 자동 리셋

#### 7.2 제안서 제출 제한
```
키 형식: rate_limit:proposal:{company_id}
값: proposal_count
TTL: 1시간 (3600초)

예시:
rate_limit:proposal:comp123 -> 5
```

---

## 📌 PostgreSQL에서 관리할 데이터

### 1. **사용자 데이터 (영구)**
- `users` - 사용자 계정 (이메일, 비밀번호, 역할)
- `user_profiles` - 사용자 프로필
- `company_profiles` - 업체 프로필
- `designer_profiles` - 디자이너 프로필

**이유:** 비즈니스 핵심 데이터, 영구 보관 필요

### 2. **소셜 로그인 연동 정보**
- `social_accounts` - OAuth 연동 정보 (provider, provider_user_id)
- **❌ Access Token, Refresh Token은 저장하지 않음** (Redis로만 관리)
- **✅ Provider User ID만 저장** (재로그인 시 사용자 매칭용)

**이유:**
- 토큰은 민감 정보 (Redis에서 암호화하여 임시 저장)
- Provider User ID는 사용자 식별용 (영구 보관)

### 3. **인증 이력 (감사용)**
- `email_verification_logs` - 이메일 인증 시도 이력 (성공/실패)
- `sms_verification_logs` - SMS 인증 시도 이력
- **❌ 인증 코드/토큰은 저장하지 않음** (Redis에서만 관리)

**이유:**
- 감사 추적 (Audit)
- 부정 행위 탐지
- 통계 분석

### 4. **비즈니스 데이터**
- `estimate_requests` - 견적 요청
- `estimate_proposals` - 견적 제안
- `matches` - 매칭
- `design_contests` - 디자인 콘테스트
- `contest_entries` - 콘테스트 참가작
- `draw_requests` - 도면 요청
- `planner_requests` - 플래너 요청

**이유:** 영구 보관, 복잡한 쿼리, 트랜잭션 필요

### 5. **결제/구독**
- `subscription_plans` - 구독 플랜
- `subscriptions` - 사용자 구독
- `invoices` - 청구서
- `payments` - 결제 내역
- `payment_logs` - 결제 로그 (Webhook)
- `credit_transactions` - 크레딧 거래

**이유:** 금융 데이터, ACID 보장 필수

### 6. **콘텐츠**
- `boards` - 게시판
- `files` - 파일 메타데이터
- `reviews` - 리뷰
- `tags` - 태그

**이유:** 영구 보관, 검색, 정렬 필요

### 7. **감사 로그**
- `audit_logs` - 시스템 감사 로그
- `download_logs` - 다운로드 이력
- `campaign_analytics` - 캠페인 분석

**이유:** 규정 준수, 통계 분석

---

## 🔄 데이터 동기화 전략

### 1. Redis → PostgreSQL 동기화

#### 조회수/좋아요 카운터
```
스케줄: 5분마다 배치 실행
동작:
1. Redis에서 counter:view:*, counter:like:* 읽기
2. PostgreSQL에 UPSERT (ON CONFLICT UPDATE)
3. Redis 카운터 초기화
```

#### 활동 로그
```
스케줄: 실시간 (이벤트 발생 시)
동작:
1. Redis에 임시 저장 (빠른 응답)
2. 비동기 큐로 PostgreSQL에 영구 저장
3. Redis 데이터 TTL 만료 대기
```

### 2. PostgreSQL → Redis 캐싱

#### 자주 조회되는 데이터
```
전략: Cache-Aside Pattern
동작:
1. 캐시 조회 (Redis)
2. 캐시 미스 → DB 조회 (PostgreSQL)
3. 캐시에 저장 (Redis, TTL 설정)
4. 데이터 업데이트 시 캐시 무효화
```

---

## 🛡️ 보안 고려사항

### Redis 데이터 암호화
- **Refresh Token**: AES-256 암호화 후 저장
- **소셜 로그인 State**: HMAC 서명
- **인증 코드**: 해시 후 저장 (선택적)

### Redis 접근 제어
- **비밀번호 설정**: `requirepass`
- **명령어 제한**: `rename-command` (FLUSHALL, FLUSHDB 비활성화)
- **네트워크 격리**: VPC 내부에서만 접근

### PostgreSQL 암호화
- **민감 정보 암호화**: 이메일, 전화번호 (선택적)
- **비밀번호**: bcrypt (강도 10 이상)
- **결제 정보**: PCI-DSS 준수

---

## 📊 모니터링

### Redis 모니터링
- **메모리 사용량**: `INFO memory`
- **키 개수**: `DBSIZE`
- **만료 키**: `INFO stats` (expired_keys)
- **히트율**: cache_hits / (cache_hits + cache_misses)

### PostgreSQL 모니터링
- **슬로우 쿼리**: `pg_stat_statements`
- **연결 수**: `pg_stat_activity`
- **테이블 크기**: `pg_total_relation_size`

---

## 🚀 성능 최적화

### Redis
- **파이프라이닝**: 여러 명령어 배치 실행
- **연결 풀링**: Lettuce/Jedis 연결 풀 사용
- **적절한 TTL 설정**: 메모리 낭비 방지

### PostgreSQL
- **인덱스 최적화**: 자주 조회되는 컬럼
- **파티셔닝**: 대용량 테이블 (로그 테이블)
- **연결 풀링**: HikariCP 설정

---

## 📋 체크리스트

### Redis로 이동해야 할 항목
- [x] JWT Refresh Token
- [x] 이메일 인증 토큰
- [x] SMS 인증 코드
- [x] 회원가입 임시 데이터
- [x] 소셜 로그인 State
- [x] 세션 데이터
- [x] 조회수/좋아요 임시 카운터
- [x] API Rate Limiting

### PostgreSQL에 유지해야 할 항목
- [x] 사용자 계정 (users)
- [x] 소셜 로그인 연동 정보 (provider_user_id만)
- [x] 인증 이력 로그 (감사용)
- [x] 비즈니스 데이터 (견적, 콘테스트 등)
- [x] 결제/구독 데이터
- [x] 콘텐츠 데이터
- [x] 감사 로그

### 제거해야 할 PostgreSQL 테이블
- [ ] `email_verifications` - Redis로 이동
- [ ] `sms_verifications` - Redis로 이동
- [ ] `refresh_tokens` - Redis로 이동

---

## 🔧 구현 예시

### Spring Boot + Redis 설정

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // JSON 직렬화
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
```

### Refresh Token 저장 (Redis)

```java
@Service
public class TokenService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void saveRefreshToken(String userId, String deviceId, String token) {
        String key = "refresh_token:" + userId + ":" + deviceId;
        redisTemplate.opsForValue().set(key, token, 14, TimeUnit.DAYS);
    }

    public String getRefreshToken(String userId, String deviceId) {
        String key = "refresh_token:" + userId + ":" + deviceId;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(String userId, String deviceId) {
        String key = "refresh_token:" + userId + ":" + deviceId;
        redisTemplate.delete(key);
    }
}
```

### 이메일 인증 토큰 (Redis)

```java
@Service
public class EmailVerificationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveVerificationToken(String token, EmailVerificationDto dto) {
        String key = "email_verify:" + token;
        redisTemplate.opsForValue().set(key, dto, 15, TimeUnit.MINUTES);
    }

    public EmailVerificationDto getVerificationData(String token) {
        String key = "email_verify:" + token;
        return (EmailVerificationDto) redisTemplate.opsForValue().get(key);
    }

    public void deleteVerificationToken(String token) {
        String key = "email_verify:" + token;
        redisTemplate.delete(key);
    }
}
```

---

## 📝 요약

| 데이터 유형 | Redis | PostgreSQL | 이유 |
|------------|-------|------------|------|
| Refresh Token | ✅ | ❌ | 임시, TTL, 빠른 조회 |
| 소셜 로그인 Token | ✅ | ❌ | 민감, 임시 |
| 소셜 로그인 연동 정보 | ❌ | ✅ (provider_user_id만) | 영구, 사용자 식별 |
| 이메일/SMS 인증 코드 | ✅ | ❌ | 짧은 TTL, 일회성 |
| 인증 이력 로그 | ❌ | ✅ | 감사 추적 |
| 회원가입 임시 데이터 | ✅ | ❌ | 인증 전 임시 |
| 세션 | ✅ | ❌ | 빠른 조회, TTL |
| 조회수/좋아요 카운터 | ✅ (임시) | ✅ (동기화) | 성능, 주기적 동기화 |
| 사용자 계정 | ❌ | ✅ | 영구, 핵심 데이터 |
| 결제/구독 | ❌ | ✅ | ACID, 금융 데이터 |
| 비즈니스 데이터 | ❌ | ✅ | 영구, 복잡한 쿼리 |

**핵심 원칙**: Redis는 **빠르고 임시적인** 데이터, PostgreSQL은 **영구적이고 중요한** 데이터!
