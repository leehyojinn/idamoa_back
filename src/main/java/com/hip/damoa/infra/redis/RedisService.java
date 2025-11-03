package com.hip.damoa.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Object 타입 메서드 (기존 호환성)
    public void setData(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    public Object getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    // String 타입 메서드 (신규 인증 시스템용)
    public void setValues(String key, String value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    public String getValues(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    // Advanced operations

    /**
     * Set value only if key does not exist (SETNX)
     * Used for idempotency key checking
     *
     * @param key the key
     * @param value the value
     * @param ttlSeconds time to live in seconds
     * @return true if the key was set, false if key already exists
     */
    public boolean setNX(String key, String value, long ttlSeconds) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(result);
    }

    /**
     * Delete key (alias for deleteValues)
     * @param key the key to delete
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
