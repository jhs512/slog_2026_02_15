package com.back.global.cache

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PgCacheIntegrationTest {

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM cache_store_unlogged")
    }

    @Test
    fun `put and get`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "hello")
        val result = cache.get("key1", String::class.java)

        assertEquals("hello", result)
    }

    @Test
    fun `get returns null for missing key`() {
        val cache = cacheManager.getCache("test")!!

        val result = cache.get("nonexistent")

        assertNull(result)
    }

    @Test
    fun `evict removes entry`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "hello")
        cache.evict("key1")
        val result = cache.get("key1")

        assertNull(result)
    }

    @Test
    fun `clear removes all entries for cache name`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "a")
        cache.put("key2", "b")
        cache.clear()

        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
    }

    @Test
    fun `clear does not affect other cache names`() {
        val cache1 = cacheManager.getCache("cache1")!!
        val cache2 = cacheManager.getCache("cache2")!!

        cache1.put("key1", "a")
        cache2.put("key1", "b")

        cache1.clear()

        assertNull(cache1.get("key1"))
        assertEquals("b", cache2.get("key1", String::class.java))
    }

    @Test
    fun `expired entry returns null`() {
        val cache = cacheManager.getCache("test")!!
        cache.put("key1", "hello")

        jdbcTemplate.update(
            "UPDATE cache_store_unlogged SET expired_at = now() - interval '1 second' WHERE cache_key = ?",
            "test::key1",
        )

        assertNull(cache.get("key1"))
    }

    @Test
    fun `put with complex object`() {
        val cache = cacheManager.getCache("test")!!

        val data = mapOf("name" to "test", "count" to 42)
        cache.put("complex", data)

        val result = cache.get("complex", Map::class.java)
        assertEquals("test", result?.get("name"))
        assertEquals(42, result?.get("count"))
    }
}
