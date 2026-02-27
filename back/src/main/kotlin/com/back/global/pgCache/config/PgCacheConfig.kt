package com.back.global.pgCache.config

import com.back.global.pgCache.domain.PgCache
import com.back.global.pgCache.domain.PgCacheManager
import com.back.global.pgCache.out.CacheItemRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(PgCacheProperties::class)
class PgCacheConfig(
    private val cacheItemRepository: CacheItemRepository,
    private val properties: PgCacheProperties,
) {
    @Bean
    fun cacheManager(objectMapper: ObjectMapper): CacheManager {
        val typingMapper = PgCache.createTypingObjectMapper(objectMapper)
        val ttlOverrides = properties.ttlOverrides.mapValues { Duration.ofSeconds(it.value) }

        return PgCacheManager(
            cacheItemRepository,
            typingMapper,
            Duration.ofSeconds(properties.ttlSeconds),
            ttlOverrides,
        )
    }
}
