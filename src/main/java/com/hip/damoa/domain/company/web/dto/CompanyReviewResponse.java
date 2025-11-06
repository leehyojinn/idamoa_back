package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReviewResponse {

    private Long id;
    private Long companyId;
    private String companyName;

    // 작성자 정보
    private Long userId;
    private String userEmail;

    // 리뷰 내용
    private BigDecimal rating;
    private String title;
    private String content;
    private String[] images;

    // 업체 답변
    private String reply;
    private LocalDateTime repliedAt;

    // 통계
    private Integer likeCount;
    private Integer reportCount;

    // 상태
    private String status;

    // 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyReviewResponse from(CompanyReview review) {
        return CompanyReviewResponse.builder()
                .id(review.getId())
                .companyId(review.getCompany().getId())
                .companyName(review.getCompany().getName())
                .userId(review.getUser().getId())
                .userEmail(review.getUser().getEmail())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .images(review.getImages())
                .reply(review.getReply())
                .repliedAt(review.getRepliedAt())
                .likeCount(review.getLikeCount())
                .reportCount(review.getReportCount())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
