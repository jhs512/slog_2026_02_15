package com.back.global.web.app

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.app.app.AppFacade
import com.back.global.exception.app.BusinessException
import com.back.global.security.domain.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class Rq(
    private val req: HttpServletRequest,
    private val resp: HttpServletResponse,
    private val actorFacade: ActorFacade,
) {
    val actorOrNull: Member?
        get() = (SecurityContextHolder.getContext()?.authentication?.principal as? SecurityUser)
            ?.let { actorFacade.memberOf(it) }

    val actor: Member
        get() = actorOrNull ?: throw BusinessException("401-1", "로그인 후 이용해주세요.")

    fun getHeader(name: String, defaultValue: String): String =
        req.getHeader(name) ?: defaultValue

    fun setHeader(name: String, value: String) {
        resp.setHeader(name, value)
    }

    fun getCookieValue(name: String, defaultValue: String): String =
        req.cookies
            ?.firstOrNull { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue

    private fun cookieDomain(): String = AppFacade.siteCookieDomain

    fun setCookie(name: String, value: String?) {
        val cookie = Cookie(name, value ?: "").apply {
            path = "/"
            isHttpOnly = true
            domain = cookieDomain()
            secure = true
            // ADR-0001: 실시간 연결(withCredentials)에 쿠키를 실기 위해 None. CSRF 방어는 CORS + JSON 본문 불변식이 담당.
            setAttribute("SameSite", "None")
            // CHIPS: SameSite=None 쿠키의 3자 컨텍스트 차단 대비. front↔back은 same-site라 파티션 키가 동일해 영향 없다.
            setAttribute("Partitioned", "")
            maxAge = if (value.isNullOrBlank()) 0 else 60 * 60 * 24 * 365
        }

        resp.addCookie(cookie)
    }

    fun deleteCookie(name: String) {
        setCookie(name, null)
    }

    fun sendRedirect(url: String) {
        resp.sendRedirect(url)
    }
}