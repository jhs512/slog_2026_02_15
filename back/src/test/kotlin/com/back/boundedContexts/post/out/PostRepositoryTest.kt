package com.back.boundedContexts.post.out


import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.dto.post.type1.PostSearchSortType1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
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

    @Test
    @DisplayName("제목 또는 본문 통합 검색이 동작한다")
    fun `통합 검색으로 제목 또는 본문 키워드 검색이 동작한다`() {
        val postPage = postRepository.findQPagedByKw(
            PostSearchKeywordType1.ALL,
            "제목",
            PageRequest.of(
                0,
                10,
                PostSearchSortType1.ID.sortBy
            ),
        )

        val content = postPage.content

        assertThat(content).isNotEmpty
        assertThat(content).anyMatch { it.title.contains("제목") || it.content.contains("제목") }
    }
}
