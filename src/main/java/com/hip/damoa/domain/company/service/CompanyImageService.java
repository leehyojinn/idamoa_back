package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.repository.CompanyImageRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.web.dto.CompanyImageRequest;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * 업체 이미지 추가
     */
    @Transactional
    public CompanyImage addCompanyImage(String userEmail, Long companyId, CompanyImageRequest request) {
        log.info("업체 이미지 추가: companyId={}, imageType={}", companyId, request.getImageType());

        // 사용자 및 권한 확인
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 소유자 또는 관리자만 가능
        if (!company.getOwner().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 대표 이미지 설정 시 기존 대표 이미지 해제
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            unsetPrimaryImages(company, request.getImageType());
        }

        // 이미지 생성
        CompanyImage image = CompanyImage.builder()
                .company(company)
                .imageUrl(request.getImageUrl())
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

        log.info("업체 이미지 추가 완료: id={}", image.getId());

        return image;
    }

    /**
     * 업체 이미지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CompanyImage> getCompanyImages(Long companyId) {
        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return companyImageRepository.findByCompanyAndIsDeletedFalseOrderByDisplayOrder(company);
    }

    /**
     * 업체 이미지 타입별 조회
     */
    @Transactional(readOnly = true)
    public List<CompanyImage> getCompanyImagesByType(Long companyId, String imageType) {
        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return companyImageRepository.findByCompanyAndImageTypeAndIsDeletedFalse(company, imageType);
    }

    /**
     * 업체 이미지 삭제
     */
    @Transactional
    public void deleteCompanyImage(String userEmail, Long imageId) {
        log.info("업체 이미지 삭제: imageId={}", imageId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyImage image = companyImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Company company = image.getCompany();

        // 소유자 또는 관리자만 가능
        if (!company.getOwner().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // Soft Delete
        image.softDelete();
        companyImageRepository.save(image);

        log.info("업체 이미지 삭제 완료: id={}", imageId);
    }

    /**
     * 대표 이미지 설정
     */
    @Transactional
    public CompanyImage setPrimaryImage(String userEmail, Long imageId) {
        log.info("대표 이미지 설정: imageId={}", imageId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyImage image = companyImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Company company = image.getCompany();

        // 소유자 또는 관리자만 가능
        if (!company.getOwner().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 기존 대표 이미지 해제
        unsetPrimaryImages(company, image.getImageType());

        // 대표 이미지 설정
        image.setPrimary();
        image = companyImageRepository.save(image);

        log.info("대표 이미지 설정 완료: id={}", imageId);

        return image;
    }

    /**
     * 기존 대표 이미지 해제
     */
    private void unsetPrimaryImages(Company company, String imageType) {
        List<CompanyImage> primaryImages = companyImageRepository
                .findByCompanyAndImageTypeAndIsDeletedFalse(company, imageType);

        for (CompanyImage img : primaryImages) {
            if (Boolean.TRUE.equals(img.getIsPrimary())) {
                img.unsetPrimary();
                companyImageRepository.save(img);
            }
        }
    }
}
