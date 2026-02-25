package com.back.global.pgPubSub.app

import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class PgPubSub(
    private val dataSource: DataSource,
    private val objectMapper: ObjectMapper,
) {

    fun publish(channel: String, payload: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT pg_notify(?, ?)").use { stmt ->
                stmt.setString(1, channel)
                stmt.setString(2, payload)
                stmt.execute()
            }
            if (!conn.autoCommit) conn.commit()
        }
    }

    fun publish(channel: String, payload: Any) =
        publish(channel, objectMapper.writeValueAsString(payload))
}
