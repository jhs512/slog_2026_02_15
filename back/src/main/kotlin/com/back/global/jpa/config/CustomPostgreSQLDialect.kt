package com.back.global.jpa.config

import com.back.global.pgroonga.config.PGroongaMatchFunction
import org.hibernate.boot.Metadata
import org.hibernate.boot.model.relational.SqlStringGenerationContext
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.mapping.Table
import org.hibernate.tool.schema.internal.StandardTableExporter
import org.hibernate.tool.schema.spi.Exporter
import org.hibernate.type.BasicType
import org.hibernate.type.SqlTypes

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

    override fun initializeFunctionRegistry(functionContributions: org.hibernate.boot.model.FunctionContributions) {
        super.initializeFunctionRegistry(functionContributions)
        @Suppress("UNCHECKED_CAST")
        val booleanType = functionContributions.typeConfiguration
            .basicTypeRegistry
            .resolve(Boolean::class.javaObjectType, SqlTypes.BOOLEAN) as BasicType<Boolean>
        functionContributions.functionRegistry.register(
            "pgroonga_match",
            PGroongaMatchFunction(booleanType)
        )
    }
}
