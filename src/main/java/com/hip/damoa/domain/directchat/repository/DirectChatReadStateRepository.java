package com.hip.damoa.domain.directchat.repository;

import com.hip.damoa.domain.directchat.model.DirectChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 채팅 읽음 상태 Repository
 */
@Repository
public interface DirectChatReadStateRepository extends JpaRepository<DirectChatReadState, Long> {

    /**
     * 채팅방과 사용자로 읽음 상태 조회
     */
    Optional<DirectChatReadState> findByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 채팅방과 사용자로 읽음 상태 조회 (메시지 페치 조인)
     */
    @Query("SELECT rs FROM DirectChatReadState rs " +
           "LEFT JOIN FETCH rs.lastReadMessage " +
           "WHERE rs.room.id = :roomId " +
           "AND rs.user.id = :userId")
    Optional<DirectChatReadState> findByRoomIdAndUserIdWithMessage(@Param("roomId") Long roomId,
                                                                    @Param("userId") Long userId);

    /**
     * 마지막으로 읽은 메시지 ID 조회
     */
    @Query("SELECT rs.lastReadMessage.id FROM DirectChatReadState rs " +
           "WHERE rs.room.id = :roomId " +
           "AND rs.user.id = :userId")
    Optional<Long> findLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
