package com.hip.damoa.domain.company.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReviewCreateRequest {

    @NotNull(message = "평점은 필수입니다")
    @DecimalMin(value = "1.0", message = "평점은 1.0 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다")
    private BigDecimal rating;

    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(min = 10, max = 5000, message = "리뷰 내용은 10자 이상 5000자 이하여야 합니다")
    private String content;

    /**
     * 리뷰 이미지 UUID 배열 (파일 업로드 후 받은 UUID)
     * Service 레이어에서 UUID → File ID로 변환 후 company_review_images 테이블에 저장
     */
    private String[] imageUuids;
}
