package com.hip.damoa.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.notification.service.EmailVerificationService;
import com.hip.damoa.domain.user.web.dto.SignupStartRequest;
import com.hip.damoa.infra.notification.UnifiedMessagingService;
import com.hip.damoa.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final RedisService redisService;
    private final UnifiedMessagingService unifiedMessagingService;
    private final EmailVerificationService emailVerificationService;
    private final ObjectMapper objectMapper;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    private static final String EMAIL_OTP_PREFIX = "otp:email:";
    private static final String SMS_OTP_PREFIX = "otp:sms:";
    private static final String SIGNUP_PREFIX = "signup:";
    private static final String EMAIL_VERIFIED_PREFIX = "signup:";
    private static final String SMS_VERIFIED_PREFIX = "signup:";
    private static final String EMAIL_VERIFIED_SUFFIX = ":email_verified";
    private static final String SMS_VERIFIED_SUFFIX = ":sms_verified";
    private static final String EMAIL_ATTEMPT_PREFIX = "attempt:email:";
    private static final String SMS_ATTEMPT_PREFIX = "attempt:sms:";
    private static final String PASSWORD_RESET_PREFIX = "password_reset:";
    private static final String PASSWORD_RESET_OTP_PREFIX = "password_reset_otp:";
    private static final String PASSWORD_RESET_VERIFIED_PREFIX = "password_reset_verified:";

    private static final Duration EMAIL_OTP_TTL = Duration.ofMinutes(15);
    private static final Duration SMS_OTP_TTL = Duration.ofMinutes(3);
    private static final Duration VERIFIED_FLAG_TTL = Duration.ofMinutes(30);
    private static final Duration ATTEMPT_TTL = Duration.ofHours(1);
    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom random = new SecureRandom();

    // 이메일 정규식 (RFC 5322 간소화 버전)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * 이메일 인증 코드 생성 및 발송 (토큰 기반)
     */
    public String sendEmailVerificationCode(String signupToken, String email) {
        // 1. 이메일 형식 검증
        validateEmailFormat(email);

        // 2. 토큰-이메일 매칭 검증
        validateTokenAndEmail(signupToken, email);

        // 3. 시도 횟수 체크
        checkAttempts(EMAIL_ATTEMPT_PREFIX + signupToken);

        // 4. 6자리 숫자 코드 생성
        String code = generateNumericCode(6);
        String key = EMAIL_OTP_PREFIX + signupToken;

        // 5. Redis에 저장
        redisService.setValues(key, code, EMAIL_OTP_TTL);

        // 6. 시도 횟수 증가
        incrementAttempts(EMAIL_ATTEMPT_PREFIX + signupToken);

        log.info("이메일 인증 코드 생성: token={}, email={} (TTL: {}분)", signupToken, email, EMAIL_OTP_TTL.toMinutes());

        // 7. 실제 이메일 발송 (실패 시 예외 던짐)
        sendVerificationEmail(signupToken, email, code);

        // 개발 모드에서는 코드 반환 (운영 환경에서는 제거 권장)
        if (!emailEnabled) {
            log.info("Email 비활성화 상태 - 개발 모드: code={}", code);
        }

        return code;
    }

    /**
     * 인증 이메일 발송 (UMS 사용)
     */
    private void sendVerificationEmail(String signupToken, String email, String code) {
        // 1. 템플릿 변수 준비
        Map<String, String> variables = Map.of(
                "verificationCode", code,
                "expiryMinutes", String.valueOf(EMAIL_OTP_TTL.toMinutes())
        );

        // 2. UMS를 통한 이메일 발송 (템플릿 처리, Notification 생성, 로깅 모두 자동 처리)
        try {
            unifiedMessagingService.sendEmail(
                    email,
                    "EMAIL_VERIFICATION_SIGNUP",
                    variables,
                    null,  // 회원가입 전이므로 userId 없음
                    "EMAIL_VERIFICATION"
            );

            log.info("이메일 인증 코드 발송 완료: email={}", email);
        } catch (Exception e) {
            // UMS에서 이미 상세 로그를 남겼으므로 여기서는 로그 없이 재던지기만
            throw e;
        }

        // 3. DB에 인증 레코드 생성 (비동기)
        LocalDateTime expiresAt = LocalDateTime.now().plus(EMAIL_OTP_TTL);
        emailVerificationService.createVerification(email, signupToken, code, "SIGNUP", expiresAt, null);
    }

    /**
     * SMS 인증 코드 생성 및 발송 (토큰 기반)
     */
    public String sendSmsVerificationCode(String signupToken, String phoneNumber) {
        // 시도 횟수 체크
        checkAttempts(SMS_ATTEMPT_PREFIX + signupToken);

        // 6자리 숫자 코드 생성
        String code = generateNumericCode(6);
        String key = SMS_OTP_PREFIX + signupToken;

        // Redis에 저장
        redisService.setValues(key, code, SMS_OTP_TTL);

        // 시도 횟수 증가
        incrementAttempts(SMS_ATTEMPT_PREFIX + signupToken);

        log.info("SMS 인증 코드 생성: token={}, phoneNumber={} (TTL: {}분)", signupToken, phoneNumber, SMS_OTP_TTL.toMinutes());

        // TODO: 실제 SMS 발송 로직 (현재는 로그만)
        log.info("SMS 인증 코드: {} (개발 모드)", code);

        return code; // 개발 모드에서만 반환, 실제 환경에서는 반환하지 않음
    }

    /**
     * 이메일 인증 코드 확인 (토큰 기반)
     */
    public boolean verifyEmailCode(String signupToken, String code, String email) {
        String key = EMAIL_OTP_PREFIX + signupToken;
        String storedCode = redisService.getValues(key);

        if (storedCode == null) {
            log.warn("이메일 인증 코드 만료 또는 존재하지 않음: token={}", signupToken);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(code)) {
            log.warn("이메일 인증 코드 불일치: token={}", signupToken);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 - 코드 삭제 및 플래그 저장
        redisService.deleteValues(key);

        // 인증 완료 플래그 저장
        String verifiedKey = EMAIL_VERIFIED_PREFIX + signupToken + EMAIL_VERIFIED_SUFFIX;
        redisService.setValues(verifiedKey, "true", VERIFIED_FLAG_TTL);

        // DB 인증 레코드 업데이트 (비동기)
        emailVerificationService.markAsVerified(email, code, null);

        log.info("이메일 인증 성공: token={}, email={}", signupToken, email);

        return true;
    }

    /**
     * SMS 인증 코드 확인 (토큰 기반)
     */
    public boolean verifySmsCode(String signupToken, String code) {
        String key = SMS_OTP_PREFIX + signupToken;
        String storedCode = redisService.getValues(key);

        if (storedCode == null) {
            log.warn("SMS 인증 코드 만료 또는 존재하지 않음: token={}", signupToken);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(code)) {
            log.warn("SMS 인증 코드 불일치: token={}", signupToken);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 - 코드 삭제 및 플래그 저장
        redisService.deleteValues(key);

        // 인증 완료 플래그 저장
        String verifiedKey = SMS_VERIFIED_PREFIX + signupToken + SMS_VERIFIED_SUFFIX;
        redisService.setValues(verifiedKey, "true", VERIFIED_FLAG_TTL);

        log.info("SMS 인증 성공: token={}", signupToken);

        return true;
    }

    /**
     * 이메일 인증 완료 여부 확인
     */
    public boolean isEmailVerified(String signupToken) {
        String verifiedKey = EMAIL_VERIFIED_PREFIX + signupToken + EMAIL_VERIFIED_SUFFIX;
        String verified = redisService.getValues(verifiedKey);
        return "true".equals(verified);
    }

    /**
     * SMS 인증 완료 여부 확인
     */
    public boolean isSmsVerified(String signupToken) {
        String verifiedKey = SMS_VERIFIED_PREFIX + signupToken + SMS_VERIFIED_SUFFIX;
        String verified = redisService.getValues(verifiedKey);
        return "true".equals(verified);
    }

    /**
     * 시도 횟수 체크
     */
    private void checkAttempts(String key) {
        String attempts = redisService.getValues(key);
        if (attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }

    /**
     * 시도 횟수 증가
     */
    private void incrementAttempts(String key) {
        String attempts = redisService.getValues(key);
        int count = attempts != null ? Integer.parseInt(attempts) + 1 : 1;
        redisService.setValues(key, String.valueOf(count), ATTEMPT_TTL);
    }

    /**
     * 숫자 인증 코드 생성
     */
    private String generateNumericCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 이메일 형식 검증
     */
    private void validateEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("이메일 형식 검증 실패: 빈 값");
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.warn("이메일 형식 검증 실패: {}", email);
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    /**
     * 토큰과 이메일 매칭 검증
     */
    private void validateTokenAndEmail(String signupToken, String email) {
        // Redis에서 회원가입 정보 조회
        String redisKey = SIGNUP_PREFIX + signupToken;
        String jsonData = redisService.getValues(redisKey);

        if (jsonData == null) {
            log.warn("회원가입 토큰을 찾을 수 없음: token={}", signupToken);
            throw new BusinessException(ErrorCode.SIGNUP_TOKEN_NOT_FOUND);
        }

        try {
            // JSON 역직렬화
            SignupStartRequest signupData = objectMapper.readValue(jsonData, SignupStartRequest.class);

            // 이메일 매칭 확인
            if (!signupData.getEmail().equals(email)) {
                log.warn("토큰-이메일 불일치: token={}, expected={}, actual={}",
                        signupToken, signupData.getEmail(), email);
                throw new BusinessException(ErrorCode.TOKEN_EMAIL_MISMATCH);
            }

            log.info("토큰-이메일 검증 성공: token={}, email={}", signupToken, email);
        } catch (BusinessException e) {
            // BusinessException은 그대로 재던지기
            throw e;
        } catch (Exception e) {
            // 기타 예외는 내부 서버 오류로 변환
            log.error("회원가입 데이터 역직렬화 실패: token={}", signupToken, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 비밀번호 재설정 인증 코드 발송 (토큰 기반)
     */
    public String sendPasswordResetCode(String resetToken, String email) {
        // 1. 이메일 형식 검증
        validateEmailFormat(email);

        // 2. 토큰-이메일 매칭 검증
        validateResetTokenAndEmail(resetToken, email);

        // 3. 시도 횟수 체크
        checkAttempts(EMAIL_ATTEMPT_PREFIX + resetToken);

        // 4. 6자리 숫자 코드 생성
        String code = generateNumericCode(6);
        String key = PASSWORD_RESET_OTP_PREFIX + resetToken;

        // 5. Redis에 저장
        redisService.setValues(key, code, EMAIL_OTP_TTL);

        // 6. 시도 횟수 증가
        incrementAttempts(EMAIL_ATTEMPT_PREFIX + resetToken);

        log.info("비밀번호 재설정 인증 코드 생성: token={}, email={} (TTL: {}분)", resetToken, email, EMAIL_OTP_TTL.toMinutes());

        // 7. 실제 이메일 발송
        sendPasswordResetEmail(email, code);

        // 개발 모드에서는 코드 반환
        if (!emailEnabled) {
            log.warn("Email 비활성화 상태 - 개발 모드: code={}", code);
        }

        return code;
    }

    /**
     * 비밀번호 재설정 이메일 발송
     */
    private void sendPasswordResetEmail(String email, String code) {
        // 1. 템플릿 변수 준비
        Map<String, String> variables = Map.of(
                "verificationCode", code,
                "expiryMinutes", String.valueOf(EMAIL_OTP_TTL.toMinutes())
        );

        // 2. UMS를 통한 이메일 발송
        try {
            unifiedMessagingService.sendEmail(
                    email,
                    "PASSWORD_RESET",  // 템플릿 코드 (DB에 등록 필요)
                    variables,
                    null,  // 비밀번호 찾기이므로 userId 없음
                    "PASSWORD_RESET"
            );

            log.info("비밀번호 재설정 이메일 발송 완료: email={}", email);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 비밀번호 재설정 인증 코드 확인 (토큰 기반)
     */
    public boolean verifyPasswordResetCode(String resetToken, String code) {
        String key = PASSWORD_RESET_OTP_PREFIX + resetToken;
        String storedCode = redisService.getValues(key);

        if (storedCode == null) {
            log.warn("비밀번호 재설정 인증 코드 만료 또는 존재하지 않음: token={}", resetToken);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(code)) {
            log.warn("비밀번호 재설정 인증 코드 불일치: token={}", resetToken);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 - 코드 삭제 및 플래그 저장
        redisService.deleteValues(key);

        // 인증 완료 플래그 저장 (30분 유효)
        String verifiedKey = PASSWORD_RESET_VERIFIED_PREFIX + resetToken;
        redisService.setValues(verifiedKey, "true", VERIFIED_FLAG_TTL);

        log.info("비밀번호 재설정 인증 성공: token={}", resetToken);

        return true;
    }

    /**
     * 비밀번호 재설정 인증 완료 여부 확인 (토큰 기반)
     */
    public boolean isPasswordResetVerified(String resetToken) {
        String verifiedKey = PASSWORD_RESET_VERIFIED_PREFIX + resetToken;
        String verified = redisService.getValues(verifiedKey);
        return "true".equals(verified);
    }

    /**
     * 비밀번호 재설정 인증 플래그 삭제 (재설정 완료 후, 토큰 기반)
     */
    public void deletePasswordResetVerifiedFlag(String resetToken) {
        String verifiedKey = PASSWORD_RESET_VERIFIED_PREFIX + resetToken;
        redisService.deleteValues(verifiedKey);
        log.info("비밀번호 재설정 인증 플래그 삭제: token={}", resetToken);
    }

    /**
     * 토큰과 이메일 매칭 검증 (비밀번호 재설정용)
     */
    private void validateResetTokenAndEmail(String resetToken, String email) {
        // Redis에서 비밀번호 재설정 정보 조회
        String redisKey = PASSWORD_RESET_PREFIX + resetToken;
        String storedEmail = redisService.getValues(redisKey);

        if (storedEmail == null) {
            log.warn("비밀번호 재설정 토큰을 찾을 수 없음: token={}", resetToken);
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND);
        }

        // 이메일 매칭 확인
        if (!storedEmail.equals(email)) {
            log.warn("토큰-이메일 불일치: token={}, expected={}, actual={}", resetToken, storedEmail, email);
            throw new BusinessException(ErrorCode.TOKEN_EMAIL_MISMATCH);
        }

        log.info("토큰-이메일 검증 성공: token={}, email={}", resetToken, email);
    }
}
