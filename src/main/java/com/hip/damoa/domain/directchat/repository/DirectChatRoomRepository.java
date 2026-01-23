package com.hip.damoa.domain.directchat.repository;

import com.hip.damoa.domain.directchat.model.DirectChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 1:1 채팅방 Repository
 */
@Repository
public interface DirectChatRoomRepository extends JpaRepository<DirectChatRoom, Long> {

    /**
     * UUID로 채팅방 조회
     */
    Optional<DirectChatRoom> findByUuid(UUID uuid);

    /**
     * UUID로 삭제되지 않은 채팅방 조회
     */
    Optional<DirectChatRoom> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 두 사용자 간의 채팅방 조회 (user1_id < user2_id 규칙 적용)
     */
    @Query("SELECT r FROM DirectChatRoom r " +
           "WHERE r.isDeleted = false " +
           "AND ((r.user1.id = :smallerId AND r.user2.id = :largerId))")
    Optional<DirectChatRoom> findByUsers(@Param("smallerId") Long smallerId,
                                         @Param("largerId") Long largerId);

    /**
     * 특정 사용자의 활성 채팅방 목록 조회 (최신 메시지 순)
     */
    @Query("SELECT r FROM DirectChatRoom r " +
           "WHERE r.isDeleted = false " +
           "AND ((r.user1.id = :userId AND r.user1Active = true) " +
           "     OR (r.user2.id = :userId AND r.user2Active = true)) " +
           "ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<DirectChatRoom> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    /**
     * 특정 사용자가 참여한 모든 채팅방 (활성/비활성 포함)
     */
    @Query("SELECT r FROM DirectChatRoom r " +
           "WHERE r.isDeleted = false " +
           "AND (r.user1.id = :userId OR r.user2.id = :userId) " +
           "ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<DirectChatRoom> findAllByUserId(@Param("userId") Long userId);

    /**
     * 채팅방에 사용자가 참여하고 있는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM DirectChatRoom r " +
           "WHERE r.uuid = :roomUuid " +
           "AND r.isDeleted = false " +
           "AND (r.user1.id = :userId OR r.user2.id = :userId)")
    boolean isParticipant(@Param("roomUuid") UUID roomUuid, @Param("userId") Long userId);

    /**
     * 사용자의 전체 미읽음 메시지 수 조회 (N+1 방지용 네이티브 쿼리)
     */
    @Query(value = """
        SELECT COALESCE(SUM(unread_count), 0) FROM (
            SELECT
                CASE
                    WHEN drs.last_read_message_id IS NULL THEN
                        (SELECT COUNT(*) FROM direct_chat_messages m
                         WHERE m.room_id = r.id AND m.is_deleted = false AND m.sender_id <> :userId)
                    ELSE
                        (SELECT COUNT(*) FROM direct_chat_messages m
                         WHERE m.room_id = r.id AND m.is_deleted = false
                         AND m.sender_id <> :userId AND m.id > drs.last_read_message_id)
                END as unread_count
            FROM direct_chat_rooms r
            LEFT JOIN direct_chat_read_states drs ON drs.room_id = r.id AND drs.user_id = :userId
            WHERE r.is_deleted = false
            AND ((r.user1_id = :userId AND r.user1_active = true)
                 OR (r.user2_id = :userId AND r.user2_active = true))
        ) sub
        """, nativeQuery = true)
    long getTotalUnreadCountByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 채팅방 목록 조회 (참여자 Fetch Join)
     */
    @Query("SELECT r FROM DirectChatRoom r " +
           "LEFT JOIN FETCH r.user1 " +
           "LEFT JOIN FETCH r.user2 " +
           "LEFT JOIN FETCH r.lastSender " +
           "WHERE r.isDeleted = false " +
           "AND ((r.user1.id = :userId AND r.user1Active = true) " +
           "     OR (r.user2.id = :userId AND r.user2Active = true)) " +
           "ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<DirectChatRoom> findByUserIdWithParticipants(@Param("userId") Long userId);

    /**
     * 사용자의 각 채팅방별 미읽음 수 조회 (N+1 방지)
     */
    @Query(value = """
        SELECT r.id as room_id,
            CASE
                WHEN drs.last_read_message_id IS NULL THEN
                    (SELECT COUNT(*) FROM direct_chat_messages m
                     WHERE m.room_id = r.id AND m.is_deleted = false AND m.sender_id <> :userId)
                ELSE
                    (SELECT COUNT(*) FROM direct_chat_messages m
                     WHERE m.room_id = r.id AND m.is_deleted = false
                     AND m.sender_id <> :userId AND m.id > drs.last_read_message_id)
            END as unread_count
        FROM direct_chat_rooms r
        LEFT JOIN direct_chat_read_states drs ON drs.room_id = r.id AND drs.user_id = :userId
        WHERE r.is_deleted = false
        AND ((r.user1_id = :userId AND r.user1_active = true)
             OR (r.user2_id = :userId AND r.user2_active = true))
        """, nativeQuery = true)
    List<Object[]> getUnreadCountsByUserId(@Param("userId") Long userId);

    // ===== Dashboard 통계용 메서드 =====

    /**
     * 사용자의 활성 채팅방 수
     */
    @Query("SELECT COUNT(r) FROM DirectChatRoom r " +
           "WHERE r.isDeleted = false " +
           "AND ((r.user1.id = :userId AND r.user1Active = true) " +
           "     OR (r.user2.id = :userId AND r.user2Active = true))")
    long countActiveByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 최근 채팅방 목록 (with fetch)
     */
    @Query("SELECT r FROM DirectChatRoom r " +
           "LEFT JOIN FETCH r.user1 " +
           "LEFT JOIN FETCH r.user2 " +
           "WHERE r.isDeleted = false " +
           "AND ((r.user1.id = :userId AND r.user1Active = true) " +
           "     OR (r.user2.id = :userId AND r.user2Active = true)) " +
           "ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<DirectChatRoom> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
