package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.web.dto.AttachmentResponse;
import com.hip.damoa.domain.estimate.web.dto.EstimateImageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자용 견적 요청 상세 응답 DTO (내부 ID 포함)
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEstimateRequestResponse {

    private Long id;  // 관리자용 내부 ID
    private UUID uuid;
    private Long userId;
    private String userEmail;
    private String userName;
    private String title;
    private String description;
    private String category;
    private Map<String, Object> requirements;
    private String[] tags;
    private String[] requiredSkills;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDate desiredStartDate;
    private LocalDate desiredEndDate;
    private String location;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<EstimateImageDto> images;
    private String status;
    private Boolean isPublic;
    private Integer proposalCount;
    private Integer viewCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> metadata;

    // V26 추가 필드
    private String clientName;
    private String businessType;
    private BigDecimal areaPyeong;
    private String contactName;
    private String contactPhone;
    private LocalDateTime submissionDeadline;
    private List<AttachmentResponse> attachments; // 첨부파일 (V30: 조인 테이블)

    // Soft delete 관련
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    // Backward compatibility: old signature without images parameter
    public static AdminEstimateRequestResponse from(EstimateRequest request, String userName) {
        return from(request, userName, List.of());
    }

    /**
     * Entity → DTO 변환
     * 삭제된 User의 경우 안전하게 처리
     */
    public static AdminEstimateRequestResponse from(EstimateRequest request, String userName, List<EstimateImageDto> images) {
        Long userId = null;
        String userEmail = null;
        String resolvedUserName = userName;

        try {
            if (request.getUser() != null) {
                userId = request.getUser().getId();
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

        return AdminEstimateRequestResponse.builder()
                .id(request.getId())
                .uuid(request.getUuid())
                .userId(userId)
                .userEmail(userEmail)
                .userName(resolvedUserName)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .requirements(request.getRequirements())
                .tags(request.getTags())
                .requiredSkills(request.getRequiredSkills())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .desiredStartDate(request.getDesiredStartDate())
                .desiredEndDate(request.getDesiredEndDate())
                .location(request.getLocation())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .images(images != null ? images : List.of())
                .status(request.getStatusString())
                .isPublic(request.getIsPublic())
                .proposalCount(request.getProposalCount())
                .viewCount(request.getViewCount())
                .expiresAt(request.getExpiresAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .metadata(request.getMetadata())
                .clientName(request.getClientName())
                .businessType(request.getBusinessType())
                .areaPyeong(request.getAreaPyeong())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .submissionDeadline(request.getSubmissionDeadline())
                // attachments는 Service에서 별도로 설정
                .isDeleted(request.getIsDeleted())
                .deletedAt(request.getDeletedAt())
                .build();
    }
}