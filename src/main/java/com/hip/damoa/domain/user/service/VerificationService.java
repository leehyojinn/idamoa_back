package com.hip.damoa.domain.user.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.model.NotificationTemplate;
import com.hip.damoa.domain.notification.repository.NotificationRepository;
import com.hip.damoa.domain.notification.service.EmailVerificationService;
import com.hip.damoa.domain.notification.service.NotificationLogService;
import com.hip.damoa.domain.notification.service.TemplateService;
import com.hip.damoa.domain.notification.service.TemplateService.RenderedTemplate;
import com.hip.damoa.infra.notification.GmailService;
import com.hip.damoa.infra.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class VerificationService {

    private final RedisService redisService;
    private final TemplateService templateService;
    private final NotificationLogService notificationLogService;
    private final EmailVerificationService emailVerificationService;
    private final NotificationRepository notificationRepository;

    // Gmail API 서비스 (선택적)
    private GmailService gmailService;

    @Value("${notification.email.gmail-api.enabled:false}")
    private boolean gmailApiEnabled;

    public VerificationService(
            RedisService redisService,
            TemplateService templateService,
            NotificationLogService notificationLogService,
            EmailVerificationService emailVerificationService,
            NotificationRepository notificationRepository) {
        this.redisService = redisService;
        this.templateService = templateService;
        this.notificationLogService = notificationLogService;
        this.emailVerificationService = emailVerificationService;
        this.notificationRepository = notificationRepository;
    }

    @Autowired(required = false)
    public void setGmailService(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    private static final String EMAIL_OTP_PREFIX = "otp:email:";
    private static final String SMS_OTP_PREFIX = "otp:sms:";
    private static final String EMAIL_VERIFIED_PREFIX = "signup:";
    private static final String SMS_VERIFIED_PREFIX = "signup:";
    private static final String EMAIL_VERIFIED_SUFFIX = ":email_verified";
    private static final String SMS_VERIFIED_SUFFIX = ":sms_verified";
    private static final String EMAIL_ATTEMPT_PREFIX = "attempt:email:";
    private static final String SMS_ATTEMPT_PREFIX = "attempt:sms:";

    private static final Duration EMAIL_OTP_TTL = Duration.ofMinutes(15);
    private static final Duration SMS_OTP_TTL = Duration.ofMinutes(3);
    private static final Duration VERIFIED_FLAG_TTL = Duration.ofMinutes(30);
    private static final Duration ATTEMPT_TTL = Duration.ofHours(1);
    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom random = new SecureRandom();

    /**
     * 이메일 인증 코드 생성 및 발송 (토큰 기반)
     */
    public String sendEmailVerificationCode(String signupToken, String email) {
        // 시도 횟수 체크
        checkAttempts(EMAIL_ATTEMPT_PREFIX + signupToken);

        // 6자리 숫자 코드 생성
        String code = generateNumericCode(6);
        String key = EMAIL_OTP_PREFIX + signupToken;

        // Redis에 저장
        redisService.setValues(key, code, EMAIL_OTP_TTL);

        // 시도 횟수 증가
        incrementAttempts(EMAIL_ATTEMPT_PREFIX + signupToken);

        log.info("이메일 인증 코드 생성: token={}, email={} (TTL: {}분)", signupToken, email, EMAIL_OTP_TTL.toMinutes());

        // 실제 이메일 발송
        try {
            sendVerificationEmail(signupToken, email, code);
        } catch (Exception e) {
            log.error("이메일 발송 실패: email={}, error={}", email, e.getMessage(), e);
            // 발송 실패해도 코드는 Redis에 저장되어 있으므로 예외를 던지지 않음
        }

        // 개발 모드에서는 코드 반환 (운영 환경에서는 제거 권장)
        if (!gmailApiEnabled) {
            log.warn("Gmail API 비활성화 상태 - 개발 모드: code={}", code);
        }

        return code;
    }

    /**
     * 인증 이메일 발송 (템플릿 기반)
     */
    private void sendVerificationEmail(String signupToken, String email, String code) {
        // 1. 템플릿 조회
        NotificationTemplate template = templateService.getTemplate("EMAIL_VERIFICATION_SIGNUP", "EMAIL");

        // 2. 템플릿 변수 치환
        Map<String, String> variables = Map.of(
                "verificationCode", code,
                "expiryMinutes", String.valueOf(EMAIL_OTP_TTL.toMinutes())
        );
        RenderedTemplate rendered = templateService.renderTemplate(template, variables);

        // 3. Notification 엔티티 생성 (로그용)
        Notification notification = Notification.builder()
                .userId(null)  // 회원가입 전이므로 userId 없음
                .notificationType("EMAIL_VERIFICATION")
                .channel("EMAIL")
                .title(rendered.title())
                .content(rendered.content())
                .templateId(template.getId())
                .templateData(variables)
                .isSent(false)
                .build();
        notification = notificationRepository.save(notification);

        // 4. Gmail API로 이메일 발송
        try {
            if (gmailService == null) {
                throw new IllegalStateException("Gmail API가 활성화되지 않았습니다. application.yml에서 notification.email.gmail-api.enabled=true로 설정하세요.");
            }

            String messageId = gmailService.sendHtmlEmail(email, rendered.title(), rendered.content());

            // 발송 성공 - Notification 및 로그 업데이트
            notification.markAsSent();
            notificationRepository.save(notification);
            notificationLogService.logEmailSent(notification, email, "GMAIL_API", messageId, null);

            log.info("이메일 인증 코드 발송 완료: email={}, messageId={}", email, messageId);
        } catch (Exception e) {
            // 발송 실패 - 로그 기록
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
            notificationLogService.logEmailFailed(notification, email, "GMAIL_API", "SEND_FAILED", e.getMessage());

            throw e;
        }

        // 5. DB에 인증 레코드 생성 (비동기)
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
}
