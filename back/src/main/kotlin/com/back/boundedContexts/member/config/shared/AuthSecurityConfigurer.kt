package com.back.boundedContexts.member.config.shared

import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl
import org.springframework.stereotype.Component

@Component
class AuthSecurityConfigurer {
    fun configure(authorize: AuthorizeHttpRequestsDsl) {
        authorize.apply {
            authorize("/member/api/*/auth/login", permitAll)
            authorize("/member/api/*/auth/logout", permitAll)
            // 네이티브 앱 카카오 로그인 — 로그인 전이므로 인증 없이 열어둔다
            authorize("/member/api/*/auth/social/*", permitAll)
        }
    }
}
