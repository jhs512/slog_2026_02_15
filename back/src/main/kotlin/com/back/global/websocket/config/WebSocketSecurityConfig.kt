package com.back.global.websocket.config

import com.back.boundedContexts.post.config.PostWebSocketSecurityConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager

@Configuration
@EnableWebSocketSecurity
class WebSocketSecurityConfig(
    private val postWebSocketSecurityConfigurer: PostWebSocketSecurityConfigurer,
) {
    // Spring Security 7이 csrfChannelInterceptor 빈이 없으면 null을 채널에 등록하는 버그 우회
    // HTTP CSRF가 비활성화되어 있으므로 WebSocket CSRF도 no-op으로 처리
    @Bean("csrfChannelInterceptor")
    fun noopCsrfChannelInterceptor(): ChannelInterceptor = object : ChannelInterceptor {}

    @Bean
    fun messageAuthorizationManager(
        messages: MessageMatcherDelegatingAuthorizationManager.Builder,
    ): AuthorizationManager<Message<*>> {
        postWebSocketSecurityConfigurer.configure(messages)
        messages.anyMessage().permitAll()
        return messages.build()
    }
}
