package com.back.global.websocket.config

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.postExtensions.canRead
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager

@Configuration
@EnableWebSocketSecurity
class WebSocketSecurityConfig(
    @Lazy private val postFacade: PostFacade,
    @Lazy private val memberFacade: MemberFacade,
) {
    private val postTopicPattern = Regex("^/topic/posts/(\\d+)/modified$")

    // Spring Security 7이 csrfChannelInterceptor 빈이 없으면 null을 채널에 등록하는 버그 우회
    // HTTP CSRF가 비활성화되어 있으므로 WebSocket CSRF도 no-op으로 처리
    @Bean("csrfChannelInterceptor")
    fun noopCsrfChannelInterceptor(): ChannelInterceptor = object : ChannelInterceptor {}

    @Bean
    fun messageAuthorizationManager(
        messages: MessageMatcherDelegatingAuthorizationManager.Builder,
    ): AuthorizationManager<Message<*>> {
        messages
            .simpSubscribeDestMatchers("/topic/posts/*/modified")
            .access { authentication, context ->
                val destination = SimpMessageHeaderAccessor.getDestination(context.message.headers)
                    ?: return@access AuthorizationDecision(false)

                val postId = postTopicPattern.find(destination)
                    ?.groupValues?.get(1)?.toIntOrNull()
                    ?: return@access AuthorizationDecision(true)

                val post = postFacade.findById(postId)
                    ?: return@access AuthorizationDecision(true)

                if (post.published) return@access AuthorizationDecision(true)

                val member = authentication.get().name
                    .let { memberFacade.findByUsername(it) }
                AuthorizationDecision(post.canRead(member))
            }
            .anyMessage().permitAll()

        return messages.build()
    }
}
