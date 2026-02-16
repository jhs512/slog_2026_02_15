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

}
