package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.domain.shared.memberExtensions.isAdmin
import com.back.standard.extensions.getOrThrow
import org.hamcrest.Matchers
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
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
