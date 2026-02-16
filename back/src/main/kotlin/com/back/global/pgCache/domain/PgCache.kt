package com.back.global.pgCache.domain

import jakarta.persistence.EntityManager
import org.springframework.cache.Cache
import org.springframework.cache.Cache.ValueWrapper
import org.springframework.cache.support.SimpleValueWrapper
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.time.Duration
import java.util.concurrent.Callable

class PgCache(
    private val cacheName: String,
    private val em: EntityManager,
    private val objectMapper: ObjectMapper,
    private val ttl: Duration,
) : Cache {

    companion object {
        fun createTypingObjectMapper(source: ObjectMapper): ObjectMapper {
            val validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Any::class.java)
                .build()
            return (source as JsonMapper).rebuild()
                .activateDefaultTyping(validator, DefaultTyping.NON_FINAL)
                .build()
        }
    }

    private fun cacheKey(key: Any): String = "$cacheName::$key"

    override fun getName(): String = cacheName

    override fun getNativeCache(): Any = this

    @Suppress("UNCHECKED_CAST")
    override fun get(key: Any): ValueWrapper? {
        val results = em.createNativeQuery(
            "SELECT CAST(value AS text) FROM cache_item_unlogged WHERE cache_key = :key AND expired_at > now()"
        )
            .setParameter("key", cacheKey(key))
            .resultList as List<String>

        val json = results.firstOrNull() ?: return null
        val value = objectMapper.readValue(json, Any::class.java)
        return SimpleValueWrapper(value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Any, type: Class<T>?): T? {
        val wrapper = get(key) ?: return null
        return wrapper.get() as T?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Any, valueLoader: Callable<T>): T? {
        val wrapper = get(key)
        if (wrapper != null) return wrapper.get() as T?

        val value = valueLoader.call()
        put(key, value)
        return value
    }

    override fun put(key: Any, value: Any?) {
        if (value == null) {
            evict(key)
            return
        }

        val json = objectMapper.writeValueAsString(value)

        em.createNativeQuery(
            """
            INSERT INTO cache_item_unlogged (cache_key, value, expired_at)
            VALUES (:key, CAST(:value AS jsonb), now() + CAST(:ttl AS interval))
            ON CONFLICT (cache_key)
            DO UPDATE SET value = EXCLUDED.value, expired_at = EXCLUDED.expired_at
            """.trimIndent()
        )
            .setParameter("key", cacheKey(key))
            .setParameter("value", json)
            .setParameter("ttl", "${ttl.seconds} seconds")
            .executeUpdate()
    }

    override fun evict(key: Any) {
        em.createNativeQuery("DELETE FROM cache_item_unlogged WHERE cache_key = :key")
            .setParameter("key", cacheKey(key))
            .executeUpdate()
    }

    override fun clear() {
        em.createNativeQuery("DELETE FROM cache_item_unlogged WHERE cache_key LIKE :pattern")
            .setParameter("pattern", "$cacheName::%")
            .executeUpdate()
    }
}
