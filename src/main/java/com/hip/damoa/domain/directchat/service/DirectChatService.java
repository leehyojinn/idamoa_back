package com.hip.damoa.domain.directchat.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.directchat.model.*;
import com.hip.damoa.domain.directchat.repository.*;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 범용 1:1 채팅 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectChatService {

    private final DirectChatRoomRepository roomRepository;
    private final DirectChatMessageRepository messageRepository;
    private final DirectChatReadStateRepository readStateRepository;
    private final DirectChatAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    // ===== 채팅방 관리 =====

    /**
     * 채팅방 생성 또는 조회
     * 이미 두 사용자 간의 채팅방이 있으면 기존 채팅방 반환
     */
    @Transactional
    public DirectChatRoom getOrCreateRoom(String userEmail, UUID targetUserUuid) {
        log.info("채팅방 생성/조회: userEmail={}, targetUserUuid={}", userEmail, targetUserUuid);

        User currentUser = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findByUuidAndIsDeletedFalse(targetUserUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_CHAT_TARGET_USER_NOT_FOUND));

        // 자기 자신과 채팅 방지
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_SELF_CHAT_NOT_ALLOWED);
        }

        // user1_id < user2_id 규칙 적용
        Long smallerId = Math.min(currentUser.getId(), targetUser.getId());
        Long largerId = Math.max(currentUser.getId(), targetUser.getId());

        // 기존 채팅방 조회
        Optional<DirectChatRoom> existingRoom = roomRepository.findByUsers(smallerId, largerId);
        if (existingRoom.isPresent()) {
            DirectChatRoom room = existingRoom.get();
            // 나간 사용자가 다시 들어오면 재활성화
            room.reactivateRoom(currentUser.getId());
            log.info("기존 채팅방 반환: roomId={}", room.getId());
            return roomRepository.save(room);
        }

        // 새 채팅방 생성
        DirectChatRoom newRoom = DirectChatRoom.create(currentUser, targetUser);
        newRoom = roomRepository.save(newRoom);

        // 읽음 상태 초기화
        readStateRepository.save(DirectChatReadState.create(newRoom, currentUser));
        readStateRepository.save(DirectChatReadState.create(newRoom, targetUser));

        log.info("새 채팅방 생성: roomId={}", newRoom.getId());
        return newRoom;
    }

    /**
     * 채팅방 조회
     */
    @Transactional(readOnly = true)
    public DirectChatRoom getRoom(UUID roomUuid, String userEmail) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        DirectChatRoom room = roomRepository.findByUuidAndIsDeletedFalse(roomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_CHAT_ROOM_NOT_FOUND));

        // 참여자 확인
        if (!room.isParticipant(user.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_ACCESS_DENIED);
        }

        return room;
    }

    /**
     * 사용자의 채팅방 목록 조회 (참여자 정보 Fetch Join)
     */
    @Transactional(readOnly = true)
    public List<DirectChatRoom> getRooms(String userEmail) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return roomRepository.findByUserIdWithParticipants(user.getId());
    }

    /**
     * 사용자의 채팅방별 미읽음 수 일괄 조회 (N+1 방지)
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getUnreadCountsForUser(String userEmail) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Object[]> results = roomRepository.getUnreadCountsByUserId(user.getId());
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (Object[] result : results) {
            Long roomId = ((Number) result[0]).longValue();
            Long count = ((Number) result[1]).longValue();
            unreadCounts.put(roomId, count);
        }
        return unreadCounts;
    }

    /**
     * 채팅방 나가기
     */
    @Transactional
    public void leaveRoom(UUID roomUuid, String userEmail) {
        log.info("채팅방 나가기: roomUuid={}, userEmail={}", roomUuid, userEmail);

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        DirectChatRoom room = roomRepository.findByUuidAndIsDeletedFalse(roomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(user.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_ACCESS_DENIED);
        }

        room.leaveRoom(user.getId());
        roomRepository.save(room);

        log.info("채팅방 나가기 완료: roomId={}", room.getId());
    }

    // ===== 메시지 관리 =====

    /**
     * 메시지 전송
     */
    @Transactional
    public DirectChatMessage sendMessage(UUID roomUuid, String userEmail, String content,
                                         MessageType messageType, List<UUID> fileUuids) {
        log.info("메시지 전송: roomUuid={}, userEmail={}, messageType={}", roomUuid, userEmail, messageType);

        User sender = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        DirectChatRoom room = roomRepository.findByUuidAndIsDeletedFalse(roomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_CHAT_ROOM_NOT_FOUND));

        // 참여자 확인
        if (!room.isParticipant(sender.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_ACCESS_DENIED);
        }

        // 채팅방 활성 상태 확인
        if (!room.isActiveForUser(sender.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_ROOM_INACTIVE);
        }

        // 메시지 내용 검증
        if ((content == null || content.trim().isEmpty()) &&
            (fileUuids == null || fileUuids.isEmpty())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_MESSAGE_EMPTY);
        }

        // 메시지 생성
        DirectChatMessage message;
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }

        switch (messageType) {
            case IMAGE:
                message = DirectChatMessage.createImageMessage(room, sender, content);
                break;
            case FILE:
                message = DirectChatMessage.createFileMessage(room, sender, content);
                break;
            case SYSTEM:
                message = DirectChatMessage.createSystemMessage(room, content);
                break;
            default:
                message = DirectChatMessage.createTextMessage(room, sender, content != null ? content : "");
        }

        message = messageRepository.save(message);

        // 첨부파일 처리
        if (fileUuids != null && !fileUuids.isEmpty()) {
            for (UUID fileUuid : fileUuids) {
                File file = fileRepository.findByUuidAndIsDeletedFalse(fileUuid).orElse(null);
                if (file != null) {
                    // 파일 엔티티 정보 업데이트 (orphan 정리 시 제외되도록)
                    file.updateEntityInfo("CHAT_MESSAGE_ATTACHMENT", message.getId());
                    fileRepository.save(file);

                    // 연결 테이블에도 저장 (Fetch Join 최적화용)
                    DirectChatAttachment attachment = DirectChatAttachment.create(message, file);
                    attachmentRepository.save(attachment);
                    message.addAttachment(attachment);
                }
            }
        }

        // 채팅방 마지막 메시지 업데이트
        room.updateLastMessage(content != null ? content : "[파일]", sender);

        // 상대방 채팅방 재활성화 (나간 상태에서 메시지 받으면 다시 보이도록)
        User otherUser = room.getOtherUser(sender.getId());
        if (otherUser != null) {
            room.reactivateRoom(otherUser.getId());
        }

        roomRepository.save(room);

        log.info("메시지 전송 완료: messageId={}", message.getId());
        return message;
    }

    /**
     * 메시지 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<DirectChatMessage> getMessages(UUID roomUuid, String userEmail, Pageable pageable) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        DirectChatRoom room = roomRepository.findByUuidAndIsDeletedFalse(roomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(user.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_ACCESS_DENIED);
        }

        return messageRepository.findByRoomIdWithSender(room.getId(), pageable);
    }

    // ===== 읽음 상태 관리 =====

    /**
     * 읽음 처리
     */
    @Transactional
    public void markAsRead(UUID roomUuid, String userEmail) {
        log.debug("읽음 처리: roomUuid={}, userEmail={}", roomUuid, userEmail);

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        DirectChatRoom room = roomRepository.findByUuidAndIsDeletedFalse(roomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(user.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_ACCESS_DENIED);
        }

        // 마지막 메시지 조회
        Optional<DirectChatMessage> lastMessage = messageRepository.findFirstByRoomIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(room.getId());
        if (lastMessage.isEmpty()) {
            return; // 메시지가 없으면 처리 불필요
        }

        // 읽음 상태 업데이트
        DirectChatReadState readState = readStateRepository.findByRoomIdAndUserId(room.getId(), user.getId())
                .orElseGet(() -> DirectChatReadState.create(room, user));

        readState.markAsRead(lastMessage.get());
        readStateRepository.save(readState);
    }

    /**
     * 미읽음 메시지 수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID roomUuid, String userEmail) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        DirectChatRoom room = roomRepository.findByUuidAndIsDeletedFalse(roomUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(user.getId())) {
            throw new BusinessException(ErrorCode.DIRECT_CHAT_ACCESS_DENIED);
        }

        // 마지막으로 읽은 메시지 ID 조회
        Optional<Long> lastReadMessageId = readStateRepository.findLastReadMessageId(room.getId(), user.getId());

        if (lastReadMessageId.isEmpty()) {
            // 읽은 적이 없으면 전체 메시지 수 반환
            return messageRepository.countAllUnreadMessages(room.getId(), user.getId());
        }

        return messageRepository.countUnreadMessages(room.getId(), lastReadMessageId.get(), user.getId());
    }

    /**
     * 사용자의 전체 미읽음 메시지 수 조회 (최적화된 단일 쿼리)
     */
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(String userEmail) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return roomRepository.getTotalUnreadCountByUserId(user.getId());
    }

    // ===== 첨부파일 =====

    /**
     * 메시지의 첨부파일 조회
     */
    @Transactional(readOnly = true)
    public List<DirectChatAttachment> getAttachments(Long messageId) {
        return attachmentRepository.findByMessageIdWithFile(messageId);
    }

    /**
     * 여러 메시지의 첨부파일 일괄 조회
     */
    @Transactional(readOnly = true)
    public List<DirectChatAttachment> getAttachmentsByMessageIds(List<Long> messageIds) {
        return attachmentRepository.findByMessageIdsWithFile(messageIds);
    }
}
