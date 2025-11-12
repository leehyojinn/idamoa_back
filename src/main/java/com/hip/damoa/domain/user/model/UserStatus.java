package com.hip.damoa.domain.user.model;

/**
 * 사용자 계정 상태
 */
public enum UserStatus {
    PENDING,    // 대기 중 (프로필 미완성)
    ACTIVE,     // 활성화 (정상 사용)
    INACTIVE,   // 비활성화 (탈퇴 등)
    SUSPENDED,  // 정지 (이용 제한)
    DELETED     // 삭제 (완전 삭제)
}
