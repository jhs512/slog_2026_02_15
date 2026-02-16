package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.Member
import com.back.global.jpa.domain.BaseTime
import com.back.global.jpa.domain.IdSource
import com.back.global.pgroonga.annotation.PGroongaIndex
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@Table(name = "post")
@DynamicUpdate
@PGroongaIndex(columns = ["title", "body"])
class Post(
    id: Long,
    @field:ManyToOne(fetch = FetchType.LAZY)
    val author: Member,
    var title: String,
    @field:Column(columnDefinition = "TEXT")
    var body: String,
    var published: Boolean = true,
) : BaseTime(id) {
    companion object : IdSource {
        override val entityName: String = "post"
    }
}
