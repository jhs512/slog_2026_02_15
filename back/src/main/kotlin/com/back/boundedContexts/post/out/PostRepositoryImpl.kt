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
            val searchClause = buildSearchClause(kw)
            filterClauses.add(searchClause.sql)
            parameters.putAll(searchClause.params)
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

    private fun buildSearchClause(rawKw: String): SearchClause {
        val tokens = rawKw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return SearchClause("1 = 1", emptyMap())

        val hasFieldToken = tokens.any { FIELD_FILTER.matches(it) }
        if (!hasFieldToken) {
            return SearchClause("(p.title &@~ :kw OR p.content &@~ :kw)", mapOf("kw" to rawKw))
        }

        val orGroups = tokens.fold(mutableListOf<MutableList<String>>(mutableListOf())) { groups, token ->
            if (token.equals("OR", ignoreCase = true)) {
                groups.add(mutableListOf())
            } else {
                groups.last().add(token)
            }
            groups
        }.filter { it.isNotEmpty() }

        val params = linkedMapOf<String, Any>()
        var paramIndex = 0

        val orClauses = orGroups.map { group ->
            val andClauses = group.map { token ->
                val fieldMatch = FIELD_FILTER.matchEntire(token)
                val paramName = "kw${paramIndex++}"

                if (fieldMatch == null) {
                    params[paramName] = token
                    "(p.title &@~ :$paramName OR p.content &@~ :$paramName)"
                } else {
                    val sign = fieldMatch.groupValues[1]
                    val fieldName = fieldMatch.groupValues[2]
                    val term = "$sign${fieldMatch.groupValues[3]}"
                    params[paramName] = term

                    val targetColumn = when (fieldName) {
                        "title" -> "p.title"
                        "content" -> "p.content"
                        else -> "p.title"
                    }

                    "$targetColumn &@~ :$paramName"
                }
            }

            if (andClauses.size == 1) andClauses[0] else andClauses.joinToString(prefix = "(", postfix = ")", separator = " AND ")
        }

        val sql = if (orClauses.size == 1) orClauses[0] else orClauses.joinToString(prefix = "(", postfix = ")", separator = " OR ")
        return SearchClause(sql, params)
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

    private data class SearchClause(
        val sql: String,
        val params: Map<String, Any>,
    )

    private companion object {
        private val FIELD_FILTER = Regex("^([+-]?)(title|content):(.+)$")
    }
}
