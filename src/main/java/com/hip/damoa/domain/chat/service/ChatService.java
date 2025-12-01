package com.hip.damoa.domain.chat.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.chat.model.ChatMessage;
import com.hip.damoa.domain.chat.model.ChatRoom;
import com.hip.damoa.domain.chat.model.SenderType;
import com.hip.damoa.domain.chat.repository.ChatMessageRepository;
import com.hip.damoa.domain.chat.repository.ChatRoomRepository;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 채팅 서비스
 *
 * 견적 요청자와 업체 간의 실시간 1:1 채팅
 * - 채팅방 생성/조회
 * - 메시지 전송
 * - 메시지 읽음 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EstimateRequestRepository estimateRequestRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    /**
     * 채팅방 생성 또는 조회
     *
     * @param estimateRequestUuid 견적 요청 UUID
     * @param userEmail 사용자 이메일 (USER 또는 COMPANY 소유자)
     * @return 채팅방
     */
    @Transactional
    public ChatRoom getOrCreateChatRoom(UUID estimateRequestUuid, String userEmail) {
        log.info("채팅방 조회/생성: estimateRequestUuid={}, userEmail={}", estimateRequestUuid, userEmail);

        // 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(estimateRequestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 현재 사용자 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 견적 요청 작성자인지, 업체 소유자인지 확인
        boolean isRequestOwner = estimateRequest.getUser().getId().equals(currentUser.getId());

        ChatRoom chatRoom;

        if (isRequestOwner) {
            // 견적 요청 작성자인 경우 - 업체 정보 필요 (에러, 업체 UUID가 필요함)
            throw new BusinessException(ErrorCode.CHAT_COMPANY_UUID_REQUIRED);
        } else {
            // 업체 소유자인 경우 - 해당 업체의 채팅방 조회/생성
            Company company = companyRepository.findByOwnerAndIsDeletedFalse(currentUser)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

            chatRoom = chatRoomRepository.findByEstimateRequestIdAndCompanyId(
                    estimateRequest.getId(), company.getId())
                    .orElseGet(() -> {
                        log.info("새 채팅방 생성: estimateRequestId={}, companyId={}",
                                estimateRequest.getId(), company.getId());

                        ChatRoom newChatRoom = ChatRoom.builder()
                                .estimateRequest(estimateRequest)
                                .user(estimateRequest.getUser())
                                .company(company)
                                .build();

                        return chatRoomRepository.save(newChatRoom);
                    });
        }

        log.info("채팅방 조회/생성 완료: chatRoomUuid={}", chatRoom.getUuid());
        return chatRoom;
    }

    /**
     * 채팅방 생성 또는 조회 (견적 요청 UUID + 업체 UUID로)
     *
     * @param estimateRequestUuid 견적 요청 UUID
     * @param companyUuid 업체 UUID
     * @param userEmail 사용자 이메일
     * @return 채팅방
     */
    @Transactional
    public ChatRoom getOrCreateChatRoom(UUID estimateRequestUuid, UUID companyUuid, String userEmail) {
        log.info("채팅방 조회/생성: estimateRequestUuid={}, companyUuid={}, userEmail={}",
                estimateRequestUuid, companyUuid, userEmail);

        // 견적 요청 조회
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(estimateRequestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 업체 조회
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 현재 사용자 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 접근 권한 확인 (견적 요청 작성자 또는 업체 소유자만 가능)
        boolean isRequestOwner = estimateRequest.getUser().getId().equals(currentUser.getId());
        boolean isCompanyOwner = company.getOwner().getId().equals(currentUser.getId());

        if (!isRequestOwner && !isCompanyOwner) {
            log.warn("채팅방 접근 권한 없음: userEmail={}, estimateRequestUuid={}, companyUuid={}",
                    userEmail, estimateRequestUuid, companyUuid);
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 채팅방 조회 또는 생성
        ChatRoom chatRoom = chatRoomRepository.findByEstimateRequestIdAndCompanyId(
                estimateRequest.getId(), company.getId())
                .orElseGet(() -> {
                    log.info("새 채팅방 생성: estimateRequestId={}, companyId={}",
                            estimateRequest.getId(), company.getId());

                    ChatRoom newChatRoom = ChatRoom.builder()
                            .estimateRequest(estimateRequest)
                            .user(estimateRequest.getUser())
                            .company(company)
                            .build();

                    return chatRoomRepository.save(newChatRoom);
                });

        log.info("채팅방 조회/생성 완료: chatRoomUuid={}", chatRoom.getUuid());
        return chatRoom;
    }

    /**
     * 메시지 전송
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param userEmail 발신자 이메일
     * @param message 메시지 내용
     * @return 생성된 메시지
     */
    @Transactional
    public ChatMessage sendMessage(UUID chatRoomUuid, String userEmail, String message) {
        log.info("메시지 전송: chatRoomUuid={}, userEmail={}, messageLength={}",
                chatRoomUuid, userEmail, message != null ? message.length() : 0);

        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_EMPTY);
        }

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByUuidAndIsDeletedFalse(chatRoomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 현재 사용자 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 발신자 타입 및 ID 확인
        SenderType senderType;
        Long senderId;

        if (chatRoom.getUser().getId().equals(currentUser.getId())) {
            // 견적 요청 작성자
            senderType = SenderType.USER;
            senderId = currentUser.getId();
        } else if (chatRoom.getCompany().getOwner().getId().equals(currentUser.getId())) {
            // 업체 소유자
            senderType = SenderType.COMPANY;
            senderId = chatRoom.getCompany().getId();
        } else {
            log.warn("채팅방 접근 권한 없음: chatRoomUuid={}, userEmail={}",
                    chatRoomUuid, userEmail);
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 메시지 생성
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderType(senderType)
                .senderId(senderId)
                .message(message.trim())
                .build();

        chatMessage = chatMessageRepository.save(chatMessage);

        // 채팅방 마지막 메시지 업데이트
        chatRoom.updateLastMessage(message.trim(), senderType);
        chatRoomRepository.save(chatRoom);

        log.info("메시지 전송 완료: chatMessageUuid={}", chatMessage.getUuid());
        return chatMessage;
    }

    /**
     * 메시지 목록 조회
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param userEmail 사용자 이메일
     * @param pageable 페이징 정보
     * @return 메시지 목록
     */
    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessages(UUID chatRoomUuid, String userEmail, Pageable pageable) {
        log.info("메시지 목록 조회: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByUuidAndIsDeletedFalse(chatRoomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 현재 사용자 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 접근 권한 확인
        boolean hasAccess = chatRoom.getUser().getId().equals(currentUser.getId()) ||
                chatRoom.getCompany().getOwner().getId().equals(currentUser.getId());

        if (!hasAccess) {
            log.warn("채팅방 접근 권한 없음: chatRoomUuid={}, userEmail={}",
                    chatRoomUuid, userEmail);
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoom.getId(), pageable);

        log.info("메시지 목록 조회 완료: count={}", messages.getTotalElements());
        return messages;
    }

    /**
     * 메시지 읽음 처리
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param userEmail 사용자 이메일
     */
    @Transactional
    public void markMessagesAsRead(UUID chatRoomUuid, String userEmail) {
        log.info("메시지 읽음 처리: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByUuidAndIsDeletedFalse(chatRoomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 현재 사용자 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 발신자 타입 확인 (읽는 사람과 반대)
        SenderType excludeSenderType;

        if (chatRoom.getUser().getId().equals(currentUser.getId())) {
            // USER가 읽음 -> COMPANY가 보낸 메시지 읽음 처리
            excludeSenderType = SenderType.USER;
            chatRoom.resetUserUnreadCount();
        } else if (chatRoom.getCompany().getOwner().getId().equals(currentUser.getId())) {
            // COMPANY가 읽음 -> USER가 보낸 메시지 읽음 처리
            excludeSenderType = SenderType.COMPANY;
            chatRoom.resetCompanyUnreadCount();
        } else {
            log.warn("채팅방 접근 권한 없음: chatRoomUuid={}, userEmail={}",
                    chatRoomUuid, userEmail);
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 미읽은 메시지 조회 및 읽음 처리
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findUnreadByChatRoomIdExcludingSenderType(chatRoom.getId(), excludeSenderType);

        for (ChatMessage message : unreadMessages) {
            message.markAsRead();
        }

        chatMessageRepository.saveAll(unreadMessages);
        chatRoomRepository.save(chatRoom);

        log.info("메시지 읽음 처리 완료: count={}", unreadMessages.size());
    }

    /**
     * 사용자의 채팅방 목록 조회
     *
     * @param userEmail 사용자 이메일
     * @return 채팅방 목록
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getChatRooms(String userEmail) {
        log.info("채팅방 목록 조회: userEmail={}", userEmail);

        // 현재 사용자 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 사용자 채팅방 조회
        List<ChatRoom> userChatRooms = chatRoomRepository.findByUserIdOrderByLastMessageAtDesc(currentUser.getId());

        // 업체 소유자인 경우 업체 채팅방도 조회
        Company company = companyRepository.findByOwnerAndIsDeletedFalse(currentUser).orElse(null);

        if (company != null) {
            List<ChatRoom> companyChatRooms = chatRoomRepository.findByCompanyIdOrderByLastMessageAtDesc(company.getId());
            userChatRooms.addAll(companyChatRooms);
        }

        log.info("채팅방 목록 조회 완료: count={}", userChatRooms.size());
        return userChatRooms;
    }

    /**
     * 채팅방 조회 (UUID로)
     *
     * @param chatRoomUuid 채팅방 UUID
     * @param userEmail 사용자 이메일
     * @return 채팅방
     */
    @Transactional(readOnly = true)
    public ChatRoom getChatRoom(UUID chatRoomUuid, String userEmail) {
        log.info("채팅방 조회: chatRoomUuid={}, userEmail={}", chatRoomUuid, userEmail);

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByUuidAndIsDeletedFalse(chatRoomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 현재 사용자 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 접근 권한 확인
        boolean hasAccess = chatRoom.getUser().getId().equals(currentUser.getId()) ||
                chatRoom.getCompany().getOwner().getId().equals(currentUser.getId());

        if (!hasAccess) {
            log.warn("채팅방 접근 권한 없음: chatRoomUuid={}, userEmail={}",
                    chatRoomUuid, userEmail);
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        log.info("채팅방 조회 완료: chatRoomUuid={}", chatRoomUuid);
        return chatRoom;
    }
}
