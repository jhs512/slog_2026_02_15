package com.back.global.pgCache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
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

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun setUp() {
        testCacheService.callCount.set(0)
    }

    @Test
    fun `@Cacheable caches the result on second call`() {
        val result1 = testCacheService.getData("foo")
        val result2 = testCacheService.getData("foo")

        assertEquals("result-foo", result1)
        assertEquals("result-foo", result2)
        assertEquals(1, testCacheService.callCount.get(), "Method should only be called once due to caching")
    }

    @Test
    fun `@Cacheable different keys call method each time`() {
        testCacheService.getData("a")
        testCacheService.getData("b")

        assertEquals(2, testCacheService.callCount.get())
    }

    @Test
    fun `@CacheEvict removes cached entry`() {
        testCacheService.getData("foo")
        assertEquals(1, testCacheService.callCount.get())

        testCacheService.evictData("foo")

        testCacheService.getData("foo")
        assertEquals(2, testCacheService.callCount.get(), "Method should be called again after eviction")
    }

    @Test
    fun `@Cacheable with custom SpEL key`() {
        val result1 = testCacheService.getByIdAndName(1, "alice")
        val result2 = testCacheService.getByIdAndName(1, "alice")
        val result3 = testCacheService.getByIdAndName(1, "bob")

        assertEquals("result-1-alice", result1)
        assertEquals("result-1-alice", result2)
        assertEquals("result-1-bob", result3)
        assertEquals(2, testCacheService.callCount.get(), "Same id+name should hit cache, different name should miss")
    }
}
