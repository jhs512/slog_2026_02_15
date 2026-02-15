package com.back.boundedContexts.member.domain

import com.back.global.jpa.domain.BaseTime
import com.back.global.jpa.domain.IdSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId

@Entity
@Table(name = "member")
@DynamicUpdate
class Member(
    id: Long = 0,
    @field:NaturalId
    @field:Column(unique = true)
    val username: String,
    var password: String? = null,
    var nickname: String,
    @field:Column(unique = true)
    var apiKey: String,
) : BaseTime(id) {
    companion object : IdSource {
        override val entityName: String = "member"
    }

    fun modifyApiKey(apiKey: String) {
        this.apiKey = apiKey
    }
}
