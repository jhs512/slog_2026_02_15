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
    // 초기 비밀번호 전용 환경변수. 기본값 없음 — prod에서 미설정 시 기동이 명확히 실패한다.
    @param:Value("\${CUSTOM__PROD_INIT_PASSWORD}")
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

        val memberSystem = memberFacade.join(Member.SYSTEM.username, initialPassword, Member.SYSTEM.nickname)
        memberSystem.modifyApiKey(memberSystem.username)

        val memberHolding = memberFacade.join("holding", initialPassword, "홀딩")
        memberHolding.modifyApiKey(memberHolding.username)

        val memberAdmin = memberFacade.join("admin", initialPassword, "관리자")
        memberAdmin.modifyApiKey(memberAdmin.username)
    }
}
