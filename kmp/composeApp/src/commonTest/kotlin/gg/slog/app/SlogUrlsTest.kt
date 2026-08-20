package gg.slog.app

import gg.slog.app.data.SlogUrls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlogUrlsTest {
    @Test
    fun 글_본문_주소를_만든다() {
        assertEquals("https://www.slog.gg/p/14849", SlogUrls.postPage(14849))
    }

    @Test
    fun 카카오_리다이렉트_주소에_돌아올_주소가_담긴다() {
        val url = SlogUrls.kakaoRedirectLogin("https://www.slog.gg/members/me")
        assertTrue(url.startsWith("https://api.slog.gg/oauth2/authorization/kakao"))
        assertTrue(url.contains("redirectUrl=https://www.slog.gg/members/me"))
    }
}
