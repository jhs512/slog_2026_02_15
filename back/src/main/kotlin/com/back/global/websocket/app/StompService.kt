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
import java.util.UUID

/**
 * 멀티 인스턴스 환경에서 STOMP 브로드캐스트를 담당한다.
 *
 * 흐름: send() → stomp_message 테이블에 저장 → TX 커밋 후 pg_notify(UUID)
 *      → 모든 인스턴스의 onBroadcast() → DB에서 payload 조회 → SimpMessagingTemplate → 브라우저
 *
 * pg_notify 페이로드를 UUID(36자)로 제한해 8000바이트 한계를 우회한다.
 */
@Service
class StompService(
    private val pgPubSub: PgPubSub,
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
    private val stompMessageRepository: StompMessageRepository,
) {
    fun send(destination: String, payload: Any) {
        val uuid = UUID.randomUUID().toString()
        stompMessageRepository.save(
            StompMessage(
                id = uuid,
                destination = destination,
                payload = objectMapper.writeValueAsString(payload),
            )
        )
        // TX 커밋 후 notify → 구독자가 DB에서 레코드를 확실히 찾을 수 있도록
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    pgPubSub.publish(CHANNEL, uuid)
                }
            })
        } else {
            pgPubSub.publish(CHANNEL, uuid)
        }
    }

    @PgSubscribe(CHANNEL)
    internal fun onBroadcast(uuid: String) {
        val msg = stompMessageRepository.findById(uuid).orElse(null) ?: return
        messagingTemplate.convertAndSend(msg.destination, objectMapper.readTree(msg.payload))
        runCatching { stompMessageRepository.deleteById(uuid) }
    }

    companion object {
        private const val CHANNEL = "stomp-broadcast"
    }
}
