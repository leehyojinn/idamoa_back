package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.core.util.ResponseUtils;
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

    private java.util.UUID uuid;
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
                .uuid(company.getUuid())
                .name(ResponseUtils.safe(company.getName()))
                .description(ResponseUtils.safe(company.getDescription()))
                .primaryPhone(ResponseUtils.safe(company.getPrimaryPhone()))
                .email(ResponseUtils.safe(company.getEmail()))
                .address(ResponseUtils.safe(company.getAddress()))
                .postalCode(ResponseUtils.safe(company.getPostalCode()))
                .status(ResponseUtils.safe(company.getStatus(), "ACTIVE"))
                .build();
    }
}
