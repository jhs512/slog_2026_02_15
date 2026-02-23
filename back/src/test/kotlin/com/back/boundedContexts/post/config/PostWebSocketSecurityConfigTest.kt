package com.back.boundedContexts.post.config

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.app.PostFacade
import com.back.standard.extensions.getOrThrow
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostWebSocketSecurityConfigTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var actorFacade: ActorFacade

    @Autowired
    private lateinit var postFacade: PostFacade

    // 기초 데이터: user1(작성자), user2(다른 사용자), post 1(공개글), post 4(비공개글)
    private val author by lazy { actorFacade.findByUsername("user1").getOrThrow() }
    private val other by lazy { actorFacade.findByUsername("user2").getOrThrow() }
    private val publicPost by lazy { postFacade.findById(1)!! }
    private val privatePost by lazy { postFacade.findById(4)!! }

    private fun connect(member: Member): Pair<StompSession, CompletableFuture<Boolean>> {
        val errorFuture = CompletableFuture<Boolean>()

        val stompClient = WebSocketStompClient(
            SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))
        ).apply { messageConverter = JacksonJsonMessageConverter() }

        val headers = WebSocketHttpHeaders()
        headers.add("Cookie", "apiKey=${member.apiKey}")

        val session = stompClient.connectAsync(
            "http://localhost:$port/ws",
            headers,
            object : StompSessionHandlerAdapter() {
                override fun handleTransportError(session: StompSession, exception: Throwable) {
                    errorFuture.complete(true)
                }
            }
        ).get(10, TimeUnit.SECONDS)

        return session to errorFuture
    }

    private val noopHandler = object : StompFrameHandler {
        override fun getPayloadType(headers: StompHeaders) = Any::class.java
        override fun handleFrame(headers: StompHeaders, payload: Any?) {}
    }

    @Nested
    inner class Subscribe {

        @Test
        fun `성공 - 공개글은 권한 없는 사용자도 구독할 수 있다`() {
            val (session, errorFuture) = connect(other)

            session.subscribe("/topic/posts/${publicPost.id}/modified", noopHandler)

            Thread.sleep(500)
            Assertions.assertThat(errorFuture.isDone).isFalse()
            Assertions.assertThat(session.isConnected).isTrue()
            session.disconnect()
        }

        @Test
        fun `성공 - 비밀글은 작성자가 구독할 수 있다`() {
            val (session, errorFuture) = connect(author)

            session.subscribe("/topic/posts/${privatePost.id}/modified", noopHandler)

            Thread.sleep(500)
            Assertions.assertThat(errorFuture.isDone).isFalse()
            Assertions.assertThat(session.isConnected).isTrue()
            session.disconnect()
        }

        @Test
        fun `실패 - 비밀글은 권한 없는 사용자가 구독하면 에러를 받는다`() {
            val (session, errorFuture) = connect(other)

            session.subscribe("/topic/posts/${privatePost.id}/modified", noopHandler)

            Assertions.assertThat(errorFuture.get(5, TimeUnit.SECONDS)).isTrue()
        }
    }
}