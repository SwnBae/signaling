package com.sign.sign.controller;

import com.sign.sign.dto.SignalingMessage;
import com.sign.sign.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalingControllerTest {

    @InjectMocks
    private SignalingController signalingController;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RoomService roomService;

    // 테스트 데이터
    private static final String ROOM_ID = "ABC123";
    private static final Long USER_A_ID = 1L;  // 호출자
    private static final Long USER_B_ID = 2L;  // 수신자
    private static final String SAMPLE_SDP = "v=0\r\no=- 123456789 0 IN IP4 192.168.1.100\r\n...";

    @Test
    @DisplayName("A가 B에게 Offer를 전송하면 B에게만 전달되어야 한다")
    void should_relay_offer_from_A_to_B() {
        // given
        SignalingMessage offer = new SignalingMessage(
                "offer", ROOM_ID, USER_A_ID, USER_B_ID, SAMPLE_SDP
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, offer);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("2"),                    // B에게만
                eq("/queue/signaling"),     // 개인 메시지로
                eq(offer)                   // Offer 메시지 그대로
        );
    }

    @Test
    @DisplayName("B가 A에게 Answer를 전송하면 A에게만 전달되어야 한다")
    void should_relay_answer_from_B_to_A() {
        // given
        SignalingMessage answer = new SignalingMessage(
                "answer", ROOM_ID, USER_B_ID, USER_A_ID, SAMPLE_SDP
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, answer);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("1"),                    // A에게만
                eq("/queue/signaling"),     // 개인 메시지로
                eq(answer)                  // Answer 메시지 그대로
        );
    }

    @Test
    @DisplayName("A가 P2P 연결에 성공하면 방 전체에 알림을 보내야 한다")
    void should_broadcast_connection_success_from_A() {
        // given
        SignalingMessage connected = new SignalingMessage(
                "connected", ROOM_ID, USER_A_ID, null, null
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, connected);

        // then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/signaling/" + ROOM_ID),  // 방 전체에
                eq(connected)                       // 연결 성공 메시지
        );
    }

    @Test
    @DisplayName("B가 P2P 연결에 성공하면 방 전체에 알림을 보내야 한다")
    void should_broadcast_connection_success_from_B() {
        // given
        SignalingMessage connected = new SignalingMessage(
                "connected", ROOM_ID, USER_B_ID, null, null
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, connected);

        // then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/signaling/" + ROOM_ID),
                eq(connected)
        );
    }

    @Test
    @DisplayName("A가 통화를 종료하면 방이 삭제되고 B에게 알림이 가야 한다")
    void should_delete_room_and_notify_when_A_leaves() {
        // given
        SignalingMessage leave = new SignalingMessage(
                "leave", ROOM_ID, USER_A_ID, null, null
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, leave);

        // then
        // 1. 방 전체에 퇴장 알림
        verify(messagingTemplate).convertAndSend(
                eq("/topic/signaling/" + ROOM_ID),
                eq(leave)
        );

        // 2. 방 삭제
        verify(roomService).remove(ROOM_ID);
    }

    @Test
    @DisplayName("B가 통화를 종료해도 같은 동작을 해야 한다")
    void should_delete_room_and_notify_when_B_leaves() {
        // given
        SignalingMessage leave = new SignalingMessage(
                "leave", ROOM_ID, USER_B_ID, null, null
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, leave);

        // then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/signaling/" + ROOM_ID),
                eq(leave)
        );
        verify(roomService).remove(ROOM_ID);
    }

    @Test
    @DisplayName("Full ICE 시나리오: A Offer → B Answer 전체 흐름 테스트")
    void should_complete_full_ice_signaling_flow() {
        // given
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when & then
        // 1. A가 B에게 Offer 전송
        SignalingMessage offer = new SignalingMessage(
                "offer", ROOM_ID, USER_A_ID, USER_B_ID, SAMPLE_SDP
        );
        signalingController.handleSignaling(ROOM_ID, offer);

        verify(messagingTemplate).convertAndSendToUser("2", "/queue/signaling", offer);

        // 2. B가 A에게 Answer 응답
        SignalingMessage answer = new SignalingMessage(
                "answer", ROOM_ID, USER_B_ID, USER_A_ID, SAMPLE_SDP
        );
        signalingController.handleSignaling(ROOM_ID, answer);

        verify(messagingTemplate).convertAndSendToUser("1", "/queue/signaling", answer);

        // 3. A 연결 성공
        SignalingMessage aConnected = new SignalingMessage(
                "connected", ROOM_ID, USER_A_ID, null, null
        );
        signalingController.handleSignaling(ROOM_ID, aConnected);

        verify(messagingTemplate).convertAndSend("/topic/signaling/" + ROOM_ID, aConnected);

        // 4. B도 연결 성공
        SignalingMessage bConnected = new SignalingMessage(
                "connected", ROOM_ID, USER_B_ID, null, null
        );
        signalingController.handleSignaling(ROOM_ID, bConnected);

        verify(messagingTemplate).convertAndSend("/topic/signaling/" + ROOM_ID, bConnected);
    }

    @Test
    @DisplayName("존재하지 않는 방에 메시지를 보내면 에러를 발신자에게만 전송해야 한다")
    void should_send_error_to_sender_when_room_not_exists() {
        // given
        String invalidRoomId = "INVALID";
        SignalingMessage offer = new SignalingMessage(
                "offer", invalidRoomId, USER_A_ID, USER_B_ID, SAMPLE_SDP
        );
        when(roomService.findByRoomId(invalidRoomId))
                .thenThrow(new IllegalArgumentException("방을 찾을 수 없습니다"));

        // when
        signalingController.handleSignaling(invalidRoomId, offer);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("1"),                    // A에게만
                eq("/queue/errors"),        // 에러 큐로
                contains("방을 찾을 수 없습니다")  // 에러 메시지
        );

        // Offer는 전송되지 않아야 함
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(), eq("/queue/signaling"), any()
        );
    }

    @Test
    @DisplayName("잘못된 타입의 메시지는 처리되지 않아야 한다")
    void should_ignore_unknown_message_type() {
        // given
        SignalingMessage unknownMessage = new SignalingMessage(
                "unknown-type", ROOM_ID, USER_A_ID, USER_B_ID, null
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, unknownMessage);

        // then
        // 어떤 메시지도 전송되지 않아야 함
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(SignalingMessage.class));
    }

    @Test
    @DisplayName("SDP가 없는 Offer는 처리되지 않아야 한다")
    void should_not_process_offer_without_sdp() {
        // given
        SignalingMessage invalidOffer = new SignalingMessage(
                "offer", ROOM_ID, USER_A_ID, USER_B_ID, null  // SDP 없음
        );
        when(roomService.findByRoomId(ROOM_ID)).thenReturn(mock(com.sign.sign.domain.Room.class));

        // when
        signalingController.handleSignaling(ROOM_ID, invalidOffer);

        // then
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}