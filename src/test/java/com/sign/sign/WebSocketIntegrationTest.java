package com.sign.sign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sign.sign.controller.MemberController;
import com.sign.sign.domain.Member;
import com.sign.sign.dto.SignalingMessage;
import com.sign.sign.dto.request.CreateRoomRequest;
import com.sign.sign.dto.request.RegistRequest;
import com.sign.sign.dto.response.CreateRoomResponse;
import com.sign.sign.controller.RoomController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private ObjectMapper objectMapper;

    @Autowired
    private RoomController roomController;

    @Autowired
    private MemberController memberController;

    // 테스트용 공통 데이터
    private String testRoomId;
    private static final Long USER_A_ID = 1L;
    private static final Long USER_B_ID = 2L;

    @BeforeEach
    void setup() {
        // WebSocket 클라이언트 설정
        stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        objectMapper = new ObjectMapper();

        Member userA = Member.create(new RegistRequest("UserA"));
        Member userB = Member.create(new RegistRequest("UserB"));

        memberController.register(userA);
        memberController.register(userB);

        // 테스트용 방 생성
        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setCreatorId(USER_A_ID);
        createRequest.setGuestId(USER_B_ID);

        ResponseEntity<?> response = roomController.createRoom(createRequest);
        CreateRoomResponse createRoomResponse = (CreateRoomResponse) response.getBody();
        testRoomId = createRoomResponse.getRoomId();  // 모든 테스트에서 사용할 방 ID
    }

    @Test
    @DisplayName("A가 B에게 Offer를 보내면 B가 실제로 받아야 한다")
    @SuppressWarnings("deprecation")
    void should_receive_offer_from_A_to_B() throws Exception {
        System.out.println("Test started with roomId: " + testRoomId);

        // given: WebSocket 연결 (방은 이미 생성됨)
        String url = "ws://localhost:" + port + "/ws-signaling";

        StompSession userA = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);
        StompSession userB = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);

        // B가 받을 메시지를 저장할 큐
        BlockingQueue<SignalingMessage> receivedMessages = new LinkedBlockingQueue<>();

        // B가 개인 메시지 구독
        userB.subscribe("/user/queue/signaling", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SignalingMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.offer((SignalingMessage) payload);
            }
        });

        // 구독이 완료될 때까지 잠깐 대기
        Thread.sleep(1000);

        // when: A가 B에게 Offer 전송 (testRoomId 사용)
        SignalingMessage offer = new SignalingMessage(
                "offer", testRoomId, USER_A_ID, USER_B_ID, "v=0\r\no=- test-offer-sdp"
        );
        userA.send("/app/signaling/" + testRoomId, offer);
        System.out.println("Offer sent from A to B");

        // then: B가 실제로 Offer를 받았는지 검증
        SignalingMessage received = receivedMessages.poll(5, TimeUnit.SECONDS);
        System.out.println("Received message: " + received);
        assertThat(received).isNotNull();
        assertThat(received.getType()).isEqualTo("offer");
        assertThat(received.getRoomId()).isEqualTo(testRoomId);
        assertThat(received.getFromId()).isEqualTo(USER_A_ID);
        assertThat(received.getToId()).isEqualTo(USER_B_ID);
        assertThat(received.getSdp()).isEqualTo("v=0\r\no=- test-offer-sdp");

        // cleanup
        userA.disconnect();
        userB.disconnect();
    }

    @Test
    @DisplayName("B가 A에게 Answer를 보내면 A가 실제로 받아야 한다")
    @SuppressWarnings("deprecation")
    void should_receive_answer_from_B_to_A() throws Exception {
        // given
        String url = "ws://localhost:" + port + "/ws-signaling";

        StompSession userA = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);
        StompSession userB = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);

        BlockingQueue<SignalingMessage> receivedMessages = new LinkedBlockingQueue<>();

        // A가 개인 메시지 구독
        userA.subscribe("/user/queue/signaling", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SignalingMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.offer((SignalingMessage) payload);
            }
        });

        Thread.sleep(1000);

        // when: B가 A에게 Answer 전송 (testRoomId 사용)
        SignalingMessage answer = new SignalingMessage(
                "answer", testRoomId, USER_B_ID, USER_A_ID, "v=0\r\no=- test-answer-sdp"
        );
        userB.send("/app/signaling/" + testRoomId, answer);

        // then: A가 실제로 Answer를 받았는지 검증
        SignalingMessage received = receivedMessages.poll(5, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.getType()).isEqualTo("answer");
        assertThat(received.getRoomId()).isEqualTo(testRoomId);
        assertThat(received.getFromId()).isEqualTo(USER_B_ID);
        assertThat(received.getToId()).isEqualTo(USER_A_ID);
        assertThat(received.getSdp()).isEqualTo("v=0\r\no=- test-answer-sdp");

        userA.disconnect();
        userB.disconnect();
    }

    @Test
    @DisplayName("연결 상태 메시지는 방 전체에 브로드캐스트되어야 한다")
    @SuppressWarnings("deprecation")
    void should_broadcast_connection_status_to_room() throws Exception {
        // given
        String url = "ws://localhost:" + port + "/ws-signaling";

        StompSession userA = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);
        StompSession userB = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);

        BlockingQueue<SignalingMessage> userAReceived = new LinkedBlockingQueue<>();
        BlockingQueue<SignalingMessage> userBReceived = new LinkedBlockingQueue<>();

        // 둘 다 방 전체 메시지 구독
        String roomTopic = "/topic/signaling/" + testRoomId;

        userA.subscribe(roomTopic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SignalingMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                userAReceived.offer((SignalingMessage) payload);
            }
        });

        userB.subscribe(roomTopic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SignalingMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                userBReceived.offer((SignalingMessage) payload);
            }
        });

        Thread.sleep(1000);

        // when: A가 연결 성공 메시지 전송
        SignalingMessage connected = new SignalingMessage(
                "connected", testRoomId, USER_A_ID, null, null
        );
        userA.send("/app/signaling/" + testRoomId, connected);

        // then: A와 B 모두 연결 성공 메시지를 받아야 함
        SignalingMessage aReceived = userAReceived.poll(5, TimeUnit.SECONDS);
        SignalingMessage bReceived = userBReceived.poll(5, TimeUnit.SECONDS);

        // A도 자신의 메시지를 받음 (브로드캐스트)
        assertThat(aReceived).isNotNull();
        assertThat(aReceived.getType()).isEqualTo("connected");
        assertThat(aReceived.getFromId()).isEqualTo(USER_A_ID);

        // B도 A의 연결 성공 메시지를 받음
        assertThat(bReceived).isNotNull();
        assertThat(bReceived.getType()).isEqualTo("connected");
        assertThat(bReceived.getFromId()).isEqualTo(USER_A_ID);

        userA.disconnect();
        userB.disconnect();
    }

    @Test
    @DisplayName("전체 시그널링 흐름: A Offer → B Answer → 연결 성공")
    @SuppressWarnings("deprecation")
    void should_complete_full_signaling_flow() throws Exception {
        // given
        String url = "ws://localhost:" + port + "/ws-signaling";

        StompSession userA = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);
        StompSession userB = stompClient.connect(url, new TestStompSessionHandler()).get(10, TimeUnit.SECONDS);

        BlockingQueue<SignalingMessage> aPersonalMessages = new LinkedBlockingQueue<>();
        BlockingQueue<SignalingMessage> bPersonalMessages = new LinkedBlockingQueue<>();
        BlockingQueue<SignalingMessage> roomMessages = new LinkedBlockingQueue<>();

        // 개인 메시지 구독
        userA.subscribe("/user/queue/signaling", new TestStompFrameHandler(aPersonalMessages));
        userB.subscribe("/user/queue/signaling", new TestStompFrameHandler(bPersonalMessages));

        // 방 전체 메시지 구독 (연결 상태용)
        userA.subscribe("/topic/signaling/" + testRoomId, new TestStompFrameHandler(roomMessages));

        Thread.sleep(1000);

        // when & then: 1. A가 B에게 Offer 전송
        SignalingMessage offer = new SignalingMessage("offer", testRoomId, USER_A_ID, USER_B_ID, "offer-sdp");
        userA.send("/app/signaling/" + testRoomId, offer);

        SignalingMessage bReceivedOffer = bPersonalMessages.poll(5, TimeUnit.SECONDS);
        assertThat(bReceivedOffer).isNotNull();
        assertThat(bReceivedOffer.getType()).isEqualTo("offer");

        // 2. B가 A에게 Answer 응답
        SignalingMessage answer = new SignalingMessage("answer", testRoomId, USER_B_ID, USER_A_ID, "answer-sdp");
        userB.send("/app/signaling/" + testRoomId, answer);

        SignalingMessage aReceivedAnswer = aPersonalMessages.poll(5, TimeUnit.SECONDS);
        assertThat(aReceivedAnswer).isNotNull();
        assertThat(aReceivedAnswer.getType()).isEqualTo("answer");

        // 3. A가 연결 성공 알림
        SignalingMessage aConnected = new SignalingMessage("connected", testRoomId, USER_A_ID, null, null);
        userA.send("/app/signaling/" + testRoomId, aConnected);

        SignalingMessage roomReceivedConnected = roomMessages.poll(5, TimeUnit.SECONDS);
        assertThat(roomReceivedConnected).isNotNull();
        assertThat(roomReceivedConnected.getType()).isEqualTo("connected");
        assertThat(roomReceivedConnected.getFromId()).isEqualTo(USER_A_ID);

        userA.disconnect();
        userB.disconnect();
    }

    // Helper Classes
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            exception.printStackTrace();
        }
    }

    private static class TestStompFrameHandler implements StompFrameHandler {
        private final BlockingQueue<SignalingMessage> messageQueue;

        public TestStompFrameHandler(BlockingQueue<SignalingMessage> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return SignalingMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            messageQueue.offer((SignalingMessage) payload);
        }
    }
}