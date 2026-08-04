package com.back.boundedContexts.post.out

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.KeyType
import org.hibernate.Session

class PostAttrRepositoryImpl : PostAttrRepositoryCustom {
    @field:PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findBySubjectAndName(subject: Post, name: String): PostAttr? {
        return entityManager.unwrap(Session::class.java)
            .find(
                PostAttr::class.java,
                mapOf(PostAttr::subject.name to subject, PostAttr::name.name to name),
                KeyType.NATURAL,
            )
    }
}
