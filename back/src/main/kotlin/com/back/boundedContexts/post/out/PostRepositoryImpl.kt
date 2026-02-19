package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageImpl

class PostRepositoryImpl(
    @field:PersistenceContext
    private val entityManager: EntityManager
) : PostRepositoryCustom {
    override fun findQPagedByAuthorAndKw(author: Member, kw: String, pageable: Pageable): Page<Post> {
        return findPosts(author, kw, pageable)
    }

    override fun findQPagedByKw(kw: String, pageable: Pageable): Page<Post> {
        return findPosts(null, kw, pageable)
    }

    private fun findPosts(author: Member?, kw: String, pageable: Pageable): Page<Post> {
        val filterClauses = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()

        if (author != null) {
            filterClauses.add("p.author_id = :authorId")
            parameters["authorId"] = author.id
        }

        if (kw.isNotBlank()) {
            filterClauses.add("(p.title &@~ :kw OR p.content &@~ :kw)")
            parameters["kw"] = kw
        }

        val whereClause = if (filterClauses.isEmpty()) "" else " WHERE ${filterClauses.joinToString(" AND ")}"

        val sort = buildSortClause(pageable)

        val querySql = buildString {
            append("SELECT p.* FROM post p")
            if (sort.requiresAuthorJoin) append(" INNER JOIN member m ON m.id = p.author_id")
            append(whereClause)
            append(" ORDER BY ").append(sort.sql)
        }

        val query = createNativeQuery(querySql)
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)

        applyParameters(query, parameters)

        @Suppress("UNCHECKED_CAST")
        val content = query.resultList as List<Post>

        val countSql = buildString {
            append("SELECT COUNT(*) FROM post p")
            append(whereClause)
        }
        val countQuery = createNativeQuery(countSql, false)
        applyParameters(countQuery, parameters)
        val total = (countQuery.singleResult as Number).toLong()

        return PageImpl(content, pageable, total)
    }

    private fun createNativeQuery(sql: String, withEntity: Boolean = true): Query =
        if (withEntity) {
            entityManager.createNativeQuery(sql, Post::class.java)
        } else {
            entityManager.createNativeQuery(sql)
        }

    private fun applyParameters(query: Query, parameters: Map<String, Any>) {
        parameters.forEach { (name, value) ->
            query.setParameter(name, value)
        }
    }

    private data class SortClause(
        val sql: String,
        val requiresAuthorJoin: Boolean = false,
    )

    private fun buildSortClause(pageable: Pageable): SortClause {
        val order = pageable.sort.firstOrNull() ?: return SortClause("p.id DESC")

        val orderBySql = order.propertyToSql()
        val direction = if (order.isAscending) "ASC" else "DESC"

        return when {
            order.property == "authorName" -> SortClause("m.nickname $direction, p.id DESC", true)
            else -> SortClause("p.$orderBySql $direction")
        }
    }

    private fun org.springframework.data.domain.Sort.Order.propertyToSql(): String =
        when (property) {
            "createdAt" -> "created_at"
            "modifiedAt" -> "modified_at"
            else -> property
        }
}
