package com.hip.damoa.domain.directchat.repository;

import com.hip.damoa.domain.directchat.model.DirectChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 첨부파일 Repository
 */
@Repository
public interface DirectChatAttachmentRepository extends JpaRepository<DirectChatAttachment, Long> {

    /**
     * UUID로 첨부파일 조회
     */
    Optional<DirectChatAttachment> findByUuid(UUID uuid);

    /**
     * UUID로 삭제되지 않은 첨부파일 조회
     */
    Optional<DirectChatAttachment> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 메시지의 첨부파일 목록 조회
     */
    @Query("SELECT a FROM DirectChatAttachment a " +
           "LEFT JOIN FETCH a.file " +
           "WHERE a.message.id = :messageId " +
           "AND a.isDeleted = false")
    List<DirectChatAttachment> findByMessageIdWithFile(@Param("messageId") Long messageId);

    /**
     * 여러 메시지의 첨부파일 일괄 조회 (N+1 방지)
     */
    @Query("SELECT a FROM DirectChatAttachment a " +
           "LEFT JOIN FETCH a.file " +
           "WHERE a.message.id IN :messageIds " +
           "AND a.isDeleted = false")
    List<DirectChatAttachment> findByMessageIdsWithFile(@Param("messageIds") List<Long> messageIds);

    /**
     * 메시지의 삭제되지 않은 첨부파일 목록 조회 (스케줄러용)
     */
    List<DirectChatAttachment> findByMessageIdAndIsDeletedFalse(Long messageId);
}
