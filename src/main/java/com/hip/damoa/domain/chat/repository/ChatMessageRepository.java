package com.hip.damoa.domain.chat.repository;

import com.hip.damoa.domain.chat.model.ChatMessage;
import com.hip.damoa.domain.chat.model.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * UUID로 메시지 조회 (삭제되지 않은 것만)
     */
    Optional<ChatMessage> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 채팅방의 메시지 조회 (페이징, 최신순)
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "AND cm.isDeleted = false " +
           "ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * 채팅방의 메시지 조회 (리스트, 오래된 순)
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "AND cm.isDeleted = false " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(@Param("chatRoomId") Long chatRoomId);

    /**
     * 채팅방의 미읽은 메시지 조회 (특정 발신자 타입이 아닌 것)
     *
     * 예: USER가 읽어야 할 메시지 = COMPANY가 보낸 메시지
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "AND cm.senderType != :excludeSenderType " +
           "AND cm.isRead = false " +
           "AND cm.isDeleted = false " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findUnreadByChatRoomIdExcludingSenderType(
            @Param("chatRoomId") Long chatRoomId,
            @Param("excludeSenderType") SenderType excludeSenderType);

    /**
     * 채팅방의 미읽은 메시지 개수 (특정 발신자 타입이 아닌 것)
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "AND cm.senderType != :excludeSenderType " +
           "AND cm.isRead = false " +
           "AND cm.isDeleted = false")
    long countUnreadByChatRoomIdExcludingSenderType(
            @Param("chatRoomId") Long chatRoomId,
            @Param("excludeSenderType") SenderType excludeSenderType);

    /**
     * 채팅방의 가장 최근 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "AND cm.isDeleted = false " +
           "ORDER BY cm.createdAt DESC " +
           "LIMIT 1")
    Optional<ChatMessage> findLatestByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
