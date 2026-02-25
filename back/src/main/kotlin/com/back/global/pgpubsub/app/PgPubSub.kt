package com.back.global.pgpubsub.app

import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class PgPubSub(private val dataSource: DataSource) {

    fun publish(channel: String, payload: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT pg_notify(?, ?)").use { stmt ->
                stmt.setString(1, channel)
                stmt.setString(2, payload)
                stmt.execute()
            }
        }
    }
}
