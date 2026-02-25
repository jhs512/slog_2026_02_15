package com.back.global.websocket.out

import com.back.global.websocket.domain.StompMessage
import org.springframework.data.jpa.repository.JpaRepository

interface StompMessageRepository : JpaRepository<StompMessage, String>
