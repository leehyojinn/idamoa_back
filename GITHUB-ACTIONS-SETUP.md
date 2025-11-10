# GitHub Actions 자동 배포 설정 가이드

## 🌿 브랜치 전략

```
local (로컬 개발)
  ↓ PR
develop (개발 서버) → 자동 배포 to EC2 Dev
  ↓ PR
main (프로덕션) → 자동 배포 to EC2 Prod
```

### 환경별 DB 이름
- **Local**: `hip_damoa_local` (application.yml)
- **Dev**: `hip_damoa_dev` (application-dev.yml, SPRING_PROFILES_ACTIVE=dev)
- **Prod**: `hip_damoa_prod` (application-prod.yml, SPRING_PROFILES_ACTIVE=prod)

---

## 🎯 배포 흐름 (Develop → Dev 서버)

```
1. 코드 수정 → Git push origin develop
2. GitHub Actions 자동 실행
3. Docker 이미지 빌드
4. GitHub Container Registry (GHCR)에 푸시
5. EC2 SSH 접속
6. App 컨테이너만 재시작 (인프라는 유지!)
7. 배포 완료! 🎉
```

### 📁 EC2 디렉토리 구조

```
~/damoa/
├── infra/                           # 인프라 (PostgreSQL, Redis)
│   ├── docker-compose.infra.yml     # 인프라 전용 compose (수동 관리)
│   └── .env                         # DB_PASSWORD만 포함
│
└── app/                             # 애플리케이션
    ├── docker-compose.app.yml       # GitHub Actions가 자동 복사
    └── .env                         # 앱 환경변수 (수동 관리)
```

**장점**:
- ✅ **인프라는 한 번만 시작** (데이터 보존, 영구 실행)
- ✅ **앱만 자동 배포** (빠른 배포, 다운타임 최소화)
- ✅ **Lightsail 패턴과 동일** (nginx, postgres 별도 관리)
- ✅ **EC2별 커스터마이징 가능** (인프라 설정은 Git 관리 안 함)

---

## 📋 사전 준비사항

### 1. GitHub Repository
- **Secrets 설정 권한 필요**
- **Packages 권한 필요** (GHCR 사용)
- 별도의 Docker Hub 계정 불필요!

### 2. EC2 SSH 키
- 이미 가지고 있는 `.pem` 키 파일

---

## 🔐 GitHub Secrets 설정

### Step 1: GitHub Repository → Settings → Secrets and variables → Actions

### Step 2: 다음 Secrets 추가 (총 3개)

#### 1️⃣ EC2_HOST
```
값: 
설명: EC2 퍼블릭 IP 주소
```

#### 2️⃣ EC2_SSH_KEY
```
값: -----BEGIN RSA PRIVATE KEY-----
     (전체 .pem 파일 내용)
     -----END RSA PRIVATE KEY-----
설명: EC2 SSH 프라이빗 키 (.pem 파일 내용 전체)
```

**키 파일 내용 복사 방법 (Windows)**:
```powershell
# PowerShell에서
Get-Content C:\workspace\awskey\.ssh\hip-damoa-develop-key.pem | Set-Clipboard

# 클립보드에 복사됨 → GitHub Secrets에 붙여넣기
```

#### 3️⃣ SLACK_WEBHOOK_URL (선택사항)
```
값: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
설명: Slack 알림용 (나중에 설정 가능)
```

**⚠️ 중요**: `GITHUB_TOKEN`은 **자동으로 제공**되므로 따로 설정할 필요 없습니다!

---

## 🐳 GitHub Container Registry (GHCR) 설정

### GHCR 장점
- GitHub에 통합되어 있어 **별도 계정 불필요**
- **무제한 프라이빗 이미지** 저장 가능
- GitHub Actions와 **자동 연동**
- `GITHUB_TOKEN`으로 인증 (별도 토큰 불필요)

### Repository Package 권한 설정

1. **워크플로우 실행 후 자동 생성**됨 (첫 배포 시)
2. 이미지 경로: `ghcr.io/[organization]/[repository]:tag`
   - 예: `ghcr.io/interior-damoa/hip-damoa-backend:latest`

### Private Package 접근 설정 (선택사항)

프라이빗 저장소의 경우, EC2에서 이미지를 pull하려면:

