package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private Long id;
    private String name;
    private String phone;  // ✅ DB 컬럼명 통일 (primaryPhone 아님!)
    private String email;
    private String address;
    private String postalCode;

    // USER 전용 필드 (COMPANY일 때 null)
    private String nickname;
    private String avatarUrl;
    private String profileVisibility;  // PUBLIC, PRIVATE, FRIENDS_ONLY

    // 공통 필드 (용도가 다름)
    private String bio;  // USER: 자기소개, COMPANY: 업체 소개 ✅ DB 컬럼명 통일 (description 아님!)

    // 프로필 생성 후 새로운 JWT 토큰 (선택적)
    private TokenInfo tokenInfo;

    /**
     * UserProfile → ProfileResponse 변환
     * @param profile UserProfile 엔티티
     * @param user User 엔티티 (email 정보 필요)
     */
    public static ProfileResponse from(UserProfile profile, User user) {
        return ProfileResponse.builder()
                .profileType(profile.getProfileType())
                .id(profile.getId())
                .name(profile.getName())
                .phone(profile.getPhone())
                .email(user.getEmail())
                .address(profile.getAddress())
                .postalCode(profile.getPostalCode())
                .nickname(profile.getNickname())
                .avatarUrl(profile.getAvatarUrl())
                .profileVisibility(profile.getProfileVisibility())
                .bio(profile.getBio())
                .build();
    }

    /**
     * UserProfile + TokenInfo → ProfileResponse 변환
     * @param profile UserProfile 엔티티
     * @param user User 엔티티
     * @param tokenInfo JWT 토큰 정보
     */
    public static ProfileResponse from(UserProfile profile, User user, TokenInfo tokenInfo) {
        return ProfileResponse.builder()
                .profileType(profile.getProfileType())
                .id(profile.getId())
                .name(profile.getName())
                .phone(profile.getPhone())
                .email(user.getEmail())
                .address(profile.getAddress())
                .postalCode(profile.getPostalCode())
                .nickname(profile.getNickname())
                .avatarUrl(profile.getAvatarUrl())
                .profileVisibility(profile.getProfileVisibility())
                .bio(profile.getBio())
                .tokenInfo(tokenInfo)
                .build();
    }
}
