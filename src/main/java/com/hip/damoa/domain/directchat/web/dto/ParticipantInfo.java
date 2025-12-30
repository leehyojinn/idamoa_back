package com.hip.damoa.domain.directchat.web.dto;

import com.hip.damoa.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * 채팅 참여자 정보 DTO
 */
@Schema(description = "채팅 참여자 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantInfo {

    @Schema(description = "사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "온라인 여부")
    private Boolean isOnline;

    /**
     * User 엔티티에서 ParticipantInfo 생성
     * Note: User 엔티티에는 nickname, profileImageUrl이 없으므로 email을 닉네임으로 사용
     */
    public static ParticipantInfo from(User user) {
        if (user == null) {
            return null;
        }

        // 이메일에서 @ 앞부분을 닉네임으로 사용
        String displayName = user.getEmail();
        if (displayName != null && displayName.contains("@")) {
            displayName = displayName.substring(0, displayName.indexOf("@"));
        }

        return ParticipantInfo.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .nickname(displayName)
                .profileImageUrl(null) // UserProfile에서 별도 조회 필요
                .isOnline(false) // 기본값, 나중에 Presence 서비스로 업데이트
                .build();
    }

    /**
     * 온라인 상태 포함하여 생성
     */
    public static ParticipantInfo from(User user, boolean isOnline) {
        if (user == null) {
            return null;
        }

        // 이메일에서 @ 앞부분을 닉네임으로 사용
        String displayName = user.getEmail();
        if (displayName != null && displayName.contains("@")) {
            displayName = displayName.substring(0, displayName.indexOf("@"));
        }

        return ParticipantInfo.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .nickname(displayName)
                .profileImageUrl(null) // UserProfile에서 별도 조회 필요
                .isOnline(isOnline)
                .build();
    }
}
