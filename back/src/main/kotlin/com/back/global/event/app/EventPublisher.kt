package com.back.global.event.app

import com.back.standard.dto.EventPayload
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service


/**
 * 도메인 이벤트 발행 서비스.
 **/
@Service
class EventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publish(event: EventPayload) {
        applicationEventPublisher.publishEvent(event)
    }
}
