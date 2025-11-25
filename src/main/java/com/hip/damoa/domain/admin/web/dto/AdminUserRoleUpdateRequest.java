package com.hip.damoa.domain.admin.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자용 회원 역할 변경 Request
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRoleUpdateRequest {

    @NotEmpty(message = "역할 배열은 비어있을 수 없습니다")
    private String[] roles;

    // Validation은 Service에서 처리 (USER, COMPANY, ADMIN만 허용)
}
