#!/bin/bash

# ===========================================
# Blue-Green 무중단 배포 스크립트 (Production)
# ===========================================
# Usage:
#   ./deploy.sh              # 일반 배포
#   ./deploy.sh rollback     # 롤백
#   ./deploy.sh status       # 상태 확인
# ===========================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 서버 구조: ~/damoa/app/ 에서 실행, nginx는 ~/damoa/nginx/
UPSTREAM_CONF="$SCRIPT_DIR/../nginx/config/upstream.inc"
NGINX_CONTAINER="nginx-core"
HEALTH_CHECK_URL_BLUE="http://localhost:8080/api/health"
HEALTH_CHECK_URL_GREEN="http://localhost:8080/api/health"
MAX_HEALTH_CHECK_ATTEMPTS=30
HEALTH_CHECK_INTERVAL=2

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 현재 Active 환경 확인
get_active_env() {
    if grep -q "damoa-app-blue" "$UPSTREAM_CONF"; then
        echo "blue"
    elif grep -q "damoa-app-green" "$UPSTREAM_CONF"; then
        echo "green"
    else
        echo "unknown"
    fi
}

# Health Check 함수 (컨테이너 내부에서 확인)
health_check() {
    local container=$1
    local attempts=0

    log_info "Health Check 시작: $container"

    while [ $attempts -lt $MAX_HEALTH_CHECK_ATTEMPTS ]; do
        attempts=$((attempts + 1))

        if docker exec "$container" wget --no-verbose --tries=1 --spider http://localhost:8080/api/health 2>/dev/null; then
            log_success "Health Check 성공! (시도: $attempts/$MAX_HEALTH_CHECK_ATTEMPTS)"
            return 0
        fi

        log_info "Health Check 대기 중... (시도: $attempts/$MAX_HEALTH_CHECK_ATTEMPTS)"
        sleep $HEALTH_CHECK_INTERVAL
    done

    log_error "Health Check 실패! 최대 시도 횟수 초과"
    return 1
}

# Upstream 전환
switch_upstream() {
    local target=$1
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    log_info "Upstream 전환: $target"

    if [ "$target" == "blue" ]; then
        cat > "$UPSTREAM_CONF" << EOF
# ===========================================
# Blue-Green Upstream Configuration
# ===========================================
# 이 파일은 deploy.sh에 의해 자동으로 수정됩니다.
# 수동으로 수정하지 마세요.
# ===========================================

# 현재 Active: blue
# 마지막 배포: $timestamp

upstream idamoa_api {
    zone idamoa_api 64k;
    server damoa-app-blue:8080;
}

upstream idamoa_frontend {
    zone idamoa_frontend 64k;
    server damoa-frontend-prod:3000;
}
EOF
    else
        cat > "$UPSTREAM_CONF" << EOF
# ===========================================
# Blue-Green Upstream Configuration
# ===========================================
# 이 파일은 deploy.sh에 의해 자동으로 수정됩니다.
# 수동으로 수정하지 마세요.
# ===========================================

# 현재 Active: green
# 마지막 배포: $timestamp

upstream idamoa_api {
    zone idamoa_api 64k;
    server damoa-app-green:8080;
}

upstream idamoa_frontend {
    zone idamoa_frontend 64k;
    server damoa-frontend-prod:3000;
}
EOF
    fi

    # Nginx reload
    docker exec $NGINX_CONTAINER nginx -s reload
    log_success "Nginx 설정 리로드 완료"
}

