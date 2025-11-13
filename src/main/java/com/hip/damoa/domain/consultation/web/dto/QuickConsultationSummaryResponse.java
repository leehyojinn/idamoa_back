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
 * 빠른상담 요약 응답 DTO (비밀번호 검증 전)
 *
 * 비회원이 비밀번호 입력 전에 조회할 수 있는 최소 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickConsultationSummaryResponse {

    private UUID uuid;
    private String name;
    private String subject;
    private ConsultationStatus status;
    private LocalDateTime createdAt;

    public static QuickConsultationSummaryResponse from(QuickConsultation consultation) {
        return QuickConsultationSummaryResponse.builder()
                .uuid(consultation.getUuid())
                .name(consultation.getName())
                .subject(consultation.getSubject())
                .status(consultation.getStatus())
                .createdAt(consultation.getCreatedAt())
                .build();
    }
}
