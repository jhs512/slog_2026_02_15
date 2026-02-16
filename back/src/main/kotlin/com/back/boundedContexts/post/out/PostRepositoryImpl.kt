package com.back.boundedContexts.post.out

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.QPost.post
import com.back.standard.util.QueryDslUtil
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PostRepositoryCustom {
    override fun findByKeyword(keyword: String, pageable: Pageable): Page<Post> {
        val builder = BooleanBuilder()

        if (keyword.isNotBlank()) {
            findByKeywordCondition(keyword).let { condition ->
                if (condition != null) builder.and(condition)
            }
        }

        val query = queryFactory
            .selectFrom(post)
            .where(builder)

        QueryDslUtil.applySorting(query, pageable) { property ->
            when (property) {
                "id" -> post.id
                "createdAt" -> post.createdAt
                else -> null
            }
        }

        val content = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val totalQuery = queryFactory
            .select(post.count())
            .from(post)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            totalQuery.fetchOne() ?: 0L
        }
    }

    private fun findByKeywordCondition(keyword: String): BooleanBuilder? {
        val orPatterns = Regex("\\s+OR\\s+", RegexOption.IGNORE_CASE)
        val andPatterns = Regex("\\s+AND\\s+", RegexOption.IGNORE_CASE)

        val orBuilder = BooleanBuilder()

        val orGroups = keyword.split(orPatterns).map { it.trim() }.filter { it.isNotBlank() }
        if (orGroups.isEmpty()) return null

        orGroups.forEach { orGroup ->
            val andTerms = orGroup.split(andPatterns).map { it.trim() }.filter { it.isNotBlank() }
            if (andTerms.isEmpty()) return@forEach

            val andBuilder = BooleanBuilder()
            andTerms.forEach { term ->
                andBuilder.and(
                    post.title.containsIgnoreCase(term)
                        .or(post.body.containsIgnoreCase(term))
                )
            }

            if (andBuilder.value != null) {
                orBuilder.or(andBuilder)
            }
        }

        return if (orBuilder.value != null) orBuilder else null
    }
}
