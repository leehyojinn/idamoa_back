package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.portfolio.model.PortfolioAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioAttachmentRepository extends JpaRepository<PortfolioAttachment, Long> {

    /**
     * 포트폴리오의 모든 첨부파일 조회 (순서대로)
     */
    @Query("SELECT pa FROM PortfolioAttachment pa " +
           "JOIN FETCH pa.file f " +
           "WHERE pa.portfolio.id = :portfolioId " +
           "AND pa.isDeleted = false " +
           "AND f.isDeleted = false " +
           "ORDER BY pa.displayOrder ASC")
    List<PortfolioAttachment> findByPortfolioIdWithFile(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오의 특정 타입 첨부파일만 조회
     */
    @Query("SELECT pa FROM PortfolioAttachment pa " +
           "JOIN FETCH pa.file f " +
           "WHERE pa.portfolio.id = :portfolioId " +
           "AND pa.attachmentType = :type " +
           "AND pa.isDeleted = false " +
           "AND f.isDeleted = false " +
           "ORDER BY pa.displayOrder ASC")
    List<PortfolioAttachment> findByPortfolioIdAndType(
            @Param("portfolioId") Long portfolioId,
            @Param("type") PortfolioAttachment.AttachmentType type);

    /**
     * 포트폴리오의 이미지만 조회
     */
    default List<PortfolioAttachment> findImagesByPortfolioId(Long portfolioId) {
        return findByPortfolioIdAndType(portfolioId, PortfolioAttachment.AttachmentType.IMAGE);
    }

    /**
     * 포트폴리오의 비디오만 조회
     */
    default List<PortfolioAttachment> findVideosByPortfolioId(Long portfolioId) {
        return findByPortfolioIdAndType(portfolioId, PortfolioAttachment.AttachmentType.VIDEO);
    }

    /**
     * 포트폴리오의 썸네일 조회
     */
    @Query("SELECT pa FROM PortfolioAttachment pa " +
           "JOIN FETCH pa.file f " +
           "WHERE pa.portfolio.id = :portfolioId " +
           "AND pa.attachmentType = 'THUMBNAIL' " +
           "AND pa.isDeleted = false " +
           "AND f.isDeleted = false")
    Optional<PortfolioAttachment> findThumbnailByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * UUID로 첨부파일 조회
     */
    Optional<PortfolioAttachment> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 포트폴리오의 모든 첨부파일 삭제 (Soft Delete)
     */
    @Modifying
    @Query("UPDATE PortfolioAttachment pa SET pa.isDeleted = true, pa.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE pa.portfolio.id = :portfolioId AND pa.isDeleted = false")
    void softDeleteByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오와 파일 ID로 존재 여부 확인
     */
    boolean existsByPortfolioIdAndFileIdAndIsDeletedFalse(Long portfolioId, Long fileId);

    /**
     * 첨부파일 개수 조회
     */
    @Query("SELECT COUNT(pa) FROM PortfolioAttachment pa " +
           "WHERE pa.portfolio.id = :portfolioId " +
           "AND pa.attachmentType = :type " +
           "AND pa.isDeleted = false")
    long countByPortfolioIdAndType(
            @Param("portfolioId") Long portfolioId,
            @Param("type") PortfolioAttachment.AttachmentType type);
}
