package com.hip.damoa.domain.user.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.jwt.JwtTokenProvider;
import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.payment.service.CreditService;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.repository.UserProfileRepository;
import com.hip.damoa.domain.user.repository.UserRepository;
import com.hip.damoa.domain.user.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 프로필 관리 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CompanyRepository companyRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileRepository fileRepository;
    private final CreditService creditService;

    /**
     * Avatar 파일 정보 추출 헬퍼 메서드 (프로필 조회용)
     * avatarUrl이 있으면 해당 URL로 File 조회하여 UUID 포함 반환
     * @param avatarUrl 프로필 avatar URL
     * @return [fileId, fileUuid, avatarUrl] 배열
     */
    private Object[] getAvatarFileInfo(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return new Object[]{null, null, null};
        }

        // URL로 File 조회
        File file = fileRepository.findByFileUrl(avatarUrl).orElse(null);
        if (file == null) {
            // File이 없으면 URL만 반환 (레거시 호환)
            return new Object[]{null, null, avatarUrl};
        }

        return new Object[]{file.getId(), file.getUuid(), avatarUrl};
    }

    private static final String ENTITY_TYPE_USER_AVATAR = "USER_PROFILE_AVATAR";

    /**
     * UUID로 File 조회하여 URL 반환 및 엔티티 연결
     * @param uuidString 파일 UUID 문자열
     * @param profileId UserProfile ID (orphan 파일 삭제 방지)
     * @return 파일 URL (없으면 null)
     */
    private String getFileUrlByUuid(String uuidString, Long profileId) {
        if (uuidString == null || uuidString.isEmpty()) {
            return null;
        }

        UUID uuid = UUID.fromString(uuidString);
        File file = fileRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // entity 정보 업데이트 (orphan 파일 삭제 방지)
        file.updateEntityInfo(ENTITY_TYPE_USER_AVATAR, profileId);
        fileRepository.save(file);
        log.info("아바타 파일 엔티티 연결: fileId={}, profileId={}", file.getId(), profileId);

        return file.getFileUrl();
    }

    /**
     * 프로필 상태 조회
     */
    @Transactional(readOnly = true)
    public ProfileStatusResponse getProfileStatus(String email) {
        log.info("프로필 상태 조회: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String profileType = null;

        // 프로필이 완성되었다면 어떤 타입인지 확인
        if (user.getProfileCompleted()) {
            // UserProfile에서 profileType 가져오기
            profileType = userProfileRepository.findByUserId(user.getId())
                    .map(UserProfile::getProfileType)
                    .orElse(null);
        }

        return ProfileStatusResponse.builder()
                .profileCompleted(user.getProfileCompleted())
                .profileType(profileType)
                .currentRole(user.getRoles()[0])
                .build();
    }

    /**
     * USER 프로필 생성
     */
    @Transactional
    public UserProfileResponse createUserProfile(String email, UserProfileCreateRequest request) {
        log.info("USER 프로필 생성 시작: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 프로필이 이미 완성되었는지 확인
        if (user.getProfileCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_COMPLETED);
        }

        // 이미 프로필이 존재하는지 확인
        if (userProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_COMPLETED);
        }

        // UserProfile 생성
        UserProfile userProfile = UserProfile.builder()
                .user(user)
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .bio(request.getBio())
                .address(request.getAddress())
                .postalCode(request.getPostalCode())
                .build();

        userProfile = userProfileRepository.save(userProfile);
        log.info("UserProfile 생성 완료: id={}, userId={}", userProfile.getId(), user.getId());

        // 일반 유저 프로필 등록 이벤트 보너스 지급
        creditService.grantUserProfileBonus(user);

        // User의 role을 USER로 설정 (이전에 COMPANY였을 수 있으므로)
        if (!user.hasRole("USER") || user.hasRole("COMPANY")) {
            user.setRoles(new String[]{"USER"});
            log.info("사용자 role 변경: userId={}, role=USER", user.getId());
        }

        // User의 프로필 완성 상태 업데이트 및 활성화
        user.completeProfile();
        user.activate();
        user = userRepository.save(user);
        log.info("프로필 완성 및 활성화 처리: userId={}, profileCompleted={}, status={}, role={}",
                user.getId(), user.getProfileCompleted(), user.getStatus(), user.getRoles()[0]);

        // JWT 토큰 재발급 (profileCompleted=true, currentRole=USER 반영)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);
        log.info("새로운 토큰 발급: userId={}, profileCompleted={}, currentRole={}",
                user.getId(), tokenInfo.isProfileCompleted(), tokenInfo.getCurrentRole());

        // Avatar 파일 정보 추출
        Object[] avatarInfo = getAvatarFileInfo(userProfile.getAvatarUrl());
        Long avatarFileId = (Long) avatarInfo[0];
        UUID avatarFileUuid = (UUID) avatarInfo[1];
        String avatarUrl = (String) avatarInfo[2];

        return UserProfileResponse.from(userProfile, avatarFileId, avatarFileUuid, avatarUrl, tokenInfo);
    }

    /**
     * COMPANY 프로필 생성
     */
    @Transactional
    public CompanyProfileResponse createCompanyProfile(String email, CompanyProfileCreateRequest request) {
        log.info("COMPANY 프로필 생성 시작: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 프로필이 이미 완성되었는지 확인
        if (user.getProfileCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_COMPLETED);
        }

        // 이미 프로필이 존재하는지 확인
        if (userProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_COMPLETED);
        }

        // UserProfile 생성 (COMPANY 타입)
        UserProfile companyProfile = UserProfile.builder()
                .user(user)
                .profileType("COMPANY")
                .name(request.getName())  // 업체명
                .bio(request.getDescription())  // 업체 소개
                .phone(request.getPrimaryPhone())  // 대표 전화
                .address(request.getAddress())
                .postalCode(request.getPostalCode())
                .build();

        companyProfile = userProfileRepository.save(companyProfile);
        log.info("COMPANY 프로필 생성 완료: id={}, userId={}, profileType=COMPANY",
                companyProfile.getId(), user.getId());

        // 업체 프로필 등록 이벤트 보너스 지급
        creditService.grantCompanyProfileBonus(user);

        // User의 role을 COMPANY로 변경 (이전에 USER였을 수 있으므로)
        if (!user.hasRole("COMPANY") || user.hasRole("USER")) {
            user.setRoles(new String[]{"COMPANY"});
            log.info("사용자 role 변경: userId={}, role=COMPANY", user.getId());
        }

        // User의 프로필 완성 상태 업데이트 및 활성화
        user.completeProfile();
        user.activate();
        user = userRepository.save(user);
        log.info("프로필 완성 및 활성화 처리: userId={}, profileCompleted={}, status={}, role={}",
                user.getId(), user.getProfileCompleted(), user.getStatus(), user.getRoles()[0]);

        // JWT 토큰 재발급 (profileCompleted=true, currentRole=COMPANY 반영)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);
        log.info("새로운 토큰 발급: userId={}, profileCompleted={}, currentRole={}",
                user.getId(), tokenInfo.isProfileCompleted(), tokenInfo.getCurrentRole());

        // CompanyProfileResponse로 변환 (UserProfile 데이터를 Company 형식으로)
        return CompanyProfileResponse.builder()
                .uuid(companyProfile.getUuid())
                .name(companyProfile.getName())
                .description(companyProfile.getBio())
                .primaryPhone(companyProfile.getPhone())
                .email(request.getEmail())  // email은 UserProfile에 없으므로 request에서 가져옴
                .address(companyProfile.getAddress())
                .postalCode(companyProfile.getPostalCode())
                .status("ACTIVE")  // 기본값
                .tokenInfo(tokenInfo)  // 새로운 JWT 토큰 포함
                .build();
    }

    /**
     * 프로필 조회 (통합 - USER/COMPANY 자동 판단)
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        log.info("프로필 조회 (통합): email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        // Avatar 파일 정보 추출
        Object[] avatarInfo = getAvatarFileInfo(profile.getAvatarUrl());
        Long avatarFileId = (Long) avatarInfo[0];
        UUID avatarFileUuid = (UUID) avatarInfo[1];
        String avatarUrl = (String) avatarInfo[2];

        return ProfileResponse.from(profile, user, avatarFileId, avatarFileUuid, avatarUrl);
    }

    /**
     * USER 프로필 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String email) {
        log.info("USER 프로필 조회: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        // Avatar 파일 정보 추출
        Object[] avatarInfo = getAvatarFileInfo(userProfile.getAvatarUrl());
        Long avatarFileId = (Long) avatarInfo[0];
        UUID avatarFileUuid = (UUID) avatarInfo[1];
        String avatarUrl = (String) avatarInfo[2];

        return UserProfileResponse.from(userProfile, avatarFileId, avatarFileUuid, avatarUrl);
    }

    /**
     * COMPANY 프로필 조회 (UserProfile 테이블에서)
     */
    @Transactional(readOnly = true)
    public CompanyProfileResponse getCompanyProfile(String email) {
        log.info("COMPANY 프로필 조회: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile companyProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        // UserProfile → CompanyProfileResponse 변환
        return CompanyProfileResponse.builder()
                .uuid(companyProfile.getUuid())
                .name(companyProfile.getName())
                .description(companyProfile.getBio())
                .primaryPhone(companyProfile.getPhone())
                .email(user.getEmail())
                .address(companyProfile.getAddress())
                .postalCode(companyProfile.getPostalCode())
                .status("ACTIVE")
                .build();
    }

    /**
     * USER 프로필 수정
     */
    @Transactional
    public UserProfileResponse updateUserProfile(String email, UserProfileUpdateRequest request) {
        log.info("USER 프로필 수정 시작: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        // 프로필 정보 업데이트
        userProfile.updateProfile(request.getName(), request.getNickname(), request.getBio());

        // 주소 정보 업데이트
        if (request.getAddress() != null || request.getPostalCode() != null) {
            userProfile.updateAddress(
                request.getAddress(),
                request.getPostalCode(),
                null,  // latitude
                null   // longitude
            );
        }

        // 아바타 업데이트 (UUID → URL 변환)
        if (request.getAvatarUuid() != null) {
            String avatarUrl = getFileUrlByUuid(request.getAvatarUuid(), userProfile.getId());
            if (avatarUrl != null) {
                userProfile.updateAvatar(avatarUrl);
            }
        }

        // 공개 여부 업데이트
        if (request.getProfileVisibility() != null) {
            userProfile.changeVisibility(request.getProfileVisibility());
        }

        userProfile = userProfileRepository.save(userProfile);
        log.info("USER 프로필 수정 완료: id={}, userId={}", userProfile.getId(), user.getId());

        // Avatar 파일 정보 추출
        Object[] avatarInfo = getAvatarFileInfo(userProfile.getAvatarUrl());
        Long avatarFileId = (Long) avatarInfo[0];
        UUID avatarFileUuid = (UUID) avatarInfo[1];
        String avatarUrl = (String) avatarInfo[2];

        return UserProfileResponse.from(userProfile, avatarFileId, avatarFileUuid, avatarUrl);
    }

    /**
     * COMPANY 프로필 수정
     */
    @Transactional
    public CompanyProfileResponse updateCompanyProfile(String email, CompanyProfileUpdateRequest request) {
        log.info("COMPANY 프로필 수정 시작: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile companyProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        // COMPANY 타입 확인
        if (!"COMPANY".equals(companyProfile.getProfileType())) {
            throw new BusinessException(ErrorCode.INVALID_PROFILE_TYPE);
        }

        // 프로필 정보 업데이트 (COMPANY는 nickname 없음)
        companyProfile.updateProfile(request.getName(), null, request.getDescription());

        // 주소 정보 업데이트
        if (request.getAddress() != null || request.getPostalCode() != null) {
            companyProfile.updateAddress(
                request.getAddress(),
                request.getPostalCode(),
                null,  // latitude
                null   // longitude
            );
        }

        companyProfile = userProfileRepository.save(companyProfile);
        log.info("COMPANY 프로필 수정 완료: id={}, userId={}", companyProfile.getId(), user.getId());

        // CompanyProfileResponse로 변환
        return CompanyProfileResponse.builder()
                .uuid(companyProfile.getUuid())
                .name(companyProfile.getName())
                .description(companyProfile.getBio())
                .primaryPhone(request.getPrimaryPhone())
                .email(request.getEmail())
                .address(companyProfile.getAddress())
                .postalCode(companyProfile.getPostalCode())
                .status("ACTIVE")
                .build();
    }
}
