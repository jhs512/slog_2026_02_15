package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.jpa.domain.BaseTime
import com.back.global.pgroonga.annotation.PGroongaIndex
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(
    indexes = [
        Index(name = "idx_post_listed_created_at", columnList = "listed, created_at"),
        Index(name = "idx_post_listed_modified_at", columnList = "listed, modified_at"),
        Index(name = "idx_post_author_created_at", columnList = "author_id, created_at"),
        Index(name = "idx_post_author_modified_at", columnList = "author_id, modified_at"),
    ]
)
@PGroongaIndex(columns = ["title"])
@PGroongaIndex(columns = ["content"])
class Post(
    @field:ManyToOne(fetch = FetchType.LAZY)
    val author: Member,
    var title: String,
    @Basic(fetch = FetchType.LAZY)
    @field:Column(columnDefinition = "TEXT")
    var content: String,
    var published: Boolean = false,
    var listed: Boolean = false,
) : BaseTime() {
    @field:OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    var likesCountAttr: PostAttr? = null

    @field:OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    var commentsCountAttr: PostAttr? = null

    @field:OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    var hitCountAttr: PostAttr? = null

    // ================================
    // Companion Object
    // ================================

    companion object {
        // Attr 이름 상수
        const val LIKES_COUNT = "likesCount"
        const val COMMENTS_COUNT = "commentsCount"
        const val HIT_COUNT = "hitCount"
    }

    fun modify(title: String, content: String, published: Boolean? = null, listed: Boolean? = null) {
        this.title = title
        this.content = content
        published?.let { this.published = it }
        listed?.let { this.listed = it }
        if (!this.published) this.listed = false
    }
}
