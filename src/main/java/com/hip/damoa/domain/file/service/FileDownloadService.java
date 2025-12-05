package com.hip.damoa.domain.file.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.model.FileDownload;
import com.hip.damoa.domain.file.model.FilePricing;
import com.hip.damoa.domain.file.repository.FileDownloadRepository;
import com.hip.damoa.domain.file.repository.FilePricingRepository;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.file.web.dto.FileDownloadResponse;
import com.hip.damoa.domain.file.web.dto.FilePurchaseStatusResponse;
import com.hip.damoa.domain.file.web.dto.PurchasedFileResponse;
import com.hip.damoa.domain.payment.model.CreditTransaction;
import com.hip.damoa.domain.payment.service.CreditService;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 파일 다운로드 Service
 *
 * 파일 다운로드 및 크레딧 기반 과금 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final FileRepository fileRepository;
    private final FilePricingRepository filePricingRepository;
    private final FileDownloadRepository fileDownloadRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;

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
     * 유료 파일 다운로드 처리 (크레딧 자동 차감)
     */
    private String handlePaidDownload(File file, User user, FilePricing pricing,
                                       String ipAddress, String userAgent, String referer) {
        log.info("유료 파일 다운로드: fileId={}, userId={}, price={}", file.getId(), user.getId(), pricing.getPrice());

        // 이미 다운로드한 이력이 있는지 확인 (구매 이력)
        boolean hasPurchased = fileDownloadRepository.existsByFileIdAndUserIdAndIsFreeFalse(file.getId(), user.getId());

        if (hasPurchased) {
            log.info("이미 구매한 파일입니다. 재다운로드 허용: fileId={}, userId={}", file.getId(), user.getId());

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

        // 크레딧 잔액 확인
        BigDecimal price = BigDecimal.valueOf(pricing.getPrice());
        if (!creditService.hasEnoughCredits(user.getEmail(), price)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS);
        }

        // 크레딧 차감
        CreditTransaction creditTx = creditService.spendCredits(
                user,
                price,
                "파일 다운로드: " + file.getOriginalFilename(),
                CreditService.ENTITY_FILE_DOWNLOAD,
                file.getId()
        );

        // 다운로드 로그 기록 (유료)
        FileDownload fileDownload = FileDownload.createPaidDownload(
                file, user, ipAddress, userAgent, referer,
                null, pricing.getPrice());
        fileDownloadRepository.save(fileDownload);

        // 파일 다운로드 카운트 증가
        file.incrementDownloadCount();

        // Presigned URL 생성
        String downloadUrl = generatePresignedDownloadUrl(file);

        log.info("유료 파일 다운로드 완료: fileId={}, userId={}, price={}, txId={}",
                file.getId(), user.getId(), pricing.getPrice(), creditTx.getId());

        return downloadUrl;
    }

    /**
     * 파일 구매 여부 확인
     */
    @Transactional(readOnly = true)
    public FilePurchaseStatusResponse getPurchaseStatus(UUID fileUuid, String userEmail) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FilePricing pricing = filePricingRepository.findByFileId(file.getId()).orElse(null);

        // 무료 파일인 경우
        if (pricing == null || pricing.isFree()) {
            return FilePurchaseStatusResponse.builder()
                    .fileUuid(fileUuid)
                    .isPaid(false)
                    .price(0)
                    .hasPurchased(true)  // 무료이므로 구매 필요 없음
                    .canDownload(true)
                    .build();
        }

        // 구매 이력 확인
        boolean hasPurchased = fileDownloadRepository.existsByFileIdAndUserIdAndIsFreeFalse(file.getId(), user.getId());

        // 크레딧 잔액 확인
        boolean hasEnoughCredits = creditService.hasEnoughCredits(userEmail, BigDecimal.valueOf(pricing.getPrice()));

        return FilePurchaseStatusResponse.builder()
                .fileUuid(fileUuid)
                .isPaid(true)
                .price(pricing.getPrice())
                .hasPurchased(hasPurchased)
                .canDownload(hasPurchased || hasEnoughCredits)
                .build();
    }

    /**
     * 내가 구매한 파일 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PurchasedFileResponse> getMyPurchasedFiles(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return fileDownloadRepository.findPurchasedFilesByUserId(user.getId(), pageable)
                .map(fd -> PurchasedFileResponse.from(fd.getFile(), fd.getPricePaid(), fd.getCreatedAt()));
    }

    /**
     * 파일 다운로드 응답 생성 (컨트롤러용)
     */
    @Transactional
    public FileDownloadResponse downloadFile(UUID fileUuid, String userEmail,
                                              String ipAddress, String userAgent, String referer) {
        String downloadUrl = requestDownload(fileUuid, userEmail, ipAddress, userAgent, referer);

        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        FilePricing pricing = filePricingRepository.findByFileId(file.getId()).orElse(null);
        Integer price = (pricing != null && !pricing.isFree()) ? pricing.getPrice() : 0;

        return FileDownloadResponse.builder()
                .fileUuid(fileUuid)
                .fileName(file.getOriginalFilename())
                .downloadUrl(downloadUrl)
                .price(price)
                .build();
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
