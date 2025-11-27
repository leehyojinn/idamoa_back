package com.hip.damoa.domain.inquiry.web.dto;

import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.model.InquiryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 일반 문의 검색 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquirySearchRequest {

    // 검색 키워드 (제목, 내용)
    private String keyword;

    // 문의 유형
    private InquiryType inquiryType;

    // 문의 상태
    private InquiryStatus status;

    // 작성자 이메일 (관리자용)
    private String userEmail;

    // 날짜 범위
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    // 답변 여부
    private Boolean hasAnswer;
}