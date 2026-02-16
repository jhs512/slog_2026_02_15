package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.member.out.MemberRepository
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.member.domain.Member
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
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var postFacade: PostFacade

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - 제목 또는 내용에서 검색")
    fun `성공 - 제목+내용 검색`() {
        val fixtures = makeSearchFixturePosts()
        val keyword = "키워드"

        mvc.perform(
            get("/post/api/v1/posts")
                .param("kw", keyword)
                .param("page", "1")
                .param("pageSize", "10")
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
        val fixtures = makeSearchFixturePosts()

        mvc.perform(
            get("/post/api/v1/posts")
                .param("kw", "코끼리 OR 호랑이")
                .param("page", "1")
                .param("pageSize", "10")
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
        val fixtures = makeSearchFixturePosts()

        mvc.perform(
            get("/post/api/v1/posts")
                .param("kw", "안드로이드 AND 개발")
                .param("page", "1")
                .param("pageSize", "10")
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
        val fixtures = makeSearchFixturePosts()

        mvc.perform(
            get("/post/api/v1/posts")
                .param("kw", "검색")
                .param("page", "1")
                .param("pageSize", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(PostApiController::class.java))
            .andExpect(handler().methodName("search"))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(fixtures.bigramTarget.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(fixtures.bigramNotMatch.id.toInt()))))
    }

    @Transactional
    fun makeSearchFixturePosts(): SearchFixturePosts {
        val user1 = memberRepository.findByUsername("user1")!!
        val user2 = memberRepository.findByUsername("user2")!!

        return SearchFixturePosts(
            titleMatch = createPost(user1, "제목 키워드 매치", "일반 내용"),
            bodyMatch = createPost(user1, "일반 제목", "내용에 키워드가 포함"),
            notMatch = createPost(user1, "일반 제목", "일반 내용"),
            orFirstMatch = createPost(user1, "코끼리 이야기", "평범한 내용"),
            orSecondMatch = createPost(user1, "평범한 제목", "호랑이 사냥 기록"),
            orNotMatch = createPost(user1, "강아지", "고양이"),
            andMatch = createPost(user2, "안드로이드 가이드", "개발 환경을 정리한 내용"),
            andNotTitleOnly = createPost(user2, "안드로이드 입문", "일반 본문"),
            andNotBodyOnly = createPost(user2, "개발 일지", "일반 본문"),
            bigramTarget = createPost(user2, "검색 엔진 인덱스", "빅램 기반 문자열 색인"),
            bigramNotMatch = createPost(user2, "색인 엔진", "일반 글")
        )
    }

    @Transactional
    fun createPost(author: Member, title: String, body: String): Post {
        return postFacade.write(Post.newId(), author, title, body)
    }
}

data class SearchFixturePosts(
    val titleMatch: Post,
    val bodyMatch: Post,
    val notMatch: Post,
    val orFirstMatch: Post,
    val orSecondMatch: Post,
    val orNotMatch: Post,
    val andMatch: Post,
    val andNotTitleOnly: Post,
    val andNotBodyOnly: Post,
    val bigramTarget: Post,
    val bigramNotMatch: Post,
)
