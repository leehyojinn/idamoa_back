package com.hip.damoa.domain.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자용 회원 상태 변경 Request
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatusUpdateRequest {

    @NotBlank(message = "상태는 필수입니다")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|SUSPENDED|PENDING)$",
            message = "상태는 ACTIVE, INACTIVE, SUSPENDED, PENDING 중 하나여야 합니다")
    private String status;

    private String reason; // 상태 변경 사유 (선택)
}
