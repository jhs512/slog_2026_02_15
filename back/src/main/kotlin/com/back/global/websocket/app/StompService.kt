package com.back.global.websocket.app

import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

/**
 * 멀티 인스턴스 환경에서 STOMP 브로드캐스트를 담당한다.
 *
 * 흐름: send() → Redis publish(destination + payload JSON)
 *      → 모든 인스턴스의 onMessage() → SimpMessagingTemplate → 브라우저
 */
@Service
class StompService(
    private val redisTemplate: StringRedisTemplate,
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    companion object {
        const val CHANNEL = "stomp-multicast"
    }

    fun send(destination: String, payload: Any) {
        val json = objectMapper.writeValueAsString(
            mapOf("destination" to destination, "payload" to payload)
        )

        redisTemplate.convertAndSend(CHANNEL, json)
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val node = objectMapper.readTree(message.body)

        val destination = node.get("destination").textValue()!!
        val payload = objectMapper.treeToValue(node.get("payload"), Any::class.java)

        messagingTemplate.convertAndSend(destination, payload)
    }
}
