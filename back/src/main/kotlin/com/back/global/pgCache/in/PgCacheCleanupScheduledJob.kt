package com.back.global.pgCache.`in`

import com.back.global.pgCache.out.CacheItemRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PgCacheCleanupScheduledJob(
    private val cacheItemRepository: CacheItemRepository,
) {
    @Scheduled(fixedRate = 5 * 60 * 1000)
    fun evictExpiredEntries() {
        cacheItemRepository.deleteByExpiredAtBefore(Instant.now())
    }
}
