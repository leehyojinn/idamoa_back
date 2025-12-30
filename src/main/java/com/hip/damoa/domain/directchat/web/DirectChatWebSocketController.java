package com.hip.damoa.domain.directchat.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.directchat.model.DirectChatMessage;
import com.hip.damoa.domain.directchat.model.DirectChatRoom;
import com.hip.damoa.domain.directchat.service.DirectChatNotificationService;
import com.hip.damoa.domain.directchat.service.DirectChatPresenceService;
import com.hip.damoa.domain.directchat.service.DirectChatService;
import com.hip.damoa.domain.directchat.web.dto.DirectChatMessageRequest;
import com.hip.damoa.domain.directchat.web.dto.DirectChatMessageResponse;
import com.hip.damoa.domain.directchat.web.dto.TypingIndicator;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * 범용 1:1 채팅 WebSocket 컨트롤러
 *
 * WebSocket 엔드포인트:
 * - 클라이언트 → 서버 (메시지): /app/direct-chats/rooms/{roomUuid}/messages
 * - 클라이언트 → 서버 (타이핑): /app/direct-chats/rooms/{roomUuid}/typing
 * - 클라이언트 → 서버 (읽음): /app/direct-chats/rooms/{roomUuid}/read
 * - 서버 → 클라이언트 (메시지): /topic/direct-chats/rooms/{roomUuid}
 * - 서버 → 클라이언트 (타이핑): /topic/direct-chats/rooms/{roomUuid}/typing
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectChatWebSocketController {

    private final DirectChatService chatService;
    private final DirectChatPresenceService presenceService;
    private final DirectChatNotificationService notificationService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 실시간 메시지 전송
     */
    @MessageMapping("/direct-chats/rooms/{roomUuid}/messages")
    public void sendMessage(
            @DestinationVariable UUID roomUuid,
            DirectChatMessageRequest request,
            Principal principal) {

        // Principal null 체크
        if (principal == null) {
            log.warn("WebSocket 메시지 전송 실패: Principal이 null, roomUuid={}", roomUuid);
            return;
        }

        String userEmail = principal.getName();
        log.info("WebSocket 메시지 전송: roomUuid={}", roomUuid);

        try {
            User currentUser = getCurrentUser(userEmail);

            // 메시지 저장
            DirectChatMessage message = chatService.sendMessage(
                    roomUuid,
                    userEmail,
                    request.getContent(),
                    request.getMessageType(),
                    request.getFileUuids()
            );

            // DTO 변환
            DirectChatMessageResponse response = DirectChatMessageResponse.from(message, currentUser.getId());

            // 채팅방 구독자들에게 브로드캐스트
            String destination = "/topic/direct-chats/rooms/" + roomUuid;
            messagingTemplate.convertAndSend(destination, response);

            // 상대방에게 알림 예약
            DirectChatRoom room = message.getRoom();
            User recipient = room.getOtherUser(currentUser.getId());
            if (recipient != null) {
                notificationService.scheduleMessageNotification(room, message, recipient);
            }

            log.info("WebSocket 메시지 전송 완료: messageUuid={}", message.getUuid());

        } catch (BusinessException e) {
            // 비즈니스 예외는 사용자에게 전달
            log.warn("WebSocket 메시지 전송 실패 (비즈니스 오류): roomUuid={}, error={}",
                    roomUuid, e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/errors",
                    e.getMessage()
            );
        } catch (Exception e) {
            // 시스템 예외는 일반 메시지로 변환 (내부 오류 숨김)
            log.error("WebSocket 메시지 전송 실패: roomUuid={}", roomUuid, e);
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/errors",
                    "메시지 전송에 실패했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    /**
     * 타이핑 인디케이터
     */
    @MessageMapping("/direct-chats/rooms/{roomUuid}/typing")
    public void typing(
            @DestinationVariable UUID roomUuid,
            TypingIndicator indicator,
            Principal principal) {

        String userEmail = principal.getName();

        try {
            User currentUser = getCurrentUser(userEmail);

            // 타이핑 인디케이터 브로드캐스트 (User에는 nickname이 없으므로 email에서 추출)
            String email = currentUser.getEmail();
            String displayName = email != null && email.contains("@")
                    ? email.substring(0, email.indexOf("@")) : email;

            TypingIndicator response = TypingIndicator.builder()
                    .roomUuid(roomUuid)
                    .userUuid(currentUser.getUuid())
                    .nickname(displayName)
                    .isTyping(indicator.getIsTyping())
                    .build();

            String destination = "/topic/direct-chats/rooms/" + roomUuid + "/typing";
            messagingTemplate.convertAndSend(destination, response);

        } catch (Exception e) {
            log.warn("타이핑 인디케이터 전송 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
        }
    }

    /**
     * 읽음 처리
     */
    @MessageMapping("/direct-chats/rooms/{roomUuid}/read")
    @SendToUser("/queue/reply")
    public String markAsRead(
            @DestinationVariable UUID roomUuid,
            Principal principal) {

        String userEmail = principal.getName();
        log.info("WebSocket 읽음 처리: roomUuid={}, userEmail={}", roomUuid, userEmail);

        try {
            User currentUser = getCurrentUser(userEmail);
            DirectChatRoom room = chatService.getRoom(roomUuid, userEmail);

            // 읽음 처리
            chatService.markAsRead(roomUuid, userEmail);

            // 대기 중인 알림 취소
            notificationService.cancelPendingNotifications(room.getId(), currentUser.getId());

            log.info("WebSocket 읽음 처리 완료: roomUuid={}", roomUuid);
            return "SUCCESS";

        } catch (Exception e) {
            log.error("WebSocket 읽음 처리 실패: roomUuid={}, userEmail={}, error={}",
                    roomUuid, userEmail, e.getMessage(), e);
            return "FAILED: " + e.getMessage();
        }
    }

    /**
     * 채팅방 입장 (Presence 업데이트)
     */
    @MessageMapping("/direct-chats/rooms/{roomUuid}/enter")
    @SendToUser("/queue/reply")
    public String enterRoom(
            @DestinationVariable UUID roomUuid,
            Principal principal) {

        String userEmail = principal.getName();

        try {
            User currentUser = getCurrentUser(userEmail);
            DirectChatRoom room = chatService.getRoom(roomUuid, userEmail);

            // Presence 업데이트
            presenceService.setOnline(currentUser.getId());
            presenceService.enterRoom(currentUser.getId(), roomUuid);

            // 읽음 처리
            chatService.markAsRead(roomUuid, userEmail);

            // 대기 중인 알림 취소
            notificationService.cancelPendingNotifications(room.getId(), currentUser.getId());

            log.debug("채팅방 입장: roomUuid={}, userId={}", roomUuid, currentUser.getId());
            return "ENTERED";

        } catch (Exception e) {
            log.warn("채팅방 입장 처리 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
            return "FAILED: " + e.getMessage();
        }
    }

    /**
     * 채팅방 퇴장 (Presence 업데이트)
     */
    @MessageMapping("/direct-chats/rooms/{roomUuid}/leave")
    @SendToUser("/queue/reply")
    public String leaveRoom(
            @DestinationVariable UUID roomUuid,
            Principal principal) {

        String userEmail = principal.getName();

        try {
            User currentUser = getCurrentUser(userEmail);

            // Presence 업데이트
            presenceService.leaveRoom(currentUser.getId(), roomUuid);

            log.debug("채팅방 퇴장: roomUuid={}, userId={}", roomUuid, currentUser.getId());
            return "LEFT";

        } catch (Exception e) {
            log.warn("채팅방 퇴장 처리 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
            return "FAILED: " + e.getMessage();
        }
    }

    /**
     * Heartbeat (온라인 상태 유지)
     */
    @MessageMapping("/direct-chats/heartbeat")
    public void heartbeat(Principal principal) {
        String userEmail = principal.getName();

        try {
            User currentUser = getCurrentUser(userEmail);
            presenceService.heartbeat(currentUser.getId());
        } catch (Exception e) {
            log.warn("Heartbeat 실패: userEmail={}, error={}", userEmail, e.getMessage());
        }
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
