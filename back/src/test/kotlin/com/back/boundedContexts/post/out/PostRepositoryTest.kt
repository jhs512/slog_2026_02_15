package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.dto.post.type1.PostSearchSortType1
import com.back.standard.extensions.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
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

    @Test
    fun `ALL 타입에서 제목에 있는 단어를 마이너스로 제외하면 본문에서만 매칭돼도 제외된다`() {
        val author = memberFacade.findByUsername("user1").getOrThrow()
        // title에 "제외단어"가 있고, content에 "검색어"가 있는 글 → 제외되어야 함
        postFacade.write(author, "제외단어 포함 제목", "검색어 있는 본문", true, true)
        // title에 "제외단어"가 없고, content에 "검색어"가 있는 글 → 결과에 포함되어야 함
        postFacade.write(author, "일반 제목", "검색어 있는 본문", true, true)

        val postPage = postRepository.findQPagedByKw(
            PostSearchKeywordType1.ALL,
            "검색어 -제외단어",
            PageRequest.of(0, 10, PostSearchSortType1.CREATED_AT.sortBy),
        )

        assertThat(postPage.content).isNotEmpty
        assertThat(postPage.content).noneMatch { it.title.contains("제외단어") || it.content.contains("제외단어") }
        assertThat(postPage.content).allMatch { it.title.contains("검색어") || it.content.contains("검색어") }
    }

    @Test
    fun `ALL 타입에서 복수 마이너스와 복수 플러스가 모두 동작한다`() {
        val author = memberFacade.findByUsername("user1").getOrThrow()
        postFacade.write(author, "사과 바나나", "맛있는 과일", true, true)
        postFacade.write(author, "사과 오렌지", "맛있는 과일", true, true)
        postFacade.write(author, "포도 바나나", "맛있는 과일", true, true)

        // 사과 필수, 바나나 제외
        val postPage = postRepository.findQPagedByKw(
            PostSearchKeywordType1.ALL,
            "+사과 -바나나",
            PageRequest.of(0, 10, PostSearchSortType1.CREATED_AT.sortBy),
        )

        assertThat(postPage.content).isNotEmpty
        assertThat(postPage.content).allMatch {
            (it.title.contains("사과") || it.content.contains("사과")) &&
                !it.title.contains("바나나") && !it.content.contains("바나나")
        }
    }

    @Test
    fun `테라폼 한밭대 GIT 리눅스 시나리오 - 제목에 GIT있는 글은 -GIT으로 제외된다`() {
        val author = memberFacade.findByUsername("user1").getOrThrow()
        // 제목에 GIT이 있고, 본문에 테라폼/한밭대가 있는 글 → -GIT 으로 제외되어야 함
        postFacade.write(author, "스프링부트/GIT", "테라폼 한밭대 강의 정리", true, true)
        // 제목에 리눅스가 있고, 본문에 테라폼/한밭대가 있는 글 → -리눅스 으로 제외되어야 함
        postFacade.write(author, "리눅스/도커", "테라폼 한밭대 강의 정리", true, true)
        // PGroonga &@~ 에서 공백은 AND → 테라폼/한밭대 둘 다 있어야 매칭됨
        postFacade.write(author, "테라폼 한밭대 입문", "클라우드 수업", true, true)

        val postPage = postRepository.findQPagedByKw(
            PostSearchKeywordType1.ALL,
            "테라폼 +한밭대 -리눅스 -GIT",
            PageRequest.of(0, 10, PostSearchSortType1.CREATED_AT.sortBy),
        )

        // 스프링부트/GIT, 리눅스/도커 둘 다 제외
        assertThat(postPage.content).noneMatch { it.title.contains("GIT") || it.title.contains("리눅스") }
        // 테라폼 한밭대 입문 글은 포함
        assertThat(postPage.content).anyMatch { it.title.contains("테라폼") && it.title.contains("한밭대") }
    }
}
