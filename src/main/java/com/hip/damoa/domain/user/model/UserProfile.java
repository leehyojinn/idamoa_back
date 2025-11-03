package com.hip.damoa.domain.user.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 사용자 프로필 상세 정보
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Type(JsonBinaryType.class)
    @Column(name = "social_links", columnDefinition = "jsonb")
    private Map<String, Object> socialLinks;

    @Type(StringArrayType.class)
    @Column(name = "interests", columnDefinition = "text[]")
    private String[] interests;

    @Column(name = "profile_visibility", length = 20, nullable = false)
    @Builder.Default
    private String profileVisibility = "PUBLIC"; // PUBLIC, PRIVATE, FRIENDS_ONLY

    // ===== Business Methods =====

    /**
     * 프로필 정보 업데이트
     */
    public void updateProfile(String name, String nickname, String bio) {
        this.name = name;
        this.nickname = nickname;
        this.bio = bio;
    }

    /**
     * 주소 업데이트
     */
    public void updateAddress(String address, String postalCode, BigDecimal latitude, BigDecimal longitude) {
        this.address = address;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * 아바타 업데이트
     */
    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 공개 여부 변경
     */
    public void changeVisibility(String visibility) {
        this.profileVisibility = visibility;
    }
}
