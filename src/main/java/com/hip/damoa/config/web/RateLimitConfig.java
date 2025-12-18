package com.hip.damoa.config.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 설정
 * - 로그인: 분당 10회
 * - 비밀번호 재설정: 시간당 5회
 * - 회원가입: 분당 5회
 */
@Component
public class RateLimitConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 로그인 Rate Limit: 분당 10회
     */
    public Bucket resolveLoginBucket(String key) {
        return buckets.computeIfAbsent("login:" + key, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * 비밀번호 재설정 Rate Limit: 시간당 5회
     */
    public Bucket resolvePasswordResetBucket(String key) {
        return buckets.computeIfAbsent("pwd:" + key, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofHours(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * 회원가입 Rate Limit: 분당 5회
     */
    public Bucket resolveSignupBucket(String key) {
        return buckets.computeIfAbsent("signup:" + key, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * 비회원 비밀번호 검증 Rate Limit: 분당 5회
     */
    public Bucket resolvePasswordVerifyBucket(String key) {
        return buckets.computeIfAbsent("verify:" + key, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }
}
