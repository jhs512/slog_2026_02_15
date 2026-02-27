package com.back.global.pgPubSub.app

import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import javax.sql.DataSource

@Component
class PgPubSub(
    private val dataSource: DataSource,
    private val objectMapper: ObjectMapper,
) {
    private val onConnectListeners = mutableListOf<() -> Unit>()

    fun addOnConnectListener(listener: () -> Unit) {
        onConnectListeners += listener
    }

    internal fun fireOnConnect() {
        onConnectListeners.forEach { it() }
    }

    fun publish(channel: String, payloadId: Int) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT pg_notify(?, ?)").use { stmt ->
                stmt.setString(1, channel)
                stmt.setString(2, payloadId.toString())
                stmt.execute()
            }

            if (!conn.autoCommit) conn.commit()
        }
    }
}
