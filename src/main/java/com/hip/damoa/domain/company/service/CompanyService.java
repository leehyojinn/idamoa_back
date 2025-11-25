package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyFilterOption;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.model.CompanyLike;
import com.hip.damoa.domain.company.repository.CompanyFilterOptionRepository;
import com.hip.damoa.domain.company.repository.CompanyImageRepository;
import com.hip.damoa.domain.company.repository.CompanyLikeRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.web.dto.CompanyCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyFilterGroupDto;
import com.hip.damoa.domain.company.web.dto.CompanyImageDto;
import com.hip.damoa.domain.company.web.dto.CompanySearchRequest;
import com.hip.damoa.domain.company.web.dto.CompanyUpdateRequest;
import com.hip.damoa.domain.company.web.dto.FilterOptionDto;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.filter.model.FilterOption;
import com.hip.damoa.domain.filter.repository.FilterOptionRepository;
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
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
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
    private final CompanyFilterOptionRepository companyFilterOptionRepository;
    private final FilterOptionRepository filterOptionRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final CompanyImageService companyImageService;

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
                // serviceAreas 제거 - 필터로 관리
                .tags(request.getTags())
                // keywords 제거됨
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

        // 필터 옵션 처리
        processFilterOptions(company, request.getFilterOptionIds());

        // 이미지 처리
        processCompanyImages(company, request.getLogoImageUuid(),
                request.getCoverImageUuid(), request.getGalleryImageUuids());

        log.info("업체 등록 완료: id={}, name={}, logo={}, cover={}, gallery={}",
                company.getId(), company.getName(),
                request.getLogoImageUuid() != null,
                request.getCoverImageUuid() != null,
                request.getGalleryImageUuids() != null ? request.getGalleryImageUuids().length : 0);

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

        // 소유자 조회 (ownerId가 있는 경우만)
        User owner = null;
        if (ownerId != null) {
            owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

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
                // serviceAreas 제거 - 필터로 관리
                .tags(request.getTags())
                // keywords 제거됨
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

        // 필터 옵션 처리
        processFilterOptions(company, request.getFilterOptionIds());

        // 이미지 처리
        processCompanyImages(company, request.getLogoImageUuid(),
                request.getCoverImageUuid(), request.getGalleryImageUuids());

        log.info("업체 등록 완료 (관리자): id={}, name={}, logo={}, cover={}, gallery={}",
                company.getId(), company.getName(),
                request.getLogoImageUuid() != null,
                request.getCoverImageUuid() != null,
                request.getGalleryImageUuids() != null ? request.getGalleryImageUuids().length : 0);

        return company;
    }

    /**
     * 업체 조회 (UUID)
     */
    @Transactional
    public Company getCompany(UUID companyUuid) {
        log.info("업체 조회: companyUuid={}", companyUuid);

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
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
    public Company updateCompany(String userEmail, UUID companyUuid, CompanyUpdateRequest request) {
        log.info("업체 수정: companyUuid={}, userEmail={}", companyUuid, userEmail);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 업체 조회
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
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

        // 필터 옵션 스마트 업데이트 (filterOptionIds가 제공된 경우에만)
        if (request.getFilterOptionIds() != null) {
            updateFilterOptionsSmart(company, request.getFilterOptionIds());
        }

        // 이미지 업데이트 (새로운 이미지가 있는 경우에만 처리)
        if (request.getLogoImageUuid() != null || request.getCoverImageUuid() != null ||
                (request.getGalleryImageUuids() != null && request.getGalleryImageUuids().length > 0)) {
            // 기존 이미지 삭제 (soft delete)
            deleteCompanyImages(company.getId());
            // 새 이미지 저장
            processCompanyImages(company, request.getLogoImageUuid(),
                    request.getCoverImageUuid(), request.getGalleryImageUuids());
        }

        log.info("업체 수정 완료: id={}", company.getId());

        return company;
    }

    /**
     * 업체 수정 (관리자용)
     */
    @Transactional
    public Company updateCompanyByAdmin(String adminEmail, UUID companyUuid, CompanyUpdateRequest request) {
        log.info("업체 수정 (관리자): companyUuid={}, adminEmail={}", companyUuid, adminEmail);

        // 관리자 확인
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 업체 조회
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
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

        // 필터 옵션 스마트 업데이트 (filterOptionIds가 제공된 경우에만)
        if (request.getFilterOptionIds() != null) {
            updateFilterOptionsSmart(company, request.getFilterOptionIds());
        }

        // 이미지 업데이트 (새로운 이미지가 있는 경우에만 처리)
        if (request.getLogoImageUuid() != null || request.getCoverImageUuid() != null ||
                (request.getGalleryImageUuids() != null && request.getGalleryImageUuids().length > 0)) {
            // 기존 이미지 삭제 (soft delete)
            deleteCompanyImages(company.getId());
            // 새 이미지 저장
            processCompanyImages(company, request.getLogoImageUuid(),
                    request.getCoverImageUuid(), request.getGalleryImageUuids());
        }

        log.info("업체 수정 완료 (관리자): id={}", company.getId());

        return company;
    }

    /**
     * 업체 삭제 (업체 소유자용) - Soft Delete
     */
    @Transactional
    public void deleteCompany(String userEmail, UUID companyUuid) {
        log.info("업체 삭제: companyUuid={}, userEmail={}", companyUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
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
    public void deleteCompanyByAdmin(String adminEmail, UUID companyUuid) {
        log.info("업체 삭제 (관리자): companyUuid={}, adminEmail={}", companyUuid, adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
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
    public Company changeCompanyStatus(String adminEmail, UUID companyUuid, String status) {
        log.info("업체 상태 변경 (관리자): companyUuid={}, status={}", companyUuid, status);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
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
    public Company verifyCompany(String adminEmail, UUID companyUuid) {
        log.info("업체 인증 (관리자): companyUuid={}", companyUuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        company.verify();
        company = companyRepository.save(company);

        log.info("업체 인증 완료: id={}", company.getId());

        return company;
    }

    /**
     * 업체 검색 (공개용, 필터링 + 정렬)
     * 필터가 1순위로 적용되고, 키워드는 필터링된 결과 내에서 검색
     */
    @Transactional(readOnly = true)
    public Page<Company> searchCompanies(CompanySearchRequest searchRequest, Pageable pageable) {
        log.info("업체 검색: keyword={}, filtersByCategory={}, sortBy={}",
            searchRequest.getKeyword(),
            searchRequest.getFiltersByCategory(),
            searchRequest.getSortBy());

        // 카테고리별 필터 옵션 가져오기
        java.util.Map<Long, List<Long>> filterOptionsByCategory = searchRequest.getFiltersByCategory();

        // null 체크 및 빈 Map으로 초기화
        if (filterOptionsByCategory == null) {
            filterOptionsByCategory = new java.util.HashMap<>();
        }

        // 각 카테고리별 필터 옵션 로그
        if (!filterOptionsByCategory.isEmpty()) {
            filterOptionsByCategory.forEach((categoryId, optionIds) -> {
                log.info("카테고리 {}: 필터 옵션 {}", categoryId, optionIds);
            });
        }

        // Native Query용 정렬 생성 (DB 컬럼명 사용)
        Pageable nativeQueryPageable = createNativeQuerySortedPageable(pageable, searchRequest.getSortBy());

        Page<Company> results;

        // 필터가 없으면 전체 조회 (키워드 검색만 적용)
        if (filterOptionsByCategory.isEmpty()) {
            log.info("필터가 없어 전체 업체를 조회합니다. 키워드: {}", searchRequest.getKeyword());

            // 필터 없이 키워드와 최소 평점만으로 검색
            results = companyRepository.searchWithFiltersAndKeyword(
                searchRequest.getKeyword(),
                searchRequest.getMinRating(),
                false, // hasFilters = false (필터 없음)
                new Long[0], // 빈 배열
                0, // categoryCount = 0
                nativeQueryPageable
            );
        } else {
            // 필터가 있으면 필터 적용
            log.info("필터를 적용하여 업체를 조회합니다.");

            // 모든 필터 옵션 ID를 하나의 배열로 변환
            List<Long> allFilterOptionIds = new java.util.ArrayList<>();
            filterOptionsByCategory.values().forEach(allFilterOptionIds::addAll);
            Long[] filterOptionIdArray = allFilterOptionIds.toArray(new Long[0]);

            // 카테고리 개수 (AND 조건 확인용)
            Integer categoryCount = filterOptionsByCategory.size();

            // 통합 검색 실행 (필터 + 키워드)
            results = companyRepository.searchWithFiltersAndKeyword(
                searchRequest.getKeyword(),
                searchRequest.getMinRating(),
                true, // hasFilters = true (필터 있음)
                filterOptionIdArray,
                categoryCount,
                nativeQueryPageable
            );
        }

        log.info("검색 결과: {} 건", results.getTotalElements());

        return results;
    }

    /**
     * 업체 이미지 조회 (최대 3개)
     */
    @Transactional(readOnly = true)
    public List<CompanyImageDto> getCompanyImages(Company company) {
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("displayOrder").ascending());
        List<CompanyImage> images = companyImageRepository
                .findByCompanyAndIsDeletedFalseOrderByDisplayOrder(company, pageRequest);

        // CompanyImageService를 사용하여 File ID → URL 변환
        return images.stream()
                .map(companyImageService::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 업체 필드 업데이트 헬퍼 메서드 (기존 엔티티 수정 방식)
     */
    private Company updateCompanyFields(Company company, CompanyUpdateRequest request) {
        // 기존 엔티티의 updateAllFields 메서드를 호출하여 필드 업데이트
        company.updateAllFields(
                request.getName(),
                request.getSlug(),
                request.getDescription(),
                request.getDetailContent(),
                request.getDetailContentFormat(),
                request.getBusinessInfo(),
                request.getBusinessHours(),
                request.getBusinessHoursNote(),
                // serviceAreas 제거 - 필터로 관리
                request.getTags(),
                // keywords 제거됨
                request.getPrimaryPhone(),
                request.getSecondaryPhone(),
                request.getEmergencyContact(),
                request.getEmail(),
                request.getWebsiteUrl(),
                request.getKakaoChatUrl(),
                request.getSocialLinks(),
                request.getAddress(),
                request.getPostalCode(),
                request.getLatitude(),
                request.getLongitude(),
                request.getStatus(),
                request.getFeatured()
        );

        return company;
    }

    /**
     * 정렬 기준에 따른 Pageable 생성 (JPA Specification용 - 엔티티 필드명 사용)
     */
    private Pageable createSortedPageable(Pageable pageable, String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return pageable;
        }

        Sort sort;
        switch (sortBy.toUpperCase()) {
            case "LATEST":
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            case "POPULAR":
                // 인기순: 좋아요 + 조회수 복합 (좋아요 우선)
                sort = Sort.by(Sort.Direction.DESC, "likeCount")
                        .and(Sort.by(Sort.Direction.DESC, "viewCount"));
                break;
            case "RATING":
                sort = Sort.by(Sort.Direction.DESC, "avgRating")
                        .and(Sort.by(Sort.Direction.DESC, "reviewCount"));
                break;
            case "REVIEW_COUNT":
                sort = Sort.by(Sort.Direction.DESC, "reviewCount");
                break;
            case "PREMIUM_TIER":
                // 프리미엄 등급순 (월정액 내림차순 → 프리미엄 등급 내림차순)
                sort = Sort.by(Sort.Direction.DESC, "premiumMonthlyAmount")
                        .and(Sort.by(Sort.Direction.DESC, "premiumTier"));
                break;
            default:
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /**
     * 정렬 기준에 따른 Pageable 생성 (Native Query용 - DB 컬럼명 사용)
     */
    private Pageable createNativeQuerySortedPageable(Pageable pageable, String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "LATEST";
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
     * UUID를 File ID로 변환
     */
    private Long convertUuidToFileId(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidString);
            return fileRepository.findByUuidAndIsDeletedFalse(uuid)
                    .map(File::getId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * 업체 이미지 처리 (UUID 기반)
     * - files 테이블의 entity 정보 업데이트
     * - company_images 테이블에 이미지 정보 저장
     */
    private void processCompanyImages(Company company, String logoImageUuid,
                                      String coverImageUuid, String[] galleryImageUuids) {
        log.info("업체 이미지 처리 시작: companyId={}, logo={}, cover={}, gallery={}",
                company.getId(),
                logoImageUuid != null,
                coverImageUuid != null,
                galleryImageUuids != null ? galleryImageUuids.length : 0);

        int displayOrder = 0;
        int savedCount = 0;

        // 로고 이미지 처리
        if (logoImageUuid != null && !logoImageUuid.isEmpty()) {
            saveCompanyImage(company, logoImageUuid, "LOGO", true, displayOrder++);
            savedCount++;
        }

        // 커버 이미지 처리
        if (coverImageUuid != null && !coverImageUuid.isEmpty()) {
            saveCompanyImage(company, coverImageUuid, "COVER", false, displayOrder++);
            savedCount++;
        }

        // 갤러리 이미지 처리
        if (galleryImageUuids != null && galleryImageUuids.length > 0) {
            for (String galleryImageUuid : galleryImageUuids) {
                if (galleryImageUuid != null && !galleryImageUuid.isEmpty()) {
                    saveCompanyImage(company, galleryImageUuid, "GALLERY", false, displayOrder++);
                    savedCount++;
                }
            }
        }

        log.info("업체 이미지 처리 완료: companyId={}, 저장된 이미지 수={}", company.getId(), savedCount);
    }

    /**
     * 단일 업체 이미지 저장 (UUID 기반)
     */
    private void saveCompanyImage(Company company, String imageUuid, String imageType,
                                  boolean isPrimary, int displayOrder) {
        // UUID → File ID 변환
        Long fileId = convertUuidToFileId(imageUuid);

        // files 테이블에서 파일 조회
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // files 테이블의 entity 정보 업데이트
        file.updateEntityInfo("COMPANY_IMAGE", company.getId());
        fileRepository.save(file);

        // company_images 테이블에 저장
        CompanyImage companyImage = CompanyImage.builder()
                .company(company)
                .fileId(file.getId())  // UUID → File ID 변환 완료
                .imageType(imageType)
                .isPrimary(isPrimary)
                .displayOrder(displayOrder)
                .width(file.getImageMetadata() != null ? (Integer) file.getImageMetadata().get("width") : null)
                .height(file.getImageMetadata() != null ? (Integer) file.getImageMetadata().get("height") : null)
                .fileSize(file.getFileSize())
                .build();

        companyImageRepository.save(companyImage);

        log.info("이미지 저장 완료: companyId={}, imageType={}, displayOrder={}, uuid={}",
                company.getId(), imageType, displayOrder, imageUuid);
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
    public boolean toggleLike(String userEmail, UUID companyUuid) {
        log.info("업체 좋아요 토글: userEmail={}, companyUuid={}", userEmail, companyUuid);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 업체 조회
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 이미 좋아요를 눌렀는지 확인
        boolean alreadyLiked = companyLikeRepository.existsByCompanyAndUser(company, user);

        if (alreadyLiked) {
            // 좋아요 취소
            companyLikeRepository.deleteByCompanyAndUser(company, user);
            company.decrementLikeCount();
            companyRepository.save(company);
            log.info("업체 좋아요 취소: companyUuid={}, userId={}, likeCount={}",
                    companyUuid, user.getId(), company.getLikeCount());
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
            log.info("업체 좋아요 추가: companyUuid={}, userId={}, likeCount={}",
                    companyUuid, user.getId(), company.getLikeCount());
            return true;
        }
    }

    /**
     * 사용자가 업체에 좋아요를 눌렀는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isLiked(String userEmail, UUID companyUuid) {
        if (userEmail == null) {
            return false;
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid).orElse(null);
        if (company == null) {
            return false;
        }

        return companyLikeRepository.existsByCompanyAndUser(company, user);
    }

    /**
     * 업체의 필터 옵션 조회
     */
    @Transactional(readOnly = true)
    public List<com.hip.damoa.domain.company.web.dto.FilterOptionDto> getCompanyFilterOptions(Company company) {
        List<CompanyFilterOption> companyFilterOptions = companyFilterOptionRepository.findByCompany(company);

        return companyFilterOptions.stream()
                .map(cfo -> com.hip.damoa.domain.company.web.dto.FilterOptionDto.from(cfo.getFilterOption()))
                .collect(Collectors.toList());
    }

    /**
     * 필터 옵션 처리 (저장)
     */
    private void processFilterOptions(Company company, List<Long> filterOptionIds) {
        if (filterOptionIds == null || filterOptionIds.isEmpty()) {
            log.info("필터 옵션 없음: companyId={}", company.getId());
            return;
        }

        log.info("필터 옵션 처리 시작: companyId={}, filterOptionCount={}",
                company.getId(), filterOptionIds.size());

        // 필터 옵션 조회
        List<FilterOption> filterOptions = filterOptionRepository.findByIdIn(filterOptionIds);

        if (filterOptions.size() != filterOptionIds.size()) {
            log.warn("일부 필터 옵션을 찾을 수 없음: 요청={}, 조회={}",
                    filterOptionIds.size(), filterOptions.size());
        }

        // CompanyFilterOption 엔티티 생성 및 저장
        List<CompanyFilterOption> companyFilterOptions = filterOptions.stream()
                .map(filterOption -> CompanyFilterOption.builder()
                        .company(company)
                        .filterOption(filterOption)
                        .build())
                .collect(Collectors.toList());

        companyFilterOptionRepository.saveAll(companyFilterOptions);

        log.info("필터 옵션 저장 완료: companyId={}, 저장된 옵션 수={}",
                company.getId(), companyFilterOptions.size());
    }

    /**
     * 필터 옵션 업데이트 (기존 것 삭제 후 새로 저장)
     */
    private void updateFilterOptions(Company company, List<Long> filterOptionIds) {
        // 기존 필터 옵션 삭제
        companyFilterOptionRepository.deleteByCompany(company);
        log.info("기존 필터 옵션 삭제 완료: companyId={}", company.getId());

        // 새 필터 옵션 저장
        processFilterOptions(company, filterOptionIds);
    }

    /**
     * 업체 필터를 카테고리별로 그룹화
     */
    public List<CompanyFilterGroupDto> getCompanyFilterGroups(Company company) {
        List<CompanyFilterOption> filterOptions = companyFilterOptionRepository.findByCompany(company);

        // 카테고리별로 그룹화
        java.util.Map<com.hip.damoa.domain.filter.model.FilterCategory, List<CompanyFilterOption>> groupedByCategory =
            filterOptions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    cfo -> cfo.getFilterOption().getCategory()
                ));

        // DTO로 변환
        return groupedByCategory.entrySet().stream()
            .map(entry -> {
                com.hip.damoa.domain.filter.model.FilterCategory category = entry.getKey();
                List<FilterOptionDto> options = entry.getValue().stream()
                    .map(cfo -> FilterOptionDto.from(cfo.getFilterOption()))
                    .collect(java.util.stream.Collectors.toList());

                return CompanyFilterGroupDto.builder()
                    .categoryId(category.getId())
                    .categoryCode(category.getCode())
                    .categoryName(category.getName())
                    .categoryDescription(category.getDescription())
                    .options(options)
                    .build();
            })
            .sorted((g1, g2) -> {
                // 카테고리 코드로 정렬 (REGION -> SPECIALTY -> DEPARTMENT 등)
                return g1.getCategoryCode().compareTo(g2.getCategoryCode());
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 업체 수정 시 필터 옵션 스마트 업데이트
     * - 동일한 것은 유지
     * - 삭제된 것은 제거
     * - 새로 추가된 것은 추가
     */
    private void updateFilterOptionsSmart(Company company, List<Long> newFilterOptionIds) {
        if (newFilterOptionIds == null) {
            return; // null이면 변경하지 않음
        }

        // 현재 필터 옵션 ID들
        List<CompanyFilterOption> currentOptions = companyFilterOptionRepository.findByCompany(company);
        Set<Long> currentOptionIds = currentOptions.stream()
            .map(cfo -> cfo.getFilterOption().getId())
            .collect(java.util.stream.Collectors.toSet());

        Set<Long> newOptionIdSet = new HashSet<>(newFilterOptionIds);

        // 삭제할 항목 (현재는 있지만 새 목록에는 없음)
        List<CompanyFilterOption> toDelete = currentOptions.stream()
            .filter(cfo -> !newOptionIdSet.contains(cfo.getFilterOption().getId()))
            .collect(java.util.stream.Collectors.toList());

        // 추가할 항목 (새 목록에는 있지만 현재는 없음)
        List<Long> toAdd = newFilterOptionIds.stream()
            .filter(id -> !currentOptionIds.contains(id))
            .collect(java.util.stream.Collectors.toList());

        // 삭제 처리
        if (!toDelete.isEmpty()) {
            companyFilterOptionRepository.deleteAll(toDelete);
            log.info("필터 옵션 삭제: companyId={}, 삭제 개수={}", company.getId(), toDelete.size());
        }

        // 추가 처리
        if (!toAdd.isEmpty()) {
            List<FilterOption> filterOptionsToAdd = filterOptionRepository.findAllById(toAdd);
            List<CompanyFilterOption> newCompanyFilterOptions = filterOptionsToAdd.stream()
                .map(filterOption -> CompanyFilterOption.builder()
                    .company(company)
                    .filterOption(filterOption)
                    .build())
                .collect(java.util.stream.Collectors.toList());

            companyFilterOptionRepository.saveAll(newCompanyFilterOptions);
            log.info("필터 옵션 추가: companyId={}, 추가 개수={}", company.getId(), newCompanyFilterOptions.size());
        }

        if (toDelete.isEmpty() && toAdd.isEmpty()) {
            log.info("필터 옵션 변경 없음: companyId={}", company.getId());
        }
    }
}
