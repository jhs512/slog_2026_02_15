package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.member.domain.Member
import com.back.boundedContexts.member.out.MemberRepository
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.`in`.PostApiController
import com.back.boundedContexts.post.out.PostRepository
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostApiControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - 제목 또는 내용에서 검색")
    @WithMockUser
    fun `성공 - 제목+내용 검색`() {
        val author = saveMember("user-search")
        val keyword = "키워드"

        val titleMatched = savePost(author, "타이틀에 키워드", "본문 일반")
        val bodyMatched = savePost(author, "일반 타이틀", "${keyword}가 들어간 본문입니다")
        val bodyOnlyAuthor = savePost(author, "일반 타이틀", "일반 본문")

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
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(titleMatched.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(bodyMatched.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(bodyOnlyAuthor.id.toInt()))))
    }

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - OR 검색")
    @WithMockUser
    fun `성공 - OR 검색`() {
        val author = saveMember("user-or")

        val orTitle = savePost(author, "사과는 단단해", "일반 본문")
        val orBody = savePost(author, "일반 제목", "바나나 스무디")
        val orNot = savePost(author, "딸기", "오렌지")

        mvc.perform(
            get("/post/api/v1/posts")
                .param("q", "사과 OR 바나나")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(PostApiController::class.java))
            .andExpect(handler().methodName("search"))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(orTitle.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(orBody.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(orNot.id.toInt()))))
    }

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - AND 검색")
    @WithMockUser
    fun `성공 - AND 검색`() {
        val author = saveMember("user-and")

        val andMatch = savePost(author, "자바 스프링", "테스트 환경 정리")
        val andNot1 = savePost(author, "자바 스프링", "일반 본문")
        val andNot2 = savePost(author, "테스트 글", "일반 본문")

        mvc.perform(
            get("/post/api/v1/posts")
                .param("q", "자바 AND 테스트")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(PostApiController::class.java))
            .andExpect(handler().methodName("search"))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(andMatch.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(andNot1.id.toInt()))))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(andNot2.id.toInt()))))
    }

    @Test
    @DisplayName("GET /post/api/v1/posts?q= - partial(bigram) 검색")
    @WithMockUser
    fun `성공 - 빅램 partial 검색`() {
        val author = saveMember("user-bigram")

        val target = savePost(author, "검색엔진", "한글 텍스트 색인")
        val notMatch = savePost(author, "인덱스", "일반 본문")

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
            .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(target.id.toInt())))
            .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(notMatch.id.toInt()))))

    }

    private fun saveMember(nameSuffix: String): Member {
        return memberRepository.save(
            Member(
                id = Member.newId(),
                username = "user-search-$nameSuffix",
                password = "1234",
                nickname = "유저-$nameSuffix",
                apiKey = "key-search-$nameSuffix",
            )
        )
    }

    private fun savePost(author: Member, title: String, body: String): Post {
        return postRepository.save(
            Post(
                id = Post.newId(),
                author = author,
                title = title,
                body = body,
                published = true,
            )
        )
    }
}
