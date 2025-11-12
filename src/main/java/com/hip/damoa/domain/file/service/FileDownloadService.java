package com.hip.damoa.domain.file.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.model.FileDownload;
import com.hip.damoa.domain.file.model.FilePricing;
import com.hip.damoa.domain.file.repository.FileDownloadRepository;
import com.hip.damoa.domain.file.repository.FilePricingRepository;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 파일 다운로드 Service
 *
 * 파일 다운로드 및 과금 처리
 * TODO: Payment 시스템 연동 필요
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final FileRepository fileRepository;
    private final FilePricingRepository filePricingRepository;
    private final FileDownloadRepository fileDownloadRepository;
    private final UserRepository userRepository;

    /**
     * 파일 다운로드 요청 (결제 확인 + Presigned URL 생성)
     */
    @Transactional
    public String requestDownload(UUID fileUuid, String userEmail, String ipAddress,
                                   String userAgent, String referer) {
        log.info("파일 다운로드 요청: fileUuid={}, userEmail={}", fileUuid, userEmail);

        // 파일 조회
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 파일 가격 정보 조회
        FilePricing pricing = filePricingRepository.findByFileId(file.getId()).orElse(null);

        // 무료 파일인 경우
        if (pricing == null || pricing.isFree()) {
            return handleFreeDownload(file, user, ipAddress, userAgent, referer);
        }

        // 유료 파일인 경우 - 결제 확인
        return handlePaidDownload(file, user, pricing, ipAddress, userAgent, referer);
    }

    /**
     * 무료 파일 다운로드 처리
     */
    private String handleFreeDownload(File file, User user, String ipAddress,
                                       String userAgent, String referer) {
        log.info("무료 파일 다운로드: fileId={}, userId={}", file.getId(), user.getId());

        // 다운로드 로그 기록
        FileDownload fileDownload = FileDownload.createFreeDownload(
                file, user, ipAddress, userAgent, referer);
        fileDownloadRepository.save(fileDownload);

        // 파일 다운로드 카운트 증가
        file.incrementDownloadCount();

        // Presigned URL 생성 (15분 유효)
        String downloadUrl = generatePresignedDownloadUrl(file);

        log.info("무료 파일 다운로드 URL 생성 완료: fileId={}", file.getId());
        return downloadUrl;
    }

    /**
     * 유료 파일 다운로드 처리
     */
    private String handlePaidDownload(File file, User user, FilePricing pricing,
                                       String ipAddress, String userAgent, String referer) {
        log.info("유료 파일 다운로드: fileId={}, userId={}, price={}", file.getId(), user.getId(), pricing.getPrice());

        // 이미 다운로드한 이력이 있는지 확인
        boolean hasDownloaded = fileDownloadRepository.existsByFileIdAndUserId(file.getId(), user.getId());

        if (hasDownloaded) {
            log.info("이미 다운로드한 파일입니다. 재다운로드 허용: fileId={}, userId={}", file.getId(), user.getId());

            // 재다운로드 로그 기록 (무료로 처리)
            FileDownload fileDownload = FileDownload.createFreeDownload(
                    file, user, ipAddress, userAgent, referer);
            fileDownloadRepository.save(fileDownload);

            return generatePresignedDownloadUrl(file);
        }

        // 다운로드 제한 확인
        if (pricing.hasDownloadLimit()) {
            long currentDownloadCount = fileDownloadRepository.countByFileId(file.getId());
            if (currentDownloadCount >= pricing.getDownloadLimit()) {
                throw new BusinessException(ErrorCode.DOWNLOAD_LIMIT_EXCEEDED);
            }
        }

        // 결제 내역 확인 (최근 결제 내역 중 해당 파일에 대한 결제 찾기)
        // 실제로는 결제 시스템과 연동하여 확인해야 함
        // 여기서는 간단히 처리
        throw new BusinessException(ErrorCode.PAYMENT_REQUIRED);
    }

    /**
     * 파일 구매 후 다운로드
     * TODO: Payment 시스템 구현 후 활성화
     */
    @Transactional
    public String downloadAfterPayment(UUID fileUuid, String userEmail, Long paymentId,
                                        String ipAddress, String userAgent, String referer) {
        log.info("결제 후 파일 다운로드: fileUuid={}, userEmail={}, paymentId={}", fileUuid, userEmail, paymentId);

        // 파일 조회
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // TODO: Payment 시스템 구현 후 결제 확인 로직 추가
        // Payment payment = paymentRepository.findById(paymentId)...

        // 파일 가격 정보 조회
        FilePricing pricing = filePricingRepository.findByFileId(file.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_PRICING_NOT_FOUND));

        // 다운로드 로그 기록 (유료) - Payment 없이 임시 처리
        FileDownload fileDownload = FileDownload.createPaidDownload(
                file, user, ipAddress, userAgent, referer, null, pricing.getPrice());
        fileDownloadRepository.save(fileDownload);

        // 파일 다운로드 카운트 증가
        file.incrementDownloadCount();

        // Presigned URL 생성
        String downloadUrl = generatePresignedDownloadUrl(file);

        log.info("유료 파일 다운로드 URL 생성 완료: fileId={}, paymentId={}", file.getId(), paymentId);
        return downloadUrl;
    }

    /**
     * 파일 다운로드 통계 조회
     */
    @Transactional(readOnly = true)
    public FileDownloadStats getDownloadStats(UUID fileUuid) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        long totalDownloads = fileDownloadRepository.countByFileId(file.getId());
        long uniqueUsers = fileDownloadRepository.countDistinctUsersByFileId(file.getId());
        Long totalRevenue = fileDownloadRepository.sumPricePaidByFileId(file.getId());

        List<FileDownload> recentDownloads = fileDownloadRepository.findTop10ByFileIdOrderByCreatedAtDesc(file.getId());

        return FileDownloadStats.builder()
                .fileUuid(fileUuid)
                .fileName(file.getOriginalFilename())
                .totalDownloads(totalDownloads)
                .uniqueUsers(uniqueUsers)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0L)
                .recentDownloads(recentDownloads)
                .build();
    }

    /**
     * Presigned Download URL 생성
     */
    private String generatePresignedDownloadUrl(File file) {
        // 실제로는 S3 Presigned URL을 생성해야 함
        // 여기서는 간단히 파일 URL 반환
        // TODO: S3 SDK를 사용하여 실제 Presigned URL 생성 구현 필요
        return file.getFileUrl();
    }

    /**
     * 파일 다운로드 통계 DTO
     */
    @lombok.Getter
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FileDownloadStats {
        private UUID fileUuid;
        private String fileName;
        private Long totalDownloads;
        private Long uniqueUsers;
        private Long totalRevenue;
        private List<FileDownload> recentDownloads;
    }
}
