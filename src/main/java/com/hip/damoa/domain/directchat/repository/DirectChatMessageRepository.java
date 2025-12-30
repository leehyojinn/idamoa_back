package com.hip.damoa.domain.directchat.repository;

import com.hip.damoa.domain.directchat.model.DirectChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 메시지 Repository
 */
@Repository
public interface DirectChatMessageRepository extends JpaRepository<DirectChatMessage, Long> {

    /**
     * UUID로 메시지 조회
     */
    Optional<DirectChatMessage> findByUuid(UUID uuid);

    /**
     * UUID로 삭제되지 않은 메시지 조회
     */
    Optional<DirectChatMessage> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 채팅방의 메시지 목록 조회 (페이징, Sender 페치 조인)
     */
    @Query("SELECT m FROM DirectChatMessage m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.room.id = :roomId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<DirectChatMessage> findByRoomIdWithSender(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * 채팅방의 미읽음 메시지 수 조회
     */
    @Query("SELECT COUNT(m) FROM DirectChatMessage m " +
           "WHERE m.room.id = :roomId " +
           "AND m.isDeleted = false " +
           "AND m.id > :lastReadMessageId " +
           "AND m.sender.id <> :userId")
    long countUnreadMessages(@Param("roomId") Long roomId,
                             @Param("lastReadMessageId") Long lastReadMessageId,
                             @Param("userId") Long userId);

    /**
     * 채팅방의 전체 미읽음 메시지 수 (처음 읽는 경우)
     */
    @Query("SELECT COUNT(m) FROM DirectChatMessage m " +
           "WHERE m.room.id = :roomId " +
           "AND m.isDeleted = false " +
           "AND m.sender.id <> :userId")
    long countAllUnreadMessages(@Param("roomId") Long roomId,
                                @Param("userId") Long userId);

    /**
     * 채팅방의 마지막 메시지 조회 (createdAt DESC, id DESC로 정렬하여 안정적인 순서 보장)
     */
    Optional<DirectChatMessage> findFirstByRoomIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(Long roomId);

    /**
     * 특정 메시지 이후의 메시지 목록 (실시간 동기화용)
     */
    @Query("SELECT m FROM DirectChatMessage m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.room.id = :roomId " +
           "AND m.id > :afterMessageId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    Page<DirectChatMessage> findMessagesAfter(@Param("roomId") Long roomId,
                                              @Param("afterMessageId") Long afterMessageId,
                                              Pageable pageable);

    /**
     * 메시지 ID 목록 조회 (페이징 최적화용)
     */
    @Query("SELECT m.id FROM DirectChatMessage m " +
           "WHERE m.room.id = :roomId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<Long> findMessageIdsByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * ID 목록으로 메시지 조회 (Fetch Join)
     */
    @Query("SELECT m FROM DirectChatMessage m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.id IN :ids")
    List<DirectChatMessage> findByIdsWithSender(@Param("ids") List<Long> ids);

    /**
     * 정리 대상 오래된 메시지 조회 (스케줄러용)
     * - threshold보다 오래된 메시지
     * - 아직 삭제되지 않은 메시지
     * - 배치 처리를 위해 limit 적용
     */
    @Query(value = "SELECT * FROM direct_chat_messages m " +
                   "WHERE m.created_at < :threshold " +
                   "AND m.is_deleted = false " +
                   "ORDER BY m.created_at ASC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<DirectChatMessage> findOldMessagesForCleanup(@Param("threshold") LocalDateTime threshold,
                                                       @Param("limit") int limit);
}
