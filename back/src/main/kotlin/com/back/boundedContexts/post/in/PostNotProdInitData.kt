package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.member.out.MemberRepository
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.Post
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.transaction.annotation.Transactional

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

@Profile("!prod")
@Configuration
class PostNotProdInitData(
    private val postFacade: PostFacade,
    private val memberRepository: MemberRepository,
) {
    @Lazy
    @Autowired
    private lateinit var self: PostNotProdInitData

    @Bean
    @Order(2)
    fun postNotProdInitDataApplicationRunner(): ApplicationRunner {
        return ApplicationRunner {
            self.makeSamplePosts()
        }
    }

    @Transactional
    fun makeSamplePosts() {
        if (postFacade.count() > 0) return

        val user1 = memberRepository.findByUsername("user1")!!
        val user2 = memberRepository.findByUsername("user2")!!
        val user3 = memberRepository.findByUsername("user3")!!

        postFacade.write(Post.newId(), user1, "첫 번째 글", "안녕하세요, 첫 번째 글입니다.")
        postFacade.write(Post.newId(), user1, "두 번째 글", "오늘 날씨가 좋네요.")
        postFacade.write(Post.newId(), user2, "세 번째 글", "Spring Boot 4가 출시되었습니다.")
        postFacade.write(Post.newId(), user2, "네 번째 글", "Kotlin과 JPA를 함께 사용하기.")
        postFacade.write(Post.newId(), user3, "다섯 번째 글", "PostgreSQL 성능 최적화 팁을 공유합니다.")
        postFacade.write(Post.newId(), user3, "여섯 번째 글", "개발자의 하루 일과를 소개합니다.")
    }

    @Transactional
    fun makeSearchFixturePosts(): SearchFixturePosts {
        val user1 = memberRepository.findByUsername("user1")!!
        val user2 = memberRepository.findByUsername("user2")!!

        val titleMatch = postFacade.write(Post.newId(), user1, "제목 키워드 매치", "일반 내용")
        val bodyMatch = postFacade.write(Post.newId(), user1, "일반 제목", "내용에 키워드가 포함")
        val notMatch = postFacade.write(Post.newId(), user1, "일반 제목", "일반 내용")
        val orFirstMatch = postFacade.write(Post.newId(), user1, "코끼리 이야기", "평범한 내용")
        val orSecondMatch = postFacade.write(Post.newId(), user1, "평범한 제목", "호랑이 사냥 기록")
        val orNotMatch = postFacade.write(Post.newId(), user1, "강아지", "고양이")
        val andMatch = postFacade.write(Post.newId(), user2, "안드로이드 가이드", "개발 환경을 정리한 내용")
        val andNotTitleOnly = postFacade.write(Post.newId(), user2, "안드로이드 입문", "일반 본문")
        val andNotBodyOnly = postFacade.write(Post.newId(), user2, "개발 일지", "일반 본문")
        val bigramTarget = postFacade.write(Post.newId(), user2, "검색 엔진 인덱스", "빅램 기반 문자열 색인")
        val bigramNotMatch = postFacade.write(Post.newId(), user2, "색인 엔진", "일반 글")

        return SearchFixturePosts(
            titleMatch,
            bodyMatch,
            notMatch,
            orFirstMatch,
            orSecondMatch,
            orNotMatch,
            andMatch,
            andNotTitleOnly,
            andNotBodyOnly,
            bigramTarget,
            bigramNotMatch,
        )
    }
}
