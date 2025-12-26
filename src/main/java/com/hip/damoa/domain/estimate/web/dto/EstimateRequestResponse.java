package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.core.util.ResponseUtils;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.file.web.dto.FileUploadResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 견적 요청 상세 응답 DTO
 */
@Slf4j
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateRequestResponse {

    private UUID uuid;
    private UUID userUuid;
    private String userEmail;
    private String userName;
    private String title;
    private String description;
    private Map<String, Object> requirements;
    @Builder.Default
    private String[] tags = new String[0];
    @Builder.Default
    private String[] requiredSkills = new String[0];
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDate desiredStartDate;
    private LocalDate desiredEndDate;
    private String location;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    @Builder.Default
    private List<EstimateImageDto> images = List.of();
    private String status;
    private String visibility;
    private Integer proposalCount;
    private Integer viewCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> metadata;

    // ===== 범용 필드 추가 (V26) =====
    private String clientName; // 사업장명/고객명
    private String businessType; // 업종
    private BigDecimal areaPyeong; // 평수
    private String contactName; // 신청자 이름
    private String contactPhone; // 연락처
    private LocalDateTime submissionDeadline; // 제안 마감일
    @Builder.Default
    private List<AttachmentResponse> attachments = List.of(); // 첨부파일 목록 (V30: 조인 테이블)

    // Backward compatibility: old signature without images parameter
    public static EstimateRequestResponse from(EstimateRequest request, String userName) {
        return from(request, userName, List.of());
    }

    /**
     * Entity → DTO 변환
     * 삭제된 User의 경우 안전하게 처리
     */
    public static EstimateRequestResponse from(EstimateRequest request, String userName, List<EstimateImageDto> images) {
        UUID userUuid = null;
        String userEmail = null;
        String resolvedUserName = userName;

        try {
            if (request.getUser() != null) {
                userUuid = request.getUser().getUuid();
                userEmail = request.getUser().getEmail();
                if (resolvedUserName == null) {
                    resolvedUserName = userEmail;
                }
            }
        } catch (Exception e) {
            log.warn("User not found for estimateRequest: {}", request.getId());
            userEmail = "알 수 없음";
            resolvedUserName = "알 수 없음";
        }

        return EstimateRequestResponse.builder()
                .uuid(request.getUuid())
                .userUuid(userUuid)
                .userEmail(ResponseUtils.safe(userEmail))
                .userName(ResponseUtils.safe(resolvedUserName))
                .title(ResponseUtils.safe(request.getTitle()))
                .description(ResponseUtils.safe(request.getDescription()))
                .requirements(ResponseUtils.safeMap(request.getRequirements()))
                .tags(ResponseUtils.safeArray(request.getTags()))
                .requiredSkills(ResponseUtils.safeArray(request.getRequiredSkills()))
                .budgetMin(ResponseUtils.safe(request.getBudgetMin()))
                .budgetMax(ResponseUtils.safe(request.getBudgetMax()))
                .desiredStartDate(request.getDesiredStartDate())
                .desiredEndDate(request.getDesiredEndDate())
                .location(ResponseUtils.safe(request.getLocation()))
                .address(ResponseUtils.safe(request.getAddress()))
                .latitude(ResponseUtils.safe(request.getLatitude()))
                .longitude(ResponseUtils.safe(request.getLongitude()))
                .images(ResponseUtils.safeList(images))
                .status(ResponseUtils.safe(request.getStatusString(), "OPEN"))
                .visibility(ResponseUtils.safe(request.getVisibility(), "PUBLIC"))
                .proposalCount(ResponseUtils.safe(request.getProposalCount()))
                .viewCount(ResponseUtils.safe(request.getViewCount()))
                .expiresAt(request.getExpiresAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .metadata(ResponseUtils.safeMap(request.getMetadata()))
                // V26 fields
                .clientName(ResponseUtils.safe(request.getClientName()))
                .businessType(ResponseUtils.safe(request.getBusinessType()))
                .areaPyeong(ResponseUtils.safe(request.getAreaPyeong()))
                .contactName(ResponseUtils.safe(request.getContactName()))
                .contactPhone(ResponseUtils.safe(request.getContactPhone()))
                .submissionDeadline(request.getSubmissionDeadline())
                // attachments는 Service에서 별도로 설정
                .build();
    }
}
