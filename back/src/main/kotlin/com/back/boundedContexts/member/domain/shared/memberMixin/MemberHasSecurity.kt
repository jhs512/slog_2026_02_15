package com.back.boundedContexts.member.domain.shared.memberMixin

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

// 확장 프로퍼티는 정적 디스패치라 MemberProxy에서 override할 수 없다.
// 인터페이스 믹스인으로 두면 필요 시 하위 클래스(프록시)가 동적으로 재정의할 수 있다.
interface MemberHasSecurity : MemberAware {
    val authoritiesAsStringList: List<String>
        get() = buildList {
            if (member.isAdmin) add("ROLE_ADMIN")
        }

    val authorities: Collection<GrantedAuthority>
        get() = authoritiesAsStringList.map(::SimpleGrantedAuthority)
}
