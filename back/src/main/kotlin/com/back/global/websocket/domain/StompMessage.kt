package com.back.global.websocket.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "stomp_message")
class StompMessage(
    @Id val id: String,
    val destination: String,
    @Column(columnDefinition = "text") val payload: String,
    val createdAt: Instant = Instant.now(),
)
