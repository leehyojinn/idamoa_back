package com.hip.damoa.domain.user.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.jwt.JwtTokenProvider;
import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.user.model.OAuthProvider;
import com.hip.damoa.domain.user.model.SocialAccount;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.SocialAccountRepository;
import com.hip.damoa.domain.user.repository.UserRepository;
import com.hip.damoa.domain.user.web.dto.OAuthCallbackResponse;
import com.hip.damoa.domain.user.web.dto.OAuthLoginResponse;
import com.hip.damoa.infra.oauth.OAuthTokenResponse;
import com.hip.damoa.infra.oauth.OAuthUserInfo;
import com.hip.damoa.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 로그인 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    private final Map<String, com.hip.damoa.infra.oauth.OAuthProvider> oauthProviders;

    @Value("${oauth.redirect-base-url}")
    private String redirectBaseUrl;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final Duration OAUTH_STATE_TTL = Duration.ofMinutes(10);
    private static final String OAUTH_CODE_PREFIX = "oauth:code:";
    private static final Duration OAUTH_CODE_TTL = Duration.ofSeconds(30);

    /**
     * OAuth 인가 URL 생성
     */
    public OAuthLoginResponse getAuthorizationUrl(String providerName) {
        log.info("OAuth 인가 URL 생성: provider={}", providerName);

        com.hip.damoa.infra.oauth.OAuthProvider provider = getOAuthProvider(providerName);

        // State 생성 및 Redis에 저장 (CSRF 방지)
        String state = UUID.randomUUID().toString();
        String stateKey = OAUTH_STATE_PREFIX + state;
        redisService.setValues(stateKey, providerName, OAUTH_STATE_TTL);

        // Redirect URI 생성
        String redirectUri = redirectBaseUrl + "/api/oauth/" + providerName.toLowerCase() + "/callback";

        // 인가 URL 생성
        String authorizationUrl = provider.getAuthorizationUrl(state, redirectUri);

        log.info("OAuth 인가 URL 생성 완료: provider={}, redirectUri={}", providerName, redirectUri);

        return OAuthLoginResponse.builder()
                .authorizationUrl(authorizationUrl)
                .message(providerName + " 로그인을 진행해주세요")
                .build();
    }

    /**
     * OAuth Callback 처리 (로그인 또는 회원가입)
     */
    @Transactional
    public OAuthCallbackResponse handleCallback(String providerName, String code, String state) {
        log.info("OAuth Callback 처리 시작: provider={}, state={}", providerName, state);

        // State 검증 (CSRF 방지)
        validateState(state, providerName);

        // OAuth Provider 가져오기
        com.hip.damoa.infra.oauth.OAuthProvider provider = getOAuthProvider(providerName);

        // Redirect URI 생성
        String redirectUri = redirectBaseUrl + "/api/oauth/" + providerName.toLowerCase() + "/callback";

        // Access Token 획득
        OAuthTokenResponse tokenResponse = provider.getAccessToken(code, redirectUri);
        log.info("OAuth Access Token 획득 완료: provider={}", providerName);

        // 사용자 정보 조회
        OAuthUserInfo userInfo = provider.getUserInfo(tokenResponse.getAccessToken());
        log.info("OAuth 사용자 정보 조회 완료: provider={}, email={}", providerName, userInfo.getEmail());

        // 이메일이 없는 경우 에러
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_NOT_PROVIDED);
        }

        // Provider ID로 SocialAccount 조회
        OAuthProvider oauthProvider = OAuthProvider.valueOf(providerName.toUpperCase());
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(oauthProvider, userInfo.getProviderId());

        User user;
        boolean isNewUser;

        if (existingSocialAccount.isPresent()) {
            // 기존 소셜 계정이 있는 경우 - 로그인
            user = existingSocialAccount.get().getUser();
            isNewUser = false;
            log.info("기존 소셜 계정 로그인: userId={}, provider={}", user.getId(), providerName);

            // 소셜 계정 정보 업데이트
            SocialAccount socialAccount = existingSocialAccount.get();
            socialAccount.updateProfile(userInfo.getEmail(), userInfo.getName(), buildProfileData(userInfo));
            socialAccount.updateTokens(
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn())
            );
            socialAccountRepository.save(socialAccount);

        } else {
            // 신규 소셜 계정 - 이메일로 기존 User 확인
            Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

            if (existingUser.isPresent()) {
                // 보안: 기존 이메일 계정이 있는 경우 자동 연동 금지
                // 다른 사람의 계정에 소셜 계정이 연동되는 것을 방지
                log.warn("소셜 로그인 실패 - 이미 가입된 이메일: email={}, provider={}", userInfo.getEmail(), providerName);
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);

            } else {
                // 완전히 신규 사용자 - User 생성
                user = User.builder()
                        .email(userInfo.getEmail())
                        .password(null)  // 소셜 로그인 전용이므로 비밀번호 없음
                        .roles(new String[]{"USER"})
                        .emailVerified(true)  // 소셜 로그인은 이메일 인증 완료로 간주
                        .emailVerifiedAt(LocalDateTime.now())
                        .termsAgreed(true)  // 소셜 로그인 시 약관 동의로 간주
                        .termsAgreedAt(LocalDateTime.now())
                        .privacyAgreed(true)
                        .privacyAgreedAt(LocalDateTime.now())
                        .profileCompleted(false)  // 추가 프로필 입력 필요
                        .build();

                user = userRepository.save(user);
                isNewUser = true;
                log.info("신규 사용자 생성: userId={}, email={}, provider={}", user.getId(), user.getEmail(), providerName);
            }

            // SocialAccount 생성
            SocialAccount newSocialAccount = SocialAccount.builder()
                    .user(user)
                    .provider(oauthProvider)
                    .providerUserId(userInfo.getProviderId())
                    .providerEmail(userInfo.getEmail())
                    .providerName(userInfo.getName())
                    .profileData(buildProfileData(userInfo))
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()))
                    .linkedAt(LocalDateTime.now())
                    .build();

            socialAccountRepository.save(newSocialAccount);
            log.info("소셜 계정 생성 완료: provider={}, providerUserId={}", providerName, userInfo.getProviderId());
        }

        // JWT 토큰 생성
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);

        // Refresh 토큰 Redis에 저장
        saveRefreshToken(user.getId(), tokenInfo.getRefreshToken());

        log.info("OAuth 로그인 완료: userId={}, provider={}, isNewUser={}", user.getId(), providerName, isNewUser);

        return OAuthCallbackResponse.builder()
                .isNewUser(isNewUser)
                .tokenInfo(tokenInfo)
                .provider(providerName)
                .providerEmail(userInfo.getEmail())
                .build();
    }

    /**
     * OAuth Provider 가져오기
     */
    private com.hip.damoa.infra.oauth.OAuthProvider getOAuthProvider(String providerName) {
        com.hip.damoa.infra.oauth.OAuthProvider provider = oauthProviders.get(providerName.toLowerCase() + "OAuthProvider");
        if (provider == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        }
        return provider;
    }

    /**
     * State 검증
     */
    private void validateState(String state, String providerName) {
        String stateKey = OAUTH_STATE_PREFIX + state;
        String storedProvider = redisService.getValues(stateKey);

        if (storedProvider == null) {
            log.warn("OAuth State 만료 또는 존재하지 않음: state={}", state);
            throw new BusinessException(ErrorCode.OAUTH_STATE_INVALID);
        }

        if (!storedProvider.equalsIgnoreCase(providerName)) {
            log.warn("OAuth State Provider 불일치: expected={}, actual={}", storedProvider, providerName);
            throw new BusinessException(ErrorCode.OAUTH_STATE_INVALID);
        }

        // State 사용 후 삭제
        redisService.deleteValues(stateKey);
    }

    /**
     * Profile Data 생성
     */
    private Map<String, Object> buildProfileData(OAuthUserInfo userInfo) {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("providerId", userInfo.getProviderId());
        profileData.put("email", userInfo.getEmail());
        profileData.put("name", userInfo.getName());
        profileData.put("profileImageUrl", userInfo.getProfileImageUrl());
        return profileData;
    }

    /**
     * Refresh 토큰 Redis에 저장
     */
    private void saveRefreshToken(Long userId, String refreshToken) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        redisService.setValues(redisKey, refreshToken, REFRESH_TOKEN_TTL);
    }

    /**
     * OAuth 임시 코드 생성 및 Redis에 토큰 정보 저장
     * @param callbackResponse OAuth 콜백 응답 (토큰 정보 포함)
     * @return 임시 코드 (30초 TTL)
     */
    public String generateOAuthCode(OAuthCallbackResponse callbackResponse) {
        String code = UUID.randomUUID().toString();
        String codeKey = OAUTH_CODE_PREFIX + code;

        // TokenInfo를 JSON 문자열로 저장
        String tokenData = String.format(
                "%s|%s|%b|%s|%b|%s|%s",
                callbackResponse.getTokenInfo().getAccessToken(),
                callbackResponse.getTokenInfo().getRefreshToken(),
                callbackResponse.getTokenInfo().isProfileCompleted(),
                callbackResponse.getTokenInfo().getCurrentRole(),
                callbackResponse.isNewUser(),
                callbackResponse.getProvider(),
                callbackResponse.getProviderEmail()
        );

        redisService.setValues(codeKey, tokenData, OAUTH_CODE_TTL);
        log.info("OAuth 임시 코드 생성: code={}, TTL=30초", code);

        return code;
    }

    /**
     * OAuth 임시 코드를 토큰으로 교환
     * @param code 임시 코드
     * @return OAuth 콜백 응답 (토큰 정보 포함)
     */
    public OAuthCallbackResponse exchangeCodeForToken(String code) {
        String codeKey = OAUTH_CODE_PREFIX + code;
        String tokenData = redisService.getValues(codeKey);

        if (tokenData == null) {
            log.warn("OAuth 임시 코드 만료 또는 존재하지 않음: code={}", code);
            throw new BusinessException(ErrorCode.OAUTH_CODE_INVALID);
        }

        // 코드 사용 후 즉시 삭제 (1회용)
        redisService.deleteValues(codeKey);

        // 토큰 데이터 파싱
        String[] parts = tokenData.split("\\|");
        if (parts.length < 7) {
            log.error("OAuth 토큰 데이터 파싱 실패: data={}", tokenData);
            throw new BusinessException(ErrorCode.OAUTH_CODE_INVALID);
        }

        TokenInfo tokenInfo = TokenInfo.builder()
                .accessToken(parts[0])
                .refreshToken(parts[1])
                .profileCompleted(Boolean.parseBoolean(parts[2]))
                .currentRole(parts[3])
                .build();

        log.info("OAuth 임시 코드 교환 완료: code={}", code);

        return OAuthCallbackResponse.builder()
                .isNewUser(Boolean.parseBoolean(parts[4]))
                .tokenInfo(tokenInfo)
                .provider(parts[5])
                .providerEmail(parts[6])
                .build();
    }
}
