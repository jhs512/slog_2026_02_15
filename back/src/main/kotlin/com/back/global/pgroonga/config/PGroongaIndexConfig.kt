package com.back.global.pgroonga.config

import com.back.global.pgroonga.annotation.PGroongaIndex
import jakarta.persistence.Column
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Table
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import java.lang.reflect.Field
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
                    val indexName = "idx_${tableName}_${anno.columns.joinToString("_")}_pgroonga"
                    val quotedTableName = tableName
                        .split(".")
                        .joinToString(".") { part -> "\"$part\"" }
                    val quotedCols = anno.columns.joinToString(", ") { col ->
                        "\"$col\" ${resolveOpclass(entityClass, col)}"
                    }

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

    private fun resolveOpclass(entityClass: Class<*>, columnName: String): String {
        val field = findField(entityClass, columnName)
        val columnDef = field?.getAnnotation(Column::class.java)?.columnDefinition ?: ""
        return if (columnDef.contains("text", ignoreCase = true)) OPCLASS_TEXT
        else OPCLASS_VARCHAR
    }

    private fun findField(entityClass: Class<*>, columnName: String): Field? {
        var cls: Class<*>? = entityClass
        while (cls != null) {
            cls.declaredFields.forEach { field ->
                val colAnno = field.getAnnotation(Column::class.java)
                val effectiveName = colAnno?.name?.takeIf { it.isNotEmpty() } ?: field.name
                if (effectiveName.equals(columnName, ignoreCase = true)) return field
            }
            cls = cls.superclass
        }
        return null
    }

    companion object {
        private const val OPCLASS_TEXT = "pgroonga_text_full_text_search_ops_v2"
        private const val OPCLASS_VARCHAR = "pgroonga_varchar_full_text_search_ops_v2"
    }
}
