package com.back.global.jpa.config

import org.hibernate.mapping.Table
import org.hibernate.tool.schema.internal.StandardTableExporter
import org.hibernate.tool.schema.spi.Exporter

// dev 전용: 모든 테이블을 unlogged로 생성해 로컬 쓰기 성능을 높인다 (WAL 미기록, 크래시 시 데이터 소실 허용)
open class CustomDevPostgreSQLDialect : CustomPostgreSQLDialect() {

    private val unloggedTableExporter = object : StandardTableExporter(this) {
        override fun tableCreateString(temporary: Boolean): String {
            return if (temporary) super.tableCreateString(true) else "create unlogged table"
        }
    }

    override fun getTableExporter(): Exporter<Table> = unloggedTableExporter
}
