3가지 방식 비교 (정확한 흐름)

1️⃣ 프론트 직접 업로드 (현재 Amplify Storage)

[흐름]
프론트 ─────────────────> S3
│                        │
│                        │
└─────> 백엔드 (경로만 전송)
└─> DB 저장

[파일 경로]
파일 자체: 프론트 → S3 (직접)
메타데이터: 프론트 → 백엔드 → DB

[문제점]
- 프론트가 S3 권한 필요 (보안 취약)
- 파일 검증 불가

2️⃣ 백엔드 업로드

[흐름]
프론트 ─────────────────> 백엔드
│
│ (파일 메모리에 적재)
│
└────────> S3
│
└─> DB 저장

[파일 경로]
파일 자체: 프론트 → 백엔드 → S3 (이중 전송!)
메타데이터: 백엔드에서 추출 → DB

[문제점]
- 백엔드 메모리/CPU 사용 (파일이 백엔드 거침)
- 느린 속도 (2번 전송)
- 대역폭 비용

3️⃣ Presigned URL ⭐

[흐름 - 3단계]

1단계: 메타데이터만 전송
프론트 ──(파일명, 크기, 타입)──> 백엔드
│ 검증
│ DB 저장 (pending)
│ Presigned URL 생성
프론트 <─(Presigned URL)────────┘


2단계: S3 직접 업로드
프론트 ─────(Presigned URL 사용)───────> S3
(파일 저장)


3단계: 완료 알림
프론트 ──("업로드 완료")──> 백엔드
└─> DB 업데이트 (completed)

[파일 경로]
파일 자체: 프론트 → S3 (직접! 백엔드 안 거침!)
메타데이터: 프론트 → 백엔드 → DB

[장점]
✅ 빠른 속도 (직접 업로드)
✅ 백엔드 부하 없음 (파일 안 거침)
✅ 보안 (백엔드가 권한 관리)

  ---
Presigned URL 상세 설명

🔑 Presigned URL이란?

임시로 사용 가능한 S3 업로드 링크입니다.

일반 S3 URL:
https://damoa-storage.s3.ap-northeast-2.amazonaws.com/uploads/image.jpg
→ 접근 불가 (권한 없음)

Presigned URL:
https://damoa-storage.s3.ap-northeast-2.amazonaws.com/uploads/image.jpg?
X-Amz-Algorithm=AWS4-HMAC-SHA256
&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE/20231030/ap-northeast-2/s3/aws4_request
&X-Amz-Date=20231030T120000Z
&X-Amz-Expires=900
&X-Amz-SignedHeaders=host
&X-Amz-Signature=abc123...
→ 15분간 이 URL로 업로드 가능!

핵심: 백엔드가 자신의 권한을 15분간 이 URL에 부여한 것!

  ---
단계별 상세 동작 (실제 데이터 흐름)

1단계: Presigned URL 요청

프론트엔드 → 백엔드

POST /api/files/presigned-url

{
"fileName": "hospital-interior.jpg",
"fileSize": 2048576,  // 2MB
"fileType": "image/jpeg"
}

중요: 파일 자체는 안 보냄! 정보만 보냄!

백엔드 처리

// 1. 검증
if (fileSize > 10MB) throw new Error("Too large");
if (fileType not in allowed) throw new Error("Invalid type");

// 2. DB에 레코드 생성
File file = new File();
file.setFileName("hospital-interior.jpg");
file.setFileSize(2048576);
file.setStatus("PENDING"); // 아직 업로드 안 됨
file.setS3Key("uploads/user-123/abc-def-456.jpg"); // 미리 경로 결정
db.save(file);

// 3. Presigned URL 생성 (AWS SDK 사용)
String presignedUrl = s3Client.generatePresignedUrl(
bucket: "damoa-storage",
key: "uploads/user-123/abc-def-456.jpg",
expires: 15분
);

// 4. 응답
return {
"uploadUrl": "https://damoa-storage.s3...?X-Amz-Signature=abc123",
"fileId": 789,
"fileKey": "uploads/user-123/abc-def-456.jpg"
}

백엔드 → 프론트엔드

{
"uploadUrl": "https://damoa-storage.s3.ap-northeast-2.amazonaws.com/uploads/user-123/abc-def-456.jpg?X-Amz-Signa
ture=abc123...",
"fileId": 789,
"fileKey": "uploads/user-123/abc-def-456.jpg"
}

  ---
2단계: S3 직접 업로드

프론트엔드 → S3 (백엔드 거치지 않음!)

