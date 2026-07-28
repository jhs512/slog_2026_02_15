package com.back.boundedContexts.member.subContexts.memberActionLog.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.jpa.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@SequenceGenerator(name = "entity_seq_gen", sequenceName = "member_action_log_seq", allocationSize = 1)
class MemberActionLog(
    val type: String,
    val primaryType: String,
    val primaryId: Int,
    @field:ManyToOne(fetch = FetchType.LAZY) val primaryOwner: Member,
    val secondaryType: String,
    val secondaryId: Int,
    @field:ManyToOne(fetch = FetchType.LAZY) val secondaryOwner: Member,
    @field:ManyToOne(fetch = FetchType.LAZY) val actor: Member,
    @field:Column(columnDefinition = "TEXT") val data: String,
) : BaseEntity()
