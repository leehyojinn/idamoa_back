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
                                "/api/auth/refresh",
                                "/api/oauth/*/login",
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
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Company endpoints - public (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/companies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/slug/{slug}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/companies").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/companies/{companyId}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/companies/{companyId}").authenticated()
                        // Authenticated endpoints
                        .requestMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                        .requestMatchers("/api/oauth/**").authenticated()
                        .requestMatchers("/api/estimates/**").authenticated()  // Estimate/bidding endpoints
                        .requestMatchers("/api/contests/**").authenticated()  // Contest endpoints
                        .requestMatchers("/api/payments/**").authenticated()  // Payment endpoints (except webhook and plans)
                        .requestMatchers("/api/planner/requests/**").authenticated()  // Planner requests (user)
                        .requestMatchers("/api/planner/admin/**").authenticated()  // Planner admin endpoints
                        .requestMatchers("/api/boards/admin/**").authenticated()  // Board admin endpoints
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
}
