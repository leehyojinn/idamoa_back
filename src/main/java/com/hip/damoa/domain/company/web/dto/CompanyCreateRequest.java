package com.hip.damoa.domain.company.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 업체 등록 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCreateRequest {

    @NotBlank(message = "업체명은 필수입니다")
    @Size(max = 200, message = "업체명은 200자를 초과할 수 없습니다")
    private String name;

    @Size(max = 200, message = "슬러그는 200자를 초과할 수 없습니다")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "슬러그는 소문자, 숫자, 하이픈만 사용 가능합니다")
    private String slug;

    @Size(max = 1000, message = "소개는 1000자를 초과할 수 없습니다")
    private String description;

    private String detailContent;

    private String detailContentFormat; // HTML, MARKDOWN

    private Map<String, Object> businessInfo;

    private Map<String, Object> businessHours;

    private String businessHoursNote;

    private String[] serviceAreas;

    private String[] tags;

    private String[] keywords;

    @NotBlank(message = "대표 전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 02-1234-5678)")
    private String primaryPhone;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String secondaryPhone;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String emergencyContact;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Size(max = 500, message = "웹사이트 URL은 500자를 초과할 수 없습니다")
    private String websiteUrl;

    @Size(max = 500, message = "카카오톡 채팅 URL은 500자를 초과할 수 없습니다")
    private String kakaoChatUrl;

    private Map<String, Object> socialLinks;

    @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다")
    private String address;

    @Size(max = 20, message = "우편번호는 20자를 초과할 수 없습니다")
    private String postalCode;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
