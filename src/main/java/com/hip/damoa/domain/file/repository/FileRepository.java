package com.hip.damoa.domain.file.repository;

import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.user.model.User;
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

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    Optional<File> findByUuid(UUID uuid);

    Optional<File> findByUuidAndIsDeletedFalse(UUID uuid);

    // Find by uploader
    List<File> findByUploader(User uploader);

    Page<File> findByUploader(User uploader, Pageable pageable);

    // Find by category ID
    List<File> findByCategoryId(Long categoryId);

    Page<File> findByCategoryId(Long categoryId, Pageable pageable);

    // Find by entity type and entity ID
    List<File> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("SELECT f FROM File f WHERE f.entityType = :entityType " +
           "AND f.entityId = :entityId " +
           "ORDER BY f.displayOrder ASC, f.createdAt DESC")
    List<File> findByEntityTypeAndEntityIdOrderByDisplayOrder(@Param("entityType") String entityType,
                                                                @Param("entityId") Long entityId);

    // Find by entity type
    List<File> findByEntityType(String entityType);

    Page<File> findByEntityType(String entityType, Pageable pageable);

    // Find by stored filename
    Optional<File> findByStoredFilename(String storedFilename);

    // Find by original filename
    List<File> findByOriginalFilename(String originalFilename);

    // Find by file URL
    Optional<File> findByFileUrl(String fileUrl);

    // Find public files
    @Query("SELECT f FROM File f WHERE f.isPublic = true " +
           "ORDER BY f.createdAt DESC")
    Page<File> findPublicFiles(Pageable pageable);

    // Find by mime type
    List<File> findByMimeType(String mimeType);

    Page<File> findByMimeType(String mimeType, Pageable pageable);

    // Find by file extension
    List<File> findByFileExtension(String fileExtension);

    // Find by uploader and entity type
    List<File> findByUploaderAndEntityType(User uploader, String entityType);

    // Find by date range
    @Query("SELECT f FROM File f WHERE f.createdAt >= :startDate " +
           "AND f.createdAt <= :endDate " +
           "ORDER BY f.createdAt DESC")
    List<File> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // Find most downloaded files
    @Query("SELECT f FROM File f ORDER BY f.downloadCount DESC")
    List<File> findMostDownloaded(Pageable pageable);

    // Find large files
    @Query("SELECT f FROM File f WHERE f.fileSize > :minSize " +
           "ORDER BY f.fileSize DESC")
    List<File> findLargeFiles(@Param("minSize") Long minSize, Pageable pageable);

    // Calculate total file size by uploader
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM File f WHERE f.uploader = :uploader")
    Long calculateTotalFileSizeByUploader(@Param("uploader") User uploader);

    // Calculate total file size by entity
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM File f " +
           "WHERE f.entityType = :entityType AND f.entityId = :entityId")
    Long calculateTotalFileSizeByEntity(@Param("entityType") String entityType,
                                         @Param("entityId") Long entityId);

    // Count by uploader
    long countByUploader(User uploader);

    // Count by category ID
    long countByCategoryId(Long categoryId);

    // Count by entity
    long countByEntityTypeAndEntityId(String entityType, Long entityId);

    // Count public files
    long countByIsPublicTrue();

    // Find orphaned files (entity_id is null = not connected to any entity)
    // entityType can be set (e.g., "COMPANY_IMAGE") but entityId is null when upload succeeds but entity creation fails
    // Note: Chat attachments now set entityId = message.id, so they won't be orphaned
    @Query("SELECT f FROM File f WHERE " +
           "f.entityId IS NULL AND " +
           "f.createdAt < :threshold AND " +
           "f.isDeleted = false " +
           "ORDER BY f.createdAt ASC")
    List<File> findOrphanedFiles(@Param("threshold") LocalDateTime threshold);

    // [N+1 최적화] File ID 목록으로 일괄 조회
    List<File> findByIdIn(List<Long> ids);

    // [N+1 최적화] UUID 목록으로 일괄 조회
    List<File> findByUuidInAndIsDeletedFalse(List<UUID> uuids);
}
