package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.jpa.domain.AfterDDL
import com.back.global.jpa.domain.BaseTime
import com.back.global.pGroonga.annotation.PGroongaIndex
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@AfterDDL(
    """
    CREATE INDEX IF NOT EXISTS post_idx_listed_created_at_desc
    ON post (listed, created_at DESC)
    """
)
@AfterDDL(
    """
    CREATE INDEX IF NOT EXISTS post_idx_listed_modified_at_desc
    ON post (listed, modified_at DESC)
    """
)
@AfterDDL(
    """
    CREATE INDEX IF NOT EXISTS post_idx_author_created_at_desc
    ON post (author_id, created_at DESC)
    """
)
@AfterDDL(
    """
    CREATE INDEX IF NOT EXISTS post_idx_author_modified_at_desc
    ON post (author_id, modified_at DESC)
    """
)
@PGroongaIndex(columns = ["title", "content"])
@SequenceGenerator(name = "entity_seq_gen", sequenceName = "post_seq", allocationSize = 1)
class Post(
    @field:ManyToOne(fetch = FetchType.LAZY)
    val author: Member,
    @field:Column(columnDefinition = "TEXT")
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
