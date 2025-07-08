package com.sign.sign.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 경로 설정
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 메시지를 보낼 수 있는 경로 설정
        config.setApplicationDestinationPrefixes("/app");

        // 사용자별 개인 메시지 경로 설정
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 시그널링 서버 연결 엔드포인트
        registry.addEndpoint("/ws-signaling")
                .setAllowedOriginPatterns("*")  // CORS 설정 (개발용)
                .withSockJS();  // SockJS fallback 지원
        
        // 채팅 서버 연결 포인트
        
        // 알림 서버 연결 포인트
    }
}