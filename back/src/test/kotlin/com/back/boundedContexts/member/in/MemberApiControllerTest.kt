package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.out.MemberRepository
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class MemberApiControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Test
    @DisplayName("POST /member/api/v1/members — 회원가입")
    fun `성공 - 회원가입`() {
        mvc.perform(
            post("/member/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "username": "user-new",
                        "password": "1234",
                        "nickname": "무명"
                    }
                    """
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.username").value("user-new"))
            .andExpect(jsonPath("$.data.nickname").value("무명"))
    }

    @Test
    @DisplayName("POST /member/api/v1/members — 중복 username")
    fun `실패 - 회원가입_중복_username`() {
        mvc.perform(
            post("/member/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "username": "user1",
                        "password": "1234",
                        "nickname": "중복"
                    }
                    """
                )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(Matchers.startsWith("이미 존재하는 username 입니다")))
    }

    @Test
    @DisplayName("POST /member/api/v1/members/login — 로그인")
    fun `성공 - 로그인`() {
        val user1 = memberRepository.findByUsername("user1")!!

        mvc.perform(
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
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("${user1.nickname}님 환영합니다."))
            .andExpect(jsonPath("$.data.item.username").value(user1.username))
            .andExpect(jsonPath("$.data.apiKey").value(user1.apiKey))
    }

    @Test
    @DisplayName("POST /member/api/v1/members/login — 비밀번호 오류")
    fun `실패 - 로그인_비밀번호_불일치`() {
        mvc.perform(
            post("/member/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "username": "user1",
                        "password": "wrong"
                    }
                    """
                )
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("GET /member/api/v1/members/me — 내 정보")
    fun `성공 - 내_정보조회`() {
        val user1 = memberRepository.findByUsername("user1")!!

        mvc.perform(
            get("/member/api/v1/members/me")
                .header("Api-Key", user1.apiKey)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.username").value(user1.username))
            .andExpect(jsonPath("$.data.nickname").value(user1.nickname))
            .andExpect(jsonPath("$.message").value("내 정보 조회 성공"))
    }

    @Test
    @DisplayName("GET /member/api/v1/members/me — Api-Key 누락")
    fun `실패 - 내_정보조회_Api_Key_누락`() {
        mvc.perform(
            get("/member/api/v1/members/me")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Api-Key가 없습니다."))
    }
}
