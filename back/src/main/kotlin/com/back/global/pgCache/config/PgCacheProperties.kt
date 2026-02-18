package com.back.global.pgCache.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("custom.cache")
data class PgCacheProperties(
    val ttlSeconds: Long = 3600,
    val ttlOverrides: Map<String, Long> = emptyMap(),
)
