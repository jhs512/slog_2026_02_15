package com.back.global.jpa.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate

@Profile("dev", "test")
@Configuration
class DevDatabaseOptimizer(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun unloggedTableOptimizer() = ApplicationRunner {
        ensureIdAutoIncrement("member")
        ensureIdAutoIncrement("post")

        val tables = jdbcTemplate.queryForList(
            """
            SELECT c.relname
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public'
              AND c.relkind = 'r'
              AND c.relpersistence = 'p'
            """,
            String::class.java
        )

        if (tables.isEmpty()) return@ApplicationRunner

        log.info("UNLOGGED 전환 대상 테이블 {}개 발견", tables.size)

        // FK 의존성 때문에 한 번에 안 될 수 있어서 반복
        var remaining = tables.toMutableList()
        var pass = 0

        while (remaining.isNotEmpty() && pass++ < 10) {
            val failed = mutableListOf<String>()

            for (table in remaining) {
                runCatching {
                    jdbcTemplate.execute("ALTER TABLE \"$table\" SET UNLOGGED")
                }.onFailure {
                    failed.add(table)
                }
            }

            remaining = failed
        }

        if (remaining.isEmpty()) {
            log.info("모든 테이블 UNLOGGED 전환 완료")
        } else {
            log.warn("UNLOGGED 전환 실패 테이블: {}", remaining)
        }
    }

    private fun ensureIdAutoIncrement(tableName: String) {
        val hasIdColumn = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS(
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = 'id'
            )
            """,
            Boolean::class.java,
            tableName
        ) ?: false

        if (!hasIdColumn) return

        val hasDefault = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS(
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = 'id'
                  AND column_default ILIKE 'nextval(%'
            )
            """,
            Boolean::class.java,
            tableName
        ) ?: false

        if (hasDefault) return

        val sequenceName = "${tableName}_id_seq"

        runCatching {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS \"${sequenceName}\"")
            jdbcTemplate.execute("ALTER TABLE \"public\".\"$tableName\" ALTER COLUMN id SET DEFAULT nextval('\"$sequenceName\"'::regclass)")

            val maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM \"$tableName\"",
                Long::class.java
            ) ?: 0L

            jdbcTemplate.execute("SELECT setval('\"$sequenceName\"'::regclass, $maxId, true)")
            jdbcTemplate.execute("ALTER SEQUENCE \"${sequenceName}\" OWNED BY \"public\".\"$tableName\".id")
            log.info("테이블 {} id 기본값을 시퀀스로 보정했습니다.", tableName)
        }.onFailure { error ->
            log.warn("테이블 {} id 보정 실패: {}", tableName, error.message)
        }
    }
}
