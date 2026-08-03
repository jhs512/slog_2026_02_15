package com.back.global.websocket.config

import com.back.global.app.app.AppFacade
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
@DependsOn("appFacade")
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // 클라이언트가 구독할 prefix
        registry.enableSimpleBroker("/topic")
        // 클라이언트가 서버로 메시지 보낼 때 prefix
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // 네이티브 WebSocket만 지원한다. SockJS(xhr-streaming/xhr-polling) fallback은 쓰지 않는다.
        registry.addEndpoint("/ws")
            .setAllowedOrigins(AppFacade.siteFrontUrl)
    }
}
