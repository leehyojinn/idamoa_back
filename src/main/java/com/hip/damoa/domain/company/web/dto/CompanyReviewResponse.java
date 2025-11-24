package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReviewResponse {

    private Long id;
    private UUID uuid;
    private Long companyId;
    private String companyName;

    // 작성자 정보
    private Long userId;
    private String userEmail;
    private String userName;

    // 리뷰 내용
    private BigDecimal rating;
    private String title;
    private String content;
    private List<ReviewImageDto> images;

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
        return from(review, null, review.getUser() != null ? review.getUser().getEmail() : null,
                review.getCompany().getId(), review.getCompany().getName());
    }

    public static CompanyReviewResponse from(CompanyReview review, List<ReviewImageDto> images, String userName) {
        return from(review, images, userName, review.getCompany().getId(), review.getCompany().getName());
    }

    /**
     * Lazy Loading 문제 방지를 위해 Company 정보를 파라미터로 받는 메서드
     */
    public static CompanyReviewResponse from(CompanyReview review, List<ReviewImageDto> images, String userName,
                                              Long companyId, String companyName) {
        return CompanyReviewResponse.builder()
                .id(review.getId())
                .uuid(review.getUuid())
                .companyId(companyId)
                .companyName(companyName)
                .userId(review.getUser().getId())
                .userEmail(review.getUser().getEmail())
                .userName(userName != null ? userName : review.getUser().getEmail())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .images(images != null ? images : List.of())
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
