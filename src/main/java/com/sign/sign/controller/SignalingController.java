package com.sign.sign.controller;

import com.sign.sign.dto.SignalingMessage;
import com.sign.sign.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

    /**
     * 통합 시그널링 메시지 처리 (Full ICE 방식)
     *
     * 클라이언트 요청: /app/signaling/{roomId}
     *
     * 메시지 타입별 처리:
     * - offer/answer: 상대방에게 개인 전송
     * - connected/disconnected/connection-failed: 상대방에게 상태 알림
     * - leave: 통화 종료 및 방 삭제
     */
    @MessageMapping("/signaling/{roomId}")
    public void handleSignaling(@DestinationVariable String roomId, SignalingMessage message) {
        try {
            // 방 존재 여부 확인
            roomService.findByRoomId(roomId);

            log.info("Received '{}' message from user {} in room {}",
                    message.getType(), message.getFromId(), roomId);

            switch (message.getType()) {
                case "offer":
                    handleOffer(roomId, message);
                    break;

                case "answer":
                    handleAnswer(roomId, message);
                    break;

                case "connected":
                    handleConnected(roomId, message);
                    break;

                case "disconnected":
                    handleDisconnected(roomId, message);
                    break;

                case "connection-failed":
                    handleConnectionFailed(roomId, message);
                    break;

                case "leave":
                    handleLeave(roomId, message);
                    break;

                default:
                    log.warn("Unknown message type '{}' from user {} in room {}",
                            message.getType(), message.getFromId(), roomId);
            }

        } catch (IllegalArgumentException e) {
            log.error("Room '{}' not found for user {}", roomId, message.getFromId());

            // 발신자에게만 에러 알림
            messagingTemplate.convertAndSendToUser(
                    message.getFromId().toString(),
                    "/queue/errors",
                    "방을 찾을 수 없습니다: " + roomId
            );
        }
    }

    /**
     * WebRTC Offer 처리 (Full ICE - 모든 ICE 후보 포함)
     * A → B로 호출 요청 전송
     */
    private void handleOffer(String roomId, SignalingMessage message) {
        if (message.getToId() == null || message.getSdp() == null) {
            log.error("Invalid offer message: missing toUserId or SDP");
            return;
        }

        log.info("Relaying Full ICE Offer from user {} to user {} in room {}",
                message.getFromId(), message.getToId(), roomId);

        // 수신자에게만 Offer 전달
        messagingTemplate.convertAndSendToUser(
                message.getToId().toString(),
                "/queue/signaling",
                message
        );
    }

    /**
     * WebRTC Answer 처리 (Full ICE - 모든 ICE 후보 포함)
     * B → A로 응답 전송
     */
    private void handleAnswer(String roomId, SignalingMessage message) {
        if (message.getToId() == null || message.getSdp() == null) {
            log.error("Invalid answer message: missing toUserId or SDP");
            return;
        }

        log.info("Relaying Full ICE Answer from user {} to user {} in room {}",
                message.getFromId(), message.getToId(), roomId);

        // 호출자에게만 Answer 전달
        messagingTemplate.convertAndSendToUser(
                message.getToId().toString(),
                "/queue/signaling",
                message
        );
    }

    /**
     * P2P 연결 성공 처리
     * 통화 시작을 상대방에게 알림
     */
    private void handleConnected(String roomId, SignalingMessage message) {
        log.info("User {} successfully connected via P2P in room {}",
                message.getFromId(), roomId);

        // 방 전체에 연결 성공 알림 (상대방이 받음)
        messagingTemplate.convertAndSend(
                "/topic/signaling/" + roomId,
                message
        );
    }

    /**
     * P2P 연결 해제 처리
     * 네트워크 문제 등으로 연결이 끊어진 경우
     */
    private void handleDisconnected(String roomId, SignalingMessage message) {
        log.info("User {} disconnected from P2P in room {}",
                message.getFromId(), roomId);

        // 방 전체에 연결 해제 알림
        messagingTemplate.convertAndSend(
                "/topic/signaling/" + roomId,
                message
        );
    }

    /**
     * P2P 연결 실패 처리
     * ICE 연결이 실패한 경우
     */
    private void handleConnectionFailed(String roomId, SignalingMessage message) {
        log.warn("P2P connection failed for user {} in room {}",
                message.getFromId(), roomId);

        // 방 전체에 연결 실패 알림
        messagingTemplate.convertAndSend(
                "/topic/signaling/" + roomId,
                message
        );
    }

    /**
     * 통화 종료 처리
     * 사용자가 의도적으로 통화를 종료한 경우
     * 1:1 통화이므로 방 자체를 삭제
     */
    private void handleLeave(String roomId, SignalingMessage message) {
        log.info("User {} ended the call in room {}", message.getFromId(), roomId);

        // 상대방에게 통화 종료 알림
        messagingTemplate.convertAndSend(
                "/topic/signaling/" + roomId,
                message
        );

        // 1:1 화상통화이므로 한 명이 나가면 방 삭제
        try {
            roomService.remove(roomId);
            log.info("Room {} deleted after user {} left", roomId, message.getFromId());
        } catch (IllegalArgumentException e) {
            log.warn("Room {} was already deleted", roomId);
        }
    }
}