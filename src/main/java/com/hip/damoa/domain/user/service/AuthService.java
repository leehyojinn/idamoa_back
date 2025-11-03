package com.hip.damoa.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.jwt.JwtTokenProvider;
import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.repository.UserProfileRepository;
import com.hip.damoa.domain.user.repository.UserRepository;
import com.hip.damoa.domain.user.web.dto.*;
import com.hip.damoa.domain.user.web.dto.*;
import com.hip.damoa.infra.redis.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 인증 관련 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final VerificationService verificationService;
    private final ObjectMapper objectMapper;

    private static final String SIGNUP_PREFIX = "signup:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";
    private static final Duration SIGNUP_TTL = Duration.ofMinutes(10);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    /**
     * 회원가입 시작 (1단계)
     * - 이메일 중복 체크
     * - Redis에 회원가입 정보 임시 저장
     * - UUID 토큰 생성 및 반환
     */
    @Transactional(readOnly = true)
    public SignupStartResponse startSignup(SignupStartRequest request) {
        log.info("회원가입 시작: email={}", request.getEmail());

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // UUID 토큰 생성
        String signupToken = UUID.randomUUID().toString();
        String redisKey = SIGNUP_PREFIX + signupToken;

        // Redis에 회원가입 정보 저장
        try {
            String jsonData = objectMapper.writeValueAsString(request);
            redisService.setValues(redisKey, jsonData, SIGNUP_TTL);
            log.info("회원가입 데이터 저장 완료: token={}", signupToken);
        } catch (JsonProcessingException e) {
            log.error("회원가입 데이터 직렬화 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return SignupStartResponse.builder()
                .signupToken(signupToken)
                .message("이메일과 SMS 인증을 진행해주세요")
                .expiresIn(SIGNUP_TTL.getSeconds())
                .build();
    }

    /**
     * 회원가입 완료 (2단계)
     * - 이메일/SMS 인증 확인
     * - Redis에서 회원가입 정보 조회
     * - User 및 Profile 생성
     */
    @Transactional
    public TokenInfo completeSignup(SignupCompleteRequest request) {
        log.info("회원가입 완료 시작: token={}", request.getSignupToken());

        // Redis에서 회원가입 정보 조회
        String redisKey = SIGNUP_PREFIX + request.getSignupToken();
        String jsonData = redisService.getValues(redisKey);

        if (jsonData == null) {
            throw new BusinessException(ErrorCode.SIGNUP_TOKEN_NOT_FOUND);
        }

        SignupStartRequest signupData;
        try {
            signupData = objectMapper.readValue(jsonData, SignupStartRequest.class);
        } catch (JsonProcessingException e) {
            log.error("회원가입 데이터 역직렬화 실패", e);
            throw new BusinessException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }

        // 이메일 인증 확인
        verificationService.verifyEmailCode(signupData.getEmail(), request.getEmailVerificationCode());

        // SMS 인증 확인
        verificationService.verifySmsCode(signupData.getPhoneNumber(), request.getSmsVerificationCode());

        // User 생성
        User user = User.builder()
                .email(signupData.getEmail())
                .password(passwordEncoder.encode(signupData.getPassword()))
                .roles(new String[]{signupData.getRole()})
                .emailVerified(true)
                .phoneVerified(true)
                .termsAgreed(signupData.getTermsAgreed())
                .privacyAgreed(signupData.getPrivacyAgreed())
                .marketingAgreed(signupData.getMarketingAgreed())
                .build();

        // 약관 동의 시간 설정
        if (signupData.getTermsAgreed()) {
            user = User.builder()
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRoles())
                    .emailVerified(true)
                    .emailVerifiedAt(LocalDateTime.now())
                    .phoneVerified(true)
                    .phoneVerifiedAt(LocalDateTime.now())
                    .termsAgreed(true)
                    .termsAgreedAt(LocalDateTime.now())
                    .privacyAgreed(true)
                    .privacyAgreedAt(LocalDateTime.now())
                    .marketingAgreed(signupData.getMarketingAgreed())
                    .marketingAgreedAt(signupData.getMarketingAgreed() ? LocalDateTime.now() : null)
                    .build();
        }

        user = userRepository.save(user);
        log.info("사용자 생성 완료: id={}, email={}, roles={}", user.getId(), user.getEmail(), String.join(",", user.getRoles()));

        // Role별 프로필 생성
        createProfileByRole(user, signupData);

        // Redis에서 회원가입 토큰 삭제
        redisService.deleteValues(redisKey);

        // JWT 토큰 발급
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user.getEmail(), user.getRoles()[0]);

        // Refresh 토큰 Redis에 저장
        saveRefreshToken(user.getId(), tokenInfo.getRefreshToken());

        log.info("회원가입 완료: userId={}, email={}", user.getId(), user.getEmail());

        return tokenInfo;
    }

    /**
     * 로그인
     */
    @Transactional
    public TokenInfo login(UserLoginRequest request, HttpServletRequest httpRequest) {
        log.info("로그인 시도: email={}", request.getEmail());

        // 인증
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // User 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 계정 상태 체크
        if (user.getIsDeleted()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!user.isAccountNonLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 로그인 성공 처리
        String ipAddress = getClientIp(httpRequest);
        user.loginSuccess(ipAddress);
        userRepository.save(user);

        // JWT 토큰 발급
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user.getEmail(), user.getRoles()[0]);

        // Refresh 토큰 Redis에 저장
        saveRefreshToken(user.getId(), tokenInfo.getRefreshToken());

        log.info("로그인 성공: userId={}, email={}, ip={}", user.getId(), user.getEmail(), ipAddress);

        return tokenInfo;
    }

    /**
     * 토큰 갱신
     */
    @Transactional
    public TokenInfo refreshToken(TokenRefreshRequest request) {
        log.info("토큰 갱신 요청");

        // Refresh 토큰 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // Refresh 토큰에서 사용자 정보 추출
        String email = jwtTokenProvider.getUserEmail(request.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Redis에서 Refresh 토큰 확인
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();
        String storedToken = redisService.getValues(redisKey);

        if (storedToken == null || !storedToken.equals(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 새로운 토큰 발급
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user.getEmail(), user.getRoles()[0]);

        // 새로운 Refresh 토큰 저장 (Refresh Token Rotation)
        saveRefreshToken(user.getId(), tokenInfo.getRefreshToken());

        log.info("토큰 갱신 완료: userId={}, email={}", user.getId(), user.getEmail());

        return tokenInfo;
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String accessToken, String email) {
        log.info("로그아웃: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Redis에서 Refresh 토큰 삭제
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();
        redisService.deleteValues(redisKey);

        // Access 토큰 블랙리스트에 추가 (남은 유효 시간만큼)
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            long expiration = jwtTokenProvider.getExpiration(accessToken);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + accessToken;
            redisService.setValues(blacklistKey, "logout", Duration.ofMillis(expiration));
        }

        log.info("로그아웃 완료: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * Refresh 토큰 Redis에 저장
     */
    private void saveRefreshToken(Long userId, String refreshToken) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        redisService.setValues(redisKey, refreshToken, REFRESH_TOKEN_TTL);
    }

    /**
     * Role별 프로필 생성
     */
    private void createProfileByRole(User user, SignupStartRequest signupData) {
        String role = user.getRoles()[0];
        switch (role) {
            case "USER" -> {
                UserProfile userProfile = UserProfile.builder()
                        .user(user)
                        .name(signupData.getName())
                        .phone(signupData.getPhoneNumber())
                        .build();
                userProfileRepository.save(userProfile);
                log.info("UserProfile 생성 완료: userId={}", user.getId());
            }
            case "COMPANY" -> {
                Company company = Company.builder()
                        .owner(user)
                        .name(signupData.getCompanyName())
                        .primaryPhone(signupData.getPhoneNumber())
                        .build();
                companyRepository.save(company);
                log.info("Company 생성 완료: userId={}", user.getId());
            }
            default -> {
                // ADMIN, BLACKLIST, DESIGNER 등은 프로필 미생성
                log.info("프로필 미생성 role: {}", role);
            }
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
