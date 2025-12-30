# Production 배포 가이드 (i-damoa.com)

## 개요

Dev 서버와 동일한 구조로 Prod 서버를 배포합니다.
**유일한 차이점: Nginx + SSL 추가**

---

## Dev vs Prod 비교

### 파일 구조

```
프로젝트 (Git)
├── docker-compose.dev.yml        # Dev 앱
├── docker-compose.prod.yml       # Prod 앱
├── docker-compose.infra.yml      # Dev 인프라
├── docker-compose.infra-prod.yml # Prod 인프라
└── nginx/                        # Prod만 사용 (SSL)
    ├── docker-compose.yml
    └── config/default.conf
```

### 서버 구조

```
# Dev 서버 (현재)
~/damoa/
├── app/
│   ├── docker-compose.dev.yml
│   ├── .env
│   └── secrets/
└── infra/
    ├── docker-compose.infra.yml
    └── .env

# Prod 서버 (신규)
~/damoa/
├── app/
│   ├── docker-compose.prod.yml
│   ├── .env
│   └── secrets/
├── infra/
│   ├── docker-compose.infra-prod.yml
│   └── .env
└── nginx/                        ← 유일한 차이
    ├── docker-compose.yml
    ├── config/default.conf
    └── certbot/                  ← SSL 인증서 (서버에서 생성)
```

### 배포 방식

| 컴포넌트 | Dev | Prod | 비고 |
|---------|-----|------|------|
| App | GitHub Actions 자동 | GitHub Actions 자동 | 동일 |
| Infra (DB/Redis) | 수동 1회 | 수동 1회 | 동일 |
| Nginx | 없음 | 수동 1회 | **Prod만** |

---

## 초기 설정 (1회)

### 1. EC2 인스턴스 준비

```bash
# Docker 설치 (Ubuntu)
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# 재로그인
exit
```

### 2. DNS 설정

도메인 관리 사이트에서 A 레코드 추가:

```
i-damoa.com     → [EC2 Elastic IP]
www.i-damoa.com → [EC2 Elastic IP]
api.i-damoa.com → [EC2 Elastic IP]
```

### 3. 프로젝트 클론

```bash
cd ~
git clone [repository-url] damoa-src
```

### 4. 디렉토리 구조 생성

```bash
# App
mkdir -p ~/damoa/app/secrets
cp ~/damoa-src/docker-compose.prod.yml ~/damoa/app/docker-compose.yml

# Infra
mkdir -p ~/damoa/infra
cp ~/damoa-src/docker-compose.infra-prod.yml ~/damoa/infra/docker-compose.yml

# Nginx
cp -r ~/damoa-src/nginx ~/damoa/nginx
mkdir -p ~/damoa/nginx/certbot/www ~/damoa/nginx/certbot/letsencrypt
```

### 5. 환경변수 설정

**~/damoa/infra/.env:**
```bash
DB_NAME=hip_damoa
DB_USERNAME=hip_damoa_user
DB_PASSWORD=[secure-password]
```

**~/damoa/app/.env:**
```bash
# GitHub Container Registry
GITHUB_REPOSITORY=interior-damoa/i-damoa-backend
IMAGE_TAG=latest

# Database (infra와 동일)
DB_NAME=hip_damoa
DB_USERNAME=hip_damoa_user
DB_PASSWORD=[secure-password]

# JWT
JWT_SECRET=[base64-encoded-secret]

# OAuth
OAUTH_REDIRECT_BASE_URL=https://i-damoa.com
OAUTH_FRONTEND_REDIRECT_URL=https://i-damoa.com/auth/callback

# Swagger
SWAGGER_SERVER_URL=https://api.i-damoa.com

# CORS
CORS_ALLOWED_ORIGINS=https://i-damoa.com,https://www.i-damoa.com

# ... (기타 설정)
```

### 6. Infra 시작

```bash
cd ~/damoa/infra
docker compose up -d

# 확인
docker ps | grep damoa
```

### 7. SSL 인증서 발급

