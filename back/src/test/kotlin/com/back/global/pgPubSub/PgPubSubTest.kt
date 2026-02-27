package com.back.global.pgPubSub

import com.back.global.pgPubSub.annotation.PgSubscribe
import com.back.global.pgPubSub.app.PgPubSub
import com.back.global.pgPubSub.app.PgPubSubManager
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
        @Bean fun intCapture() = IntCapture()
    }

    class IntCapture {
        val queue = LinkedBlockingQueue<Int>()

        @PgSubscribe("pgpubsub-int-test")
        fun handle(payload: Int) = queue.put(payload)
    }

    @Autowired lateinit var pgPubSub: PgPubSub
    @Autowired lateinit var pgPubSubManager: PgPubSubManager
    @Autowired lateinit var intCapture: IntCapture
    @Autowired lateinit var dataSource: javax.sql.DataSource

    @BeforeEach
    fun waitForListener() {
        pgPubSubManager.listenReady.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `리스너 상태 진단`() {
        println("[DIAG] handlers.keys = ${pgPubSubManager.handlers.keys}")
        println("[DIAG] listenReady.isDone = ${pgPubSubManager.listenReady.isDone}")

        assertThat(pgPubSubManager.handlers.keys)
            .describedAs("채널이 등록되어 있어야 함")
            .contains("pgpubsub-int-test")
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
    fun `Int 페이로드를 발행하면 구독자가 동일한 값을 수신한다`() {
        pgPubSub.publish("pgpubsub-int-test", 42)

        assertThat(intCapture.queue.poll(5, TimeUnit.SECONDS))
            .isEqualTo(42)
    }
}
