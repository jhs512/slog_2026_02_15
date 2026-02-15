package com.back.global.jpa.domain

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable

@MappedSuperclass
abstract class BaseEntity(
    @field:Id
    val id: Long = 0
) : Persistable<Long> {

    @Transient
    private val attrCache: MutableMap<String, Any> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPutAttr(key: String, defaultValue: () -> T): T =
        attrCache.getOrPut(key, defaultValue) as T

    override fun getId(): Long = id

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BaseEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
