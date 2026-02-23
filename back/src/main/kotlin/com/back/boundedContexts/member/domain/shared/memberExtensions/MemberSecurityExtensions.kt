package com.back.boundedContexts.member.domain.shared.memberExtensions

import com.back.boundedContexts.member.domain.shared.Member
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

// ================================
// Security 영역
// ================================

val Member.isAdmin: Boolean
    get() = username in setOf("system", "admin")

val Member.authoritiesAsStringList: List<String>
    get() = buildList { if (isAdmin) add("ROLE_ADMIN") }

val Member.authorities: Collection<GrantedAuthority>
    get() = authoritiesAsStringList.map { SimpleGrantedAuthority(it) }
