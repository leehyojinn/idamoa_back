# Blue-Green 무중단 배포 가이드

## 개요

Blue-Green 배포는 두 개의 동일한 프로덕션 환경(Blue, Green)을 운영하여 다운타임 없이 배포하는 전략입니다.

## 아키텍처

```
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   Nginx     │
                    │ (Load Balancer)
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
       ┌─────────────┐           ┌─────────────┐
       │  Blue (v1)  │           │ Green (v2)  │
       │   :8081     │           │   :8082     │
       │  [ACTIVE]   │           │  [STANDBY]  │
       └─────────────┘           └─────────────┘
              │                         │
              └────────────┬────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
       ┌─────────────┐           ┌─────────────┐
       │ PostgreSQL  │           │    Redis    │
       └─────────────┘           └─────────────┘
```

## 배포 Flow 상세

### Phase 1: 현재 상태 확인

```
┌──────────────────────────────────────────────────────────────┐
│ 1. 배포 시작                                                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────┐     ┌─────────────┐                       │
│   │   Nginx     │────▶│ Blue (v1.0) │  ◀── 현재 Active      │
│   └─────────────┘     │   :8081     │                       │
│                       └─────────────┘                       │
│                                                              │
│   1. upstream.conf 파일 읽어서 현재 Active 환경 확인          │
│   2. Active가 Blue면 → Green에 배포                          │
│   3. Active가 Green면 → Blue에 배포                          │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Phase 2: 새 버전 배포 (Standby 환경)

```
┌──────────────────────────────────────────────────────────────┐
│ 2. 새 버전 컨테이너 시작                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────┐     ┌─────────────┐                       │
│   │   Nginx     │────▶│ Blue (v1.0) │  ◀── 트래픽 유지      │
│   └─────────────┘     │   :8081     │                       │
│                       └─────────────┘                       │
│                                                              │
│                       ┌─────────────┐                       │
│                       │Green (v2.0) │  ◀── 새 버전 시작     │
│                       │   :8082     │      (트래픽 없음)     │
│                       └─────────────┘                       │
│                                                              │
│   Commands:                                                  │
│   $ docker pull <image>:latest                               │
│   $ docker-compose -f docker-compose.green.yml up -d         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Phase 3: Health Check

```
┌──────────────────────────────────────────────────────────────┐
│ 3. 새 버전 Health Check                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   Health Check 요청:                                          │
│   GET http://localhost:8082/api/health                       │
│                                                              │
│   ┌─────────────────────────────────────────────┐            │
│   │ 최대 30회 시도 (2초 간격 = 최대 60초 대기)   │            │
│   │                                             │            │
│   │  시도 1: 502 Bad Gateway (앱 시작 중)       │            │
│   │  시도 2: 502 Bad Gateway                    │            │
│   │  ...                                        │            │
│   │  시도 N: 200 OK {"status": "healthy"}       │            │
│   │                                             │            │
│   │  → Health Check 성공!                       │            │
│   └─────────────────────────────────────────────┘            │
│                                                              │
│   실패 시: 새 컨테이너 제거, 롤백                             │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Phase 4: 트래픽 전환

```
┌──────────────────────────────────────────────────────────────┐
│ 4. Nginx upstream 전환 (무중단)                               │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   Before:                                                    │
│   upstream.conf:                                             │
│   ┌─────────────────────────────────────┐                   │
│   │ upstream idamoa_api {               │                   │
│   │     server damoa-app-blue:8080;     │ ◀── Blue Active   │
│   │ }                                   │                   │
│   └─────────────────────────────────────┘                   │
│                                                              │
│   After:                                                     │
│   ┌─────────────────────────────────────┐                   │
│   │ upstream idamoa_api {               │                   │
│   │     server damoa-app-green:8080;    │ ◀── Green Active  │
│   │ }                                   │                   │
│   └─────────────────────────────────────┘                   │
│                                                              │
│   Commands:                                                  │
│   $ sed -i 's/blue/green/' upstream.conf                     │
│   $ docker exec nginx-core nginx -s reload                   │
│                                                              │
│   ※ nginx -s reload는 기존 연결을 끊지 않고 새 설정 적용      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Phase 5: 이전 버전 정리

