package com.hip.damoa.domain.community.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.community.model.CommunityCategory;
import com.hip.damoa.domain.community.repository.CommunityCategoryRepository;
import com.hip.damoa.domain.community.web.dto.AdminCategoryCreateRequest;
import com.hip.damoa.domain.community.web.dto.AdminCategoryUpdateRequest;
import com.hip.damoa.domain.community.web.dto.CommunityCategoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCategoryService {

    private final CommunityCategoryRepository categoryRepository;

    /**
     * 활성 카테고리 목록 조회 (공개)
     */
    @Transactional(readOnly = true)
    public List<CommunityCategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveAndIsDeletedFalseOrderByDisplayOrderAsc(true)
                .stream()
                .map(CommunityCategoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전체 카테고리 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public Page<CommunityCategoryResponse> getAllCategories(String keyword, Pageable pageable) {
        return categoryRepository.searchForAdmin(keyword, pageable)
                .map(CommunityCategoryResponse::from);
    }

    /**
     * 카테고리 상세 조회
     */
    @Transactional(readOnly = true)
    public CommunityCategoryResponse getCategory(UUID uuid) {
        CommunityCategory category = categoryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));
        return CommunityCategoryResponse.from(category);
    }

    /**
     * 슬러그로 카테고리 조회
     */
    @Transactional(readOnly = true)
    public CommunityCategory getCategoryBySlug(String slug) {
        return categoryRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));
    }

    /**
     * 카테고리 생성 (관리자)
     */
    @Transactional
    public CommunityCategoryResponse createCategory(AdminCategoryCreateRequest request) {
        log.info("[관리자] 커뮤니티 카테고리 생성: slug={}", request.getSlug());

        // 슬러그 중복 체크
        if (categoryRepository.existsBySlugAndIsDeletedFalse(request.getSlug())) {
            throw new BusinessException(ErrorCode.COMMUNITY_CATEGORY_SLUG_DUPLICATE);
        }

        CommunityCategory category = CommunityCategory.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .icon(request.getIcon())
                .displayOrder(request.getDisplayOrder())
                .isActive(request.getIsActive())
                .allowAnonymous(request.getAllowAnonymous())
                .requireLogin(request.getRequireLogin())
                .allowAttachments(request.getAllowAttachments())
                .maxAttachments(request.getMaxAttachments())
                .build();

        category = categoryRepository.save(category);

        log.info("[관리자] 커뮤니티 카테고리 생성 완료: uuid={}", category.getUuid());
        return CommunityCategoryResponse.from(category);
    }

    /**
     * 카테고리 수정 (관리자)
     */
    @Transactional
    public CommunityCategoryResponse updateCategory(UUID uuid, AdminCategoryUpdateRequest request) {
        log.info("[관리자] 커뮤니티 카테고리 수정: uuid={}", uuid);

        CommunityCategory category = categoryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));

        category.update(
                request.getName(),
                request.getDescription(),
                request.getIcon(),
                request.getDisplayOrder(),
                request.getIsActive(),
                request.getAllowAnonymous(),
                request.getRequireLogin(),
                request.getAllowAttachments(),
                request.getMaxAttachments()
        );

        category = categoryRepository.save(category);

        log.info("[관리자] 커뮤니티 카테고리 수정 완료: uuid={}", uuid);
        return CommunityCategoryResponse.from(category);
    }

    /**
     * 카테고리 삭제 (관리자)
     */
    @Transactional
    public void deleteCategory(UUID uuid) {
        log.info("[관리자] 커뮤니티 카테고리 삭제: uuid={}", uuid);

        CommunityCategory category = categoryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));

        category.softDelete();
        categoryRepository.save(category);

        log.info("[관리자] 커뮤니티 카테고리 삭제 완료: uuid={}", uuid);
    }
}
