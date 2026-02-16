package com.back.global.jpa.config

import org.hibernate.boot.Metadata
import org.hibernate.boot.model.relational.SqlStringGenerationContext
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.mapping.Table
import org.hibernate.tool.schema.internal.StandardTableExporter
import org.hibernate.tool.schema.spi.Exporter

class CustomPostgreSQLDialect : PostgreSQLDialect() {
    private val tableExporter = object : StandardTableExporter(this) {
        override fun getSqlCreateStrings(
            table: Table,
            metadata: Metadata,
            context: SqlStringGenerationContext,
        ): Array<String> {
            val sqls = super.getSqlCreateStrings(table, metadata, context)

            if (table.name.endsWith("_unlogged")) {
                return sqls.map { sql ->
                    sql.replaceFirst("create table", "create unlogged table")
                }.toTypedArray()
            }

            return sqls
        }
    }

    override fun getTableExporter(): Exporter<Table> = tableExporter
}
