package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.postExtensions.checkActorCanDelete
import com.back.boundedContexts.post.domain.postExtensions.checkActorCanModify
import com.back.boundedContexts.post.domain.postExtensions.getCheckActorCanDeleteRs
import com.back.boundedContexts.post.domain.postExtensions.getCheckActorCanModifyRs
import com.back.global.exception.app.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PostCommentPolicyTest {

    private val postAuthor = Member(1, "user1", "글작성자")
    private val commentAuthor = Member(2, "user2", "댓글작성자")
    private val admin = Member(3, "admin", "관리자")
    private val other = Member(4, "other", "다른사용자")

    private val post = Post(postAuthor, "제목", "내용", true, true)

    @Nested
    inner class CanModify {
        @Test
        fun `댓글 작성자는 댓글을 수정할 수 있다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            val rs = comment.getCheckActorCanModifyRs(commentAuthor)

            assertThat(rs.isSuccess).isTrue()
        }

        @Test
        fun `다른 사용자는 댓글을 수정할 수 없다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            val rs = comment.getCheckActorCanModifyRs(other)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("403-1")
        }

        @Test
        fun `비로그인 사용자는 댓글을 수정할 수 없다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            val rs = comment.getCheckActorCanModifyRs(null)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("401-1")
        }

        @Test
        fun `checkActorCanModify 는 권한 없을 때 예외를 던진다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            assertThatThrownBy { comment.checkActorCanModify(other) }
                .isInstanceOf(BusinessException::class.java)
        }
    }

    @Nested
    inner class CanDelete {
        @Test
        fun `댓글 작성자는 댓글을 삭제할 수 있다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            val rs = comment.getCheckActorCanDeleteRs(commentAuthor)

            assertThat(rs.isSuccess).isTrue()
        }

        @Test
        fun `관리자는 다른 사람 댓글을 삭제할 수 있다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            val rs = comment.getCheckActorCanDeleteRs(admin)

            assertThat(rs.isSuccess).isTrue()
        }

        @Test
        fun `다른 사용자는 댓글을 삭제할 수 없다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            val rs = comment.getCheckActorCanDeleteRs(other)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("403-2")
        }

        @Test
        fun `비로그인 사용자는 댓글을 삭제할 수 없다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            val rs = comment.getCheckActorCanDeleteRs(null)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("401-1")
        }

        @Test
        fun `checkActorCanDelete 는 권한 없을 때 예외를 던진다`() {
            val comment = PostComment(commentAuthor, post, "댓글")

            assertThatThrownBy { comment.checkActorCanDelete(other) }
                .isInstanceOf(BusinessException::class.java)
        }
    }
}
