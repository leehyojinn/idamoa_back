package com.hip.damoa.domain.company.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 업체 등록 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "업체 등록 요청")
public class CompanyCreateRequest {

    @Schema(description = "업체명", example = "강남 인테리어", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "업체명은 필수입니다")
    @Size(max = 200, message = "업체명은 200자를 초과할 수 없습니다")
    private String name;

    @Schema(description = "URL 슬러그 (영문 소문자, 숫자, 하이픈만 사용 - 선택)",
            example = "gangnam-interior",
            pattern = "^[a-z0-9-]+$")
    @Size(max = 200, message = "슬러그는 200자를 초과할 수 없습니다")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "슬러그는 소문자, 숫자, 하이픈만 사용 가능합니다")
    private String slug;

    @Schema(description = "업체 소개 (짧은 설명 - 선택)", example = "강남 최고의 인테리어 전문 업체입니다")
    @Size(max = 1000, message = "소개는 1000자를 초과할 수 없습니다")
    private String description;

    @Schema(description = "상세 내용 (긴 설명 - 선택)", example = "## 회사 소개\n저희 업체는 20년 전통의...")
    private String detailContent;

    @Schema(description = "상세 내용 형식", example = "MARKDOWN", allowableValues = {"HTML", "MARKDOWN"})
    private String detailContentFormat;

    @Schema(description = "사업자 정보 (JSON - 선택)",
            example = "{\"businessNumber\": \"123-45-67890\", \"ceoName\": \"홍길동\"}")
    private Map<String, Object> businessInfo;

    @Schema(description = "영업 시간 (JSON - 선택)",
            example = "{\"monday\": \"09:00-18:00\", \"saturday\": \"09:00-15:00\"}")
    private Map<String, Object> businessHours;

    @Schema(description = "영업 시간 비고 (선택)", example = "주말 및 공휴일 휴무")
    private String businessHoursNote;

    @Schema(description = "서비스 지역 배열 (선택)", example = "[\"강남구\", \"서초구\", \"송파구\"]")
    private String[] serviceAreas;

    @Schema(description = "태그 배열 (선택)", example = "[\"인테리어\", \"리모델링\", \"상업공간\"]")
    private String[] tags;

    // keywords 필드 제거 - 통합 검색으로 대체

    @Schema(description = "필터 옵션 ID 목록 (업체 분류, 전문영역 등 - 선택)",
            example = "[1, 2, 5]")
    private List<Long> filterOptionIds;

    @Schema(description = "대표 전화번호", example = "02-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "대표 전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 02-1234-5678)")
    private String primaryPhone;

    @Schema(description = "보조 전화번호 (선택)", example = "02-9876-5432")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String secondaryPhone;

    @Schema(description = "비상 연락처 (선택)", example = "010-1234-5678")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String emergencyContact;

    @Schema(description = "이메일 (선택)", example = "contact@gangnam-interior.com")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "웹사이트 URL (선택)", example = "https://gangnam-interior.com")
    @Size(max = 500, message = "웹사이트 URL은 500자를 초과할 수 없습니다")
    private String websiteUrl;

    @Schema(description = "카카오톡 채팅 URL (선택)", example = "https://pf.kakao.com/_your-kakao-id")
    @Size(max = 500, message = "카카오톡 채팅 URL은 500자를 초과할 수 없습니다")
    private String kakaoChatUrl;

    @Schema(description = "소셜 링크 (JSON - 선택)",
            example = "{\"instagram\": \"@gangnam_interior\", \"facebook\": \"gangnam.interior\"}")
    private Map<String, Object> socialLinks;

    @Schema(description = "주소 (선택)", example = "서울시 강남구 테헤란로 123")
    @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다")
    private String address;

    @Schema(description = "우편번호 (선택)", example = "06234")
    @Size(max = 20, message = "우편번호는 20자를 초과할 수 없습니다")
    private String postalCode;

    @Schema(description = "위도 (선택)", example = "37.123456")
    private BigDecimal latitude;

    @Schema(description = "경도 (선택)", example = "127.123456")
    private BigDecimal longitude;

    @Schema(description = "로고 이미지 파일 UUID (파일 업로드 API로 받은 UUID - 선택)",
            example = "550e8400-e29b-41d4-a716-446655440001")
    private String logoImageUuid;

    @Schema(description = "커버 이미지 파일 UUID (파일 업로드 API로 받은 UUID - 선택)",
            example = "550e8400-e29b-41d4-a716-446655440002")
    private String coverImageUuid;

    @Schema(description = "갤러리 이미지 파일 UUID 배열 (선택)",
            example = "[\"550e8400-e29b-41d4-a716-446655440003\", \"550e8400-e29b-41d4-a716-446655440004\"]")
    private String[] galleryImageUuids;
}
