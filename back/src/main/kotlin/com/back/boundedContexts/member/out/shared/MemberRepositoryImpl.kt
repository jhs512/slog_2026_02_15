package com.back.boundedContexts.member.out.shared

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.domain.shared.QMember.member
import com.back.standard.dto.member.type1.MemberSearchKeywordType1
import com.back.standard.util.QueryDslUtil
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class MemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : MemberRepositoryCustom {

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
        pageable: Pageable,
    ): Page<Member> {
        val builder = BooleanBuilder()

        if (kw.isNotBlank()) builder.and(buildKwPredicate(kwType, kw))

        val itemsQuery = createItemsQuery(builder, pageable)
        val countQuery = createCountQuery(builder)

        return PageableExecutionUtils.getPage(
            itemsQuery.fetch(),
            pageable,
        ) { countQuery.fetchOne() ?: 0L }
    }

    private fun buildKwPredicate(kwType: MemberSearchKeywordType1, kw: String): com.querydsl.core.types.Predicate {
        val cols = targetCols(kwType)

        return when {
            kw.contains(" AND ") -> {
                val terms = kw.split(" AND ").filter { it.isNotBlank() }
                val andBuilder = BooleanBuilder()
                terms.forEach { term -> andBuilder.and(colsContain(cols, term)) }
                andBuilder
            }
            kw.contains(" OR ") -> {
                val terms = kw.split(" OR ").filter { it.isNotBlank() }
                val orBuilder = BooleanBuilder()
                terms.forEach { term -> orBuilder.or(colsContain(cols, term)) }
                orBuilder
            }
            else -> colsContain(cols, kw)
        }
    }

    private fun targetCols(kwType: MemberSearchKeywordType1): List<StringPath> =
        when (kwType) {
            MemberSearchKeywordType1.USERNAME -> listOf(member.username)
            MemberSearchKeywordType1.NICKNAME -> listOf(member.nickname)
            MemberSearchKeywordType1.ALL -> listOf(member.username, member.nickname)
        }

    private fun colsContain(cols: List<StringPath>, term: String): com.querydsl.core.types.Predicate {
        val orBuilder = BooleanBuilder()
        cols.forEach { col -> orBuilder.or(col.containsIgnoreCase(term)) }
        return orBuilder
    }

    private fun createItemsQuery(builder: BooleanBuilder, pageable: Pageable): JPAQuery<Member> {
        val query = queryFactory
            .selectFrom(member)
            .where(builder)

        QueryDslUtil.applySorting(query, pageable) { property ->
            when (property) {
                "createdAt" -> member.createdAt
                "username" -> member.username
                "nickname" -> member.nickname
                else -> null
            }
        }

        if (pageable.sort.isEmpty) query.orderBy(member.id.desc())

        return query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
    }

    private fun createCountQuery(builder: BooleanBuilder): JPAQuery<Long> =
        queryFactory
            .select(member.count())
            .from(member)
            .where(builder)
}
