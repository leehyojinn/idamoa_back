package com.hip.damoa.domain.estimate.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.model.EstimateProposalAttachment;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.model.EstimateStatus;
import com.hip.damoa.domain.estimate.repository.EstimateProposalAttachmentRepository;
import com.hip.damoa.domain.estimate.repository.EstimateProposalRepository;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.estimate.web.dto.AttachmentRequest;
import com.hip.damoa.domain.estimate.web.dto.AttachmentResponse;
import com.hip.damoa.domain.estimate.web.dto.ProposalCreateRequest;
import com.hip.damoa.domain.estimate.web.dto.ProposalUpdateRequest;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.notification.service.NotificationService;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 견적 제안 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final EstimateProposalRepository proposalRepository;
    private final EstimateProposalAttachmentRepository attachmentRepository;
    private final EstimateRequestRepository estimateRequestRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final NotificationService notificationService;

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
        if (estimateRequest.getStatus() != EstimateStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND);
        }

        // 이미 제안했는지 확인 (WITHDRAWN 제외)
        if (proposalRepository.existsByRequestAndCompanyAndStatusNotWithdrawn(estimateRequest, company)) {
            throw new BusinessException(ErrorCode.PROPOSAL_ALREADY_EXISTS);
        }

        // 제안 생성
        // 일정 정보를 timeline Map으로 구성 (선택사항)
        Map<String, Object> timeline = request.getTimeline();
        if (timeline == null && (request.getProposedStartDate() != null || request.getProposedEndDate() != null)) {
            timeline = new HashMap<>();
            if (request.getProposedStartDate() != null) {
                timeline.put("startDate", request.getProposedStartDate().toString());
            }
            if (request.getProposedEndDate() != null) {
                timeline.put("endDate", request.getProposedEndDate().toString());
            }
        }

        EstimateProposal proposal = EstimateProposal.builder()
                .request(estimateRequest)
                .company(company)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .validUntil(request.getValidUntil())
                .pricingDetails(request.getPricingDetails())
                .timeline(timeline)
                .build();

        proposal = proposalRepository.save(proposal);

        // 첨부파일 처리 (조인 테이블)
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            processAttachments(proposal, request.getAttachments());
            // cascade로 첨부파일 저장을 위해 다시 save
            proposal = proposalRepository.save(proposal);
        }

        // 견적 요청의 제안 수 증가
        estimateRequest.incrementProposalCount();
        estimateRequestRepository.save(estimateRequest);

        // 견적 요청 작성자에게 알림 전송
        String requestOwnerEmail = estimateRequest.getUser().getEmail();
        notificationService.notifyNewProposal(
                requestOwnerEmail,
                estimateRequest.getUuid(),
                company.getName(),
                proposal.getTitle()
        );

        log.info("제안 제출 완료: id={}, companyId={}, attachments={}",
                 proposal.getId(), company.getId(), proposal.getAttachments().size());

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

        // 제안 수정 (기존 엔티티 필드 직접 수정 - UPDATE 쿼리)
        // 일정 정보를 timeline Map으로 구성 (선택사항)
        Map<String, Object> timeline = request.getTimeline();
        if (timeline == null && (request.getProposedStartDate() != null || request.getProposedEndDate() != null)) {
            timeline = new HashMap<>();
            if (request.getProposedStartDate() != null) {
                timeline.put("startDate", request.getProposedStartDate().toString());
            }
            if (request.getProposedEndDate() != null) {
                timeline.put("endDate", request.getProposedEndDate().toString());
            }
        }

        // Entity의 update 메서드 사용 (JPA dirty checking으로 UPDATE 쿼리 실행)
        proposal.update(
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                request.getValidUntil(),
                request.getPricingDetails(),
                timeline
        );

        // 첨부파일 처리 (V30: 조인 테이블)
        if (request.getAttachments() != null) {
            processAttachments(proposal, request.getAttachments());
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

        // 철회 가능한 상태 확인 (수락된 제안은 철회 불가)
        if ("SELECTED".equals(proposal.getStatus())) {
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
     * 견적 요청에 대한 제안 목록 조회 (권한별 필터링)
     * - 요청자: 모든 제안 조회 가능
     * - 제안한 업체: 자신의 제안만 조회 가능
     * - 기타 회원: 제안 개수만 조회 가능
     */
    @Transactional(readOnly = true)
    public Object getProposalsByRequest(String userEmail, Long requestId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        EstimateRequest estimateRequest = estimateRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 1. 견적 요청자 본인인 경우 - 모든 제안 조회 (WITHDRAWN 제외)
        if (estimateRequest.getUser().getId().equals(user.getId())) {
            return proposalRepository.findByRequestAndStatusNotWithdrawn(estimateRequest);
        }

        // 2. 업체 회원인 경우 - 자신의 제안만 조회
        if (user.hasRole("COMPANY")) {
            Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                    .orElse(null); // 업체 프로필이 없을 수도 있음

            if (company != null) {
                List<EstimateProposal> proposals = new ArrayList<>();
                try {
                    proposalRepository.findByRequestAndCompanyAndIsDeletedFalse(estimateRequest, company)
                            .ifPresent(proposals::add);
                } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                    // 버그: 같은 업체가 여러 제안을 제출한 경우
                    log.error("데이터 정합성 오류: 업체 {}가 견적 요청 {}에 여러 개의 제안을 제출했습니다.",
                            company.getId(), estimateRequest.getId());
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
                return proposals;
            }
        }

        // 3. 기타 회원 - 제안 개수만 반환
        Map<String, Object> countInfo = new HashMap<>();
        countInfo.put("proposalCount", estimateRequest.getProposalCount());
        countInfo.put("requestId", requestId);
        return countInfo;
    }

    /**
     * 제안 조회 및 확인 처리 (요청자 또는 제안 업체)
     */
    @Transactional
    public EstimateProposal viewProposal(String userEmail, Long proposalId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        EstimateProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 권한 확인 (견적 요청자 또는 제안 작성 업체)
        boolean isRequestOwner = proposal.getRequest().getUser().getId().equals(user.getId());
        boolean isProposalOwner = false;

        // 업체 소유자인지 확인
        if (!isRequestOwner) {
            Company userCompany = companyRepository.findByOwnerAndIsDeletedFalse(user).orElse(null);
            if (userCompany != null) {
                isProposalOwner = proposal.getCompany().getId().equals(userCompany.getId());
            }
        }

        // 권한이 없는 경우
        if (!isRequestOwner && !isProposalOwner) {
            log.warn("제안 조회 권한 없음: userEmail={}, proposalId={}", userEmail, proposalId);
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 확인 처리 (요청자만)
        if (isRequestOwner) {
            proposal.markAsViewed();
            proposalRepository.save(proposal);
        }

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
        if ("SELECTED".equals(proposal.getStatus()) || "REJECTED".equals(proposal.getStatus())) {
            throw new BusinessException(ErrorCode.PROPOSAL_CANNOT_BE_ACCEPTED);
        }

        // 제안 수락 처리
        proposal.select();
        proposalRepository.save(proposal);

        // 견적 요청 상태를 MATCHED로 변경
        EstimateRequest estimateRequest = proposal.getRequest();
        estimateRequest.match();
        estimateRequestRepository.save(estimateRequest);

        log.info("제안 수락 완료: id={}, 견적 요청 상태 MATCHED로 변경", proposalId);

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

        // 수락된 제안은 삭제 불가
        if ("SELECTED".equals(proposal.getStatus())) {
            throw new BusinessException(ErrorCode.PROPOSAL_CANNOT_BE_DELETED);
        }

        // 첨부파일 Soft Delete
        attachmentRepository.softDeleteByEstimateProposalId(proposal.getId(), java.time.LocalDateTime.now());
        log.info("제안 첨부파일 soft delete 완료: proposalId={}", proposalId);

        // 제안 Soft Delete
        proposal.softDelete();
        proposalRepository.save(proposal);

        // 견적 요청의 제안 수 감소
        EstimateRequest estimateRequest = proposal.getRequest();
        estimateRequest.decrementProposalCount();
        estimateRequestRepository.save(estimateRequest);

        log.info("관리자 제안 삭제 완료: id={}", proposalId);
    }

    // ========== UUID 기반 메서드 ==========

    /**
     * 제안 제출 (UUID 사용)
     */
    @Transactional
    public EstimateProposal createProposalByUuid(String userEmail, UUID requestUuid, ProposalCreateRequest request) {
        log.info("제안 제출 시작 (UUID): userEmail={}, requestUuid={}", userEmail, requestUuid);

        // UUID로 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 기존 createProposal 메서드 호출
        return createProposal(userEmail, estimateRequest.getId(), request);
    }

    /**
     * 제안 수정 (UUID 사용)
     */
    @Transactional
    public EstimateProposal updateProposalByUuid(String userEmail, UUID proposalUuid, ProposalUpdateRequest request) {
        log.info("제안 수정 시작 (UUID): userEmail={}, proposalUuid={}", userEmail, proposalUuid);

        // UUID로 제안 조회
        EstimateProposal proposal = proposalRepository.findByUuidAndIsDeletedFalse(proposalUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 기존 updateProposal 메서드 호출
        return updateProposal(userEmail, proposal.getId(), request);
    }

    /**
     * 제안 철회 (UUID 사용)
     */
    @Transactional
    public void withdrawProposalByUuid(String userEmail, UUID proposalUuid) {
        log.info("제안 철회 시작 (UUID): userEmail={}, proposalUuid={}", userEmail, proposalUuid);

        // UUID로 제안 조회
        EstimateProposal proposal = proposalRepository.findByUuidAndIsDeletedFalse(proposalUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 기존 withdrawProposal 메서드 호출
        withdrawProposal(userEmail, proposal.getId());
    }

    /**
     * 견적 요청별 제안 조회 (UUID 사용)
     */
    @Transactional(readOnly = true)
    public Object getProposalsByRequestUuid(String userEmail, UUID requestUuid) {
        log.info("견적 요청별 제안 조회 (UUID): userEmail={}, requestUuid={}", userEmail, requestUuid);

        // UUID로 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 기존 getProposalsByRequest 메서드 호출
        return getProposalsByRequest(userEmail, estimateRequest.getId());
    }

    /**
     * 제안 상세 조회 (UUID 사용)
     */
    @Transactional
    public EstimateProposal viewProposalByUuid(String userEmail, UUID proposalUuid) {
        log.info("제안 상세 조회 (UUID): userEmail={}, proposalUuid={}", userEmail, proposalUuid);

        // UUID로 제안 조회
        EstimateProposal proposal = proposalRepository.findByUuidAndIsDeletedFalse(proposalUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 기존 viewProposal 메서드 호출
        return viewProposal(userEmail, proposal.getId());
    }

    /**
     * 제안 수락 (UUID 사용)
     */
    @Transactional
    public EstimateProposal acceptProposalByUuid(String userEmail, UUID proposalUuid) {
        log.info("제안 수락 시작 (UUID): userEmail={}, proposalUuid={}", userEmail, proposalUuid);

        // UUID로 제안 조회
        EstimateProposal proposal = proposalRepository.findByUuidAndIsDeletedFalse(proposalUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 기존 acceptProposal 메서드 호출
        return acceptProposal(userEmail, proposal.getId());
    }

    /**
     * 제안 거절 (UUID 사용)
     */
    @Transactional
    public EstimateProposal rejectProposalByUuid(String userEmail, UUID proposalUuid, String reason) {
        log.info("제안 거절 시작 (UUID): userEmail={}, proposalUuid={}", userEmail, proposalUuid);

        // UUID로 제안 조회
        EstimateProposal proposal = proposalRepository.findByUuidAndIsDeletedFalse(proposalUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPOSAL_NOT_FOUND));

        // 기존 rejectProposal 메서드 호출
        return rejectProposal(userEmail, proposal.getId(), reason);
    }

    // ===== 첨부파일 처리 메서드 (V30 Migration) =====

    /**
     * 첨부파일 리스트 처리 (UUID → File ID 변환 및 조인 테이블 저장)
     */
    private void processAttachments(EstimateProposal proposal, List<AttachmentRequest> attachmentRequests) {
        log.info("첨부파일 처리 시작: proposalId={}, count={}", proposal.getId(), attachmentRequests.size());

        // 기존 첨부파일 제거 (orphanRemoval = true이므로 자동 삭제됨)
        proposal.clearAttachments();

        // 새로운 첨부파일 추가
        for (int i = 0; i < attachmentRequests.size(); i++) {
            AttachmentRequest req = attachmentRequests.get(i);

            // UUID → File ID 변환
            Long fileId = convertUuidToFileId(req.getFileUuid());

            // Files 테이블의 entityId 업데이트 (스케줄러 삭제 방지)
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
            file.updateEntityInfo("PROPOSAL_ATTACHMENT", proposal.getId());
            fileRepository.save(file);

            // displayOrder가 null이면 인덱스 사용
            Integer displayOrder = req.getDisplayOrder() != null ? req.getDisplayOrder() : i;

            // EstimateProposalAttachment 엔티티 생성
            EstimateProposalAttachment attachment = EstimateProposalAttachment.builder()
                    .estimateProposal(proposal)
                    .fileId(fileId)
                    .fileType(req.getFileType())
                    .fileDescription(req.getFileDescription())
                    .displayOrder(displayOrder)
                    .build();

            // 부모 엔티티에 추가 (cascade로 자동 저장됨)
            proposal.addAttachment(attachment);

            log.debug("첨부파일 추가: fileId={}, type={}, order={}", fileId, req.getFileType(), displayOrder);
        }

        log.info("첨부파일 처리 완료: proposalId={}, count={}", proposal.getId(), attachmentRequests.size());
    }

    /**
     * EstimateProposal의 첨부파일을 AttachmentResponse 리스트로 변환
     * (File ID → URL 변환 + File 정보 포함)
     */
    public List<AttachmentResponse> getAttachmentResponses(EstimateProposal proposal) {
        if (proposal.getAttachments() == null || proposal.getAttachments().isEmpty()) {
            return new ArrayList<>();
        }

        return proposal.getAttachments().stream()
                .map(attachment -> {
                    // File 엔티티 조회
                    File file = fileRepository.findById(attachment.getFileId()).orElse(null);
                    if (file == null) {
                        return null;
                    }

                    return AttachmentResponse.from(
                        attachment,
                        file.getUuid(),
                        file.getFileUrl(),
                        file.getOriginalFilename(),
                        file.getMimeType(),
                        file.getFileSize()
                    );
                })
                .filter(response -> response != null)  // null 제거
                .toList();
    }

    /**
     * UUID를 File ID로 변환
     */
    private Long convertUuidToFileId(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidString);
            return fileRepository.findByUuidAndIsDeletedFalse(uuid)
                    .map(File::getId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }
}
