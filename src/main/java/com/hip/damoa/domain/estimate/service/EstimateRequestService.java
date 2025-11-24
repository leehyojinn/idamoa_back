package com.hip.damoa.domain.estimate.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.model.EstimateRequestAttachment;
import com.hip.damoa.domain.estimate.model.EstimateStatus;
import com.hip.damoa.domain.estimate.repository.EstimateProposalRepository;
import com.hip.damoa.domain.estimate.repository.EstimateRequestAttachmentRepository;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.estimate.web.dto.*;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
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
 * 견적 요청 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstimateRequestService {

    private final EstimateRequestRepository estimateRequestRepository;
    private final EstimateRequestAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final EstimateProposalRepository proposalRepository;
    private final CompanyRepository companyRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;

    /**
     * 견적 요청 생성
     */
    @Transactional
    public EstimateRequest createEstimateRequest(String userEmail, EstimateRequestCreateRequest request) {
        log.info("견적 요청 생성 시작: userEmail={}", userEmail);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 위치 정보 생성
        String location = buildLocation(request.getSiteAddress(), request.getSiteCity(), request.getSiteState());

        // Requirements JSONB 데이터 생성
        Map<String, Object> requirements = new HashMap<>();
        if (request.getAreaSqm() != null) {
            requirements.put("areaSqm", request.getAreaSqm());
        }
        if (request.getAreaPyeong() != null) {
            requirements.put("areaPyeong", request.getAreaPyeong());
        }
        if (request.getEstimateType() != null) {
            requirements.put("estimateType", request.getEstimateType());
        }
        if (request.getDesiredStartDate() != null) {
            requirements.put("desiredStartDate", request.getDesiredStartDate().toString());
        }
        if (request.getDesiredCompletionDate() != null) {
            requirements.put("desiredCompletionDate", request.getDesiredCompletionDate().toString());
        }
        // 주소 정보를 requirements에도 저장
        if (request.getSiteAddress() != null) {
            requirements.put("siteAddress", request.getSiteAddress());
        }
        if (request.getSiteCity() != null) {
            requirements.put("siteCity", request.getSiteCity());
        }
        if (request.getSiteState() != null) {
            requirements.put("siteState", request.getSiteState());
        }

        // 견적 요청 생성 (바로 PUBLISHED 상태로 생성)
        EstimateRequest estimateRequest = EstimateRequest.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getBusinessType()) // 업종을 카테고리로 매핑
                .requirements(requirements)
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .desiredStartDate(request.getDesiredStartDate())
                .desiredEndDate(request.getDesiredCompletionDate())
                .location(location)
                .address(request.getSiteAddress())
                .expiresAt(request.getSubmissionDeadline())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .status(EstimateStatus.PUBLISHED)  // DRAFT가 아닌 PUBLISHED로 생성
                // V26 필드 추가
                .clientName(request.getClientName())
                .businessType(request.getBusinessType())
                .areaPyeong(request.getAreaPyeong())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .submissionDeadline(request.getSubmissionDeadline())
                .build();

        estimateRequest = estimateRequestRepository.save(estimateRequest);

        // 첨부파일 처리 (조인 테이블)
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            processAttachments(estimateRequest, request.getAttachments());
            // cascade로 첨부파일 저장을 위해 다시 save
            estimateRequest = estimateRequestRepository.save(estimateRequest);
        }

        log.info("견적 요청 생성 완료: id={}, title={}, attachments={}",
                 estimateRequest.getId(), estimateRequest.getTitle(),
                 estimateRequest.getAttachments().size());

        return estimateRequest;
    }

    /**
     * 견적 요청 수정
     */
    @Transactional
    public EstimateRequest updateEstimateRequest(
            String userEmail,
            Long requestId,
            EstimateRequestUpdateRequest request) {

        log.info("견적 요청 수정 시작: requestId={}, userEmail={}", requestId, userEmail);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 권한 확인
        if (!estimateRequest.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 매칭된 견적 요청은 수정 불가
        if (estimateRequest.getStatus() == EstimateStatus.MATCHED) {
            throw new BusinessException(ErrorCode.ESTIMATE_REQUEST_ALREADY_MATCHED);
        }

        // Requirements JSONB 업데이트
        Map<String, Object> requirements = estimateRequest.getRequirements() != null
            ? new HashMap<>(estimateRequest.getRequirements())
            : new HashMap<>();

        if (request.getAreaSqm() != null) {
            requirements.put("areaSqm", request.getAreaSqm());
        }
        if (request.getAreaPyeong() != null) {
            requirements.put("areaPyeong", request.getAreaPyeong());
        }
        if (request.getEstimateType() != null) {
            requirements.put("estimateType", request.getEstimateType());
        }
        if (request.getDesiredStartDate() != null) {
            requirements.put("desiredStartDate", request.getDesiredStartDate().toString());
        }
        if (request.getDesiredCompletionDate() != null) {
            requirements.put("desiredCompletionDate", request.getDesiredCompletionDate().toString());
        }
        if (request.getSiteAddress() != null) {
            requirements.put("siteAddress", request.getSiteAddress());
        }
        if (request.getSiteCity() != null) {
            requirements.put("siteCity", request.getSiteCity());
        }
        if (request.getSiteState() != null) {
            requirements.put("siteState", request.getSiteState());
        }

        // 위치 정보 생성
        String location = null;
        if (request.getSiteAddress() != null || request.getSiteCity() != null || request.getSiteState() != null) {
            location = buildLocation(
                    request.getSiteAddress() != null ? request.getSiteAddress() : estimateRequest.getAddress(),
                    request.getSiteCity() != null ? request.getSiteCity() : "",
                    request.getSiteState() != null ? request.getSiteState() : ""
            );
        }

        // Entity 필드 업데이트
        if (request.getTitle() != null) {
            estimateRequest.updateTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            estimateRequest.updateDescription(request.getDescription());
        }
        if (request.getBusinessType() != null) {
            estimateRequest.updateCategory(request.getBusinessType());
            estimateRequest.updateBusinessType(request.getBusinessType());
        }
        if (!requirements.isEmpty()) {
            estimateRequest.updateRequirements(requirements);
        }
        if (request.getBudgetMin() != null) {
            estimateRequest.updateBudgetMin(request.getBudgetMin());
        }
        if (request.getBudgetMax() != null) {
            estimateRequest.updateBudgetMax(request.getBudgetMax());
        }
        if (request.getDesiredStartDate() != null) {
            estimateRequest.updateDesiredStartDate(request.getDesiredStartDate());
        }
        if (request.getDesiredCompletionDate() != null) {
            estimateRequest.updateDesiredEndDate(request.getDesiredCompletionDate());
        }
        if (location != null) {
            estimateRequest.updateLocation(location);
        }
        if (request.getSiteAddress() != null) {
            estimateRequest.updateAddress(request.getSiteAddress());
        }
        if (request.getIsPublic() != null) {
            estimateRequest.updateIsPublic(request.getIsPublic());
        }
        if (request.getSubmissionDeadline() != null) {
            estimateRequest.updateExpiresAt(request.getSubmissionDeadline());
            estimateRequest.updateSubmissionDeadline(request.getSubmissionDeadline());
        }

        // V26 fields update
        if (request.getClientName() != null) {
            estimateRequest.updateClientName(request.getClientName());
        }
        if (request.getAreaPyeong() != null) {
            estimateRequest.updateAreaPyeong(request.getAreaPyeong());
        }
        if (request.getContactName() != null) {
            estimateRequest.updateContactName(request.getContactName());
        }
        if (request.getContactPhone() != null) {
            estimateRequest.updateContactPhone(request.getContactPhone());
        }
        // 첨부파일 처리 (V30: 조인 테이블)
        if (request.getAttachments() != null) {
            processAttachments(estimateRequest, request.getAttachments());
        }

        estimateRequest = estimateRequestRepository.save(estimateRequest);

        log.info("견적 요청 수정 완료: id={}", estimateRequest.getId());

        return estimateRequest;
    }

    /**
     * 견적 요청 발행 (DRAFT → PUBLISHED)
     */
    @Transactional
    public EstimateRequest publishEstimateRequest(String userEmail, Long requestId) {
        log.info("견적 요청 발행: requestId={}, userEmail={}", requestId, userEmail);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 권한 확인
        if (!estimateRequest.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 상태 확인
        if (estimateRequest.getStatus() != EstimateStatus.DRAFT) {
            throw new BusinessException(ErrorCode.ESTIMATE_REQUEST_ALREADY_PUBLISHED);
        }

        // 발행
        estimateRequest.publish();
        estimateRequest = estimateRequestRepository.save(estimateRequest);

        log.info("견적 요청 발행 완료: id={}", estimateRequest.getId());

        return estimateRequest;
    }

    /**
     * 견적 요청 조회 (단건)
     */
    @Transactional
    public EstimateRequest getEstimateRequest(Long requestId) {
        log.info("견적 요청 조회: requestId={}", requestId);

        EstimateRequest estimateRequest = estimateRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 조회수 증가
        estimateRequest.incrementViewCount();
        estimateRequestRepository.save(estimateRequest);

        return estimateRequest;
    }

    /**
     * 사용자의 견적 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<EstimateRequest> getUserEstimateRequests(String userEmail, Pageable pageable) {
        log.info("사용자 견적 요청 목록 조회: userEmail={}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return estimateRequestRepository.findByUserAndIsDeletedFalse(user, pageable);
    }

    /**
     * 공개 견적 요청 목록 조회 (모든 상태)
     */
    @Transactional(readOnly = true)
    public Page<EstimateRequest> getPublicEstimateRequests(Pageable pageable) {
        log.info("공개 견적 요청 목록 조회 (모든 상태)");
        return estimateRequestRepository.findByIsPublicAndIsDeletedFalse(true, pageable);
    }

    /**
     * 견적 요청 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteEstimateRequest(String userEmail, Long requestId) {
        log.info("견적 요청 삭제: requestId={}, userEmail={}", requestId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        EstimateRequest estimateRequest = estimateRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 권한 확인
        if (!estimateRequest.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 매칭된 견적 요청은 삭제 불가
        if (estimateRequest.getStatus() == EstimateStatus.MATCHED) {
            throw new BusinessException(ErrorCode.ESTIMATE_REQUEST_ALREADY_MATCHED);
        }

        // 제안이 있는지 확인
        long proposalCount = proposalRepository.countByRequestAndIsDeletedFalse(estimateRequest);
        if (proposalCount > 0) {
            throw new BusinessException(ErrorCode.ESTIMATE_REQUEST_HAS_PROPOSALS);
        }

        // 첨부파일 Soft Delete
        attachmentRepository.softDeleteByEstimateRequestId(estimateRequest.getId(), java.time.LocalDateTime.now());
        log.info("견적 요청 첨부파일 soft delete 완료: requestId={}", estimateRequest.getId());

        // Soft Delete
        estimateRequest.softDelete();
        estimateRequestRepository.save(estimateRequest);

        log.info("견적 요청 삭제 완료: id={}", estimateRequest.getId());
    }

    // ========== UUID 기반 메서드 ==========

    /**
     * 견적 요청 수정 (UUID 사용)
     */
    @Transactional
    public EstimateRequest updateEstimateRequestByUuid(
            String userEmail,
            UUID requestUuid,
            EstimateRequestUpdateRequest request) {

        log.info("견적 요청 수정 시작 (UUID): requestUuid={}, userEmail={}", requestUuid, userEmail);

        // UUID로 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 기존 updateEstimateRequest 메서드 호출
        return updateEstimateRequest(userEmail, estimateRequest.getId(), request);
    }

    /**
     * 견적 요청 발행 (UUID 사용)
     */
    @Transactional
    public EstimateRequest publishEstimateRequestByUuid(String userEmail, UUID requestUuid) {
        log.info("견적 요청 발행 (UUID): requestUuid={}, userEmail={}", requestUuid, userEmail);

        // UUID로 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 기존 publishEstimateRequest 메서드 호출
        return publishEstimateRequest(userEmail, estimateRequest.getId());
    }

    /**
     * 견적 요청 조회 (UUID 사용)
     */
    @Transactional
    public EstimateRequest getEstimateRequestByUuid(UUID requestUuid) {
        log.info("견적 요청 조회 (UUID): requestUuid={}", requestUuid);

        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 조회수 증가
        estimateRequest.incrementViewCount();
        estimateRequestRepository.save(estimateRequest);

        return estimateRequest;
    }

    /**
     * 견적 요청 상세 조회 (제안 목록 포함, 권한별 필터링)
     * 로그인 안한 경우(userEmail=null) PUBLIC 레벨로 조회
     */
    @Transactional
    public EstimateRequestDetailResponse getEstimateRequestDetailByUuid(String userEmail, UUID requestUuid) {
        log.info("견적 요청 상세 조회 (제안 포함): requestUuid={}, userEmail={}", requestUuid, userEmail);

        // 사용자 조회 (로그인 안한 경우 null)
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail)
                    .orElse(null);  // 이메일이 잘못된 경우도 null 처리
        }

        // 견적 요청 조회 (조회수 증가 포함)
        EstimateRequest estimateRequest = getEstimateRequestByUuid(requestUuid);

        // 기본 응답 생성
        String userName = getUserName(estimateRequest);
        EstimateRequestDetailResponse response = EstimateRequestDetailResponse.from(estimateRequest, userName);

        // 첨부파일 추가
        List<AttachmentResponse> attachments = getAttachmentResponses(estimateRequest);
        response.setAttachments(attachments);

        // 제안 정보 추가 (권한별 필터링, user가 null이면 PUBLIC 레벨)
        ProposalsInfo proposalsInfo = getProposalsInfo(estimateRequest, user);
        response.setProposals(proposalsInfo);

        return response;
    }

    /**
     * 권한별 제안 정보 조회 (user가 null이면 PUBLIC 레벨)
     */
    private ProposalsInfo getProposalsInfo(EstimateRequest estimateRequest, User user) {
        // 0. 로그인 안한 경우 - PUBLIC 레벨 (제안 요약만 조회)
        if (user == null) {
            List<EstimateProposal> proposals = proposalRepository.findByRequestAndStatusNotWithdrawn(estimateRequest);

            List<ProposalSummaryResponse> summaries = proposals.stream()
                    .map(ProposalSummaryResponse::from)
                    .toList();

            return ProposalsInfo.builder()
                    .totalCount(proposals.size())
                    .viewedCount(null)
                    .items(summaries)
                    .accessLevel("PUBLIC")
                    .build();
        }

        // 1. 견적 요청자 본인인 경우 - 모든 제안 조회 (WITHDRAWN 제외)
        if (estimateRequest.getUser().getId().equals(user.getId())) {
            List<EstimateProposal> proposals = proposalRepository.findByRequestAndStatusNotWithdrawn(estimateRequest);

            long viewedCount = proposals.stream()
                    .filter(p -> "VIEWED".equals(p.getStatus()) ||
                                "SELECTED".equals(p.getStatus()) ||
                                "REJECTED".equals(p.getStatus()))
                    .count();

            List<ProposalResponse> proposalResponses = proposals.stream()
                    .map(ProposalResponse::from)
                    .toList();

            return ProposalsInfo.builder()
                    .totalCount(proposals.size())
                    .viewedCount((int) viewedCount)
                    .items(proposalResponses)
                    .accessLevel("OWNER")
                    .build();
        }

        // 2. 업체 회원인 경우 - 자신의 제안만 조회
        if (user.hasRole("COMPANY")) {
            Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                    .orElse(null);

            if (company != null) {
                List<ProposalResponse> myProposals = new ArrayList<>();
                proposalRepository.findByRequestAndCompanyAndIsDeletedFalse(estimateRequest, company)
                        .ifPresent(proposal -> myProposals.add(ProposalResponse.from(proposal)));

                return ProposalsInfo.builder()
                        .totalCount(estimateRequest.getProposalCount())
                        .viewedCount(null) // 본인 제안만 보므로 의미 없음
                        .items(myProposals)
                        .accessLevel("PROPOSER")
                        .build();
            }
        }

        // 3. 기타 회원 - 제안 요약만 조회
        List<EstimateProposal> proposals = proposalRepository.findByRequestAndStatusNotWithdrawn(estimateRequest);

        List<ProposalSummaryResponse> summaries = proposals.stream()
                .map(ProposalSummaryResponse::from)
                .toList();

        return ProposalsInfo.builder()
                .totalCount(proposals.size())
                .viewedCount(null) // 기타 회원은 확인 불가
                .items(summaries)
                .accessLevel("PUBLIC")
                .build();
    }

    /**
     * 견적 요청 삭제 (UUID 사용)
     */
    @Transactional
    public void deleteEstimateRequestByUuid(String userEmail, UUID requestUuid) {
        log.info("견적 요청 삭제 (UUID): requestUuid={}, userEmail={}", requestUuid, userEmail);

        // UUID로 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 기존 deleteEstimateRequest 메서드 호출
        deleteEstimateRequest(userEmail, estimateRequest.getId());
    }

    /**
     * 위치 정보 문자열 생성
     */
    private String buildLocation(String address, String city, String state) {
        StringBuilder location = new StringBuilder();
        if (state != null && !state.isEmpty()) {
            location.append(state);
        }
        if (city != null && !city.isEmpty()) {
            if (location.length() > 0) location.append(" ");
            location.append(city);
        }
        if (address != null && !address.isEmpty()) {
            if (location.length() > 0) location.append(" ");
            location.append(address);
        }
        return location.toString();
    }

    // ===== 첨부파일 처리 메서드 (V30 Migration) =====

    /**
     * 첨부파일 리스트 처리 (UUID → File ID 변환 및 조인 테이블 저장)
     * 기존 첨부파일과 비교하여 변경된 것만 처리:
     * - 유지할 파일: 그대로 둠
     * - 삭제할 파일: soft delete
     * - 추가할 파일: insert
     */
    private void processAttachments(EstimateRequest estimateRequest, List<AttachmentRequest> attachmentRequests) {
        log.info("첨부파일 처리 시작: requestId={}, count={}", estimateRequest.getId(), attachmentRequests.size());

        // 1. 기존 첨부파일 조회 (삭제되지 않은 것만)
        List<EstimateRequestAttachment> existingAttachments = attachmentRepository
                .findByEstimateRequestIdAndIsDeletedFalseOrderByDisplayOrderAsc(estimateRequest.getId());

        // 기존 첨부파일의 File ID Set
        java.util.Set<Long> existingFileIds = existingAttachments.stream()
                .map(EstimateRequestAttachment::getFileId)
                .collect(java.util.stream.Collectors.toSet());

        // 2. 새 첨부파일 UUID를 File ID로 변환
        java.util.Set<Long> newFileIds = new java.util.HashSet<>();
        java.util.Map<Long, AttachmentRequest> fileIdToRequestMap = new java.util.HashMap<>();

        for (AttachmentRequest req : attachmentRequests) {
            if (req.getFileUuid() != null && !req.getFileUuid().isEmpty()) {
                Long fileId = convertUuidToFileId(req.getFileUuid());
                newFileIds.add(fileId);
                fileIdToRequestMap.put(fileId, req);
            }
        }

        // 3. 삭제할 첨부파일 처리 (기존에 있지만 새 목록에 없는 것)
        int deletedCount = 0;
        for (EstimateRequestAttachment existing : existingAttachments) {
            if (!newFileIds.contains(existing.getFileId())) {
                existing.softDelete();
                attachmentRepository.save(existing);
                deletedCount++;
            }
        }
        log.info("삭제된 첨부파일 수: {}", deletedCount);

        // 4. 추가할 첨부파일 처리 (새 목록에 있지만 기존에 없는 것)
        int addedCount = 0;
        int displayOrder = 0;

        for (AttachmentRequest req : attachmentRequests) {
            if (req.getFileUuid() != null && !req.getFileUuid().isEmpty()) {
                Long fileId = convertUuidToFileId(req.getFileUuid());

                // 기존에 없는 파일만 추가
                if (!existingFileIds.contains(fileId)) {
                    // Files 테이블의 entityId 업데이트 (스케줄러 삭제 방지)
                    File file = fileRepository.findById(fileId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
                    file.updateEntityInfo("ESTIMATE_ATTACHMENT", estimateRequest.getId());
                    fileRepository.save(file);

                    // displayOrder가 null이면 인덱스 사용
                    Integer order = req.getDisplayOrder() != null ? req.getDisplayOrder() : displayOrder;

                    // EstimateRequestAttachment 엔티티 생성
                    EstimateRequestAttachment attachment = EstimateRequestAttachment.builder()
                            .estimateRequest(estimateRequest)
                            .fileId(fileId)
                            .fileType(req.getFileType())
                            .fileDescription(req.getFileDescription())
                            .displayOrder(order)
                            .build();

                    attachmentRepository.save(attachment);
                    addedCount++;
                }
                displayOrder++;
            }
        }
        log.info("추가된 첨부파일 수: {}", addedCount);
        log.info("첨부파일 처리 완료: requestId={}, 삭제={}, 추가={}", estimateRequest.getId(), deletedCount, addedCount);
    }

    /**
     * EstimateRequest의 첨부파일을 AttachmentResponse 리스트로 변환
     * (File ID → URL 변환 + File 정보 포함)
     */
    public List<AttachmentResponse> getAttachmentResponses(EstimateRequest estimateRequest) {
        if (estimateRequest.getAttachments() == null || estimateRequest.getAttachments().isEmpty()) {
            return new ArrayList<>();
        }

        return estimateRequest.getAttachments().stream()
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

    /**
     * EstimateRequest의 User로부터 userName 조회
     * UserProfile이 있으면 name 반환, 없으면 email 반환
     */
    private String getUserName(EstimateRequest request) {
        if (request.getUser() == null) {
            return null;
        }

        return userProfileRepository.findByUserId(request.getUser().getId())
                .map(com.hip.damoa.domain.user.model.UserProfile::getName)
                .orElse(request.getUser().getEmail());
    }
}
