package com.hip.damoa.domain.user.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.jwt.JwtTokenProvider;
import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.repository.UserProfileRepository;
import com.hip.damoa.domain.user.repository.UserRepository;
import com.hip.damoa.domain.user.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // User의 role을 USER로 설정 (이전에 COMPANY였을 수 있으므로)
        if (!user.hasRole("USER") || user.hasRole("COMPANY")) {
            user.setRoles(new String[]{"USER"});
            log.info("사용자 role 변경: userId={}, role=USER", user.getId());
        }

        // User의 프로필 완성 상태 업데이트
        user.completeProfile();
        user = userRepository.save(user);
        log.info("프로필 완성 처리: userId={}, profileCompleted={}, role={}",
                user.getId(), user.getProfileCompleted(), user.getRoles()[0]);

        // JWT 토큰 재발급 (profileCompleted=true, currentRole=USER 반영)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);
        log.info("새로운 토큰 발급: userId={}, profileCompleted={}, currentRole={}",
                user.getId(), tokenInfo.isProfileCompleted(), tokenInfo.getCurrentRole());

        return UserProfileResponse.from(userProfile, tokenInfo);
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

        // User의 role을 COMPANY로 변경 (이전에 USER였을 수 있으므로)
        if (!user.hasRole("COMPANY") || user.hasRole("USER")) {
            user.setRoles(new String[]{"COMPANY"});
            log.info("사용자 role 변경: userId={}, role=COMPANY", user.getId());
        }

        // User의 프로필 완성 상태 업데이트
        user.completeProfile();
        user = userRepository.save(user);
        log.info("프로필 완성 처리: userId={}, profileCompleted={}, role={}",
                user.getId(), user.getProfileCompleted(), user.getRoles()[0]);

        // JWT 토큰 재발급 (profileCompleted=true, currentRole=COMPANY 반영)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(user);
        log.info("새로운 토큰 발급: userId={}, profileCompleted={}, currentRole={}",
                user.getId(), tokenInfo.isProfileCompleted(), tokenInfo.getCurrentRole());

        // CompanyProfileResponse로 변환 (UserProfile 데이터를 Company 형식으로)
        return CompanyProfileResponse.builder()
                .id(companyProfile.getId())
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
}
