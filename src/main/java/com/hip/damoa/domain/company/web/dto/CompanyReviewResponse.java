package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyReview;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "업체 리뷰 응답")
public class CompanyReviewResponse {

    @Schema(description = "리뷰 내부 ID", example = "1")
    private Long id;

    @Schema(description = "리뷰 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "업체 ID", example = "1")
    private Long companyId;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String companyName;

    // 작성자 정보
    @Schema(description = "작성자 ID", example = "1")
    private Long userId;

    @Schema(description = "작성자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String userName;

    // 리뷰 내용
    @Schema(description = "평점 (1.0~5.0)", example = "4.5")
    private BigDecimal rating;

    @Schema(description = "리뷰 제목", example = "훌륭한 서비스였습니다")
    private String title;

    @Schema(description = "리뷰 내용", example = "친절하고 꼼꼼하게 작업해주셔서 만족합니다.")
    private String content;

    @Schema(description = "리뷰 이미지 목록")
    private List<ReviewImageDto> images;

    // 업체 답변
    @Schema(description = "업체 답변 내용", example = "감사합니다. 좋은 평가 남겨주셔서 감사합니다.")
    private String reply;

    @Schema(description = "업체 답변 작성일시", example = "2025-01-02T14:00:00")
    private LocalDateTime repliedAt;

    // 통계
    @Schema(description = "좋아요 수", example = "15")
    private Integer likeCount;

    @Schema(description = "신고 수", example = "0")
    private Integer reportCount;

    // 상태
    @Schema(description = "리뷰 상태 (VISIBLE, HIDDEN, REPORTED)", example = "VISIBLE")
    private String status;

    // 시간
    @Schema(description = "작성일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-01T12:00:00")
    private LocalDateTime updatedAt;

    /**
     * 사용자 이메일 안전하게 조회 (삭제된 사용자 처리)
     */
    private static String getUserEmailSafe(CompanyReview review) {
        try {
            return review.getUser() != null ? review.getUser().getEmail() : null;
        } catch (Exception e) {
            log.warn("User not found for review: {}", review.getId());
            return "[삭제된 사용자]";
        }
    }

    public static CompanyReviewResponse from(CompanyReview review) {
        return from(review, null, getUserEmailSafe(review),
                review.getCompany().getId(), review.getCompany().getName());
    }

    public static CompanyReviewResponse from(CompanyReview review, List<ReviewImageDto> images, String userName) {
        return from(review, images, userName, review.getCompany().getId(), review.getCompany().getName());
    }

    /**
     * Lazy Loading 문제 방지를 위해 Company 정보를 파라미터로 받는 메서드
     * 삭제된 User의 경우 안전하게 처리
     */
    public static CompanyReviewResponse from(CompanyReview review, List<ReviewImageDto> images, String userName,
                                              Long companyId, String companyName) {
        Long userId = null;
        String userEmail = null;
        String resolvedUserName = userName;

        try {
            if (review.getUser() != null) {
                userId = review.getUser().getId();
                userEmail = review.getUser().getEmail();
                if (resolvedUserName == null) {
                    resolvedUserName = userEmail;
                }
            }
        } catch (Exception e) {
            log.warn("User not found for review: {}", review.getId());
            userEmail = "[삭제된 사용자]";
            resolvedUserName = "[삭제된 사용자]";
        }

        return CompanyReviewResponse.builder()
                .id(review.getId())
                .uuid(review.getUuid())
                .companyId(companyId)
                .companyName(companyName)
                .userId(userId)
                .userEmail(userEmail)
                .userName(resolvedUserName)
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
