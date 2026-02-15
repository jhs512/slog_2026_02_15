package com.back.global.cache

import org.springframework.cache.Cache
import org.springframework.cache.Cache.ValueWrapper
import org.springframework.cache.support.SimpleValueWrapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.time.Duration
import java.util.concurrent.Callable

class PgCache(
    private val cacheName: String,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val ttl: Duration,
    private val transactionTemplate: TransactionTemplate,
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

    override fun get(key: Any): ValueWrapper? {
        return transactionTemplate.execute {
            val json = jdbcTemplate.query(
                "SELECT value FROM cache_store_unlogged WHERE cache_key = ? AND expired_at > now()",
                { rs, _ -> rs.getString("value") },
                cacheKey(key),
            ).firstOrNull() ?: return@execute null

            val value = objectMapper.readValue(json, Any::class.java)
            SimpleValueWrapper(value)
        }
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

        transactionTemplate.executeWithoutResult {
            jdbcTemplate.update(
                """
                INSERT INTO cache_store_unlogged (cache_key, value, expired_at)
                VALUES (?, ?::jsonb, now() + ?::interval)
                ON CONFLICT (cache_key)
                DO UPDATE SET value = EXCLUDED.value, expired_at = EXCLUDED.expired_at
                """.trimIndent(),
                cacheKey(key),
                json,
                "${ttl.seconds} seconds",
            )
        }
    }

    override fun evict(key: Any) {
        transactionTemplate.executeWithoutResult {
            jdbcTemplate.update(
                "DELETE FROM cache_store_unlogged WHERE cache_key = ?",
                cacheKey(key),
            )
        }
    }

    override fun clear() {
        transactionTemplate.executeWithoutResult {
            jdbcTemplate.update(
                "DELETE FROM cache_store_unlogged WHERE cache_key LIKE ?",
                "$cacheName::%",
            )
        }
    }
}
