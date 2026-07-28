package com.back.global.jpa.domain

import jakarta.persistence.*
import org.hibernate.Hibernate

@MappedSuperclass
abstract class BaseEntity(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0
) {
    @Transient
    private val attrCache: MutableMap<String, Any> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPutAttr(key: String, defaultValue: () -> T): T =
        attrCache.getOrPut(key, defaultValue) as T

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BaseEntity) return false

        // Hibernate 프록시와 실체, 그리고 MemberProxy 같은 상속 래퍼를 동일 취급한다
        val thisClass = Hibernate.getClass(this)
        val otherClass = Hibernate.getClass(other)
        if (!thisClass.isAssignableFrom(otherClass) && !otherClass.isAssignableFrom(thisClass)) return false

        // 미영속(id=0) 엔티티는 동등성 비교 불가
        if (id == 0 || other.id == 0) return false

        return id == other.id
    }

    override fun hashCode(): Int =
        if (id != 0) id.hashCode()
        else super.hashCode()
}
