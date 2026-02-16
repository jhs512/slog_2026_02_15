package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.domain.Member
import com.back.boundedContexts.member.out.MemberRepository
import com.back.boundedContexts.post.domain.Post
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
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Test
    @DisplayName("검색어로 제목 또는 내용에서 조회")
    fun `성공 - 검색어 기반 다건 조회`() {
        val author = saveMember("author-title-body")
        val titleMatch = savePost(author, "제목 키워드 매치", "일반 내용")
        val bodyMatch = savePost(author, "일반 제목", "내용에 키워드가 포함")
        val notMatch = savePost(author, "일반 제목", "일반 내용")

        val posts = postRepository.findByKeyword("키워드", PageRequest.of(0, 10)).content
        val ids = posts.map { it.id }

        assertThat(ids).contains(titleMatch.id, bodyMatch.id)
        assertThat(ids).doesNotContain(notMatch.id)
    }

    @Test
    @DisplayName("검색어로 OR 조건 조회")
    fun `성공 - OR 조건 검색`() {
        val author = saveMember("author-or")
        val orFirstMatch = savePost(author, "코끼리 이야기", "평범한 내용")
        val orSecondMatch = savePost(author, "평범한 제목", "호랑이 사냥 기록")
        val notMatch = savePost(author, "강아지", "고양이")

        val posts = postRepository.findByKeyword("코끼리 OR 호랑이", PageRequest.of(0, 10)).content
        val ids = posts.map { it.id }

        assertThat(ids).contains(orFirstMatch.id, orSecondMatch.id)
        assertThat(ids).doesNotContain(notMatch.id)
    }

    @Test
    @DisplayName("검색어로 AND 조건 조회")
    fun `성공 - AND 조건 검색`() {
        val author = saveMember("author-and")
        val andMatch = savePost(author, "안드로이드 가이드", "개발 환경을 정리한 내용")
        val titleOnlyMatch = savePost(author, "안드로이드 입문", "일반 글")
        val bodyOnlyMatch = savePost(author, "개발 일지", "일반 글")

        val posts = postRepository.findByKeyword("안드로이드 AND 개발", PageRequest.of(0, 10)).content
        val ids = posts.map { it.id }

        assertThat(ids).contains(andMatch.id)
        assertThat(ids).doesNotContain(titleOnlyMatch.id, bodyOnlyMatch.id)
    }

    @Test
    @DisplayName("검색어로 부분어(bigram) 조회")
    fun `성공 - 부분어 검색`() {
        val author = saveMember("author-bigram")
        val target = savePost(author, "검색 엔진 인덱스", "빅램 기반 문자열 색인")
        val notMatch = savePost(author, "검색기", "일반 글")

        val posts = postRepository.findByKeyword("엔진", PageRequest.of(0, 10)).content
        val ids = posts.map { it.id }

        assertThat(ids).contains(target.id)
        assertThat(ids).doesNotContain(notMatch.id)
    }

    private fun saveMember(nameSuffix: String): Member {
        return memberRepository.save(
            Member(
                id = Member.newId(),
                username = "user-$nameSuffix",
                password = "1234",
                nickname = "유저-$nameSuffix",
                apiKey = "key-$nameSuffix",
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
