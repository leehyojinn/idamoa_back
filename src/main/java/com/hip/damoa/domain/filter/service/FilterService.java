package com.hip.damoa.domain.filter.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.filter.model.FilterCategory;
import com.hip.damoa.domain.filter.model.FilterOption;
import com.hip.damoa.domain.filter.repository.FilterCategoryRepository;
import com.hip.damoa.domain.filter.repository.FilterOptionRepository;
import com.hip.damoa.domain.filter.web.dto.BoardFilterResponse;
import com.hip.damoa.domain.filter.web.dto.FilterCategoryResponse;
import com.hip.damoa.domain.filter.web.dto.FilterOptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 필터 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilterService {

    private final FilterCategoryRepository filterCategoryRepository;
    private final FilterOptionRepository filterOptionRepository;

    /**
     * 모든 활성 카테고리와 옵션 조회
     */
    @Transactional(readOnly = true)
    public List<FilterCategoryResponse> getAllActiveCategories() {
        log.info("모든 활성 필터 카테고리 조회 시작");

        List<FilterCategory> categories = filterCategoryRepository.findActiveCategories();

        List<FilterCategoryResponse> response = categories.stream()
                .map(category -> {
                    List<FilterOption> options = filterOptionRepository
                            .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

                    List<FilterOptionResponse> optionResponses = options.stream()
                            .map(FilterOptionResponse::from)
                            .collect(Collectors.toList());

                    return FilterCategoryResponse.from(category, optionResponses);
                })
                .collect(Collectors.toList());

        log.info("활성 필터 카테고리 조회 완료: {}개", response.size());
        return response;
    }

    /**
     * 카테고리 코드로 특정 카테고리와 옵션 조회
     */
    @Transactional(readOnly = true)
    public FilterCategoryResponse getCategoryByCode(String categoryCode) {
        log.info("필터 카테고리 조회 시작: categoryCode={}", categoryCode);

        FilterCategory category = filterCategoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> {
                    log.error("필터 카테고리를 찾을 수 없음: categoryCode={}", categoryCode);
                    return new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND);
                });

        if (!category.getIsActive()) {
            log.error("비활성화된 필터 카테고리: categoryCode={}", categoryCode);
            throw new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND);
        }

        List<FilterOption> options = filterOptionRepository
                .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

        List<FilterOptionResponse> optionResponses = options.stream()
                .map(FilterOptionResponse::from)
                .collect(Collectors.toList());

        log.info("필터 카테고리 조회 완료: categoryCode={}, 옵션 개수={}", categoryCode, optionResponses.size());
        return FilterCategoryResponse.from(category, optionResponses);
    }

    /**
     * 카테고리 코드로 옵션만 조회
     */
    @Transactional(readOnly = true)
    public List<FilterOptionResponse> getOptionsByCategory(String categoryCode) {
        log.info("필터 옵션 조회 시작: categoryCode={}", categoryCode);

        FilterCategory category = filterCategoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> {
                    log.error("필터 카테고리를 찾을 수 없음: categoryCode={}", categoryCode);
                    return new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND);
                });

        if (!category.getIsActive()) {
            log.error("비활성화된 필터 카테고리: categoryCode={}", categoryCode);
            throw new BusinessException(ErrorCode.FILTER_CATEGORY_NOT_FOUND);
        }

        List<FilterOption> options = filterOptionRepository
                .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

        List<FilterOptionResponse> response = options.stream()
                .map(FilterOptionResponse::from)
                .collect(Collectors.toList());

        log.info("필터 옵션 조회 완료: categoryCode={}, 옵션 개수={}", categoryCode, response.size());
        return response;
    }

    /**
     * 필수 카테고리와 옵션 조회
     */
    @Transactional(readOnly = true)
    public List<FilterCategoryResponse> getRequiredCategories() {
        log.info("필수 필터 카테고리 조회 시작");

        List<FilterCategory> categories = filterCategoryRepository.findRequiredCategories();

        List<FilterCategoryResponse> response = categories.stream()
                .map(category -> {
                    List<FilterOption> options = filterOptionRepository
                            .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

                    List<FilterOptionResponse> optionResponses = options.stream()
                            .map(FilterOptionResponse::from)
                            .collect(Collectors.toList());

                    return FilterCategoryResponse.from(category, optionResponses);
                })
                .collect(Collectors.toList());

        log.info("필수 필터 카테고리 조회 완료: {}개", response.size());
        return response;
    }

    /**
     * 필터 타입별 카테고리와 옵션 조회
     */
    @Transactional(readOnly = true)
    public List<FilterCategoryResponse> getCategoriesByFilterType(String filterType) {
        log.info("필터 타입별 카테고리 조회 시작: filterType={}", filterType);

        List<FilterCategory> categories = filterCategoryRepository.findActiveByFilterType(filterType);

        List<FilterCategoryResponse> response = categories.stream()
                .map(category -> {
                    List<FilterOption> options = filterOptionRepository
                            .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

                    List<FilterOptionResponse> optionResponses = options.stream()
                            .map(FilterOptionResponse::from)
                            .collect(Collectors.toList());

                    return FilterCategoryResponse.from(category, optionResponses);
                })
                .collect(Collectors.toList());

        log.info("필터 타입별 카테고리 조회 완료: filterType={}, 카테고리 개수={}", filterType, response.size());
        return response;
    }

    /**
     * 갤러리 게시판용 필터 목록 조회
     * entityType이 'BOARD'이고 metadata의 board_types 배열에 'gallery'가 포함된 필터들을 조회
     */
    @Transactional(readOnly = true)
    public List<BoardFilterResponse> getGalleryFilters() {
        log.info("갤러리 필터 목록 조회 시작 (metadata 기반)");

        // entityType이 BOARD이고 metadata의 board_types 배열에 'gallery'가 포함된 카테고리 조회
        List<FilterCategory> categories = filterCategoryRepository.findBoardFiltersByType("[\"GALLERY\"]");

        List<BoardFilterResponse> response = categories.stream()
                .map(category -> {
                    try {
                        List<FilterOption> options = filterOptionRepository
                                .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

                        List<FilterOptionResponse> optionResponses = options.stream()
                                .map(FilterOptionResponse::from)
                                .collect(Collectors.toList());

                        FilterCategoryResponse categoryResponse = FilterCategoryResponse.from(category, optionResponses);
                        return BoardFilterResponse.from(categoryResponse);
                    } catch (Exception e) {
                        log.error("갤러리 필터 조회 중 오류 발생: categoryCode={}", category.getCode(), e);
                        return null;
                    }
                })
                .filter(filter -> filter != null)
                .collect(Collectors.toList());

        log.info("갤러리 필터 목록 조회 완료: {}개 카테고리", response.size());
        return response;
    }

    /**
     * 자료실 게시판용 필터 목록 조회
     * entityType이 'BOARD'이고 metadata의 board_types 배열에 'document'가 포함된 필터들을 조회
     */
    @Transactional(readOnly = true)
    public List<BoardFilterResponse> getDocumentFilters() {
        log.info("자료실 필터 목록 조회 시작 (metadata 기반)");

        // entityType이 BOARD이고 metadata의 board_types 배열에 'document'가 포함된 카테고리 조회
        List<FilterCategory> categories = filterCategoryRepository.findBoardFiltersByType("[\"DOCUMENT\"]");

        List<BoardFilterResponse> response = categories.stream()
                .map(category -> {
                    try {
                        List<FilterOption> options = filterOptionRepository
                                .findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);

                        List<FilterOptionResponse> optionResponses = options.stream()
                                .map(FilterOptionResponse::from)
                                .collect(Collectors.toList());

                        FilterCategoryResponse categoryResponse = FilterCategoryResponse.from(category, optionResponses);
                        return BoardFilterResponse.from(categoryResponse);
                    } catch (Exception e) {
                        log.error("자료실 필터 조회 중 오류 발생: categoryCode={}", category.getCode(), e);
                        return null;
                    }
                })
                .filter(filter -> filter != null)
                .collect(Collectors.toList());

        log.info("자료실 필터 목록 조회 완료: {}개 카테고리", response.size());
        return response;
    }
}
