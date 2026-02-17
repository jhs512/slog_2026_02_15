package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.MemberFacade
import com.back.standard.extensions.getOrThrow
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AdmMemberControllerTest {
    @Autowired
    private lateinit var memberFacade: MemberFacade

    @Autowired
    private lateinit var mvc: MockMvc


    @Nested
    @DisplayName("GET /member/api/v1/adm/members — 회원 다건조회")
    inner class GetItems {
        @Test
        @DisplayName("성공: page/pageSize 기본값으로 조회")
        @WithUserDetails("admin")
        fun `성공 - 기본값 조회`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 5).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))
        }

        @Test
        @DisplayName("성공: page/pageSize 경계값은 보정되어 조회")
        @WithUserDetails("admin")
        fun `성공 - page와 pageSize 경계 보정 조회`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=0&pageSize=31")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 5).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))
        }

        @Test
        @DisplayName("성공: 공백 키워드는 전체 조회로 처리")
        @WithUserDetails("admin")
        fun `성공 - 공백 키워드 검색`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                        .param("kw", "   ")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 5).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))
        }

        @Test
        @DisplayName("성공: 관리자가 조회")
        @WithUserDetails("admin")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=5")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 5).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))

            for (i in members.indices) {
                val member = members[i]
                resultActions
                    .andExpect(jsonPath("$.content[$i].id").value(member.id))
                    .andExpect(
                        jsonPath("$.content[$i].createdAt")
                            .value(Matchers.startsWith(member.createdAt.toString().take(20)))
                    )
                    .andExpect(
                        jsonPath("$.content[$i].modifiedAt")
                            .value(Matchers.startsWith(member.modifiedAt.toString().take(20)))
                    )
                    .andExpect(jsonPath("$.content[$i].name").value(member.name))
                    .andExpect(jsonPath("$.content[$i].username").value(member.username))
                .andExpect(jsonPath("$.content[$i].isAdmin").value(member.isAdmin))
            }
        }

        @Test
        @DisplayName("성공: 다건 조회 - USERNAME 키워드(bigram) 검색")
        @WithUserDetails("admin")
        fun `성공 - username 키워드 검색`() {
            makeMemberSearchFixture()

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=USERNAME&kw=android")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(
                    jsonPath("$.content[*].username")
                        .value(Matchers.containsInAnyOrder("android-a", "android-guide"))
                )
        }

        @Test
        @DisplayName("성공: 다건 조회 - NICKNAME 키워드(bigram) 검색")
        @WithUserDetails("admin")
        fun `성공 - nickname 키워드 검색`() {
            makeMemberSearchFixture()

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=NICKNAME&kw=안드로이드")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(
                    jsonPath("$.content[*].name")
                        .value(Matchers.containsInAnyOrder("안드로이드 가이드", "안드로이드 레시피"))
                )
        }

        @Test
        @DisplayName("성공: 다건 조회 - ALL + AND/OR 키워드 검색")
        @WithUserDetails("admin")
        fun `성공 - all with and or 검색`() {
            makeMemberSearchFixture()

            val andResult = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=ALL&kw=안드로이드 AND 가이드")
                )
                .andDo(print())

            andResult
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(
                    jsonPath("$.content[*].name")
                        .value(Matchers.contains("안드로이드 가이드"))
                )

            val orResult = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=ALL&kw=안드로이드 OR 가이드")
                )
                .andDo(print())

            orResult
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(
                    jsonPath("$.content[*].name")
                        .value(Matchers.containsInAnyOrder(
                            "안드로이드 가이드",
                            "안드로이드 레시피",
                            "개발 가이드"
                        ))
                )
        }

        @Test
        @DisplayName("실패: 일반 사용자가 조회 → 403")
        @WithUserDetails("user1")
        fun `실패 - 일반 사용자`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."))
        }

        @Test
        @DisplayName("실패: 일반 사용자가 검색 조회 → 403")
        @WithUserDetails("user1")
        fun `실패 - 일반 사용자 검색`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=ALL&kw=android")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."))
        }

        private fun makeMemberSearchFixture() {
            memberFacade.join("android-a", "1234", "안드로이드 가이드")
            memberFacade.join("guide-search", "1234", "안드로이드 레시피")
            memberFacade.join("dev-guide", "1234", "개발 가이드")
            memberFacade.join("android-guide", "1234", "일반 사용자")
        }
    }


    @Nested
    @DisplayName("GET /member/api/v1/adm/members/{id} — 회원 단건조회")
    inner class GetItem {
        @Test
        @DisplayName("성공: 관리자가 조회")
        @WithUserDetails("admin")
        fun `성공`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members/$id")
                )
                .andDo(print())

            val member = memberFacade.findById(id).getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(member.id))
                .andExpect(
                    jsonPath("$.createdAt")
                        .value(Matchers.startsWith(member.createdAt.toString().take(20)))
                )
                .andExpect(
                    jsonPath("$.modifiedAt")
                        .value(Matchers.startsWith(member.modifiedAt.toString().take(20)))
                )
                .andExpect(jsonPath("$.name").value(member.name))
                .andExpect(jsonPath("$.username").value(member.username))
                .andExpect(jsonPath("$.isAdmin").value(member.isAdmin))
        }

        @Test
        @DisplayName("실패: 일반 사용자가 조회 → 403")
        @WithUserDetails("user1")
        fun `실패 - 일반 사용자`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."))
        }
    }
}
