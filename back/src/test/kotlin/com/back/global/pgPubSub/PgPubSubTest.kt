package com.back.global.pgPubSub

import com.back.global.pgPubSub.annotation.PgSubscribe
import com.back.global.pgPubSub.app.PgPubSub
import com.back.global.pgPubSub.config.PgPubSubConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest
class PgPubSubTest {

    @TestConfiguration
    class Cfg {
        @Bean fun stringCapture() = StringCapture()
        @Bean fun dtoCapture() = DtoCapture()
    }

    class StringCapture {
        val queue = LinkedBlockingQueue<String>()

        @PgSubscribe("pgpubsub-str-test")
        fun handle(payload: String) = queue.put(payload)
    }

    data class SampleDto(val id: Int = 0, val name: String = "")

    class DtoCapture {
        val queue = LinkedBlockingQueue<SampleDto>()

        @PgSubscribe("pgpubsub-dto-test")
        fun handle(payload: SampleDto) = queue.put(payload)
    }

    @Autowired lateinit var pgPubSub: PgPubSub
    @Autowired lateinit var pgPubSubConfig: PgPubSubConfig
    @Autowired lateinit var stringCapture: StringCapture
    @Autowired lateinit var dtoCapture: DtoCapture
    @Autowired lateinit var dataSource: javax.sql.DataSource

    @BeforeEach
    fun waitForListener() {
        pgPubSubConfig.listenReady.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `리스너 상태 진단`() {
        println("[DIAG] handlers.keys = ${pgPubSubConfig.handlers.keys}")
        println("[DIAG] listenReady.isDone = ${pgPubSubConfig.listenReady.isDone}")
        println("[DIAG] stringCapture id = ${System.identityHashCode(stringCapture)}")
        val handlerBean = pgPubSubConfig.handlers["pgpubsub-str-test"]?.firstOrNull()?.bean
        println("[DIAG] handler bean id = ${handlerBean?.let { System.identityHashCode(it) }}")
        println("[DIAG] same instance = ${handlerBean === stringCapture}")

        assertThat(pgPubSubConfig.handlers.keys)
            .describedAs("채널이 등록되어 있어야 함")
            .contains("pgpubsub-str-test", "pgpubsub-dto-test")
    }

    @Test
    fun `raw JDBC LISTEN-NOTIFY 동작 확인`() {
        val hikari = dataSource as com.zaxxer.hikari.HikariDataSource
        val listenConn = java.sql.DriverManager.getConnection(hikari.jdbcUrl, hikari.username, hikari.password)
        listenConn.autoCommit = true
        val pgListenConn = listenConn.unwrap(org.postgresql.PGConnection::class.java)
        listenConn.createStatement().use { it.execute("LISTEN raw_test_channel") }

        val publishConn = java.sql.DriverManager.getConnection(hikari.jdbcUrl, hikari.username, hikari.password)
        publishConn.autoCommit = true
        publishConn.prepareStatement("SELECT pg_notify(?, ?)").use { stmt ->
            stmt.setString(1, "raw_test_channel")
            stmt.setString(2, "raw_payload")
            stmt.execute()
        }
        publishConn.close()

        val notifications = pgListenConn.getNotifications(5000)
        println("[DIAG] raw notifications = ${notifications?.map { "${it.name}:${it.parameter}" }}")
        listenConn.close()

        assertThat(notifications).isNotNull()
        assertThat(notifications!!.first().parameter).isEqualTo("raw_payload")
    }

    @Test
    fun `문자열 페이로드를 발행하면 구독자가 동일한 문자열을 수신한다`() {
        pgPubSub.publish("pgpubsub-str-test", "hello world")

        assertThat(stringCapture.queue.poll(5, TimeUnit.SECONDS))
            .isEqualTo("hello world")
    }

    @Test
    fun `객체를 발행하면 JSON 직렬화 후 전송되고 구독자에서 역직렬화하여 수신한다`() {
        val dto = SampleDto(id = 42, name = "테스트")
        pgPubSub.publish("pgpubsub-dto-test", dto)

        assertThat(dtoCapture.queue.poll(5, TimeUnit.SECONDS))
            .isEqualTo(dto)
    }
}
