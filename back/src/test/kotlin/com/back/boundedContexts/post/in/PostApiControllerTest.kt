package com.back.boundedContexts.post.`in`

import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PostApiControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc
    
    @Autowired
    private lateinit var postNotProdInitData: PostNotProdInitData

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - 제목 또는 내용에서 검색")
    fun `성공 - 제목+내용 검색`() {
        val fixtures = postNotProdInitData.makeSearchFixturePosts()
        val keyword = "키워드"

        mvc.perform(
            get("/post/api/v1/posts")
                .param("q", keyword)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(PostApiController::class.java))
            .andExpect(handler().methodName("search"))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(fixtures.titleMatch.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(fixtures.bodyMatch.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(fixtures.notMatch.id.toInt()))))
    }

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - OR 검색")
    fun `성공 - OR 검색`() {
        val fixtures = postNotProdInitData.makeSearchFixturePosts()

        mvc.perform(
            get("/post/api/v1/posts")
                .param("q", "코끼리 OR 호랑이")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(PostApiController::class.java))
            .andExpect(handler().methodName("search"))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(fixtures.orFirstMatch.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(fixtures.orSecondMatch.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(fixtures.orNotMatch.id.toInt()))))
    }

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - AND 검색")
    fun `성공 - AND 검색`() {
        val fixtures = postNotProdInitData.makeSearchFixturePosts()

        mvc.perform(
            get("/post/api/v1/posts")
                .param("q", "안드로이드 AND 개발")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(PostApiController::class.java))
            .andExpect(handler().methodName("search"))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(fixtures.andMatch.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(fixtures.andNotTitleOnly.id.toInt()))))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(fixtures.andNotBodyOnly.id.toInt()))))
    }

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - partial(bigram) 검색")
    fun `성공 - 빅램 partial 검색`() {
        val fixtures = postNotProdInitData.makeSearchFixturePosts()

        mvc.perform(
            get("/post/api/v1/posts")
                .param("q", "검색")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(PostApiController::class.java))
            .andExpect(handler().methodName("search"))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(fixtures.bigramTarget.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(fixtures.bigramNotMatch.id.toInt()))))
    }
}
