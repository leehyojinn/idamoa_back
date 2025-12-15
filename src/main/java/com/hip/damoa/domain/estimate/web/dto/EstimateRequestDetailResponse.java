package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * 견적 요청 상세 응답 DTO (제안 목록 포함)
 */
@Slf4j
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "견적 요청 상세 정보 (제안 목록 포함)")
public class EstimateRequestDetailResponse {

    // ===== 견적 요청 기본 정보 =====

    @Schema(description = "견적 요청 ID")
    private Long id;

    @Schema(description = "견적 요청 UUID")
    private UUID uuid;

    @Schema(description = "작성자 ID")
    private Long userId;

    @Schema(description = "작성자 이메일")
    private String userEmail;

    @Schema(description = "작성자 이름")
    private String userName;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "상세 설명")
    private String description;

    @Schema(description = "요구사항 (JSON)")
    private Map<String, Object> requirements;

    @Schema(description = "태그")
    private String[] tags;

    @Schema(description = "필요 기술")
    private String[] requiredSkills;

    @Schema(description = "최소 예산")
    private BigDecimal budgetMin;

    @Schema(description = "최대 예산")
    private BigDecimal budgetMax;

    @Schema(description = "희망 시작일")
    private LocalDate desiredStartDate;

    @Schema(description = "희망 종료일")
    private LocalDate desiredEndDate;

    @Schema(description = "위치")
    private String location;

    @Schema(description = "주소")
    private String address;

    @Schema(description = "위도")
    private BigDecimal latitude;

    @Schema(description = "경도")
    private BigDecimal longitude;

    @Schema(description = "이미지 목록")
    private List<EstimateImageDto> images;

    @Schema(description = "상태")
    private String status;

    @Schema(description = "공개 여부")
    private String visibility;

    @Schema(description = "조회수")
    private Integer viewCount;

    @Schema(description = "만료일")
    private LocalDateTime expiresAt;

    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    @Schema(description = "수정일")
    private LocalDateTime updatedAt;

    @Schema(description = "메타데이터 (JSON)")
    private Map<String, Object> metadata;

    // ===== 범용 필드 (V26) =====

    @Schema(description = "사업장명")
    private String clientName;

    @Schema(description = "업종")
    private String businessType;

    @Schema(description = "평수")
    private BigDecimal areaPyeong;

    @Schema(description = "신청자 이름")
    private String contactName;

    @Schema(description = "연락처")
    private String contactPhone;

    @Schema(description = "제안 마감일")
    private LocalDateTime submissionDeadline;

    @Schema(description = "첨부파일 목록")
    private List<AttachmentResponse> attachments;

    // ===== 제안 정보 (중첩) =====

    @Schema(description = "제안 목록 정보 (권한에 따라 다른 레벨의 정보)")
    private ProposalsInfo proposals;

    /**
     * EstimateRequest → DetailResponse 변환 (backward compatibility)
     */
    public static EstimateRequestDetailResponse from(EstimateRequest request, String userName) {
        return from(request, userName, List.of());
    }

    /**
     * EstimateRequest → DetailResponse 변환
     * 삭제된 User의 경우 안전하게 처리
     */
    public static EstimateRequestDetailResponse from(EstimateRequest request, String userName, List<EstimateImageDto> images) {
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

        return EstimateRequestDetailResponse.builder()
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
                // attachments와 proposals는 Service에서 별도로 설정
                .build();
    }
}
