package com.hip.damoa.domain.chat.repository;

import com.hip.damoa.domain.chat.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * UUID로 채팅방 조회 (삭제되지 않은 것만)
     */
    Optional<ChatRoom> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 견적 요청 ID와 업체 ID로 채팅방 조회
     * (한 견적 요청당 한 업체와의 채팅방은 하나만 존재)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.estimateRequest.id = :estimateRequestId " +
           "AND cr.company.id = :companyId " +
           "AND cr.isDeleted = false")
    Optional<ChatRoom> findByEstimateRequestIdAndCompanyId(
            @Param("estimateRequestId") Long estimateRequestId,
            @Param("companyId") Long companyId);

    /**
     * 사용자의 모든 채팅방 조회 (최신 메시지 순)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.user.id = :userId " +
           "AND cr.isDeleted = false " +
           "ORDER BY cr.lastMessageAt DESC NULLS LAST")
    List<ChatRoom> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    /**
     * 업체의 모든 채팅방 조회 (최신 메시지 순)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.company.id = :companyId " +
           "AND cr.isDeleted = false " +
           "ORDER BY cr.lastMessageAt DESC NULLS LAST")
    List<ChatRoom> findByCompanyIdOrderByLastMessageAtDesc(@Param("companyId") Long companyId);

    /**
     * 사용자의 미읽음 채팅방 개수
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr " +
           "WHERE cr.user.id = :userId " +
           "AND cr.unreadCountUser > 0 " +
           "AND cr.isDeleted = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 업체의 미읽음 채팅방 개수
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr " +
           "WHERE cr.company.id = :companyId " +
           "AND cr.unreadCountCompany > 0 " +
           "AND cr.isDeleted = false")
    long countUnreadByCompanyId(@Param("companyId") Long companyId);
}
