package com.back.global.pgAdvisoryLock

import com.back.global.pgAdvisoryLock.annotation.PgAdvisoryLock
import com.back.global.pgAdvisoryLock.app.PgAdvisoryLockAcquisitionException
import com.back.global.pgAdvisoryLock.app.PgAdvisoryLockAspect
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

@ActiveProfiles("test")
@SpringBootTest
class PgAdvisoryLockTest {

    @TestConfiguration
    class Cfg {
        @Bean
        fun lockTestService(dataSource: DataSource) = LockTestService(dataSource)
    }

    open class LockTestService(private val dataSource: DataSource) {

        @PgAdvisoryLock(key = "'pglock:test:static'")
        open fun staticKey(holdMs: Long = 0) {
            if (holdMs > 0) Thread.sleep(holdMs)
        }

        @PgAdvisoryLock(key = "#id")
        open fun paramKey(id: String) {}

        @PgAdvisoryLock(key = "'pglock:' + #prefix + ':' + #id")
        open fun expressionKey(prefix: String, id: Int) {}

        @PgAdvisoryLock(key = "'pglock:test:throwing'")
        open fun throwingMethod() {
            throw RuntimeException("의도된 예외")
        }

        /** 현재 키에 대한 락이 해제되어 있으면 true */
        open fun isLockFree(key: String): Boolean {
            val lockId = PgAdvisoryLockAspect.keyToLong(key)
            dataSource.connection.use { conn ->
                // pg_try_advisory_lock 성공 시 락 없음 → 즉시 해제 후 true 반환
                val acquired = conn.prepareStatement("SELECT pg_try_advisory_lock(?)").use { ps ->
                    ps.setLong(1, lockId)
                    ps.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
                }
                if (acquired) {
                    conn.prepareStatement("SELECT pg_advisory_unlock(?)").use { ps ->
                        ps.setLong(1, lockId)
                        ps.executeQuery()
                    }
                }
                return acquired
            }
        }
    }

    @Autowired lateinit var svc: LockTestService

    @Test
    fun `기본 동작 - 락 획득 및 정상 해제`() {
        assertDoesNotThrow { svc.staticKey() }
        assertThat(svc.isLockFree("pglock:test:static")).isTrue()
    }

    @Test
    fun `파라미터 참조 SpEL - 다른 키는 독립적으로 획득`() {
        assertDoesNotThrow {
            svc.paramKey("key-a")
            svc.paramKey("key-b")
        }
    }

    @Test
    fun `복합 SpEL 표현식 - prefix + id 조합`() {
        assertDoesNotThrow { svc.expressionKey("post", 42) }
    }

    @Test
    fun `동시 요청 - 같은 키는 하나만 성공하고 나머지는 예외`() {
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // thread1이 락을 1초 동안 유지
        val thread1 = Thread {
            svc.staticKey(holdMs = 1_000)
            successCount.incrementAndGet()
        }
        thread1.start()
        Thread.sleep(100) // thread1이 락을 먼저 잡도록 대기

        // thread2는 같은 키로 시도 → 실패해야 함
        val thread2 = Thread {
            try {
                svc.staticKey()
                successCount.incrementAndGet()
            } catch (_: PgAdvisoryLockAcquisitionException) {
                failCount.incrementAndGet()
            }
        }
        thread2.start()

        thread1.join(5_000)
        thread2.join(5_000)

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(1)
    }

    @Test
    fun `예외 발생해도 락은 반드시 해제 - 이후 재획득 가능`() {
        // 첫 번째 호출: 예외 발생
        assertThrows<RuntimeException> { svc.throwingMethod() }

        // 락이 해제되었는지 확인
        assertThat(svc.isLockFree("pglock:test:throwing")).isTrue()

        // 두 번째 호출: 동일 키로 다시 획득 가능 (LockAcquisitionException이 아닌 RuntimeException)
        val ex = assertThrows<RuntimeException> { svc.throwingMethod() }
        assertThat(ex).isNotInstanceOf(PgAdvisoryLockAcquisitionException::class.java)
    }
}
