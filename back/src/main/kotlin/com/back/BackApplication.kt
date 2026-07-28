package com.back

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession

@SpringBootApplication
@EnableJpaAuditing
@EnableRedisHttpSession
@ConfigurationPropertiesScan
class BackApplication

fun main(args: Array<String>) {
    runApplication<BackApplication>(*args)
}
