package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.web.dto.AttachmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자용 견적 요청 상세 응답 DTO (내부 ID 포함)
 */
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
    private String[] images;
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

    public static AdminEstimateRequestResponse from(EstimateRequest request, String userName) {
        return AdminEstimateRequestResponse.builder()
                .id(request.getId())
                .uuid(request.getUuid())
                .userId(request.getUser().getId())
                .userEmail(request.getUser().getEmail())
                .userName(userName != null ? userName : request.getUser().getEmail())
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
                .images(request.getImages())
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