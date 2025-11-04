package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.company.model.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * COMPANY 프로필 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileResponse {

    private Long id;
    private String name;
    private String description;
    private String primaryPhone;
    private String email;
    private String address;
    private String postalCode;
    private String status;

    // 프로필 생성 후 새로운 JWT 토큰 (profileCompleted=true, currentRole=COMPANY 업데이트)
    private TokenInfo tokenInfo;

    /**
     * Entity → DTO 변환
     */
    public static CompanyProfileResponse from(Company company) {
        return CompanyProfileResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .description(company.getDescription())
                .primaryPhone(company.getPrimaryPhone())
                .email(company.getEmail())
                .address(company.getAddress())
                .postalCode(company.getPostalCode())
                .status(company.getStatus())
                .build();
    }
}
