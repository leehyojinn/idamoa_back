package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
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
public class QuickConsultationListResponse {

    private UUID uuid;

    // 신청자 정보 (요약)
    private String name;
    private String phone;

    // 상담 내용 (요약)
    private String subject;
    private String messagePreview; // 메시지 앞 100자

    // 상태
    private ConsultationStatus status;

    // 답변 여부
    private Boolean hasResponse;

    // 본인 상담 여부 (로그인 사용자 기준)
    private Boolean isMyConsultation;

    // 시간 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 삭제 상태 (관리자용)
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
