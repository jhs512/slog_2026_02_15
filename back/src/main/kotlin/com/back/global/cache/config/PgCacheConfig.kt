package com.back.global.cache.config

import com.back.global.cache.PgCache
import com.back.global.cache.PgCacheManager
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@ConfigurationProperties(prefix = "custom.cache")
data class PgCacheProperties(
    val ttlSeconds: Long = 3600,
    val ttlOverrides: Map<String, Long> = emptyMap(),
)

@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(PgCacheProperties::class)
class PgCacheConfig(
    private val jdbcTemplate: JdbcTemplate,
    private val transactionManager: PlatformTransactionManager,
    private val properties: PgCacheProperties,
) {

    @Bean
    fun cacheManager(
        objectMapper: ObjectMapper,
    ): CacheManager {
        val typingMapper = PgCache.createTypingObjectMapper(objectMapper)
        val transactionTemplate = TransactionTemplate(transactionManager)
        val ttlOverrides = properties.ttlOverrides.mapValues { Duration.ofSeconds(it.value) }
        return PgCacheManager(jdbcTemplate, typingMapper, Duration.ofSeconds(properties.ttlSeconds), ttlOverrides, transactionTemplate)
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    fun evictExpiredEntries() {
        jdbcTemplate.update("DELETE FROM cache_store_unlogged WHERE expired_at <= now()")
    }
}
