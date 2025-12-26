package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.core.util.ResponseUtils;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 통합 프로필 응답 DTO
 * USER와 COMPANY 프로필을 모두 처리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    // 프로필 타입
    private String profileType;  // "USER" or "COMPANY"

    // 공통 필드
    private String name;
    private String phone;  // ✅ DB 컬럼명 통일 (primaryPhone 아님!)
    private String email;
    private String address;
    private String postalCode;

    // USER 전용 필드 (COMPANY일 때 null)
    private String nickname;
    private java.util.UUID avatarFileUuid;  // 파일 UUID (수정 시 사용)
    private String avatarUrl;
    private String profileVisibility;  // PUBLIC, PRIVATE, FRIENDS_ONLY

    // 공통 필드 (용도가 다름)
    private String bio;  // USER: 자기소개, COMPANY: 업체 소개 ✅ DB 컬럼명 통일 (description 아님!)

    // 소셜 링크
    private Map<String, Object> socialLinks;

    // 약관 동의
    private Boolean termsAgreed;
    private Boolean privacyAgreed;
    private Boolean marketingAgreed;

    // 프로필 생성 후 새로운 JWT 토큰 (선택적)
    private TokenInfo tokenInfo;

    /**
     * UserProfile → ProfileResponse 변환
     * @param profile UserProfile 엔티티
     * @param user User 엔티티 (email 정보 필요)
     */
    public static ProfileResponse from(UserProfile profile, User user) {
        return from(profile, user, null, null, null, null);
    }

    /**
     * UserProfile + Avatar 파일 정보 → ProfileResponse 변환
     */
    public static ProfileResponse from(UserProfile profile, User user, Long avatarFileId, java.util.UUID avatarFileUuid, String avatarUrl) {
        return from(profile, user, avatarFileId, avatarFileUuid, avatarUrl, null);
    }

    /**
     * UserProfile + Avatar 파일 정보 + TokenInfo → ProfileResponse 변환
     */
    public static ProfileResponse from(UserProfile profile, User user, Long avatarFileId, java.util.UUID avatarFileUuid, String avatarUrl, TokenInfo tokenInfo) {
        return ProfileResponse.builder()
                .profileType(ResponseUtils.safe(profile.getProfileType(), "USER"))
                .name(ResponseUtils.safe(profile.getName()))
                .phone(ResponseUtils.safe(profile.getPhone()))
                .email(ResponseUtils.safe(user.getEmail()))
                .address(ResponseUtils.safe(profile.getAddress()))
                .postalCode(ResponseUtils.safe(profile.getPostalCode()))
                .nickname(ResponseUtils.safe(profile.getNickname()))
                .avatarFileUuid(avatarFileUuid)
                .avatarUrl(ResponseUtils.safe(avatarUrl != null ? avatarUrl : profile.getAvatarUrl()))
                .profileVisibility(ResponseUtils.safe(profile.getProfileVisibility(), "PUBLIC"))
                .bio(ResponseUtils.safe(profile.getBio()))
                .socialLinks(ResponseUtils.safeMap(profile.getSocialLinks()))
                .termsAgreed(ResponseUtils.safe(user.getTermsAgreed()))
                .privacyAgreed(ResponseUtils.safe(user.getPrivacyAgreed()))
                .marketingAgreed(ResponseUtils.safe(user.getMarketingAgreed()))
                .tokenInfo(tokenInfo)
                .build();
    }

    /**
     * UserProfile + TokenInfo → ProfileResponse 변환 (레거시 호환)
     * @param profile UserProfile 엔티티
     * @param user User 엔티티
     * @param tokenInfo JWT 토큰 정보
     */
    public static ProfileResponse from(UserProfile profile, User user, TokenInfo tokenInfo) {
        return from(profile, user, null, null, null, tokenInfo);
    }
}
