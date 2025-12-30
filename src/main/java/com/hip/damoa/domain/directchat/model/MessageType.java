package com.hip.damoa.domain.directchat.model;

/**
 * 채팅 메시지 타입
 */
public enum MessageType {
    /** 일반 텍스트 메시지 */
    TEXT,
    /** 이미지 첨부 */
    IMAGE,
    /** 파일 첨부 */
    FILE,
    /** 시스템 메시지 (입장, 퇴장 등) */
    SYSTEM
}
