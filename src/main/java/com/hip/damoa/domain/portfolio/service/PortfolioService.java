package com.hip.damoa.domain.portfolio.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.company.repository.CompanyPortfolioRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.filter.model.FilterOption;
import com.hip.damoa.domain.filter.repository.FilterOptionRepository;
import com.hip.damoa.domain.payment.service.CreditService;
import com.hip.damoa.domain.portfolio.model.*;
import com.hip.damoa.domain.portfolio.repository.*;
import static com.hip.damoa.domain.portfolio.repository.PortfolioSpecifications.*;
import com.hip.damoa.domain.portfolio.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 포트폴리오 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final String ENTITY_TYPE_PORTFOLIO = "PORTFOLIO";

    private final CompanyPortfolioRepository portfolioRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FilterOptionRepository filterOptionRepository;
    private final PortfolioFilterOptionRepository portfolioFilterOptionRepository;
    private final PortfolioBookmarkRepository bookmarkRepository;
    private final PortfolioLikeRepository likeRepository;
    private final PortfolioPromotionRepository promotionRepository;
    private final PortfolioPromotionService promotionService;
    private final PortfolioPromotionSettingsService settingsService;
    private final PortfolioAttachmentRepository attachmentRepository;
    private final CreditService creditService;

    // ===== 생성 =====

    /**
     * 포트폴리오 생성
     */
    @Transactional
    public PortfolioResponse createPortfolio(String userEmail, PortfolioCreateRequest request) {
        log.info("포트폴리오 생성 시작: userEmail={}, title={}", userEmail, request.getTitle());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 업체 확인 (업체 소유자만 등록 가능)
        Company company = companyRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 이미지 검증
        if (request.getImageUuids() == null || request.getImageUuids().isEmpty()) {
            throw new BusinessException(ErrorCode.PORTFOLIO_IMAGE_REQUIRED);
        }

        // 우대 신청 시 크레딧 잔액 확인
        PortfolioPromotionType promotionType = null;
        if (request.getPromotionType() != null && !request.getPromotionType().isBlank()) {
            promotionType = PortfolioPromotionType.fromString(request.getPromotionType());
            if (promotionType == null) {
                throw new BusinessException(ErrorCode.INVALID_PROMOTION_TYPE);
            }
            var dynamicPrice = settingsService.getPriceByType(promotionType);
            if (!creditService.hasEnoughCredits(userEmail, dynamicPrice)) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS);
            }
        }

        // thumbnailUuid로 파일 조회하여 URL 저장
        String thumbnailUrl = null;
        File thumbnailFile = null;
        if (request.getThumbnailUuid() != null && !request.getThumbnailUuid().isBlank()) {
            UUID thumbnailUuid = UUID.fromString(request.getThumbnailUuid());
            thumbnailFile = fileRepository.findByUuidAndIsDeletedFalse(thumbnailUuid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
            thumbnailUrl = thumbnailFile.getFileUrl();
        }

        // 포트폴리오 생성 (이미지 배열은 더이상 사용하지 않음)
        CompanyPortfolio portfolio = CompanyPortfolio.builder()
                .company(company)
                .title(request.getTitle())
                .description(request.getDescription())
                .content(request.getContent())
                .category(request.getCategory())
                .projectType(request.getProjectType())
                .projectScale(request.getProjectScale())
                .projectDuration(request.getProjectDuration())
                .projectDate(request.getProjectDate())
                .budgetRange(request.getBudgetRange())
                .actualCost(request.getActualCost())
                .thumbnailUrl(thumbnailUrl)
                .tags(request.getTags())
                .relatedLink(request.getRelatedLink())
                .copyrightOwner(request.getCopyrightOwner())
                .copyrightLicense(request.getCopyrightLicense())
                .copyrightAttribution(request.getCopyrightAttribution())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        portfolio = portfolioRepository.save(portfolio);

        // 썸네일 파일에 엔티티 정보 연결
        if (thumbnailFile != null) {
            thumbnailFile.updateEntityInfo(ENTITY_TYPE_PORTFOLIO, portfolio.getId());
            fileRepository.save(thumbnailFile);
        }

        // 이미지 첨부파일 추가 (중간 테이블 사용)
        addAttachments(portfolio, request.getImageUuids(), PortfolioAttachment.AttachmentType.IMAGE);

        // 비디오 첨부파일 추가
        if (request.getVideoUuids() != null && !request.getVideoUuids().isEmpty()) {
            addAttachments(portfolio, request.getVideoUuids(), PortfolioAttachment.AttachmentType.VIDEO);
        }

        // 필터 옵션 추가
        if (request.getFilterOptionIds() != null && !request.getFilterOptionIds().isEmpty()) {
            addFilterOptions(portfolio, request.getFilterOptionIds());
        }

        // 우대 등록 처리
        PortfolioPromotion promotion = null;
        if (promotionType != null) {
            promotion = promotionService.createPromotion(user, portfolio, promotionType, request.getAutoRenew());
            log.info("포트폴리오 우대 등록 완료: portfolioUuid={}, promotionType={}", portfolio.getUuid(), promotionType);
        }

        log.info("포트폴리오 생성 완료: uuid={}", portfolio.getUuid());

        List<PortfolioFilterOption> filterOptions = portfolioFilterOptionRepository.findByPortfolioId(portfolio.getId());
        List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
        List<PortfolioResponse.FileInfo> imageInfos = toFileInfos(imageAttachments);

        return PortfolioResponse.from(portfolio, filterOptions, imageInfos, null, false, false, getCompanySummary(company), null, promotion);
    }

    // ===== 조회 =====

    /**
     * 포트폴리오 상세 조회
     */
    @Transactional
    public PortfolioResponse getPortfolio(UUID uuid, String userEmail) {
        log.info("포트폴리오 조회: uuid={}", uuid);

        CompanyPortfolio portfolio = portfolioRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 조회수 증가
        portfolio.incrementViewCount();
        portfolioRepository.save(portfolio);

        // 필터 옵션 조회
        List<PortfolioFilterOption> filterOptions = portfolioFilterOptionRepository.findByPortfolioId(portfolio.getId());

        // 첨부파일 조회 (중간 테이블에서)
        List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
        List<PortfolioAttachment> videoAttachments = attachmentRepository.findVideosByPortfolioId(portfolio.getId());

        List<PortfolioResponse.FileInfo> images = toFileInfos(imageAttachments);
        List<PortfolioResponse.FileInfo> videos = toFileInfos(videoAttachments);

        // 북마크/좋아요 확인
        boolean isBookmarked = false;
        boolean isLiked = false;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                isBookmarked = bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
                isLiked = likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
            }
        }

        // 우대 정보
        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);

        // 업체 정보
        PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

        return PortfolioResponse.from(portfolio, filterOptions, images, videos, isBookmarked, isLiked, company, null, promotion);
    }

    /**
     * 포트폴리오 목록 조회 (공개)
     */
    @Transactional(readOnly = true)
    public Page<PortfolioResponse> getPublicPortfolios(String userEmail, Pageable pageable) {
        Page<CompanyPortfolio> portfolios = portfolioRepository.findByIsPublicTrueAndIsDeletedFalse(pageable);

        return portfolios.map(portfolio -> toSimpleResponse(portfolio, userEmail));
    }

    /**
     * 업체별 포트폴리오 목록
     */
    @Transactional(readOnly = true)
    public Page<PortfolioResponse> getPortfoliosByCompany(UUID companyUuid, String userEmail, Pageable pageable) {
        Page<CompanyPortfolio> portfolios = portfolioRepository.findPublicByCompanyUuid(companyUuid, pageable);

        return portfolios.map(portfolio -> toSimpleResponse(portfolio, userEmail));
    }

    /**
     * 내 포트폴리오 목록
     */
    @Transactional(readOnly = true)
    public Page<PortfolioResponse> getMyPortfolios(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Page<CompanyPortfolio> portfolios = portfolioRepository.findByOwnerId(user.getId(), pageable);

        return portfolios.map(portfolio -> {
            List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
            List<PortfolioResponse.FileInfo> images = toFileInfos(imageAttachments);

            PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);
            PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

            boolean isBookmarked = bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
            boolean isLiked = likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());

            return PortfolioResponse.simpleFrom(portfolio, images, isBookmarked, isLiked, company, promotion);
        });
    }

    // ===== 수정 =====

    /**
     * 포트폴리오 수정
     */
    @Transactional
    public PortfolioResponse updatePortfolio(UUID uuid, String userEmail, PortfolioUpdateRequest request) {
        log.info("포트폴리오 수정 시작: uuid={}, userEmail={}", uuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyPortfolio portfolio = portfolioRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 권한 확인
        if (!portfolio.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_ACCESS_DENIED);
        }

        // thumbnailUuid로 파일 조회하여 URL 저장
        String thumbnailUrl = null;
        File thumbnailFile = null;
        if (request.getThumbnailUuid() != null && !request.getThumbnailUuid().isBlank()) {
            UUID thumbnailUuid = UUID.fromString(request.getThumbnailUuid());
            thumbnailFile = fileRepository.findByUuidAndIsDeletedFalse(thumbnailUuid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
            thumbnailUrl = thumbnailFile.getFileUrl();
        }

        // 포트폴리오 기본 정보 수정
        portfolio.update(
                request.getTitle(),
                request.getDescription(),
                request.getContent(),
                request.getCategory(),
                request.getProjectType(),
                request.getProjectScale(),
                request.getProjectDuration(),
                request.getProjectDate(),
                request.getBudgetRange(),
                request.getActualCost(),
                thumbnailUrl,
                request.getTags(),
                request.getRelatedLink(),
                request.getCopyrightOwner(),
                request.getCopyrightLicense(),
                request.getCopyrightAttribution(),
                request.getIsPublic(),
                request.getDisplayOrder()
        );

        portfolio = portfolioRepository.save(portfolio);

        // 썸네일 파일에 엔티티 정보 연결
        if (thumbnailFile != null) {
            thumbnailFile.updateEntityInfo(ENTITY_TYPE_PORTFOLIO, portfolio.getId());
            fileRepository.save(thumbnailFile);
        }

        // 이미지 첨부파일 업데이트
        if (request.getImageUuids() != null) {
            updateAttachments(portfolio, request.getImageUuids(), PortfolioAttachment.AttachmentType.IMAGE);
        }

        // 비디오 첨부파일 업데이트
        if (request.getVideoUuids() != null) {
            updateAttachments(portfolio, request.getVideoUuids(), PortfolioAttachment.AttachmentType.VIDEO);
        }

        // 필터 옵션 업데이트
        if (request.getFilterOptionIds() != null) {
            portfolioFilterOptionRepository.deleteByPortfolioId(portfolio.getId());
            if (!request.getFilterOptionIds().isEmpty()) {
                addFilterOptions(portfolio, request.getFilterOptionIds());
            }
        }

        // 프로모션 처리
        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);

        // 우대 취소
        if (Boolean.TRUE.equals(request.getCancelPromotion()) && promotion != null) {
            promotionService.cancelPromotion(user, portfolio);
            promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);
        }
        // 신규 등록 또는 업그레이드
        else if (request.getPromotionType() != null && !request.getPromotionType().isBlank()) {
            PortfolioPromotionType requestedType = PortfolioPromotionType.fromString(request.getPromotionType());
            if (requestedType == null) {
                throw new BusinessException(ErrorCode.INVALID_PROMOTION_TYPE);
            }

            if (promotion == null) {
                // 신규 등록
                var dynamicPrice = settingsService.getPriceByType(requestedType);
                if (!creditService.hasEnoughCredits(userEmail, dynamicPrice)) {
                    throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS);
                }
                promotion = promotionService.createPromotion(user, portfolio, requestedType, request.getAutoRenew());
            } else if (PortfolioPromotionType.STANDARD.name().equals(promotion.getPromotionType())
                    && requestedType == PortfolioPromotionType.PREMIUM) {
                // 업그레이드
                promotion = promotionService.upgradePromotion(user, portfolio);
            }
        }

        // 자동갱신 설정
        if (request.getAutoRenew() != null && promotion != null && !Boolean.TRUE.equals(request.getCancelPromotion())) {
            promotionService.updateAutoRenew(user, portfolio, request.getAutoRenew());
            promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);
        }

        log.info("포트폴리오 수정 완료: uuid={}", uuid);

        List<PortfolioFilterOption> filterOptions = portfolioFilterOptionRepository.findByPortfolioId(portfolio.getId());
        List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
        List<PortfolioAttachment> videoAttachments = attachmentRepository.findVideosByPortfolioId(portfolio.getId());

        List<PortfolioResponse.FileInfo> imageInfos = toFileInfos(imageAttachments);
        List<PortfolioResponse.FileInfo> videoInfos = toFileInfos(videoAttachments);
        PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

        boolean isBookmarked = bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
        boolean isLiked = likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());

        return PortfolioResponse.from(portfolio, filterOptions, imageInfos, videoInfos, isBookmarked, isLiked, company, null, promotion);
    }

    // ===== 삭제 =====

    /**
     * 포트폴리오 삭제 (소프트 삭제)
     */
    @Transactional
    public void deletePortfolio(UUID uuid, String userEmail) {
        log.info("포트폴리오 삭제: uuid={}, userEmail={}", uuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyPortfolio portfolio = portfolioRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 권한 확인
        if (!portfolio.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_ACCESS_DENIED);
        }

        // 첨부파일도 소프트 삭제
        attachmentRepository.softDeleteByPortfolioId(portfolio.getId());

        portfolio.softDelete();
        portfolioRepository.save(portfolio);

        log.info("포트폴리오 삭제 완료: uuid={}", uuid);
    }

    // ===== Featured =====

    /**
     * 우대 포트폴리오 조회 (가중치 기반 랜덤)
     */
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getFeaturedPortfolios(List<Long> filterOptionIds, int count, String userEmail) {
        List<PortfolioPromotion> promotions = promotionService.getFeaturedPromotions(filterOptionIds, count);

        if (promotions.isEmpty()) {
            return List.of();
        }

        User user = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;

        return promotions.stream()
                .map(promotion -> {
                    CompanyPortfolio portfolio = promotion.getPortfolio();
                    List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
                    List<PortfolioResponse.FileInfo> images = toFileInfos(imageAttachments);
                    PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

                    boolean isBookmarked = false;
                    boolean isLiked = false;
                    if (user != null) {
                        isBookmarked = bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
                        isLiked = likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
                    }

                    return PortfolioResponse.simpleFrom(portfolio, images, isBookmarked, isLiked, company, promotion);
                })
                .toList();
    }

    // ===== 검색 =====

    /**
     * 포트폴리오 검색
     */
    @Transactional(readOnly = true)
    public Page<PortfolioResponse> searchPortfolios(PortfolioSearchRequest request, String userEmail, Pageable pageable) {
        log.info("포트폴리오 검색: keyword={}, filterOptionIds={}, companyUuid={}, onlyBookmarked={}, onlyMyPosts={}",
                request.getKeyword(), request.getFilterOptionIds(), request.getCompanyUuid(),
                request.getOnlyBookmarked(), request.getOnlyMyPosts());

        // 사용자 정보 조회 (북마크/내글 필터에 필요)
        Long userId = null;
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        // 북마크/내글 필터는 로그인 필수
        if ((request.isOnlyBookmarked() || request.isOnlyMyPosts()) && userId == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        // Specification 기반 검색
        var spec = PortfolioSpecifications.search(
                request.getKeyword(),
                request.getFilterOptionIds(),
                request.getCompanyUuid(),
                request.getOnlyBookmarked(),
                request.getOnlyMyPosts(),
                userId
        );

        Page<CompanyPortfolio> portfolios = portfolioRepository.findAll(spec, pageable);

        final User finalUser = user;
        return portfolios.map(portfolio -> toSimpleResponseWithUser(portfolio, finalUser));
    }

    /**
     * 간단 Response 변환 (User 객체 재사용)
     */
    private PortfolioResponse toSimpleResponseWithUser(CompanyPortfolio portfolio, User user) {
        List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
        List<PortfolioResponse.FileInfo> images = toFileInfos(imageAttachments);

        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);
        PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

        boolean isBookmarked = false;
        boolean isLiked = false;
        if (user != null) {
            isBookmarked = bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
            isLiked = likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
        }

        return PortfolioResponse.simpleFrom(portfolio, images, isBookmarked, isLiked, company, promotion);
    }

    // ===== Helper Methods =====

    /**
     * 첨부파일 추가 (중간 테이블)
     */
    private void addAttachments(CompanyPortfolio portfolio, List<String> fileUuids, PortfolioAttachment.AttachmentType type) {
        int order = 0;
        for (String uuidStr : fileUuids) {
            UUID fileUuid = UUID.fromString(uuidStr);
            File file = fileRepository.findByUuidAndIsDeletedFalse(fileUuid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            // 파일에 엔티티 정보 연결 (쓰레기 파일 정리용)
            file.updateEntityInfo(ENTITY_TYPE_PORTFOLIO, portfolio.getId());
            fileRepository.save(file);

            PortfolioAttachment attachment = PortfolioAttachment.builder()
                    .portfolio(portfolio)
                    .file(file)
                    .attachmentType(type)
                    .displayOrder(order++)
                    .build();

            attachmentRepository.save(attachment);
        }
    }

    /**
     * 첨부파일 업데이트 (기존 삭제 후 새로 추가)
     */
    private void updateAttachments(CompanyPortfolio portfolio, List<String> fileUuids, PortfolioAttachment.AttachmentType type) {
        // 기존 해당 타입 첨부파일 조회
        List<PortfolioAttachment> existing = attachmentRepository.findByPortfolioIdAndType(portfolio.getId(), type);

        // 기존 첨부파일 소프트 삭제
        for (PortfolioAttachment attachment : existing) {
            attachment.softDelete();
            attachmentRepository.save(attachment);
        }

        // 새 첨부파일 추가
        if (!fileUuids.isEmpty()) {
            addAttachments(portfolio, fileUuids, type);
        }
    }

    /**
     * 첨부파일 -> FileInfo 변환
     */
    private List<PortfolioResponse.FileInfo> toFileInfos(List<PortfolioAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(att -> {
                    File file = att.getFile();
                    String extension = extractFileExtension(file.getOriginalFilename());
                    return PortfolioResponse.FileInfo.builder()
                            .uuid(file.getUuid())
                            .originalFilename(file.getOriginalFilename())
                            .fileUrl(file.getFileUrl())
                            .fileSize(file.getFileSize())
                            .mimeType(file.getMimeType())
                            .fileExtension(extension)
                            .build();
                })
                .toList();
    }

    /**
     * 파일명에서 확장자 추출
     */
    private String extractFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 간단 Response 변환 (목록용)
     */
    private PortfolioResponse toSimpleResponse(CompanyPortfolio portfolio, String userEmail) {
        List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
        List<PortfolioResponse.FileInfo> images = toFileInfos(imageAttachments);

        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);
        PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

        boolean isBookmarked = false;
        boolean isLiked = false;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                isBookmarked = bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
                isLiked = likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
            }
        }

        return PortfolioResponse.simpleFrom(portfolio, images, isBookmarked, isLiked, company, promotion);
    }

    private void addFilterOptions(CompanyPortfolio portfolio, List<Long> filterOptionIds) {
        for (Long filterOptionId : filterOptionIds) {
            FilterOption filterOption = filterOptionRepository.findById(filterOptionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

            PortfolioFilterOption pfo = PortfolioFilterOption.of(portfolio, filterOption);
            portfolioFilterOptionRepository.save(pfo);
        }
    }

    private PortfolioResponse.CompanySummary getCompanySummary(Company company) {
        if (company == null) {
            return null;
        }

        return PortfolioResponse.CompanySummary.builder()
                .companyUuid(company.getUuid())
                .companyName(company.getName())
                .phone(company.getPrimaryPhone())
                .build();
    }

    // ===== 관리자 전용 =====

    /**
     * 관리자용 포트폴리오 검색 (키워드 검색만)
     */
    @Transactional(readOnly = true)
    public Page<PortfolioResponse> searchPortfoliosForAdmin(String keyword, Pageable pageable) {
        log.info("[관리자] 포트폴리오 검색: keyword={}", keyword);

        Page<CompanyPortfolio> portfolios = portfolioRepository.searchForAdmin(keyword, pageable);

        return portfolios.map(this::toAdminResponse);
    }

    /**
     * 관리자용 포트폴리오 상세 조회 (조회수 증가 없음)
     */
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioForAdmin(UUID uuid) {
        log.info("[관리자] 포트폴리오 상세 조회: uuid={}", uuid);

        CompanyPortfolio portfolio = portfolioRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        List<PortfolioFilterOption> filterOptions = portfolioFilterOptionRepository.findByPortfolioId(portfolio.getId());
        List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
        List<PortfolioAttachment> videoAttachments = attachmentRepository.findVideosByPortfolioId(portfolio.getId());

        List<PortfolioResponse.FileInfo> images = toFileInfos(imageAttachments);
        List<PortfolioResponse.FileInfo> videos = toFileInfos(videoAttachments);

        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);
        PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

        return PortfolioResponse.from(portfolio, filterOptions, images, videos, false, false, company, null, promotion);
    }

    /**
     * 포트폴리오 추천 설정/해제 (관리자)
     */
    @Transactional
    public boolean setFeatured(UUID uuid, boolean featured) {
        log.info("[관리자] 포트폴리오 추천 설정: uuid={}, featured={}", uuid, featured);

        CompanyPortfolio portfolio = portfolioRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        portfolio.setFeatured(featured);
        portfolioRepository.save(portfolio);

        log.info("[관리자] 포트폴리오 추천 설정 완료: uuid={}, featured={}", uuid, featured);

        return featured;
    }

    /**
     * 관리자용 포트폴리오 삭제
     */
    @Transactional
    public void deletePortfolioByAdmin(UUID uuid) {
        log.info("[관리자] 포트폴리오 삭제: uuid={}", uuid);

        CompanyPortfolio portfolio = portfolioRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 첨부파일도 소프트 삭제
        attachmentRepository.softDeleteByPortfolioId(portfolio.getId());

        portfolio.softDelete();
        portfolioRepository.save(portfolio);

        log.info("[관리자] 포트폴리오 삭제 완료: uuid={}", uuid);
    }

    /**
     * 관리자용 Response 변환
     */
    private PortfolioResponse toAdminResponse(CompanyPortfolio portfolio) {
        List<PortfolioAttachment> imageAttachments = attachmentRepository.findImagesByPortfolioId(portfolio.getId());
        List<PortfolioResponse.FileInfo> images = toFileInfos(imageAttachments);

        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId()).orElse(null);
        PortfolioResponse.CompanySummary company = getCompanySummary(portfolio.getCompany());

        return PortfolioResponse.simpleFrom(portfolio, images, false, false, company, promotion);
    }
}
