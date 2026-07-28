package com.back.global.jpa.domain

/**
 * JPA로 표현할 수 없는 DDL(DESC 인덱스 등)을 엔티티에 선언적으로 붙인다.
 * ddl-auto 실행 이후, 기동 직후 ApplicationRunner(@Order(0))가 순서대로 실행한다.
 * 멱등하게 작성할 것 (CREATE INDEX IF NOT EXISTS 등).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class AfterDDL(
    val sql: String,
)
