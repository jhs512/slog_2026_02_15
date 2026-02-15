package com.back.boundedContexts.member.out

import com.back.boundedContexts.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long>, MemberRepositoryCustom {
}
