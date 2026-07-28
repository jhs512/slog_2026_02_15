package com.back.boundedContexts.member.domain.shared

import com.back.boundedContexts.member.domain.shared.memberMixin.MemberHasProfileImgUrl
import com.back.boundedContexts.member.domain.shared.memberMixin.MemberHasSecurity
import com.back.boundedContexts.member.out.shared.MemberAttrRepository
import com.back.boundedContexts.post.domain.PostMember
import com.back.global.jpa.domain.AfterDDL
import com.back.global.jpa.domain.BaseTime
import com.back.global.pGroonga.annotation.PGroongaIndex
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.SequenceGenerator
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId
import java.util.*

@Entity
@DynamicUpdate
@AfterDDL(
    """
    CREATE INDEX IF NOT EXISTS member_idx_created_at_desc
    ON member (created_at DESC)
    """
)
@AfterDDL(
    """
    CREATE INDEX IF NOT EXISTS member_idx_modified_at_desc
    ON member (modified_at DESC)
    """
)
@PGroongaIndex(columns = ["username", "nickname"])
@SequenceGenerator(name = "entity_seq_gen", sequenceName = "member_seq", allocationSize = 1)
class Member(
    id: Int,
    @field:NaturalId
    @field:Column(unique = true)
    val username: String,
    var password: String? = null,
    var nickname: String,
    @field:Column(unique = true)
    var apiKey: String,
) : BaseTime(id), PostMember, MemberHasSecurity, MemberHasProfileImgUrl {

    // ================================
    // Companion Object
    // ================================

    companion object {
        lateinit var attrRepository_: MemberAttrRepository
        val attrRepository by lazy { attrRepository_ }

        val SYSTEM = Member(1, "system", "시스템")

        fun genApiKey() = UUID.randomUUID().toString()
    }

    // ================================
    // Constructors
    // ================================

    constructor(id: Int) : this(id, "", "")

    constructor(id: Int, username: String, nickname: String) : this(
        id,
        username,
        null,
        nickname,
        ""
    )

    constructor(username: String, password: String?, nickname: String) : this(
        0,
        username,
        password,
        nickname,
        genApiKey(),
    )

    // ================================
    // 인터페이스(PostMember, memberMixin 등) 구현을 위한 속성
    // ================================
    override val member: Member get() = this

    override val name: String
        get() = nickname

    val isAdmin: Boolean
        get() = username in setOf("system", "admin")

    // ================================
    // Member 전용 메서드
    // ================================

    fun modify(nickname: String, profileImgUrl: String?) {
        this.nickname = nickname
        profileImgUrl?.let { this.profileImgUrl = it }
    }

    fun modifyApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

}
