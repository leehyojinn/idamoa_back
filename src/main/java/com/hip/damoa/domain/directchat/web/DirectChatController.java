package com.hip.damoa.domain.directchat.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.directchat.model.DirectChatMessage;
import com.hip.damoa.domain.directchat.model.DirectChatRoom;
import com.hip.damoa.domain.directchat.service.DirectChatNotificationService;
import com.hip.damoa.domain.directchat.service.DirectChatPresenceService;
import com.hip.damoa.domain.directchat.service.DirectChatService;
import com.hip.damoa.domain.directchat.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 범용 1:1 채팅 REST API 컨트롤러
 */
@Tag(name = "Direct Chat", description = "1:1 채팅 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/direct-chats")
public class DirectChatController {

    private final DirectChatService chatService;
    private final DirectChatPresenceService presenceService;
    private final DirectChatNotificationService notificationService;
    private final UserRepository userRepository;

    // ===== 채팅방 관리 =====

    @Operation(summary = "채팅방 생성/조회", description = "대상 사용자와의 채팅방을 생성하거나 기존 채팅방을 조회합니다")
    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DirectChatRoomResponse> createOrGetRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DirectChatRoomCreateRequest request) {

        User currentUser = getCurrentUser(userDetails.getUsername());
        DirectChatRoom room = chatService.getOrCreateRoom(userDetails.getUsername(), request.getTargetUserUuid());

        long unreadCount = chatService.getUnreadCount(room.getUuid(), userDetails.getUsername());
        User otherUser = room.getOtherUser(currentUser.getId());
        boolean isOtherOnline = otherUser != null && presenceService.isOnline(otherUser.getId());

        return ApiResponse.success(DirectChatRoomResponse.from(room, currentUser.getId(), unreadCount, isOtherOnline));
    }

    @Operation(summary = "채팅방 목록 조회", description = "내 채팅방 목록을 조회합니다")
    @GetMapping("/rooms")
    public ApiResponse<List<DirectChatRoomResponse>> getRooms(
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = getCurrentUser(userDetails.getUsername());
        List<DirectChatRoom> rooms = chatService.getRooms(userDetails.getUsername());

        // 미읽음 수 일괄 조회 (N+1 방지)
        Map<Long, Long> unreadCounts = chatService.getUnreadCountsForUser(userDetails.getUsername());

        // 온라인 상태 일괄 조회를 위한 사용자 ID 수집
        List<Long> otherUserIds = rooms.stream()
                .map(room -> room.getOtherUser(currentUser.getId()))
                .filter(user -> user != null)
                .map(User::getId)
                .collect(Collectors.toList());

        // 온라인 상태 일괄 조회
        Map<Long, Boolean> onlineStatuses = presenceService.getOnlineStatuses(otherUserIds);

        List<DirectChatRoomResponse> responses = rooms.stream()
                .map(room -> {
                    long unreadCount = unreadCounts.getOrDefault(room.getId(), 0L);
                    User otherUser = room.getOtherUser(currentUser.getId());
                    boolean isOtherOnline = otherUser != null && onlineStatuses.getOrDefault(otherUser.getId(), false);
                    return DirectChatRoomResponse.from(room, currentUser.getId(), unreadCount, isOtherOnline);
                })
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @Operation(summary = "채팅방 상세 조회", description = "채팅방 상세 정보를 조회합니다")
    @GetMapping("/rooms/{roomUuid}")
    public ApiResponse<DirectChatRoomResponse> getRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "채팅방 UUID") @PathVariable UUID roomUuid) {

        User currentUser = getCurrentUser(userDetails.getUsername());
        DirectChatRoom room = chatService.getRoom(roomUuid, userDetails.getUsername());

        long unreadCount = chatService.getUnreadCount(roomUuid, userDetails.getUsername());
        User otherUser = room.getOtherUser(currentUser.getId());
        boolean isOtherOnline = otherUser != null && presenceService.isOnline(otherUser.getId());

        return ApiResponse.success(DirectChatRoomResponse.from(room, currentUser.getId(), unreadCount, isOtherOnline));
    }

    @Operation(summary = "채팅방 나가기", description = "채팅방을 나갑니다 (비활성화)")
    @DeleteMapping("/rooms/{roomUuid}")
    public ApiResponse<Void> leaveRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "채팅방 UUID") @PathVariable UUID roomUuid) {

        chatService.leaveRoom(roomUuid, userDetails.getUsername());
        return ApiResponse.success();
    }

    // ===== 메시지 관리 =====

    @Operation(summary = "메시지 목록 조회", description = "채팅방의 메시지 목록을 조회합니다")
    @GetMapping("/rooms/{roomUuid}/messages")
    public ApiResponse<Page<DirectChatMessageResponse>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "채팅방 UUID") @PathVariable UUID roomUuid,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        User currentUser = getCurrentUser(userDetails.getUsername());
        Page<DirectChatMessage> messages = chatService.getMessages(roomUuid, userDetails.getUsername(), pageable);

        Page<DirectChatMessageResponse> responses = messages.map(
                msg -> DirectChatMessageResponse.from(msg, currentUser.getId()));

        return ApiResponse.success(responses);
    }

    @Operation(summary = "메시지 전송 (REST)", description = "채팅방에 메시지를 전송합니다 (REST API)")
    @PostMapping("/rooms/{roomUuid}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DirectChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "채팅방 UUID") @PathVariable UUID roomUuid,
            @Valid @RequestBody DirectChatMessageRequest request) {

        User currentUser = getCurrentUser(userDetails.getUsername());
        DirectChatMessage message = chatService.sendMessage(
                roomUuid,
                userDetails.getUsername(),
                request.getContent(),
                request.getMessageType(),
                request.getFileUuids()
        );

        // 상대방에게 알림 예약
        DirectChatRoom room = message.getRoom();
        User recipient = room.getOtherUser(currentUser.getId());
        if (recipient != null) {
            notificationService.scheduleMessageNotification(room, message, recipient);
        }

        return ApiResponse.success(DirectChatMessageResponse.from(message, currentUser.getId()));
    }

    // ===== 읽음 상태 =====

    @Operation(summary = "읽음 처리", description = "채팅방의 모든 메시지를 읽음 처리합니다")
    @PostMapping("/rooms/{roomUuid}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "채팅방 UUID") @PathVariable UUID roomUuid) {

        User currentUser = getCurrentUser(userDetails.getUsername());
        DirectChatRoom room = chatService.getRoom(roomUuid, userDetails.getUsername());

        // 읽음 처리
        chatService.markAsRead(roomUuid, userDetails.getUsername());

        // 대기 중인 알림 취소
        notificationService.cancelPendingNotifications(room.getId(), currentUser.getId());

        return ApiResponse.success();
    }

    @Operation(summary = "미읽음 수 조회", description = "채팅방의 미읽음 메시지 수를 조회합니다")
    @GetMapping("/rooms/{roomUuid}/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "채팅방 UUID") @PathVariable UUID roomUuid) {

        long count = chatService.getUnreadCount(roomUuid, userDetails.getUsername());
        return ApiResponse.success(UnreadCountResponse.of(count));
    }

    @Operation(summary = "전체 미읽음 수 조회", description = "모든 채팅방의 총 미읽음 메시지 수를 조회합니다")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getTotalUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        long count = chatService.getTotalUnreadCount(userDetails.getUsername());
        return ApiResponse.success(UnreadCountResponse.of(count));
    }

    // ===== Helper =====

    private User getCurrentUser(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
