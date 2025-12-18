package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.user.model.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * USER 프로필 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 프로필 응답")
public class UserProfileResponse {

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "닉네임", example = "길동이")
    private String nickname;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "자기소개", example = "안녕하세요. 인테리어에 관심이 많습니다.")
    private String bio;

    @Schema(description = "아바타 파일 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private java.util.UUID avatarFileUuid;

    @Schema(description = "아바타 이미지 URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "우편번호", example = "06123")
    private String postalCode;

    @Schema(description = "프로필 공개 범위 (PUBLIC, PRIVATE)", example = "PUBLIC")
    private String profileVisibility;

    @Schema(description = "소셜 링크", example = "{\"instagram\": \"@user123\", \"blog\": \"https://blog.example.com\"}")
    private Map<String, Object> socialLinks;

    @Schema(description = "서비스 이용약관 동의 여부", example = "true")
    private Boolean termsAgreed;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    private Boolean privacyAgreed;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "false")
    private Boolean marketingAgreed;

    @Schema(description = "JWT 토큰 정보 (프로필 생성 후)")
    private TokenInfo tokenInfo;

    /**
     * Entity → DTO 변환
     */
    public static UserProfileResponse from(UserProfile profile) {
        return from(profile, null, null, null);
    }

    /**
     * Entity + Avatar 파일 정보 → DTO 변환
     */
    public static UserProfileResponse from(UserProfile profile, Long avatarFileId, java.util.UUID avatarFileUuid, String avatarUrl) {
        return UserProfileResponse.builder()
                .name(profile.getName())
                .nickname(profile.getNickname())
                .phone(profile.getPhone())
                .bio(profile.getBio())
                .avatarFileUuid(avatarFileUuid)
                .avatarUrl(avatarUrl != null ? avatarUrl : profile.getAvatarUrl())
                .address(profile.getAddress())
                .postalCode(profile.getPostalCode())
                .profileVisibility(profile.getProfileVisibility())
                .socialLinks(profile.getSocialLinks())
                .termsAgreed(profile.getUser().getTermsAgreed())
                .privacyAgreed(profile.getUser().getPrivacyAgreed())
                .marketingAgreed(profile.getUser().getMarketingAgreed())
                .build();
    }

    /**
     * Entity + TokenInfo → DTO 변환
     */
    public static UserProfileResponse from(UserProfile profile, TokenInfo tokenInfo) {
        return from(profile, null, null, null, tokenInfo);
    }

    /**
     * Entity + Avatar 파일 정보 + TokenInfo → DTO 변환
     */
    public static UserProfileResponse from(UserProfile profile, Long avatarFileId, java.util.UUID avatarFileUuid, String avatarUrl, TokenInfo tokenInfo) {
        return UserProfileResponse.builder()
                .name(profile.getName())
                .nickname(profile.getNickname())
                .phone(profile.getPhone())
                .bio(profile.getBio())
                .avatarFileUuid(avatarFileUuid)
                .avatarUrl(avatarUrl != null ? avatarUrl : profile.getAvatarUrl())
                .address(profile.getAddress())
                .postalCode(profile.getPostalCode())
                .profileVisibility(profile.getProfileVisibility())
                .socialLinks(profile.getSocialLinks())
                .termsAgreed(profile.getUser().getTermsAgreed())
                .privacyAgreed(profile.getUser().getPrivacyAgreed())
                .marketingAgreed(profile.getUser().getMarketingAgreed())
                .tokenInfo(tokenInfo)
                .build();
    }
}
