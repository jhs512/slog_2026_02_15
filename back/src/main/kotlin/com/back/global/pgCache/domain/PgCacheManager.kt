package com.back.global.pgCache.domain

import com.back.global.pgCache.out.CacheItemRepository
import org.springframework.cache.Cache
import org.springframework.cache.support.AbstractCacheManager
import tools.jackson.databind.ObjectMapper
import java.time.Duration

class PgCacheManager(
    private val cacheItemRepository: CacheItemRepository,
    private val objectMapper: ObjectMapper,
    private val defaultTtl: Duration,
    private val ttlOverrides: Map<String, Duration>,
) : AbstractCacheManager() {

    override fun loadCaches(): Collection<Cache> = emptyList()

    override fun getMissingCache(name: String): Cache {
        val ttl = ttlOverrides[name] ?: defaultTtl
        return PgCache(name, cacheItemRepository, objectMapper, ttl)
    }
}
