package com.hip.damoa.domain.consultation.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.company.repository.CompanyPortfolioRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.consultation.model.ContactMethod;
import com.hip.damoa.domain.consultation.model.PortfolioConsultation;
import com.hip.damoa.domain.consultation.model.PortfolioConsultationStatus;
import com.hip.damoa.domain.consultation.repository.PortfolioConsultationRepository;
import com.hip.damoa.domain.consultation.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioConsultationService {

    private final PortfolioConsultationRepository consultationRepository;
    private final CompanyPortfolioRepository portfolioRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    // ===== 사용자 API =====

    /**
     * 상담신청 생성
     */
    @Transactional
    public PortfolioConsultation createConsultation(String userEmail, PortfolioConsultationCreateRequest request) {
        log.info("포트폴리오 상담신청 생성: userEmail={}, portfolioUuid={}", userEmail, request.getPortfolioUuid());

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyPortfolio portfolio = portfolioRepository.findByUuidAndIsDeletedFalse(request.getPortfolioUuid())
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        Company company = portfolio.getCompany();

        PortfolioConsultation consultation = PortfolioConsultation.builder()
                .user(user)
                .portfolio(portfolio)
                .company(company)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .title(request.getTitle())
                .content(request.getContent())
                .contactMethod(request.getContactMethod())
                .availableTime(request.getAvailableTime())
                .build();

        consultation = consultationRepository.save(consultation);
        log.info("포트폴리오 상담신청 생성 완료: uuid={}", consultation.getUuid());

        return consultation;
    }

    /**
     * 내 상담신청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PortfolioConsultation> getMyConsultations(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return consultationRepository.findByUserId(user.getId(), pageable);
    }

    /**
     * 내 상담신청 상세 조회
     */
    @Transactional(readOnly = true)
    public PortfolioConsultation getMyConsultation(String userEmail, UUID consultationUuid) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PortfolioConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        if (!consultation.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_ACCESS_DENIED);
        }

        return consultation;
    }

    /**
     * 내 상담신청 수정
     */
    @Transactional
    public PortfolioConsultation updateMyConsultation(String userEmail, UUID consultationUuid,
                                                       PortfolioConsultationUpdateRequest request) {
        log.info("포트폴리오 상담신청 수정: consultationUuid={}", consultationUuid);

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PortfolioConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        if (!consultation.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_ACCESS_DENIED);
        }

        if (!consultation.isEditable()) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_EDITABLE);
        }

        consultation.updateConsultation(
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                request.getTitle(),
                request.getContent(),
                request.getContactMethod(),
                request.getAvailableTime()
        );

        log.info("포트폴리오 상담신청 수정 완료: uuid={}", consultation.getUuid());
        return consultation;
    }

    /**
     * 내 상담신청 삭제
     */
    @Transactional
    public void deleteMyConsultation(String userEmail, UUID consultationUuid) {
        log.info("포트폴리오 상담신청 삭제: consultationUuid={}", consultationUuid);

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PortfolioConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        if (!consultation.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_ACCESS_DENIED);
        }

        if (!consultation.isDeletable()) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_DELETABLE);
        }

        consultation.softDelete();
        log.info("포트폴리오 상담신청 삭제 완료: uuid={}", consultationUuid);
    }

    // ===== 업체 API =====

    /**
     * 업체의 상담신청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PortfolioConsultation> getCompanyConsultations(String ownerEmail, UUID companyUuid,
                                                                PortfolioConsultationStatus status,
                                                                Pageable pageable) {
        User owner = userRepository.findByEmailAndIsDeletedFalse(ownerEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 업체 소유자 확인
        if (company.getOwner() == null || !company.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        if (status != null) {
            return consultationRepository.findByCompanyUuidAndStatus(companyUuid, status, pageable);
        }
        return consultationRepository.findByCompanyUuid(companyUuid, pageable);
    }

    /**
     * 업체의 상담신청 상세 조회
     */
    @Transactional(readOnly = true)
    public PortfolioConsultation getCompanyConsultation(String ownerEmail, UUID companyUuid, UUID consultationUuid) {
        User owner = userRepository.findByEmailAndIsDeletedFalse(ownerEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 업체 소유자 확인
        if (company.getOwner() == null || !company.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        PortfolioConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        // 해당 업체의 상담신청인지 확인
        if (!consultation.getCompany().getUuid().equals(companyUuid)) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_ACCESS_DENIED);
        }

        return consultation;
    }

    /**
     * 상담신청 상태 변경
     */
    @Transactional
    public PortfolioConsultation updateConsultationStatus(String ownerEmail, UUID companyUuid,
                                                           UUID consultationUuid,
                                                           PortfolioConsultationStatusUpdateRequest request) {
        log.info("포트폴리오 상담신청 상태 변경: consultationUuid={}, newStatus={}",
                consultationUuid, request.getStatus());

        User owner = userRepository.findByEmailAndIsDeletedFalse(ownerEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 업체 소유자 확인
        if (company.getOwner() == null || !company.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        PortfolioConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        // 해당 업체의 상담신청인지 확인
        if (!consultation.getCompany().getUuid().equals(companyUuid)) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_ACCESS_DENIED);
        }

        consultation.changeStatus(request.getStatus());
        log.info("포트폴리오 상담신청 상태 변경 완료: uuid={}, status={}", consultationUuid, request.getStatus());

        return consultation;
    }

    /**
     * 상담신청 답변 등록
     */
    @Transactional
    public PortfolioConsultation submitAnswer(String ownerEmail, UUID companyUuid,
                                               UUID consultationUuid,
                                               PortfolioConsultationAnswerRequest request) {
        log.info("포트폴리오 상담신청 답변 등록: consultationUuid={}", consultationUuid);

        User owner = userRepository.findByEmailAndIsDeletedFalse(ownerEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 업체 소유자 확인
        if (company.getOwner() == null || !company.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        PortfolioConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        // 해당 업체의 상담신청인지 확인
        if (!consultation.getCompany().getUuid().equals(companyUuid)) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_ACCESS_DENIED);
        }

        consultation.submitAnswer(request.getAnswer(), owner);
        log.info("포트폴리오 상담신청 답변 등록 완료: uuid={}, answeredBy={}", consultationUuid, owner.getEmail());

        return consultation;
    }

    /**
     * 상담신청 메모 등록/수정
     */
    @Transactional
    public PortfolioConsultation updateMemo(String ownerEmail, UUID companyUuid,
                                             UUID consultationUuid,
                                             PortfolioConsultationMemoRequest request) {
        log.info("포트폴리오 상담신청 메모 등록: consultationUuid={}", consultationUuid);

        User owner = userRepository.findByEmailAndIsDeletedFalse(ownerEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 업체 소유자 확인
        if (company.getOwner() == null || !company.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        PortfolioConsultation consultation = consultationRepository.findByUuidAndIsDeletedFalse(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        // 해당 업체의 상담신청인지 확인
        if (!consultation.getCompany().getUuid().equals(companyUuid)) {
            throw new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_ACCESS_DENIED);
        }

        consultation.updateMemo(request.getMemo());
        log.info("포트폴리오 상담신청 메모 등록 완료: uuid={}", consultationUuid);

        return consultation;
    }

    // ===== Admin API =====

    /**
     * [Admin] 전체 상담신청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PortfolioConsultation> getAdminConsultations(
            PortfolioConsultationStatus status,
            Boolean isDeleted,
            String keyword,
            Pageable pageable) {

        // 키워드 검색
        if (keyword != null && !keyword.isBlank()) {
            if (isDeleted != null) {
                return consultationRepository.searchByKeywordAndIsDeleted(keyword, isDeleted, pageable);
            }
            return consultationRepository.searchByKeyword(keyword, pageable);
        }

        // 상태 + 삭제 여부 필터
        if (status != null && isDeleted != null) {
            return consultationRepository.findByStatusAndIsDeleted(status, isDeleted, pageable);
        }

        // 상태 필터만
        if (status != null) {
            return consultationRepository.findByStatusForAdmin(status, pageable);
        }

        // 삭제 여부 필터만
        if (isDeleted != null) {
            return consultationRepository.findByIsDeleted(isDeleted, pageable);
        }

        // 전체 조회
        return consultationRepository.findAllForAdmin(pageable);
    }

    /**
     * [Admin] 상담신청 상세 조회 (삭제된 것도 조회 가능)
     */
    @Transactional(readOnly = true)
    public PortfolioConsultation getAdminConsultation(UUID consultationUuid) {
        return consultationRepository.findByUuid(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));
    }

    /**
     * [Admin] 상담신청 수정
     */
    @Transactional
    public PortfolioConsultation updateAdminConsultation(UUID consultationUuid,
                                                          PortfolioConsultationUpdateRequest request) {
        log.info("[Admin] 포트폴리오 상담신청 수정: consultationUuid={}", consultationUuid);

        PortfolioConsultation consultation = consultationRepository.findByUuid(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        consultation.updateConsultation(
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                request.getTitle(),
                request.getContent(),
                request.getContactMethod(),
                request.getAvailableTime()
        );

        log.info("[Admin] 포트폴리오 상담신청 수정 완료: uuid={}", consultationUuid);
        return consultation;
    }

    /**
     * [Admin] 상담신청 상태 변경
     */
    @Transactional
    public PortfolioConsultation updateAdminConsultationStatus(UUID consultationUuid,
                                                                PortfolioConsultationStatusUpdateRequest request) {
        log.info("[Admin] 포트폴리오 상담신청 상태 변경: consultationUuid={}, newStatus={}",
                consultationUuid, request.getStatus());

        PortfolioConsultation consultation = consultationRepository.findByUuid(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        consultation.changeStatus(request.getStatus());
        log.info("[Admin] 포트폴리오 상담신청 상태 변경 완료: uuid={}", consultationUuid);

        return consultation;
    }

    /**
     * [Admin] 상담신청 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteAdminConsultation(UUID consultationUuid) {
        log.info("[Admin] 포트폴리오 상담신청 삭제: consultationUuid={}", consultationUuid);

        PortfolioConsultation consultation = consultationRepository.findByUuid(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        consultation.softDelete();
        log.info("[Admin] 포트폴리오 상담신청 삭제 완료: uuid={}", consultationUuid);
    }

    /**
     * [Admin] 상담신청 복구
     */
    @Transactional
    public PortfolioConsultation restoreAdminConsultation(UUID consultationUuid) {
        log.info("[Admin] 포트폴리오 상담신청 복구: consultationUuid={}", consultationUuid);

        PortfolioConsultation consultation = consultationRepository.findByUuid(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        consultation.restore();
        log.info("[Admin] 포트폴리오 상담신청 복구 완료: uuid={}", consultationUuid);

        return consultation;
    }

    /**
     * [Admin] 상담신청 영구 삭제
     */
    @Transactional
    public void hardDeleteAdminConsultation(UUID consultationUuid) {
        log.info("[Admin] 포트폴리오 상담신청 영구 삭제: consultationUuid={}", consultationUuid);

        PortfolioConsultation consultation = consultationRepository.findByUuid(consultationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_CONSULTATION_NOT_FOUND));

        consultationRepository.delete(consultation);
        log.info("[Admin] 포트폴리오 상담신청 영구 삭제 완료: uuid={}", consultationUuid);
    }
}
