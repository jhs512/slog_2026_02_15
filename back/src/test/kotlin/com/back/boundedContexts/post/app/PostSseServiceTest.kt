package com.back.boundedContexts.post.app

import com.back.boundedContexts.member.app.shared.ActorFacade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PostSseServiceTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var actorFacade: ActorFacade

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private fun subscribeSse(channel: String): LinkedBlockingQueue<String> {
        val received = LinkedBlockingQueue<String>()

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI("http://localhost:$port/sse/$channel"))
            .header("Accept", "text/event-stream")
            .GET()
            .build()

        val thread = Thread {
            try {
                client.send(request, HttpResponse.BodyHandlers.ofLines()).body().forEach { line ->
                    if (line.startsWith("data:")) {
                        received.put(line.removePrefix("data:"))
                    }
                }
            } catch (_: Exception) {
            }
        }
        thread.isDaemon = true
        thread.start()

        val connectData = received.poll(5, TimeUnit.SECONDS)
        assertThat(connectData).describedAs("connect 이벤트를 수신해야 함").isNotNull()

        return received
    }

    @Test
    fun `조회수가 10을 최초 돌파하는 순간에만 posts-new 알림이 발행된다`() {
        val received = subscribeSse("posts-new")

        val author = actorFacade.findByUsername("user1")!!
        val postId = transactionTemplate.execute {
            postFacade.write(author, "SSE 알림 테스트 글", "본문", published = true, listed = true).id
        }!!

        // 1~9번째 조회: 알림 없음
        repeat(9) {
            transactionTemplate.execute {
                val post = postFacade.findById(postId)!!
                postFacade.incrementHit(post, null)
            }
        }
        assertThat(received.poll(1, TimeUnit.SECONDS))
            .describedAs("임계 도달 전에는 알림이 없어야 함")
            .isNull()

        // 10번째 조회: 알림 발행
        transactionTemplate.execute {
            val post = postFacade.findById(postId)!!
            postFacade.incrementHit(post, null)
        }
        val data = received.poll(5, TimeUnit.SECONDS)
        assertThat(data).describedAs("10번째 조회에서 알림을 수신해야 함").isNotNull()
        assertThat(data).contains(postId.toString())
        assertThat(data).contains("SSE 알림 테스트 글")

        // 11번째 조회: 재발행 없음
        transactionTemplate.execute {
            val post = postFacade.findById(postId)!!
            postFacade.incrementHit(post, null)
        }
        assertThat(received.poll(1, TimeUnit.SECONDS))
            .describedAs("임계 돌파 이후에는 재발행이 없어야 함")
            .isNull()
    }

    @Test
    fun `작성자 본인의 조회는 조회수를 올리지 않는다`() {
        val author = actorFacade.findByUsername("user1")!!

        transactionTemplate.execute {
            val post = postFacade.write(author, "본인 조회 테스트 글", "본문", published = true, listed = true)
            val incremented = postFacade.incrementHit(post, author)
            assertThat(incremented).isFalse()
        }
    }
}
