package com.back.global.pgCache.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "cache_item_unlogged",
    indexes = [
        Index(name = "idx_cache_item_expired_at", columnList = "expired_at")
    ]
)
class CacheItem(
    @field:Id
    @field:Column(name = "cache_key", length = 512)
    val cacheKey: String = "",

    @field:Column(name = "value", nullable = false)
    @field:JdbcTypeCode(SqlTypes.JSON)
    val value: String = "",

    @field:Column(name = "expired_at", nullable = false)
    val expiredAt: Instant = Instant.now(),
)
