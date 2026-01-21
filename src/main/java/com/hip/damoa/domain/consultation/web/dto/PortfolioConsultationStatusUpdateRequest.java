package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.PortfolioConsultationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioConsultationStatusUpdateRequest {

    @NotNull(message = "상태는 필수입니다")
    private PortfolioConsultationStatus status;
}
