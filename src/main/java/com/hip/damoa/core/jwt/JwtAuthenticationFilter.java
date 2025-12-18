package com.hip.damoa.core.jwt;

import com.hip.damoa.infra.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = resolveToken((HttpServletRequest) request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 블랙리스트 확인 (로그아웃된 토큰인지 체크)
            if (!isTokenBlacklisted(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.debug("Blacklisted token rejected");
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인 (로그아웃된 토큰)
     */
    private boolean isTokenBlacklisted(String token) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        String blacklisted = redisService.getValues(blacklistKey);
        return blacklisted != null;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}