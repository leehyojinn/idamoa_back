# Gmail API 설정 가이드

이 문서는 Gmail API를 사용하여 이메일 인증을 구현하는 방법을 설명합니다.

## 📋 목차
1. [Google Cloud 설정](#1-google-cloud-설정)
2. [Service Account 생성](#2-service-account-생성)
3. [Domain-wide Delegation 설정](#3-domain-wide-delegation-설정)
4. [프로젝트 설정](#4-프로젝트-설정)
5. [테스트](#5-테스트)

---

## 1. Google Cloud 설정

### 1.1 Google Cloud Console 접속
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성 또는 기존 프로젝트 선택

### 1.2 Gmail API 활성화
1. **API 및 서비스 > 라이브러리** 메뉴 선택
2. "Gmail API" 검색
3. **사용 설정** 클릭

---

## 2. Service Account 생성

### 2.1 Service Account 만들기
1. **IAM 및 관리자 > 서비스 계정** 메뉴 선택
2. **서비스 계정 만들기** 클릭
3. 서비스 계정 세부정보:
   - **이름**: `damoa-gmail-sender` (예시)
   - **설명**: `다모아 이메일 발송용 서비스 계정`
4. **만들기** 클릭
5. 역할 선택은 **건너뛰기** (Domain-wide Delegation으로 권한 부여)
6. **완료** 클릭

### 2.2 키 파일 생성
1. 생성된 서비스 계정 클릭
2. **키** 탭 선택
3. **키 추가 > 새 키 만들기**
4. **JSON** 선택
5. **만들기** 클릭
6. 다운로드된 JSON 파일을 **안전한 위치에 저장**

### 2.3 Client ID 확인
1. 서비스 계정 세부정보에서 **고유 ID (Client ID)** 복사
   - 예: `123456789012345678901`
2. 이 값은 Domain-wide Delegation 설정에서 사용됩니다

---

## 3. Domain-wide Delegation 설정

**⚠️ 중요**: Google Workspace 관리자 권한이 필요합니다.

### 3.1 Admin Console에서 설정
1. [Google Admin Console](https://admin.google.com/) 접속
2. **보안 > 액세스 및 데이터 제어 > API 제어** 메뉴 선택
3. **도메인 전체 위임 관리** 클릭
4. **새로 추가** 클릭
5. 다음 정보 입력:
   - **클라이언트 ID**: 위에서 복사한 Service Account Client ID
   - **OAuth 범위**: `https://www.googleapis.com/auth/gmail.send`
6. **승인** 클릭

### 3.2 Service Account에서 Domain-wide Delegation 활성화
1. Google Cloud Console로 돌아가기
2. **IAM 및 관리자 > 서비스 계정** 선택
3. 생성한 서비스 계정 클릭
4. **세부정보** 탭에서 **고급 설정** 확장
5. **G Suite 도메인 전체 위임 사용 설정** 체크박스 활성화
6. **저장** 클릭

---

## 4. 프로젝트 설정

### 4.1 키 파일 저장
다운로드한 JSON 키 파일을 다음 위치에 저장:
```
C:\workspace\damoa\secrets\service-account.json
```

### 4.2 .env 파일 설정
`.env` 파일에 다음 환경변수 추가:

```bash
# Gmail API 설정
GMAIL_API_ENABLED=true
GMAIL_DELEGATED_USER=noreply@yourdomain.com
GMAIL_SERVICE_ACCOUNT_KEY=file:./secrets/service-account.json
GMAIL_APP_NAME=damoa
EMAIL_NOTIFICATION_ENABLED=true
EMAIL_FROM=noreply@yourdomain.com
```

**주의사항**:
- `GMAIL_DELEGATED_USER`: 실제 이메일을 발송할 Google Workspace 계정
- `EMAIL_FROM`: 발신자 이메일 주소
- `yourdomain.com`: 실제 도메인으로 변경

### 4.3 디렉토리 구조 확인
```
damoa/
├── secrets/
│   ├── service-account.json  ← 키 파일 (절대 Git 커밋 금지!)
│   └── README.md
├── .env                        ← 환경변수 설정
├── .gitignore                  ← secrets/ 포함 확인
└── ...
```

---

## 5. 테스트

### 5.1 애플리케이션 실행
```bash
./gradlew bootRun
```

### 5.2 이메일 인증 코드 발송 테스트

**API 엔드포인트**: `POST /api/auth/verification/email/send`

**요청 예시**:
```json
{
  "signupToken": "회원가입-토큰-UUID",
  "email": "user@example.com"
}
```

**cURL 예시**:
```bash
curl -X POST http://localhost:8080/api/auth/verification/email/send \
  -H "Content-Type: application/json" \
  -d '{
    "signupToken": "test-token-uuid",
    "email": "test@example.com"
  }'
```

### 5.3 로그 확인
애플리케이션 로그에서 다음 메시지 확인:
```
Gmail API 클라이언트 초기화 완료
이메일 인증 코드 생성: token=..., email=...
이메일 인증 코드 발송 완료: email=..., messageId=...
```

### 5.4 DB 확인
1. **notifications 테이블**: 발송된 이메일 정보
2. **notification_logs 테이블**: 발송 로그 (성공/실패)
3. **email_verifications 테이블**: 인증 이력

---

## 🔍 문제 해결 (Troubleshooting)

### 1. "Service Account 키 파일을 읽을 수 없습니다"
- 키 파일 경로 확인: `./secrets/service-account.json`
- 파일 권한 확인
- JSON 파일 유효성 검사

### 2. "Gmail API가 활성화되지 않았습니다"
- `.env` 파일에 `GMAIL_API_ENABLED=true` 설정 확인
- 애플리케이션 재시작

### 3. "403 Forbidden" 에러
- Domain-wide Delegation 설정 확인
- Service Account Client ID가 올바른지 확인
- OAuth 범위가 `https://www.googleapis.com/auth/gmail.send`인지 확인

### 4. "401 Unauthorized" 에러
- Service Account 키 파일이 올바른지 확인
- `GMAIL_DELEGATED_USER`가 실제 Google Workspace 계정인지 확인

### 5. "Template not found" 에러
- Flyway 마이그레이션 실행 확인: `V2__Add_email_verification_template.sql`
- DB에 `notification_templates` 테이블 및 데이터 확인

---

## 📚 참고 자료

- [Gmail API 공식 문서](https://developers.google.com/gmail/api)
- [Service Account 가이드](https://cloud.google.com/iam/docs/service-accounts)
- [Domain-wide Delegation](https://developers.google.com/identity/protocols/oauth2/service-account#delegatingauthority)
- [OAuth 2.0 범위](https://developers.google.com/gmail/api/auth/scopes)

---

## 🔒 보안 주의사항

1. **절대 Git에 커밋하지 마세요**:
   - `secrets/` 디렉토리는 `.gitignore`에 포함
   - Service Account 키 파일 유출 시 즉시 폐기

2. **키 파일 관리**:
   - 운영 환경에서는 환경변수 또는 비밀 관리 시스템 사용
   - AWS Secrets Manager, Google Secret Manager 등 활용

3. **권한 최소화**:
   - Service Account에는 이메일 발송 권한만 부여 (`gmail.send`)
   - 불필요한 권한 제거

4. **로그 모니터링**:
   - `notification_logs` 테이블로 발송 이력 추적
   - 비정상 발송 패턴 감지
