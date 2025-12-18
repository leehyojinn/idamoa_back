package com.hip.damoa.config.web;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting 필터
 * - 로그인, 비밀번호 재설정, 회원가입 등 무차별 대입 공격 방어
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIP = getClientIP(request);

        // POST 요청만 Rate Limit 적용
        if ("POST".equals(method) && isRateLimitedEndpoint(path)) {
            Bucket bucket = getBucketForPath(path, clientIP);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (!probe.isConsumed()) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIP, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}"
                );
                return;
            }

            // 남은 요청 수 헤더 추가
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Rate Limit 적용 대상 엔드포인트 확인
     */
    private boolean isRateLimitedEndpoint(String path) {
        return path.equals("/api/auth/login") ||
               path.startsWith("/api/auth/password/reset") ||
               path.startsWith("/api/auth/signup") ||
               path.contains("/with-password") ||  // 비회원 비밀번호 검증
               path.contains("/verify");  // 인증 코드 검증
    }

    /**
     * 경로에 따른 적절한 Bucket 반환
     */
    private Bucket getBucketForPath(String path, String clientIP) {
        if (path.startsWith("/api/auth/password/reset")) {
            return rateLimitConfig.resolvePasswordResetBucket(clientIP);
        }
        if (path.startsWith("/api/auth/signup")) {
            return rateLimitConfig.resolveSignupBucket(clientIP);
        }
        if (path.contains("/with-password") || path.contains("/verify")) {
            return rateLimitConfig.resolvePasswordVerifyBucket(clientIP);
        }
        return rateLimitConfig.resolveLoginBucket(clientIP);
    }

    /**
     * 클라이언트 IP 주소 추출
     * - X-Forwarded-For 헤더 우선 (Nginx/ALB 뒤에 있을 경우)
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