// XMLHttpRequest 또는 fetch 사용
const xhr = new XMLHttpRequest();
xhr.open('PUT', uploadUrl); // Presigned URL
xhr.setRequestHeader('Content-Type', 'image/jpeg');
xhr.send(file); // 실제 파일 (2MB)

요청:
PUT
https://damoa-storage.s3.ap-northeast-2.amazonaws.com/uploads/user-123/abc-def-456.jpg?X-Amz-Signature=abc123...
Content-Type: image/jpeg
Content-Length: 2048576

[파일 바이너리 데이터 2MB]

S3 처리:
- Signature 검증 ✓
- 15분 이내 ✓
- Content-Type 확인 ✓
- 파일 저장 완료!

응답:
HTTP/1.1 200 OK
ETag: "abc123def456..."

  ---
3단계: 업로드 완료 알림

프론트엔드 → 백엔드

POST /api/files/789/complete

{
"fileId": 789
}

백엔드 처리

// DB에서 파일 찾기
File file = db.findById(789);

// 상태 업데이트
file.setStatus("COMPLETED");
file.setUploadedAt(now());
db.save(file);

// S3에서 실제 파일 크기 확인 (선택적)
Long actualSize = s3Client.getObjectMetadata(file.getS3Key()).getContentLength();
file.setActualSize(actualSize);

  ---
각 방식의 백엔드 메모리 사용 비교

백엔드 업로드 방식

@PostMapping("/upload")
public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
// ⚠️ 파일이 백엔드 메모리에 적재됨!
byte[] fileBytes = file.getBytes(); // 2MB 메모리 사용!

      // S3 업로드
      s3Client.putObject(bucket, key, new ByteArrayInputStream(fileBytes));

      // 메모리 사용: 2MB
      // CPU 사용: 파일 처리
}

Presigned URL 방식

@PostMapping("/presigned-url")
public ResponseEntity<?> getPresignedUrl(@RequestBody FileRequest request) {
// ✅ 파일 안 받음! 메타데이터만!
String fileName = request.getFileName(); // "hospital-interior.jpg" (50 bytes)
Long fileSize = request.getFileSize();   // 2048576 (8 bytes)

      // Presigned URL 생성
      String url = s3Presigner.presignUrl(...);

      // 메모리 사용: ~100 bytes (메타데이터만)
      // CPU 사용: 최소
}

차이:
- 백엔드 업로드: 2MB 메모리 사용
- Presigned URL: 100 bytes 메모리 사용
- 차이: 20,000배!

  ---
실제 동작 시각화

[백엔드 업로드 - 파일이 백엔드 거침]
프론트 ─(파일 2MB)─> 백엔드 ─(파일 2MB)─> S3
[메모리 2MB]
[CPU 사용]
시간: 5초

[Presigned URL - 파일이 백엔드 안 거침]
프론트 ─(메타데이터 100bytes)─> 백엔드
[메모리 100bytes]
[Presigned URL 생성]
프론트 <─(URL)──────────────────┘
│
└─(파일 2MB)────────────────> S3
시간: 2초

프론트 ─(완료 알림)─> 백엔드
[DB 업데이트]

  ---
프론트엔드가 받는 정보

Presigned URL Response

{
uploadUrl: "https://damoa-storage.s3.ap-northeast-2.amazonaws.com/uploads/user-123/abc-def-456.jpg?X-Amz-Algorit
hm=AWS4-HMAC-SHA256&X-Amz-Credential=...",
fileId: 789,
fileKey: "uploads/user-123/abc-def-456.jpg",
expiresAt: "2025-10-30T15:30:00Z"
}

프론트엔드가 하는 일:
1. uploadUrl로 파일을 S3에 직접 업로드
2. 업로드 완료되면 fileId로 백엔드에 알림

  ---
정리

| 항목          | 백엔드 업로드    | Presigned URL    |
  |-------------|------------|------------------|
| 파일이 백엔드 거침? | ✅ 예 (2MB)  | ❌ 아니오 (100bytes) |
| 백엔드 메모리     | 2MB        | 100bytes         |
| 백엔드 CPU     | 높음         | 낮음               |
| 업로드 속도      | 느림 (2번 전송) | 빠름 (직접 전송)       |
| 파일 정보 확인    | 백엔드에서 추출   | 프론트가 보내줌         |

Presigned URL의 핵심:
- 프론트가 파일 정보만 백엔드에 보냄 (파일 자체는 안 보냄!)
- 백엔드가 임시 업로드 URL 생성해서 프론트에 줌
- 프론트가 그 URL로 S3에 직접 업로드
- 파일이 백엔드를 거치지 않음!