package com.back.global.pgCache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.AtomicInteger

@Service
class TestCacheService {
    val callCount = AtomicInteger(0)

    @Cacheable("testCache")
    fun getData(key: String): String {
        callCount.incrementAndGet()
        return "result-$key"
    }

    @CacheEvict("testCache")
    fun evictData(key: String) {
    }

    @Cacheable(value = ["testCache"], key = "#id + ':' + #name")
    fun getByIdAndName(id: Long, name: String): String {
        callCount.incrementAndGet()
        return "result-$id-$name"
    }
}

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CacheableAnnotationTest {

    @Autowired
    lateinit var testCacheService: TestCacheService

    @BeforeEach
    fun setUp() {
        testCacheService.callCount.set(0)
    }

    @Test
    fun `같은 키로 동일 메서드를 두 번 호출하면 두 번째 호출은 캐시된 값으로 처리되어 재실행되지 않는다`() {
        val result1 = testCacheService.getData("foo")
        val result2 = testCacheService.getData("foo")

        assertEquals("result-foo", result1)
        assertEquals("result-foo", result2)
        assertEquals(1, testCacheService.callCount.get(), "Method should only be called once due to caching")
    }

    @Test
    fun `캐시 키가 다르면 같은 메서드라도 호출할 때마다 실제 연산이 다시 수행된다`() {
        testCacheService.getData("a")
        testCacheService.getData("b")

        assertEquals(2, testCacheService.callCount.get())
    }

    @Test
    fun `캐시 항목을 제거한 뒤 같은 키를 다시 조회하면 재계산이 다시 수행된다`() {
        testCacheService.getData("foo")
        assertEquals(1, testCacheService.callCount.get())

        testCacheService.evictData("foo")

        testCacheService.getData("foo")
        assertEquals(2, testCacheService.callCount.get(), "Method should be called again after eviction")
    }

    @Test
    fun `사용자 정의 스펠 표현식 키가 동일하면 캐시 재사용, 다르면 캐시 미스가 발생한다`() {
        val result1 = testCacheService.getByIdAndName(1, "alice")
        val result2 = testCacheService.getByIdAndName(1, "alice")
        val result3 = testCacheService.getByIdAndName(1, "bob")

        assertEquals("result-1-alice", result1)
        assertEquals("result-1-alice", result2)
        assertEquals("result-1-bob", result3)
        assertEquals(2, testCacheService.callCount.get(), "Same id+name should hit cache, different name should miss")
    }
}
