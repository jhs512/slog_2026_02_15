package com.back.boundedContexts.post.out

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostAttrRepository : JpaRepository<PostAttr, Int>, PostAttrRepositoryCustom {
    fun deleteBySubject(subject: Post)

    @Modifying
    @Query("delete from PostAttr pa where pa.subject.id = :subjectId")
    fun deleteBySubjectId(@Param("subjectId") subjectId: Int)
}
