package com.hip.damoa.domain.company.web.dto.dashboard;

import com.hip.damoa.domain.consultation.model.PortfolioConsultation;
import com.hip.damoa.domain.consultation.model.PortfolioConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 상담신청 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationStatsResponse {

    private long total;
    private long pending;
    private long inProgress;
    private long answered;
    private long completed;
    private long cancelled;

    private List<RecentConsultation> recentConsultations;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentConsultation {
        private UUID uuid;
        private String name;
        private String title;
        private PortfolioConsultationStatus status;
        private LocalDateTime createdAt;

        public static RecentConsultation from(PortfolioConsultation consultation) {
            return RecentConsultation.builder()
                    .uuid(consultation.getUuid())
                    .name(consultation.getName())
                    .title(consultation.getTitle())
                    .status(consultation.getStatus())
                    .createdAt(consultation.getCreatedAt())
                    .build();
        }
    }

    /**
     * 요약용 (전체 대시보드 요약에 포함)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long total;
        private long pending;
        private long inProgress;
        private long answered;
        private long completed;
        private long cancelled;
    }
}
