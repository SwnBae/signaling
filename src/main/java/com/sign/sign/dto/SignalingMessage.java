// src/main/java/com/sign/sign/dto/signaling/SignalingMessage.java
package com.sign.sign.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Full ICE 방식 1:1 화상통화 시그널링 메시지
 * 모든 시그널링 통신을 하나의 DTO로 처리
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {

    /**
     * 메시지 타입
     * - "offer": WebRTC Offer SDP (모든 ICE 후보 포함)
     * - "answer": WebRTC Answer SDP (모든 ICE 후보 포함)  
     * - "connected": P2P 연결 성공 알림
     * - "disconnected": P2P 연결 해제 알림
     * - "connection-failed": P2P 연결 실패 알림
     * - "leave": 통화 종료 요청
     */
    private String type;

    /**
     * 방 ID (UUID 6자리)
     */
    private String roomId;

    /**
     * 보내는 사용자 ID
     */
    private Long fromId;

    /**
     * 받는 사용자 ID 
     * - offer/answer: 필수 (상대방 ID)
     * - connected/disconnected/connection-failed/leave: 선택 (null 가능)
     */
    private Long toId;

    /**
     * SDP 데이터 (Full ICE 방식 - 모든 ICE 후보 포함)
     * offer/answer 타입에서만 사용, 나머지는 null
     */
    private String sdp;
}