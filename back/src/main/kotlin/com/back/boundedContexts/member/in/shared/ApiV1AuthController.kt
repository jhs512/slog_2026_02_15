package com.back.boundedContexts.member.`in`.shared

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.member.dto.MemberWithUsernameDto
import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException
import com.back.global.web.util.Rq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/member/api/v1/auth")
@Tag(name = "ApiV1ActorController", description = "API 회원 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
class ApiV1AuthController(
    private val memberFacade: MemberFacade,
    private val rq: Rq
) {
    data class MemberLoginRequest(
        @field:NotBlank @field:Size(min = 2, max = 30)
        val username: String,
        @field:NotBlank @field:Size(min = 2, max = 30)
        val password: String,
    )

    data class MemberLoginResBody(
        val item: MemberDto,
        val apiKey: String,
        val accessToken: String
    )

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "로그인")
    fun login(
        @RequestBody @Valid reqBody: MemberLoginRequest
    ): RsData<MemberLoginResBody> {
        val member = memberFacade
            .findByUsername(reqBody.username)
            ?: throw BusinessException("401-1", "존재하지 않는 아이디입니다.")

        memberFacade.checkPassword(
            member,
            reqBody.password
        )

        val accessToken = memberFacade.genAccessToken(member)

        rq.setCookie("apiKey", member.apiKey)
        rq.setCookie("accessToken", accessToken)

        return RsData(
            "200-1",
            "${member.name}님 환영합니다.",
            MemberLoginResBody(
                MemberDto(member),
                member.apiKey,
                accessToken
            )
        )
    }


    @DeleteMapping("/logout")
    @Operation(summary = "로그아웃")
    fun logout(): RsData<Void> {
        rq.deleteCookie("apiKey")
        rq.deleteCookie("accessToken")

        return RsData(
            "200-1",
            "로그아웃 되었습니다."
        )
    }


    @GetMapping("/me")
    @Transactional(readOnly = true)
    @Operation(summary = "내 정보")
    fun me(): MemberWithUsernameDto {
        val actor = rq.actor

        return MemberWithUsernameDto(actor)
    }
}
