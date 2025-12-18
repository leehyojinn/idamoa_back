package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.repository.CompanyImageRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.web.dto.CompanyImageRequest;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 업체 이미지 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyImageService {

    private final CompanyImageRepository companyImageRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    /**
     * 업체 이미지 추가
     */
    @Transactional
    public CompanyImage addCompanyImage(String userEmail, UUID companyUuid, CompanyImageRequest request) {
        log.info("업체 이미지 추가: companyUuid={}, imageType={}", companyUuid, request.getImageType());

        // 사용자 및 권한 확인
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 소유자 또는 관리자만 가능
        if (!company.getOwner().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.NOT_COMPANY_OWNER);
        }

        // 대표 이미지 설정 시 기존 대표 이미지 해제
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            unsetPrimaryImages(company, request.getImageType());
        }

        // UUID → File ID 변환
        Long fileId = convertUuidToFileId(request.getFileUuid());

        // 이미지 생성
        CompanyImage image = CompanyImage.builder()
                .company(company)
                .fileId(fileId)
                .imageType(request.getImageType())
                .isPrimary(request.getIsPrimary())
                .displayOrder(request.getDisplayOrder())
                .title(request.getTitle())
                .description(request.getDescription())
                .width(request.getWidth())
                .height(request.getHeight())
                .fileSize(request.getFileSize())
                .build();

        image = companyImageRepository.save(image);

        log.info("업체 이미지 추가 완료: id={}, fileId={}", image.getId(), fileId);

        return image;
    }

    /**
     * 업체 이미지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CompanyImage> getCompanyImages(UUID companyUuid) {
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return companyImageRepository.findByCompanyAndIsDeletedFalseOrderByDisplayOrder(company);
    }

    /**
     * 업체 이미지 타입별 조회
     */
    @Transactional(readOnly = true)
    public List<CompanyImage> getCompanyImagesByType(UUID companyUuid, String imageType) {
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return companyImageRepository.findByCompanyAndImageTypeAndIsDeletedFalse(company, imageType);
    }

    /**
     * 업체 이미지 삭제
     */
    @Transactional
    public void deleteCompanyImage(String userEmail, UUID imageUuid) {
        log.info("업체 이미지 삭제: imageUuid={}", imageUuid);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyImage image = companyImageRepository.findByUuidAndIsDeletedFalse(imageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Company company = image.getCompany();

        // 소유자 또는 관리자만 가능
        if (!company.getOwner().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.NOT_COMPANY_OWNER);
        }

        // Soft Delete
        image.softDelete();
        companyImageRepository.save(image);

        log.info("업체 이미지 삭제 완료: id={}", imageUuid);
    }

    /**
     * 대표 이미지 설정
     */
    @Transactional
    public CompanyImage setPrimaryImage(String userEmail, UUID imageUuid) {
        log.info("대표 이미지 설정: imageUuid={}", imageUuid);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyImage image = companyImageRepository.findByUuidAndIsDeletedFalse(imageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Company company = image.getCompany();

        // 소유자 또는 관리자만 가능
        if (!company.getOwner().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.NOT_COMPANY_OWNER);
        }

        // 기존 대표 이미지 해제
        unsetPrimaryImages(company, image.getImageType());

        // 대표 이미지 설정
        image.setPrimary();
        image = companyImageRepository.save(image);

        log.info("대표 이미지 설정 완료: id={}", imageUuid);

        return image;
    }

    /**
     * 기존 대표 이미지 해제
     */
    private void unsetPrimaryImages(Company company, String imageType) {
        List<CompanyImage> primaryImages = companyImageRepository
                .findByCompanyAndImageTypeAndIsDeletedFalse(company, imageType);

        // [N+1 최적화] saveAll 일괄 저장
        List<CompanyImage> toUpdate = primaryImages.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .peek(CompanyImage::unsetPrimary)
                .toList();

        if (!toUpdate.isEmpty()) {
            companyImageRepository.saveAll(toUpdate);
        }
    }

    /**
     * CompanyImage 엔티티를 Response DTO로 변환 (File ID → URL, UUID 변환 포함)
     */
    public com.hip.damoa.domain.company.web.dto.CompanyImageResponse toResponse(CompanyImage image) {
        if (image.getFileId() == null) {
            return com.hip.damoa.domain.company.web.dto.CompanyImageResponse.from(image, null, null);
        }

        // File 한 번만 조회해서 URL과 UUID 모두 가져오기
        File file = fileRepository.findById(image.getFileId()).orElse(null);
        String imageUrl = (file != null) ? file.getFileUrl() : null;
        UUID fileUuid = (file != null) ? file.getUuid() : null;

        return com.hip.damoa.domain.company.web.dto.CompanyImageResponse.from(image, imageUrl, fileUuid);
    }

    /**
     * [N+1 최적화] CompanyImage 리스트를 Response DTO 리스트로 일괄 변환
     */
    public List<com.hip.damoa.domain.company.web.dto.CompanyImageResponse> toResponses(List<CompanyImage> images) {
        if (images.isEmpty()) {
            return Collections.emptyList();
        }

        // 파일 ID 일괄 조회
        Map<Long, File> fileMap = getFileMap(images);

        return images.stream()
                .map(image -> {
                    File file = (image.getFileId() != null) ? fileMap.get(image.getFileId()) : null;
                    String imageUrl = (file != null) ? file.getFileUrl() : null;
                    UUID fileUuid = (file != null) ? file.getUuid() : null;
                    return com.hip.damoa.domain.company.web.dto.CompanyImageResponse.from(image, imageUrl, fileUuid);
                })
                .toList();
    }

    /**
     * CompanyImage 엔티티를 DTO로 변환 (File ID → URL, UUID 변환 포함)
     */
    public com.hip.damoa.domain.company.web.dto.CompanyImageDto toDto(CompanyImage image) {
        if (image.getFileId() == null) {
            return com.hip.damoa.domain.company.web.dto.CompanyImageDto.from(image, null, null);
        }

        // File 한 번만 조회해서 URL과 UUID 모두 가져오기
        File file = fileRepository.findById(image.getFileId()).orElse(null);
        String imageUrl = (file != null) ? file.getFileUrl() : null;
        UUID fileUuid = (file != null) ? file.getUuid() : null;

        return com.hip.damoa.domain.company.web.dto.CompanyImageDto.from(image, imageUrl, fileUuid);
    }

    /**
     * [N+1 최적화] CompanyImage 리스트를 DTO 리스트로 일괄 변환
     */
    public List<com.hip.damoa.domain.company.web.dto.CompanyImageDto> toDtos(List<CompanyImage> images) {
        if (images.isEmpty()) {
            return Collections.emptyList();
        }

        // 파일 ID 일괄 조회
        Map<Long, File> fileMap = getFileMap(images);

        return images.stream()
                .map(image -> {
                    File file = (image.getFileId() != null) ? fileMap.get(image.getFileId()) : null;
                    String imageUrl = (file != null) ? file.getFileUrl() : null;
                    UUID fileUuid = (file != null) ? file.getUuid() : null;
                    return com.hip.damoa.domain.company.web.dto.CompanyImageDto.from(image, imageUrl, fileUuid);
                })
                .toList();
    }

    /**
     * [N+1 최적화] 이미지 목록에서 파일 맵 생성
     */
    private Map<Long, File> getFileMap(List<CompanyImage> images) {
        List<Long> fileIds = images.stream()
                .map(CompanyImage::getFileId)
                .filter(id -> id != null)
                .toList();

        if (fileIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return fileRepository.findByIdIn(fileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f));
    }

    /**
     * UUID를 File ID로 변환
     */
    private Long convertUuidToFileId(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return null;
        }

        UUID uuid = UUID.fromString(uuidString);
        return fileRepository.findByUuidAndIsDeletedFalse(uuid)
                .map(File::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    /**
     * File ID를 URL로 변환
     */
    private String convertFileIdToUrl(Long fileId) {
        if (fileId == null) {
            return null;
        }

        return fileRepository.findById(fileId)
                .map(File::getFileUrl)
                .orElse(null);
    }
}
