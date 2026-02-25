package com.back.global.websocket.app

import com.back.global.pgPubSub.annotation.PgSubscribe
import com.back.global.pgPubSub.app.PgPubSub
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

// PG pub/sub 전송 단위. JsonNode로 payload를 보존해 이중 직렬화를 피함
internal data class StompBroadcastMsg(val destination: String, val payload: JsonNode)

/**
 * 멀티 인스턴스 환경에서 STOMP 브로드캐스트를 담당한다.
 *
 * 흐름: send() → pg_notify → 모든 인스턴스의 onBroadcast() → SimpMessagingTemplate → 브라우저
 */
@Service
class StompService(
    private val pgPubSub: PgPubSub,
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun send(destination: String, payload: Any) {
        pgPubSub.publish(CHANNEL, StompBroadcastMsg(destination, objectMapper.valueToTree(payload)))
    }

    @PgSubscribe(CHANNEL)
    internal fun onBroadcast(msg: StompBroadcastMsg) {
        messagingTemplate.convertAndSend(msg.destination, msg.payload)
    }

    companion object {
        private const val CHANNEL = "stomp-broadcast"
    }
}
