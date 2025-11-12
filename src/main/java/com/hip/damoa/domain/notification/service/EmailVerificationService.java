package com.hip.damoa.domain.notification.service;

import com.hip.damoa.domain.notification.model.EmailVerification;
import com.hip.damoa.domain.notification.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이메일 인증 기록 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;

    /**
     * 인증 레코드 생성 (비동기)
     *
     * @param email             이메일
     * @param verificationToken Redis signupToken
     * @param verificationCode  인증 코드
     * @param purpose           인증 목적 (SIGNUP, PASSWORD_RESET, EMAIL_CHANGE)
     * @param expiresAt         만료 시간
     * @param requestIp         요청 IP
     */
    @Async
    @Transactional
    public void createVerification(String email, String verificationToken, String verificationCode,
                                    String purpose, LocalDateTime expiresAt, String requestIp) {
        try {
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .verificationToken(verificationToken)
                    .verificationCode(verificationCode)
                    .purpose(purpose)
                    .expiresAt(expiresAt)
                    .requestIp(requestIp)
                    .build();

            verificationRepository.save(verification);
            log.info("이메일 인증 레코드 생성: email={}, purpose={}, token={}", email, purpose, verificationToken);
        } catch (Exception e) {
            log.error("이메일 인증 레코드 생성 실패: email={}, purpose={}", email, purpose, e);
        }
    }

    /**
     * 인증 완료 처리 (비동기)
     *
     * @param email        이메일
     * @param code         인증 코드
     * @param verifiedIp   인증 IP
     */
    @Async
    @Transactional
    public void markAsVerified(String email, String code, String verifiedIp) {
        try {
            EmailVerification verification = verificationRepository
                    .findByEmailAndVerificationCodeAndStatus(email, code, "PENDING")
                    .orElse(null);

            if (verification != null) {
                verification.markAsVerified(verifiedIp);
                verificationRepository.save(verification);
                log.info("이메일 인증 완료: email={}, code={}", email, code);
            } else {
                log.warn("이메일 인증 레코드를 찾을 수 없습니다: email={}, code={}", email, code);
            }
        } catch (Exception e) {
            log.error("이메일 인증 완료 처리 실패: email={}, code={}", email, code, e);
        }
    }

    /**
     * 만료된 인증 레코드 정리 (매일 새벽 2시 실행)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpired() {
        try {
            LocalDateTime now = LocalDateTime.now();
            long deletedCount = verificationRepository.deleteByExpiresAtBefore(now);
            log.info("만료된 이메일 인증 레코드 정리 완료: 삭제된 레코드 수={}", deletedCount);
        } catch (Exception e) {
            log.error("만료된 이메일 인증 레코드 정리 실패", e);
        }
    }
}
