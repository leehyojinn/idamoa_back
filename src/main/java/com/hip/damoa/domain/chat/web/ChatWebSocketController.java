package com.hip.damoa.domain.chat.web;

import com.hip.damoa.domain.chat.model.ChatMessage;
import com.hip.damoa.domain.chat.service.ChatService;
import com.hip.damoa.domain.chat.web.dto.ChatMessageResponse;
import com.hip.damoa.domain.chat.web.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * 채팅 WebSocket 컨트롤러
 *
 * 실시간 채팅 메시지 전송
 * - 클라이언트 → 서버: /app/chats/rooms/{chatRoomUuid}/messages
 * - 서버 → 클라이언트: /topic/chats/rooms/{chatRoomUuid}
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 실시간 메시지 전송
     *
     * 클라이언트 → 서버: /app/chats/rooms/{chatRoomUuid}/messages
     * 서버 → 채팅방 구독자들: /topic/chats/rooms/{chatRoomUuid}
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param request 메시지 전송 요청
     * @param principal 인증된 사용자 (JWT에서 추출된 이메일)
     */
    @MessageMapping("/chats/rooms/{chatRoomUuid}/messages")
    public void sendMessage(
            @DestinationVariable UUID chatRoomUuid,
            SendMessageRequest request,
            Principal principal) {

        String userEmail = principal.getName();
        log.info("WebSocket 메시지 전송: chatRoomUuid={}, userEmail={}, messageLength={}",
                chatRoomUuid, userEmail, request.getMessage() != null ? request.getMessage().length() : 0);

        try {
            // 메시지 저장
            ChatMessage chatMessage = chatService.sendMessage(chatRoomUuid, userEmail, request.getMessage());

            // DTO 변환
            ChatMessageResponse response = ChatMessageResponse.from(chatMessage);

            // 채팅방 구독자들에게 브로드캐스트
            String destination = "/topic/chats/rooms/" + chatRoomUuid;
            messagingTemplate.convertAndSend(destination, response);

            log.info("WebSocket 메시지 전송 완료: chatMessageUuid={}, destination={}",
                    chatMessage.getUuid(), destination);

        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패: chatRoomUuid={}, userEmail={}, error={}",
                    chatRoomUuid, userEmail, e.getMessage(), e);

            // 에러를 발신자에게만 전송
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/errors",
                    "메시지 전송에 실패했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 메시지 읽음 처리
     *
     * 클라이언트 → 서버: /app/chats/rooms/{chatRoomUuid}/read
     * 서버 → 클라이언트: /user/queue/reply
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param principal 인증된 사용자
     */
    @MessageMapping("/chats/rooms/{chatRoomUuid}/read")
    @SendToUser("/queue/reply")
    public String markMessagesAsRead(
            @DestinationVariable UUID chatRoomUuid,
            Principal principal) {

        String userEmail = principal.getName();
        log.info("WebSocket 메시지 읽음 처리: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        try {
            chatService.markMessagesAsRead(chatRoomUuid, userEmail);
            log.info("WebSocket 메시지 읽음 처리 완료: chatRoomUuid={}", chatRoomUuid);
            return "SUCCESS";
        } catch (Exception e) {
            log.error("WebSocket 메시지 읽음 처리 실패: chatRoomUuid={}, userEmail={}, error={}",
                    chatRoomUuid, userEmail, e.getMessage(), e);
            return "FAILED: " + e.getMessage();
        }
    }
}
