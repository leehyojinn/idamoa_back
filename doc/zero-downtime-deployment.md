# 무중단 배포 (Zero-Downtime Deployment) 가이드

> **상태**: 미구현 (TODO)
> **작성일**: 2026-01-02

## 현재 문제점

- Docker 이미지 교체 시 다운타임 발생
- 에러 발생 시 서버가 망가진 상태로 배포됨
- 롤백 메커니즘 없음

---

## 해결 방법 옵션

### 1. Blue-Green 배포

두 개의 동일한 환경(Blue/Green)을 유지하고, 트래픽을 전환하는 방식.

```yaml
# docker-compose.blue-green.yml
version: '3.8'

services:
  app-blue:
    image: damoa-app:current
    container_name: damoa-blue
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  app-green:
    image: damoa-app:new
    container_name: damoa-green
    ports:
      - "8081:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - app-blue
      - app-green
```

---

### 2. Docker Swarm Rolling Update

```bash
# 스웜 초기화 (최초 1회)
docker swarm init

# 서비스 배포
docker stack deploy -c docker-compose.yml damoa

# 롤링 업데이트 (자동으로 헬스체크 후 전환)
docker service update --image damoa-app:new damoa_app
```

**docker-compose.yml (Swarm 모드):**
```yaml
version: '3.8'

services:
  app:
    image: damoa-app:latest
    deploy:
      replicas: 2
      update_config:
        parallelism: 1
        delay: 30s
        failure_action: rollback
        order: start-first
      rollback_config:
        parallelism: 1
        delay: 10s
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 60s
    ports:
      - "8080:8080"
```

---

### 3. 쉘 스크립트 방식 (단일 서버용, 추천)

**deploy.sh:**
```bash
#!/bin/bash
set -e

APP_NAME="damoa-app"
NEW_IMAGE="damoa-app:new"
OLD_CONTAINER="${APP_NAME}-old"
NEW_CONTAINER="${APP_NAME}-new"
HEALTH_URL="http://localhost:8081/api/health"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "=== 무중단 배포 시작 ==="

# 1. 새 컨테이너를 다른 포트(8081)로 시작
echo "1. 새 컨테이너 시작 (포트 8081)..."
docker run -d \
  --name $NEW_CONTAINER \
  -p 8081:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file .env \
  $NEW_IMAGE

# 2. 헬스체크 (새 컨테이너가 정상 작동하는지 확인)
echo "2. 헬스체크 중..."
for i in $(seq 1 $MAX_RETRIES); do
  if curl -sf $HEALTH_URL > /dev/null 2>&1; then
    echo "   헬스체크 성공! (${i}/${MAX_RETRIES})"
    break
  fi

  if [ $i -eq $MAX_RETRIES ]; then
    echo "   헬스체크 실패! 롤백합니다."
    docker stop $NEW_CONTAINER
    docker rm $NEW_CONTAINER
    exit 1
  fi

  echo "   대기 중... (${i}/${MAX_RETRIES})"
  sleep $RETRY_INTERVAL
done

# 3. 기존 컨테이너 중지 및 제거
echo "3. 기존 컨테이너 교체..."
docker stop $APP_NAME 2>/dev/null || true
docker rm $APP_NAME 2>/dev/null || true

# 4. 새 컨테이너를 메인 포트(8080)로 재시작
docker stop $NEW_CONTAINER
docker rm $NEW_CONTAINER

docker run -d \
  --name $APP_NAME \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file .env \
  --restart unless-stopped \
  $NEW_IMAGE

# 5. 최종 헬스체크
echo "4. 최종 헬스체크..."
sleep 5
if curl -sf http://localhost:8080/api/health > /dev/null 2>&1; then
  echo "=== 배포 성공! ==="
else
  echo "=== 최종 헬스체크 실패 ==="
  exit 1
fi
```

---

### 4. Nginx + Blue-Green (가장 안정적)

**nginx.conf:**
```nginx
upstream backend {
    server app-blue:8080 weight=1;
    server app-green:8080 weight=1 backup;
}

server {
    listen 80;

    location / {
        proxy_pass http://backend;
        proxy_connect_timeout 5s;
        proxy_read_timeout 60s;
        proxy_next_upstream error timeout http_500 http_502 http_503;
    }

    location /health {
        access_log off;
        return 200 "healthy\n";
    }
}
```

**deploy-with-nginx.sh:**
```bash
#!/bin/bash

CURRENT=$(cat /tmp/active_server || echo "blue")

if [ "$CURRENT" = "blue" ]; then
  NEW="green"
  NEW_PORT=8081
else
  NEW="blue"
  NEW_PORT=8080
fi

echo "현재: $CURRENT -> 새 버전: $NEW"

# 1. 새 컨테이너 시작
docker-compose up -d app-$NEW

# 2. 헬스체크
for i in {1..30}; do
  if curl -sf http://localhost:$NEW_PORT/api/health; then
    echo "헬스체크 성공!"
    break
  fi
  sleep 2
done

# 3. Nginx 설정 변경 (트래픽 전환)
sed -i "s/app-$CURRENT/app-$NEW/g" /etc/nginx/conf.d/upstream.conf
nginx -s reload

# 4. 기존 컨테이너 중지
docker-compose stop app-$CURRENT

# 5. 현재 서버 기록
echo $NEW > /tmp/active_server

echo "배포 완료: $NEW 서버로 전환됨"
```

---

## 환경별 추천

| 환경 | 추천 방법 |
|------|----------|
| 단일 서버 + Docker | 3번 쉘 스크립트 |
| 단일 서버 + Nginx | 4번 Nginx + Blue-Green |
| 여러 서버 | Docker Swarm or Kubernetes |
| AWS/GCP | ECS, Cloud Run, GKE |

---

## 핵심 요구사항

1. **헬스체크 엔드포인트**: `/api/health` 구현 필요
2. **새 컨테이너 먼저 시작**: 기존 것 중지 전에 새 것이 정상인지 확인
3. **실패 시 롤백**: 헬스체크 실패하면 새 컨테이너 제거, 기존 유지
4. **다운타임 최소화**: 트래픽 전환은 순간적으로

---

## TODO

- [ ] 현재 환경에 맞는 방식 선택
- [ ] 헬스체크 엔드포인트 구현 확인
- [ ] 배포 스크립트 작성
- [ ] CI/CD 파이프라인 연동
- [ ] 테스트 환경에서 검증
