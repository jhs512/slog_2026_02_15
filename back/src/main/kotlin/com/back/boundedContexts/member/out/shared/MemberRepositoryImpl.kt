package com.back.boundedContexts.member.out.shared

import com.back.boundedContexts.member.domain.shared.Member
import com.back.standard.dto.member.type1.MemberSearchKeywordType1
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class MemberRepositoryImpl : MemberRepositoryCustom {
    @field:PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findByUsername(username: String): Member? {
        return entityManager
            .createNativeQuery("SELECT * FROM member WHERE username = :username", Member::class.java)
            .setParameter("username", username)
            .resultList
            .firstOrNull() as? Member
    }

    override fun findQPagedByKw(
        kwType: MemberSearchKeywordType1,
        kw: String,
        pageable: Pageable
    ): Page<Member> {
        val searchClause = buildWhereClause(kwType, kw)

        val members = findMembersByCriteria(searchClause, pageable)
        val totalCount = countMembersByCriteria(searchClause)

        return PageImpl(members, pageable, totalCount)
    }

    private fun findMembersByCriteria(searchClause: SearchClause?, pageable: Pageable): List<Member> {
        val orderBy = pageable.orderBySql()

        val sql = buildString {
            append("SELECT * FROM member")
            searchClause?.whereClause?.let { append(" WHERE ").append(it) }
            if (orderBy.isNotBlank()) append(" ORDER BY ").append(orderBy)
        }

        val query = entityManager
            .createNativeQuery(sql, Member::class.java)
            .apply {
                searchClause?.keywordParameters?.forEach { (name, value) ->
                    setParameter(name, value)
                }
            }
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)

        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<Member>
    }

    private fun countMembersByCriteria(searchClause: SearchClause?): Long {
        val sql = buildString {
            append("SELECT COUNT(*) FROM member")
            searchClause?.whereClause?.let { append(" WHERE ").append(it) }
        }

        val query = entityManager.createNativeQuery(sql)
        searchClause?.keywordParameters?.forEach { (name, value) ->
            query.setParameter(name, value)
        }

        return (query.singleResult as Number).toLong()
    }

    private fun buildWhereClause(kwType: MemberSearchKeywordType1, kw: String): SearchClause? {
        if (kw.isBlank()) return null

        val operator = when {
            kw.contains(" AND ") -> SearchOperator.AND
            kw.contains(" OR ") -> SearchOperator.OR
            else -> SearchOperator.NONE
        }

        val terms = when (operator) {
            SearchOperator.NONE -> listOf(kw)
            SearchOperator.OR -> kw.split(" OR ").filter { it.isNotBlank() }
            SearchOperator.AND -> kw.split(" AND ").filter { it.isNotBlank() }
        }

        if (terms.isEmpty()) return null

        return when (operator) {
            SearchOperator.NONE -> buildSingleClause(kwType, terms.first())
            SearchOperator.OR -> buildGroupClause(kwType, terms, "OR")
            SearchOperator.AND -> buildGroupClause(kwType, terms, "AND")
        }
    }

    private fun buildSingleClause(kwType: MemberSearchKeywordType1, term: String): SearchClause {
        val targetColumns = targetColumns(kwType)

        return when (kwType) {
            MemberSearchKeywordType1.USERNAME, MemberSearchKeywordType1.NICKNAME, MemberSearchKeywordType1.ALL ->
                SearchClause(buildLikeClause(targetColumns, "kw0"), mapOf("kw0" to likePattern(term)))
        }
    }

    private fun buildGroupClause(
        kwType: MemberSearchKeywordType1,
        terms: List<String>,
        separator: String
    ): SearchClause {
        val clauses = mutableListOf<String>()
        val parameters = linkedMapOf<String, Any>()
        val targetColumns = targetColumns(kwType)

        terms.forEachIndexed { index, term ->
            val position = index + 1
            val clause = buildLikeClause(targetColumns, "kw$position")
            clauses.add("($clause)")
            parameters["kw$position"] = likePattern(term)
        }

        val sql = clauses.joinToString(" $separator ") { it }
        val safeSql = if (terms.size > 1) "($sql)" else sql

        return SearchClause(safeSql, parameters)
    }

    private fun targetColumns(kwType: MemberSearchKeywordType1): List<String> =
        when (kwType) {
            MemberSearchKeywordType1.USERNAME -> listOf("username")
            MemberSearchKeywordType1.NICKNAME -> listOf("nickname")
            MemberSearchKeywordType1.ALL -> listOf("username", "nickname")
        }

    private fun buildLikeClause(columns: List<String>, paramName: String): String {
        val clause = columns.joinToString(" OR ") { "$it LIKE :$paramName ESCAPE '\\'" }
        return if (columns.size == 1) clause else "($clause)"
    }

    private fun likePattern(term: String): String {
        val escaped = term
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }

    private fun Pageable.orderBySql(): String {
        return this.sort
            .asSequence()
            .mapNotNull { order ->
                val column = when (order.property) {
                    "createdAt" -> "created_at"
                    "id" -> "id"
                    "username" -> "username"
                    "nickname" -> "nickname"
                    else -> null
                }
                val direction = if (order.isAscending) "ASC" else "DESC"
                "$column $direction"
            }
            .joinToString(", ")
            .ifBlank { "id DESC" }
    }

    private data class SearchClause(
        val whereClause: String,
        val keywordParameters: Map<String, Any>,
    )

    private enum class SearchOperator {
        NONE,
        AND,
        OR,
    }

}
