package com.back.boundedContexts.member.out

import com.back.boundedContexts.member.domain.Member
import jakarta.persistence.EntityManager
import org.hibernate.Session

class MemberRepositoryImpl(
    private val entityManager: EntityManager,
) : MemberRepositoryCustom {
    override fun findByUsername(username: String): Member? {
        return entityManager.unwrap(Session::class.java)
            .byNaturalId(Member::class.java)
            .using(Member::username.name, username)
            .load()
    }
}
