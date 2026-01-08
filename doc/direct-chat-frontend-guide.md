# Direct Chat API 가이드 (프론트엔드용)

## 1. 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                         프론트엔드                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   [초기 로드]              [실시간]                [파일]        │
│       │                      │                      │          │
│   REST API               WebSocket              REST API        │
│       │                      │                      │          │
└───────┼──────────────────────┼──────────────────────┼──────────┘
        │                      │                      │
        ▼                      ▼                      ▼
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│  /api/direct  │      │     /ws       │      │  /api/files   │
│   -chats/*    │      │   (STOMP)     │      │               │
└───────────────┘      └───────────────┘      └───────────────┘
```

---

## 2. 인증

모든 API 요청에 JWT 토큰 필요

```
Authorization: Bearer {accessToken}
```

---

## 3. REST API

### 3.1 채팅방 관리

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/direct-chats/rooms` | 채팅방 생성/조회 |
| GET | `/api/direct-chats/rooms` | 내 채팅방 목록 |
| GET | `/api/direct-chats/rooms/{roomUuid}` | 채팅방 상세 |
| DELETE | `/api/direct-chats/rooms/{roomUuid}` | 채팅방 나가기 |

#### 채팅방 생성/조회
```
POST /api/direct-chats/rooms

Request:
{
  "targetUserUuid": "550e8400-e29b-41d4-a716-446655440000"
}

Response:
{
  "success": true,
  "data": {
    "uuid": "채팅방-uuid",
    "otherUser": {
      "uuid": "상대방-uuid",
      "email": "user@example.com",
      "nickname": "닉네임",
      "profileImageUrl": "https://...",
      "isOnline": true
    },
    "lastMessage": "마지막 메시지",
    "lastMessageAt": "2024-01-15T10:30:00",
    "unreadCount": 3
  }
}
```

#### 채팅방 목록
```
GET /api/direct-chats/rooms

Response:
{
  "success": true,
  "data": [
    {
      "uuid": "채팅방-uuid",
      "otherUser": { ... },
      "lastMessage": "마지막 메시지",
      "lastMessageAt": "2024-01-15T10:30:00",
      "unreadCount": 3
    }
  ]
}
```

### 3.2 메시지 관리

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/direct-chats/rooms/{roomUuid}/messages` | 메시지 목록 (페이징) |
| POST | `/api/direct-chats/rooms/{roomUuid}/messages` | 메시지 전송 (REST) |
| POST | `/api/direct-chats/rooms/{roomUuid}/read` | 읽음 처리 |
| GET | `/api/direct-chats/rooms/{roomUuid}/unread-count` | 미읽음 수 |
| GET | `/api/direct-chats/unread-count` | 전체 미읽음 수 |

#### 메시지 목록
```
GET /api/direct-chats/rooms/{roomUuid}/messages?page=0&size=50

Response:
{
  "success": true,
  "data": {
    "content": [
      {
        "uuid": "메시지-uuid",
        "content": "메시지 내용",
        "messageType": "TEXT",        // TEXT, IMAGE, FILE, SYSTEM
        "senderUuid": "발신자-uuid",
        "senderNickname": "닉네임",
        "isMine": false,
        "attachments": [
          {
            "uuid": "첨부파일-uuid",
            "fileUuid": "파일-uuid",
            "originalFilename": "image.jpg",
            "fileUrl": "https://s3.../image.jpg",
            "fileSize": 102400,
            "mimeType": "image/jpeg"
          }
        ],
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "totalPages": 5,
    "totalElements": 230,
    "size": 50,
    "number": 0
  }
}
```

#### 메시지 전송 (REST)
```
POST /api/direct-chats/rooms/{roomUuid}/messages

Request:
{
  "content": "메시지 내용",
  "messageType": "TEXT",
  "fileUuids": ["파일-uuid-1", "파일-uuid-2"]  // 선택
}

Response:
{
  "success": true,
  "data": {
    "uuid": "메시지-uuid",
    "content": "메시지 내용",
    "messageType": "TEXT",
    "isMine": true,
    "attachments": [...],
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

### 3.3 파일 업로드 (3단계)

```
[1단계] Presigned URL 요청
POST /api/files/presigned
{
  "filename": "image.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 102400,
  "entityType": "CHAT_ATTACHMENT"
}

Response:
{
  "success": true,
  "data": {
    "presignedUrl": "https://s3...?signature=...",  // S3 업로드용 URL
    "uploadId": "업로드-id",                         // 저장 필요!
    "fileKey": "uploads/2024/01/abc123.jpg",        // 저장 필요!
    "expiresIn": 3600
  }
}

[2단계] S3 직접 업로드
PUT {presignedUrl}
Content-Type: image/jpeg
Body: (파일 바이너리)

[3단계] 업로드 완료
POST /api/files/complete
{
  "uploadId": "업로드-id",
  "fileKey": "uploads/2024/01/abc123.jpg"
}

Response:
{
  "success": true,
  "data": {
    "uuid": "파일-uuid",           // 메시지 전송 시 사용
    "fileUrl": "https://cdn.../image.jpg",
    "originalFilename": "image.jpg",
    "fileSize": 102400,
    "mimeType": "image/jpeg"
  }
}
```

---

## 4. WebSocket (STOMP)

### 4.1 연결

```
WebSocket URL: /ws (SockJS)

Connect Headers:
{
  "Authorization": "Bearer {accessToken}"
}
```

### 4.2 메시지 흐름

```
┌─────────────────────────────────────────────────────────────┐
│                    클라이언트 → 서버                         │
├─────────────────────────────────────────────────────────────┤
│ /app/direct-chats/rooms/{roomUuid}/messages   메시지 전송    │
│ /app/direct-chats/rooms/{roomUuid}/typing     타이핑 표시    │
│ /app/direct-chats/rooms/{roomUuid}/read       읽음 처리      │
│ /app/direct-chats/rooms/{roomUuid}/enter      채팅방 입장    │
│ /app/direct-chats/rooms/{roomUuid}/leave      채팅방 퇴장    │
│ /app/direct-chats/heartbeat                   온라인 유지    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    서버 → 클라이언트 (구독)                   │
├─────────────────────────────────────────────────────────────┤
│ /topic/direct-chats/rooms/{roomUuid}          메시지 수신    │
│ /topic/direct-chats/rooms/{roomUuid}/typing   타이핑 수신    │
│ /user/queue/errors                            에러 수신      │
│ /user/queue/reply                             응답 수신      │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 메시지 형식

#### 메시지 전송
```
Destination: /app/direct-chats/rooms/{roomUuid}/messages

Body:
{
  "content": "메시지 내용",
  "messageType": "TEXT",           // TEXT, IMAGE, FILE
  "fileUuids": ["파일-uuid"]       // 선택
}
```

#### 메시지 수신
```
Subscribe: /topic/direct-chats/rooms/{roomUuid}

Received:
{
  "uuid": "메시지-uuid",
  "content": "메시지 내용",
  "messageType": "TEXT",
  "senderUuid": "발신자-uuid",
  "senderNickname": "닉네임",
  "isMine": false,
  "attachments": [...],
  "createdAt": "2024-01-15T10:30:00"
}
```

#### 타이핑 표시
```
Send: /app/direct-chats/rooms/{roomUuid}/typing
{ "isTyping": true }

Receive: /topic/direct-chats/rooms/{roomUuid}/typing
{
  "roomUuid": "채팅방-uuid",
  "userUuid": "사용자-uuid",
  "nickname": "닉네임",
  "isTyping": true
}
```

---

## 5. 전체 플로우

### 5.1 채팅방 진입

```
1. GET /api/direct-chats/rooms/{roomUuid}/messages?size=50
   → 기존 메시지 로드 (페이징)

2. WebSocket 연결
   → SockJS + STOMP

3. 구독 등록
   → /topic/direct-chats/rooms/{roomUuid}
   → /topic/direct-chats/rooms/{roomUuid}/typing

4. 채팅방 입장 알림
   → /app/direct-chats/rooms/{roomUuid}/enter
   → 읽음 처리 + 온라인 상태 업데이트
```

### 5.2 메시지 전송 (텍스트)

```
1. 사용자 입력 → Enter

2. WebSocket 전송
   → /app/direct-chats/rooms/{roomUuid}/messages
   → { content: "안녕", messageType: "TEXT" }

3. 서버: DB 저장 → 브로드캐스트

4. 구독자 모두 수신
   → /topic/direct-chats/rooms/{roomUuid}
   → 화면에 메시지 추가
```

### 5.3 메시지 전송 (파일 첨부)

```
1. 파일 선택

2. [REST] POST /api/files/presigned
   → presignedUrl, uploadId, fileKey 획득

3. [HTTP] PUT presignedUrl (S3 직접 업로드)
   → 파일 바이너리 전송

4. [REST] POST /api/files/complete
   → fileUuid 획득

5. [WebSocket] 메시지 전송
   → /app/direct-chats/rooms/{roomUuid}/messages
   → { content: "파일명", messageType: "FILE", fileUuids: ["uuid"] }

6. 수신자: 메시지 + 첨부파일 정보 수신
```

### 5.4 타이핑 표시

```
1. 입력 시작
   → /app/.../typing { isTyping: true }

2. 2초간 입력 없음
   → /app/.../typing { isTyping: false }

3. 상대방 화면
   → "OOO님이 입력 중..." 표시
```

### 5.5 채팅방 퇴장

```
1. 페이지 이탈 또는 다른 채팅방 선택

2. WebSocket 전송
   → /app/direct-chats/rooms/{roomUuid}/leave

3. WebSocket 연결 해제 (필요시)
```

---

## 6. 에러 코드

| 코드 | 설명 |
|------|------|
| DC001 | 채팅방을 찾을 수 없습니다 |
| DC002 | 채팅방에 접근할 권한이 없습니다 |
| DC003 | 자기 자신과 채팅할 수 없습니다 |
| DC004 | 메시지 내용이 비어있습니다 |
| DC005 | 대상 사용자를 찾을 수 없습니다 |
| DC006 | 비활성화된 채팅방입니다 |

---

## 7. 주의사항

1. **WebSocket 재연결**: 연결 끊김 시 자동 재연결 로직 필요 (5초 간격 권장)

2. **타이핑 디바운스**: 입력 시 매번 전송하지 말고, 2초 디바운스 적용

3. **메시지 정렬**: API 응답은 최신순(DESC), 화면 표시는 시간순(ASC)으로 reverse 필요

4. **파일 크기 제한**: 최대 10MB, 첨부파일 최대 10개

5. **읽음 처리**: 채팅방 입장 시 자동으로 처리됨 (enter 호출 시)

6. **온라인 상태**: heartbeat 5분 간격으로 전송하면 온라인 유지

---

## 9. 시퀀스 다이어그램

### 9.1 메시지 전송 플로우

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   User A    │     │   Server    │     │     DB      │     │   User B    │
│  (발신자)    │     │ (WebSocket) │     │ (PostgreSQL)│     │  (수신자)   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │ 1. 메시지 전송     │                   │                   │
       │──────────────────>│                   │                   │
       │  /app/.../messages│                   │                   │
       │                   │                   │                   │
       │                   │ 2. DB 저장        │                   │
       │                   │──────────────────>│                   │
       │                   │  INSERT message   │                   │
       │                   │<──────────────────│                   │
       │                   │                   │                   │
       │                   │ 3. DTO 변환       │                   │
       │                   │ (DB 재조회 없음)   │                   │
       │                   │                   │                   │
       │ 4. 브로드캐스트    │                   │                   │
       │<──────────────────│───────────────────────────────────────>│
       │ /topic/.../rooms  │                   │   /topic/.../rooms│
       │                   │                   │                   │
       │ 5. 화면 표시      │                   │   5. 화면 표시    │
       ▼                   ▼                   ▼                   ▼
```

### 9.2 파일 첨부 메시지 플로우

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │     │   Server    │     │     S3      │     │     DB      │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │ 1. Presigned URL  │                   │                   │
       │──────────────────>│                   │                   │
       │ POST /presigned   │                   │                   │
       │<──────────────────│                   │                   │
       │ {presignedUrl}    │                   │                   │
       │                   │                   │                   │
       │ 2. S3 직접 업로드  │                   │                   │
       │───────────────────────────────────────>│                   │
       │ PUT presignedUrl  │                   │                   │
       │<───────────────────────────────────────│                   │
       │ 200 OK            │                   │                   │
       │                   │                   │                   │
       │ 3. 업로드 완료     │                   │                   │
       │──────────────────>│                   │                   │
       │ POST /complete    │                   │                   │
       │                   │───────────────────────────────────────>│
       │                   │                   │   INSERT file     │
       │<──────────────────│                   │                   │
       │ {fileUuid}        │                   │                   │
       │                   │                   │                   │
       │ 4. 메시지 전송     │                   │                   │
       │──────────────────>│                   │                   │
       │ WS /messages      │                   │                   │
       │ {fileUuids:[...]} │───────────────────────────────────────>│
       │                   │                   │   INSERT message  │
       │                   │                   │   + attachment    │
       ▼                   ▼                   ▼                   ▼
```
