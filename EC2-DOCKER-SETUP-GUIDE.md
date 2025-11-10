# AWS EC2 Docker 배포 가이드 (Damoa Develop 환경)

## 📋 목차
1. [EC2 인스턴스 생성](#1-ec2-인스턴스-생성)
2. [보안 그룹 설정](#2-보안-그룹-설정)
3. [SSH 접속](#3-ssh-접속)
4. [Ubuntu 기본 설정](#4-ubuntu-기본-설정)
5. [Docker & Docker Compose 설치](#5-docker--docker-compose-설치)
6. [프로젝트 Dockerfile 작성](#6-프로젝트-dockerfile-작성)
7. [docker-compose.yml 작성](#7-docker-composeyml-작성)
8. [환경 변수 설정](#8-환경-변수-설정)
9. [배포 준비 및 확인](#9-배포-준비-및-확인)

---

## 1. EC2 인스턴스 생성

### 1.1 AWS Console에서 EC2 생성

1. **AWS Console** → **EC2** → **인스턴스 시작**

2. **이름 및 태그**
   ```
   이름: damoa-develop
   환경: develop
   프로젝트: damoa
   ```

3. **AMI 선택**
   - **Ubuntu Server 22.04 LTS (HVM), SSD Volume Type**
   - Architecture: **64비트 (x86)**

4. **인스턴스 유형**
   - **t3.small** (2 vCPU, 2GB RAM)
   - 나중에 필요시 t3.medium (4GB RAM)으로 업그레이드

5. **키 페어 (로그인)**
   - **새 키 페어 생성**
   - 키 페어 이름: `damoa-develop-key`
   - 키 페어 유형: `RSA`
   - 프라이빗 키 파일 형식: `.pem`
   - **다운로드** → 안전한 곳에 보관 (재다운로드 불가)

6. **네트워크 설정**
   - VPC: 기본 VPC
   - 퍼블릭 IP 자동 할당: **활성화**

7. **스토리지 구성**
   - 크기: **30 GB** (최소, 권장 50GB)
   - 볼륨 유형: **gp3**
   - Docker 이미지 + 데이터베이스 저장 공간 고려

8. **인스턴스 시작**

---

## 2. 보안 그룹 설정

### 2.1 인바운드 규칙 설정

EC2 인스턴스 → **보안** 탭 → **보안 그룹** → **인바운드 규칙 편집**

| 유형 | 프로토콜 | 포트 범위 | 소스 | 설명 |
|------|----------|----------|------|------|
| SSH | TCP | 22 | 내 IP | SSH 접속 (본인 IP만) |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP (나중에 Nginx) |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS (나중에 SSL) |
| 사용자 지정 TCP | TCP | 8080 | 0.0.0.0/0 | Spring Boot (개발용) |

⚠️ **보안 주의**:
- PostgreSQL(5432), Redis(6379)는 **외부 노출 금지** (Docker 내부 네트워크만)
- SSH는 본인 IP만 허용
- 나중에 Nginx 리버스 프록시 설정 후 8080 포트는 내부로 변경

---

## 3. SSH 접속

### 3.1 키 페어 권한 설정

**Windows (PowerShell)**:
```powershell
# 키 파일을 안전한 위치로 이동
mkdir C:\Users\YourName\.ssh
move C:\Users\YourName\Downloads\damoa-develop-key.pem C:\Users\YourName\.ssh\

# 권한 설정
icacls C:\Users\YourName\.ssh\damoa-develop-key.pem /reset
icacls C:\Users\YourName\.ssh\damoa-develop-key.pem /grant:r "$($env:USERNAME):(R)"
icacls C:\Users\YourName\.ssh\damoa-develop-key.pem /inheritance:r
```

**Mac/Linux**:
```bash
mv ~/Downloads/damoa-develop-key.pem ~/.ssh/
chmod 400 ~/.ssh/damoa-develop-key.pem
```

### 3.2 SSH 접속

```bash
# EC2 퍼블릭 IP 확인 (AWS Console)
# 예: 52.78.123.456

ssh -i ~/.ssh/damoa-develop-key.pem ubuntu@52.78.123.456

# 첫 접속 시 yes 입력
```

---

## 4. Ubuntu 기본 설정

### 4.1 시스템 업데이트

```bash
# 패키지 업데이트
sudo apt update && sudo apt upgrade -y

# 재부팅 필요 시
sudo reboot
# (재부팅 후 다시 SSH 접속)
```

### 4.2 기본 설정

```bash
# 타임존 설정 (한국 시간)
sudo timedatectl set-timezone Asia/Seoul

# 호스트네임 변경
sudo hostnamectl set-hostname damoa-develop

# 스왑 메모리 설정 (t3.small은 2GB RAM)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 스왑 확인
free -h
```

### 4.3 필수 유틸리티 설치

```bash
sudo apt install -y \
  curl \
  wget \
  git \
  vim \
  htop \
  unzip \
  net-tools \
  ca-certificates \
  gnupg \
  lsb-release
```

---

## 5. Docker & Docker Compose 설치

### 5.1 Docker 설치

```bash
# Docker 공식 GPG 키 추가
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Docker 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 패키지 업데이트
sudo apt update

# Docker 설치
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Docker 서비스 시작 및 자동 시작
sudo systemctl start docker
sudo systemctl enable docker

# 설치 확인
sudo docker --version
# 출력: Docker version 24.0.x, build xxxxx
```

### 5.2 Docker 사용자 권한 설정

```bash
# ubuntu 사용자를 docker 그룹에 추가
sudo usermod -aG docker ubuntu

# 변경사항 적용 (재로그인 필요)
# 방법 1: SSH 재접속
exit
ssh -i ~/.ssh/damoa-develop-key.pem ubuntu@52.78.123.456

# 방법 2: 현재 세션에서 적용
newgrp docker

# 확인 (sudo 없이 실행)
docker ps
```

### 5.3 Docker Compose 설치 확인

```bash
# Docker Compose 버전 확인 (플러그인 방식)
docker compose version
# 출력: Docker Compose version v2.x.x
```

---

## 6. 프로젝트 Dockerfile 작성

### 6.1 멀티 스테이지 Dockerfile

프로젝트 루트에 `Dockerfile` 생성:

```dockerfile
# Stage 1: Build
FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 먼저 다운로드
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 타임존 설정
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 비root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 환경 변수
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 6.2 .dockerignore 파일 생성

```bash
# .dockerignore
.git
.gradle
build
.idea
*.iml
.env
*.log
node_modules
.DS_Store
```

---

## 7. docker-compose.yml 작성

### 7.1 프로덕션용 docker-compose.yml

프로젝트 루트에 `docker-compose.prod.yml` 생성:

```yaml
version: '3.8'

services:
  # PostgreSQL 16
  postgres:
    image: postgres:16-alpine
    container_name: damoa-postgres-dev
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-damoa}
      POSTGRES_USER: ${DB_USERNAME:-damoa_user}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --locale=C"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d  # 초기화 스크립트
    ports:
      - "127.0.0.1:5432:5432"  # 로컬호스트에만 바인딩
    networks:
      - damoa-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-damoa_user} -d ${DB_NAME:-damoa}"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis 7
  redis:
    image: redis:7-alpine
    container_name: damoa-redis-dev
    restart: unless-stopped
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    ports:
      - "127.0.0.1:6379:6379"  # 로컬호스트에만 바인딩
    networks:
      - damoa-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: damoa-app-dev
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      # Spring Profile
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}

      # Database
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME:-damoa}
      DB_USERNAME: ${DB_USERNAME:-damoa_user}
      DB_PASSWORD: ${DB_PASSWORD}

      # Redis
      REDIS_HOST: redis
      REDIS_PORT: 6379

      # JWT
      JWT_SECRET: ${JWT_SECRET}
      JWT_ACCESS_TOKEN_EXPIRATION: ${JWT_ACCESS_TOKEN_EXPIRATION:-86400000}
      JWT_REFRESH_TOKEN_EXPIRATION: ${JWT_REFRESH_TOKEN_EXPIRATION:-604800000}

      # OAuth2
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:-}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:-}
      KAKAO_CLIENT_ID: ${KAKAO_CLIENT_ID:-}
      KAKAO_CLIENT_SECRET: ${KAKAO_CLIENT_SECRET:-}
      NAVER_CLIENT_ID: ${NAVER_CLIENT_ID:-}
      NAVER_CLIENT_SECRET: ${NAVER_CLIENT_SECRET:-}

      # AWS S3
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID:-}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY:-}
      AWS_REGION: ${AWS_REGION:-ap-northeast-2}
      S3_BUCKET_NAME: ${S3_BUCKET_NAME:-damoa-files-dev}

      # JVM Options
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"

    ports:
      - "8080:8080"
    volumes:
      - app-logs:/app/logs
    networks:
      - damoa-network
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres-data:
    driver: local
  redis-data:
    driver: local
  app-logs:
    driver: local

networks:
  damoa-network:
    driver: bridge
```

---

## 8. 환경 변수 설정

### 8.1 .env 파일 생성 (서버에서)

```bash
# 프로젝트 디렉토리 생성
mkdir -p /home/ubuntu/damoa
cd /home/ubuntu/damoa

# .env 파일 생성
vim .env
```

### 8.2 .env 파일 내용

```bash
# ================================
# Damoa Develop Environment
# ================================

# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Database Configuration
DB_NAME=damoa
DB_USERNAME=damoa_user
DB_PASSWORD=StrongPassword123!@#ChangeMe

# JWT Configuration (최소 256비트 base64 인코딩)
JWT_SECRET=your-super-secret-jwt-key-base64-encoded-min-256-bits-change-this-in-production
JWT_ACCESS_TOKEN_EXPIRATION=86400000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# OAuth2 (나중에 설정)
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=

# AWS S3 (나중에 설정)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=ap-northeast-2
S3_BUCKET_NAME=damoa-files-dev
```

### 8.3 .env 파일 권한 설정

```bash
# 읽기 전용 권한
chmod 600 .env

# 확인
ls -la .env
# 출력: -rw------- 1 ubuntu ubuntu ... .env
```

### 8.4 JWT Secret 생성

```bash
# 안전한 JWT Secret 생성 (base64)
openssl rand -base64 64

# 출력된 문자열을 .env의 JWT_SECRET에 복사
```

---

## 9. 배포 준비 및 확인

### 9.1 프로젝트 파일 업로드

**방법 1: Git Clone (권장)**
```bash
cd /home/ubuntu/damoa

# GitHub 저장소 클론
git clone https://github.com/your-username/damoa.git .

# develop 브랜치 체크아웃
git checkout develop
```

**방법 2: SCP로 파일 전송 (로컬에서)**
```bash
# Windows (PowerShell)
scp -i C:\Users\YourName\.ssh\damoa-develop-key.pem -r C:\workspace\damoa ubuntu@52.78.123.456:/home/ubuntu/

# Mac/Linux
scp -i ~/.ssh/damoa-develop-key.pem -r ~/workspace/damoa ubuntu@52.78.123.456:/home/ubuntu/
```

### 9.2 디렉토리 구조 확인

```bash
cd /home/ubuntu/damoa
tree -L 2 -a

# 예상 구조:
# damoa/
# ├── .env                          # 환경 변수
# ├── .dockerignore
# ├── Dockerfile
# ├── docker-compose.prod.yml
# ├── build.gradle
# ├── settings.gradle
# ├── src/
# └── ...
```

### 9.3 Docker 이미지 빌드

```bash
cd /home/ubuntu/damoa

# 이미지 빌드 (시간이 좀 걸립니다)
docker compose -f docker-compose.prod.yml build

# 빌드 확인
docker images | grep damoa
```

### 9.4 컨테이너 실행

```bash
# 백그라운드로 실행
docker compose -f docker-compose.prod.yml up -d

# 로그 확인
docker compose -f docker-compose.prod.yml logs -f

# 특정 서비스 로그만 보기
docker compose -f docker-compose.prod.yml logs -f app

# Ctrl+C로 로그 보기 종료 (컨테이너는 계속 실행)
```

### 9.5 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker compose -f docker-compose.prod.yml ps

# 예상 출력:
# NAME                  SERVICE    STATUS    PORTS
# damoa-app-dev         app        running   0.0.0.0:8080->8080/tcp
# damoa-postgres-dev    postgres   running   127.0.0.1:5432->5432/tcp
# damoa-redis-dev       redis      running   127.0.0.1:6379->6379/tcp

# 헬스체크 확인
docker inspect damoa-app-dev | grep -A 10 Health

# 애플리케이션 접속 테스트
curl http://localhost:8080/actuator/health

# 예상 출력:
# {"status":"UP"}
```

### 9.6 데이터베이스 확인

```bash
# PostgreSQL 컨테이너 접속
docker exec -it damoa-postgres-dev psql -U damoa_user -d damoa

# SQL 명령어 실행
\dt  # 테이블 목록
\q   # 종료

# Redis 컨테이너 접속
docker exec -it damoa-redis-dev redis-cli

# Redis 명령어
PING  # PONG 출력
INFO server
exit
```

---

## 📁 배포 스크립트 작성

### 10.1 배포 스크립트

`/home/ubuntu/damoa/deploy.sh` 생성:

```bash
#!/bin/bash

# Damoa Develop 배포 스크립트

set -e

echo "=========================================="
echo "Damoa Develop 배포 시작"
echo "=========================================="

# 환경 변수 확인
if [ ! -f .env ]; then
    echo "❌ .env 파일이 없습니다!"
    exit 1
fi

# Git Pull
echo "📥 최신 코드 가져오기..."
git pull origin develop

# 기존 컨테이너 중지 및 제거
echo "🛑 기존 컨테이너 중지..."
docker compose -f docker-compose.prod.yml down

# 이미지 빌드
echo "🏗️  이미지 빌드..."
docker compose -f docker-compose.prod.yml build --no-cache

# 컨테이너 실행
echo "🚀 컨테이너 실행..."
docker compose -f docker-compose.prod.yml up -d

# 로그 확인
echo "📋 로그 확인 (10초)..."
sleep 10
docker compose -f docker-compose.prod.yml logs --tail=50 app

# 헬스체크
echo "🏥 헬스체크..."
sleep 5
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 배포 성공!"
else
    echo "❌ 헬스체크 실패!"
    docker compose -f docker-compose.prod.yml logs app
    exit 1
fi

echo "=========================================="
echo "배포 완료: $(date)"
echo "=========================================="
```

### 10.2 스크립트 권한 설정

```bash
chmod +x /home/ubuntu/damoa/deploy.sh
```

### 10.3 스크립트 실행

```bash
cd /home/ubuntu/damoa
./deploy.sh
```

---

## 🔧 유용한 Docker 명령어

### 컨테이너 관리
```bash
# 실행
docker compose -f docker-compose.prod.yml up -d

# 중지
docker compose -f docker-compose.prod.yml stop

# 중지 및 제거
docker compose -f docker-compose.prod.yml down

# 중지, 제거 + 볼륨 삭제 (⚠️ 데이터 삭제!)
docker compose -f docker-compose.prod.yml down -v

# 재시작
docker compose -f docker-compose.prod.yml restart

# 특정 서비스만 재시작
docker compose -f docker-compose.prod.yml restart app
```

### 로그 확인
```bash
# 전체 로그
docker compose -f docker-compose.prod.yml logs

# 실시간 로그
docker compose -f docker-compose.prod.yml logs -f

# 특정 서비스 로그
docker compose -f docker-compose.prod.yml logs -f app

# 마지막 100줄
docker compose -f docker-compose.prod.yml logs --tail=100 app
```

### 컨테이너 접속
```bash
# 애플리케이션 컨테이너
docker exec -it damoa-app-dev sh

# PostgreSQL
docker exec -it damoa-postgres-dev psql -U damoa_user -d damoa

# Redis
docker exec -it damoa-redis-dev redis-cli
```

### 리소스 확인
```bash
# 컨테이너 상태
docker compose -f docker-compose.prod.yml ps

# 리소스 사용량
docker stats

# 디스크 사용량
docker system df

# 이미지 목록
docker images
```

### 정리
```bash
# 사용하지 않는 리소스 정리
docker system prune -a

# 볼륨 정리 (⚠️ 데이터 삭제 주의!)
docker volume prune
```

---

## ✅ 배포 체크리스트

### 배포 전
- [ ] EC2 인스턴스 생성 (t3.small)
- [ ] 보안 그룹 설정 (22, 80, 443, 8080)
- [ ] SSH 접속 확인
- [ ] Docker & Docker Compose 설치
- [ ] .env 파일 생성 및 설정
- [ ] JWT_SECRET 생성

### 배포 중
- [ ] 프로젝트 파일 업로드/클론
- [ ] docker-compose.prod.yml 확인
- [ ] Docker 이미지 빌드
- [ ] 컨테이너 실행

### 배포 후
- [ ] 컨테이너 상태 확인 (`docker ps`)
- [ ] 헬스체크 확인 (`curl http://localhost:8080/actuator/health`)
- [ ] 로그 확인 (에러 없는지)
- [ ] 데이터베이스 접속 확인
- [ ] Redis 접속 확인

---

## 🆘 문제 해결

### 컨테이너가 시작되지 않음
```bash
# 로그 확인
docker compose -f docker-compose.prod.yml logs

# 특정 서비스 재시작
docker compose -f docker-compose.prod.yml restart app

# 완전히 재시작
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

### 데이터베이스 연결 오류
```bash
# PostgreSQL 컨테이너 상태 확인
docker inspect damoa-postgres-dev | grep Health

# 로그 확인
docker logs damoa-postgres-dev

# 네트워크 확인
docker network inspect damoa_damoa-network
```

### 메모리 부족
```bash
# 컨테이너 리소스 확인
docker stats

# 스왑 메모리 확인
free -h

# 불필요한 이미지 삭제
docker image prune -a
```

### 디스크 공간 부족
```bash
# 디스크 사용량 확인
df -h

# Docker 디스크 사용량
docker system df

# 정리
docker system prune -a
```

---

## 🔐 보안 권장사항

1. **.env 파일 보안**
   - GitHub에 절대 커밋하지 마세요 (`.gitignore`에 추가)
   - 권한: `chmod 600 .env`

2. **PostgreSQL 비밀번호**
   - 16자 이상, 영문 대소문자, 숫자, 특수문자 조합
   - 정기적으로 변경

3. **포트 바인딩**
   - PostgreSQL, Redis는 `127.0.0.1`에만 바인딩
   - 외부 접근 차단

4. **방화벽**
   - UFW 활성화
   - 필요한 포트만 개방

5. **Docker 보안**
   - 정기적으로 이미지 업데이트
   - 취약점 스캔: `docker scan damoa-app-dev`

---

## 📝 다음 단계

- [x] EC2 인스턴스 생성
- [x] Docker 설치
- [x] docker-compose.yml 작성
- [x] 환경 변수 설정
- [x] 수동 배포 테스트
- [ ] **GitHub Actions CI/CD 설정** (다음 단계)
- [ ] Nginx 리버스 프록시 설정
- [ ] SSL 인증서 (Let's Encrypt)
- [ ] 로깅 및 모니터링 (CloudWatch)
- [ ] 백업 자동화

---

**작성일**: 2025-11-10
**환경**: AWS EC2 (t3.small), Ubuntu 22.04, Docker, PostgreSQL 16, Redis 7
**다음**: Git 자동 배포 (GitHub Actions)
