package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.model.CompanyLike;
import com.hip.damoa.domain.company.repository.CompanyImageRepository;
import com.hip.damoa.domain.company.repository.CompanyLikeRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.web.dto.CompanyCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyImageDto;
import com.hip.damoa.domain.company.web.dto.CompanySearchRequest;
import com.hip.damoa.domain.company.web.dto.CompanyUpdateRequest;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 업체 관리 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyImageRepository companyImageRepository;
    private final CompanyLikeRepository companyLikeRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    /**
     * 업체 등록 (업체 소유자용)
     */
    @Transactional
    public Company createCompany(String userEmail, CompanyCreateRequest request) {
        log.info("업체 등록 시작: userEmail={}, companyName={}", userEmail, request.getName());

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // COMPANY 역할 확인
        if (!user.hasRole("COMPANY")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 이미 업체가 있는지 확인 (한 사용자당 하나의 업체만)
        companyRepository.findByOwnerAndIsDeletedFalse(user).ifPresent(company -> {
            throw new BusinessException(ErrorCode.COMPANY_ALREADY_EXISTS);
        });

        // Slug 중복 확인
        if (request.getSlug() != null && companyRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.COMPANY_SLUG_ALREADY_EXISTS);
        }

        // 업체 생성
        Company company = Company.builder()
                .owner(user)
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .detailContent(request.getDetailContent())
                .detailContentFormat(request.getDetailContentFormat())
                .businessInfo(request.getBusinessInfo())
                .businessHours(request.getBusinessHours())
                .businessHoursNote(request.getBusinessHoursNote())
                .serviceAreas(request.getServiceAreas())
                .tags(request.getTags())
                .keywords(request.getKeywords())
                .primaryPhone(request.getPrimaryPhone())
                .secondaryPhone(request.getSecondaryPhone())
                .emergencyContact(request.getEmergencyContact())
                .email(request.getEmail())
                .websiteUrl(request.getWebsiteUrl())
                .kakaoChatUrl(request.getKakaoChatUrl())
                .socialLinks(request.getSocialLinks())
                .address(request.getAddress())
                .postalCode(request.getPostalCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status("ACTIVE")
                .build();

        company = companyRepository.save(company);

        // 이미지 처리
        processCompanyImages(company, request.getLogoImageUrl(),
                request.getCoverImageUrl(), request.getGalleryImageUrls());

        log.info("업체 등록 완료: id={}, name={}, logo={}, cover={}, gallery={}",
                company.getId(), company.getName(),
                request.getLogoImageUrl() != null,
                request.getCoverImageUrl() != null,
                request.getGalleryImageUrls() != null ? request.getGalleryImageUrls().length : 0);

        return company;
    }

    /**
     * 업체 등록 (관리자용)
     */
    @Transactional
    public Company createCompanyByAdmin(String adminEmail, Long ownerId, CompanyCreateRequest request) {
        log.info("업체 등록 (관리자): adminEmail={}, ownerId={}, companyName={}",
                 adminEmail, ownerId, request.getName());

        // 관리자 확인
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 소유자 조회
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Slug 중복 확인
        if (request.getSlug() != null && companyRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.COMPANY_SLUG_ALREADY_EXISTS);
        }

        // 업체 생성
        Company company = Company.builder()
                .owner(owner)
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .detailContent(request.getDetailContent())
                .detailContentFormat(request.getDetailContentFormat())
                .businessInfo(request.getBusinessInfo())
                .businessHours(request.getBusinessHours())
                .businessHoursNote(request.getBusinessHoursNote())
                .serviceAreas(request.getServiceAreas())
                .tags(request.getTags())
                .keywords(request.getKeywords())
                .primaryPhone(request.getPrimaryPhone())
                .secondaryPhone(request.getSecondaryPhone())
                .emergencyContact(request.getEmergencyContact())
                .email(request.getEmail())
                .websiteUrl(request.getWebsiteUrl())
                .kakaoChatUrl(request.getKakaoChatUrl())
                .socialLinks(request.getSocialLinks())
                .address(request.getAddress())
                .postalCode(request.getPostalCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status("ACTIVE")
                .build();

        company = companyRepository.save(company);

        // 이미지 처리
        processCompanyImages(company, request.getLogoImageUrl(),
                request.getCoverImageUrl(), request.getGalleryImageUrls());

        log.info("업체 등록 완료 (관리자): id={}, name={}, logo={}, cover={}, gallery={}",
                company.getId(), company.getName(),
                request.getLogoImageUrl() != null,
                request.getCoverImageUrl() != null,
                request.getGalleryImageUrls() != null ? request.getGalleryImageUrls().length : 0);

        return company;
    }

    /**
     * 업체 조회 (ID)
     */
    @Transactional
    public Company getCompany(Long companyId) {
        log.info("업체 조회: companyId={}", companyId);

        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 조회수 증가
        company.incrementViewCount();
        companyRepository.save(company);

        return company;
    }

    /**
     * 업체 조회 (Slug)
     */
    @Transactional
    public Company getCompanyBySlug(String slug) {
        log.info("업체 조회 (slug): slug={}", slug);

        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 삭제된 업체 확인
        if (company.getIsDeleted()) {
            throw new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND);
        }

        // 조회수 증가
        company.incrementViewCount();
        companyRepository.save(company);

        return company;
    }

    /**
     * 내 업체 조회 (업체 소유자용)
     */
    @Transactional(readOnly = true)
    public Company getMyCompany(String userEmail) {
        log.info("내 업체 조회: userEmail={}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return companyRepository.findByOwnerAndIsDeletedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));
    }

    /**
     * 업체 목록 조회 (전체, 관리자용)
     */
    @Transactional(readOnly = true)
    public Page<Company> getAllCompanies(Pageable pageable) {
        log.info("업체 목록 조회 (전체)");
        return companyRepository.findAll(pageable);
    }

    /**
     * 업체 목록 조회 (활성, 공개용)
     */
    @Transactional(readOnly = true)
    public Page<Company> getActiveCompanies(Pageable pageable) {
        log.info("업체 목록 조회 (활성)");
        return companyRepository.findByStatusAndIsDeletedFalse("ACTIVE", pageable);
    }

    /**
     * 업체 수정 (업체 소유자용)
     */
    @Transactional
    public Company updateCompany(String userEmail, Long companyId, CompanyUpdateRequest request) {
        log.info("업체 수정: companyId={}, userEmail={}", companyId, userEmail);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 업체 조회
        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 권한 확인 (소유자만)
        if (!company.getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // Slug 변경 시 중복 확인
        if (request.getSlug() != null && !request.getSlug().equals(company.getSlug())) {
            if (companyRepository.existsBySlug(request.getSlug())) {
                throw new BusinessException(ErrorCode.COMPANY_SLUG_ALREADY_EXISTS);
            }
        }

        // 업체 정보 업데이트
        company = updateCompanyFields(company, request);
        company = companyRepository.save(company);

        // 이미지 업데이트 (새로운 이미지가 있는 경우에만 처리)
        if (request.getLogoImageUrl() != null || request.getCoverImageUrl() != null ||
                (request.getGalleryImageUrls() != null && request.getGalleryImageUrls().length > 0)) {
            // 기존 이미지 삭제 (soft delete)
            deleteCompanyImages(company.getId());
            // 새 이미지 저장
            processCompanyImages(company, request.getLogoImageUrl(),
                    request.getCoverImageUrl(), request.getGalleryImageUrls());
        }

        log.info("업체 수정 완료: id={}", company.getId());

        return company;
    }

    /**
     * 업체 수정 (관리자용)
     */
    @Transactional
    public Company updateCompanyByAdmin(String adminEmail, Long companyId, CompanyUpdateRequest request) {
        log.info("업체 수정 (관리자): companyId={}, adminEmail={}", companyId, adminEmail);

        // 관리자 확인
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 업체 조회
        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // Slug 변경 시 중복 확인
        if (request.getSlug() != null && !request.getSlug().equals(company.getSlug())) {
            if (companyRepository.existsBySlug(request.getSlug())) {
                throw new BusinessException(ErrorCode.COMPANY_SLUG_ALREADY_EXISTS);
            }
        }

        // 업체 정보 업데이트 (관리자는 status, featured 등도 변경 가능)
        company = updateCompanyFields(company, request);
        company = companyRepository.save(company);

        // 이미지 업데이트 (새로운 이미지가 있는 경우에만 처리)
        if (request.getLogoImageUrl() != null || request.getCoverImageUrl() != null ||
                (request.getGalleryImageUrls() != null && request.getGalleryImageUrls().length > 0)) {
            // 기존 이미지 삭제 (soft delete)
            deleteCompanyImages(company.getId());
            // 새 이미지 저장
            processCompanyImages(company, request.getLogoImageUrl(),
                    request.getCoverImageUrl(), request.getGalleryImageUrls());
        }

        log.info("업체 수정 완료 (관리자): id={}", company.getId());

        return company;
    }

    /**
     * 업체 삭제 (업체 소유자용) - Soft Delete
     */
    @Transactional
    public void deleteCompany(String userEmail, Long companyId) {
        log.info("업체 삭제: companyId={}, userEmail={}", companyId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 권한 확인
        if (!company.getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // Soft Delete
        company.softDelete();
        companyRepository.save(company);

        log.info("업체 삭제 완료: id={}", company.getId());
    }

    /**
     * 업체 삭제 (관리자용) - Soft Delete
     */
    @Transactional
    public void deleteCompanyByAdmin(String adminEmail, Long companyId) {
        log.info("업체 삭제 (관리자): companyId={}, adminEmail={}", companyId, adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // Soft Delete
        company.softDelete();
        companyRepository.save(company);

        log.info("업체 삭제 완료 (관리자): id={}", company.getId());
    }

    /**
     * 업체 상태 변경 (관리자용)
     */
    @Transactional
    public Company changeCompanyStatus(String adminEmail, Long companyId, String status) {
        log.info("업체 상태 변경 (관리자): companyId={}, status={}", companyId, status);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        company.changeStatus(status);
        company = companyRepository.save(company);

        log.info("업체 상태 변경 완료: id={}, status={}", company.getId(), status);

        return company;
    }

    /**
     * 업체 인증 (관리자용)
     */
    @Transactional
    public Company verifyCompany(String adminEmail, Long companyId) {
        log.info("업체 인증 (관리자): companyId={}", companyId);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        company.verify();
        company = companyRepository.save(company);

        log.info("업체 인증 완료: id={}", company.getId());

        return company;
    }

    /**
     * 업체 검색 (공개용, 필터링 + 정렬)
     */
    @Transactional(readOnly = true)
    public Page<Company> searchCompanies(CompanySearchRequest searchRequest, Pageable pageable) {
        log.info("업체 검색: keyword={}, sortBy={}", searchRequest.getKeyword(), searchRequest.getSortBy());

        // 정렬 기준 결정
        Pageable sortedPageable = createSortedPageable(pageable, searchRequest.getSortBy());

        // 배열 파라미터 처리
        String[] serviceAreas = searchRequest.getServiceAreas();
        String[] tags = searchRequest.getTags();

        int serviceAreasSize = (serviceAreas != null) ? serviceAreas.length : 0;
        int tagsSize = (tags != null) ? tags.length : 0;

        // 검색 실행 (네이티브 쿼리 사용)
        return companyRepository.searchCompaniesWithFilters(
            searchRequest.getKeyword(),
            serviceAreas,
            serviceAreasSize,
            tags,
            tagsSize,
            searchRequest.getMinRating(),
            sortedPageable
        );
    }

    /**
     * 업체 이미지 조회 (최대 3개)
     */
    @Transactional(readOnly = true)
    public List<CompanyImageDto> getCompanyImages(Company company) {
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("displayOrder").ascending());
        List<CompanyImage> images = companyImageRepository
                .findByCompanyAndIsDeletedFalseOrderByDisplayOrder(company, pageRequest);

        return images.stream()
                .map(CompanyImageDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 업체 필드 업데이트 헬퍼 메서드
     */
    private Company updateCompanyFields(Company company, CompanyUpdateRequest request) {
        return Company.builder()
                .owner(company.getOwner())
                .name(request.getName() != null ? request.getName() : company.getName())
                .slug(request.getSlug() != null ? request.getSlug() : company.getSlug())
                .description(request.getDescription() != null ? request.getDescription() : company.getDescription())
                .detailContent(request.getDetailContent() != null ? request.getDetailContent() : company.getDetailContent())
                .detailContentFormat(request.getDetailContentFormat() != null ? request.getDetailContentFormat() : company.getDetailContentFormat())
                .businessInfo(request.getBusinessInfo() != null ? request.getBusinessInfo() : company.getBusinessInfo())
                .businessHours(request.getBusinessHours() != null ? request.getBusinessHours() : company.getBusinessHours())
                .businessHoursNote(request.getBusinessHoursNote() != null ? request.getBusinessHoursNote() : company.getBusinessHoursNote())
                .serviceAreas(request.getServiceAreas() != null ? request.getServiceAreas() : company.getServiceAreas())
                .tags(request.getTags() != null ? request.getTags() : company.getTags())
                .keywords(request.getKeywords() != null ? request.getKeywords() : company.getKeywords())
                .primaryPhone(request.getPrimaryPhone() != null ? request.getPrimaryPhone() : company.getPrimaryPhone())
                .secondaryPhone(request.getSecondaryPhone() != null ? request.getSecondaryPhone() : company.getSecondaryPhone())
                .emergencyContact(request.getEmergencyContact() != null ? request.getEmergencyContact() : company.getEmergencyContact())
                .email(request.getEmail() != null ? request.getEmail() : company.getEmail())
                .websiteUrl(request.getWebsiteUrl() != null ? request.getWebsiteUrl() : company.getWebsiteUrl())
                .kakaoChatUrl(request.getKakaoChatUrl() != null ? request.getKakaoChatUrl() : company.getKakaoChatUrl())
                .socialLinks(request.getSocialLinks() != null ? request.getSocialLinks() : company.getSocialLinks())
                .address(request.getAddress() != null ? request.getAddress() : company.getAddress())
                .postalCode(request.getPostalCode() != null ? request.getPostalCode() : company.getPostalCode())
                .latitude(request.getLatitude() != null ? request.getLatitude() : company.getLatitude())
                .longitude(request.getLongitude() != null ? request.getLongitude() : company.getLongitude())
                .avgRating(company.getAvgRating())
                .reviewCount(company.getReviewCount())
                .viewCount(company.getViewCount())
                .likeCount(company.getLikeCount())
                .portfolioCount(company.getPortfolioCount())
                .completedProjects(company.getCompletedProjects())
                .status(request.getStatus() != null ? request.getStatus() : company.getStatus())
                .featured(request.getFeatured() != null ? request.getFeatured() : company.getFeatured())
                .verified(company.getVerified())
                .verifiedAt(company.getVerifiedAt())
                .premiumUntil(company.getPremiumUntil())
                .premiumTier(company.getPremiumTier())
                .premiumMonthlyAmount(company.getPremiumMonthlyAmount())
                .build();
    }

    /**
     * 정렬 기준에 따른 Pageable 생성 (Native Query용 - DB 컬럼명 사용)
     */
    private Pageable createSortedPageable(Pageable pageable, String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return pageable;
        }

        Sort sort;
        switch (sortBy.toUpperCase()) {
            case "LATEST":
                sort = Sort.by(Sort.Direction.DESC, "created_at");
                break;
            case "POPULAR":
                // 인기순: 좋아요 + 조회수 복합 (좋아요 우선)
                sort = Sort.by(Sort.Direction.DESC, "like_count")
                        .and(Sort.by(Sort.Direction.DESC, "view_count"));
                break;
            case "RATING":
                sort = Sort.by(Sort.Direction.DESC, "avg_rating")
                        .and(Sort.by(Sort.Direction.DESC, "review_count"));
                break;
            case "REVIEW_COUNT":
                sort = Sort.by(Sort.Direction.DESC, "review_count");
                break;
            case "PREMIUM_TIER":
                // 프리미엄 등급순 (월정액 내림차순 → 프리미엄 등급 내림차순)
                sort = Sort.by(Sort.Direction.DESC, "premium_monthly_amount")
                        .and(Sort.by(Sort.Direction.DESC, "premium_tier"));
                break;
            default:
                sort = Sort.by(Sort.Direction.DESC, "created_at");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /**
     * 업체 이미지 처리
     * - files 테이블의 entity 정보 업데이트
     * - company_images 테이블에 이미지 정보 저장
     */
    private void processCompanyImages(Company company, String logoImageUrl,
                                      String coverImageUrl, String[] galleryImageUrls) {
        log.info("업체 이미지 처리 시작: companyId={}, logo={}, cover={}, gallery={}",
                company.getId(),
                logoImageUrl != null,
                coverImageUrl != null,
                galleryImageUrls != null ? galleryImageUrls.length : 0);

        int displayOrder = 0;
        int savedCount = 0;

        // 로고 이미지 처리
        if (logoImageUrl != null && !logoImageUrl.isEmpty()) {
            saveCompanyImage(company, logoImageUrl, "LOGO", true, displayOrder++);
            savedCount++;
        }

        // 커버 이미지 처리
        if (coverImageUrl != null && !coverImageUrl.isEmpty()) {
            saveCompanyImage(company, coverImageUrl, "COVER", false, displayOrder++);
            savedCount++;
        }

        // 갤러리 이미지 처리
        if (galleryImageUrls != null && galleryImageUrls.length > 0) {
            for (String galleryImageUrl : galleryImageUrls) {
                if (galleryImageUrl != null && !galleryImageUrl.isEmpty()) {
                    saveCompanyImage(company, galleryImageUrl, "GALLERY", false, displayOrder++);
                    savedCount++;
                }
            }
        }

        log.info("업체 이미지 처리 완료: companyId={}, 저장된 이미지 수={}", company.getId(), savedCount);
    }

    /**
     * 단일 업체 이미지 저장
     */
    private void saveCompanyImage(Company company, String imageUrl, String imageType,
                                  boolean isPrimary, int displayOrder) {
        // files 테이블에서 URL로 파일 조회
        File file = fileRepository.findByFileUrl(imageUrl)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // files 테이블의 entity 정보 업데이트
        file.updateEntityInfo("COMPANY_IMAGE", company.getId());
        fileRepository.save(file);

        // company_images 테이블에 저장
        CompanyImage companyImage = CompanyImage.builder()
                .company(company)
                .imageUrl(imageUrl)
                .imageType(imageType)
                .isPrimary(isPrimary)
                .displayOrder(displayOrder)
                .width(file.getImageMetadata() != null ? (Integer) file.getImageMetadata().get("width") : null)
                .height(file.getImageMetadata() != null ? (Integer) file.getImageMetadata().get("height") : null)
                .fileSize(file.getFileSize())
                .build();

        companyImageRepository.save(companyImage);

        log.info("이미지 저장 완료: companyId={}, imageType={}, displayOrder={}, url={}",
                company.getId(), imageType, displayOrder, imageUrl);
    }

    /**
     * 업체 이미지 삭제 (Soft Delete)
     */
    private void deleteCompanyImages(Long companyId) {
        log.info("기존 업체 이미지 삭제 시작: companyId={}", companyId);

        List<CompanyImage> images = companyImageRepository.findByCompany_IdAndIsDeletedFalse(companyId);

        for (CompanyImage image : images) {
            image.softDelete();
            companyImageRepository.save(image);
        }

        log.info("기존 업체 이미지 삭제 완료: companyId={}, 삭제된 이미지 수={}", companyId, images.size());
    }

    /**
     * 사용자가 이미 업체를 보유하고 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasCompany(String userEmail) {
        return companyRepository.existsByOwnerEmailAndIsDeletedFalse(userEmail);
    }

    /**
     * 업체 좋아요 토글 (좋아요 추가 or 취소)
     */
    @Transactional
    public boolean toggleLike(String userEmail, Long companyId) {
        log.info("업체 좋아요 토글: userEmail={}, companyId={}", userEmail, companyId);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 업체 조회
        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 이미 좋아요를 눌렀는지 확인
        boolean alreadyLiked = companyLikeRepository.existsByCompanyAndUser(company, user);

        if (alreadyLiked) {
            // 좋아요 취소
            companyLikeRepository.deleteByCompanyAndUser(company, user);
            company.decrementLikeCount();
            companyRepository.save(company);
            log.info("업체 좋아요 취소: companyId={}, userId={}, likeCount={}",
                    companyId, user.getId(), company.getLikeCount());
            return false;
        } else {
            // 좋아요 추가
            CompanyLike like = CompanyLike.builder()
                    .company(company)
                    .user(user)
                    .build();
            companyLikeRepository.save(like);
            company.incrementLikeCount();
            companyRepository.save(company);
            log.info("업체 좋아요 추가: companyId={}, userId={}, likeCount={}",
                    companyId, user.getId(), company.getLikeCount());
            return true;
        }
    }

    /**
     * 사용자가 업체에 좋아요를 눌렀는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isLiked(String userEmail, Long companyId) {
        if (userEmail == null) {
            return false;
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        Company company = companyRepository.findByIdAndIsDeletedFalse(companyId).orElse(null);
        if (company == null) {
            return false;
        }

        return companyLikeRepository.existsByCompanyAndUser(company, user);
    }
}