```bash
# EC2에서 GitHub에 로그인 (1회만)
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

**Personal Access Token (PAT) 생성 방법**:
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. **Generate new token (classic)** 클릭
3. Note: `damoa-ec2-deploy`
4. Scopes: **`read:packages`** 체크
5. **Generate token** → 토큰 복사 (다시 볼 수 없음!)
6. EC2에서 위 명령으로 로그인

---

## 📝 EC2에 인프라 및 환경 설정

EC2 SSH 접속 후:

```bash
cd ~
mkdir -p damoa/infra
mkdir -p damoa/app
```

### 1. 인프라 설정 (PostgreSQL, Redis)

```bash
cd ~/damoa/infra
vim docker-compose.infra.yml
```

**`docker-compose.infra.yml` 내용**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: damoa-postgres-dev
    restart: unless-stopped
    environment:
      POSTGRES_DB: hip_damoa_dev
      POSTGRES_USER: hip_damoa_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --locale=C"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:5432:5432"
    networks:
      - damoa-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U hip_damoa_user -d hip_damoa_dev"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: damoa-redis-dev
    restart: unless-stopped
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    ports:
      - "127.0.0.1:6379:6379"
    networks:
      - damoa-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-data:
    driver: local
  redis-data:
    driver: local

networks:
  damoa-network:
    driver: bridge
```

**인프라 시작 (한 번만 실행)**:
```bash
cd ~/damoa/infra

# .env 파일 생성 (DB_PASSWORD만 필요)
echo "DB_PASSWORD=YourStrongPassword123!@#" > .env

# 인프라 컨테이너 시작
docker compose -f docker-compose.infra.yml up -d

# 상태 확인
docker compose -f docker-compose.infra.yml ps
```

### 2. 애플리케이션 환경변수 설정

```bash
cd ~/damoa/app
vim .env
```

**`.env` 파일 내용**:
```bash
# GitHub Container Registry
GITHUB_REPOSITORY=interior-damoa/hip-damoa-backend

# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Database
DB_NAME=hip_damoa_dev
DB_USERNAME=hip_damoa_user
DB_PASSWORD=YourStrongPassword123!@#

# JWT
JWT_SECRET=your-jwt-secret-base64-encoded
JWT_ACCESS_TOKEN_EXPIRATION=86400000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# OAuth2 (나중에 설정)
OAUTH_REDIRECT_BASE_URL=http://43.203.237.51:8080
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# AWS S3 (나중에 설정)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_S3_BUCKET=
AWS_REGION=ap-northeast-2

# Payment (나중에 설정)
KAKAOPAY_ADMIN_KEY=
KAKAOPAY_CID=
TOSS_SECRET_KEY=
TOSS_CLIENT_KEY=
INICIS_MID=
INICIS_SIGN_KEY=

# Notifications (나중에 설정)
EMAIL_NOTIFICATION_ENABLED=false
SMS_NOTIFICATION_ENABLED=false
```

**권한 설정**:
```bash
chmod 600 .env
```

**⚠️ 중요**: `GITHUB_REPOSITORY`는 반드시 정확한 경로로 설정하세요!
- 형식: `organization/repository` 또는 `username/repository`
- 예: `interior-damoa/hip-damoa-backend`

---

## 🚀 배포 테스트

### 1. Git Push로 배포 트리거

```powershell
cd C:\workspace\damoa

# 변경사항 확인
git status

# 파일 추가
git add .github/workflows/deploy-develop.yml
git add docker-compose.app.yml
git add Dockerfile

# 커밋
git commit -m "feat: GitHub Actions 자동 배포 설정 (Develop, 인프라 분리)"

# Push (자동 배포 시작!)
git push origin develop
```

### 2. GitHub Actions 확인

1. GitHub Repository → **Actions** 탭
2. 워크플로우 실행 상태 확인
3. 로그 실시간 확인 가능

### 3. 배포 진행 상황

```
✅ Checkout code
✅ Set up JDK 17
✅ Build with Gradle
✅ Build and push Docker image
✅ Deploy to EC2
✅ Send Slack notification
```

### 4. EC2에서 확인

```bash
# SSH 접속
ssh -i C:\workspace\awskey\.ssh\hip-damoa-develop-key.pem ubuntu@43.203.237.51

# 인프라 상태 확인
cd ~/damoa/infra
docker compose -f docker-compose.infra.yml ps

# 앱 상태 확인
cd ~/damoa/app
docker compose -f docker-compose.app.yml ps

# 앱 로그 확인
docker compose -f docker-compose.app.yml logs -f app

# 헬스체크
curl http://localhost:8080/actuator/health
```

