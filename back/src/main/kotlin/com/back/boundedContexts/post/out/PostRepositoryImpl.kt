package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.QPost.post
import com.back.standard.util.QueryDslUtil
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {
    override fun findQPagedByAuthorAndKw(author: Member, kw: String, pageable: Pageable): Page<Post> {
        val builder = BooleanBuilder()
        builder.and(post.author.eq(author))
        applyKwFilter(builder, kw)

        return findPosts(pageable, builder)
    }

    override fun findQPagedByKw(kw: String, pageable: Pageable): Page<Post> {
        val builder = BooleanBuilder()
        applyKwFilter(builder, kw)

        return findPosts(pageable, builder)
    }

    private fun applyKwFilter(builder: BooleanBuilder, kw: String) {
        if (kw.isBlank()) return
        // PGroonga 쿼리 문법을 그대로 전달해 title/content 구분과 AND/OR/NOT 동작을 위임
        builder.and(pgroongaMatch(post.title, kw).or(pgroongaMatch(post.content, kw)))
    }

    private fun findPosts(pageable: Pageable, builder: BooleanBuilder): Page<Post> {
        val query = buildPostListQuery().where(builder)

        QueryDslUtil.applySorting(query, pageable) { property ->
            when (property) {
                "createdAt" -> post.createdAt
                "modifiedAt" -> post.modifiedAt
                "authorName" -> post.author.nickname
                else -> null
            }
        }

        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val totalQuery = queryFactory
            .select(post.count())
            .from(post)
            .where(builder)

        return PageableExecutionUtils.getPage(results, pageable) {
            totalQuery.fetchFirst() ?: 0L
        }
    }

    /** PGroonga &@~ 연산자를 QueryDSL 표현식으로 래핑 */
    private fun pgroongaMatch(field: StringPath, query: String): BooleanExpression =
        Expressions.booleanTemplate("({0} &@~ {1})", field, query)

    private fun buildPostListQuery(): JPAQuery<Post> = queryFactory
        .selectFrom(post)
        .leftJoin(post.author).fetchJoin()
        .leftJoin(post.likesCountAttr).fetchJoin()
        .leftJoin(post.commentsCountAttr).fetchJoin()
        .leftJoin(post.hitCountAttr).fetchJoin()
}
