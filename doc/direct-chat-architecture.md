# 범용 1:1 채팅 기능 아키텍처

## 목차
1. [전체 아키텍처](#1-전체-아키텍처)
2. [채팅방 생성/조회 Flow](#2-채팅방-생성조회-flow)
3. [메시지 전송 Flow (WebSocket)](#3-메시지-전송-flow-websocket)
4. [알림 디바운스 Flow](#4-알림-디바운스-flow)
5. [Transactional Outbox 패턴](#5-transactional-outbox-패턴)
6. [온라인 Presence Flow](#6-온라인-presence-flow)
7. [채팅방 입장 시 알림 취소](#7-채팅방-입장-시-알림-취소-flow)
8. [사용 예시 (클라이언트)](#8-사용-예시-클라이언트-관점)
9. [API 레퍼런스](#9-api-레퍼런스)
10. [핵심 설계 결정](#10-핵심-설계-결정-요약)

---

## 1. 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Client (App/Web)                           │
├─────────────────────────────────────────────────────────────────────────┤
│  REST API (/api/direct-chats/*)  │  WebSocket (/ws → /app/direct-chats) │
└────────────────┬─────────────────┴──────────────────┬───────────────────┘
                 │                                    │
                 ▼                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DirectChatController (REST)                      │
│                    DirectChatWebSocketController (WebSocket)            │
└────────────────┬─────────────────┬──────────────────┬───────────────────┘
                 │                 │                  │
                 ▼                 ▼                  ▼
┌────────────────────┐  ┌──────────────────┐  ┌─────────────────────────┐
│  DirectChatService │  │ PresenceService  │  │ NotificationService     │
│  (채팅 핵심 로직)   │  │ (온라인 상태)     │  │ (알림 + 디바운스)        │
└─────────┬──────────┘  └────────┬─────────┘  └───────────┬─────────────┘
          │                      │                        │
          ▼                      ▼                        ▼
┌─────────────────────┐  ┌──────────────────┐  ┌─────────────────────────┐
│     PostgreSQL      │  │      Redis       │  │  NotificationOutbox     │
│  (영구 데이터 저장)  │  │  (임시 상태 저장) │  │  (Transactional Outbox) │
└─────────────────────┘  └──────────────────┘  └───────────┬─────────────┘
                                                           │
                                                           ▼
                                              ┌─────────────────────────┐
                                              │  OutboxWorker (10초)    │
                                              │  → 카카오 알림톡 발송    │
                                              └─────────────────────────┘
```

### 컴포넌트 설명

| 컴포넌트 | 역할 | 저장소 |
|---------|------|--------|
| DirectChatService | 채팅방/메시지 CRUD, 읽음 처리 | PostgreSQL |
| DirectChatPresenceService | 온라인 상태, 채팅방 입장 상태, 디바운스 | Redis |
| DirectChatNotificationService | 알림 예약, 디바운스 체크, 알림 취소 | PostgreSQL + Redis |
| NotificationOutboxWorker | 대기 중인 알림 폴링 및 발송 | PostgreSQL |

---

## 2. 채팅방 생성/조회 Flow

```
┌──────────┐     POST /api/direct-chats/rooms        ┌─────────────────┐
│  User A  │  ──────────────────────────────────────▶│ DirectChatCtrl  │
│          │     { targetUserUuid: "user-b-uuid" }   └────────┬────────┘
└──────────┘                                                   │
                                                              ▼
                                              ┌─────────────────────────┐
                                              │   DirectChatService     │
                                              │   getOrCreateRoom()     │
                                              └────────────┬────────────┘
                                                           │
                    ┌──────────────────────────────────────┼──────────────────┐
                    │                                      │                  │
                    ▼                                      ▼                  │
           ┌────────────────┐                    ┌──────────────────┐         │
           │ 기존 채팅방 있음? │───── Yes ────────▶│ 기존 채팅방 반환  │         │
           └────────┬───────┘                    └──────────────────┘         │
                    │ No                                                      │
                    ▼                                                         │
           ┌─────────────────────────────────────────────────────┐           │
           │  새 채팅방 생성                                       │           │
           │  user1_id = min(A.id, B.id)  ← 중복 방지 규칙         │           │
           │  user2_id = max(A.id, B.id)                          │           │
           └─────────────────────────────────────────────────────┘           │
                    │                                                         │
                    ▼                                                         │
           ┌──────────────────┐                                              │
           │  DirectChatRoom  │◀─────────────────────────────────────────────┘
           │  UUID 반환       │
           └──────────────────┘
```

### 핵심 포인트: 중복 방지 규칙

```java
// DirectChatRoom.java
public static DirectChatRoom create(User userA, User userB) {
    // user1_id < user2_id 규칙으로 동일한 두 사용자 간 채팅방 중복 생성 방지
    User user1 = userA.getId() < userB.getId() ? userA : userB;
    User user2 = userA.getId() < userB.getId() ? userB : userA;

    return DirectChatRoom.builder()
            .user1(user1)
            .user2(user2)
            .build();
}
```

**DB 제약 조건**:
```sql
CONSTRAINT uq_direct_chat_room_users UNIQUE (user1_id, user2_id)
```

---

## 3. 메시지 전송 Flow (WebSocket)

```
┌──────────┐                                                      ┌──────────┐
│  User A  │                                                      │  User B  │
│ (발신자)  │                                                      │ (수신자)  │
└────┬─────┘                                                      └────┬─────┘
     │                                                                 │
     │  /app/direct-chats/rooms/{roomUuid}/messages                    │
     │  { content: "안녕하세요", messageType: "TEXT" }                  │
     │                                                                 │
     ▼                                                                 │
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DirectChatWebSocketController                        │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │       DirectChatService         │
                    │       sendMessage()             │
                    │  1. 메시지 DB 저장               │
                    │  2. 채팅방 lastMessage 업데이트  │
                    └─────────────────┬───────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
        ┌───────────────────┐  ┌────────────────┐  ┌───────────────────────┐
        │ SimpMessaging     │  │ Notification   │  │  PostgreSQL           │
        │ Template          │  │ Service        │  │  direct_chat_messages │
        │                   │  │                │  └───────────────────────┘
        │ /topic/direct-    │  │ (알림 예약)    │
        │ chats/rooms/      │  │                │
        │ {roomUuid}        │  └────────────────┘
        └─────────┬─────────┘
                  │
                  │  브로드캐스트
                  ▼
     ┌────────────────────────────────────────────────────┐
     │                                                    │
     ▼                                                    ▼
┌──────────┐                                        ┌──────────┐
│  User A  │  실시간 수신                           │  User B  │
│ (본인)   │                                        │ (상대방)  │
└──────────┘                                        └──────────┘
```

### 메시지 타입

```java
public enum MessageType {
    TEXT,    // 일반 텍스트
    IMAGE,   // 이미지
    FILE,    // 파일
    SYSTEM   // 시스템 메시지 (입장/퇴장 등)
}
```

---

## 4. 알림 디바운스 Flow

### 문제 상황
- User A가 짧은 시간에 여러 메시지를 보내면?
- 매번 알림을 보내면 User B에게 알림 폭탄

### 해결: 5분 디바운스

```
시간 →  0분        1분        2분        3분        4분        5분        6분
        │          │          │          │          │          │          │
User A  ├──msg1──▶├──msg2───▶├──msg3───▶├─────────├──────────├──────────├──msg4──▶
        │          │          │          │          │          │          │
        │          │          │          │          │          │          │
        ▼          ▼          ▼          ▼          ▼          ▼          ▼
   ┌─────────┐                                                       ┌─────────┐
   │ 알림 #1 │  (무시)     (무시)                                     │ 알림 #2 │
   │ 발송!   │  디바운스   디바운스                                   │ 발송!   │
   └─────────┘  TTL 5분    TTL 5분                                   └─────────┘
        │          내 활성   내 활성                                       │
        ▼                                                                 ▼
   ┌───────────────────────────────────────────────────────────────────────────┐
   │                              Redis                                        │
   │  KEY: chat:notification:debounce:room:{roomId}:user:{userId}              │
   │  TTL: 300초 (5분)                                                         │
   │                                                                           │
   │  setNX → 첫 번째 메시지만 true 반환 (알림 발송)                            │
   │  setNX → 이후 메시지는 false 반환 (알림 무시)                              │
   │  TTL 만료 → 키 삭제 → 다음 메시지에 다시 알림 발송                         │
   └───────────────────────────────────────────────────────────────────────────┘
```

### 구현 코드

```java
// DirectChatPresenceService.java
public boolean checkAndSetDebounce(Long roomId, Long userId, long ttlSeconds) {
    String key = String.format("chat:notification:debounce:room:%d:user:%d", roomId, userId);
    // setNX: 키가 없으면 설정하고 true, 이미 있으면 false
    return redisService.setNX(key, "1", Duration.ofSeconds(ttlSeconds));
}

// DirectChatNotificationService.java
@Transactional
public void scheduleMessageNotification(DirectChatRoom room, DirectChatMessage message, User recipient) {
    // 1. 수신자가 현재 채팅방을 보고 있으면 알림 불필요
    if (presenceService.isViewingRoom(recipient.getId(), room.getUuid())) {
        return;
    }

    // 2. 디바운스 체크 (5분 내 중복 알림 방지)
    boolean shouldSend = presenceService.checkAndSetDebounce(
            room.getId(), recipient.getId(), DEBOUNCE_TTL_SECONDS);

    if (!shouldSend) {
        log.debug("디바운스 중이므로 알림 생략");
        return;
    }

    // 3. Outbox에 알림 저장
    NotificationOutbox outbox = NotificationOutbox.createChatNotification(
            recipient, senderName, messagePreview, room.getId());
    outboxRepository.save(outbox);
}
```

---

## 5. Transactional Outbox 패턴

### 왜 Outbox 패턴을 사용하는가?

**문제**: 메시지 저장과 알림 발송이 별개 시스템
```
1. 메시지 저장 (PostgreSQL) ✅
2. 알림 발송 (카카오 API) ❌ 실패!
→ 메시지는 저장됐는데 알림은 안 감
```

**해결**: 알림도 같은 트랜잭션에서 DB에 저장
```
트랜잭션 시작
  1. 메시지 저장 (PostgreSQL)
  2. 알림 Outbox 저장 (PostgreSQL) ← 같은 DB!
트랜잭션 커밋
→ 둘 다 성공하거나 둘 다 실패 (원자성)
```

### Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          트랜잭션 (Atomic)                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  1. 메시지 저장 (direct_chat_messages)                                  ││
│  │  2. 알림 Outbox 저장 (notification_outbox) ← 같은 트랜잭션!             ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ 트랜잭션 커밋
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         notification_outbox 테이블                           │
│  ┌───────┬───────────┬─────────┬──────────────┬────────────┬──────────────┐ │
│  │  id   │ recipient │ status  │ scheduled_at │ retry_cnt  │ content      │ │
│  ├───────┼───────────┼─────────┼──────────────┼────────────┼──────────────┤ │
│  │  101  │  user_b   │ PENDING │ 2025-12-30   │     0      │ A님이 메시지..│ │
│  └───────┴───────────┴─────────┴──────────────┴────────────┴──────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ @Scheduled(fixedDelay = 10000) - 10초마다
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       NotificationOutboxWorker                               │
│                                                                              │
│  1. status=PENDING, scheduled_at <= now 조회                                │
│  2. status → PROCESSING                                                     │
│  3. 채널별 발송 (카카오 알림톡, SMS, PUSH 등)                                 │
│  4. 성공 → status=SENT / 실패 → retry_count++, 지수 백오프                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
             ┌──────────┐     ┌──────────┐     ┌──────────┐
             │  카카오   │     │   SMS    │     │   PUSH   │
             │  알림톡   │     │          │     │          │
             └──────────┘     └──────────┘     └──────────┘
```

### 지수 백오프 재시도

```
1차 실패 → 1분 후 재시도  (2^0 = 1분)
2차 실패 → 2분 후 재시도  (2^1 = 2분)
3차 실패 → 4분 후 재시도  (2^2 = 4분)
4차 실패 → status = FAILED (포기)
```

```java
// NotificationOutbox.java
public void markFailed(String error) {
    this.lastError = error;
    this.retryCount++;

    if (this.retryCount >= this.maxRetries) {
        this.status = OutboxStatus.FAILED;
    } else {
        this.status = OutboxStatus.PENDING;
        // 지수 백오프
        int delayMinutes = (int) Math.pow(2, this.retryCount - 1);
        this.scheduledAt = LocalDateTime.now().plusMinutes(delayMinutes);
    }
}
```

---

## 6. 온라인 Presence Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              User A (앱 실행)                                │
└────────────────────────────────────────┬─────────────────────────────────────┘
                                         │
          ┌──────────────────────────────┼──────────────────────────────┐
          │                              │                              │
          ▼                              ▼                              ▼
   ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
   │  WebSocket 연결  │         │  채팅방 입장     │         │  Heartbeat      │
   │  → setOnline()  │         │  → enterRoom()  │         │  (30초마다)      │
   └────────┬────────┘         └────────┬────────┘         └────────┬────────┘
            │                           │                           │
            ▼                           ▼                           ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                   Redis                                       │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │  KEY: chat:presence:{userId}                                            │ │
│  │  VALUE: "online"                                                        │ │
│  │  TTL: 300초 (5분) ← heartbeat 마다 갱신                                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │  KEY: chat:room:viewing:{userId}                                        │ │
│  │  VALUE: "{roomUuid}"                                                    │ │
│  │  TTL: 600초 (10분)                                                      │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         │ TTL 만료 (5분 heartbeat 없으면)
                                         ▼
                              ┌─────────────────────┐
                              │  자동 오프라인 처리  │
                              │  (키 삭제됨)         │
                              └─────────────────────┘
```

### Redis 키 구조

| 키 패턴 | 용도 | TTL |
|--------|------|-----|
| `chat:presence:{userId}` | 온라인 상태 | 5분 |
| `chat:room:viewing:{userId}` | 현재 보고 있는 채팅방 | 10분 |
| `chat:notification:debounce:room:{roomId}:user:{userId}` | 알림 디바운스 | 5분 |

### 구현 코드

```java
// DirectChatPresenceService.java
private static final String PRESENCE_PREFIX = "chat:presence:";
private static final String ROOM_VIEWING_PREFIX = "chat:room:viewing:";
private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);

public void setOnline(Long userId) {
    String key = PRESENCE_PREFIX + userId;
    redisService.setValues(key, "online", PRESENCE_TTL);
}

public boolean isOnline(Long userId) {
    String key = PRESENCE_PREFIX + userId;
    return redisService.getValues(key) != null;
}

public void enterRoom(Long userId, UUID roomUuid) {
    String key = ROOM_VIEWING_PREFIX + userId;
    redisService.setValues(key, roomUuid.toString(), Duration.ofMinutes(10));
}

public boolean isViewingRoom(Long userId, UUID roomUuid) {
    String key = ROOM_VIEWING_PREFIX + userId;
    String value = redisService.getValues(key);
    return roomUuid.toString().equals(value);
}
```

---

## 7. 채팅방 입장 시 알림 취소 Flow

### 시나리오
1. User A가 메시지를 보냄
2. 알림이 Outbox에 저장됨 (PENDING 상태)
3. 10초 후 OutboxWorker가 알림 발송 예정
4. **그 사이에 User B가 채팅방에 들어옴**
5. User B는 이미 메시지를 봤으므로 알림 불필요
6. → 대기 중인 알림을 취소해야 함

```
                    User B가 채팅방에 들어옴
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  /app/direct-chats/rooms/{roomUuid}/enter                                   │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
           ┌─────────────────────┼─────────────────────┐
           │                     │                     │
           ▼                     ▼                     ▼
┌───────────────────┐  ┌───────────────────┐  ┌───────────────────────────────┐
│  Presence 업데이트 │  │  읽음 처리         │  │  대기 중 알림 취소             │
│                   │  │                   │  │                               │
│  enterRoom()      │  │  markAsRead()     │  │  cancelPendingNotifications() │
│  - Redis에        │  │  - 마지막 읽은    │  │  - Outbox에서 PENDING 상태    │
│    현재 보는 방    │  │    메시지 ID 저장  │  │    알림을 CANCELLED로 변경    │
│    기록           │  │                   │  │  - 디바운스 키 삭제            │
└───────────────────┘  └───────────────────┘  └───────────────────────────────┘
```

### 구현 코드

```java
// DirectChatNotificationService.java
@Transactional
public void cancelPendingNotifications(Long roomId, Long userId) {
    // 1. 디바운스 해제
    presenceService.clearDebounce(roomId, userId);

    // 2. Outbox에서 대기 중인 알림 취소
    int cancelled = outboxRepository.cancelPendingByEntity(
            userId, "DirectChatRoom", roomId, LocalDateTime.now());

    if (cancelled > 0) {
        log.info("대기 중인 알림 {} 건 취소됨", cancelled);
    }
}
```

---

## 8. 사용 예시 (클라이언트 관점)

### REST API 사용

```javascript
// 1. 채팅방 생성/조회
const response = await fetch('/api/direct-chats/rooms', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer {jwt}',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ targetUserUuid: 'target-user-uuid' })
});
const { data: room } = await response.json();
// room.uuid → 채팅방 UUID

// 2. 메시지 목록 조회
const messages = await fetch(`/api/direct-chats/rooms/${room.uuid}/messages`, {
  headers: { 'Authorization': 'Bearer {jwt}' }
});

// 3. 채팅방 목록 조회
const rooms = await fetch('/api/direct-chats/rooms', {
  headers: { 'Authorization': 'Bearer {jwt}' }
});

// 4. 미읽음 수 조회
const unread = await fetch('/api/direct-chats/unread-count', {
  headers: { 'Authorization': 'Bearer {jwt}' }
});
```

### WebSocket 사용

```javascript
// 1. WebSocket 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({ Authorization: 'Bearer {jwt}' }, () => {

  // 2. 채팅방 구독 (메시지 수신)
  stompClient.subscribe(`/topic/direct-chats/rooms/${roomUuid}`, (message) => {
    const msg = JSON.parse(message.body);
    console.log('새 메시지:', msg);
    // {
    //   uuid: "message-uuid",
    //   senderUuid: "user-uuid",
    //   content: "안녕하세요",
    //   messageType: "TEXT",
    //   createdAt: "2025-12-30T10:00:00"
    // }
  });

  // 3. 타이핑 인디케이터 구독
  stompClient.subscribe(`/topic/direct-chats/rooms/${roomUuid}/typing`, (data) => {
    const { nickname, isTyping } = JSON.parse(data.body);
    if (isTyping) {
      showTypingIndicator(`${nickname}님이 입력 중...`);
    } else {
      hideTypingIndicator();
    }
  });

  // 4. 채팅방 입장 알림
  stompClient.send(`/app/direct-chats/rooms/${roomUuid}/enter`, {}, {});

  // 5. 메시지 전송
  stompClient.send(`/app/direct-chats/rooms/${roomUuid}/messages`, {}, JSON.stringify({
    content: '안녕하세요!',
    messageType: 'TEXT'
  }));

  // 6. 파일 첨부 메시지 전송
  stompClient.send(`/app/direct-chats/rooms/${roomUuid}/messages`, {}, JSON.stringify({
    content: '사진 보내드립니다',
    messageType: 'IMAGE',
    fileUuids: ['file-uuid-1', 'file-uuid-2']
  }));

  // 7. 타이핑 인디케이터 전송
  stompClient.send(`/app/direct-chats/rooms/${roomUuid}/typing`, {}, JSON.stringify({
    isTyping: true
  }));

  // 8. 채팅방 퇴장
  stompClient.send(`/app/direct-chats/rooms/${roomUuid}/leave`, {}, {});

  // 9. Heartbeat (30초마다)
  setInterval(() => {
    stompClient.send('/app/direct-chats/heartbeat', {}, {});
  }, 30000);
});

// 연결 해제 시
stompClient.disconnect(() => {
  console.log('WebSocket 연결 해제');
});
```

---

## 9. API 레퍼런스

### REST API

| Method | Endpoint | 설명 | Request Body |
|--------|----------|------|--------------|
| POST | `/api/direct-chats/rooms` | 채팅방 생성/조회 | `{ targetUserUuid }` |
| GET | `/api/direct-chats/rooms` | 채팅방 목록 | - |
| GET | `/api/direct-chats/rooms/{roomUuid}` | 채팅방 상세 | - |
| DELETE | `/api/direct-chats/rooms/{roomUuid}` | 채팅방 나가기 | - |
| GET | `/api/direct-chats/rooms/{roomUuid}/messages` | 메시지 목록 | `?page=0&size=50` |
| POST | `/api/direct-chats/rooms/{roomUuid}/messages` | 메시지 전송 | `{ content, messageType, fileUuids }` |
| POST | `/api/direct-chats/rooms/{roomUuid}/read` | 읽음 처리 | - |
| GET | `/api/direct-chats/rooms/{roomUuid}/unread-count` | 미읽음 수 | - |
| GET | `/api/direct-chats/unread-count` | 전체 미읽음 수 | - |

### WebSocket Endpoints

| Direction | Destination | 설명 | Payload |
|-----------|-------------|------|---------|
| Client → Server | `/app/direct-chats/rooms/{roomUuid}/messages` | 메시지 전송 | `{ content, messageType, fileUuids }` |
| Client → Server | `/app/direct-chats/rooms/{roomUuid}/typing` | 타이핑 상태 | `{ isTyping }` |
| Client → Server | `/app/direct-chats/rooms/{roomUuid}/read` | 읽음 처리 | - |
| Client → Server | `/app/direct-chats/rooms/{roomUuid}/enter` | 채팅방 입장 | - |
| Client → Server | `/app/direct-chats/rooms/{roomUuid}/leave` | 채팅방 퇴장 | - |
| Client → Server | `/app/direct-chats/heartbeat` | Heartbeat | - |
| Server → Client | `/topic/direct-chats/rooms/{roomUuid}` | 메시지 수신 | `DirectChatMessageResponse` |
| Server → Client | `/topic/direct-chats/rooms/{roomUuid}/typing` | 타이핑 알림 | `TypingIndicator` |
| Server → Client | `/user/queue/errors` | 에러 알림 | `String` |

---

## 10. 핵심 설계 결정 요약

| 기능 | 구현 방식 | 이유 |
|------|----------|------|
| 실시간 메시지 | WebSocket + STOMP | 양방향 통신, 저지연, Spring 통합 용이 |
| 온라인 상태 | Redis TTL | 자동 만료로 오프라인 감지, 고성능 |
| 알림 디바운스 | Redis setNX + TTL | 원자적 연산, 5분 자동 해제 |
| 알림 발송 | Transactional Outbox | 메시지-알림 일관성 보장, 재시도 가능 |
| 재시도 | 지수 백오프 | 시스템 부하 방지, 점진적 재시도 |
| 채팅방 중복 방지 | user1_id < user2_id | DB 레벨 유니크 제약으로 확실한 방지 |
| 영구 데이터 | PostgreSQL | 트랜잭션, 관계형 쿼리, 데이터 무결성 |
| 임시 상태 | Redis | 고성능, TTL 지원, 휘발성 데이터에 적합 |

---

## 파일 구조

```
src/main/java/com/hip/damoa/domain/
├── directchat/
│   ├── model/
│   │   ├── DirectChatRoom.java
│   │   ├── DirectChatMessage.java
│   │   ├── DirectChatAttachment.java
│   │   ├── DirectChatReadState.java
│   │   └── MessageType.java
│   ├── repository/
│   │   ├── DirectChatRoomRepository.java
│   │   ├── DirectChatMessageRepository.java
│   │   ├── DirectChatReadStateRepository.java
│   │   └── DirectChatAttachmentRepository.java
│   ├── service/
│   │   ├── DirectChatService.java
│   │   ├── DirectChatPresenceService.java
│   │   └── DirectChatNotificationService.java
│   └── web/
│       ├── DirectChatController.java
│       ├── DirectChatWebSocketController.java
│       └── dto/
│           ├── DirectChatRoomCreateRequest.java
│           ├── DirectChatMessageRequest.java
│           ├── DirectChatRoomResponse.java
│           ├── DirectChatMessageResponse.java
│           ├── AttachmentResponse.java
│           ├── ParticipantInfo.java
│           ├── UnreadCountResponse.java
│           └── TypingIndicator.java
└── notification/
    ├── model/
    │   ├── NotificationOutbox.java
    │   ├── OutboxStatus.java
    │   ├── NotificationChannel.java
    │   └── NotificationType.java
    ├── repository/
    │   └── NotificationOutboxRepository.java
    └── service/
        └── NotificationOutboxWorker.java
```

---

## DB 스키마

```sql
-- 1:1 채팅방
CREATE TABLE direct_chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    user1_id BIGINT NOT NULL REFERENCES users(id),
    user2_id BIGINT NOT NULL REFERENCES users(id),
    last_message TEXT,
    last_message_at TIMESTAMP,
    CONSTRAINT uq_direct_chat_room_users UNIQUE (user1_id, user2_id)
);

-- 채팅 메시지
CREATE TABLE direct_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    room_id BIGINT NOT NULL REFERENCES direct_chat_rooms(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 읽음 상태
CREATE TABLE direct_chat_read_states (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES direct_chat_rooms(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    last_read_message_id BIGINT REFERENCES direct_chat_messages(id),
    last_read_at TIMESTAMP,
    CONSTRAINT uq_direct_chat_read_state UNIQUE (room_id, user_id)
);

-- 알림 Outbox
CREATE TABLE notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    channel VARCHAR(30) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    debounce_key VARCHAR(200)
);
```
