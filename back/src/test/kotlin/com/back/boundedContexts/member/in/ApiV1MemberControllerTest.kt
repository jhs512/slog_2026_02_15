package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.standard.extensions.getOrThrow
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1MemberControllerTest {
    @Autowired
    private lateinit var actorFacade: ActorFacade

    @Autowired
    private lateinit var mvc: MockMvc


    @Nested
    inner class Join {
        @Test
        fun `성공 - 회원가입 요청 시 회원이 생성되고 인증 응답이 반환된다`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "usernew",
                                "password": "1234",
                                "nickname": "무명"
                            }
                            """
                        )
                )
                .andDo(print())

            val member = actorFacade.findByUsername("usernew").getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("${member.name}님 환영합니다. 회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(member.id))
                .andExpect(jsonPath("$.data.createdAt").value(Matchers.startsWith(member.createdAt.toString().take(20))))
                .andExpect(jsonPath("$.data.modifiedAt").value(Matchers.startsWith(member.modifiedAt.toString().take(20))))
                .andExpect(jsonPath("$.data.name").value(member.name))
                .andExpect(jsonPath("$.data.isAdmin").value(member.isAdmin))
        }

        @Test
        fun `실패 - 중복 사용자명`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "user1",
                                "password": "1234",
                                "nickname": "중복유저"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 회원 아이디입니다."))
        }

        @Test
        fun `실패 - 유효성 검증 실패`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "",
                                "password": "",
                                "nickname": ""
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
        }
    }


    @Nested
    inner class Login {
        @Test
        fun `성공 - 올바른 계정으로 로그인하면 상태코드가 200이고 토큰과 쿠키가 내려온다`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "user1",
                                "password": "1234"
                            }
                            """
                        )
                )
                .andDo(print())

            val member = actorFacade.findByUsername("user1").getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("${member.nickname}님 환영합니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.item").exists())
                .andExpect(jsonPath("$.data.item.id").value(member.id))
                .andExpect(
                    jsonPath("$.data.item.createdAt").value(
                        Matchers.startsWith(
                            member.createdAt.toString().take(20)
                        )
                    )
                )
                .andExpect(
                    jsonPath("$.data.item.modifiedAt").value(
                        Matchers.startsWith(
                            member.modifiedAt.toString().take(20)
                        )
                    )
                )
                .andExpect(jsonPath("$.data.item.name").value(member.name))
                .andExpect(jsonPath("$.data.item.isAdmin").value(member.isAdmin))
                .andExpect(jsonPath("$.data.apiKey").value(member.apiKey))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)

            resultActions.andExpect { result ->
                val apiKeyCookie = result.response.getCookie("apiKey").getOrThrow()
                assertThat(apiKeyCookie.value).isEqualTo(member.apiKey)
                assertThat(apiKeyCookie.path).isEqualTo("/")
                assertThat(apiKeyCookie.isHttpOnly).isTrue

                val accessTokenCookie = result.response.getCookie("accessToken").getOrThrow()
                assertThat(accessTokenCookie.value).isNotBlank
                assertThat(accessTokenCookie.path).isEqualTo("/")
                assertThat(accessTokenCookie.isHttpOnly).isTrue
            }
        }

        @Test
        fun `실패 - 잘못된 비밀번호`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "user1",
                                "password": "wrong-password"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."))
        }

        @Test
        fun `실패 - 존재하지 않는 사용자`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "nonexistent",
                                "password": "1234"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 아이디입니다."))
        }
    }


    @Nested
    inner class Logout {
        @Test
        fun `성공 - 로그아웃 요청 시 세션 인증 정보가 제거되고 쿠키 만료가 처리된다`() {
            val resultActions = mvc
                .perform(
                    delete("/member/api/v1/members/logout")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("logout"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."))
                .andExpect { result ->
                    val apiKeyCookie = result.response.getCookie("apiKey").getOrThrow()
                    assertThat(apiKeyCookie.value).isEmpty()
                    assertThat(apiKeyCookie.maxAge).isEqualTo(0)
                    assertThat(apiKeyCookie.path).isEqualTo("/")
                    assertThat(apiKeyCookie.isHttpOnly).isTrue

                    val accessTokenCookie = result.response.getCookie("accessToken").getOrThrow()
                    assertThat(accessTokenCookie.value).isEmpty()
                    assertThat(accessTokenCookie.maxAge).isEqualTo(0)
                    assertThat(accessTokenCookie.path).isEqualTo("/")
                    assertThat(accessTokenCookie.isHttpOnly).isTrue
                }
        }
    }


    @Nested
    inner class Me {
        @Test
        @WithUserDetails("user1")
        fun `성공 - 인증된 사용자가 내 정보를 조회하면 상세 정보가 반환된다`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                )
                .andDo(print())

            val member = actorFacade.findByUsername("user1").getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(member.id))
                .andExpect(jsonPath("$.createdAt").value(Matchers.startsWith(member.createdAt.toString().take(20))))
                .andExpect(jsonPath("$.modifiedAt").value(Matchers.startsWith(member.modifiedAt.toString().take(20))))
                .andExpect(jsonPath("$.name").value(member.name))
                .andExpect(jsonPath("$.username").value(member.username))
                .andExpect(jsonPath("$.isAdmin").value(member.isAdmin))
        }

        @Test
        fun `성공 - API 키 쿠키만 있어도 내 정보 조회가 가능하다`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val actorApiKey: String = actor.apiKey

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                        .cookie(Cookie("apiKey", actorApiKey))
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk)
        }

        @Test
        fun `성공 - API 키는 유효하고 액세스 토큰은 잘못되어도 재발급이 수행된다`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val actorApiKey: String = actor.apiKey

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $actorApiKey wrong-access-token")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk)

            resultActions.andExpect { result ->
                val accessTokenCookie = result.response.getCookie("accessToken").getOrThrow()
                assertThat(accessTokenCookie.value).isNotBlank
                assertThat(accessTokenCookie.path).isEqualTo("/")
                assertThat(accessTokenCookie.isHttpOnly).isTrue

                val headerAuthorization = result.response.getHeader(HttpHeaders.AUTHORIZATION)
                assertThat(headerAuthorization).isNotBlank

                assertThat(headerAuthorization).isEqualTo(accessTokenCookie.value)
            }
        }

        @Test
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }

        @Test
        fun `실패 - 베어러 형식 아님`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "key")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-2"))
                .andExpect(jsonPath("$.msg").value("Authorization 헤더가 Bearer 형식이 아닙니다."))
        }
    }


    @Nested
    inner class RedirectToProfileImg {
        @Test
        fun `성공 - 프로필 이미지 리다이렉트 응답이 반환된다`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/$id/redirectToProfileImg")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("redirectToProfileImg"))
                .andExpect(status().isFound)
                .andExpect(header().exists("Location"))
        }

        @Test
        fun `실패 - 존재하지 않는 회원`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/$id/redirectToProfileImg")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("redirectToProfileImg"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    inner class RandomSecureTip {
        @Test
        @WithUserDetails("user1")
        fun `인증된 사용자가 랜덤 보안 팁을 정상 조회한다`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/randomSecureTip")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("randomSecureTip"))
                .andExpect(status().isOk)
        }

        @Test
        fun `실패 - 미인증 상태에서는 랜덤 보안 팁 조회가 거부된다`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/randomSecureTip")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }
}
