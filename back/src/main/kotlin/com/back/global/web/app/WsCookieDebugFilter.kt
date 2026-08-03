package com.back.global.web.app

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// TODO(임시): /ws 핸드셰이크에 쿠키가 어떻게 실려 오는지 운영에서 확인하기 위한 디버그 필터.
// 인증 쿠키 값이 로그에 그대로 남으므로 확인이 끝나는 대로 이 파일을 통째로 삭제할 것.
@Component
class WsCookieDebugFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/ws")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val cookies = request.cookies
            ?.joinToString(", ") { "${it.name}=${it.value}" }
            ?: "(쿠키 없음)"
        log.info("[WS-COOKIE-DEBUG] {} {} → {}", request.method, request.requestURI, cookies)

        filterChain.doFilter(request, response)
    }
}
