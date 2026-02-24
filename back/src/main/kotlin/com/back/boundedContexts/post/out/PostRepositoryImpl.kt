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
    ): Page<Post> = findPosts(null, kwType, kw, pageable, publicOnly = true)

    override fun findQPagedByAuthorAndKw(
        author: Member,
        kwType: PostSearchKeywordType1,
        kw: String,
        pageable: Pageable,
    ): Page<Post> = findPosts(author, kwType, kw, pageable, publicOnly = false)

    private fun findPosts(
        author: Member?,
        kwType: PostSearchKeywordType1,
        kw: String,
        pageable: Pageable,
        publicOnly: Boolean = false,
    ): Page<Post> {
        val builder = BooleanBuilder()

        if (publicOnly) {
            builder.and(post.published.isTrue)
            builder.and(post.listed.isTrue)
        }
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
            PostSearchKeywordType1.ALL -> buildAllKwPredicate(kw)
        }

    // -"quoted phrase" 또는 -word 형태의 부정 항 추출
    private val negativeTermRegex = Regex("""-"[^"]*"|-\S+""")

    /**
     * ALL 검색: 부정 항(-term, -"phrase")을 파싱하여 title/content 전체에 전역 적용.
     *
     * 단순 OR 구조로는 제외가 컬럼별로만 동작하는 버그가 있음:
     *   (title &@~ 'A -B') OR (content &@~ 'A -B')
     *   → title에 B가 있어도 content에서 매칭되면 포함됨
     *
     * 파싱 후 구조:
     *   (title &@~ positiveKw OR content &@~ positiveKw)
     *   AND NOT (title &@~ negKw OR content &@~ negKw)
     */
    private fun buildAllKwPredicate(kw: String): BooleanExpression {
        val negatives = negativeTermRegex.findAll(kw).map { it.value.removePrefix("-") }.toList()
        val positiveKw = negativeTermRegex.replace(kw, "").trim()

        val positiveExpr: BooleanExpression? = if (positiveKw.isNotBlank()) {
            pgroonga(post.title, positiveKw).or(pgroonga(post.content, positiveKw))
        } else null

        // PGroonga query syntax에서 공백은 AND이므로 각 부정 항을 별도로 적용
        val negativeExpr: BooleanExpression? = if (negatives.isNotEmpty()) {
            negatives.map { neg ->
                pgroonga(post.title, neg).or(pgroonga(post.content, neg)).not()
            }.reduce { acc, expr -> acc.and(expr) }
        } else null

        return when {
            positiveExpr != null && negativeExpr != null -> positiveExpr.and(negativeExpr)
            positiveExpr != null -> positiveExpr
            negativeExpr != null -> negativeExpr
            else -> Expressions.asBoolean(Expressions.constant(true))
        }
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
