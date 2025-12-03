package com.hip.damoa.domain.admin.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.admin.web.dto.*;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.company.model.CompanyFilterOption;
import com.hip.damoa.domain.company.repository.CompanyFilterOptionRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final CompanyFilterOptionRepository companyFilterOptionRepository;

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

        // BOARD 타입 메타데이터 검증
        validateBoardTypeMetadata(request.getEntityType(), request.getMetadata());

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
            // BOARD 타입일 경우 메타데이터 검증
            validateBoardTypeMetadata(category.getEntityType(), request.getMetadata());
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

    /**
     * BOARD 타입 필터 카테고리의 메타데이터 검증
     * - BOARD 타입인 경우 board_types 필드가 필수
     * - board_types에는 유효한 값만 허용 (GALLERY, DOCUMENT)
     */
    private void validateBoardTypeMetadata(String entityType, Map<String, Object> metadata) {
        if (!"BOARD".equals(entityType)) {
            return; // BOARD 타입이 아니면 검증 스킵
        }

        // BOARD 타입인데 metadata가 없거나 board_types가 없는 경우
        if (metadata == null || !metadata.containsKey("board_types")) {
            throw new BusinessException(ErrorCode.FILTER_BOARD_TYPES_REQUIRED);
        }

        Object boardTypes = metadata.get("board_types");

        // board_types가 List인지 확인
        if (!(boardTypes instanceof List)) {
            log.error("board_types가 배열이 아닙니다: {}", boardTypes);
            throw new BusinessException(ErrorCode.FILTER_INVALID_METADATA);
        }

        @SuppressWarnings("unchecked")
        List<String> boardTypeList = (List<String>) boardTypes;

        // 빈 배열 체크
        if (boardTypeList.isEmpty()) {
            log.error("board_types가 비어있습니다");
            throw new BusinessException(ErrorCode.FILTER_BOARD_TYPES_REQUIRED);
        }

        // 유효한 board_types 값 목록
        List<String> validBoardTypes = List.of("GALLERY", "DOCUMENT");

        // 각 값이 유효한지 확인
        for (Object type : boardTypeList) {
            if (!(type instanceof String)) {
                log.error("board_types의 값이 문자열이 아닙니다: {}", type);
                throw new BusinessException(ErrorCode.FILTER_INVALID_METADATA);
            }
            String typeStr = ((String) type).toUpperCase();
            if (!validBoardTypes.contains(typeStr)) {
                log.error("유효하지 않은 board_type 값입니다: {} (허용: {})", type, validBoardTypes);
                throw new BusinessException(ErrorCode.FILTER_INVALID_METADATA);
            }
        }

        log.debug("BOARD 타입 메타데이터 검증 완료: board_types={}", boardTypeList);
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

    // ==================== 필터 옵션 마이그레이션 ====================

    /**
     * 필터 옵션 마이그레이션 미리보기
     * 실제 마이그레이션 전에 영향받는 업체/게시글 수를 미리 확인
     */
    @Transactional(readOnly = true)
    public FilterMigratePreviewResponse previewFilterMigration(FilterMigrateRequest request) {
        log.info("필터 옵션 마이그레이션 미리보기: sourceOptionId={}, targetOptionId={}",
                request.getSourceOptionId(), request.getTargetOptionId());

        // 동일한 옵션 체크
        if (request.getSourceOptionId().equals(request.getTargetOptionId())) {
            throw new BusinessException(ErrorCode.FILTER_SAME_OPTION_MIGRATION);
        }

        FilterOption sourceOption = filterOptionRepository.findById(request.getSourceOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        FilterOption targetOption = filterOptionRepository.findById(request.getTargetOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        // 영향받는 업체 수
        int affectedCompanyCount = companyFilterOptionRepository
                .countDistinctCompanyByFilterOption(sourceOption);

        // 중복 업체 수 (이미 타겟 옵션을 가진 경우)
        List<Long> duplicateCompanyIds = companyFilterOptionRepository
                .findCompanyIdsWithBothOptions(sourceOption, targetOption);

        // 샘플 업체 조회
        List<CompanyFilterOption> samples = companyFilterOptionRepository
                .findByFilterOptionWithCompany(sourceOption, PageRequest.of(0, 10));

        List<FilterMigratePreviewResponse.AffectedCompanyInfo> companySamples = samples.stream()
                .map(cfo -> FilterMigratePreviewResponse.AffectedCompanyInfo.builder()
                        .id(cfo.getCompany().getId())
                        .name(cfo.getCompany().getName())
                        .hasDuplicate(duplicateCompanyIds.contains(cfo.getCompany().getId()))
                        .build())
                .collect(Collectors.toList());

        log.info("마이그레이션 미리보기 완료: affectedCompanies={}, duplicates={}",
                affectedCompanyCount, duplicateCompanyIds.size());

        return FilterMigratePreviewResponse.builder()
                .sourceOption(FilterMigratePreviewResponse.OptionInfo.builder()
                        .id(sourceOption.getId())
                        .code(sourceOption.getCode())
                        .name(sourceOption.getName())
                        .categoryCode(sourceOption.getCategory().getCode())
                        .categoryName(sourceOption.getCategory().getName())
                        .usageCount(sourceOption.getUsageCount())
                        .build())
                .targetOption(FilterMigratePreviewResponse.OptionInfo.builder()
                        .id(targetOption.getId())
                        .code(targetOption.getCode())
                        .name(targetOption.getName())
                        .categoryCode(targetOption.getCategory().getCode())
                        .categoryName(targetOption.getCategory().getName())
                        .usageCount(targetOption.getUsageCount())
                        .build())
                .affectedCompanyCount(affectedCompanyCount)
                .affectedBoardCount(0) // TODO: board_filter_options 구현 필요
                .duplicateCompanyCount(duplicateCompanyIds.size())
                .duplicateBoardCount(0) // TODO: board_filter_options 구현 필요
                .affectedCompanySamples(companySamples)
                .build();
    }

    /**
     * 필터 옵션 마이그레이션 실행
     */
    @Transactional
    public FilterMigrateResponse migrateFilterOption(FilterMigrateRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("필터 옵션 마이그레이션 시작: sourceOptionId={}, targetOptionId={}",
                request.getSourceOptionId(), request.getTargetOptionId());

        // 동일한 옵션 체크
        if (request.getSourceOptionId().equals(request.getTargetOptionId())) {
            throw new BusinessException(ErrorCode.FILTER_SAME_OPTION_MIGRATION);
        }

        FilterOption sourceOption = filterOptionRepository.findById(request.getSourceOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        FilterOption targetOption = filterOptionRepository.findById(request.getTargetOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

        // 중복 업체 ID 조회
        List<Long> duplicateCompanyIds = companyFilterOptionRepository
                .findCompanyIdsWithBothOptions(sourceOption, targetOption);

        // 중복이 아닌 업체들의 source 옵션을 target으로 변경
        int migratedCompanyCount = companyFilterOptionRepository
                .bulkUpdateFilterOption(sourceOption, targetOption,
                        duplicateCompanyIds.isEmpty() ? Collections.singletonList(-1L) : duplicateCompanyIds);

        // 중복인 경우 source 옵션 레코드 삭제
        int deletedCount = 0;
        if (!duplicateCompanyIds.isEmpty()) {
            deletedCount = companyFilterOptionRepository
                    .bulkDeleteByFilterOptionAndCompanyIds(sourceOption, duplicateCompanyIds);
        }

        // usage_count 업데이트
        int totalMigrated = migratedCompanyCount + deletedCount;
        sourceOption.setUsageCount(sourceOption.getUsageCount() - totalMigrated);
        targetOption.setUsageCount(targetOption.getUsageCount() + migratedCompanyCount);
        filterOptionRepository.save(sourceOption);
        filterOptionRepository.save(targetOption);

        // 선택적으로 source 옵션 비활성화
        if (Boolean.TRUE.equals(request.getDeactivateSource())) {
            sourceOption.deactivate();
            filterOptionRepository.save(sourceOption);
            log.info("소스 옵션 비활성화: optionId={}", sourceOption.getId());
        }

        // 선택적으로 source 옵션 soft delete
        boolean sourceDeleted = false;
        if (Boolean.TRUE.equals(request.getDeleteSource())) {
            sourceOption.softDelete();
            filterOptionRepository.save(sourceOption);
            sourceDeleted = true;
            log.info("소스 옵션 삭제: optionId={}", sourceOption.getId());
        }

        long processingTimeMs = System.currentTimeMillis() - startTime;

        log.info("필터 옵션 마이그레이션 완료: migratedCount={}, duplicateCount={}, deletedCount={}, processingTime={}ms",
                migratedCompanyCount, duplicateCompanyIds.size(), deletedCount, processingTimeMs);

        return FilterMigrateResponse.builder()
                .sourceOptionCode(sourceOption.getCode())
                .sourceOptionName(sourceOption.getName())
                .targetOptionCode(targetOption.getCode())
                .targetOptionName(targetOption.getName())
                .migratedCompanyCount(migratedCompanyCount)
                .migratedBoardCount(0) // TODO: board_filter_options 구현 필요
                .skippedCompanyCount(duplicateCompanyIds.size())
                .skippedBoardCount(0) // TODO: board_filter_options 구현 필요
                .sourceDeactivated(Boolean.TRUE.equals(request.getDeactivateSource()))
                .sourceDeleted(sourceDeleted)
                .completedAt(LocalDateTime.now())
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * 모든 필터 옵션 목록 조회 (마이그레이션용 드롭다운)
     */
    @Transactional(readOnly = true)
    public List<FilterOptionResponse> getAllFilterOptionsForMigration() {
        log.info("마이그레이션용 전체 필터 옵션 목록 조회");

        List<FilterOption> options = filterOptionRepository
                .findByIsDeletedFalseOrderByCategoryIdAscDisplayOrderAsc();

        return options.stream()
                .map(option -> FilterOptionResponse.from(option, option.getChildren().size()))
                .collect(Collectors.toList());
    }
}