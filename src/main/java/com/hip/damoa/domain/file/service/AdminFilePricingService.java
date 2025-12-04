package com.hip.damoa.domain.file.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.model.FilePricing;
import com.hip.damoa.domain.file.repository.FileDownloadRepository;
import com.hip.damoa.domain.file.repository.FilePricingRepository;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.file.web.dto.FilePricingRequest;
import com.hip.damoa.domain.file.web.dto.FilePricingResponse;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 관리자용 파일 가격 설정 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFilePricingService {

    private final FileRepository fileRepository;
    private final FilePricingRepository filePricingRepository;
    private final FileDownloadRepository fileDownloadRepository;
    private final UserRepository userRepository;

    /**
     * 파일 가격 설정 (유료/무료)
     */
    @Transactional
    public FilePricingResponse setPricing(UUID fileUuid, FilePricingRequest request, String adminEmail) {
        log.info("파일 가격 설정: fileUuid={}, isPaid={}, price={}", fileUuid, request.getIsPaid(), request.getPrice());

        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FilePricing pricing = filePricingRepository.findByFileId(file.getId())
                .orElse(null);

        if (pricing == null) {
            // 새 가격 정보 생성
            pricing = FilePricing.builder()
                    .file(file)
                    .isPaid(request.getIsPaid())
                    .price(request.getIsPaid() ? request.getPrice() : 0)
                    .downloadLimit(request.getDownloadLimit())
                    .isActive(true)
                    .description(request.getDescription())
                    .createdBy(admin.getId())
                    .build();
        } else {
            // 기존 가격 정보 업데이트
            if (request.getIsPaid()) {
                pricing.setAsPaid(request.getPrice());
            } else {
                pricing.setAsFree();
            }
            if (request.getDownloadLimit() != null) {
                pricing.updateDownloadLimit(request.getDownloadLimit());
            }
            pricing.setUpdatedBy(admin.getId());
        }

        pricing = filePricingRepository.save(pricing);

        log.info("파일 가격 설정 완료: fileId={}, isPaid={}, price={}",
                file.getId(), pricing.getIsPaid(), pricing.getPrice());

        return FilePricingResponse.from(pricing, file);
    }

    /**
     * 파일 가격 정보 조회
     */
    @Transactional(readOnly = true)
    public FilePricingResponse getPricing(UUID fileUuid) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        FilePricing pricing = filePricingRepository.findByFileId(file.getId())
                .orElse(null);

        if (pricing == null) {
            // 가격 정보가 없으면 무료로 간주
            return FilePricingResponse.builder()
                    .fileUuid(fileUuid)
                    .fileName(file.getOriginalFilename())
                    .isPaid(false)
                    .price(0)
                    .isActive(true)
                    .totalDownloads(0L)
                    .totalRevenue(0L)
                    .build();
        }

        // 통계 조회
        long totalDownloads = fileDownloadRepository.countByFileId(file.getId());
        Long totalRevenue = fileDownloadRepository.sumPricePaidByFileId(file.getId());

        return FilePricingResponse.from(pricing, file, totalDownloads, totalRevenue != null ? totalRevenue : 0L);
    }

    /**
     * 유료 파일 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<FilePricingResponse> getPaidFiles(Pageable pageable) {
        return filePricingRepository.findByIsPaidTrueAndIsActiveTrue(pageable)
                .map(pricing -> {
                    long totalDownloads = fileDownloadRepository.countByFileId(pricing.getFile().getId());
                    Long totalRevenue = fileDownloadRepository.sumPricePaidByFileId(pricing.getFile().getId());
                    return FilePricingResponse.from(pricing, pricing.getFile(),
                            totalDownloads, totalRevenue != null ? totalRevenue : 0L);
                });
    }

    /**
     * 파일 가격 비활성화
     */
    @Transactional
    public void deactivatePricing(UUID fileUuid, String adminEmail) {
        log.info("파일 가격 비활성화: fileUuid={}", fileUuid);

        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FilePricing pricing = filePricingRepository.findByFileId(file.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_PRICING_NOT_FOUND));

        pricing.deactivate();
        pricing.setUpdatedBy(admin.getId());
        filePricingRepository.save(pricing);

        log.info("파일 가격 비활성화 완료: fileId={}", file.getId());
    }

    /**
     * 파일 무료로 전환
     */
    @Transactional
    public FilePricingResponse setFree(UUID fileUuid, String adminEmail) {
        log.info("파일 무료 전환: fileUuid={}", fileUuid);

        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FilePricing pricing = filePricingRepository.findByFileId(file.getId())
                .orElse(null);

        if (pricing == null) {
            // 이미 무료
            return FilePricingResponse.builder()
                    .fileUuid(fileUuid)
                    .fileName(file.getOriginalFilename())
                    .isPaid(false)
                    .price(0)
                    .isActive(true)
                    .build();
        }

        pricing.setAsFree();
        pricing.setUpdatedBy(admin.getId());
        pricing = filePricingRepository.save(pricing);

        log.info("파일 무료 전환 완료: fileId={}", file.getId());

        return FilePricingResponse.from(pricing, file);
    }
}
