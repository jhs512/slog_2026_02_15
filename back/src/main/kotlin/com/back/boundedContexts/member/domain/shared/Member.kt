package com.back.boundedContexts.member.domain.shared

import com.back.boundedContexts.member.out.shared.MemberAttrRepository
import com.back.boundedContexts.post.domain.PostMember
import com.back.global.app.app.AppFacade
import com.back.global.jpa.domain.BaseTime
import com.back.global.pGroonga.annotation.PGroongaIndex
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId
import java.util.*

private const val PROFILE_IMG_URL = "profileImgUrl"

@Entity
@DynamicUpdate
@Table(indexes = [
    Index(name = "idx_member_created_at", columnList = "created_at"),
])
@PGroongaIndex(columns = ["username", "nickname"])
class Member(
    id: Int,
    @field:NaturalId
    @field:Column(unique = true)
    val username: String,
    var password: String? = null,
    var nickname: String,
    @field:Column(unique = true)
    var apiKey: String,
) : BaseTime(id), PostMember {

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
    // 인터페이스(PostMember 등) 구현을 위한 속성
    // ================================
    override val member: Member get() = this

    // ================================
    // 가상 속성 (Profile)
    // ================================
    override val name: String
        get() = nickname

    private val profileImgUrlAttr: MemberAttr
        get() = getOrPutAttr(PROFILE_IMG_URL) {
            attrRepository.findBySubjectAndName(this, PROFILE_IMG_URL)
                ?: MemberAttr(this, PROFILE_IMG_URL, "")
        }

    var profileImgUrl: String
        get() = profileImgUrlAttr.value
        set(value) {
            profileImgUrlAttr.value = value
            attrRepository.save(profileImgUrlAttr)
        }

    val profileImgUrlOrDefault: String
        get() = profileImgUrl
            .takeIf { it.isNotBlank() }
            ?: "https://placehold.co/600x600?text=U_U"

    val redirectToProfileImgUrlOrDefault: String
        get() = "${AppFacade.siteBackUrl}/member/api/v1/members/${id}/redirectToProfileImg"

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
