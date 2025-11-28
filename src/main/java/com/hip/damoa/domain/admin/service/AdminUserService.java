package com.hip.damoa.domain.admin.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.admin.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.repository.UserProfileRepository;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 관리자 회원 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private static final Set<String> VALID_STATUSES = new HashSet<>(Arrays.asList(
            "ACTIVE", "INACTIVE", "SUSPENDED", "PENDING"
    ));

    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList(
            "USER", "COMPANY", "ADMIN"
    ));

    /**
     * 전체 회원 목록 조회 (페이징, 검색, 필터)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserListResponse> getAllUsers(
            String keyword,
            String status,
            String role,
            Pageable pageable) {

        log.info("전체 회원 목록 조회: keyword={}, status={}, role={}", keyword, status, role);

        Page<User> users = userRepository.searchForAdmin(keyword, status, role, pageable);

        return users.map(user -> {
            String name = getUserName(user);
            return AdminUserListResponse.from(user, name);
        });
    }

    /**
     * 회원 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(UUID userUuid) {
        log.info("회원 상세 정보 조회: userUuid={}", userUuid);

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String name = getUserName(user);
        String phoneNumber = getUserPhoneNumber(user);

        return AdminUserDetailResponse.from(user, name, phoneNumber);
    }

    /**
     * 회원 상태 변경
     */
    @Transactional
    public AdminUserDetailResponse updateUserStatus(
            UUID userUuid,
            AdminUserStatusUpdateRequest request) {

        log.info("회원 상태 변경: userUuid={}, newStatus={}, reason={}",
                userUuid, request.getStatus(), request.getReason());

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 상태 검증
        if (!VALID_STATUSES.contains(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS);
        }

        // 상태 변경
        user.updateStatus(request.getStatus());

        userRepository.save(user);

        log.info("회원 상태 변경 완료: userUuid={}, newStatus={}", userUuid, request.getStatus());

        String name = getUserName(user);
        String phoneNumber = getUserPhoneNumber(user);

        return AdminUserDetailResponse.from(user, name, phoneNumber);
    }

    /**
     * 회원 역할 변경
     */
    @Transactional
    public AdminUserDetailResponse updateUserRoles(
            UUID userUuid,
            AdminUserRoleUpdateRequest request) {

        log.info("회원 역할 변경: userUuid={}, newRoles={}",
                userUuid, Arrays.toString(request.getRoles()));

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 역할 검증
        for (String role : request.getRoles()) {
            if (!VALID_ROLES.contains(role)) {
                throw new BusinessException(ErrorCode.INVALID_USER_ROLE);
            }
        }

        // 중복 제거
        Set<String> uniqueRoles = new HashSet<>(Arrays.asList(request.getRoles()));
        String[] roles = uniqueRoles.toArray(new String[0]);

        // 역할 변경
        user.updateRoles(roles);

        userRepository.save(user);

        log.info("회원 역할 변경 완료: userUuid={}, newRoles={}", userUuid, Arrays.toString(roles));

        String name = getUserName(user);
        String phoneNumber = getUserPhoneNumber(user);

        return AdminUserDetailResponse.from(user, name, phoneNumber);
    }

    /**
     * 회원 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteUser(UUID userUuid) {
        log.info("회원 삭제 (Soft Delete): userUuid={}", userUuid);

        User user = userRepository.findByUuidAndIsDeletedFalse(userUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Soft delete
        user.softDelete();

        userRepository.save(user);

        log.info("회원 삭제 완료: userUuid={}", userUuid);
    }

    /**
     * 회원 이름 조회 (UserProfile에서)
     */
    private String getUserName(User user) {
        return userProfileRepository.findByUser(user)
                .map(UserProfile::getName)
                .orElse(user.getEmail()); // 프로필 없으면 이메일 반환
    }

    /**
     * 회원 전화번호 조회 (UserProfile에서)
     */
    private String getUserPhoneNumber(User user) {
        return userProfileRepository.findByUser(user)
                .map(UserProfile::getPhone)
                .orElse(null);
    }
}
