package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.ContactMethod;
import com.hip.damoa.domain.consultation.model.PortfolioConsultation;
import com.hip.damoa.domain.consultation.model.PortfolioConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioConsultationResponse {

    private UUID uuid;
    private UUID portfolioUuid;
    private String portfolioTitle;
    private UUID companyUuid;
    private String companyName;

    // 신청자 유저 정보
    private UUID userUuid;
    private String userEmail;

    // 신청자 입력 정보
    private String name;
    private String phone;
    private String email;

    // 상담 내용
    private String title;
    private String content;
    private ContactMethod contactMethod;
    private String availableTime;

    // 상태
    private PortfolioConsultationStatus status;

    // 업체 메모 (업체용)
    private String companyMemo;

    // 답변
    private String answer;
    private LocalDateTime answeredAt;

    // 답변자 정보
    private UUID answeredByUuid;
    private String answeredByEmail;

    // 삭제 여부 (Admin용)
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 업체용 응답 (전체 정보)
     */
    public static PortfolioConsultationResponse from(PortfolioConsultation consultation) {
        PortfolioConsultationResponseBuilder builder = PortfolioConsultationResponse.builder()
                .uuid(consultation.getUuid())
                .portfolioUuid(consultation.getPortfolio().getUuid())
                .portfolioTitle(consultation.getPortfolio().getTitle())
                .companyUuid(consultation.getCompany().getUuid())
                .companyName(consultation.getCompany().getName())
                // 신청자 유저 정보
                .userUuid(consultation.getUser() != null ? consultation.getUser().getUuid() : null)
                .userEmail(consultation.getUser() != null ? consultation.getUser().getEmail() : null)
                // 신청자 입력 정보
                .name(consultation.getName())
                .phone(consultation.getPhone())
                .email(consultation.getEmail())
                .title(consultation.getTitle())
                .content(consultation.getContent())
                .contactMethod(consultation.getContactMethod())
                .availableTime(consultation.getAvailableTime())
                .status(consultation.getStatus())
                .companyMemo(consultation.getCompanyMemo())
                .answer(consultation.getAnswer())
                .answeredAt(consultation.getAnsweredAt())
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt());

        // 답변자 정보
        if (consultation.getAnsweredBy() != null) {
            builder.answeredByUuid(consultation.getAnsweredBy().getUuid())
                    .answeredByEmail(consultation.getAnsweredBy().getEmail());
        }

        return builder.build();
    }

    /**
     * 사용자용 응답 (업체 메모, 답변자 정보 제외)
     */
    public static PortfolioConsultationResponse forUser(PortfolioConsultation consultation) {
        return PortfolioConsultationResponse.builder()
                .uuid(consultation.getUuid())
                .portfolioUuid(consultation.getPortfolio().getUuid())
                .portfolioTitle(consultation.getPortfolio().getTitle())
                .companyUuid(consultation.getCompany().getUuid())
                .companyName(consultation.getCompany().getName())
                // 신청자 유저 정보
                .userUuid(consultation.getUser() != null ? consultation.getUser().getUuid() : null)
                .userEmail(consultation.getUser() != null ? consultation.getUser().getEmail() : null)
                // 신청자 입력 정보
                .name(consultation.getName())
                .phone(consultation.getPhone())
                .email(consultation.getEmail())
                .title(consultation.getTitle())
                .content(consultation.getContent())
                .contactMethod(consultation.getContactMethod())
                .availableTime(consultation.getAvailableTime())
                .status(consultation.getStatus())
                .companyMemo(null) // 사용자에게는 업체 메모 노출 안함
                .answer(consultation.getAnswer())
                .answeredAt(consultation.getAnsweredAt())
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }

    /**
     * 관리자용 응답 (삭제 여부 포함)
     */
    public static PortfolioConsultationResponse forAdmin(PortfolioConsultation consultation) {
        PortfolioConsultationResponseBuilder builder = PortfolioConsultationResponse.builder()
                .uuid(consultation.getUuid())
                .portfolioUuid(consultation.getPortfolio().getUuid())
                .portfolioTitle(consultation.getPortfolio().getTitle())
                .companyUuid(consultation.getCompany().getUuid())
                .companyName(consultation.getCompany().getName())
                // 신청자 유저 정보
                .userUuid(consultation.getUser() != null ? consultation.getUser().getUuid() : null)
                .userEmail(consultation.getUser() != null ? consultation.getUser().getEmail() : null)
                // 신청자 입력 정보
                .name(consultation.getName())
                .phone(consultation.getPhone())
                .email(consultation.getEmail())
                .title(consultation.getTitle())
                .content(consultation.getContent())
                .contactMethod(consultation.getContactMethod())
                .availableTime(consultation.getAvailableTime())
                .status(consultation.getStatus())
                .companyMemo(consultation.getCompanyMemo())
                .answer(consultation.getAnswer())
                .answeredAt(consultation.getAnsweredAt())
                // 삭제 여부
                .isDeleted(consultation.getIsDeleted())
                .deletedAt(consultation.getDeletedAt())
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt());

        // 답변자 정보
        if (consultation.getAnsweredBy() != null) {
            builder.answeredByUuid(consultation.getAnsweredBy().getUuid())
                    .answeredByEmail(consultation.getAnsweredBy().getEmail());
        }

        return builder.build();
    }
}
