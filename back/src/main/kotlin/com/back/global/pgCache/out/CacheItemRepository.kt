package com.back.global.pgCache.out

import com.back.global.pgCache.domain.CacheItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface CacheItemRepository : JpaRepository<CacheItem, String> {
    fun findByCacheKeyAndExpiredAtAfter(cacheKey: String, now: Instant): CacheItem?

    @Modifying
    @Transactional
    fun deleteByCacheKeyStartingWith(prefix: String)

    @Modifying
    @Transactional
    fun deleteByExpiredAtBefore(now: Instant)

    @Modifying
    @Transactional
    @Query(
        value = """
            INSERT INTO cache_item_unlogged (cache_key, value, expired_at)
            VALUES (:key, CAST(:value AS jsonb), now() + CAST(:ttl AS interval))
            ON CONFLICT (cache_key)
            DO UPDATE SET value = EXCLUDED.value, expired_at = EXCLUDED.expired_at
        """,
        nativeQuery = true
    )
    fun upsert(@Param("key") key: String, @Param("value") value: String, @Param("ttl") ttl: String)
}
