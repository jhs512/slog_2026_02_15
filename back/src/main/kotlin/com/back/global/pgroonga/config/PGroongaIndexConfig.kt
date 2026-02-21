package com.back.global.pgroonga.config

import com.back.global.pgroonga.annotation.PGroongaIndex
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Table
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import javax.sql.DataSource

@Configuration
class PGroongaIndexConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    @Order(0)
    fun pgroongaIndexRunner(
        dataSource: DataSource,
        entityManagerFactory: EntityManagerFactory,
    ) = ApplicationRunner {
        dataSource.connection.use { conn ->
            conn.autoCommit = true

            conn.createStatement().use { stmt ->
                runCatching {
                    stmt.execute("CREATE EXTENSION IF NOT EXISTS pgroonga")
                }.onFailure { ex ->
                    log.warn("PGroonga 확장 확인 실패: {}", ex.message)
                    return@ApplicationRunner
                }
            }
            log.info("PGroonga extension 확인 완료")

            val entityClasses = entityManagerFactory.metamodel.entities
                .mapNotNull { it.javaType }

            for (entityClass in entityClasses) {
                val annotations = entityClass.getAnnotationsByType(PGroongaIndex::class.java)
                if (annotations.isEmpty()) continue

                val tableName = entityClass.getAnnotation(Table::class.java)?.name
                    ?.takeIf { it.isNotEmpty() }
                    ?: entityClass.simpleName.lowercase()

                for (anno in annotations) {
                    val cols = anno.columns.joinToString(", ")
                    val indexName = "idx_${tableName}_${anno.columns.joinToString("_")}_pgroonga"
                    val quotedTableName = tableName
                        .split(".")
                        .joinToString(".") { part -> "\"$part\"" }
                    val quotedCols = anno.columns.joinToString(", ") { col -> "\"$col\"" }

                    val ddl = """
                        CREATE INDEX IF NOT EXISTS $indexName
                        ON $quotedTableName USING pgroonga ($quotedCols)
                        WITH (tokenizer = '${anno.tokenizer}')
                    """.trimIndent()

                    runCatching {
                        conn.createStatement().use { stmt ->
                            log.info("PGroonga 인덱스 DDL: {}", ddl)
                            stmt.execute(ddl)
                        }
                        log.info("PGroonga 인덱스 생성: {}", indexName)
                    }.onFailure { ex ->
                        log.warn("PGroonga 인덱스 생성 실패: {} (DDL: {})", ex.message, ddl)
                    }
                }
            }
        }
    }
}
