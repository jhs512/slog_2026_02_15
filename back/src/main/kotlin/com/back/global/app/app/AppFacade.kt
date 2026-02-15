package com.back.global.app.app

import com.back.standard.util.IdGenerator
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class AppFacade(
    environment: Environment,
    idGenerator: IdGenerator
) {
    init {
        _environment = environment
        _idGenerator = idGenerator
    }

    companion object {
        private lateinit var _environment: Environment
        private lateinit var _idGenerator: IdGenerator

        val idGenerator: IdGenerator by lazy { _idGenerator }

        val isDev: Boolean by lazy { _environment.matchesProfiles("dev") }
        val isTest: Boolean by lazy { _environment.matchesProfiles("test") }
        val isProd: Boolean by lazy { _environment.matchesProfiles("prod") }
        val isNotProd: Boolean by lazy { !isProd }
        val systemMemberApiKey: String by lazy { _environment.getProperty("custom.systemMemberApiKey")!! }

        // 사이트 도메인 설정
        val siteCookieDomain: String by lazy { _environment.getProperty("custom.site.cookieDomain")!! }
        val siteFrontUrl: String by lazy { _environment.getProperty("custom.site.frontUrl")!! }
        val siteBackUrl: String by lazy { _environment.getProperty("custom.site.backUrl")!! }
    }
}