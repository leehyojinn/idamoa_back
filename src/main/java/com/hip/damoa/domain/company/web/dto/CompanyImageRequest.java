package com.hip.damoa.domain.company.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업체 이미지 등록/수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyImageRequest {

    @NotBlank(message = "이미지 URL은 필수입니다")
    @Size(max = 500, message = "이미지 URL은 500자를 초과할 수 없습니다")
    private String imageUrl;

    @NotBlank(message = "이미지 타입은 필수입니다")
    private String imageType; // LOGO, COVER, GALLERY, INTERIOR, EXTERIOR, PORTFOLIO

    @Builder.Default
    private Boolean isPrimary = false;

    @Builder.Default
    private Integer displayOrder = 0;

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    private String description;

    private Integer width;

    private Integer height;

    private Long fileSize;
}
