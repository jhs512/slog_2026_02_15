package com.back.global.pgCache

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PgCacheIntegrationTest {

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var em: EntityManager

    @Test
    fun `캐시 저장소에 값을 넣고 같은 키로 다시 읽으면 즉시 저장한 값을 반환한다`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "hello")
        val result = cache.get("key1", String::class.java)

        assertEquals("hello", result)
    }

    @Test
    fun `등록되지 않은 키를 조회하면 널이 반환되고 예외가 발생하지 않는다`() {
        val cache = cacheManager.getCache("test")!!

        val result = cache.get("nonexistent")

        assertNull(result)
    }

    @Test
    fun `캐시 항목을 제거하면 해당 키만 캐시에서 삭제된다`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "hello")
        cache.evict("key1")
        val result = cache.get("key1")

        assertNull(result)
    }

    @Test
    fun `캐시 이름을 지정해 전체 캐시 항목을 한 번에 초기화할 수 있다`() {
        val cache = cacheManager.getCache("test")!!

        cache.put("key1", "a")
        cache.put("key2", "b")
        cache.clear()

        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
    }

    @Test
    fun `한 캐시만 삭제해도 다른 캐시 이름의 데이터는 유지된다`() {
        val cache1 = cacheManager.getCache("cache1")!!
        val cache2 = cacheManager.getCache("cache2")!!

        cache1.put("key1", "a")
        cache2.put("key1", "b")

        cache1.clear()

        assertNull(cache1.get("key1"))
        assertEquals("b", cache2.get("key1", String::class.java))
    }

    @Test
    fun `만료된 캐시 항목은 조회 시 널로 처리되어 갱신 대상이 된다`() {
        val cache = cacheManager.getCache("test")!!
        cache.put("key1", "hello")

        em.createNativeQuery(
            "UPDATE cache_item_unlogged SET expired_at = now() - interval '1 second' WHERE cache_key = :key"
        )
            .setParameter("key", "test::key1")
            .executeUpdate()

        assertNull(cache.get("key1"))
    }

    @Test
    fun `복합 객체를 키와 함께 저장하고 동일 키로 정확히 복원되는지 확인한다`() {
        val cache = cacheManager.getCache("test")!!

        val data = mapOf("name" to "test", "count" to 42)
        cache.put("complex", data)

        val result = cache.get("complex", Map::class.java)
        assertEquals("test", result?.get("name"))
        assertEquals(42, result?.get("count"))
    }
}
