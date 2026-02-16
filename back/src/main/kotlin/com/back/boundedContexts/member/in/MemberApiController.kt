package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.domain.Member
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/member/api/v1/members")
class MemberApiController(
    private val memberFacade: MemberFacade
) {
    @PostMapping
    fun join(
        @RequestBody @Valid reqBody: MemberJoinReqBody
    ): ResponseEntity<MemberApiResponse<MemberDto>> {
        return try {
            val member = memberFacade.join(
                Member.newId(),
                reqBody.username,
                reqBody.password,
                reqBody.nickname
            )

            ResponseEntity.status(HttpStatus.CREATED).body(
                MemberApiResponse(
                    message = "회원가입이 완료되었습니다.",
                    data = MemberDto(member)
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                MemberApiResponse<MemberDto>(message = e.message ?: "회원가입 실패")
            )
        }
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid reqBody: MemberLoginReqBody,
        response: HttpServletResponse
    ): ResponseEntity<MemberApiResponse<MemberLoginResBody>> {
        val member = memberFacade.findByUsername(reqBody.username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                MemberApiResponse(message = "존재하지 않는 아이디입니다.")
            )

        if (!memberFacade.checkPassword(member, reqBody.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                MemberApiResponse(message = "비밀번호가 일치하지 않습니다.")
            )
        }

        val apiKeyCookie = Cookie("apiKey", member.apiKey).also {
            it.path = "/"
            it.isHttpOnly = true
        }
        response.addCookie(apiKeyCookie)

        return ResponseEntity.ok(
            MemberApiResponse(
                message = "${member.nickname}님 환영합니다.",
                data = MemberLoginResBody(
                    item = MemberDto(member),
                    apiKey = member.apiKey
                )
            )
        )
    }

    @DeleteMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<MemberApiResponse<Unit>> {
        val apiKeyCookie = Cookie("apiKey", "")
            .also {
                it.path = "/"
                it.maxAge = 0
                it.isHttpOnly = true
            }
        response.addCookie(apiKeyCookie)

        return ResponseEntity.ok(MemberApiResponse(message = "로그아웃 되었습니다."))
    }

    @GetMapping("/me")
    fun me(@RequestHeader("Api-Key") apiKey: String?): ResponseEntity<MemberApiResponse<MemberDto>> {
        if (apiKey.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                MemberApiResponse(message = "Api-Key가 없습니다.")
            )
        }

        val member = memberFacade.findByApiKey(apiKey)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                MemberApiResponse(message = "유효하지 않은 Api-Key 입니다.")
            )

        return ResponseEntity.ok(
            MemberApiResponse(
                data = MemberDto(member),
                message = "내 정보 조회 성공"
            )
        )
    }

    data class MemberJoinReqBody(
        @field:NotBlank
        val username: String,
        @field:NotBlank
        val password: String,
        @field:NotBlank
        val nickname: String,
    )

    data class MemberLoginReqBody(
        @field:NotBlank
        val username: String,
        @field:NotBlank
        val password: String,
    )

    data class MemberLoginResBody(
        val item: MemberDto,
        val apiKey: String,
    )

    data class MemberDto(
        val id: Long,
        val username: String,
        val nickname: String,
        val apiKey: String,
    ) {
        constructor(member: Member) : this(
            id = member.id,
            username = member.username,
            nickname = member.nickname,
            apiKey = member.apiKey,
        )
    }

    data class MemberApiResponse<T>(
        val message: String,
        val data: T? = null,
    )
}
