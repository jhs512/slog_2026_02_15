package com.back.global.cache

import org.springframework.cache.Cache
import org.springframework.cache.support.AbstractCacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Duration

class PgCacheManager(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val defaultTtl: Duration,
    private val ttlOverrides: Map<String, Duration>,
    private val transactionTemplate: TransactionTemplate,
) : AbstractCacheManager() {

    override fun loadCaches(): Collection<Cache> = emptyList()

    override fun getMissingCache(name: String): Cache {
        val ttl = ttlOverrides[name] ?: defaultTtl
        return PgCache(name, jdbcTemplate, objectMapper, ttl, transactionTemplate)
    }
}
