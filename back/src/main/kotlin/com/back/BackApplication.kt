package com.back

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession

@SpringBootApplication
@EnableJpaAuditing
@EnableJdbcHttpSession
class BackApplication

fun main(args: Array<String>) {
    runApplication<BackApplication>(*args)
}
