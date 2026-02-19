package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.QPost.post
import com.back.standard.dto.post.type1.PostSearchKeywordType1
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
    private val queryFactory: JPAQueryFactory,
) : PostRepositoryCustom {

    override fun findQPagedByKw(
        kwType: PostSearchKeywordType1,
        kw: String,
        pageable: Pageable,
    ): Page<Post> = findPosts(null, kwType, kw, pageable)

    override fun findQPagedByAuthorAndKw(
        author: Member,
        kwType: PostSearchKeywordType1,
        kw: String,
        pageable: Pageable,
    ): Page<Post> = findPosts(author, kwType, kw, pageable)

    private fun findPosts(
        author: Member?,
        kwType: PostSearchKeywordType1,
        kw: String,
        pageable: Pageable,
    ): Page<Post> {
        val builder = BooleanBuilder()

        author?.let { builder.and(post.author.eq(it)) }
        if (kw.isNotBlank()) builder.and(buildKwPredicate(kwType, kw))

        val postsQuery = createPostsQuery(builder, pageable)
        val countQuery = createCountQuery(builder)

        return PageableExecutionUtils.getPage(
            postsQuery.fetch(),
            pageable,
        ) { countQuery.fetchOne() ?: 0L }
    }

    private fun buildKwPredicate(kwType: PostSearchKeywordType1, kw: String): BooleanExpression =
        when (kwType) {
            PostSearchKeywordType1.TITLE -> pgroonga(post.title, kw)
            PostSearchKeywordType1.CONTENT -> pgroonga(post.content, kw)
            PostSearchKeywordType1.AUTHOR -> post.author.nickname.containsIgnoreCase(kw)
            PostSearchKeywordType1.ALL -> pgroonga(post.title, kw).or(pgroonga(post.content, kw))
        }

    private fun pgroonga(col: StringPath, kw: String): BooleanExpression =
        Expressions.booleanTemplate(
            "function('pgroonga_match', {0}, {1}) = true",
            col,
            Expressions.constant(kw),
        )

    private fun createPostsQuery(builder: BooleanBuilder, pageable: Pageable): JPAQuery<Post> {
        val query = queryFactory
            .selectFrom(post)
            .where(builder)

        QueryDslUtil.applySorting(query, pageable) { property ->
            when (property) {
                "createdAt" -> post.createdAt
                "modifiedAt" -> post.modifiedAt
                "authorName" -> post.author.nickname
                else -> null
            }
        }

        if (pageable.sort.isEmpty) query.orderBy(post.id.desc())

        return query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
    }

    private fun createCountQuery(builder: BooleanBuilder): JPAQuery<Long> =
        queryFactory
            .select(post.count())
            .from(post)
            .where(builder)
}
