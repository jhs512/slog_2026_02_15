package com.back.global.sse.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SseServiceTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var sseService: SseService

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

        // connect 이벤트 수신 대기
        val connectData = received.poll(5, TimeUnit.SECONDS)
        assertThat(connectData).describedAs("connect 이벤트를 수신해야 함").isNotNull()

        return received
    }

    @Test
    fun `send 를 호출하면 해당 채널을 구독 중인 클라이언트가 데이터를 수신한다`() {
        val received = subscribeSse("test-channel")

        sseService.send("test-channel", mapOf("hello" to "world"))

        val data = received.poll(5, TimeUnit.SECONDS)
        assertThat(data)
            .describedAs("5초 내에 SSE 메시지를 수신해야 함 (Redis publish → dispatch → SSE 전달)")
            .isNotNull()
        assertThat(data).contains("world")
    }

    @Test
    fun `다른 채널의 발행은 수신되지 않는다`() {
        val received = subscribeSse("channel-a")

        sseService.send("channel-b", mapOf("secret" to "b-only"))

        val data = received.poll(2, TimeUnit.SECONDS)
        assertThat(data).describedAs("다른 채널 데이터는 오지 않아야 함").isNull()
    }
}
