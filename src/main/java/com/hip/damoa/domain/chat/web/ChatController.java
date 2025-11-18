package com.hip.damoa.domain.chat.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.chat.model.ChatMessage;
import com.hip.damoa.domain.chat.model.ChatRoom;
import com.hip.damoa.domain.chat.service.ChatService;
import com.hip.damoa.domain.chat.web.dto.ChatMessageResponse;
import com.hip.damoa.domain.chat.web.dto.ChatRoomResponse;
import com.hip.damoa.domain.chat.web.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 채팅 REST API 컨트롤러
 *
 * 견적 요청자와 업체 간의 1:1 채팅 REST API
 * - 채팅방 조회/생성
 * - 메시지 전송
 * - 메시지 읽음 처리
 */
@Tag(name = "Chat", description = "채팅 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
//@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅방 목록 조회
     *
     * @param userDetails 인증된 사용자
     * @return 채팅방 목록
     */
    @Operation(
            summary = "채팅방 목록 조회",
            description = "현재 사용자의 모든 채팅방을 조회합니다 (사용자 및 업체 소유 채팅방 모두 포함)"
    )
    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomResponse>> getChatRooms(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("채팅방 목록 조회 API: userEmail={}", userEmail);

        List<ChatRoom> chatRooms = chatService.getChatRooms(userEmail);
        List<ChatRoomResponse> responses = chatRooms.stream()
                .map(ChatRoomResponse::from)
                .toList();

        return ApiResponse.success(responses);
    }

    /**
     * 채팅방 조회/생성
     *
     * @param estimateRequestUuid 견적 요청 UUID
     * @param companyUuid 업체 UUID
     * @param userDetails 인증된 사용자
     * @return 채팅방
     */
    @Operation(
            summary = "채팅방 조회/생성",
            description = "견적 요청 UUID와 업체 UUID로 채팅방을 조회하거나 생성합니다"
    )
    @GetMapping("/rooms/by-request/{estimateRequestUuid}/company/{companyUuid}")
    public ApiResponse<ChatRoomResponse> getOrCreateChatRoom(
            @PathVariable UUID estimateRequestUuid,
            @PathVariable UUID companyUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("채팅방 조회/생성 API: estimateRequestUuid={}, companyUuid={}, userEmail={}",
                estimateRequestUuid, companyUuid, userEmail);

        ChatRoom chatRoom = chatService.getOrCreateChatRoom(estimateRequestUuid, companyUuid, userEmail);
        ChatRoomResponse response = ChatRoomResponse.from(chatRoom);

        return ApiResponse.success(response);
    }

    /**
     * 채팅방 상세 조회
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param userDetails 인증된 사용자
     * @return 채팅방 상세 정보
     */
    @Operation(
            summary = "채팅방 상세 조회",
            description = "특정 채팅방의 상세 정보를 조회합니다"
    )
    @GetMapping("/rooms/{chatRoomUuid}")
    public ApiResponse<ChatRoomResponse> getChatRoom(
            @PathVariable UUID chatRoomUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("채팅방 상세 조회 API: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        ChatRoom chatRoom = chatService.getChatRoom(chatRoomUuid, userEmail);
        ChatRoomResponse response = ChatRoomResponse.from(chatRoom);

        return ApiResponse.success(response);
    }

    /**
     * 메시지 목록 조회
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param pageable 페이징 정보
     * @param userDetails 인증된 사용자
     * @return 메시지 목록
     */
    @Operation(
            summary = "메시지 목록 조회",
            description = "채팅방의 메시지 목록을 조회합니다 (페이징, 최신순)"
    )
    @GetMapping("/rooms/{chatRoomUuid}/messages")
    public ApiResponse<Page<ChatMessageResponse>> getMessages(
            @PathVariable UUID chatRoomUuid,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("메시지 목록 조회 API: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        Page<ChatMessage> messages = chatService.getMessages(chatRoomUuid, userEmail, pageable);
        Page<ChatMessageResponse> responses = messages.map(ChatMessageResponse::from);

        return ApiResponse.success(responses);
    }

    /**
     * 메시지 전송
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param request 메시지 전송 요청
     * @param userDetails 인증된 사용자
     * @return 전송된 메시지
     */
    @Operation(
            summary = "메시지 전송",
            description = "채팅방에 메시지를 전송합니다"
    )
    @PostMapping("/rooms/{chatRoomUuid}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable UUID chatRoomUuid,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("메시지 전송 API: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        ChatMessage chatMessage = chatService.sendMessage(chatRoomUuid, userEmail, request.getMessage());
        ChatMessageResponse response = ChatMessageResponse.from(chatMessage);

        return ApiResponse.success(response);
    }

    /**
     * 메시지 읽음 처리
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param userDetails 인증된 사용자
     * @return 성공 응답
     */
    @Operation(
            summary = "메시지 읽음 처리",
            description = "채팅방의 미읽은 메시지를 모두 읽음 처리합니다"
    )
    @PatchMapping("/rooms/{chatRoomUuid}/read")
    public ApiResponse<Void> markMessagesAsRead(
            @PathVariable UUID chatRoomUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("메시지 읽음 처리 API: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        chatService.markMessagesAsRead(chatRoomUuid, userEmail);

        return ApiResponse.success();
    }
}
