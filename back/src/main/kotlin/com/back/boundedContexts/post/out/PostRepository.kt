package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface PostRepository : JpaRepository<Post, Int>, PostRepositoryCustom {
    fun findFirstByOrderByIdDesc(): Post?
    fun findFirstByAuthorAndTitleAndPublishedFalseOrderByIdAsc(author: Member, title: String): Post?

    @Transactional
    @Modifying
    @Query("delete from Post p where p.id = :id")
    fun deleteRowById(@Param("id") id: Int)
}
