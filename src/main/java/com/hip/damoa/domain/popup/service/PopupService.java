package com.hip.damoa.domain.popup.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.popup.model.Popup;
import com.hip.damoa.domain.popup.repository.PopupRepository;
import com.hip.damoa.domain.popup.web.dto.PopupCreateRequest;
import com.hip.damoa.domain.popup.web.dto.PopupResponse;
import com.hip.damoa.domain.popup.web.dto.PopupUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 팝업 Service
 *
 * 홈페이지 팝업 관리 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopupService {

    private final PopupRepository popupRepository;
    private final FileRepository fileRepository;

    /**
     * 팝업 생성 (ADMIN)
     */
    @Transactional
    public PopupResponse createPopup(String userEmail, PopupCreateRequest request) {
        log.info("팝업 생성 시작: userEmail={}, title={}", userEmail, request.getTitle());

        // 이미지 UUID 검증 (선택사항)
        if (request.getImageUuid() != null) {
            fileRepository.findByUuidAndIsDeletedFalse(request.getImageUuid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        }

        // Popup 엔티티 생성
        Popup popup = Popup.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUuid(request.getImageUuid())
                .linkUrl(request.getLinkUrl())
                .width(request.getWidth())
                .height(request.getHeight())
                .position(request.getPosition())
                .offsetX(request.getOffsetX())
                .offsetY(request.getOffsetY())
                .displayStartDate(request.getDisplayStartDate())
                .displayEndDate(request.getDisplayEndDate())
                .displayOrder(request.getDisplayOrder())
                .isActive(request.getIsActive())
                .createdBy(userEmail)
                .build();

        popup = popupRepository.save(popup);

        log.info("팝업 생성 완료: uuid={}", popup.getUuid());

        // 이미지 URL 로드
        String imageUrl = loadImageUrl(popup.getImageUuid());

        return PopupResponse.from(popup, imageUrl);
    }

    /**
     * 팝업 수정 (ADMIN)
     */
    @Transactional
    public PopupResponse updatePopup(UUID uuid, String userEmail, PopupUpdateRequest request) {
        log.info("팝업 수정 시작: uuid={}, userEmail={}", uuid, userEmail);

        Popup popup = popupRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));

        // 이미지 UUID 검증 (선택사항)
        if (request.getImageUuid() != null) {
            fileRepository.findByUuidAndIsDeletedFalse(request.getImageUuid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        }

        // 팝업 정보 업데이트
        popup.update(
                request.getTitle(),
                request.getContent(),
                request.getImageUuid(),
                request.getLinkUrl(),
                request.getWidth(),
                request.getHeight(),
                request.getPosition(),
                request.getOffsetX(),
                request.getOffsetY(),
                request.getDisplayStartDate(),
                request.getDisplayEndDate(),
                request.getDisplayOrder(),
                userEmail
        );

        log.info("팝업 수정 완료: uuid={}", uuid);

        // 이미지 URL 로드
        String imageUrl = loadImageUrl(popup.getImageUuid());

        return PopupResponse.from(popup, imageUrl);
    }

    /**
     * 팝업 삭제 (Soft Delete) (ADMIN)
     */
    @Transactional
    public void deletePopup(UUID uuid) {
        log.info("팝업 삭제 시작: uuid={}", uuid);

        Popup popup = popupRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));

        popup.softDelete();

        log.info("팝업 삭제 완료: uuid={}", uuid);
    }

    /**
     * 팝업 조회 (ADMIN)
     */
    @Transactional(readOnly = true)
    public PopupResponse getPopup(UUID uuid) {
        log.info("팝업 조회: uuid={}", uuid);

        Popup popup = popupRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));

        // 이미지 URL 로드
        String imageUrl = loadImageUrl(popup.getImageUuid());

        return PopupResponse.from(popup, imageUrl);
    }

    /**
     * 팝업 목록 조회 (ADMIN, 페이징)
     */
    @Transactional(readOnly = true)
    public Page<PopupResponse> getPopupList(Pageable pageable) {
        log.info("팝업 목록 조회: pageable={}", pageable);

        Page<Popup> popups = popupRepository.findByIsDeletedFalse(pageable);

        return popups.map(popup -> {
            String imageUrl = loadImageUrl(popup.getImageUuid());
            return PopupResponse.from(popup, imageUrl);
        });
    }

    /**
     * 팝업 활성화 (ADMIN)
     */
    @Transactional
    public PopupResponse activatePopup(UUID uuid) {
        log.info("팝업 활성화: uuid={}", uuid);

        Popup popup = popupRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));

        popup.activate();

        log.info("팝업 활성화 완료: uuid={}", uuid);

        // 이미지 URL 로드
        String imageUrl = loadImageUrl(popup.getImageUuid());

        return PopupResponse.from(popup, imageUrl);
    }

    /**
     * 팝업 비활성화 (ADMIN)
     */
    @Transactional
    public PopupResponse deactivatePopup(UUID uuid) {
        log.info("팝업 비활성화: uuid={}", uuid);

        Popup popup = popupRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));

        popup.deactivate();

        log.info("팝업 비활성화 완료: uuid={}", uuid);

        // 이미지 URL 로드
        String imageUrl = loadImageUrl(popup.getImageUuid());

        return PopupResponse.from(popup, imageUrl);
    }

    /**
     * 활성 팝업 목록 조회 (Public, 기간 필터링 포함)
     */
    @Transactional(readOnly = true)
    public List<PopupResponse> getActivePopups() {
        log.info("활성 팝업 목록 조회");

        LocalDateTime now = LocalDateTime.now();
        List<Popup> popups = popupRepository.findDisplayablePopups(now);

        return popups.stream()
                .map(popup -> {
                    String imageUrl = loadImageUrl(popup.getImageUuid());
                    return PopupResponse.from(popup, imageUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * 조회수 증가 (Public)
     */
    @Transactional
    public void incrementViewCount(UUID uuid) {
        log.debug("팝업 조회수 증가: uuid={}", uuid);

        Popup popup = popupRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));

        popup.incrementViewCount();
    }

    /**
     * 클릭수 증가 (Public)
     */
    @Transactional
    public void incrementClickCount(UUID uuid) {
        log.debug("팝업 클릭수 증가: uuid={}", uuid);

        Popup popup = popupRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));

        popup.incrementClickCount();
    }

    /**
     * 이미지 URL 로드 (private 헬퍼 메서드)
     */
    private String loadImageUrl(UUID imageUuid) {
        if (imageUuid == null) {
            return null;
        }

        return fileRepository.findByUuidAndIsDeletedFalse(imageUuid)
                .map(File::getFileUrl)
                .orElse(null);
    }
}