---

## 🔧 워크플로우 구조 설명

### Job 1: build-and-push
```yaml
1. Checkout 코드
2. JDK 17 설정
3. Gradle 빌드 (테스트 제외)
4. Docker 이미지 빌드
5. GitHub Container Registry (GHCR)에 푸시
   - 태그: latest, v1.0.0, dev.TIMESTAMP, git-commit-sha
```

### Job 2: deploy
```yaml
1. EC2 SSH 접속
2. docker-compose.prod.yml 복사
3. GHCR에서 Docker 이미지 pull
4. 기존 컨테이너 중지
5. 새 컨테이너 시작
6. 헬스체크 대기
7. Slack 알림 (성공/실패)
```

---

## 🎨 배포 버전 관리

### 이미지 태그 전략

```bash
# 1. latest (항상 최신)
ghcr.io/interior-damoa/hip-damoa-backend:latest

# 2. 버전 태그 (build.gradle의 version)
ghcr.io/interior-damoa/hip-damoa-backend:1.0.0

# 3. 타임스탬프 태그 (개발 환경)
ghcr.io/interior-damoa/hip-damoa-backend:dev.20251110.025800

# 4. Git Commit SHA (롤백용)
ghcr.io/interior-damoa/hip-damoa-backend:a1b2c3d
```

### 특정 버전으로 롤백

**방법 1: docker-compose.app.yml 수정**
```bash
# EC2 SSH 접속
cd ~/damoa/app
vim docker-compose.app.yml

# app 서비스의 image 수정
# image: ghcr.io/interior-damoa/hip-damoa-backend:a1b2c3d

# App 재시작
docker compose -f docker-compose.app.yml down
docker compose -f docker-compose.app.yml up -d
```

**방법 2: 직접 pull**
```bash
cd ~/damoa/app

# 특정 버전 pull
docker pull ghcr.io/interior-damoa/hip-damoa-backend:dev.20251110.025800

# 태그 변경
docker tag ghcr.io/interior-damoa/hip-damoa-backend:dev.20251110.025800 \
            ghcr.io/interior-damoa/hip-damoa-backend:latest

# App 재시작
docker compose -f docker-compose.app.yml down
docker compose -f docker-compose.app.yml up -d
```

---

## 📊 모니터링 & 로그

### GitHub Actions 로그
```
GitHub Repository → Actions → 워크플로우 클릭 → 로그 확인
```

### EC2 애플리케이션 로그
```bash
cd ~/damoa/app

# 실시간 로그
docker compose -f docker-compose.app.yml logs -f app

# 마지막 100줄
docker compose -f docker-compose.app.yml logs --tail=100 app

# 특정 시간대 로그
docker compose -f docker-compose.app.yml logs --since 30m app
```

### GHCR 이미지 확인
```
# GitHub Repository → Packages
https://github.com/orgs/interior-damoa/packages?repo_name=hip-damoa-backend

# 또는
https://github.com/interior-damoa/hip-damoa-backend/pkgs/container/hip-damoa-backend
```

---

## 🔥 문제 해결

### 1. Build 실패
```bash
# Gradle 빌드 오류
# → GitHub Actions 로그에서 에러 확인
# → 로컬에서 ./gradlew clean build 테스트
```

### 2. GHCR Push 실패
```yaml
# 에러: denied: permission_denied
# 원인: Repository의 Packages 권한 없음
# 해결:
# 1. GitHub Repository → Settings → Actions → General
# 2. Workflow permissions → Read and write permissions 체크
# 3. 워크플로우 재실행
```

**또는**
```yaml
# 에러: denied: installation not allowed
# 원인: GITHUB_TOKEN 권한 부족
# 해결: .github/workflows/deploy-develop.yml 확인
permissions:
  contents: read
  packages: write  # 이 부분이 있는지 확인
```

### 3. EC2에서 이미지 Pull 실패
```bash
# 에러: denied: permission_denied
# 원인: 프라이빗 이미지 인증 필요
# 해결: EC2에서 GHCR 로그인

# Personal Access Token 생성 후
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 이미지 pull 재시도
cd ~/damoa/app
docker compose -f docker-compose.app.yml pull
```

