package com.hip.damoa.domain.estimate.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestCreateRequest;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestUpdateRequest;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 견적 요청 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstimateRequestService {

    private final EstimateRequestRepository estimateRequestRepository;
    private final UserRepository userRepository;

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
        if (request.getEstimateType() != null) {
            requirements.put("estimateType", request.getEstimateType());
        }
        if (request.getDesiredStartDate() != null) {
            requirements.put("desiredStartDate", request.getDesiredStartDate().toString());
        }
        if (request.getDesiredCompletionDate() != null) {
            requirements.put("desiredCompletionDate", request.getDesiredCompletionDate().toString());
        }

        // 견적 요청 생성
        EstimateRequest estimateRequest = EstimateRequest.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(requirements)
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .location(location)
                .deadline(request.getSubmissionDeadline())
                .visibility(Boolean.TRUE.equals(request.getIsPublic()) ? "PUBLIC" : "PRIVATE")
                .status("DRAFT")
                .build();

        estimateRequest = estimateRequestRepository.save(estimateRequest);

        log.info("견적 요청 생성 완료: id={}, title={}", estimateRequest.getId(), estimateRequest.getTitle());

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

        // 상태 확인 (DRAFT 상태만 수정 가능)
        if (!"DRAFT".equals(estimateRequest.getStatus())) {
            throw new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_EDITABLE);
        }

        // 위치 정보 업데이트
        if (request.getSiteAddress() != null || request.getSiteCity() != null || request.getSiteState() != null) {
            String location = buildLocation(
                    request.getSiteAddress() != null ? request.getSiteAddress() : "",
                    request.getSiteCity() != null ? request.getSiteCity() : "",
                    request.getSiteState() != null ? request.getSiteState() : ""
            );

            // Entity를 재생성 (Lombok @Builder 사용)
            Map<String, Object> requirements = estimateRequest.getRequirements() != null
                ? new HashMap<>(estimateRequest.getRequirements())
                : new HashMap<>();

            if (request.getAreaSqm() != null) {
                requirements.put("areaSqm", request.getAreaSqm());
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

            estimateRequest = EstimateRequest.builder()
                    .user(estimateRequest.getUser())
                    .title(request.getTitle() != null ? request.getTitle() : estimateRequest.getTitle())
                    .description(request.getDescription() != null ? request.getDescription() : estimateRequest.getDescription())
                    .requirements(requirements)
                    .tags(estimateRequest.getTags())
                    .requiredSkills(estimateRequest.getRequiredSkills())
                    .budgetMin(request.getBudgetMin() != null ? request.getBudgetMin() : estimateRequest.getBudgetMin())
                    .budgetMax(request.getBudgetMax() != null ? request.getBudgetMax() : estimateRequest.getBudgetMax())
                    .budgetType(estimateRequest.getBudgetType())
                    .preferredStartDate(request.getDesiredStartDate())
                    .expectedDurationDays(estimateRequest.getExpectedDurationDays())
                    .location(location)
                    .postalCode(estimateRequest.getPostalCode())
                    .status(estimateRequest.getStatus())
                    .visibility(request.getIsPublic() != null ?
                        (Boolean.TRUE.equals(request.getIsPublic()) ? "PUBLIC" : "PRIVATE") :
                        estimateRequest.getVisibility())
                    .proposalCount(estimateRequest.getProposalCount())
                    .viewCount(estimateRequest.getViewCount())
                    .publishedAt(estimateRequest.getPublishedAt())
                    .deadline(request.getSubmissionDeadline() != null ? request.getSubmissionDeadline() : estimateRequest.getDeadline())
                    .completedAt(estimateRequest.getCompletedAt())
                    .build();

            estimateRequest = estimateRequestRepository.save(estimateRequest);
        }

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
        if (!"DRAFT".equals(estimateRequest.getStatus())) {
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
     * 공개 견적 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<EstimateRequest> getPublicEstimateRequests(Pageable pageable) {
        log.info("공개 견적 요청 목록 조회");
        return estimateRequestRepository.findByStatusAndVisibilityAndIsDeletedFalse("PUBLISHED", "PUBLIC", pageable);
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

        // Soft Delete
        estimateRequest.softDelete();
        estimateRequestRepository.save(estimateRequest);

        log.info("견적 요청 삭제 완료: id={}", estimateRequest.getId());
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
}
