package com.hip.damoa.domain.directchat.service;

import com.hip.damoa.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 채팅 온라인/오프라인 상태 관리 서비스 (Redis 기반)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectChatPresenceService {

    private final RedisService redisService;

    // Redis 키 prefix
    private static final String PRESENCE_PREFIX = "chat:presence:";
    private static final String ROOM_VIEWING_PREFIX = "chat:viewing:";

    // TTL 설정
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);
    private static final Duration ROOM_VIEWING_TTL = Duration.ofMinutes(30);

    // ===== 온라인 상태 관리 =====

    /**
     * 사용자 온라인 상태 설정
     */
    public void setOnline(Long userId) {
        String key = PRESENCE_PREFIX + userId;
        redisService.setValues(key, "online", PRESENCE_TTL);
        log.debug("사용자 온라인 설정: userId={}", userId);
    }

    /**
     * 사용자 오프라인 상태 설정
     */
    public void setOffline(Long userId) {
        String key = PRESENCE_PREFIX + userId;
        redisService.deleteValues(key);
        log.debug("사용자 오프라인 설정: userId={}", userId);
    }

    /**
     * 사용자 온라인 여부 확인
     */
    public boolean isOnline(Long userId) {
        String key = PRESENCE_PREFIX + userId;
        String value = redisService.getValues(key);
        return "online".equals(value);
    }

    /**
     * 여러 사용자의 온라인 상태 일괄 조회 (N+1 방지)
     */
    public Map<Long, Boolean> getOnlineStatuses(List<Long> userIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        for (Long userId : userIds) {
            result.put(userId, isOnline(userId));
        }
        return result;
    }

    /**
     * Heartbeat (TTL 갱신)
     */
    public void heartbeat(Long userId) {
        String key = PRESENCE_PREFIX + userId;
        String value = redisService.getValues(key);
        if (value != null) {
            redisService.setValues(key, value, PRESENCE_TTL);
        } else {
            setOnline(userId);
        }
    }

    // ===== 채팅방 입장 상태 관리 =====

    /**
     * 채팅방 입장
     */
    public void enterRoom(Long userId, UUID roomUuid) {
        String key = ROOM_VIEWING_PREFIX + "room:" + roomUuid + ":user:" + userId;
        redisService.setValues(key, "viewing", ROOM_VIEWING_TTL);
        log.debug("채팅방 입장: userId={}, roomUuid={}", userId, roomUuid);
    }

    /**
     * 채팅방 퇴장
     */
    public void leaveRoom(Long userId, UUID roomUuid) {
        String key = ROOM_VIEWING_PREFIX + "room:" + roomUuid + ":user:" + userId;
        redisService.deleteValues(key);
        log.debug("채팅방 퇴장: userId={}, roomUuid={}", userId, roomUuid);
    }

    /**
     * 채팅방 보고 있는지 확인
     */
    public boolean isViewingRoom(Long userId, UUID roomUuid) {
        String key = ROOM_VIEWING_PREFIX + "room:" + roomUuid + ":user:" + userId;
        String value = redisService.getValues(key);
        return "viewing".equals(value);
    }

    /**
     * 채팅방 Heartbeat (TTL 갱신)
     */
    public void roomHeartbeat(Long userId, UUID roomUuid) {
        String key = ROOM_VIEWING_PREFIX + "room:" + roomUuid + ":user:" + userId;
        String value = redisService.getValues(key);
        if (value != null) {
            redisService.setValues(key, value, ROOM_VIEWING_TTL);
        }
    }

    // ===== 알림 디바운스 =====

    /**
     * 디바운스 키 생성
     */
    public String createDebounceKey(Long roomId, Long userId) {
        return "chat:notification:debounce:room:" + roomId + ":user:" + userId;
    }

    /**
     * 알림 디바운스 체크 (SETNX)
     * @return true: 알림 발송 가능 (첫 번째 알림), false: 디바운스 중 (무시)
     */
    public boolean checkAndSetDebounce(Long roomId, Long userId, long ttlSeconds) {
        String key = createDebounceKey(roomId, userId);
        return redisService.setNX(key, "debouncing", ttlSeconds);
    }

    /**
     * 디바운스 해제 (사용자가 채팅방 입장 시)
     */
    public void clearDebounce(Long roomId, Long userId) {
        String key = createDebounceKey(roomId, userId);
        redisService.deleteValues(key);
    }
}
