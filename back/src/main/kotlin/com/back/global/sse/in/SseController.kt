package com.back.global.sse.`in`

import com.back.global.sse.app.SseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/sse")
@Tag(name = "SseController", description = "SSE 구독 컨트롤러")
class SseController(
    private val sseService: SseService,
) {
    @GetMapping("/{channel}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "채널 구독")
    fun subscribe(@PathVariable channel: String): SseEmitter {
        return sseService.subscribe(channel)
    }
}
