package com.hip.damoa.domain.user.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final RedisService redisService;

    private static final String EMAIL_OTP_PREFIX = "otp:email:";
    private static final String SMS_OTP_PREFIX = "otp:sms:";
    private static final String EMAIL_ATTEMPT_PREFIX = "attempt:email:";
    private static final String SMS_ATTEMPT_PREFIX = "attempt:sms:";

    private static final Duration EMAIL_OTP_TTL = Duration.ofMinutes(15);
    private static final Duration SMS_OTP_TTL = Duration.ofMinutes(3);
    private static final Duration ATTEMPT_TTL = Duration.ofHours(1);
    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom random = new SecureRandom();

    /**
     * 이메일 인증 코드 생성 및 발송
     */
    public String sendEmailVerificationCode(String email) {
        // 시도 횟수 체크
        checkAttempts(EMAIL_ATTEMPT_PREFIX + email);

        // 6자리 숫자 코드 생성
        String code = generateNumericCode(6);
        String key = EMAIL_OTP_PREFIX + email;

        // Redis에 저장
        redisService.setValues(key, code, EMAIL_OTP_TTL);

        // 시도 횟수 증가
        incrementAttempts(EMAIL_ATTEMPT_PREFIX + email);

        log.info("이메일 인증 코드 생성: {} (TTL: {}분)", email, EMAIL_OTP_TTL.toMinutes());

        // TODO: 실제 이메일 발송 로직 (현재는 로그만)
        log.info("이메일 인증 코드: {} (개발 모드)", code);

        return code; // 개발 모드에서만 반환, 실제 환경에서는 반환하지 않음
    }

    /**
     * SMS 인증 코드 생성 및 발송
     */
    public String sendSmsVerificationCode(String phoneNumber) {
        // 시도 횟수 체크
        checkAttempts(SMS_ATTEMPT_PREFIX + phoneNumber);

        // 6자리 숫자 코드 생성
        String code = generateNumericCode(6);
        String key = SMS_OTP_PREFIX + phoneNumber;

        // Redis에 저장
        redisService.setValues(key, code, SMS_OTP_TTL);

        // 시도 횟수 증가
        incrementAttempts(SMS_ATTEMPT_PREFIX + phoneNumber);

        log.info("SMS 인증 코드 생성: {} (TTL: {}분)", phoneNumber, SMS_OTP_TTL.toMinutes());

        // TODO: 실제 SMS 발송 로직 (현재는 로그만)
        log.info("SMS 인증 코드: {} (개발 모드)", code);

        return code; // 개발 모드에서만 반환, 실제 환경에서는 반환하지 않음
    }

    /**
     * 이메일 인증 코드 확인
     */
    public boolean verifyEmailCode(String email, String code) {
        String key = EMAIL_OTP_PREFIX + email;
        String storedCode = redisService.getValues(key);

        if (storedCode == null) {
            log.warn("이메일 인증 코드 만료 또는 존재하지 않음: {}", email);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(code)) {
            log.warn("이메일 인증 코드 불일치: {}", email);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 - 코드 삭제
        redisService.deleteValues(key);
        log.info("이메일 인증 성공: {}", email);

        return true;
    }

    /**
     * SMS 인증 코드 확인
     */
    public boolean verifySmsCode(String phoneNumber, String code) {
        String key = SMS_OTP_PREFIX + phoneNumber;
        String storedCode = redisService.getValues(key);

        if (storedCode == null) {
            log.warn("SMS 인증 코드 만료 또는 존재하지 않음: {}", phoneNumber);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(code)) {
            log.warn("SMS 인증 코드 불일치: {}", phoneNumber);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 - 코드 삭제
        redisService.deleteValues(key);
        log.info("SMS 인증 성공: {}", phoneNumber);

        return true;
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
}
