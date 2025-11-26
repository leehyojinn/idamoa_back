package com.hip.damoa.domain.admin.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.admin.web.dto.*;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.filter.model.FilterCategory;
import com.hip.damoa.domain.filter.model.FilterOption;
import com.hip.damoa.domain.filter.repository.FilterCategoryRepository;
import com.hip.damoa.domain.filter.repository.FilterOptionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 필터 관리자 서비스
 * 필터 카테고리 및 옵션의 CRUD 및 통계 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFilterService {

    private final FilterCategoryRepository filterCategoryRepository;
    private final FilterOptionRepository filterOptionRepository;
    private final CompanyRepository companyRepository;
    private final BoardRepository boardRepository;

    // ==================== 필터 카테고리 관리 ====================

    /**
     * 필터 카테고리 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<FilterCategoryResponse> getFilterCategories(String entityType, String keyword, Pageable pageable) {
        log.info("필터 카테고리 목록 조회 시작: entityType={}, keyword={}", entityType, keyword);

        Specification<FilterCategory> spec = Specification.where(isNotDeleted());

        if (StringUtils.hasText(entityType)) {
            spec = spec.and(hasEntityType(entityType));
        }

        if (StringUtils.hasText(keyword)) {
            spec = spec.and(hasKeyword(keyword));
        }

        Page<FilterCategory> categories = filterCategoryRepository.findAll(spec, pageable);

        // 각 카테고리의 옵션 개수 조회
        Page<FilterCategoryResponse> response = categories.map(category -> {
            int optionCount = filterOptionRepository
                    .findByCategoryAndIsActiveTrueAndIsDeletedFalse(category).size();
            return FilterCategoryResponse.from(category, optionCount);
        });

        log.info("필터 카테고리 목록 조회 완료: total={}", response.getTotalElements());
        return response;
    }

    /**
     * 필터 카테고리 상세 조회
     */
    @Transactional(readOnly = true)
    public FilterCategoryDetailResponse getFilterCategory(Long categoryId) {
        log.info("필터 카테고리 상세 조회: categoryId={}", categoryId);

        FilterCategory category = filterCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND));

        List<FilterOption> options = filterOptionRepository
                .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

        List<FilterOptionResponse> optionResponses = options.stream()
                .map(option -> FilterOptionResponse.from(option, option.getChildren().size()))
                .collect(Collectors.toList());

        return FilterCategoryDetailResponse.from(category, optionResponses);
    }

    /**
     * 필터 카테고리 생성
     */
    @Transactional
    public FilterCategoryResponse createFilterCategory(FilterCategoryCreateRequest request) {
        log.info("필터 카테고리 생성: code={}, name={}", request.getCode(), request.getName());

        // 중복 체크
        if (filterCategoryRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException(ErrorCode.FILTER_CATEGORY_CODE_DUPLICATE);
        }

        FilterCategory category = request.toEntity();
        if (request.getMetadata() != null) {
            category.setMetadata(request.getMetadata());
        }

        category = filterCategoryRepository.save(category);
        log.info("필터 카테고리 생성 완료: id={}", category.getId());

        return FilterCategoryResponse.from(category, 0);
    }

    /**
     * 필터 카테고리 수정
     */
    @Transactional
    public FilterCategoryResponse updateFilterCategory(Long categoryId, FilterCategoryUpdateRequest request) {
        log.info("필터 카테고리 수정: categoryId={}", categoryId);

        FilterCategory category = filterCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND));

        // 필드 업데이트
        updateCategoryFields(category, request);

        category = filterCategoryRepository.save(category);

        int optionCount = filterOptionRepository
                .findByCategoryAndIsActiveTrueAndIsDeletedFalse(category).size();

        log.info("필터 카테고리 수정 완료");
        return FilterCategoryResponse.from(category, optionCount);
    }

    /**
     * 필터 카테고리 삭제
     */
    @Transactional
    public void deleteFilterCategory(Long categoryId) {
        log.info("필터 카테고리 삭제: categoryId={}", categoryId);

        FilterCategory category = filterCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND));

        // 사용 중인지 확인
        checkCategoryUsage(category);

        // Soft Delete
        category.softDelete();
        filterCategoryRepository.save(category);

        log.info("필터 카테고리 삭제 완료");
    }

    /**
     * 필터 카테고리 활성화/비활성화
     */
    @Transactional
    public FilterCategoryResponse toggleFilterCategoryActive(Long categoryId, boolean isActive) {
        log.info("필터 카테고리 활성 상태 변경: categoryId={}, isActive={}", categoryId, isActive);

        FilterCategory category = filterCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND));

        if (isActive) {
            category.activate();
        } else {
            category.deactivate();
        }

        category = filterCategoryRepository.save(category);

        int optionCount = filterOptionRepository
                .findByCategoryAndIsActiveTrueAndIsDeletedFalse(category).size();

        return FilterCategoryResponse.from(category, optionCount);
    }

    // ==================== 필터 옵션 관리 ====================

    /**
     * 필터 옵션 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<FilterOptionResponse> getFilterOptions(Long categoryId, String keyword, Boolean isActive, Pageable pageable) {
        log.info("필터 옵션 목록 조회: categoryId={}, keyword={}, isActive={}", categoryId, keyword, isActive);

        Specification<FilterOption> spec = Specification.where(optionIsNotDeleted());

        if (categoryId != null) {
            spec = spec.and(belongsToCategory(categoryId));
        }

        if (StringUtils.hasText(keyword)) {
            spec = spec.and(optionHasKeyword(keyword));
        }

        if (isActive != null) {
            spec = spec.and(optionIsActive(isActive));
        }

        Page<FilterOption> options = filterOptionRepository.findAll(spec, pageable);

        Page<FilterOptionResponse> response = options.map(option ->
                FilterOptionResponse.from(option, option.getChildren().size())
        );

        log.info("필터 옵션 목록 조회 완료: total={}", response.getTotalElements());
        return response;
    }

    /**
     * 필터 옵션 상세 조회
     */
    @Transactional(readOnly = true)
    public FilterOptionDetailResponse getFilterOption(Long optionId) {
        log.info("필터 옵션 상세 조회: optionId={}", optionId);

        FilterOption option = filterOptionRepository.findById(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        return FilterOptionDetailResponse.from(option);
    }

    /**
     * 필터 옵션 생성
     */
    @Transactional
    public FilterOptionResponse createFilterOption(FilterOptionCreateRequest request) {
        log.info("필터 옵션 생성: categoryId={}, code={}, name={}",
                request.getCategoryId(), request.getCode(), request.getName());

        FilterCategory category = filterCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND));

        // 중복 체크
        if (filterOptionRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException(ErrorCode.FILTER_OPTION_CODE_DUPLICATE);
        }

        FilterOption parent = null;
        if (request.getParentId() != null) {
            parent = filterOptionRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_PARENT_OPTION_NOT_FOUND));
        }

        FilterOption option = request.toEntity(category, parent);
        if (request.getMetadata() != null) {
            option.setMetadata(request.getMetadata());
        }

        option = filterOptionRepository.save(option);
        log.info("필터 옵션 생성 완료: id={}", option.getId());

        return FilterOptionResponse.from(option, 0);
    }

    /**
     * 필터 옵션 수정
     */
    @Transactional
    public FilterOptionResponse updateFilterOption(Long optionId, FilterOptionUpdateRequest request) {
        log.info("필터 옵션 수정: optionId={}", optionId);

        FilterOption option = filterOptionRepository.findById(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        // 필드 업데이트
        updateOptionFields(option, request);

        option = filterOptionRepository.save(option);

        log.info("필터 옵션 수정 완료");
        return FilterOptionResponse.from(option, option.getChildren().size());
    }

    /**
     * 필터 옵션 삭제
     */
    @Transactional
    public void deleteFilterOption(Long optionId) {
        log.info("필터 옵션 삭제: optionId={}", optionId);

        FilterOption option = filterOptionRepository.findById(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        // 사용 중인지 확인
        checkOptionUsage(option);

        // Soft Delete
        option.softDelete();
        filterOptionRepository.save(option);

        log.info("필터 옵션 삭제 완료");
    }

    /**
     * 필터 옵션 활성화/비활성화
     */
    @Transactional
    public FilterOptionResponse toggleFilterOptionActive(Long optionId, boolean isActive) {
        log.info("필터 옵션 활성 상태 변경: optionId={}, isActive={}", optionId, isActive);

        FilterOption option = filterOptionRepository.findById(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        if (isActive) {
            option.activate();
        } else {
            option.deactivate();
        }

        option = filterOptionRepository.save(option);

        return FilterOptionResponse.from(option, option.getChildren().size());
    }


    /**
     * 필터 옵션 순서 변경
     */
    @Transactional
    public List<FilterOptionResponse> reorderFilterOptions(FilterOptionReorderRequest request) {
        log.info("필터 옵션 순서 변경: categoryId={}", request.getCategoryId());

        FilterCategory category = filterCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND));

        List<FilterOption> updatedOptions = new ArrayList<>();

        for (FilterOptionReorderRequest.OptionOrder order : request.getOptionOrders()) {
            FilterOption option = filterOptionRepository.findById(order.getOptionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

            // 카테고리 일치 확인
            if (!option.getCategory().getId().equals(category.getId())) {
                throw new BusinessException(ErrorCode.FILTER_ENTITY_TYPE_MISMATCH);
            }

            // Use existing setter that we already added
            option.setDisplayOrder(order.getDisplayOrder());
            updatedOptions.add(option);
        }

        updatedOptions = filterOptionRepository.saveAll(updatedOptions);

        log.info("필터 옵션 순서 변경 완료");
        return updatedOptions.stream()
                .map(option -> FilterOptionResponse.from(option, option.getChildren().size()))
                .collect(Collectors.toList());
    }


    // ==================== 헬퍼 메서드 ====================

    private void updateCategoryFields(FilterCategory category, FilterCategoryUpdateRequest request) {
        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getFilterType())) {
            category.setFilterType(request.getFilterType());
        }
        if (request.getSupportsHierarchy() != null) {
            category.setSupportsHierarchy(request.getSupportsHierarchy());
        }
        if (request.getMaxDepth() != null) {
            category.setMaxDepth(request.getMaxDepth());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getIsActive() != null) {
            if (request.getIsActive()) {
                category.activate();
            } else {
                category.deactivate();
            }
        }
        if (request.getIsRequired() != null) {
            category.setIsRequired(request.getIsRequired());
        }
        if (request.getMetadata() != null) {
            category.setMetadata(request.getMetadata());
        }
    }

    private void updateOptionFields(FilterOption option, FilterOptionUpdateRequest request) {
        if (StringUtils.hasText(request.getName())) {
            option.setName(request.getName());
        }
        if (request.getShortName() != null) {
            option.setShortName(request.getShortName());
        }
        if (request.getDescription() != null) {
            option.setDescription(request.getDescription());
        }
        if (request.getPath() != null) {
            option.setPath(request.getPath());
        }
        if (request.getDisplayOrder() != null) {
            option.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIcon() != null) {
            option.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            option.setColor(request.getColor());
        }
        if (request.getIsActive() != null) {
            if (request.getIsActive()) {
                option.activate();
            } else {
                option.deactivate();
            }
        }
        if (request.getIsDefault() != null) {
            if (request.getIsDefault()) {
                option.setAsDefault();
            } else {
                option.unsetDefault();
            }
        }
        if (request.getMetadata() != null) {
            option.setMetadata(request.getMetadata());
        }
    }

    private void checkCategoryUsage(FilterCategory category) {
        // 옵션이 있는지 확인
        List<FilterOption> options = filterOptionRepository
                .findByCategoryAndIsActiveTrueAndIsDeletedFalse(category);
        if (!options.isEmpty()) {
            throw new BusinessException(ErrorCode.FILTER_CATEGORY_HAS_OPTIONS);
        }
    }

    private void checkOptionUsage(FilterOption option) {
        // 자식 옵션이 있는지 확인
        if (!option.getChildren().isEmpty()) {
            throw new BusinessException(ErrorCode.FILTER_OPTION_HAS_CHILDREN);
        }

        // 사용 중인지 확인 (usageCount > 0)
        if (option.getUsageCount() > 0) {
            log.warn("사용 중인 필터 옵션 삭제 시도: optionId={}, usageCount={}",
                    option.getId(), option.getUsageCount());
        }
    }

    // ==================== JPA Specifications ====================

    private Specification<FilterCategory> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    private Specification<FilterCategory> hasEntityType(String entityType) {
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    private Specification<FilterCategory> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    private Specification<FilterOption> optionIsNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    private Specification<FilterOption> belongsToCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private Specification<FilterOption> optionHasKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    private Specification<FilterOption> optionIsActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }
}