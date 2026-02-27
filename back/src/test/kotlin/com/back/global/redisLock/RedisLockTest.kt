package com.back.global.redisLock

import com.back.global.redisLock.annotation.RedisLock
import com.back.global.redisLock.app.RedisLockAcquisitionException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.atomic.AtomicInteger

@ActiveProfiles("test")
@SpringBootTest
class RedisLockTest {

    @TestConfiguration
    class Cfg {
        @Bean
        fun lockTestService(redissonClient: RedissonClient) = LockTestService(redissonClient)
    }

    open class LockTestService(private val redissonClient: RedissonClient) {

        @RedisLock(key = "'test:static'")
        open fun staticKey(holdMs: Long = 0) {
            if (holdMs > 0) Thread.sleep(holdMs)
        }

        @RedisLock(key = "#id")
        open fun paramKey(id: String) {}

        @RedisLock(key = "'test:' + #prefix + ':' + #id")
        open fun expressionKey(prefix: String, id: Int) {}

        @RedisLock(key = "'test:throwing'")
        open fun throwingMethod() {
            throw RuntimeException("의도된 예외")
        }

    }

    @Autowired lateinit var svc: LockTestService
    @Autowired lateinit var redissonClient: RedissonClient

    private fun isLockFree(key: String): Boolean = !redissonClient.getLock("redislock:$key").isLocked

    @Test
    fun `기본 동작 - 락 획득 및 정상 해제`() {
        assertDoesNotThrow { svc.staticKey() }
        assertThat(isLockFree("test:static")).isTrue()
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
            } catch (_: RedisLockAcquisitionException) {
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
        assertThat(isLockFree("test:throwing")).isTrue()

        // 두 번째 호출: 동일 키로 다시 획득 가능 (LockAcquisitionException이 아닌 RuntimeException)
        val ex = assertThrows<RuntimeException> { svc.throwingMethod() }
        assertThat(ex).isNotInstanceOf(RedisLockAcquisitionException::class.java)
    }
}
