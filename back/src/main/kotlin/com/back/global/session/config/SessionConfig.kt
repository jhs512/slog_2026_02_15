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
            conn.autoCommit = true

            val tables = listOf("spring_session_attributes", "spring_session")
            for (table in tables) {
                val persistence = conn.prepareStatement(
                    """
                    SELECT relpersistence
                    FROM pg_class
                    WHERE relname = ?
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, table)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) rs.getString(1) else null
                    }
                }

                if (persistence == null) {
                    log.info("테이블 {}이 존재하지 않아 건너뜁니다.", table)
                    continue
                }

                if (persistence != "u") {
                    conn.createStatement().use { stmt ->
                        stmt.execute("ALTER TABLE $table SET UNLOGGED")
                    }
                    log.info("테이블 {}을 unlogged로 전환했습니다.", table)
                }
            }
        }
    }
}
