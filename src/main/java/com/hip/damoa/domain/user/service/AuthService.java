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
import com.hip.damoa.infra.notification.UnifiedMessagingService;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
    private final UnifiedMessagingService unifiedMessagingService;
    private final ObjectMapper objectMapper;

    private static final String SIGNUP_PREFIX = "signup:";
    private static final String PASSWORD_RESET_PREFIX = "password_reset:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";
    private static final Duration SIGNUP_TTL = Duration.ofMinutes(10);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
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
     * - 이메일 인증 확인
     * - Redis에서 회원가입 정보 조회
     * - User 생성 (프로필은 별도 단계에서 생성)
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

        // 이메일 인증 완료 여부 확인 (필수)
        if (!verificationService.isEmailVerified(request.getSignupToken())) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        // SMS 인증은 선택사항 (나중에 인증 가능)
        boolean smsVerified = verificationService.isSmsVerified(request.getSignupToken());

        LocalDateTime now = LocalDateTime.now();

        // User 생성 (role은 항상 USER로 고정, ADMIN은 관리자 패널에서만 부여)
        User user = User.builder()
                .email(signupData.getEmail())
                .password(passwordEncoder.encode(signupData.getPassword()))
                .roles(new String[]{"USER"})  // 강제로 USER 고정
                .emailVerified(true)
                .emailVerifiedAt(now)
                .phoneVerified(smsVerified)
                .phoneVerifiedAt(smsVerified ? now : null)
                .termsAgreed(signupData.getTermsAgreed())
                .termsAgreedAt(signupData.getTermsAgreed() ? now : null)
                .privacyAgreed(signupData.getPrivacyAgreed())
                .privacyAgreedAt(signupData.getPrivacyAgreed() ? now : null)
                .marketingAgreed(signupData.getMarketingAgreed() != null ? signupData.getMarketingAgreed() : false)
                .marketingAgreedAt(Boolean.TRUE.equals(signupData.getMarketingAgreed()) ? now : null)
                .profileCompleted(false)  // 프로필 미완성 상태
                .build();

        user = userRepository.save(user);
        log.info("사용자 생성 완료: id={}, email={}, profileCompleted={}", user.getId(), user.getEmail(), user.getProfileCompleted());

        // Redis에서 회원가입 토큰 삭제
        redisService.deleteValues(redisKey);

        // JWT 토큰 발급 (프로필 상태 포함)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);

        // Refresh 토큰 Redis에 저장
        saveRefreshToken(user.getId(), tokenInfo.getRefreshToken());

        log.info("회원가입 완료: userId={}, email={}, profileCompleted={}, currentRole={}",
                user.getId(), user.getEmail(), tokenInfo.isProfileCompleted(), tokenInfo.getCurrentRole());

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
        user = userRepository.save(user);

        // JWT 토큰 발급 (프로필 상태 포함)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);

        // Refresh 토큰 Redis에 저장
        saveRefreshToken(user.getId(), tokenInfo.getRefreshToken());

        log.info("로그인 성공: userId={}, email={}, ip={}, profileCompleted={}",
                user.getId(), user.getEmail(), ipAddress, user.getProfileCompleted());

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

        // 새로운 토큰 발급 (프로필 상태 포함)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);

        // 새로운 Refresh 토큰 저장 (Refresh Token Rotation)
        saveRefreshToken(user.getId(), tokenInfo.getRefreshToken());

        log.info("토큰 갱신 완료: userId={}, email={}, profileCompleted={}",
                user.getId(), user.getEmail(), user.getProfileCompleted());

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

    /**
     * 비밀번호 재설정 시작 (1단계)
     * - 사용자 존재 확인
     * - Redis에 이메일 저장
     * - UUID 토큰 생성 및 반환
     */
    @Transactional(readOnly = true)
    public PasswordResetStartResponse startPasswordReset(String email) {
        log.info("비밀번호 재설정 시작: email={}", email);

        // 사용자 존재 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // UUID 토큰 생성
        String resetToken = UUID.randomUUID().toString();
        String redisKey = PASSWORD_RESET_PREFIX + resetToken;

        // Redis에 이메일 저장
        redisService.setValues(redisKey, email, PASSWORD_RESET_TTL);
        log.info("비밀번호 재설정 토큰 생성 완료: token={}, email={}", resetToken, email);

        return PasswordResetStartResponse.builder()
                .resetToken(resetToken)
                .message("이메일 인증을 진행해주세요")
                .expiresIn(PASSWORD_RESET_TTL.getSeconds())
                .build();
    }

    /**
     * 비밀번호 재설정 인증 코드 확인 (2단계)
     */
    public void verifyPasswordResetCode(String resetToken, String code) {
        log.info("비밀번호 재설정 인증 코드 확인: token={}", resetToken);

        // 인증 코드 확인 (VerificationService에서 플래그 저장까지 처리)
        verificationService.verifyPasswordResetCode(resetToken, code);

        log.info("비밀번호 재설정 인증 코드 확인 완료: token={}", resetToken);
    }

    /**
     * 비밀번호 재설정 완료 (3단계)
     */
    @Transactional
    public void completePasswordReset(String resetToken, String newPassword) {
        log.info("비밀번호 재설정 완료 시작: token={}", resetToken);

        // 1. 인증 완료 여부 확인
        if (!verificationService.isPasswordResetVerified(resetToken)) {
            log.warn("비밀번호 재설정 인증 미완료: token={}", resetToken);
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 2. Redis에서 이메일 조회
        String redisKey = PASSWORD_RESET_PREFIX + resetToken;
        String email = redisService.getValues(redisKey);

        if (email == null) {
            log.warn("비밀번호 재설정 토큰 만료: token={}", resetToken);
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        // 3. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        // 5. Redis에서 토큰 및 인증 플래그 삭제
        redisService.deleteValues(redisKey);
        verificationService.deletePasswordResetVerifiedFlag(resetToken);

        log.info("비밀번호 재설정 완료: email={}, token={}", email, resetToken);
    }

    /**
     * 비밀번호 변경 (로그인 상태)
     */
    @Transactional
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        log.info("비밀번호 변경 시작: email={}", userEmail);

        // 1. 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("비밀번호 변경 실패 - 현재 비밀번호 불일치: email={}", userEmail);
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 새 비밀번호와 현재 비밀번호가 같은지 확인
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("비밀번호 변경 실패 - 새 비밀번호가 현재 비밀번호와 동일: email={}", userEmail);
            throw new BusinessException(ErrorCode.SAME_PASSWORD);
        }

        // 4. 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        log.info("비밀번호 변경 완료: email={}", userEmail);
    }

    /**
     * 비밀번호 변경 알림 이메일 발송
     */
    private void sendPasswordChangedEmail(String email, Long userId, String changeMethod) {
        try {
            String changedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            Map<String, String> variables = Map.of(
                    "changedAt", changedAt,
                    "changeMethod", changeMethod
            );

            unifiedMessagingService.sendEmail(
                    email,
                    "PASSWORD_CHANGED",
                    variables,
                    userId,
                    "PASSWORD_CHANGE"
            );

            log.info("비밀번호 변경 알림 이메일 발송 완료: email={}, changeMethod={}", email, changeMethod);
        } catch (Exception e) {
            log.error("비밀번호 변경 알림 이메일 발송 실패: email={}", email, e);
            // 이메일 발송 실패는 비밀번호 변경 자체에 영향을 주지 않도록 로그만 기록
        }
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUserInfo(String email) {
        log.info("사용자 정보 조회: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserInfoResponse.from(user);
    }
}
