package com.back.global.session.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class SessionConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun sessionTableUnloggedRunner(dataSource: DataSource) = ApplicationRunner {
        dataSource.connection.use { conn ->
            val tables = listOf("spring_session", "spring_session_attributes")
            for (table in tables) {
                val isUnlogged = conn.prepareStatement(
                    """
                    SELECT relpersistence = 'u'
                    FROM pg_class
                    WHERE relname = ?
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, table)
                    stmt.executeQuery().use { rs ->
                        rs.next() && rs.getBoolean(1)
                    }
                }

                if (!isUnlogged) {
                    conn.createStatement().use { stmt ->
                        stmt.execute("ALTER TABLE $table SET UNLOGGED")
                    }
                    log.info("테이블 {}을 unlogged로 전환했습니다.", table)
                }
            }
        }
    }
}
