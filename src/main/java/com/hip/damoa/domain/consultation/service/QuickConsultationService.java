package com.hip.damoa.domain.consultation.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
import com.hip.damoa.domain.consultation.repository.QuickConsultationRepository;
import com.hip.damoa.domain.consultation.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 빠른상담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuickConsultationService {

    private final QuickConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    /**
     * 상담 신청 (비회원/회원 모두 가능)
     *
     * @param userEmail 로그인한 사용자 이메일 (비회원인 경우 null)
     * @param request   상담 신청 요청
     * @param httpRequest HTTP 요청 (IP, User-Agent 등 메타데이터 추출용)
     * @return 생성된 상담
     */
    @Transactional
    public QuickConsultation createConsultation(
            String userEmail,
            QuickConsultationCreateRequest request,
            HttpServletRequest httpRequest) {

        log.info("빠른상담 신청 시작: userEmail={}, name={}", userEmail, request.getName());

        // 회원인 경우 User 조회
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
            log.info("회원 상담 신청: userId={}", user != null ? user.getId() : null);
        } else {
            log.info("비회원 상담 신청");
        }

        // 필수 동의 확인
        if (!Boolean.TRUE.equals(request.getPersonalInfoConsent()) ||
            !Boolean.TRUE.equals(request.getThirdPartyConsent()) ||
            !Boolean.TRUE.equals(request.getTermsOfServiceConsent())) {
            throw new BusinessException(ErrorCode.CONSULTATION_CONSENT_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();

        // QuickConsultation 엔티티 생성
        QuickConsultation consultation = QuickConsultation.builder()
                .user(user)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(request.getPassword()) // 비밀번호 (4자리 평문)
                .subject(request.getSubject())
                .message(request.getMessage())
                .preferredContactMethod(request.getPreferredContactMethod())
                .preferredContactTime(request.getPreferredContactTime())
                // 동의 정보
                .personalInfoConsent(request.getPersonalInfoConsent())
                .personalInfoConsentAt(request.getPersonalInfoConsent() ? now : null)
                .thirdPartyConsent(request.getThirdPartyConsent())
                .thirdPartyConsentAt(request.getThirdPartyConsent() ? now : null)
                .termsOfServiceConsent(request.getTermsOfServiceConsent())
                .termsOfServiceConsentAt(request.getTermsOfServiceConsent() ? now : null)
                .marketingConsent(Boolean.TRUE.equals(request.getMarketingConsent()))
                .marketingConsentAt(Boolean.TRUE.equals(request.getMarketingConsent()) ? now : null)
                .consentIpAddress(getClientIp(httpRequest))
                .consentVersion(request.getConsentVersion())
                // 메타데이터
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .referrer(httpRequest.getHeader("Referer"))
                .status(ConsultationStatus.SUBMITTED)
                .build();

        consultation = consultationRepository.save(consultation);

        log.info("빠른상담 신청 완료: consultationUuid={}, isMember={}",
                consultation.getUuid(), consultation.isMember());

        return consultation;
    }

    /**
     * 상담 상세 조회 (통합)
     * - 로그인한 회원: 본인 상담이면 비밀번호 불필요
     * - 비회원: 비밀번호 필수
     */
    @Transactional(readOnly = true)
    public QuickConsultation getConsultationDetail(
            UUID consultationUuid,
            String userEmail,
            String password) {

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        // 회원인 경우 본인 확인
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null && consultation.isOwnedBy(user.getId())) {
                // 본인 상담이면 비밀번호 불필요
                return consultation;
            }
        }

        // 비회원이거나 본인 상담이 아닌 경우 비밀번호 검증
        if (password == null || !consultation.verifyPassword(password)) {
            throw new BusinessException(ErrorCode.CONSULTATION_PASSWORD_MISMATCH);
        }

        return consultation;
    }

    /**
     * 내 상담 목록 조회 (회원용)
     */
    @Transactional(readOnly = true)
    public Page<QuickConsultationListResponse> getMyConsultations(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Page<QuickConsultation> consultations = consultationRepository
                .findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user, pageable);

        // 본인 상담만 조회하므로 모두 isMyConsultation = true
        return consultations.map(c -> QuickConsultationListResponse.from(c, user.getId()));
    }


    /**
     * 상담 수정 (회원용)
     */
    @Transactional
    public QuickConsultation updateConsultation(
            UUID consultationUuid,
            String userEmail,
            QuickConsultationUpdateRequest request) {

        log.info("상담 수정 시작: consultationUuid={}, userEmail={}", consultationUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        // 본인 확인
        if (!consultation.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 수정 가능 여부 확인 (SUBMITTED 상태만 수정 가능)
        if (!consultation.isEditable()) {
            throw new BusinessException(ErrorCode.CONSULTATION_CANNOT_BE_UPDATED);
        }

        consultation.updateConsultation(
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage(),
                request.getPreferredContactMethod(),
                request.getPreferredContactTime()
        );

        consultation = consultationRepository.save(consultation);

        log.info("상담 수정 완료: consultationUuid={}", consultationUuid);
        return consultation;
    }

    /**
     * 상담 수정 (비회원용 - 비밀번호 검증)
     */
    @Transactional
    public QuickConsultation updateConsultationWithPassword(
            UUID consultationUuid,
            QuickConsultationUpdateRequest request) {

        log.info("비회원 상담 수정 시작: consultationUuid={}", consultationUuid);

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        // 비밀번호 검증
        if (!consultation.verifyPassword(request.getPassword())) {
            throw new BusinessException(ErrorCode.CONSULTATION_PASSWORD_MISMATCH);
        }

        // 수정 가능 여부 확인
        if (!consultation.isEditable()) {
            throw new BusinessException(ErrorCode.CONSULTATION_CANNOT_BE_UPDATED);
        }

        consultation.updateConsultation(
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage(),
                request.getPreferredContactMethod(),
                request.getPreferredContactTime()
        );

        consultation = consultationRepository.save(consultation);

        log.info("비회원 상담 수정 완료: consultationUuid={}", consultationUuid);
        return consultation;
    }

    /**
     * 상담 취소 (회원용)
     */
    @Transactional
    public void cancelConsultation(UUID consultationUuid, String userEmail, String reason) {
        log.info("상담 취소 시작: consultationUuid={}, userEmail={}", consultationUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        // 본인 확인
        if (!consultation.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 취소 가능 여부 확인 (이미 완료된 상담은 취소 불가)
        if (consultation.getStatus() == ConsultationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONSULTATION_CANNOT_BE_UPDATED);
        }

        consultation.cancel(reason);
        consultationRepository.save(consultation);

        log.info("상담 취소 완료: consultationUuid={}", consultationUuid);
    }

    /**
     * 상담 취소 (비회원용 - 비밀번호 검증)
     */
    @Transactional
    public void cancelConsultationWithPassword(UUID consultationUuid, String password, String reason) {
        log.info("비회원 상담 취소 시작: consultationUuid={}", consultationUuid);

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        // 비밀번호 검증
        if (!consultation.verifyPassword(password)) {
            throw new BusinessException(ErrorCode.CONSULTATION_PASSWORD_MISMATCH);
        }

        // 취소 가능 여부 확인 (이미 완료된 상담은 취소 불가)
        if (consultation.getStatus() == ConsultationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONSULTATION_CANNOT_BE_UPDATED);
        }

        consultation.cancel(reason);
        consultationRepository.save(consultation);

        log.info("비회원 상담 취소 완료: consultationUuid={}", consultationUuid);
    }

    // ===== Public 조회 기능 (비회원도 조회 가능) =====

    /**
     * 전체/상태별 상담 목록 조회 (Public - 통합)
     * @param userEmail 현재 로그인한 사용자 이메일 (비로그인 시 null)
     * @param status 상태 (null이면 전체 조회)
     */
    @Transactional(readOnly = true)
    public Page<QuickConsultationListResponse> getAllConsultationsPublic(
            String userEmail,
            ConsultationStatus status,
            Pageable pageable) {

        // 현재 사용자 ID 조회 (비로그인 시 null)
        Long currentUserId = null;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                currentUserId = user.getId();
            }
        }

        // 상담 목록 조회
        Page<QuickConsultation> consultations;
        if (status != null) {
            consultations = consultationRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(status, pageable);
        } else {
            consultations = consultationRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        }

        // DTO 변환 (currentUserId를 전달하여 isMyConsultation 설정)
        final Long userId = currentUserId;
        return consultations.map(c -> QuickConsultationListResponse.from(c, userId));
    }

    // ===== 관리자 기능 =====

    /**
     * 전체 상담 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<QuickConsultation> getAllConsultations(String adminEmail, Pageable pageable) {
        validateAdmin(adminEmail);
        return consultationRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
    }

    /**
     * 상태별 상담 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<QuickConsultation> getConsultationsByStatus(
            String adminEmail,
            ConsultationStatus status,
            Pageable pageable) {

        validateAdmin(adminEmail);
        return consultationRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * 상담 상세 조회 (관리자용, 권한 체크 없음)
     */
    @Transactional(readOnly = true)
    public QuickConsultation getConsultationForAdmin(UUID consultationUuid, String adminEmail) {
        validateAdmin(adminEmail);

        return consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));
    }

    /**
     * 상태 변경 (관리자용)
     */
    @Transactional
    public void updateConsultationStatus(
            UUID consultationUuid,
            String adminEmail,
            ConsultationStatusUpdateRequest request) {

        log.info("상담 상태 변경 시작: consultationUuid={}, status={}", consultationUuid, request.getStatus());

        validateAdmin(adminEmail);

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        consultation.changeStatus(request.getStatus());

        // 완료 처리
        if (request.getStatus() == ConsultationStatus.COMPLETED) {
            consultation.complete(request.getNotes());
        }

        consultationRepository.save(consultation);

        log.info("상담 상태 변경 완료: consultationUuid={}, newStatus={}", consultationUuid, request.getStatus());
    }

    /**
     * 답변 작성 (관리자용)
     */
    @Transactional
    public void respondToConsultation(
            UUID consultationUuid,
            String adminEmail,
            ConsultationResponseRequest request) {

        log.info("상담 답변 작성 시작: consultationUuid={}", consultationUuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        consultation.respond(request.getResponseMessage(), admin);
        consultationRepository.save(consultation);

        log.info("상담 답변 작성 완료: consultationUuid={}", consultationUuid);
    }

    /**
     * 업체 배정 (관리자용)
     */
    @Transactional
    public void assignConsultationToCompany(
            UUID consultationUuid,
            UUID companyUuid,
            String adminEmail) {

        log.info("상담 업체 배정 시작: consultationUuid={}, companyUuid={}", consultationUuid, companyUuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        consultation.assignToCompany(company, admin);
        consultationRepository.save(consultation);

        log.info("상담 업체 배정 완료: consultationUuid={}, companyUuid={}", consultationUuid, companyUuid);
    }

    /**
     * 상담 삭제 (관리자용)
     */
    @Transactional
    public void deleteConsultation(UUID consultationUuid, String adminEmail) {
        log.info("상담 삭제 시작: consultationUuid={}", consultationUuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        QuickConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND));

        consultation.softDelete(admin);
        consultationRepository.save(consultation);

        log.info("상담 삭제 완료: consultationUuid={}", consultationUuid);
    }

    // ===== Private Helper Methods =====

    /**
     * 관리자 권한 검증
     */
    private void validateAdmin(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
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
        // 여러 IP가 있는 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