```bash
cd ~/damoa/nginx

# nginx 먼저 시작 (SSL 설정 임시 비활성화)
# config/default.conf에서 443 server 블록을 주석처리하거나
# 또는 아래처럼 임시 설정 사용

# 임시 HTTP only 설정
cat > config/default.conf << 'EOF'
server {
    listen 80;
    server_name i-damoa.com www.i-damoa.com api.i-damoa.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 'SSL Setup in Progress';
        add_header Content-Type text/plain;
    }
}
EOF

# nginx 시작
docker compose up -d nginx

# 인증서 발급
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d i-damoa.com \
  -d www.i-damoa.com \
  -d api.i-damoa.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# 원래 설정 복원
git checkout ~/damoa-src/nginx/config/default.conf
cp ~/damoa-src/nginx/config/default.conf ~/damoa/nginx/config/

# 전체 서비스 시작
docker compose up -d
```

### 8. App 시작

```bash
cd ~/damoa/app

# GHCR 로그인
echo $GITHUB_TOKEN | docker login ghcr.io -u [username] --password-stdin

# 앱 시작
docker compose pull
docker compose up -d

# 로그 확인
docker compose logs -f
```

### 9. 확인

```bash
curl https://i-damoa.com/actuator/health
curl https://api.i-damoa.com/actuator/health

# 브라우저
# https://api.i-damoa.com/swagger-ui/index.html
```

---

## GitHub Actions 자동배포

### .github/workflows/deploy-prod.yml

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to EC2
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.PROD_EC2_HOST }}
          username: ubuntu
          key: ${{ secrets.PROD_EC2_SSH_KEY }}
          script: |
            cd ~/damoa/app
            docker compose pull
            docker compose up -d
            docker image prune -f
```

### GitHub Secrets 설정

| Secret | 값 |
|--------|-----|
| `PROD_EC2_HOST` | EC2 Elastic IP |
| `PROD_EC2_SSH_KEY` | EC2 SSH Private Key |

---

## 운영 명령어

### App

```bash
cd ~/damoa/app

# 시작/중지/재시작
docker compose up -d
docker compose down
docker compose restart

# 업데이트 (수동)
docker compose pull
docker compose up -d

# 로그
docker compose logs -f
```

### Infra

```bash
cd ~/damoa/infra

# 시작/중지
docker compose up -d
docker compose down

# DB 접속
docker exec -it damoa-postgres-prod psql -U hip_damoa_user -d hip_damoa

# Redis 접속
docker exec -it damoa-redis-prod redis-cli
```

### Nginx

```bash
cd ~/damoa/nginx

# 설정 리로드
docker compose exec nginx nginx -s reload

# 설정 테스트
docker compose exec nginx nginx -t

# 로그
docker compose logs -f nginx

# SSL 수동 갱신 (보통 자동)
docker compose run --rm certbot renew
docker compose exec nginx nginx -s reload
```

---

## 트러블슈팅

### 502 Bad Gateway

```bash
# App 컨테이너 상태 확인
docker ps | grep damoa-app

# App 로그 확인
cd ~/damoa/app && docker compose logs -f

# 네트워크 확인
docker network ls | grep nginx
docker network inspect nginx-network
```

### SSL 인증서 문제

```bash
# 인증서 확인
ls -la ~/damoa/nginx/certbot/letsencrypt/live/i-damoa.com/

# 인증서 갱신
cd ~/damoa/nginx
docker compose run --rm certbot renew
docker compose exec nginx nginx -s reload
```

### 컨테이너 네트워크 연결 안 됨

```bash
# nginx-network에 app이 연결되어 있는지 확인
docker inspect damoa-app-prod --format '{{json .NetworkSettings.Networks}}' | jq

# 수동으로 네트워크 연결
docker network connect nginx-network damoa-app-prod
```

---

## 체크리스트

### 초기 설정
- [ ] EC2 인스턴스 생성
- [ ] Elastic IP 할당
- [ ] 보안 그룹 설정 (80, 443, 22)
- [ ] Docker 설치
- [ ] DNS A 레코드 설정
- [ ] 프로젝트 클론
- [ ] 디렉토리 구조 생성
- [ ] .env 파일 설정
- [ ] Infra 시작
- [ ] SSL 인증서 발급
- [ ] Nginx 시작
- [ ] App 시작
- [ ] HTTPS 접속 테스트

### GitHub Actions 설정
- [ ] PROD_EC2_HOST secret 추가
- [ ] PROD_EC2_SSH_KEY secret 추가
- [ ] deploy-prod.yml 워크플로우 추가
- [ ] 자동배포 테스트
