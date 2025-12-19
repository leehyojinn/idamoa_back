package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.GalleryPromotionType;
import com.hip.damoa.domain.board.model.GalleryPromotionTypeSetting;
import com.hip.damoa.domain.board.repository.GalleryPromotionTypeSettingRepository;
import com.hip.damoa.domain.board.web.dto.GalleryPromotionTypeSettingCreateRequest;
import com.hip.damoa.domain.board.web.dto.GalleryPromotionTypeSettingUpdateRequest;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 갤러리 우대등록 설정 관리 서비스
 * - 타입별 개별 설정 관리 (확장 가능한 구조)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryPromotionSettingsService {

    private final GalleryPromotionTypeSettingRepository settingRepository;
    private final UserRepository userRepository;

    /**
     * 모든 설정 조회 (정렬순)
     */
    @Transactional(readOnly = true)
    public List<GalleryPromotionTypeSetting> getAllSettings() {
        return settingRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * 활성화된 설정만 조회 (정렬순)
     */
    @Transactional(readOnly = true)
    public List<GalleryPromotionTypeSetting> getActiveSettings() {
        return settingRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * UUID로 설정 조회
     */
    @Transactional(readOnly = true)
    public GalleryPromotionTypeSetting getSettingByUuid(UUID uuid) {
        return settingRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * 프로모션 타입으로 설정 조회
     */
    @Transactional(readOnly = true)
    public GalleryPromotionTypeSetting getSettingByType(String promotionType) {
        return settingRepository.findByPromotionType(promotionType)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * 프로모션 타입으로 활성화된 설정 조회
     */
    @Transactional(readOnly = true)
    public GalleryPromotionTypeSetting getActiveSettingByType(String promotionType) {
        return settingRepository.findByPromotionTypeAndIsActiveTrue(promotionType)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * 새 타입 설정 생성 (관리자 전용)
     */
    @Transactional
    public GalleryPromotionTypeSetting createSetting(String adminEmail, GalleryPromotionTypeSettingCreateRequest request) {
        log.info("갤러리 우대등록 타입 생성 시작: adminEmail={}, promotionType={}", adminEmail, request.getPromotionType());

        // 중복 체크
        if (settingRepository.existsByPromotionType(request.getPromotionType())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }

        GalleryPromotionTypeSetting setting = GalleryPromotionTypeSetting.builder()
                .promotionType(request.getPromotionType())
                .displayName(request.getDisplayName())
                .price(request.getPrice())
                .weight(request.getWeight())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .description(request.getDescription())
                .build();

        setting = settingRepository.save(setting);

        log.info("갤러리 우대등록 타입 생성 완료: uuid={}, promotionType={}", setting.getUuid(), setting.getPromotionType());

        return setting;
    }

    /**
     * 설정 업데이트 (관리자 전용)
     */
    @Transactional
    public GalleryPromotionTypeSetting updateSetting(String adminEmail, UUID uuid, GalleryPromotionTypeSettingUpdateRequest request) {
        log.info("갤러리 우대등록 타입 업데이트 시작: adminEmail={}, uuid={}", adminEmail, uuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        GalleryPromotionTypeSetting setting = settingRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        setting.update(
                request.getDisplayName(),
                request.getPrice(),
                request.getWeight(),
                request.getDisplayOrder(),
                request.getIsActive(),
                request.getDescription(),
                admin
        );

        setting = settingRepository.save(setting);

        log.info("갤러리 우대등록 타입 업데이트 완료: uuid={}, promotionType={}, price={}, weight={}",
                setting.getUuid(), setting.getPromotionType(), setting.getPrice(), setting.getWeight());

        return setting;
    }

    /**
     * 설정 삭제 (비활성화)
     */
    @Transactional
    public void deactivateSetting(String adminEmail, UUID uuid) {
        log.info("갤러리 우대등록 타입 비활성화 시작: adminEmail={}, uuid={}", adminEmail, uuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        GalleryPromotionTypeSetting setting = settingRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        setting.deactivate(admin);
        settingRepository.save(setting);

        log.info("갤러리 우대등록 타입 비활성화 완료: uuid={}, promotionType={}", uuid, setting.getPromotionType());
    }

    /**
     * 프로모션 타입별 가격 조회 (GalleryPromotionType enum 사용)
     */
    @Transactional(readOnly = true)
    public BigDecimal getPriceByType(GalleryPromotionType type) {
        GalleryPromotionTypeSetting setting = settingRepository.findByPromotionTypeAndIsActiveTrue(type.name())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return setting.getPrice();
    }

    /**
     * 프로모션 타입별 가중치 조회 (GalleryPromotionType enum 사용)
     */
    @Transactional(readOnly = true)
    public Integer getWeightByType(GalleryPromotionType type) {
        GalleryPromotionTypeSetting setting = settingRepository.findByPromotionTypeAndIsActiveTrue(type.name())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return setting.getWeight();
    }

    /**
     * 업그레이드 차액 조회 (PREMIUM - STANDARD)
     * - 기존 호환성 유지를 위한 메서드
     */
    @Transactional(readOnly = true)
    public BigDecimal getUpgradePrice() {
        BigDecimal standardPrice = getPriceByType(GalleryPromotionType.STANDARD);
        BigDecimal premiumPrice = getPriceByType(GalleryPromotionType.PREMIUM);
        return premiumPrice.subtract(standardPrice);
    }
}