# 배포 함수
deploy() {
    local current_env=$(get_active_env)
    local target_env
    local target_compose
    local target_container
    local old_compose

    log_info "=========================================="
    log_info "Blue-Green 무중단 배포 시작 (Production)"
    log_info "=========================================="

    # 배포 대상 결정
    if [ "$current_env" == "blue" ]; then
        target_env="green"
        target_compose="docker-compose.green.yml"
        target_container="damoa-app-green"
        old_compose="docker-compose.blue.yml"
    elif [ "$current_env" == "green" ]; then
        target_env="blue"
        target_compose="docker-compose.blue.yml"
        target_container="damoa-app-blue"
        old_compose="docker-compose.green.yml"
    else
        log_warning "현재 Active 환경을 확인할 수 없습니다. Blue로 배포합니다."
        target_env="blue"
        target_compose="docker-compose.blue.yml"
        target_container="damoa-app-blue"
        old_compose=""
    fi

    log_info "현재 Active: $current_env"
    log_info "배포 대상: $target_env"

    # Phase 1: 새 버전 컨테이너 시작
    log_info "=========================================="
    log_info "Phase 1: 새 버전 컨테이너 시작"
    log_info "=========================================="

    cd "$SCRIPT_DIR"
    docker compose -f "$target_compose" pull
    docker compose -f "$target_compose" up -d

    # Phase 2: Health Check
    log_info "=========================================="
    log_info "Phase 2: Health Check"
    log_info "=========================================="

    if ! health_check "$target_container"; then
        log_error "새 버전 Health Check 실패!"
        log_warning "새 버전 컨테이너를 정리합니다..."
        docker compose -f "$target_compose" down
        exit 1
    fi

    # Phase 3: 트래픽 전환
    log_info "=========================================="
    log_info "Phase 3: 트래픽 전환"
    log_info "=========================================="

    switch_upstream "$target_env"

    # Phase 4: 이전 버전 정리
    log_info "=========================================="
    log_info "Phase 4: 이전 버전 정리"
    log_info "=========================================="

    if [ -n "$old_compose" ]; then
        log_info "이전 버전 컨테이너 정리 중..."
        docker compose -f "$old_compose" down || true
    fi

    # 정리
    docker image prune -f > /dev/null 2>&1 || true

    log_success "=========================================="
    log_success "배포 완료!"
    log_success "Active 환경: $target_env"
    log_success "=========================================="
}

# 롤백 함수
rollback() {
    local current_env=$(get_active_env)
    local target_env
    local target_compose
    local target_container

    log_warning "=========================================="
    log_warning "롤백 시작"
    log_warning "=========================================="

    # 롤백 대상 결정
    if [ "$current_env" == "blue" ]; then
        target_env="green"
        target_compose="docker-compose.green.yml"
        target_container="damoa-app-green"
    else
        target_env="blue"
        target_compose="docker-compose.blue.yml"
        target_container="damoa-app-blue"
    fi

    log_info "현재 Active: $current_env"
    log_info "롤백 대상: $target_env"

    # 이전 버전 시작
    cd "$SCRIPT_DIR"
    docker compose -f "$target_compose" up -d

    # Health Check
    if ! health_check "$target_container"; then
        log_error "롤백 실패! 이전 버전도 정상 작동하지 않습니다."
        exit 1
    fi

    # 트래픽 전환
    switch_upstream "$target_env"

    log_success "=========================================="
    log_success "롤백 완료!"
    log_success "Active 환경: $target_env"
    log_success "=========================================="
}

# 상태 확인 함수
status() {
    local current_env=$(get_active_env)

    echo ""
    log_info "=========================================="
    log_info "Blue-Green 배포 상태 (Production)"
    log_info "=========================================="
    echo ""
    log_info "현재 Active 환경: $current_env"
    echo ""

    log_info "Blue 컨테이너 상태:"
    docker ps --filter "name=damoa-app-blue" --format "  {{.Names}}: {{.Status}}" || echo "  실행 중인 컨테이너 없음"
    echo ""

    log_info "Green 컨테이너 상태:"
    docker ps --filter "name=damoa-app-green" --format "  {{.Names}}: {{.Status}}" || echo "  실행 중인 컨테이너 없음"
    echo ""

    log_info "=========================================="
}

# 메인
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    rollback)
        rollback
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {deploy|rollback|status}"
        exit 1
        ;;
esac
