package com.back.global.pgCache.domain

import com.back.global.pgCache.out.CacheItemRepository
import org.springframework.cache.Cache
import org.springframework.cache.Cache.ValueWrapper
import org.springframework.cache.support.SimpleValueWrapper
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable

class PgCache(
    private val cacheName: String,
    private val cacheItemRepository: CacheItemRepository,
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
        val item = cacheItemRepository.findByCacheKeyAndExpiredAtAfter(cacheKey(key), Instant.now())
            ?: return null

        val value = objectMapper.readValue(item.value, Any::class.java)

        return SimpleValueWrapper(value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Any, type: Class<T>?): T? = get(key)?.get() as T?

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

        cacheItemRepository.upsert(cacheKey(key), objectMapper.writeValueAsString(value), "${ttl.seconds} seconds")
    }

    override fun evict(key: Any) {
        cacheItemRepository.deleteById(cacheKey(key))
    }

    override fun clear() {
        cacheItemRepository.deleteByCacheKeyStartingWith("$cacheName::")
    }
}
