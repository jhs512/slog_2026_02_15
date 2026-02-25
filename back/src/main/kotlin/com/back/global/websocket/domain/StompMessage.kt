package com.back.global.websocket.domain

import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "stomp_message")
class StompMessage(
    val destination: String,
    @Column(columnDefinition = "text") val payload: String,
) : BaseTime()
