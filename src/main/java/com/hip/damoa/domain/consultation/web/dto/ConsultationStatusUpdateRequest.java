package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상담 상태 변경 요청 DTO (관리자용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationStatusUpdateRequest {

    @NotNull(message = "상태는 필수입니다")
    private ConsultationStatus status;

    private String notes; // 상태 변경 메모
}
