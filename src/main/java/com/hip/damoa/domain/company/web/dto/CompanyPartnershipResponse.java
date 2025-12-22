package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyPartnership;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제휴업체 응답 DTO (관리자용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPartnershipResponse {

    private UUID uuid;
    private UUID companyUuid;
    private String companyName;
    private String companySlug;
    private Integer displayOrder;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String adminMemo;
    private String registeredByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static CompanyPartnershipResponse from(CompanyPartnership partnership) {
        return CompanyPartnershipResponse.builder()
                .uuid(partnership.getUuid())
                .companyUuid(partnership.getCompany().getUuid())
                .companyName(partnership.getCompany().getName())
                .companySlug(partnership.getCompany().getSlug())
                .displayOrder(partnership.getDisplayOrder())
                .startDate(partnership.getStartDate())
                .endDate(partnership.getEndDate())
                .status(partnership.getStatus())
                .adminMemo(partnership.getAdminMemo())
                .registeredByEmail(partnership.getRegisteredBy() != null
                        ? partnership.getRegisteredBy().getEmail() : null)
                .createdAt(partnership.getCreatedAt())
                .updatedAt(partnership.getUpdatedAt())
                .build();
    }
}
