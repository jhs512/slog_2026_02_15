package com.back.boundedContexts.member.out

import com.back.boundedContexts.member.domain.Member

interface MemberRepositoryCustom {
    fun findByUsername(username: String): Member?
}
