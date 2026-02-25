package com.back.global.pgpubsub.config

import com.back.global.pgpubsub.annotation.PgSubscribe
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import javax.sql.DataSource

@Configuration
class PgPubSubConfig(
    private val dataSource: DataSource,
    private val applicationContext: ApplicationContext,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // channel → (bean, method) 목록
    private data class HandlerEntry(val bean: Any, val method: java.lang.reflect.Method)
    private val handlers = mutableMapOf<String, MutableList<HandlerEntry>>()

    @EventListener(ContextRefreshedEvent::class)
    fun start() {
        scan()
        if (handlers.isEmpty()) return
        Thread.ofVirtual().name("pg-pubsub-listener").start { listenLoop() }
    }

    private fun scan() {
        applicationContext.beanDefinitionNames.forEach { beanName ->
            val bean = runCatching { applicationContext.getBean(beanName) }.getOrNull() ?: return@forEach
            bean.javaClass.methods.forEach { method ->
                val anno = method.getAnnotation(PgSubscribe::class.java) ?: return@forEach
                require(method.parameterCount == 1 && method.parameterTypes[0] == String::class.java) {
                    "@PgSubscribe 메서드는 String 파라미터 하나만 가져야 합니다: ${method.name}"
                }
                handlers.getOrPut(anno.channel) { mutableListOf() }.add(HandlerEntry(bean, method))
                log.info("PgPubSub 구독 등록: channel='{}' → {}.{}()", anno.channel, bean.javaClass.simpleName, method.name)
            }
        }
    }

    private fun listenLoop() {
        while (true) {
            try {
                dataSource.connection.use { conn ->
                    val pgConn = conn.unwrap(PGConnection::class.java)
                    handlers.keys.forEach { channel ->
                        conn.createStatement().use { it.execute("LISTEN \"$channel\"") }
                        log.info("PgPubSub LISTEN 등록: channel='{}'", channel)
                    }
                    while (true) {
                        val notifications = pgConn.getNotifications(500) ?: continue
                        notifications.forEach { notification ->
                            dispatch(notification.name, notification.parameter)
                        }
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.info("PgPubSub 리스너 종료")
                return
            } catch (e: Exception) {
                log.warn("PgPubSub 커넥션 오류, 3초 후 재연결: {}", e.message)
                Thread.sleep(3_000)
            }
        }
    }

    private fun dispatch(channel: String, payload: String) {
        handlers[channel]?.forEach { entry ->
            runCatching {
                entry.method.invoke(entry.bean, payload)
            }.onFailure { e ->
                log.error("PgPubSub 핸들러 오류: channel='{}' method='{}'", channel, entry.method.name, e)
            }
        }
    }
}
