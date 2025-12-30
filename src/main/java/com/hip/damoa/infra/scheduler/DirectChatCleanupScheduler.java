package com.hip.damoa.infra.scheduler;

import com.hip.damoa.domain.directchat.model.DirectChatAttachment;
import com.hip.damoa.domain.directchat.model.DirectChatMessage;
import com.hip.damoa.domain.directchat.repository.DirectChatAttachmentRepository;
import com.hip.damoa.domain.directchat.repository.DirectChatMessageRepository;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 메시지 정리 스케줄러
 *
 * 30일이 지난 채팅 메시지와 첨부파일을 soft delete 처리
 * - 메시지: direct_chat_messages.is_deleted = true
 * - 첨부파일: direct_chat_attachments.is_deleted = true
 * - 파일: files.is_deleted = true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DirectChatCleanupScheduler {

    private final DirectChatMessageRepository messageRepository;
    private final DirectChatAttachmentRepository attachmentRepository;
    private final FileRepository fileRepository;

    private static final int RETENTION_DAYS = 30;
    private static final int BATCH_SIZE = 100;

    /**
     * 30일 지난 채팅 메시지 정리
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldMessages() {
        log.info("=== 채팅 메시지 정리 스케줄러 시작 ===");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
            int totalDeleted = 0;
            int totalAttachmentsDeleted = 0;
            int totalFilesDeleted = 0;

            // 배치 처리로 메모리 효율화
            List<DirectChatMessage> oldMessages;
            do {
                oldMessages = messageRepository.findOldMessagesForCleanup(threshold, BATCH_SIZE);

                for (DirectChatMessage message : oldMessages) {
                    // 1. 첨부파일 soft delete
                    List<DirectChatAttachment> attachments = attachmentRepository
                            .findByMessageIdAndIsDeletedFalse(message.getId());

                    for (DirectChatAttachment attachment : attachments) {
                        // 파일도 soft delete
                        File file = attachment.getFile();
                        if (file != null && !file.getIsDeleted()) {
                            file.softDelete();
                            fileRepository.save(file);
                            totalFilesDeleted++;
                        }

                        attachment.softDelete();
                        attachmentRepository.save(attachment);
                        totalAttachmentsDeleted++;
                    }

                    // 2. 메시지 soft delete
                    message.softDelete();
                    messageRepository.save(message);
                    totalDeleted++;
                }

            } while (!oldMessages.isEmpty());

            log.info("채팅 메시지 정리 완료: 메시지 {}개, 첨부파일 {}개, 파일 {}개 삭제",
                    totalDeleted, totalAttachmentsDeleted, totalFilesDeleted);

        } catch (Exception e) {
            log.error("채팅 메시지 정리 중 오류 발생", e);
        }
    }

    /**
     * 수동 실행용 (테스트/관리자용)
     */
    @Transactional
    public CleanupResult cleanupOldMessagesNow(int retentionDays) {
        log.info("=== 수동 채팅 메시지 정리 시작: {}일 이전 ===", retentionDays);

        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int messagesDeleted = 0;
        int attachmentsDeleted = 0;
        int filesDeleted = 0;

        List<DirectChatMessage> oldMessages;
        do {
            oldMessages = messageRepository.findOldMessagesForCleanup(threshold, BATCH_SIZE);

            for (DirectChatMessage message : oldMessages) {
                List<DirectChatAttachment> attachments = attachmentRepository
                        .findByMessageIdAndIsDeletedFalse(message.getId());

                for (DirectChatAttachment attachment : attachments) {
                    File file = attachment.getFile();
                    if (file != null && !file.getIsDeleted()) {
                        file.softDelete();
                        fileRepository.save(file);
                        filesDeleted++;
                    }

                    attachment.softDelete();
                    attachmentRepository.save(attachment);
                    attachmentsDeleted++;
                }

                message.softDelete();
                messageRepository.save(message);
                messagesDeleted++;
            }

        } while (!oldMessages.isEmpty());

        log.info("수동 정리 완료: 메시지 {}개, 첨부파일 {}개, 파일 {}개",
                messagesDeleted, attachmentsDeleted, filesDeleted);

        return new CleanupResult(messagesDeleted, attachmentsDeleted, filesDeleted);
    }

    public record CleanupResult(int messagesDeleted, int attachmentsDeleted, int filesDeleted) {}
}
