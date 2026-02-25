package com.back.global.websocket.`in`

import com.back.global.websocket.out.StompMessageRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class StompMessageCleanup(
    private val stompMessageRepository: StompMessageRepository,
) {
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    fun deleteOldMessages() {
        stompMessageRepository.deleteOlderThan(Instant.now().minus(7, ChronoUnit.DAYS))
    }
}
