package com.back.global.websocket.out

import com.back.global.websocket.domain.StompMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface StompMessageRepository : JpaRepository<StompMessage, Int> {
    @Query("SELECT MAX(m.id) FROM StompMessage m")
    fun findMaxId(): Int?

    fun findAllByIdGreaterThanOrderByIdAsc(id: Int): List<StompMessage>

    @Modifying
    @Transactional
    @Query("DELETE FROM StompMessage m WHERE m.createdAt < :cutoff")
    fun deleteOlderThan(cutoff: Instant)
}
