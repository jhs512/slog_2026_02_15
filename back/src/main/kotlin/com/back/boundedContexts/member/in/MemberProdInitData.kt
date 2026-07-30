package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.domain.shared.Member
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.transaction.annotation.Transactional

@Profile("prod")
@Configuration
class MemberProdInitData(
    private val memberFacade: MemberFacade,
    // 초기 계정을 실제로 만들 때만 필요하다. 회원이 이미 있는 기존 운영 DB에서는
    // 미설정이어도 기동을 막지 않도록 기본값을 비워둔다 (검증은 makeBaseMembers에서).
    @param:Value("\${CUSTOM__PROD_INIT_PASSWORD:}")
    private val initialPassword: String,
) {
    @Lazy
    @Autowired
    private lateinit var self: MemberProdInitData

    @Bean
    @Order(1)
    fun memberProdInitDataApplicationRunner(): ApplicationRunner {
        return ApplicationRunner {
            self.makeBaseMembers()
        }
    }

    @Transactional
    fun makeBaseMembers() {
        if (memberFacade.count() > 0) return

        require(initialPassword.isNotBlank()) {
            "prod 초기 계정을 생성하려면 CUSTOM__PROD_INIT_PASSWORD 환경변수를 설정해야 합니다."
        }

        val memberSystem = memberFacade.join(Member.SYSTEM.username, initialPassword, Member.SYSTEM.nickname)
        memberSystem.modifyApiKey(memberSystem.username)

        val memberHolding = memberFacade.join("holding", initialPassword, "홀딩")
        memberHolding.modifyApiKey(memberHolding.username)

        val memberAdmin = memberFacade.join("admin", initialPassword, "관리자")
        memberAdmin.modifyApiKey(memberAdmin.username)
    }
}
