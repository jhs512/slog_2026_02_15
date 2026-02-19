package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.MemberFacade
import com.back.standard.extensions.getOrThrow
import org.hamcrest.Matchers
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
    inner class GetItems {
        @Test
        @WithUserDetails("admin")
        fun `성공 - 관리자 목록 조회에서 페이지와 크기 기본값이 적용된 상태로 응답한다`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 30).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - 페이지와 페이지 크기 경계 보정 조회`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=0&pageSize=31")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 30).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kw=   (공백은 검색 미적용)`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                        .param("kw", "   ")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 30).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?page=1&pageSize=5`() {
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
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kwType=USERNAME&kw=android`() {
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
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kwType=USERNAME&kw=KAKAO (부분일치)`() {
            memberFacade.join("super-KAKAO-user", "1234", "안드로이드 가이드")
            memberFacade.join("KAKAO-hero", "1234", "일반 사용자")
            memberFacade.join("other-user", "1234", "KAKAO 관리자")

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=USERNAME&kw=KAKAO")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].username").value(Matchers.containsInAnyOrder("super-KAKAO-user", "KAKAO-hero")))
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kwType=NICKNAME&kw=안드로이드`() {
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
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kwType=NICKNAME&kw=dev%_guide (와일드카드 이스케이프)`() {
            memberFacade.join("nicktest1", "1234", "dev%_guide")
            memberFacade.join("nicktest2", "1234", "devabcguide")

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("kwType", "NICKNAME")
                        .param("kw", "dev%_guide")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("nicktest1"))
                .andExpect(jsonPath("$.content[0].name").value("dev%_guide"))
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kwType=ALL&kw=안드로이드 AND 가이드`() {
            makeMemberSearchFixture()

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=ALL&kw=안드로이드 AND 가이드")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(
                    jsonPath("$.content[*].name")
                        .value(Matchers.contains("안드로이드 가이드"))
                )
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kwType=ALL&kw=안드로이드 OR 가이드`() {
            makeMemberSearchFixture()

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kwType=ALL&kw=안드로이드 OR 가이드")
                )
                .andDo(print())

            resultActions
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
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kwType=ALL&kw=guide`() {
            makeMemberSearchFixture()

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("kwType", "ALL")
                        .param("kw", "guide")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[*].username").value(Matchers.containsInAnyOrder("guide-search", "dev-guide", "android-guide")))
                .andExpect(jsonPath("$.content[*].name").value(Matchers.hasItem("개발 가이드")))
                .andExpect(jsonPath("$.content[*].name").value(Matchers.hasItem("안드로이드 레시피")))
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - GET member api v1 adm members?kw=guide (kwType 생략 시 ALL)`() {
            makeMemberSearchFixture()

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=10&kw=guide")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[*].username").value(Matchers.containsInAnyOrder("guide-search", "dev-guide", "android-guide")))
                .andExpect(jsonPath("$.content[*].name").value(Matchers.hasItem("개발 가이드")))
                .andExpect(jsonPath("$.content[*].name").value(Matchers.hasItem("안드로이드 레시피")))
        }

        @Test
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
    inner class GetItem {
        @Test
        @WithUserDetails("admin")
        fun `성공 - 관리자 기본 조회 조건으로 단일 회원 상세 정보를 정확히 반환한다`() {
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
