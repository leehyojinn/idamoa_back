package com.hip.damoa.domain.file.repository;

import com.hip.damoa.domain.file.model.FileDownload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileDownloadRepository extends JpaRepository<FileDownload, Long> {

    // File별 다운로드 내역
    Page<FileDownload> findByFileId(Long fileId, Pageable pageable);

    List<FileDownload> findByFileId(Long fileId);

    // User별 다운로드 내역
    Page<FileDownload> findByUserId(Long userId, Pageable pageable);

    List<FileDownload> findByUserIdAndFileId(Long userId, Long fileId);

    // 특정 파일을 다운로드한 사용자 수 (중복 제거)
    @Query("SELECT COUNT(DISTINCT fd.user.id) FROM FileDownload fd WHERE fd.file.id = :fileId")
    long countDistinctUsersByFileId(@Param("fileId") Long fileId);

    // 파일별 다운로드 횟수
    long countByFileId(Long fileId);

    // 사용자별 다운로드 횟수
    long countByUserId(Long userId);

    // 유료 다운로드 내역
    List<FileDownload> findByIsFreeFalse();

    Page<FileDownload> findByIsFreeFalse(Pageable pageable);

    // 특정 기간 다운로드 내역
    @Query("SELECT fd FROM FileDownload fd WHERE fd.file.id = :fileId " +
           "AND fd.createdAt BETWEEN :startDate AND :endDate")
    List<FileDownload> findByFileIdAndCreatedAtBetween(@Param("fileId") Long fileId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    // 사용자의 특정 파일 다운로드 여부
    boolean existsByFileIdAndUserId(Long fileId, Long userId);

    // 파일별 수익 통계
    @Query("SELECT COALESCE(SUM(fd.pricePaid), 0) FROM FileDownload fd WHERE fd.file.id = :fileId")
    Long sumPricePaidByFileId(@Param("fileId") Long fileId);

    // 기간별 수익 통계
    @Query("SELECT COALESCE(SUM(fd.pricePaid), 0) FROM FileDownload fd " +
           "WHERE fd.createdAt BETWEEN :startDate AND :endDate")
    Long sumPricePaidBetween(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    // 최근 다운로드 내역
    List<FileDownload> findTop10ByFileIdOrderByCreatedAtDesc(Long fileId);

    // 사용자의 특정 파일 유료 구매 여부 (isFree=false인 다운로드 이력)
    boolean existsByFileIdAndUserIdAndIsFreeFalse(Long fileId, Long userId);

    // 사용자가 구매한 파일 목록 (유료 다운로드 이력)
    @Query("""
        SELECT fd FROM FileDownload fd
        WHERE fd.user.id = :userId
          AND fd.isFree = false
        ORDER BY fd.createdAt DESC
        """)
    Page<FileDownload> findPurchasedFilesByUserId(@Param("userId") Long userId, Pageable pageable);

    // 사용자의 총 구매 금액
    @Query("SELECT COALESCE(SUM(fd.pricePaid), 0) FROM FileDownload fd WHERE fd.user.id = :userId AND fd.isFree = false")
    Long sumPricePaidByUserId(@Param("userId") Long userId);
}
