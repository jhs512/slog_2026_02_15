package com.back.boundedContexts.member.app

import com.back.boundedContexts.member.domain.Member
import com.back.boundedContexts.member.out.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class MemberFacade(
    private val memberRepository: MemberRepository,
) {
    @Transactional(readOnly = true)
    fun count(): Long = memberRepository.count()

    @Transactional
    fun join(id: Long, username: String, password: String?, nickname: String): Member {
        memberRepository.findByUsername(username)
            ?.let { throw IllegalArgumentException("이미 존재하는 username 입니다: $username") }

        return memberRepository.save(
            Member(
                id,
                username,
                password,
                nickname,
                UUID.randomUUID().toString()
            )
        )
    }
}
