package com.hip.damoa.infra.scheduler;

import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.infra.storage.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orphaned file cleanup scheduler
 *
 * Automatically deletes orphaned files where entity_id is null.
 *
 * Background:
 * - File upload succeeds but entity creation fails (e.g., company registration failure)
 * - File has entityType (e.g., "COMPANY_IMAGE") but entityId is null
 * - S3 and PostgreSQL cannot participate in same ACID transaction
 * - Therefore, periodic cleanup is necessary
 *
 * Process:
 * 1. User uploads image -> entityType="COMPANY_IMAGE", entityId=null
 * 2. Company registration fails -> orphaned file remains
 * 3. Scheduler finds files where entityId IS NULL and created > threshold
 * 4. Deletes from S3 and soft-deletes from DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final FileRepository fileRepository;
    private final S3Service s3Service;

    /**
     * Orphaned file cleanup scheduler
     * Runs every 5 minutes after previous execution completes
     * First execution occurs 1 minute after application startup
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)  // 5분마다, 시작 후 1분 뒤 첫 실행
    @Transactional
    public void cleanupOrphanedFiles() {
        try {
            log.info("=== 쓰레기 파일 정리 스케줄러 시작 ===");

            // Find orphaned files created before threshold (entityId is null)
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
            List<File> orphanedFiles = fileRepository.findOrphanedFiles(threshold);

            if (orphanedFiles.isEmpty()) {
                log.info("쓰레기 파일 없음 (정상)");
                return;
            }

            log.warn("쓰레기 파일 {}개 발견 - 삭제 시작", orphanedFiles.size());

            int successCount = 0;
            int failCount = 0;

            for (File file : orphanedFiles) {
                try {
                    // S3에서 삭제
                    s3Service.deleteFile(file.getFilePath());

                    // DB에서 soft delete
                    file.softDelete();
                    fileRepository.save(file);

                    successCount++;
                    log.info("쓰레기 파일 삭제 성공: id={}, filePath={}, fileUrl={}, createdAt={}",
                            file.getId(), file.getFilePath(), file.getFileUrl(), file.getCreatedAt());

                } catch (Exception e) {
                    failCount++;
                    log.error("쓰레기 파일 삭제 실패: id={}, filePath={}, error={}",
                            file.getId(), file.getFilePath(), e.getMessage());
                }
            }

            log.warn("쓰레기 파일 정리 완료: 총 {}개, 성공 {}개, 실패 {}개",
                    orphanedFiles.size(), successCount, failCount);

        } catch (Exception e) {
            log.error("쓰레기 파일 정리 작업 중 오류 발생", e);
        }
    }

    /**
     * 즉시 정리 실행 (테스트/수동 실행용)
     *
     * API 엔드포인트에서 호출하여 즉시 정리 가능
     */
    @Transactional
    public int cleanupOrphanedFilesNow(int minutesThreshold) {
        log.info("=== 수동 쓰레기 파일 정리 시작: threshold={}분 ===", minutesThreshold);

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutesThreshold);
        List<File> orphanedFiles = fileRepository.findOrphanedFiles(threshold);

        int count = 0;
        for (File file : orphanedFiles) {
            try {
                s3Service.deleteFile(file.getFilePath());
                file.softDelete();
                fileRepository.save(file);
                count++;
            } catch (Exception e) {
                log.error("파일 삭제 실패: id={}, error={}", file.getId(), e.getMessage());
            }
        }

        log.info("수동 정리 완료: {}개 파일 삭제", count);
        return count;
    }
}
