package com.back.global.pgCache.config

import com.back.global.pgCache.domain.PgCache
import com.back.global.pgCache.domain.PgCacheManager
import jakarta.persistence.EntityManager
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Duration


@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(PgCacheProperties::class)
class PgCacheConfig(
    private val em: EntityManager,
    private val properties: PgCacheProperties,
) {
    @Bean
    fun cacheManager(
        objectMapper: ObjectMapper,
    ): CacheManager {
        val typingMapper = PgCache.createTypingObjectMapper(objectMapper)
        val ttlOverrides = properties.ttlOverrides.mapValues { Duration.ofSeconds(it.value) }
        return PgCacheManager(
            em,
            typingMapper,
            Duration.ofSeconds(properties.ttlSeconds),
            ttlOverrides,
        )
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    fun evictExpiredEntries() {
        em.createNativeQuery("DELETE FROM cache_item_unlogged WHERE expired_at <= now()")
            .executeUpdate()
    }
}
