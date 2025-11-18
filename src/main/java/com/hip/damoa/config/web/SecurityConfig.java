package com.hip.damoa.config.web;

import com.hip.damoa.core.jwt.JwtAuthenticationFilter;
import com.hip.damoa.core.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.disable())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다. 로그인 후 다시 시도해주세요.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // Check if user is anonymous (not authenticated)
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                            if (auth instanceof AnonymousAuthenticationToken) {
                                // Return 401 for anonymous users
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다. 로그인 후 다시 시도해주세요.\"}");
                            } else {
                                // Return 403 for authenticated users without permission
                                response.setStatus(403);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"message\":\"접근 권한이 없습니다.\"}");
                            }
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints
                        .requestMatchers(
                                "/api/health/**",
                                "/api/auth/login",
                                "/api/auth/signup/**",
                                "/api/auth/email/**",
                                "/api/auth/sms/**",
                                "/api/auth/verification/**",
                                "/api/auth/password/reset/**",  // 비밀번호 찾기 (로그인 불필요)
                                "/api/auth/refresh",
                                "/api/oauth/*/authorize",
                                "/api/oauth/*/callback",
                                "/api/estimates/requests/public",  // Public estimate requests
                                "/api/estimates/requests/{requestId}",  // View estimate request
                                "/api/contests/public",  // Public contests
                                "/api/contests/{contestId}",  // View contest
                                "/api/contests/winners",  // View contests with winners
                                "/api/contests/upcoming",  // View upcoming contests
                                "/api/contests/by-prize",  // View contests by prize
                                "/api/contests/free",  // View free entry contests
                                "/api/contests/ending-soon",  // View contests ending soon
                                "/api/contests/{contestId}/entries",  // View contest entries
                                "/api/contests/{contestId}/winner",  // View contest winner
                                "/api/contests/entries/{entryId}",  // View entry
                                "/api/payments/webhook",  // Payment webhook (PG providers)
                                "/api/payments/plans",  // View subscription plans
                                "/api/payments/plans/*",  // View specific plan
                                "/api/boards/*/search",  // Search boards (public)
                                "/api/boards/*/popular",  // Popular boards (public)
                                "/api/boards/*/*",  // View specific board (public)
                                "/api/boards/*",  // View boards by type (public)
                                "/api/popups/active",  // Active popups (public)
                                "/api/popups/*/view",  // Increment view count (public)
                                "/api/popups/*/click",  // Increment click count (public)
                                "/api/filters",  // Get all filter categories (public)
                                "/api/filters/**",  // Get specific filter category/options (public)
                                "/*.html",  // HTML pages (public)
                                "/js/**",  // JavaScript files
                                "/css/**",  // CSS files
                                "/images/**",  // Image files
                                "/favicon.ico",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/ws/**"  // WebSocket endpoint
                        ).permitAll()
                        // Quick Consultation - public (non-member consultation)
                        .requestMatchers(HttpMethod.GET, "/api/consultations").permitAll()  // List (public)
                        .requestMatchers(HttpMethod.POST, "/api/consultations").permitAll()  // Create (public)
                        .requestMatchers(HttpMethod.POST, "/api/consultations/*/verify").permitAll()  // Detail (unified - member/non-member)
                        .requestMatchers(HttpMethod.PUT, "/api/consultations/*/with-password").permitAll()  // Update (non-member)
                        .requestMatchers(HttpMethod.DELETE, "/api/consultations/*/with-password").permitAll()  // Cancel (non-member)
                        // Company endpoints - public (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/companies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/slug/{slug}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/companies").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/companies/{companyId}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/companies/{companyId}").authenticated()
                        // Company Review endpoints - public (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/{reviewId}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/companies/{companyId}/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{reviewId}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reviews/{reviewId}/reply").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{reviewId}/reply").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}/reply").authenticated()
                        // Estimate Request endpoints - public (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/estimates/requests").permitAll()  // Public list
                        .requestMatchers(HttpMethod.GET, "/api/estimates/requests/*").permitAll()  // View detail (UUID)
                        // Authenticated endpoints
                        .requestMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                        .requestMatchers("/api/estimates/**").authenticated()  // Estimate/bidding endpoints (other than public GET)
                        .requestMatchers("/api/contests/**").authenticated()  // Contest endpoints
                        .requestMatchers("/api/payments/**").authenticated()  // Payment endpoints (except webhook and plans)
                        .requestMatchers("/api/planner/requests/**").authenticated()  // Planner requests (user)
                        .requestMatchers("/api/planner/admin/**").authenticated()  // Planner admin endpoints
                        .requestMatchers("/api/boards/admin/**").authenticated()  // Board admin endpoints
                        .requestMatchers("/api/notifications/**").authenticated()  // Notification endpoints
                        .requestMatchers("/api/chats/**").authenticated()  // Chat endpoints
                        .requestMatchers("/api/admin/**").authenticated()  // Admin endpoints
                        .requestMatchers("/api/files/**").authenticated()  // File upload endpoints
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 설정
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.0.217:*",
                "http://192.168.0.217"
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 자격증명 허용 (쿠키, 인증 헤더 등)
        configuration.setAllowCredentials(true);

        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        // 노출할 헤더 (클라이언트에서 접근 가능한 헤더)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
