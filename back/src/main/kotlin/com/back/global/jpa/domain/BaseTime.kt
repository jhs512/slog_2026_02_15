package com.back.global.jpa.domain

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTime(
    id: Long = 0
) : BaseEntity(id) {
    @CreatedDate
    lateinit var createdAt: Instant

    @LastModifiedDate
    lateinit var modifiedAt: Instant

    @Transient
    override fun isNew(): Boolean = !::createdAt.isInitialized

    fun updateModifiedAt() {
        this.modifiedAt = Instant.now()
    }
}
