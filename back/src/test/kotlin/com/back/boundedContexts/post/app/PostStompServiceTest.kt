package com.back.boundedContexts.post.app

import com.back.global.pgPubSub.app.PgPubSubManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostStompServiceTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var postStompService: PostStompService

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var pgPubSubManager: PgPubSubManager

    // PG LISTEN 루프가 준비되기 전에 publish 하면 알림이 유실됨
    @BeforeEach
    fun waitForListener() {
        pgPubSubManager.listenReady.get(5, TimeUnit.SECONDS)
    }

    @Test
    @Transactional // post.author 등 lazy 필드 접근을 위해 세션 유지
    fun `notifyPostModified 를 호출하면 구독 중인 클라이언트가 해당 글의 데이터를 수신한다`() {
        val post = postFacade.findById(1)!!
        val received = LinkedBlockingQueue<Map<*, *>>()

        val stompClient = WebSocketStompClient(
            SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))
        ).apply { messageConverter = JacksonJsonMessageConverter() }

        val session = stompClient.connectAsync(
            "http://localhost:$port/ws",
            object : StompSessionHandlerAdapter() {}
        ).get(10, TimeUnit.SECONDS)

        session.subscribe("/topic/posts/${post.id}/modified", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders) = Map::class.java

            @Suppress("UNCHECKED_CAST")
            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                (payload as? Map<*, *>)?.let { received.put(it) }
            }
        })

        Thread.sleep(200) // 구독 확립 대기

        postStompService.notifyPostModified(post)

        // TX 커밋 → afterCommit → pg_notify 발동 (poll 전에 커밋해야 STOMP 메시지가 도착)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        val message = received.poll(5, TimeUnit.SECONDS)
        assertThat(message)
            .describedAs("5초 내에 메시지를 수신해야 함 (PG listen → dispatch → STOMP 전달)")
            .isNotNull()
        assertThat(message!!["id"]).isEqualTo(post.id)
        assertThat(message["title"]).isEqualTo(post.title)

        session.disconnect()
    }
}
