package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 빠른상담 목록 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빠른상담 목록 응답")
public class QuickConsultationListResponse {

    @Schema(description = "상담 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    // 신청자 정보 (요약)
    @Schema(description = "신청자 이름", example = "홍길동")
    private String name;

    @Schema(description = "전화번호 (일부 마스킹)", example = "010-****-5678")
    private String phone;

    // 상담 내용 (요약)
    @Schema(description = "상담 제목", example = "인테리어 상담 문의")
    private String subject;

    @Schema(description = "상담 내용 미리보기 (100자)", example = "안녕하세요. 사무실 인테리어 관련해서 문의드립니다...")
    private String messagePreview;

    // 상태
    @Schema(description = "상담 상태 (SUBMITTED, IN_PROGRESS, COMPLETED, CANCELLED)", example = "SUBMITTED")
    private ConsultationStatus status;

    // 답변 여부
    @Schema(description = "관리자 답변 여부", example = "false")
    private Boolean hasResponse;

    // 본인 상담 여부 (로그인 사용자 기준)
    @Schema(description = "본인이 작성한 상담 여부 (로그인 시)", example = "true")
    private Boolean isMyConsultation;

    // 시간 정보
    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-01T12:00:00")
    private LocalDateTime updatedAt;

    // 삭제 상태 (관리자용)
    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    /**
     * Entity → DTO 변환 (비로그인 사용자용)
     */
    public static QuickConsultationListResponse from(QuickConsultation consultation) {
        return from(consultation, null);
    }

    /**
     * Entity → DTO 변환 (로그인 사용자용)
     * @param consultation 상담 엔티티
     * @param currentUserId 현재 로그인한 사용자 ID (비로그인 시 null)
     */
    public static QuickConsultationListResponse from(QuickConsultation consultation, Long currentUserId) {
        String messagePreview = consultation.getMessage();
        if (messagePreview != null && messagePreview.length() > 100) {
            messagePreview = messagePreview.substring(0, 100) + "...";
        }

        // 본인 상담 여부 판단
        boolean isMyConsultation = false;
        if (currentUserId != null && consultation.getUser() != null) {
            isMyConsultation = consultation.isOwnedBy(currentUserId);
        }

        return QuickConsultationListResponse.builder()
                .uuid(consultation.getUuid())
                .name(consultation.getName())
                .phone(consultation.getPhone())
                .subject(consultation.getSubject())
                .messagePreview(messagePreview)
                .status(consultation.getStatus())
                .hasResponse(consultation.getResponseMessage() != null)
                .isMyConsultation(isMyConsultation)
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .isDeleted(consultation.getIsDeleted())
                .build();
    }
}
