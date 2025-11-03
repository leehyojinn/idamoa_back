# HIP Damoa Project (Hospital Interior Platform)

## 1. 프로젝트 개요

병원 인테리어 플랫폼 다모아(HIP Damoa)는 병원과 인테리어 업체를 연결하는 중개 플랫폼입니다.
Spring Boot, JPA, Docker를 사용하여 구성된 웹 애플리케이션입니다.

## 2. 기술 스택

- Java 17
- Spring Boot 3.2.4
- Gradle
- PostgreSQL 16
- Redis 7
- Docker
- Springdoc (Swagger UI)

## 3. 데이터베이스 정보

### 환경별 DB 이름
- **Local**: `hip_damoa_local` (로컬 개발)
- **Dev**: `hip_damoa_dev` (개발 서버)
- **Prod**: `hip_damoa_prod` (운영 서버)

> HIP = Hospital Interior Platform (병원 인테리어 플랫폼)

## 4. 실행 방법

### 4.1. Docker를 이용한 실행 (권장)

프로젝트 루트 디렉토리에서 아래 명령어를 실행합니다.

```bash
docker-compose up --build
```

애플리케이션은 `http://localhost:8080` 에서 실행됩니다.

### 4.2. 로컬 환경에서 직접 실행

로컬에 PostgreSQL, Redis가 설치 및 실행되어 있어야 합니다.

1.  **의존성 설치**
    ```bash
    ./gradlew clean build
    ```

2.  **애플리케이션 실행**
    ```bash
    java -jar build/libs/damoa-0.0.1-SNAPSHOT.jar
    ```

### 4.3. 환경 설정

1. **환경 변수 파일 생성**
   ```bash
   cp .env.example .env
   ```

2. **`.env` 파일 수정**
   - DB 이름을 환경에 맞게 설정
   - 필요한 API 키와 시크릿 설정

## 5. API 명세

애플리케이션 실행 후, 아래 주소에서 API 문서를 확인할 수 있습니다.

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### 주요 API 엔드포인트

- `GET /api/health`: 서버 상태 확인
- `POST /api/health/db`: DB 저장 기능 확인
- `GET /api/health/db`: DB 조회 기능 확인