```
┌──────────────────────────────────────────────────────────────┐
│ 5. 이전 버전 컨테이너 정리                                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────┐     ┌─────────────┐                       │
│   │   Nginx     │────▶│Green (v2.0) │  ◀── 새 Active       │
│   └─────────────┘     │   :8082     │                       │
│                       └─────────────┘                       │
│                                                              │
│                       ┌─────────────┐                       │
│                       │ Blue (v1.0) │  ◀── 제거됨           │
│                       │   :8081     │                       │
│                       └─────────────┘                       │
│                                                              │
│   Commands:                                                  │
│   $ docker-compose -f docker-compose.blue.yml down           │
│   $ docker image prune -f                                    │
│                                                              │
│   ※ 롤백이 필요한 경우를 위해 이미지는 유지할 수 있음          │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 롤백 Flow

```
┌──────────────────────────────────────────────────────────────┐
│ 롤백 시나리오                                                 │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   문제 발견 시:                                               │
│                                                              │
│   1. 이전 버전 컨테이너 재시작                                │
│      $ docker-compose -f docker-compose.blue.yml up -d       │
│                                                              │
│   2. Health Check 확인                                       │
│      $ curl http://localhost:8081/api/health                 │
│                                                              │
│   3. Nginx upstream 원복                                     │
│      $ sed -i 's/green/blue/' upstream.conf                  │
│      $ docker exec nginx-core nginx -s reload                │
│                                                              │
│   4. 문제가 있는 버전 정리                                    │
│      $ docker-compose -f docker-compose.green.yml down       │
│                                                              │
│   ※ 롤백 소요 시간: 약 30초 ~ 1분                            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 파일 구조

```
damoa/
├── docker-compose.yml           # 기존 개발용 (단일 앱)
├── docker-compose.blue.yml      # Blue 환경 (prod)
├── docker-compose.green.yml     # Green 환경 (prod)
├── deploy.sh                    # 배포 스크립트
│
└── nginx/
    ├── docker-compose.yml       # Nginx + Certbot
    └── config/
        ├── default.conf         # Nginx 메인 설정
        └── upstream.conf        # Blue/Green upstream 설정
```

## 배포 명령어

### 자동 배포 (권장)
```bash
# 전체 배포 프로세스 자동 실행
./deploy.sh

# 강제 롤백
./deploy.sh rollback
```

### 수동 배포
```bash
# 1. 현재 Active 환경 확인
cat nginx/config/upstream.conf

# 2. 새 버전 배포 (예: Green으로 배포)
docker pull your-registry/damoa:latest
docker-compose -f docker-compose.green.yml up -d

# 3. Health Check
curl http://localhost:8082/api/health

# 4. Nginx 전환
sed -i 's/blue/green/' nginx/config/upstream.conf
docker exec nginx-core nginx -s reload

# 5. 이전 버전 정리
docker-compose -f docker-compose.blue.yml down
```

## Health Check 엔드포인트

애플리케이션에 다음 엔드포인트가 구현되어 있어야 합니다:

```
GET /api/health

Response (200 OK):
{
  "status": "healthy",
  "timestamp": "2026-01-08T12:00:00Z",
  "version": "1.0.0"
}
```

## 주의사항

1. **데이터베이스 마이그레이션**: Flyway 등의 DB 마이그레이션은 Blue-Green 전환 전에 별도 실행
2. **세션 처리**: Redis에 세션 저장하므로 전환 시 세션 유지됨
3. **네트워크**: Blue와 Green 컨테이너는 같은 Docker 네트워크에 있어야 함
4. **포트 충돌**: Blue(8081), Green(8082)로 포트 분리

## GitHub Actions 연동

`.github/workflows/deploy.yml`에서 자동으로 `deploy.sh`를 실행합니다.

```yaml
- name: Deploy with Blue-Green
  run: |
    ssh user@server "cd /app && ./deploy.sh"
```
