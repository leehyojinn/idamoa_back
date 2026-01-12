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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCategoryService {

    private final CommunityCategoryRepository categoryRepository;

    /**
     * 활성 카테고리 목록 조회 (공개) - 플랫 리스트
     */
    @Transactional(readOnly = true)
    public List<CommunityCategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveAndIsDeletedFalseOrderByDisplayOrderAsc(true)
                .stream()
                .map(CommunityCategoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성 카테고리 트리 구조 조회 (공개)
     * - 모든 활성 카테고리를 한 번에 가져와서 메모리에서 트리 구성
     */
    @Transactional(readOnly = true)
    public List<CommunityCategoryResponse> getActiveCategoriesTree() {
        // 모든 활성 카테고리를 한 번에 조회
        List<CommunityCategory> allCategories = categoryRepository
                .findByIsActiveAndIsDeletedFalseOrderByDisplayOrderAsc(true);

        return buildTreeResponse(allCategories, false);
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
     * 전체 카테고리 트리 구조 조회 (관리자) - 비활성 포함
     * - 모든 카테고리를 한 번에 가져와서 메모리에서 트리 구성
     */
    @Transactional(readOnly = true)
    public List<CommunityCategoryResponse> getAllCategoriesTree() {
        // 삭제되지 않은 모든 카테고리 조회
        List<CommunityCategory> allCategories = categoryRepository.findByIsDeletedFalseOrderByDisplayOrderAsc();

        return buildTreeResponse(allCategories, true);
    }

    /**
     * 카테고리 목록을 트리 구조 Response로 변환
     * @param categories 전체 카테고리 목록
     * @param forAdmin 관리자용 여부
     * @return 트리 구조로 정렬된 Response 목록
     */
    private List<CommunityCategoryResponse> buildTreeResponse(List<CommunityCategory> categories, boolean forAdmin) {
        // ID별 카테고리 맵 생성
        Map<Long, CommunityCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(CommunityCategory::getId, c -> c));

        // 부모별 자식 목록 맵 생성
        Map<Long, List<CommunityCategory>> childrenMap = categories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // 루트 카테고리 (parent가 null인 것) 찾기
        List<CommunityCategory> rootCategories = categories.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        // 루트부터 재귀적으로 Response 생성
        return rootCategories.stream()
                .map(root -> buildCategoryResponse(root, childrenMap, forAdmin))
                .collect(Collectors.toList());
    }

    /**
     * 단일 카테고리를 Response로 변환 (재귀)
     */
    private CommunityCategoryResponse buildCategoryResponse(
            CommunityCategory category,
            Map<Long, List<CommunityCategory>> childrenMap,
            boolean forAdmin) {

        // 자식 카테고리 조회
        List<CommunityCategory> children = childrenMap.getOrDefault(category.getId(), new ArrayList<>());

        // 자식 Response 생성 (재귀)
        List<CommunityCategoryResponse> childResponses = children.stream()
                .map(child -> buildCategoryResponse(child, childrenMap, forAdmin))
                .collect(Collectors.toList());

        // Response 빌드
        CommunityCategoryResponse.CommunityCategoryResponseBuilder builder = CommunityCategoryResponse.builder()
                .uuid(category.getUuid())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .icon(category.getIcon())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .allowAnonymous(category.getAllowAnonymous())
                .requireLogin(category.getRequireLogin())
                .allowAttachments(category.getAllowAttachments())
                .maxAttachments(category.getMaxAttachments())
                .createdAt(category.getCreatedAt())
                .depth(category.getDepth());

        // 부모 정보
        if (category.getParent() != null) {
            builder.parent(CommunityCategoryResponse.ParentInfo.builder()
                    .uuid(category.getParent().getUuid())
                    .name(category.getParent().getName())
                    .slug(category.getParent().getSlug())
                    .build());
        }

        // 자식 카테고리
        if (!childResponses.isEmpty()) {
            builder.children(childResponses);
        }

        return builder.build();
    }

    /**
     * 카테고리 상세 조회
     */
    @Transactional(readOnly = true)
    public CommunityCategoryResponse getCategory(UUID uuid) {
        CommunityCategory category = categoryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));
        return CommunityCategoryResponse.from(category, true);
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
        log.info("[관리자] 커뮤니티 카테고리 생성: slug={}, parentUuid={}",
                request.getSlug(), request.getParentUuid());

        // 슬러그 중복 체크
        if (categoryRepository.existsBySlugAndIsDeletedFalse(request.getSlug())) {
            throw new BusinessException(ErrorCode.COMMUNITY_CATEGORY_SLUG_DUPLICATE);
        }

        // 부모 카테고리 조회
        CommunityCategory parent = null;
        if (request.getParentUuid() != null) {
            parent = categoryRepository.findByUuidAndIsDeletedFalse(request.getParentUuid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));
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
                .parent(parent)
                .build();

        category = categoryRepository.save(category);

        log.info("[관리자] 커뮤니티 카테고리 생성 완료: uuid={}, depth={}",
                category.getUuid(), category.getDepth());
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

        // 기본 정보 업데이트
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

        // 부모 변경 처리
        if (Boolean.TRUE.equals(request.getChangeParent())) {
            CommunityCategory newParent = null;
            if (request.getParentUuid() != null) {
                newParent = categoryRepository.findByUuidAndIsDeletedFalse(request.getParentUuid())
                        .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));

                // 순환 참조 방지: 자기 자신이나 자신의 하위를 부모로 설정 불가
                if (newParent.getId().equals(category.getId())) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                }
            }
            category.updateParent(newParent);
        }

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

        // 자식 카테고리가 있으면 삭제 불가
        if (categoryRepository.hasChildren(category.getId())) {
            throw new BusinessException(ErrorCode.COMMUNITY_CATEGORY_HAS_CHILDREN);
        }

        category.softDelete();
        categoryRepository.save(category);

        log.info("[관리자] 커뮤니티 카테고리 삭제 완료: uuid={}", uuid);
    }
}
