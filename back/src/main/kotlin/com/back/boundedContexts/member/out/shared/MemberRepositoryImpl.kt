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
                searchClause?.keywordParameters?.forEachIndexed { index, keyword ->
                    setParameter(index + 1, keyword)
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
        searchClause?.keywordParameters?.forEachIndexed { index, keyword ->
            query.setParameter(index + 1, keyword)
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
        return when (kwType) {
            MemberSearchKeywordType1.USERNAME -> SearchClause("username &@~ ?1", listOf(term))
            MemberSearchKeywordType1.NICKNAME -> SearchClause("nickname &@~ ?1", listOf(term))
            MemberSearchKeywordType1.ALL -> SearchClause("(username &@~ ?1 OR nickname &@~ ?1)", listOf(term))
        }
    }

    private fun buildGroupClause(
        kwType: MemberSearchKeywordType1,
        terms: List<String>,
        separator: String
    ): SearchClause {
        val clauses = mutableListOf<String>()
        val parameters = mutableListOf<String>()

        terms.forEachIndexed { index, term ->
            val position = index + 1
            val clause = when (kwType) {
                MemberSearchKeywordType1.USERNAME -> "$USERNAME_SEARCH_KEY$position"
                MemberSearchKeywordType1.NICKNAME -> "$NICKNAME_SEARCH_KEY$position"
                MemberSearchKeywordType1.ALL -> "$USERNAME_SEARCH_KEY$position OR $NICKNAME_SEARCH_KEY$position"
            }
            clauses.add("($clause)")
            parameters.add(term)
        }

        val sql = clauses.joinToString(" $separator ") { it }
        val safeSql = if (terms.size > 1) "($sql)" else sql

        return SearchClause(safeSql, parameters)
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
        val keywordParameters: List<String>,
    )

    private enum class SearchOperator {
        NONE,
        AND,
        OR,
    }

    private companion object {
        const val USERNAME_SEARCH_KEY = "username &@~ ?"
        const val NICKNAME_SEARCH_KEY = "nickname &@~ ?"
    }
}