### 4. 인프라 컨테이너가 실행되지 않음
```bash
# 인프라가 먼저 실행되어 있어야 함!
cd ~/damoa/infra
docker compose -f docker-compose.infra.yml ps

# 실행 안 되어 있으면 시작
docker compose -f docker-compose.infra.yml up -d

# 네트워크 확인
docker network ls | grep damoa-network
```

### 5. EC2 SSH 접속 실패
```bash
# EC2_SSH_KEY 확인
# → .pem 파일 내용 전체 복사했는지 확인
# → EC2 보안 그룹에서 22번 포트 열려있는지 확인
```

### 6. 컨테이너 시작 실패
```bash
# EC2에서 로그 확인
cd ~/damoa/app
docker compose -f docker-compose.app.yml logs app

# 환경 변수 확인
docker compose -f docker-compose.app.yml config

# .env 파일 확인
cat .env

# GITHUB_REPOSITORY 값 확인 (반드시!)
grep GITHUB_REPOSITORY .env
# → interior-damoa/hip-damoa-backend 여야 함
```

### 7. 헬스체크 실패
```bash
# 애플리케이션 로그 확인
cd ~/damoa/app
docker compose -f docker-compose.app.yml logs app

# PostgreSQL 연결 확인
docker exec -it damoa-postgres-dev psql -U hip_damoa_user -d hip_damoa_dev

# Redis 연결 확인
docker exec -it damoa-redis-dev redis-cli ping
```

---

## ✅ 체크리스트

### GitHub 설정
- [ ] GitHub Secrets 설정 (3개)
  - [ ] EC2_HOST
  - [ ] EC2_SSH_KEY
  - [ ] SLACK_WEBHOOK_URL (선택사항)
- [ ] GitHub Actions 권한 설정
  - [ ] Repository → Settings → Actions → General
  - [ ] Workflow permissions → **Read and write permissions** 체크
- [ ] `.github/workflows/deploy-develop.yml` 생성
- [ ] `docker-compose.app.yml` 생성 (앱 배포용)
- [ ] `Dockerfile` 생성

### GHCR 설정
- [ ] 별도 설정 불필요 (첫 배포 시 자동 생성)
- [ ] (선택) EC2에서 프라이빗 이미지 pull을 위한 PAT 생성 및 로그인

### EC2 인프라 설정 (한 번만)
- [ ] EC2 인스턴스 생성 (Ubuntu 22.04, t3.small)
- [ ] 보안 그룹 설정 (22, 8080 포트)
- [ ] Docker & Docker Compose 설치
- [ ] `~/damoa/infra` 디렉토리 생성
- [ ] `~/damoa/infra/docker-compose.infra.yml` 파일 생성
- [ ] `~/damoa/infra/.env` 파일 생성 (DB_PASSWORD)
- [ ] 인프라 컨테이너 시작 (`docker compose up -d`)

### EC2 앱 설정
- [ ] `~/damoa/app` 디렉토리 생성
- [ ] `~/damoa/app/.env` 파일 생성 및 설정
- [ ] (선택) GHCR 로그인 (프라이빗 저장소인 경우)

### 배포 테스트
- [ ] Git push → GitHub Actions 실행
- [ ] Docker 이미지 빌드 성공
- [ ] GHCR에 이미지 푸시 성공
- [ ] EC2 배포 성공
- [ ] 헬스체크 통과
- [ ] API 테스트 성공 (http://43.203.237.51:8080/actuator/health)

---

## 🎉 완료!

이제 **develop 브랜치에 push만 하면 자동으로 배포**됩니다!

```bash
git add .
git commit -m "feat: 새 기능 추가"
git push origin develop
# 👆 이것만 하면 자동 배포!
```

**배포 확인**:
- GitHub Actions: https://github.com/interior-damoa/hip-damoa-backend/actions
- GHCR 이미지: https://github.com/interior-damoa/hip-damoa-backend/pkgs/container/hip-damoa-backend
- EC2 API: http://[YOUR-EC2-IP]:8080/actuator/health

---

**작성일**: 2025-11-10
**업데이트**: 2025-11-10 (GHCR + 인프라/앱 분리 + 브랜치 전략)
**다음 단계**:
- [ ] Production 배포 워크플로우 (deploy-production.yml)
- [ ] Nginx 리버스 프록시 & SSL 인증서
