package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.dto.post.type1.PostSearchSortType1
import com.back.standard.extensions.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PostRepositoryTest {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var memberFacade: MemberFacade

    @Test
    fun `ALL 타입으로 제목 또는 본문 키워드 검색이 동작한다`() {
        val postPage = postRepository.findQPagedByKw(
            PostSearchKeywordType1.ALL,
            "제목",
            PageRequest.of(0, 10, PostSearchSortType1.CREATED_AT.sortBy),
        )

        val content = postPage.content

        assertThat(content).isNotEmpty
        assertThat(content).anyMatch { it.title.contains("제목") || it.content.contains("제목") }
    }

    @Test
    fun `TITLE 타입으로 제목 키워드 검색이 동작한다`() {
        val postPage = postRepository.findQPagedByKw(
            PostSearchKeywordType1.TITLE,
            "제목",
            PageRequest.of(0, 10, PostSearchSortType1.CREATED_AT.sortBy),
        )

        assertThat(postPage.content).isNotEmpty
        assertThat(postPage.content).allMatch { it.title.contains("제목") }
    }

    @Test
    fun `PGroonga 마이너스 문법으로 특정 단어를 제외한 검색이 동작한다`() {
        val author = memberFacade.findByUsername("user1").getOrThrow()
        postFacade.write(author, "사과와 배 이야기", "과일 이야기", true, true)
        postFacade.write(author, "사과 이야기", "배 없는 이야기", true, true)

        // "사과"를 포함하면서 "배"를 제외 — PGroonga &@~ 가 네이티브로 처리
        val postPage = postRepository.findQPagedByKw(
            PostSearchKeywordType1.TITLE,
            "사과 -배",
            PageRequest.of(0, 10, PostSearchSortType1.CREATED_AT.sortBy),
        )

        assertThat(postPage.content).isNotEmpty
        assertThat(postPage.content).allMatch { it.title.contains("사과") && !it.title.contains("배") }
    }
}
