# EC2 개발 서버 초기 설정 스크립트

## 🎯 환경 정보

- **환경**: Development (개발 서버)
- **브랜치**: develop
- **DB 이름**: hip_damoa_dev
- **DB 사용자**: hip_damoa_user
- **Spring Profile**: dev

---

## 📝 설정 스크립트 (EC2에서 실행)

```bash
# EC2 SSH 접속
ssh -i [YOUR_PEM_FILE] ubuntu@[YOUR_EC2_IP]

# 1. 디렉토리 생성
mkdir -p ~/damoa/infra ~/damoa/app

# 2. 인프라 docker-compose 파일 생성
cd ~/damoa/infra
cat > docker-compose.infra.yml <<'EOF'
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
EOF

# 3. 인프라 환경변수 파일 생성
cat > .env <<'EOF'
DB_PASSWORD=YourStrongPassword123!@#
EOF

chmod 600 .env

# 4. 인프라 시작
docker compose -f docker-compose.infra.yml up -d

# 5. 인프라 상태 확인
docker compose -f docker-compose.infra.yml ps

# 6. 앱 환경변수 파일 생성
cd ~/damoa/app
cat > .env <<'EOF'
# GitHub Container Registry
GITHUB_REPOSITORY=interior-damoa/hip-damoa-backend

# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Database
DB_NAME=hip_damoa_dev
DB_USERNAME=hip_damoa_user
DB_PASSWORD=YourStrongPassword123!@#

# JWT
JWT_SECRET=V29vTGVlS2ltSmVvbmdNaW5Hb29kSGVsbG9Xb3JsZEhpSGVsbG9Xb3JsZEhpSGVsbG9Xb3JsZEhpSGVsbG9Xb3JsZEhp
JWT_ACCESS_TOKEN_EXPIRATION=86400000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# OAuth2 (나중에 설정)
OAUTH_REDIRECT_BASE_URL=http://[YOUR_EC2_IP]:8080
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# AWS S3 (나중에 설정)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_S3_BUCKET=damoa-dev-files
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
EOF

chmod 600 .env

# 7. 설정 완료!
echo "✅ EC2 인프라 설정 완료!"
echo "📌 다음 단계: GitHub Actions에서 자동 배포 시작"
```

---

## 🔍 확인 사항

### 인프라 상태 확인
```bash
cd ~/damoa/infra
docker compose -f docker-compose.infra.yml ps
```

예상 출력:
```
NAME                   STATUS    PORTS
damoa-postgres-dev    running   127.0.0.1:5432->5432/tcp
damoa-redis-dev       running   127.0.0.1:6379->6379/tcp
```

### PostgreSQL 연결 확인
```bash
docker exec -it damoa-postgres-dev psql -U hip_damoa_user -d hip_damoa_dev

# psql에서
\l  # 데이터베이스 목록
\q  # 종료
```

### Redis 연결 확인
```bash
docker exec -it damoa-redis-dev redis-cli ping
# 출력: PONG
```

---

## 🚀 배포 시작

EC2 설정 완료 후:

1. 로컬에서 develop 브랜치에 push
2. GitHub Actions 자동 실행
3. docker-compose.app.yml 자동 복사
4. App 컨테이너 자동 시작

---

## 📂 최종 디렉토리 구조

```
~/damoa/
├── infra/
│   ├── docker-compose.infra.yml
│   └── .env  (DB_PASSWORD만)
│
└── app/
    ├── docker-compose.app.yml  (GitHub Actions가 복사)
    └── .env  (앱 환경변수)
```

---

## 🔄 향후 작업

### Production 서버 설정 (main 브랜치)

나중에 Production 서버 설정 시:
- DB 이름: `hip_damoa_prod`
- DB 사용자: `hip_damoa_user`
- Spring Profile: `prod`
- 워크플로우: `.github/workflows/deploy-production.yml`

---

**작성일**: 2025-11-10
**환경**: Development
**브랜치**: develop → EC2 Dev
