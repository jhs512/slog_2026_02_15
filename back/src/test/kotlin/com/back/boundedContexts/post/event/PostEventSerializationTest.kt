package com.back.boundedContexts.post.event

import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.post.dto.PostCommentDto
import com.back.boundedContexts.post.dto.PostDto
import com.back.standard.dto.EventPayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID

/**
 * 이벤트 페이로드는 task 테이블에 JSON으로 저장된 뒤 나중에 역직렬화되어 처리된다.
 * 생성자 파라미터에 @param:JsonProperty가 붙지 않으면(예: @field:만 붙이면)
 * 역직렬화 시 값이 채워지지 않아 non-nullable 파라미터에서 실패하고,
 * 해당 이벤트의 후속 처리(활동 로그 기록)가 영구히 실패한다.
 */
class PostEventSerializationTest {

    // Spring 컨텍스트 없이 도는 순수 단위 테스트.
    // findAndAddModules로 Spring Boot와 동일하게 Kotlin/JavaTime 모듈을 등록한다.
    private val objectMapper = JsonMapper.builder().findAndAddModules().build()

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun postDto() = PostDto(
        1, now, now, 10, "작성자", "https://example.com/img",
        "제목", true, true, 0, 0, 0,
    )

    private fun postCommentDto() = PostCommentDto(
        2, now, now, 10, "작성자", "https://example.com/img",
        1, "댓글 내용", false, false,
    )

    private fun memberDto() = MemberDto(
        10, now, now, false, "행위자", "https://example.com/actor",
    )

    private inline fun <reified T : EventPayload> assertRoundTrip(event: T) {
        val json = objectMapper.writeValueAsString(event)
        val restored = objectMapper.readValue(json, T::class.java)

        assertThat(restored.uid).describedAs("uid").isEqualTo(event.uid)
        assertThat(restored.aggregateId).describedAs("aggregateId").isEqualTo(event.aggregateId)
    }

    @Test
    fun `PostDeletedEvent는 JSON 왕복 후에도 복원된다`() {
        assertRoundTrip(PostDeletedEvent(UUID.randomUUID(), postDto(), memberDto()))
    }

    @Test
    fun `PostModifiedEvent는 JSON 왕복 후에도 복원된다`() {
        assertRoundTrip(PostModifiedEvent(UUID.randomUUID(), postDto(), memberDto()))
    }

    @Test
    fun `PostWrittenEvent는 JSON 왕복 후에도 복원된다`() {
        assertRoundTrip(PostWrittenEvent(UUID.randomUUID(), postDto(), memberDto()))
    }

    @Test
    fun `PostCommentDeletedEvent는 JSON 왕복 후에도 복원된다`() {
        assertRoundTrip(
            PostCommentDeletedEvent(UUID.randomUUID(), postCommentDto(), postDto(), memberDto())
        )
    }

    @Test
    fun `PostCommentModifiedEvent는 JSON 왕복 후에도 복원된다`() {
        assertRoundTrip(
            PostCommentModifiedEvent(UUID.randomUUID(), postCommentDto(), postDto(), memberDto())
        )
    }

    @Test
    fun `PostCommentWrittenEvent는 JSON 왕복 후에도 복원된다`() {
        assertRoundTrip(
            PostCommentWrittenEvent(UUID.randomUUID(), postCommentDto(), postDto(), memberDto())
        )
    }
}
