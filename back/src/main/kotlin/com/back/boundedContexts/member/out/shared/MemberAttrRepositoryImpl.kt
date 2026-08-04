package com.back.boundedContexts.member.out.shared

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.domain.shared.MemberAttr
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.KeyType
import org.hibernate.Session

class MemberAttrRepositoryImpl : MemberAttrRepositoryCustom {
    @field:PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findBySubjectAndName(subject: Member, name: String): MemberAttr? {
        return entityManager.unwrap(Session::class.java)
            .find(
                MemberAttr::class.java,
                mapOf(MemberAttr::subject.name to subject, MemberAttr::name.name to name),
                KeyType.NATURAL,
            )
    }
}
