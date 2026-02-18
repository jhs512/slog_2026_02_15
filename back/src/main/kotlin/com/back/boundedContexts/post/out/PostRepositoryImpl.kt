package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.QPost.post
import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.util.QueryDslUtil
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {
    override fun findQPagedByAuthor(author: Member, pageable: Pageable): Page<Post> {
        val query = buildPostListQuery()
            .where(post.author.eq(author))
            .orderBy(post.createdAt.desc(), post.id.desc())

        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val totalQuery = queryFactory
            .select(post.count())
            .from(post)
            .where(post.author.eq(author))

        return PageableExecutionUtils.getPage(results, pageable) {
            totalQuery.fetchFirst() ?: 0L
        }
    }

    override fun findQPagedByKw(kwType: PostSearchKeywordType1, kw: String, pageable: Pageable): Page<Post> {
        val builder = BooleanBuilder()

        if (kw.isNotBlank()) {
            // 정책: 글 검색은 title + content 통합 검색만 허용
            builder.and(
                post.title.containsIgnoreCase(kw)
                    .or(post.content.containsIgnoreCase(kw))
            )
        }

        val query = buildPostListQuery()
            .where(builder)

        QueryDslUtil.applySorting(query, pageable) { property ->
            when (property) {
                "createdAt" -> post.createdAt
                "id" -> post.createdAt
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

    private fun buildPostListQuery(): JPAQuery<Post> = queryFactory
        .selectFrom(post)
        .leftJoin(post.author).fetchJoin()
        .leftJoin(post.likesCountAttr).fetchJoin()
        .leftJoin(post.commentsCountAttr).fetchJoin()
        .leftJoin(post.hitCountAttr).fetchJoin()
}
