package com.back.global.app.app

import com.back.standard.util.Ut
import org.springframework.stereotype.Component
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import tools.jackson.databind.ObjectMapper

@Component
class AppFacade(
    environment: Environment,
    objectMapper: ObjectMapper,
) {
    init {
        Companion.environment = environment
        Ut.JSON.objectMapper = objectMapper
    }

    companion object {
        private lateinit var environment: Environment
        val isDev: Boolean by lazy { environment.matchesProfiles("dev") }
        val isTest: Boolean by lazy { environment.matchesProfiles("test") }
        val isProd: Boolean by lazy { environment.matchesProfiles("prod") }
        val isNotProd: Boolean by lazy { !isProd }
        val systemMemberApiKey: String by lazy { environment.getProperty("custom.systemMemberApiKey")!! }

        // 사이트 도메인 설정
        val siteCookieDomain: String by lazy { environment.getProperty("custom.site.cookieDomain")!! }
        val siteFrontUrl: String by lazy { environment.getProperty("custom.site.frontUrl")!! }
        val siteBackUrl: String by lazy { environment.getProperty("custom.site.backUrl")!! }

        fun getTempDirPath(): String = System.getProperty("java.io.tmpdir")

        val resourcesSampleDirPath: String by lazy {
            val resource = ClassPathResource("sample")
            if (resource.exists()) resource.file.absolutePath
            else "src/main/resources/sample"
        }
    }
}
