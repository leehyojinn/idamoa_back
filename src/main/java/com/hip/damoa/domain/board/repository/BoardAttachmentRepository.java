package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardAttachmentRepository extends JpaRepository<BoardAttachment, Long> {

    // 게시글의 모든 첨부파일 조회 (순서대로)
    @Query("SELECT ba FROM BoardAttachment ba WHERE ba.board = :board " +
           "AND ba.isDeleted = false " +
           "ORDER BY ba.displayOrder ASC")
    List<BoardAttachment> findByBoardOrderByDisplayOrder(@Param("board") Board board);

    // 게시글의 특정 타입 첨부파일 조회
    @Query("SELECT ba FROM BoardAttachment ba WHERE ba.board = :board " +
           "AND ba.attachmentType = :type " +
           "AND ba.isDeleted = false " +
           "ORDER BY ba.displayOrder ASC")
    List<BoardAttachment> findByBoardAndTypeOrderByDisplayOrder(
        @Param("board") Board board,
        @Param("type") BoardAttachment.AttachmentType type);

    // 파일 ID로 첨부파일 조회
    Optional<BoardAttachment> findByFileIdAndIsDeletedFalse(Long fileId);

    // 게시글의 첨부파일 개수
    @Query("SELECT COUNT(ba) FROM BoardAttachment ba WHERE ba.board = :board " +
           "AND ba.isDeleted = false")
    long countByBoard(@Param("board") Board board);

    // 게시글의 특정 타입 첨부파일 개수
    @Query("SELECT COUNT(ba) FROM BoardAttachment ba WHERE ba.board = :board " +
           "AND ba.attachmentType = :type " +
           "AND ba.isDeleted = false")
    long countByBoardAndType(@Param("board") Board board,
                             @Param("type") BoardAttachment.AttachmentType type);

    // 게시글의 모든 첨부파일 삭제 (Soft delete)
    @Query("UPDATE BoardAttachment ba SET ba.isDeleted = true, ba.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE ba.board = :board AND ba.isDeleted = false")
    void softDeleteByBoard(@Param("board") Board board);

    // [N+1 최적화] Board ID 목록으로 첨부파일 일괄 조회
    @Query("SELECT ba FROM BoardAttachment ba " +
           "WHERE ba.board.id IN :boardIds AND ba.isDeleted = false " +
           "ORDER BY ba.board.id, ba.displayOrder")
    List<BoardAttachment> findByBoardIdIn(@Param("boardIds") List<Long> boardIds);

    // [N+1 최적화] Board ID 목록 + 타입으로 첨부파일 일괄 조회
    @Query("SELECT ba FROM BoardAttachment ba " +
           "WHERE ba.board.id IN :boardIds " +
           "AND ba.attachmentType = :type " +
           "AND ba.isDeleted = false " +
           "ORDER BY ba.board.id, ba.displayOrder")
    List<BoardAttachment> findByBoardIdInAndType(@Param("boardIds") List<Long> boardIds,
                                                  @Param("type") BoardAttachment.AttachmentType type);
}
