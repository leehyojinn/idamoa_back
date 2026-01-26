package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByUserAndIsDeletedFalse(User user);

    Optional<UserProfile> findByUserId(Long userId);

    // [N+1 최적화] User ID 목록으로 UserProfile 일괄 조회
    @Query("SELECT up FROM UserProfile up WHERE up.user.id IN :userIds")
    List<UserProfile> findByUserIdIn(@Param("userIds") List<Long> userIds);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    // 닉네임 중복 체크 (본인 제외 - 프로필 수정 시 사용)
    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserProfile up " +
           "WHERE up.nickname = :nickname AND up.user.id != :userId")
    boolean existsByNicknameAndUserIdNot(@Param("nickname") String nickname, @Param("userId") Long userId);
}
