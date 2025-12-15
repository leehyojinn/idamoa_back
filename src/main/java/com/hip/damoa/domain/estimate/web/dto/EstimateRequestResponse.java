package com.hip.damoa.domain.estimate.web.dto;

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

    private Long id;  // 클라이언트 편의를 위해 ID도 포함
    private UUID uuid;
    private Long userId;
    private String userEmail;
    private String userName;
    private String title;
    private String description;
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
    private List<AttachmentResponse> attachments; // 첨부파일 목록 (V30: 조인 테이블)

    // Backward compatibility: old signature without images parameter
    public static EstimateRequestResponse from(EstimateRequest request, String userName) {
        return from(request, userName, List.of());
    }

    /**
     * Entity → DTO 변환
     * 삭제된 User의 경우 안전하게 처리
     */
    public static EstimateRequestResponse from(EstimateRequest request, String userName, List<EstimateImageDto> images) {
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
            userEmail = "[삭제된 사용자]";
            resolvedUserName = "[삭제된 사용자]";
        }

        return EstimateRequestResponse.builder()
                .id(request.getId())
                .uuid(request.getUuid())
                .userId(userId)
                .userEmail(userEmail)
                .userName(resolvedUserName)
                .title(request.getTitle())
                .description(request.getDescription())
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
                .visibility(request.getVisibility())
                .proposalCount(request.getProposalCount())
                .viewCount(request.getViewCount())
                .expiresAt(request.getExpiresAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .metadata(request.getMetadata())
                // V26 fields
                .clientName(request.getClientName())
                .businessType(request.getBusinessType())
                .areaPyeong(request.getAreaPyeong())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .submissionDeadline(request.getSubmissionDeadline())
                // attachments는 Service에서 별도로 설정
                .build();
    }
}
