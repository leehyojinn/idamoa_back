package com.hip.damoa.domain.estimate.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.repository.EstimateProposalRepository;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.estimate.web.dto.ProposalCreateRequest;
import com.hip.damoa.domain.estimate.web.dto.ProposalUpdateRequest;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 견적 제안 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final EstimateProposalRepository proposalRepository;
    private final EstimateRequestRepository estimateRequestRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    // ========== 업체용 메서드 ==========

    /**
     * 제안 제출 (업체)
     */
    @Transactional
    public EstimateProposal createProposal(String userEmail, Long requestId, ProposalCreateRequest request) {
        log.info("제안 제출 시작: userEmail={}, requestId={}", userEmail, requestId);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // COMPANY 역할 확인
        if (!user.hasRole("COMPANY")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 업체 조회
        Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 견적 요청 상태 확인 (PUBLISHED 상태만 제안 가능)
        if (!"PUBLISHED".equals(estimateRequest.getStatus())) {
            throw new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND);
        }

        // 이미 제안했는지 확인
        if (proposalRepository.existsByRequestAndCompany(estimateRequest, company)) {
            throw new BusinessException(ErrorCode.PROPOSAL_ALREADY_EXISTS);
        }

        // 제안 생성
        String[] portfolioLinks = request.getPortfolioLinks() != null ?
                request.getPortfolioLinks().toArray(new String[0]) : null;

        EstimateProposal proposal = EstimateProposal.builder()
                .request(estimateRequest)
                .company(company)
                .proposalAmount(request.getProposalAmount())
                .proposalContent(request.getProposalContent())
                .estimatedDurationDays(request.getEstimatedDurationDays())
                .proposedStartDate(request.getProposedStartDate())
                .proposedEndDate(request.getProposedEndDate())
                .portfolioLinks(portfolioLinks)
                .coverLetter(request.getCoverLetter())
                .build();

        proposal = proposalRepository.save(proposal);

        // 견적 요청의 제안 수 증가
        estimateRequest.incrementProposalCount();
        estimateRequestRepository.save(estimateRequest);

        log.info("제안 제출 완료: id={}, companyId={}", proposal.getId(), company.getId());

        return proposal;
    }

    /**
     * 제안 수정 (업체)
     */
    @Transactional
    public EstimateProposal updateProposal(String userEmail, Long proposalId, ProposalUpdateRequest request) {
        log.info("제안 수정 시작: userEmail={}, proposalId={}", userEmail, proposalId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        EstimateProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 권한 확인
        if (!proposal.getCompany().getId().equals(company.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 수정 가능한 상태 확인 (SUBMITTED, VIEWED만 수정 가능)
        if (!"SUBMITTED".equals(proposal.getStatus()) && !"VIEWED".equals(proposal.getStatus())) {
            throw new BusinessException(ErrorCode.PROPOSAL_CANNOT_BE_UPDATED);
        }

        // 제안 수정
        if (request.getProposalAmount() != null || request.getProposalContent() != null) {
            String[] portfolioLinks = request.getPortfolioLinks() != null ?
                    request.getPortfolioLinks().toArray(new String[0]) : proposal.getPortfolioLinks();

            proposal = EstimateProposal.builder()
                    .request(proposal.getRequest())
                    .company(proposal.getCompany())
                    .proposalAmount(request.getProposalAmount() != null ? request.getProposalAmount() : proposal.getProposalAmount())
                    .proposalContent(request.getProposalContent() != null ? request.getProposalContent() : proposal.getProposalContent())
                    .estimatedDurationDays(request.getEstimatedDurationDays() != null ? request.getEstimatedDurationDays() : proposal.getEstimatedDurationDays())
                    .proposedStartDate(request.getProposedStartDate() != null ? request.getProposedStartDate() : proposal.getProposedStartDate())
                    .proposedEndDate(request.getProposedEndDate() != null ? request.getProposedEndDate() : proposal.getProposedEndDate())
                    .portfolioLinks(portfolioLinks)
                    .coverLetter(request.getCoverLetter() != null ? request.getCoverLetter() : proposal.getCoverLetter())
                    .status(proposal.getStatus())
                    .viewedAt(proposal.getViewedAt())
                    .acceptedAt(proposal.getAcceptedAt())
                    .rejectedAt(proposal.getRejectedAt())
                    .rejectionReason(proposal.getRejectionReason())
                    .build();

            proposal = proposalRepository.save(proposal);
        }

        log.info("제안 수정 완료: id={}", proposal.getId());

        return proposal;
    }

    /**
     * 제안 철회 (업체)
     */
    @Transactional
    public void withdrawProposal(String userEmail, Long proposalId) {
        log.info("제안 철회 시작: userEmail={}, proposalId={}", userEmail, proposalId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        EstimateProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 권한 확인
        if (!proposal.getCompany().getId().equals(company.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 철회 가능한 상태 확인
        if ("ACCEPTED".equals(proposal.getStatus())) {
            throw new BusinessException(ErrorCode.PROPOSAL_CANNOT_BE_UPDATED);
        }

        proposal.withdraw();
        proposalRepository.save(proposal);

        // 견적 요청의 제안 수 감소
        EstimateRequest estimateRequest = proposal.getRequest();
        estimateRequest.decrementProposalCount();
        estimateRequestRepository.save(estimateRequest);

        log.info("제안 철회 완료: id={}", proposalId);
    }

    /**
     * 업체의 제안 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<EstimateProposal> getCompanyProposals(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return proposalRepository.findByCompany(company, pageable);
    }

    // ========== 견적 요청자용 메서드 ==========

    /**
     * 견적 요청에 대한 제안 목록 조회 (요청자)
     */
    @Transactional(readOnly = true)
    public List<EstimateProposal> getProposalsByRequest(String userEmail, Long requestId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        EstimateRequest estimateRequest = estimateRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 권한 확인 (요청자 본인만)
        if (!estimateRequest.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return proposalRepository.findByRequest(estimateRequest);
    }

    /**
     * 제안 조회 및 확인 처리 (요청자)
     */
    @Transactional
    public EstimateProposal viewProposal(String userEmail, Long proposalId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        EstimateProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 권한 확인 (요청자 본인만)
        if (!proposal.getRequest().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 확인 처리
        proposal.markAsViewed();
        proposalRepository.save(proposal);

        return proposal;
    }

    /**
     * 제안 수락 (요청자)
     */
    @Transactional
    public EstimateProposal acceptProposal(String userEmail, Long proposalId) {
        log.info("제안 수락 시작: userEmail={}, proposalId={}", userEmail, proposalId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        EstimateProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 권한 확인
        if (!proposal.getRequest().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 수락 가능한 상태 확인
        if ("ACCEPTED".equals(proposal.getStatus()) || "REJECTED".equals(proposal.getStatus())) {
            throw new BusinessException(ErrorCode.PROPOSAL_CANNOT_BE_ACCEPTED);
        }

        proposal.accept();
        proposalRepository.save(proposal);

        log.info("제안 수락 완료: id={}", proposalId);

        return proposal;
    }

    /**
     * 제안 거절 (요청자)
     */
    @Transactional
    public EstimateProposal rejectProposal(String userEmail, Long proposalId, String reason) {
        log.info("제안 거절 시작: userEmail={}, proposalId={}", userEmail, proposalId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        EstimateProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 권한 확인
        if (!proposal.getRequest().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        proposal.reject(reason);
        proposalRepository.save(proposal);

        log.info("제안 거절 완료: id={}", proposalId);

        return proposal;
    }

    // ========== 관리자용 메서드 ==========

    /**
     * 관리자 - 모든 제안 조회
     */
    @Transactional(readOnly = true)
    public Page<EstimateProposal> getAllProposals(String adminEmail, Pageable pageable) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return proposalRepository.findAll(pageable);
    }

    /**
     * 관리자 - 제안 삭제
     */
    @Transactional
    public void deleteProposal(String adminEmail, Long proposalId) {
        log.info("관리자 제안 삭제: adminEmail={}, proposalId={}", adminEmail, proposalId);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        EstimateProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        proposalRepository.delete(proposal);

        // 견적 요청의 제안 수 감소
        EstimateRequest estimateRequest = proposal.getRequest();
        estimateRequest.decrementProposalCount();
        estimateRequestRepository.save(estimateRequest);

        log.info("관리자 제안 삭제 완료: id={}", proposalId);
    }
}
