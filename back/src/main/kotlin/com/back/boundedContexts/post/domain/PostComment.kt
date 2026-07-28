package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@SequenceGenerator(name = "entity_seq_gen", sequenceName = "post_comment_seq", allocationSize = 1)
class PostComment(
    @field:ManyToOne(fetch = FetchType.LAZY)
    val author: Member,
    @field:ManyToOne(fetch = FetchType.LAZY)
    val post: Post,
    var content: String,
) : BaseTime() {
    fun modify(content: String) {
        this.content = content
    }
}
