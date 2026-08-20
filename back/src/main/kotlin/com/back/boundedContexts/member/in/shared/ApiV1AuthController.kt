package com.back.boundedContexts.member.`in`.shared

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.member.dto.MemberWithUsernameDto
import com.back.global.dto.RsData
import com.back.global.security.config.oauth2.app.KakaoApiClient
import com.back.global.exception.app.BusinessException
import com.back.global.web.app.Rq
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
    private val kakaoApiClient: KakaoApiClient,
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


    data class SocialLoginRequest(
        @field:NotBlank
        val accessToken: String,
    )

    /**
     * 네이티브 앱용 카카오 로그인.
     *
     * 웹은 /oauth2/authorization/kakao 리다이렉트를 쓰지만, 앱은 카카오 SDK 로 받은
     * 액세스 토큰을 들고 온다. 그 토큰이 우리 앱에서 발급된 것인지 확인한 뒤
     * 웹 로그인과 같은 규칙(username = "KAKAO__{id}")으로 회원을 찾거나 만든다.
     */
    @PostMapping("/social/kakao")
    @Transactional
    @Operation(summary = "카카오 네이티브 로그인")
    fun loginWithKakao(
        @RequestBody @Valid reqBody: SocialLoginRequest
    ): RsData<MemberLoginResBody> {
        val profile = kakaoApiClient.fetchProfile(reqBody.accessToken)

        val member = memberFacade.modifyOrJoin(
            "KAKAO__${profile.oauthUserId}",
            "",
            profile.nickname,
            profile.profileImgUrl
        ).data

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
