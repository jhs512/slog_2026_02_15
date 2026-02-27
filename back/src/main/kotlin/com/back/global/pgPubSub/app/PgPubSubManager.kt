package com.back.global.pgPubSub.app

import com.back.global.pgPubSub.annotation.PgSubscribe
import com.zaxxer.hikari.HikariDataSource
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.sql.DriverManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

@Component
class PgPubSubManager(
    private val dataSource: DataSource,
    private val applicationContext: ApplicationContext,
    private val objectMapper: ObjectMapper,
    private val pgPubSub: PgPubSub,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // channel → (bean, method) 목록
    internal data class HandlerEntry(val bean: Any, val method: java.lang.reflect.Method)
    internal val handlers = mutableMapOf<String, MutableList<HandlerEntry>>()

    private val started = AtomicBoolean(false)
    internal val listenReady = CompletableFuture<Unit>()

    @EventListener(ContextRefreshedEvent::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scan()
        if (handlers.isEmpty()) return
        Thread.ofVirtual().name("pg-pubsub-listener").start { listenLoop() }
    }

    private fun scan() {
        applicationContext.beanDefinitionNames.forEach { beanName ->
            val bean = runCatching { applicationContext.getBean(beanName) }.getOrNull() ?: return@forEach
            bean.javaClass.methods.forEach { method ->
                val anno = method.getAnnotation(PgSubscribe::class.java) ?: return@forEach

                require(method.parameterCount == 1) {
                    "@PgSubscribe 메서드는 파라미터가 정확히 하나여야 합니다: ${method.name}"
                }

                handlers.getOrPut(anno.channel) { mutableListOf() }.add(HandlerEntry(bean, method))

                log.info("PgPubSub 구독 등록: channel='{}' → {}.{}()", anno.channel, bean.javaClass.simpleName, method.name)
            }
        }
    }

    private fun listenLoop() {
        val hikari = dataSource as HikariDataSource
        while (true) {
            try {
                // HikariCP 풀을 우회해 직접 커넥션 생성 (LISTEN 전용 커넥션은 풀 관리 대상 제외)
                DriverManager.getConnection(hikari.jdbcUrl, hikari.username, hikari.password).use { conn ->
                    conn.autoCommit = true

                    val pgConn = conn.unwrap(PGConnection::class.java)

                    handlers.keys.forEach { channel ->
                        conn.createStatement().use { it.execute("LISTEN \"$channel\"") }
                        log.info("PgPubSub LISTEN 등록: channel='{}'", channel)
                    }

                    listenReady.complete(Unit)
                    pgPubSub.fireOnConnect()

                    while (true) {
                        val notifications = pgConn.getNotifications(500) ?: continue

                        notifications.forEach { notification ->
                            dispatch(notification.name, notification.parameter.toInt())
                        }
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                log.info("PgPubSub 리스너 종료")

                return
            } catch (e: Exception) {
                log.warn("PgPubSub 커넥션 오류, 3초 후 재연결: {}", e.message)
                Thread.sleep(3_000)
            }
        }
    }

    private fun dispatch(channel: String, payloadId: Int) {
        handlers[channel]?.forEach { entry ->
            Thread.ofVirtual().name("pg-pubsub-handler").start {
                runCatching {
                    entry.method.invoke(entry.bean, payloadId)
                }.onFailure { e ->
                    log.error("PgPubSub 핸들러 오류: channel='{}' method='{}'", channel, entry.method.name, e)
                }
            }
        }
    }
}
