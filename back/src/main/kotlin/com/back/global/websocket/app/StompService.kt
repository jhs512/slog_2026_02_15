package com.back.global.websocket.app

import com.back.global.pgPubSub.annotation.PgSubscribe
import com.back.global.pgPubSub.app.PgPubSub
import com.back.global.websocket.domain.StompMessage
import com.back.global.websocket.out.StompMessageRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.databind.ObjectMapper

/**
 * 멀티 인스턴스 환경에서 STOMP 브로드캐스트를 담당한다.
 *
 * 흐름: send() → stomp_message 테이블에 저장 → TX 커밋 후 pg_notify(id)
 *      → 모든 인스턴스의 onBroadcast() → DB에서 payload 조회 → SimpMessagingTemplate → 브라우저
 *
 * pg_notify 페이로드를 숫자 id로 제한해 8000바이트 한계를 우회한다.
 */
@Service
class StompService(
    private val pgPubSub: PgPubSub,
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
    private val stompMessageRepository: StompMessageRepository,
) {
    fun send(destination: String, payload: Any) {
        val msg = stompMessageRepository.save(
            StompMessage(
                destination = destination,
                payload = objectMapper.writeValueAsString(payload),
            )
        )

        val id = msg.id

        // TX 커밋 후 notify → 구독자가 DB에서 레코드를 확실히 찾을 수 있도록
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    pgPubSub.publish(CHANNEL, id.toString())
                }
            })
        } else {
            pgPubSub.publish(CHANNEL, id.toString())
        }
    }

    @PgSubscribe(CHANNEL)
    internal fun onBroadcast(idStr: String) {
        val msg = stompMessageRepository.findById(idStr.toInt()).orElse(null) ?: return

        messagingTemplate.convertAndSend(msg.destination, objectMapper.readTree(msg.payload))
    }

    companion object {
        private const val CHANNEL = "stomp-multicast"
    }
}
